package com.mobileplatform.creator.data.db;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.mobileplatform.creator.data.model.InstallLogEntry;

/**
 * 安装日志数据库实体类
 */
@Entity(tableName = "install_logs")
public class InstallLogEntity {
    
    @PrimaryKey
    @NonNull
    private String id;
    
    private String appId;
    
    private String appName;
    
    private String packageName;
    
    private String version;
    
    private int operationType;
    
    private long operationTime;
    
    private boolean success;
    
    private String errorMessage;
    
    private String additionalInfo;
    
    /**
     * 默认构造函数
     */
    public InstallLogEntity() {
    }
    
    /**
     * 从LogEntry转换为Entity
     */
    public static InstallLogEntity fromLogEntry(InstallLogEntry logEntry) {
        InstallLogEntity entity = new InstallLogEntity();
        entity.id = logEntry.getId();
        entity.appId = logEntry.getAppId();
        entity.appName = logEntry.getAppName();
        entity.packageName = logEntry.getPackageName();
        entity.version = logEntry.getVersion();
        entity.operationType = logEntry.getOperationType() != null ? 
                logEntry.getOperationType().ordinal() : 0;
        entity.operationTime = logEntry.getOperationTime();
        entity.success = logEntry.isSuccess();
        entity.errorMessage = logEntry.getErrorMessage();
        entity.additionalInfo = logEntry.getAdditionalInfo();
        return entity;
    }
    
    /**
     * 转换为LogEntry
     */
    public InstallLogEntry toLogEntry() {
        InstallLogEntry logEntry = new InstallLogEntry();
        logEntry.setId(id);
        logEntry.setAppId(appId);
        logEntry.setAppName(appName);
        logEntry.setPackageName(packageName);
        logEntry.setVersion(version);
        
        InstallLogEntry.OperationType type = InstallLogEntry.OperationType.INSTALL;
        if (operationType >= 0 && operationType < InstallLogEntry.OperationType.values().length) {
            type = InstallLogEntry.OperationType.values()[operationType];
        }
        logEntry.setOperationType(type);
        
        logEntry.setOperationTime(operationTime);
        logEntry.setSuccess(success);
        logEntry.setErrorMessage(errorMessage);
        logEntry.setAdditionalInfo(additionalInfo);
        return logEntry;
    }
    
    // Getter和Setter方法
    
    @NonNull
    public String getId() {
        return id;
    }
    
    public void setId(@NonNull String id) {
        this.id = id;
    }
    
    public String getAppId() {
        return appId;
    }
    
    public void setAppId(String appId) {
        this.appId = appId;
    }
    
    public String getAppName() {
        return appName;
    }
    
    public void setAppName(String appName) {
        this.appName = appName;
    }
    
    public String getPackageName() {
        return packageName;
    }
    
    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }
    
    public String getVersion() {
        return version;
    }
    
    public void setVersion(String version) {
        this.version = version;
    }
    
    public int getOperationType() {
        return operationType;
    }
    
    public void setOperationType(int operationType) {
        this.operationType = operationType;
    }
    
    public long getOperationTime() {
        return operationTime;
    }
    
    public void setOperationTime(long operationTime) {
        this.operationTime = operationTime;
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public String getAdditionalInfo() {
        return additionalInfo;
    }
    
    public void setAdditionalInfo(String additionalInfo) {
        this.additionalInfo = additionalInfo;
    }
} 