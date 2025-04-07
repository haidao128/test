package com.mobileplatform.creator.data;

import android.graphics.Bitmap;

/**
 * 商店应用信息数据模型
 * 用于存储应用商店中的应用信息
 */
public class StoreAppInfo {
    // 应用ID
    private String id;
    // 应用名称
    private String name;
    // 开发者名称
    private String developer;
    // 应用描述
    private String description;
    // 应用版本
    private String version;
    // 应用大小（以字节为单位）
    private long size;
    // 应用评分（1-5星）
    private float rating;
    // 下载次数
    private int downloadCount;
    // 应用图标
    private Bitmap icon;
    // 应用横幅图片（用于精选应用展示）
    private Bitmap banner;
    // 应用包类型（wasm或native）
    private String packageType;
    // 下载URL
    private String downloadUrl;
    // 是否为精选应用
    private boolean featured;
    // 发布日期
    private String releaseDate;
    // 下载状态（0: 未下载, 1: 下载中, 2: 已下载未安装, 3: 已安装）
    private int downloadStatus;
    // 下载进度（0-100）
    private int downloadProgress;

    /**
     * 构造函数
     */
    public StoreAppInfo() {
        // 默认值
        this.rating = 0.0f;
        this.downloadCount = 0;
        this.featured = false;
        this.downloadStatus = 0;
        this.downloadProgress = 0;
    }

    /**
     * 带参数的构造函数
     * 
     * @param id 应用ID
     * @param name 应用名称
     * @param developer 开发者名称
     * @param description 应用描述
     */
    public StoreAppInfo(String id, String name, String developer, String description) {
        this();
        this.id = id;
        this.name = name;
        this.developer = developer;
        this.description = description;
    }

    // Getter和Setter方法

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDeveloper() {
        return developer;
    }

    public void setDeveloper(String developer) {
        this.developer = developer;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public float getRating() {
        return rating;
    }

    public void setRating(float rating) {
        this.rating = rating;
    }

    public int getDownloadCount() {
        return downloadCount;
    }

    public void setDownloadCount(int downloadCount) {
        this.downloadCount = downloadCount;
    }

    public Bitmap getIcon() {
        return icon;
    }

    public void setIcon(Bitmap icon) {
        this.icon = icon;
    }

    public Bitmap getBanner() {
        return banner;
    }

    public void setBanner(Bitmap banner) {
        this.banner = banner;
    }

    public String getPackageType() {
        return packageType;
    }

    public void setPackageType(String packageType) {
        this.packageType = packageType;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public boolean isFeatured() {
        return featured;
    }

    public void setFeatured(boolean featured) {
        this.featured = featured;
    }

    public String getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(String releaseDate) {
        this.releaseDate = releaseDate;
    }

    public int getDownloadStatus() {
        return downloadStatus;
    }

    public void setDownloadStatus(int downloadStatus) {
        this.downloadStatus = downloadStatus;
    }

    public int getDownloadProgress() {
        return downloadProgress;
    }

    public void setDownloadProgress(int downloadProgress) {
        this.downloadProgress = downloadProgress;
    }

    /**
     * 获取应用大小的友好显示字符串
     * 
     * @return 格式化后的大小字符串，如：1.2 MB
     */
    public String getFormattedSize() {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.1f KB", size / 1024.0);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", size / (1024.0 * 1024.0));
        } else {
            return String.format("%.1f GB", size / (1024.0 * 1024.0 * 1024.0));
        }
    }

    /**
     * 获取下载状态的文字描述
     * 
     * @return 下载状态的描述字符串
     */
    public String getDownloadStatusText() {
        switch (downloadStatus) {
            case 0:
                return "下载";
            case 1:
                return "下载中…";
            case 2:
                return "安装";
            case 3:
                return "打开";
            default:
                return "下载";
        }
    }
} 