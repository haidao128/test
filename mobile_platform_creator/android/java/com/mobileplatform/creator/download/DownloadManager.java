package com.mobileplatform.creator.download;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.mobileplatform.creator.data.model.DownloadTask;
import com.mobileplatform.creator.data.model.StoreAppInfo;
import com.mobileplatform.creator.data.repository.DownloadTaskRepository;
import com.mobileplatform.creator.utils.AppInstaller;
import com.mobileplatform.creator.utils.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 下载管理器
 * 管理应用下载任务
 */
public class DownloadManager {
    private static final String TAG = "DownloadManager";
    
    // 单例实例
    private static DownloadManager instance;
    
    // 最大同时下载数
    private static final int MAX_CONCURRENT_DOWNLOADS = 3;
    
    // 上下文
    private final Context context;
    
    // 下载任务Map
    private final ConcurrentHashMap<String, DownloadTask> downloadTasks;
    
    // 下载执行器Map
    private final Map<String, DownloadExecutor> executors;
    
    // 线程池
    private final ExecutorService executorService;
    
    // 主线程Handler
    private final Handler mainHandler;
    
    // 下载通知管理器
    private final DownloadNotificationManager notificationManager;
    
    // 下载监听器列表
    private final List<DownloadListener> listeners;
    
    // 下载任务仓库
    private final DownloadTaskRepository taskRepository;
    
    /**
     * 获取单例实例
     * 
     * @param context 上下文
     * @return 下载管理器实例
     */
    public static synchronized DownloadManager getInstance(Context context) {
        if (instance == null) {
            instance = new DownloadManager(context.getApplicationContext());
        }
        return instance;
    }
    
    /**
     * 构造函数
     * 
     * @param context 上下文
     */
    private DownloadManager(Context context) {
        this.context = context;
        this.downloadTasks = new ConcurrentHashMap<>();
        this.executors = new HashMap<>();
        this.executorService = Executors.newFixedThreadPool(MAX_CONCURRENT_DOWNLOADS);
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.notificationManager = new DownloadNotificationManager(context);
        this.listeners = new CopyOnWriteArrayList<>();
        this.taskRepository = DownloadTaskRepository.getInstance(context);
        
        // 恢复未完成的下载任务
        loadUnfinishedTasks();
    }
    
    /**
     * 加载未完成的下载任务
     */
    private void loadUnfinishedTasks() {
        taskRepository.getUnfinishedTasks(tasks -> {
            if (tasks != null && !tasks.isEmpty()) {
                Log.d(TAG, "恢复未完成的下载任务: " + tasks.size() + "个");
                
                // 添加到内存任务列表
                for (DownloadTask task : tasks) {
                    downloadTasks.put(task.getId(), task);
                    
                    // 通知任务添加
                    notifyDownloadAdded(task);
                    
                    // 显示通知
                    notificationManager.showNotification(task);
                }
                
                // 检查并开始等待的任务
                checkPendingTasks();
            }
        });
    }
    
    /**
     * 添加下载任务
     * 
     * @param storeAppInfo 应用信息
     * @return 下载任务
     */
    public DownloadTask addTask(StoreAppInfo storeAppInfo) {
        if (storeAppInfo == null || storeAppInfo.getDownloadUrl() == null) {
            Log.e(TAG, "添加下载任务失败: 无效的应用信息");
            return null;
        }
        
        // 检查是否已存在相同的下载任务
        final String downloadUrl = storeAppInfo.getDownloadUrl();
        taskRepository.findTaskByUrl(downloadUrl, existingTask -> {
            if (existingTask != null) {
                // 如果任务已完成，则重新创建任务
                if (existingTask.isCompleted() || existingTask.isFailed() || existingTask.isCanceled()) {
                    // 删除已存在的任务
                    taskRepository.deleteTask(existingTask);
                    // 创建新任务
                    createAndStartTask(storeAppInfo);
                } else {
                    // 如果任务正在下载或暂停，则恢复现有任务
                    Log.d(TAG, "已存在相同的下载任务: " + existingTask.getId());
                    
                    // 添加到内存任务列表
                    downloadTasks.put(existingTask.getId(), existingTask);
                    
                    // 通知任务添加
                    notifyDownloadAdded(existingTask);
                    
                    // 显示通知
                    notificationManager.showNotification(existingTask);
                    
                    // 如果任务是暂停状态，设置为等待状态并检查是否可以开始
                    if (existingTask.isPaused()) {
                        existingTask.setStatus(DownloadTask.STATUS_PENDING);
                        taskRepository.updateTask(existingTask);
                        checkPendingTasks();
                    }
                }
            } else {
                // 不存在相同的任务，创建新任务
                createAndStartTask(storeAppInfo);
            }
        });
        
        // 为了保持接口兼容性，先返回null，实际任务会异步创建
        return null;
    }
    
