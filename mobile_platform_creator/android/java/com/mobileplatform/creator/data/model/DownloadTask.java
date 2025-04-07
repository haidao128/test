package com.mobileplatform.creator.data.model;

import android.graphics.Bitmap;
import androidx.annotation.NonNull;

import java.util.UUID;

/**
 * 下载任务数据模型
 * 存储下载任务的信息
 */
public class DownloadTask {
    // 下载状态常量
    public static final int STATUS_PENDING = 0;       // 等待中
    public static final int STATUS_RUNNING = 1;       // 下载中
    public static final int STATUS_PAUSED = 2;        // 已暂停
    public static final int STATUS_COMPLETED = 3;     // 已完成
    public static final int STATUS_FAILED = 4;        // 失败
    public static final int STATUS_CANCELED = 5;      // 已取消
    
    // 任务唯一标识
    private String id;
    
    // 下载链接
    private String url;
    
    // 保存路径
    private String savePath;
    
    // 文件名
    private String fileName;
    
    // 文件总大小(字节)
    private long totalSize;
    
    // 已下载大小(字节)
    private long downloadedSize;
    
    // 下载状态
    private int status;
    
    // 错误信息
    private String errorMessage;
    
    // 应用信息
    private StoreAppInfo appInfo;
    
    // 任务创建时间
    private long createTime;
    
    // 下载速度(字节/秒)
    private long speed;
    
    // 预计剩余时间(秒)
    private long estimatedTimeRemaining;
    
    /**
     * 默认构造函数
     */
    public DownloadTask() {
        this.id = UUID.randomUUID().toString();
        this.status = STATUS_PENDING;
        this.createTime = System.currentTimeMillis();
    }
    
    /**
     * 构造函数
     * 
     * @param url 下载链接
     * @param savePath 保存路径
     * @param fileName 文件名
     * @param appInfo 应用信息
     */
    public DownloadTask(String url, String savePath, String fileName, StoreAppInfo appInfo) {
        this();
        this.url = url;
        this.savePath = savePath;
        this.fileName = fileName;
        this.appInfo = appInfo;
    }
    
    /**
     * 获取任务ID
     * 
     * @return 任务ID
     */
    public String getId() {
        return id;
    }
    
    /**
     * 设置任务ID
     * 
     * @param id 任务ID
     */
    public void setId(String id) {
        this.id = id;
    }
    
    /**
     * 获取下载链接
     * 
     * @return 下载链接
     */
    public String getUrl() {
        return url;
    }
    
    /**
     * 设置下载链接
     * 
     * @param url 下载链接
     */
    public void setUrl(String url) {
        this.url = url;
    }
    
    /**
     * 获取保存路径
     * 
     * @return 保存路径
     */
    public String getSavePath() {
        return savePath;
    }
    
    /**
     * 设置保存路径
     * 
     * @param savePath 保存路径
     */
    public void setSavePath(String savePath) {
        this.savePath = savePath;
    }
    
    /**
     * 获取文件名
     * 
     * @return 文件名
     */
    public String getFileName() {
        return fileName;
    }
    
    /**
     * 设置文件名
     * 
     * @param fileName 文件名
     */
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
    
    /**
     * 获取文件总大小
     * 
     * @return 文件总大小(字节)
     */
    public long getTotalSize() {
        return totalSize;
    }
    
    /**
     * 设置文件总大小
     * 
     * @param totalSize 文件总大小(字节)
     */
    public void setTotalSize(long totalSize) {
        this.totalSize = totalSize;
    }
    
    /**
     * 获取已下载大小
     * 
     * @return 已下载大小(字节)
     */
    public long getDownloadedSize() {
        return downloadedSize;
    }
    
    /**
     * 设置已下载大小
     * 
     * @param downloadedSize 已下载大小(字节)
     */
    public void setDownloadedSize(long downloadedSize) {
        this.downloadedSize = downloadedSize;
    }
    
    /**
     * 获取下载状态
     * 
     * @return 下载状态
     */
    public int getStatus() {
        return status;
    }
    
    /**
     * 设置下载状态
     * 
     * @param status 下载状态
     */
    public void setStatus(int status) {
        this.status = status;
    }
    
    /**
     * 获取错误信息
     * 
     * @return 错误信息
     */
    public String getErrorMessage() {
        return errorMessage;
    }
    
