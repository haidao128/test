package com.mobileplatform.creator.model;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * 应用分类数据模型
 */
@Entity(tableName = "categories")
public class Category {
    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id")
    private String id;          // 分类ID
    
    @ColumnInfo(name = "name")
    private String name;        // 分类名称
    
    @ColumnInfo(name = "description")
    private String description; // 分类描述
    
    @ColumnInfo(name = "app_count")
    private int appCount;       // 应用数量
    
    @ColumnInfo(name = "create_time")
    private long createTime;    // 创建时间
    
    @ColumnInfo(name = "update_time")
    private long updateTime;    // 更新时间

    public Category(@NonNull String id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.appCount = 0;
        this.createTime = System.currentTimeMillis();
        this.updateTime = System.currentTimeMillis();
    }

    // Getters
    @NonNull
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public int getAppCount() {
        return appCount;
    }

    public long getCreateTime() {
        return createTime;
    }

    public long getUpdateTime() {
        return updateTime;
    }

    // Setters
    public void setId(@NonNull String id) {
        this.id = id;
    }
    
    public void setName(String name) {
        this.name = name;
        this.updateTime = System.currentTimeMillis();
    }

    public void setDescription(String description) {
        this.description = description;
        this.updateTime = System.currentTimeMillis();
    }

    public void setAppCount(int appCount) {
        this.appCount = appCount;
        this.updateTime = System.currentTimeMillis();
    }
    
    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }
    
    public void setUpdateTime(long updateTime) {
        this.updateTime = updateTime;
    }

    public void incrementAppCount() {
        this.appCount++;
        this.updateTime = System.currentTimeMillis();
    }

    public void decrementAppCount() {
        if (this.appCount > 0) {
            this.appCount--;
            this.updateTime = System.currentTimeMillis();
        }
    }
} 