    /**
     * 创建并开始下载任务
     * 
     * @param storeAppInfo 应用信息
     */
    private DownloadTask createAndStartTask(StoreAppInfo storeAppInfo) {
        // 创建下载目录
        File downloadDir = new File(context.getExternalFilesDir(null), "downloads");
        if (!downloadDir.exists()) {
            if (!downloadDir.mkdirs()) {
                Log.e(TAG, "创建下载目录失败");
                return null;
            }
        }
        
        // 生成文件名
        String fileName = storeAppInfo.getPackageName() + "_" + storeAppInfo.getVersionName() + ".apk";
        
        // 创建下载任务
        DownloadTask task = new DownloadTask();
        task.setId(UUID.randomUUID().toString());
        task.setUrl(storeAppInfo.getDownloadUrl());
        task.setSavePath(downloadDir.getAbsolutePath());
        task.setFileName(fileName);
        task.setTotalSize(storeAppInfo.getSize());
        task.setStatus(DownloadTask.STATUS_PENDING);
        task.setAppInfo(storeAppInfo);
        task.setCreateTime(System.currentTimeMillis());
        
        // 保存到数据库
        taskRepository.saveTask(task);
        
        // 添加到内存任务列表
        downloadTasks.put(task.getId(), task);
        
        // 通知任务添加
        notifyDownloadAdded(task);
        
        // 显示通知
        notificationManager.showNotification(task);
        
        // 开始下载
        checkPendingTasks();
        
        Log.d(TAG, "添加下载任务: " + task.getId() + ", URL: " + task.getUrl());
        
        return task;
    }
    
    /**
     * 开始下载任务
     * 
     * @param task 下载任务
     */
    private void startTask(DownloadTask task) {
        if (task == null) {
            return;
        }
        
        // 检查当前正在下载的任务数
        int runningCount = 0;
        for (DownloadTask t : downloadTasks.values()) {
            if (t.isRunning()) {
                runningCount++;
            }
        }
        
        // 如果正在下载的任务数已达到最大值，则等待
        if (runningCount >= MAX_CONCURRENT_DOWNLOADS) {
            task.setStatus(DownloadTask.STATUS_PENDING);
            
            // 更新数据库
            taskRepository.updateTask(task);
            
            Log.d(TAG, "下载任务等待中: " + task.getId());
            return;
        }
        
        // 创建下载执行器
        DownloadExecutor executor = new DownloadExecutor(context, task, new DownloadCallbackImpl());
        
        // 添加到执行器Map
        executors.put(task.getId(), executor);
        
        // 设置任务状态
        task.setStatus(DownloadTask.STATUS_RUNNING);
        
        // 更新数据库
        taskRepository.updateTask(task);
        
        // 提交到线程池执行
        executorService.submit(executor);
        
        // 更新通知
        notificationManager.updateNotification(task);
        
        Log.d(TAG, "开始下载任务: " + task.getId());
    }
    
    /**
     * 暂停下载任务
     * 
     * @param taskId 任务ID
     */
    public void pauseTask(String taskId) {
        DownloadTask task = downloadTasks.get(taskId);
        if (task == null || !task.isRunning()) {
            return;
        }
        
        DownloadExecutor executor = executors.get(taskId);
        if (executor != null) {
            executor.pause();
        }
        
        task.setStatus(DownloadTask.STATUS_PAUSED);
        
        // 更新数据库
        taskRepository.updateTask(task);
        
        // 更新通知
        notificationManager.updateNotification(task);
        
        // 通知任务暂停
        notifyDownloadPaused(task);
        
        // 检查是否有等待的任务可以开始
        checkPendingTasks();
        
        Log.d(TAG, "暂停下载任务: " + taskId);
    }
    
