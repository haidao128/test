package com.mobileplatform.creator.data.model;

import android.graphics.drawable.Drawable;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 应用信息数据模型
 */
public class AppInfo {
    private String id;
    private String name;
    private String packageName;
    private String version;
    private long size;
    private Drawable icon;
    private String installPath;
    private long installTime;
    private long updateTime;
    private boolean isSystemApp;
    
    /**
     * 创建应用信息对象
     */
    public AppInfo(String id, String name, String packageName, String version, 
                   long size, Drawable icon, String installPath) {
        this.id = id;
        this.name = name;
        this.packageName = packageName;
        this.version = version;
        this.size = size;
        this.icon = icon;
        this.installPath = installPath;
        this.installTime = System.currentTimeMillis();
        this.updateTime = System.currentTimeMillis();
        this.isSystemApp = false;
    }
    
    /**
     * 获取应用ID
     */
    public String getId() {
        return id;
    }
    
    /**
     * 设置应用ID
     */
    public void setId(String id) {
        this.id = id;
    }
    
    /**
     * 获取应用名称
     */
    public String getName() {
        return name;
    }
    
    /**
     * 设置应用名称
     */
    public void setName(String name) {
        this.name = name;
    }
    
    /**
     * 获取应用包名
     */
    public String getPackageName() {
        return packageName;
    }
    
    /**
     * 设置应用包名
     */
    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }
    
    /**
     * 获取应用版本
     */
    public String getVersion() {
        return version;
    }
    
    /**
     * 设置应用版本
     */
    public void setVersion(String version) {
        this.version = version;
    }
    
    /**
     * 获取应用大小
     */
    public long getSize() {
        return size;
    }
    
    /**
     * 设置应用大小
     */
    public void setSize(long size) {
        this.size = size;
    }
    
    /**
     * 获取应用图标
     */
    public Drawable getIcon() {
        return icon;
    }
    
    /**
     * 设置应用图标
     */
    public void setIcon(Drawable icon) {
        this.icon = icon;
    }
    
    /**
     * 获取安装路径
     */
    public String getInstallPath() {
        return installPath;
    }
    
    /**
     * 设置安装路径
     */
    public void setInstallPath(String installPath) {
        this.installPath = installPath;
    }
    
    /**
     * 获取安装时间
     */
    public long getInstallTime() {
        return installTime;
    }
    
    /**
     * 设置安装时间
     */
    public void setInstallTime(long installTime) {
        this.installTime = installTime;
    }
    
    /**
     * 获取更新时间
     */
    public long getUpdateTime() {
        return updateTime;
    }
    
    /**
     * 设置更新时间
     */
    public void setUpdateTime(long updateTime) {
        this.updateTime = updateTime;
    }
    
    /**
     * 获取系统应用标记
     */
    public boolean isSystemApp() {
        return isSystemApp;
    }
    
    /**
     * 设置系统应用标记
     */
    public void setSystemApp(boolean systemApp) {
        isSystemApp = systemApp;
    }
    
    /**
     * 获取格式化的应用大小
     */
    public String getFormattedSize() {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format(Locale.getDefault(), "%.1f KB", size / 1024.0);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format(Locale.getDefault(), "%.1f MB", size / (1024.0 * 1024.0));
        } else {
            return String.format(Locale.getDefault(), "%.1f GB", size / (1024.0 * 1024.0 * 1024.0));
        }
    }
    
    /**
     * 获取格式化的安装时间
     */
    public String getFormattedInstallTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date(installTime));
    }
    
    /**
     * 获取格式化的更新时间
     */
    public String getFormattedUpdateTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date(updateTime));
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        AppInfo appInfo = (AppInfo) o;
        
        return id.equals(appInfo.id);
    }
    
    @Override
    public int hashCode() {
        return id.hashCode();
    }
    
    @Override
    public String toString() {
        return "AppInfo{" +
                "name='" + name + '\'' +
                ", packageName='" + packageName + '\'' +
                ", version='" + version + '\'' +
                '}';
    }
} 