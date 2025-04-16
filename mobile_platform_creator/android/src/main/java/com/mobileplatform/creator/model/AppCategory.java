package com.mobileplatform.creator.model;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;

/**
 * 应用与分类的关联表
 * 多对多关系：一个应用可以属于多个分类，一个分类可以包含多个应用
 */
@Entity(
    tableName = "app_categories",
    primaryKeys = {"package_name", "category_id"},
    indices = {
        @Index("package_name"),
        @Index("category_id")
    },
    foreignKeys = {
        @ForeignKey(
            entity = Category.class,
            parentColumns = "id",
            childColumns = "category_id",
            onDelete = ForeignKey.CASCADE
        )
    }
)
public class AppCategory {
    
    @NonNull
    @ColumnInfo(name = "package_name")
    private String packageName;  // 应用包名作为主键的一部分
    
    @NonNull
    @ColumnInfo(name = "category_id")
    private String categoryId;   // 分类ID作为主键的一部分
    
    @ColumnInfo(name = "add_time")
    private long addTime;        // 添加到分类的时间
    
    /**
     * 构造函数
     */
    public AppCategory(@NonNull String packageName, @NonNull String categoryId) {
        this.packageName = packageName;
        this.categoryId = categoryId;
        this.addTime = System.currentTimeMillis();
    }
    
    /**
     * 获取应用包名
     */
    @NonNull
    public String getPackageName() {
        return packageName;
    }
    
    /**
     * 设置应用包名
     */
    public void setPackageName(@NonNull String packageName) {
        this.packageName = packageName;
    }
    
    /**
     * 获取分类ID
     */
    @NonNull
    public String getCategoryId() {
        return categoryId;
    }
    
    /**
     * 设置分类ID
     */
    public void setCategoryId(@NonNull String categoryId) {
        this.categoryId = categoryId;
    }
    
    /**
     * 获取添加时间
     */
    public long getAddTime() {
        return addTime;
    }
    
    /**
     * 设置添加时间
     */
    public void setAddTime(long addTime) {
        this.addTime = addTime;
    }
} 