    /**
     * 设置错误信息
     * 
     * @param errorMessage 错误信息
     */
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    /**
     * 获取应用信息
     * 
     * @return 应用信息
     */
    public StoreAppInfo getAppInfo() {
        return appInfo;
    }
    
    /**
     * 设置应用信息
     * 
     * @param appInfo 应用信息
     */
    public void setAppInfo(StoreAppInfo appInfo) {
        this.appInfo = appInfo;
    }
    
    /**
     * 获取任务创建时间
     * 
     * @return 任务创建时间
     */
    public long getCreateTime() {
        return createTime;
    }
    
    /**
     * 设置任务创建时间
     * 
     * @param createTime 任务创建时间
     */
    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }
    
    /**
     * 获取下载速度
     * 
     * @return 下载速度(字节/秒)
     */
    public long getSpeed() {
        return speed;
    }
    
    /**
     * 设置下载速度
     * 
     * @param speed 下载速度(字节/秒)
     */
    public void setSpeed(long speed) {
        this.speed = speed;
    }
    
    /**
     * 获取预计剩余时间
     * 
     * @return 预计剩余时间(秒)
     */
    public long getEstimatedTimeRemaining() {
        return estimatedTimeRemaining;
    }
    
    /**
     * 设置预计剩余时间
     * 
     * @param estimatedTimeRemaining 预计剩余时间(秒)
     */
    public void setEstimatedTimeRemaining(long estimatedTimeRemaining) {
        this.estimatedTimeRemaining = estimatedTimeRemaining;
    }
    
    /**
     * 计算下载进度
     * 
     * @return 下载进度(0-100)
     */
    public int getProgress() {
        if (totalSize <= 0) {
            return 0;
        }
        return (int) (downloadedSize * 100 / totalSize);
    }
    
    /**
     * 是否下载中
     * 
     * @return 是否下载中
     */
    public boolean isRunning() {
        return status == STATUS_RUNNING;
    }
    
    /**
     * 是否等待中
     * 
     * @return 是否等待中
     */
    public boolean isPending() {
        return status == STATUS_PENDING;
    }
    
    /**
     * 是否已暂停
     * 
     * @return 是否已暂停
     */
    public boolean isPaused() {
        return status == STATUS_PAUSED;
    }
    
    /**
     * 是否已完成
     * 
     * @return 是否已完成
     */
    public boolean isCompleted() {
        return status == STATUS_COMPLETED;
    }
    
    /**
     * 是否失败
     * 
     * @return 是否失败
     */
    public boolean isFailed() {
        return status == STATUS_FAILED;
    }
    
    /**
     * 是否已取消
     * 
     * @return 是否已取消
     */
    public boolean isCanceled() {
        return status == STATUS_CANCELED;
    }
    
    /**
     * 状态字符串
     * 
     * @return 状态字符串
     */
    public String getStatusText() {
        switch (status) {
            case STATUS_PENDING:
                return "等待中";
            case STATUS_RUNNING:
                return "下载中";
            case STATUS_PAUSED:
                return "已暂停";
            case STATUS_COMPLETED:
                return "已完成";
            case STATUS_FAILED:
                return "下载失败";
            case STATUS_CANCELED:
                return "已取消";
            default:
                return "未知状态";
        }
    }
    
    /**
     * 可以暂停
     * 
     * @return 是否可暂停
     */
    public boolean canPause() {
        return status == STATUS_RUNNING || status == STATUS_PENDING;
    }
    
    /**
     * 可以恢复
     * 
     * @return 是否可恢复
     */
    public boolean canResume() {
        return status == STATUS_PAUSED || status == STATUS_FAILED;
    }
    
    /**
     * 可以取消
     * 
     * @return 是否可取消
     */
    public boolean canCancel() {
        return status == STATUS_RUNNING || status == STATUS_PENDING || status == STATUS_PAUSED;
    }
    
    /**
     * 可以重试
     * 
     * @return 是否可重试
     */
    public boolean canRetry() {
        return status == STATUS_FAILED;
    }
    
    /**
     * 可以安装
     * 
     * @return 是否可安装
     */
    public boolean canInstall() {
        return status == STATUS_COMPLETED;
    }
    
    /**
     * 获取完整保存路径
     * 
     * @return 完整保存路径
     */
    @NonNull
    public String getFullSavePath() {
        if (savePath == null || fileName == null) {
            return "";
        }
        return savePath + "/" + fileName;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        DownloadTask that = (DownloadTask) o;
        
        return id != null ? id.equals(that.id) : that.id == null;
    }
    
    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
} 