package com.mobileplatform.creator.data.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.mobileplatform.creator.data.converter.StoreAppInfoConverter;
import com.mobileplatform.creator.data.model.DownloadTask;
import com.mobileplatform.creator.data.model.StoreAppInfo;

/**
 * 下载任务数据库实体
 */
@Entity(tableName = "download_tasks")
@TypeConverters(StoreAppInfoConverter.class)
public class DownloadTaskEntity {
    
    // 任务唯一标识
    @PrimaryKey
    @NonNull
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
    
    // 应用信息 (使用类型转换器转为JSON存储)
    private StoreAppInfo appInfo;
    
    // 任务创建时间
    private long createTime;
    
    /**
     * 默认构造函数
     */
    public DownloadTaskEntity() {
    }
    
    /**
     * 从DownloadTask转换构造函数
     * 
     * @param task 下载任务模型
     */
    public DownloadTaskEntity(DownloadTask task) {
        this.id = task.getId();
        this.url = task.getUrl();
        this.savePath = task.getSavePath();
        this.fileName = task.getFileName();
        this.totalSize = task.getTotalSize();
        this.downloadedSize = task.getDownloadedSize();
        this.status = task.getStatus();
        this.errorMessage = task.getErrorMessage();
        this.appInfo = task.getAppInfo();
        this.createTime = task.getCreateTime();
    }
    
    /**
     * 转换为DownloadTask
     * 
     * @return 下载任务模型
     */
    public DownloadTask toDownloadTask() {
        DownloadTask task = new DownloadTask();
        task.setId(this.id);
        task.setUrl(this.url);
        task.setSavePath(this.savePath);
        task.setFileName(this.fileName);
        task.setTotalSize(this.totalSize);
        task.setDownloadedSize(this.downloadedSize);
        task.setStatus(this.status);
        task.setErrorMessage(this.errorMessage);
        task.setAppInfo(this.appInfo);
        task.setCreateTime(this.createTime);
        return task;
    }
    
    @NonNull
    public String getId() {
        return id;
    }
    
    public void setId(@NonNull String id) {
        this.id = id;
    }
    
    public String getUrl() {
        return url;
    }
    
    public void setUrl(String url) {
        this.url = url;
    }
    
    public String getSavePath() {
        return savePath;
    }
    
    public void setSavePath(String savePath) {
        this.savePath = savePath;
    }
    
    public String getFileName() {
        return fileName;
    }
    
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
    
    public long getTotalSize() {
        return totalSize;
    }
    
    public void setTotalSize(long totalSize) {
        this.totalSize = totalSize;
    }
    
    public long getDownloadedSize() {
        return downloadedSize;
    }
    
    public void setDownloadedSize(long downloadedSize) {
        this.downloadedSize = downloadedSize;
    }
    
    public int getStatus() {
        return status;
    }
    
    public void setStatus(int status) {
        this.status = status;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public StoreAppInfo getAppInfo() {
        return appInfo;
    }
    
    public void setAppInfo(StoreAppInfo appInfo) {
        this.appInfo = appInfo;
    }
    
    public long getCreateTime() {
        return createTime;
    }
    
    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }
} 