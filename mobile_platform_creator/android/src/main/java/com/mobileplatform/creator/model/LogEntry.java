package com.mobileplatform.creator.model;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * 安装日志条目数据模型
 */
@Entity(tableName = "log_entries")
public class LogEntry {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    private long id;
    
    @ColumnInfo(name = "app_name")
    private String appName;        // 应用名称
    
    @ColumnInfo(name = "package_name")
    private String packageName;    // 包名
    
    @ColumnInfo(name = "operation_type")
    private String operationType;  // 操作类型（安装/卸载/更新）
    
    @ColumnInfo(name = "status")
    private String status;         // 状态（成功/失败）
    
    @ColumnInfo(name = "details")
    private String details;        // 详细信息
    
    @ColumnInfo(name = "timestamp")
    private long timestamp;        // 时间戳

    public LogEntry(String appName, String packageName, String operationType, 
                   String status, String details) {
        this.appName = appName;
        this.packageName = packageName;
        this.operationType = operationType;
        this.status = status;
        this.details = details;
        this.timestamp = System.currentTimeMillis();
    }

    // Getters
    public long getId() {
        return id;
    }

    public String getAppName() {
        return appName;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getOperationType() {
        return operationType;
    }

    public String getStatus() {
        return status;
    }

    public String getDetails() {
        return details;
    }

    public long getTimestamp() {
        return timestamp;
    }

    // Setters
    public void setId(long id) {
        this.id = id;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public void setOperationType(String operationType) {
        this.operationType = operationType;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
} 