    /**
     * 恢复下载任务
     * 
     * @param taskId 任务ID
     */
    public void resumeTask(String taskId) {
        DownloadTask task = downloadTasks.get(taskId);
        if (task == null || !task.isPaused()) {
            return;
        }
        
        // 设置为等待状态
        task.setStatus(DownloadTask.STATUS_PENDING);
        
        // 更新数据库
        taskRepository.updateTask(task);
        
        // 更新通知
        notificationManager.updateNotification(task);
        
        // 通知任务恢复
        notifyDownloadResumed(task);
        
        // 检查是否可以立即开始
        checkPendingTasks();
        
        Log.d(TAG, "恢复下载任务: " + taskId);
    }
    
    /**
     * 取消下载任务
     * 
     * @param taskId 任务ID
     */
    public void cancelTask(String taskId) {
        DownloadTask task = downloadTasks.get(taskId);
        if (task == null) {
            return;
        }
        
        DownloadExecutor executor = executors.get(taskId);
        if (executor != null) {
            executor.cancel();
            executors.remove(taskId);
        }
        
        task.setStatus(DownloadTask.STATUS_CANCELED);
        
        // 更新数据库
        taskRepository.updateTask(task);
        
        // 取消通知
        notificationManager.cancelNotification(task);
        
        // 通知任务取消
        notifyDownloadCancelled(task);
        
        // 删除临时文件
        File file = new File(task.getFullSavePath());
        if (file.exists()) {
            if (file.delete()) {
                Log.d(TAG, "删除已取消的下载文件: " + file.getAbsolutePath());
            }
        }
        
        // 从任务列表中移除
        downloadTasks.remove(taskId);
        
        // 检查是否有等待的任务可以开始
        checkPendingTasks();
        
        Log.d(TAG, "取消下载任务: " + taskId);
    }
    
    /**
     * 重试下载任务
     * 
     * @param taskId 任务ID
     */
    public void retryTask(String taskId) {
        DownloadTask task = downloadTasks.get(taskId);
        if (task == null || (!task.isFailed() && !task.isCanceled())) {
            return;
        }
        
        // 重置任务状态
        task.setStatus(DownloadTask.STATUS_PENDING);
        task.setDownloadedSize(0);
        task.setErrorMessage(null);
        
        // 更新数据库
        taskRepository.updateTask(task);
        
        // 更新通知
        notificationManager.showNotification(task);
        
        // 检查是否可以立即开始
        checkPendingTasks();
        
        Log.d(TAG, "重试下载任务: " + taskId);
    }
    
    /**
     * 安装下载任务
     * 
     * @param taskId 任务ID
     */
    public void installTask(String taskId) {
        DownloadTask task = downloadTasks.get(taskId);
        if (task == null || !task.isCompleted()) {
            return;
        }
        
        // 安装应用
        AppInstaller.installApkFile(context, task.getFullSavePath(), result -> {
            if (result.isSuccess()) {
                Log.d(TAG, "安装成功: " + task.getFileName());
                
                // 可以在安装成功后做一些操作，比如更新任务状态
            } else {
                Log.e(TAG, "安装失败: " + result.getErrorMessage());
            }
        });
        
        Log.d(TAG, "安装下载任务: " + taskId);
    }
    
    /**
     * 删除下载任务
     * 
     * @param taskId 任务ID
     * @param deleteFile 是否删除文件
     */
    public void deleteTask(String taskId, boolean deleteFile) {
        DownloadTask task = downloadTasks.get(taskId);
        if (task == null) {
            return;
        }
        
        // 如果任务正在下载，先取消
        if (task.isRunning() || task.isPaused() || task.isPending()) {
            cancelTask(taskId);
        }
        
        // 取消通知
        notificationManager.cancelNotification(task);
        
        // 删除文件
        if (deleteFile) {
            File file = new File(task.getFullSavePath());
            if (file.exists()) {
                if (file.delete()) {
                    Log.d(TAG, "删除下载文件: " + file.getAbsolutePath());
                }
            }
        }
        
        // 从数据库中删除
        taskRepository.deleteTask(task);
        
        // 从任务列表中移除
        downloadTasks.remove(taskId);
        
        Log.d(TAG, "删除下载任务: " + taskId);
    }
    
