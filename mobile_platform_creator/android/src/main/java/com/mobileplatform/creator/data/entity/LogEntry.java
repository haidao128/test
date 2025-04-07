package com.mobileplatform.creator.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * 安装日志的数据表实体类。
 */
@Entity(tableName = "install_logs")
public class LogEntry {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public String packageName; // 应用包名
    public String appName;     // 应用名称
    public String versionName; // 版本名称
    public int versionCode;    // 版本号
    public long timestamp;     // 操作时间戳
    public String operationType; // 操作类型 (e.g., INSTALL, UPDATE, UNINSTALL, FAILED)
    public String status;        // 操作状态 (e.g., SUCCESS, FAILURE)
    public String details;       // 详细信息或错误原因

    // 可以根据需要添加更多字段，例如操作者、设备信息等

    // Room 需要一个无参构造函数
    public LogEntry() {}

    // 为方便创建实例，可以添加一个带参数的构造函数（但Room主要使用无参构造和setter）
    @androidx.room.Ignore
    public LogEntry(String packageName, String appName, String versionName, int versionCode, 
                    long timestamp, String operationType, String status, String details) {
        this.packageName = packageName;
        this.appName = appName;
        this.versionName = versionName;
        this.versionCode = versionCode;
        this.timestamp = timestamp;
        this.operationType = operationType;
        this.status = status;
        this.details = details;
    }
} 