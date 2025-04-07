package com.mobileplatform.creator.model;

import android.graphics.drawable.Drawable;

/**
 * 用于表示应用信息的数据模型类。
 */
public class AppInfo {
    private String appName;
    private String packageName;
    private String versionName;
    private int versionCode;
    private Drawable icon;
    private String appPath; // 应用APK文件的路径
    // 可以根据需要添加其他信息，例如安装时间、权限等

    public AppInfo(String appName, String packageName, String versionName, int versionCode, Drawable icon, String appPath) {
        this.appName = appName;
        this.packageName = packageName;
        this.versionName = versionName;
        this.versionCode = versionCode;
        this.icon = icon;
        this.appPath = appPath;
    }

    // Getters
    public String getAppName() {
        return appName;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getVersionName() {
        return versionName;
    }

    public int getVersionCode() {
        return versionCode;
    }

    public Drawable getIcon() {
        return icon;
    }
    
    public String getAppPath() {
        return appPath;
    }

    // (可选) Setters - 如果需要修改 AppInfo 对象
    // public void setAppName(String appName) { this.appName = appName; }
    // ...
} 