    /**
     * 检查等待中的任务
     */
    private void checkPendingTasks() {
        // 统计正在下载的任务数
        int runningCount = 0;
        for (DownloadTask task : downloadTasks.values()) {
            if (task.isRunning()) {
                runningCount++;
            }
        }
        
        // 如果正在下载的任务数已达到最大值，则不处理
        if (runningCount >= MAX_CONCURRENT_DOWNLOADS) {
            return;
        }
        
        // 按创建时间排序
        List<DownloadTask> pendingTasks = new ArrayList<>();
        for (DownloadTask task : downloadTasks.values()) {
            if (task.isPending()) {
                pendingTasks.add(task);
            }
        }
        
        // 排序
        Collections.sort(pendingTasks, Comparator.comparingLong(DownloadTask::getCreateTime));
        
        // 开始等待中的任务
        for (DownloadTask task : pendingTasks) {
            if (runningCount < MAX_CONCURRENT_DOWNLOADS) {
                startTask(task);
                runningCount++;
            } else {
                break;
            }
        }
    }
    
    /**
     * 获取所有任务
     * 
     * @return 任务列表
     */
    public List<DownloadTask> getAllTasks() {
        return new ArrayList<>(downloadTasks.values());
    }
    
    /**
     * 获取正在下载和等待中的任务
     * 
     * @return 任务列表
     */
    public List<DownloadTask> getRunningAndPendingTasks() {
        List<DownloadTask> tasks = new ArrayList<>();
        for (DownloadTask task : downloadTasks.values()) {
            if (task.isRunning() || task.isPending() || task.isPaused()) {
                tasks.add(task);
            }
        }
        return tasks;
    }
    
    /**
     * 获取已完成的任务
     * 
     * @return 任务列表
     */
    public List<DownloadTask> getCompletedTasks() {
        List<DownloadTask> tasks = new ArrayList<>();
        for (DownloadTask task : downloadTasks.values()) {
            if (task.isCompleted()) {
                tasks.add(task);
            }
        }
        return tasks;
    }
    
    /**
     * 获取失败的任务
     * 
     * @return 任务列表
     */
    public List<DownloadTask> getFailedTasks() {
        List<DownloadTask> tasks = new ArrayList<>();
        for (DownloadTask task : downloadTasks.values()) {
            if (task.isFailed() || task.isCanceled()) {
                tasks.add(task);
            }
        }
        return tasks;
    }
    
    /**
     * 获取任务
     * 
     * @param taskId 任务ID
     * @return 下载任务
     */
    public DownloadTask getTask(String taskId) {
        return downloadTasks.get(taskId);
    }
    
    /**
     * 添加下载监听器
     * 
     * @param listener 监听器
     */
    public void addDownloadListener(DownloadListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }
    
    /**
     * 移除下载监听器
     * 
     * @param listener 监听器
     */
    public void removeDownloadListener(DownloadListener listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }
    
    /**
     * 通知下载任务添加
     * 
     * @param task 下载任务
     */
    private void notifyDownloadAdded(DownloadTask task) {
        mainHandler.post(() -> {
            for (DownloadListener listener : listeners) {
                listener.onDownloadAdded(task);
            }
        });
    }
    
    /**
     * 通知下载任务更新
     * 
     * @param task 下载任务
     */
    private void notifyDownloadUpdated(DownloadTask task) {
        mainHandler.post(() -> {
            for (DownloadListener listener : listeners) {
                listener.onDownloadUpdated(task);
            }
        });
    }
    
    /**
     * 通知下载任务暂停
     * 
     * @param task 下载任务
     */
    private void notifyDownloadPaused(DownloadTask task) {
        mainHandler.post(() -> {
            for (DownloadListener listener : listeners) {
                listener.onDownloadPaused(task);
            }
        });
    }
    
    /**
     * 通知下载任务恢复
     * 
     * @param task 下载任务
     */
    private void notifyDownloadResumed(DownloadTask task) {
        mainHandler.post(() -> {
            for (DownloadListener listener : listeners) {
                listener.onDownloadResumed(task);
            }
        });
    }
    
