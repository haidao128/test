package com.mobileplatform.creator.download;

import android.content.Context;
import android.util.Log;

import com.mobileplatform.creator.data.model.DownloadTask;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * 下载执行器
 * 实际负责下载任务的执行
 */
public class DownloadExecutor implements Runnable {
    private static final String TAG = "DownloadExecutor";
    
    // 缓冲区大小
    private static final int BUFFER_SIZE = 8192;
    
    // 下载任务
    private final DownloadTask task;
    
    // 上下文
    private final Context context;
    
    // 回调接口
    private final Callback callback;
    
    // 是否暂停
    private volatile boolean isPaused = false;
    
    // 是否取消
    private volatile boolean isCancelled = false;
    
    // 上次更新时间
    private long lastUpdateTime = 0;
    
    // 上次更新大小
    private long lastDownloadedSize = 0;
    
    /**
     * 构造函数
     * 
     * @param context 上下文
     * @param task 下载任务
     * @param callback 下载回调
     */
    public DownloadExecutor(Context context, DownloadTask task, Callback callback) {
        this.context = context;
        this.task = task;
        this.callback = callback;
    }
    
    /**
     * 暂停下载
     */
    public void pause() {
        isPaused = true;
    }
    
    /**
     * 恢复下载
     */
    public void resume() {
        isPaused = false;
    }
    
    /**
     * 取消下载
     */
    public void cancel() {
        isCancelled = true;
    }
    
    @Override
    public void run() {
        HttpURLConnection connection = null;
        InputStream input = null;
        RandomAccessFile randomAccessFile = null;
        BufferedInputStream bufferedInput = null;
        
        try {
            // 通知开始下载
            callback.onStart(task);
            
            // 创建文件目录
            File file = new File(task.getSavePath());
            File dir = file.getParentFile();
            if (!dir.exists()) {
                if (!dir.mkdirs()) {
                    throw new IOException("创建目录失败: " + dir.getAbsolutePath());
                }
            }
            
            // 获取已下载大小
            long downloadedSize = 0;
            if (file.exists()) {
                downloadedSize = file.length();
                task.setDownloadedSize(downloadedSize);
            }
            
            // 创建URL
            URL url = new URL(task.getUrl());
            
            // 设置连接
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            
            // 设置断点续传
            if (downloadedSize > 0) {
                connection.setRequestProperty("Range", "bytes=" + downloadedSize + "-");
            }
            
            // 获取响应码
            int responseCode = connection.getResponseCode();
            
            // 获取文件总大小
            long totalSize = downloadedSize;
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // 不支持断点续传，重新下载
                totalSize = connection.getContentLength();
                if (totalSize <= 0) {
                    totalSize = task.getTotalSize();
                }
                
                // 删除已存在的文件
                if (file.exists()) {
                    if (!file.delete()) {
                        throw new IOException("删除文件失败: " + file.getAbsolutePath());
                    }
                }
                
                downloadedSize = 0;
                task.setDownloadedSize(0);
                
            } else if (responseCode == HttpURLConnection.HTTP_PARTIAL) {
                // 支持断点续传
                String contentRange = connection.getHeaderField("Content-Range");
                if (contentRange != null) {
                    int separatorIndex = contentRange.lastIndexOf('/');
                    if (separatorIndex > 0) {
                        totalSize = Long.parseLong(contentRange.substring(separatorIndex + 1));
                    } else {
                        totalSize = connection.getContentLength() + downloadedSize;
                    }
                } else {
                    totalSize = connection.getContentLength() + downloadedSize;
                }
                
            } else {
                throw new IOException("服务器响应异常: " + responseCode);
            }
            
            // 设置总大小
            task.setTotalSize(totalSize);
            
            // 获取输入流
            input = connection.getInputStream();
            bufferedInput = new BufferedInputStream(input);
            
            // 创建文件
            randomAccessFile = new RandomAccessFile(file, "rw");
            if (downloadedSize > 0) {
                randomAccessFile.seek(downloadedSize);
            }
            
            // 设置下载开始时间
            lastUpdateTime = System.currentTimeMillis();
            lastDownloadedSize = downloadedSize;
            
            // 读取数据
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            
            while ((bytesRead = bufferedInput.read(buffer)) != -1) {
                // 检查是否暂停
                if (isPaused) {
                    callback.onPause(task);
                    return;
                }
                
                // 检查是否取消
                if (isCancelled) {
                    callback.onCancel(task);
                    return;
                }
                
                // 写入文件
                randomAccessFile.write(buffer, 0, bytesRead);
                
                // 更新下载进度
                downloadedSize += bytesRead;
                task.setDownloadedSize(downloadedSize);
                
                // 计算下载速度，每秒更新一次
                long currentTime = System.currentTimeMillis();
                long timeDiff = currentTime - lastUpdateTime;
                if (timeDiff >= 1000) {
                    long sizeDiff = downloadedSize - lastDownloadedSize;
                    long speed = (sizeDiff * 1000) / timeDiff;
                    
                    task.setSpeed(speed);
                    
                    // 计算剩余时间
                    if (speed > 0) {
                        long remainingSize = totalSize - downloadedSize;
                        long remainingTime = remainingSize / speed;
                        task.setEstimatedTimeRemaining(remainingTime);
                    }
                    
                    // 通知进度更新
                    callback.onProgress(task);
                    
                    // 更新基准值
                    lastUpdateTime = currentTime;
                    lastDownloadedSize = downloadedSize;
                }
            }
            
            // 如果下载完成
            if (downloadedSize >= totalSize) {
                task.setDownloadedSize(totalSize);
                task.setStatus(DownloadTask.STATUS_COMPLETED);
                callback.onComplete(task);
            } else {
                // 下载未完成但已结束（可能是因为服务器问题）
                callback.onError(task, "下载未完成，可能是网络连接中断");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "下载错误: " + e.getMessage(), e);
            
            // 设置错误信息
            task.setStatus(DownloadTask.STATUS_FAILED);
            task.setErrorMessage(e.getMessage());
            
            // 通知下载失败
            callback.onError(task, e.getMessage());
            
        } finally {
            // 关闭资源
            try {
                if (randomAccessFile != null) {
                    randomAccessFile.close();
                }
                
                if (bufferedInput != null) {
                    bufferedInput.close();
                }
                
                if (input != null) {
                    input.close();
                }
                
                if (connection != null) {
                    connection.disconnect();
                }
            } catch (IOException e) {
                Log.e(TAG, "关闭资源错误: " + e.getMessage(), e);
            }
        }
    }
    
    /**
     * 下载回调接口
     */
    public interface Callback {
        /**
         * 开始下载
         * 
         * @param task 下载任务
         */
        void onStart(DownloadTask task);
        
        /**
         * 下载进度更新
         * 
         * @param task 下载任务
         */
        void onProgress(DownloadTask task);
        
        /**
         * 下载暂停
         * 
         * @param task 下载任务
         */
        void onPause(DownloadTask task);
        
        /**
         * 下载取消
         * 
         * @param task 下载任务
         */
        void onCancel(DownloadTask task);
        
        /**
         * 下载完成
         * 
         * @param task 下载任务
         */
        void onComplete(DownloadTask task);
        
        /**
         * 下载错误
         * 
         * @param task 下载任务
         * @param errorMessage 错误信息
         */
        void onError(DownloadTask task, String errorMessage);
    }
} 