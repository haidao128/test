package com.mobileplatform.creator.data.model;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

/**
 * 安装日志条目模型类
 */
public class InstallLogEntry {
    
    // 日志操作类型
    public enum OperationType {
        INSTALL("安装"),
        UPDATE("更新"),
        UNINSTALL("卸载"),
        FAILED("失败");
        
        private final String displayName;
        
        OperationType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    // 日志ID
    private String id;
    
    // 应用ID
    private String appId;
    
    // 应用名称
    private String appName;
    
    // 应用包名
    private String packageName;
    
    // 应用版本
    private String version;
    
    // 操作类型
    private OperationType operationType;
    
    // 操作时间
    private long operationTime;
    
    // 操作结果
    private boolean success;
    
    // 错误信息
    private String errorMessage;
    
    // 附加信息
    private String additionalInfo;
    
    /**
     * 默认构造函数
     */
    public InstallLogEntry() {
        this.id = UUID.randomUUID().toString();
        this.operationTime = System.currentTimeMillis();
        this.success = true;
    }
    
    /**
     * 完整构造函数
     */
    public InstallLogEntry(String appId, String appName, String packageName, String version,
                         OperationType operationType, boolean success, String errorMessage,
                         String additionalInfo) {
        this();
        this.appId = appId;
        this.appName = appName;
        this.packageName = packageName;
        this.version = version;
        this.operationType = operationType;
        this.success = success;
        this.errorMessage = errorMessage;
        this.additionalInfo = additionalInfo;
    }
    
    /**
     * 快速创建成功的日志条目
     */
    public static InstallLogEntry createSuccessLog(String appId, String appName, String packageName,
                                               String version, OperationType operationType) {
        return new InstallLogEntry(appId, appName, packageName, version, operationType, true, null, null);
    }
    
    /**
     * 快速创建失败的日志条目
     */
    public static InstallLogEntry createFailureLog(String appId, String appName, String packageName,
                                               String version, OperationType operationType,
                                               String errorMessage) {
        return new InstallLogEntry(appId, appName, packageName, version, operationType, false, errorMessage, null);
    }
    
    // Getter和Setter方法
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
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
    
    public OperationType getOperationType() {
        return operationType;
    }
    
    public void setOperationType(OperationType operationType) {
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
    
    /**
     * 获取格式化的操作时间
     */
    public String getFormattedTime() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return dateFormat.format(new Date(operationTime));
    }
    
    /**
     * 获取格式化的操作时间(别名方法，与getFormattedTime功能相同)
     */
    public String getFormattedOperationTime() {
        return getFormattedTime();
    }
    
    /**
     * 获取操作类型的显示名称
     */
    public String getOperationTypeDisplayName() {
        return operationType != null ? operationType.getDisplayName() : "未知";
    }
    
    /**
     * 获取状态显示文本
     */
    public String getStatusText() {
        return success ? "成功" : "失败";
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        InstallLogEntry that = (InstallLogEntry) o;
        return id.equals(that.id);
    }
    
    @Override
    public int hashCode() {
        return id.hashCode();
    }
} 