    /**
     * 通知下载任务完成
     * 
     * @param task 下载任务
     */
    private void notifyDownloadCompleted(DownloadTask task) {
        mainHandler.post(() -> {
            for (DownloadListener listener : listeners) {
                listener.onDownloadCompleted(task);
            }
        });
    }
    
    /**
     * 通知下载任务失败
     * 
     * @param task 下载任务
     */
    private void notifyDownloadFailed(DownloadTask task) {
        mainHandler.post(() -> {
            for (DownloadListener listener : listeners) {
                listener.onDownloadFailed(task);
            }
        });
    }
    
    /**
     * 通知下载任务取消
     * 
     * @param task 下载任务
     */
    private void notifyDownloadCancelled(DownloadTask task) {
        mainHandler.post(() -> {
            for (DownloadListener listener : listeners) {
                listener.onDownloadCancelled(task);
            }
        });
    }
    
    /**
     * 清除所有已完成的任务
     */
    public void clearCompletedTasks() {
        List<DownloadTask> completedTasks = getCompletedTasks();
        for (DownloadTask task : completedTasks) {
            deleteTask(task.getId(), false);
        }
    }
    
    /**
     * 清除所有已失败的任务
     */
    public void clearFailedTasks() {
        List<DownloadTask> failedTasks = getFailedTasks();
        for (DownloadTask task : failedTasks) {
            deleteTask(task.getId(), true);
        }
    }
    
    /**
     * 下载回调实现类
     */
    private class DownloadCallbackImpl implements DownloadExecutor.Callback {
        @Override
        public void onStart(DownloadTask task) {
            // 更新数据库
            taskRepository.updateTask(task);
            
            // 更新通知
            notificationManager.updateNotification(task);
            
            // 通知任务更新
            notifyDownloadUpdated(task);
        }
        
        @Override
        public void onProgress(DownloadTask task) {
            // 更新数据库
            taskRepository.updateTask(task);
            
            // 更新通知
            notificationManager.updateNotification(task);
            
            // 通知任务更新
            notifyDownloadUpdated(task);
        }
        
        @Override
        public void onPause(DownloadTask task) {
            // 更新数据库
            taskRepository.updateTask(task);
            
            // 更新通知
            notificationManager.updateNotification(task);
            
            // 通知任务暂停
            notifyDownloadPaused(task);
            
            // 检查是否有等待的任务可以开始
            checkPendingTasks();
        }
        
        @Override
        public void onCancel(DownloadTask task) {
            // 更新数据库
            taskRepository.updateTask(task);
            
            // 取消通知
            notificationManager.cancelNotification(task);
            
            // 通知任务取消
            notifyDownloadCancelled(task);
            
            // 检查是否有等待的任务可以开始
            checkPendingTasks();
        }
        
        @Override
        public void onComplete(DownloadTask task) {
            // 更新数据库
            taskRepository.updateTask(task);
            
            // 更新通知
            notificationManager.completeNotification(task);
            
            // 通知任务完成
            notifyDownloadCompleted(task);
            
            // 检查是否有等待的任务可以开始
            checkPendingTasks();
        }
        
        @Override
        public void onError(DownloadTask task, String errorMessage) {
            // 更新数据库
            taskRepository.updateTask(task);
            
            // 更新通知
            notificationManager.failNotification(task, errorMessage);
            
            // 通知任务失败
            notifyDownloadFailed(task);
            
            // 检查是否有等待的任务可以开始
            checkPendingTasks();
        }
    }
    
    /**
     * 下载监听器接口
     */
    public interface DownloadListener {
        // 下载任务添加
        void onDownloadAdded(DownloadTask task);
        
        // 下载任务更新
        void onDownloadUpdated(DownloadTask task);
        
        // 下载任务暂停
        void onDownloadPaused(DownloadTask task);
        
        // 下载任务恢复
        void onDownloadResumed(DownloadTask task);
        
        // 下载任务完成
        void onDownloadCompleted(DownloadTask task);
        
        // 下载任务失败
        void onDownloadFailed(DownloadTask task);
        
        // 下载任务取消
        void onDownloadCancelled(DownloadTask task);
    }
} 