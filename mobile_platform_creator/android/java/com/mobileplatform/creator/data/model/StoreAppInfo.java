package com.mobileplatform.creator.data.model;

import android.graphics.drawable.Drawable;

/**
 * 商店应用信息数据模型
 */
public class StoreAppInfo {
    private final String id;            // 应用唯一标识
    private final String name;          // 应用名称
    private final String packageName;   // 包名
    private final String version;       // 版本号
    private final long size;            // 应用大小（字节）
    private final String description;   // 应用描述
    private final String developer;     // 开发者
    private final String downloadUrl;   // 下载地址
    private Drawable icon;              // 应用图标
    private boolean isDownloading;      // 是否正在下载
    private int downloadProgress;       // 下载进度（0-100）
    private boolean isInstalled;        // 是否已安装
    
    /**
     * 创建商店应用信息对象
     */
    public StoreAppInfo(String id, String name, String packageName, String version, 
                        long size, String description, String developer, 
                        String downloadUrl, Drawable icon) {
        this.id = id;
        this.name = name;
        this.packageName = packageName;
        this.version = version;
        this.size = size;
        this.description = description;
        this.developer = developer;
        this.downloadUrl = downloadUrl;
        this.icon = icon;
        this.isDownloading = false;
        this.downloadProgress = 0;
        this.isInstalled = false;
    }
    
    /**
     * 获取应用ID
     */
    public String getId() {
        return id;
    }
    
    /**
     * 获取应用名称
     */
    public String getName() {
        return name;
    }
    
    /**
     * 获取应用包名
     */
    public String getPackageName() {
        return packageName;
    }
    
    /**
     * 获取应用版本
     */
    public String getVersion() {
        return version;
    }
    
    /**
     * 获取应用大小
     */
    public long getSize() {
        return size;
    }
    
    /**
     * 获取应用描述
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * 获取开发者
     */
    public String getDeveloper() {
        return developer;
    }
    
    /**
     * 获取下载地址
     */
    public String getDownloadUrl() {
        return downloadUrl;
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
     * 判断是否正在下载
     */
    public boolean isDownloading() {
        return isDownloading;
    }
    
    /**
     * 设置下载状态
     */
    public void setDownloading(boolean downloading) {
        isDownloading = downloading;
    }
    
    /**
     * 获取下载进度
     */
    public int getDownloadProgress() {
        return downloadProgress;
    }
    
    /**
     * 设置下载进度
     */
    public void setDownloadProgress(int downloadProgress) {
        this.downloadProgress = downloadProgress;
    }
    
    /**
     * 判断是否已安装
     */
    public boolean isInstalled() {
        return isInstalled;
    }
    
    /**
     * 设置安装状态
     */
    public void setInstalled(boolean installed) {
        isInstalled = installed;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        StoreAppInfo that = (StoreAppInfo) o;
        
        return id.equals(that.id);
    }
    
    @Override
    public int hashCode() {
        return id.hashCode();
    }
    
    @Override
    public String toString() {
        return "StoreAppInfo{" +
                "name='" + name + '\'' +
                ", packageName='" + packageName + '\'' +
                ", version='" + version + '\'' +
                ", developer='" + developer + '\'' +
                ", size=" + size +
                '}';
    }
} 