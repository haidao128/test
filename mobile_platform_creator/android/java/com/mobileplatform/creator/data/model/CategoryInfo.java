package com.mobileplatform.creator.data.model;

import android.graphics.Color;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 应用分类信息模型类
 */
public class CategoryInfo {
    
    // 分类ID
    private String id;
    
    // 分类名称
    private String name;
    
    // 分类颜色
    private int color;
    
    // 分类描述
    private String description;
    
    // 分类图标
    private String icon;
    
    // 分类顺序
    private int order;
    
    // 是否为系统分类
    private boolean systemCategory;
    
    // 已添加到该分类的应用ID列表
    private List<String> appIds;
    
    /**
     * 默认构造函数
     */
    public CategoryInfo() {
        this.id = UUID.randomUUID().toString();
        this.appIds = new ArrayList<>();
        this.order = 0;
        this.systemCategory = false;
        this.color = Color.parseColor("#2196F3"); // 默认蓝色
    }
    
    /**
     * 构造函数
     *
     * @param name 分类名称
     */
    public CategoryInfo(String name) {
        this();
        this.name = name;
    }
    
    /**
     * 构造函数
     *
     * @param name 分类名称
     * @param color 分类颜色
     * @param icon 分类图标
     */
    public CategoryInfo(String name, int color, String icon) {
        this();
        this.name = name;
        this.color = color;
        this.icon = icon;
    }
    
    /**
     * 构造函数
     *
     * @param id 分类ID
     * @param name 分类名称
     * @param color 分类颜色
     * @param description 分类描述
     * @param icon 分类图标
     * @param order 分类顺序
     * @param systemCategory 是否为系统分类
     */
    public CategoryInfo(String id, String name, int color, String description, 
                        String icon, int order, boolean systemCategory) {
        this.id = id;
        this.name = name;
        this.color = color;
        this.description = description;
        this.icon = icon;
        this.order = order;
        this.systemCategory = systemCategory;
        this.appIds = new ArrayList<>();
    }
    
    /**
     * 获取分类ID
     */
    public String getId() {
        return id;
    }
    
    /**
     * 设置分类ID
     */
    public void setId(String id) {
        this.id = id;
    }
    
    /**
     * 获取分类名称
     */
    public String getName() {
        return name;
    }
    
    /**
     * 设置分类名称
     */
    public void setName(String name) {
        this.name = name;
    }
    
    /**
     * 获取分类颜色
     */
    public int getColor() {
        return color;
    }
    
    /**
     * 设置分类颜色
     */
    public void setColor(int color) {
        this.color = color;
    }
    
    /**
     * 获取分类描述
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * 设置分类描述
     */
    public void setDescription(String description) {
        this.description = description;
    }
    
    /**
     * 获取分类图标
     */
    public String getIcon() {
        return icon;
    }
    
    /**
     * 设置分类图标
     */
    public void setIcon(String icon) {
        this.icon = icon;
    }
    
    /**
     * 获取分类顺序
     */
    public int getOrder() {
        return order;
    }
    
    /**
     * 设置分类顺序
     */
    public void setOrder(int order) {
        this.order = order;
    }
    
    /**
     * 是否为系统分类
     */
    public boolean isSystemCategory() {
        return systemCategory;
    }
    
    /**
     * 设置是否为系统分类
     */
    public void setSystemCategory(boolean systemCategory) {
        this.systemCategory = systemCategory;
    }
    
    /**
     * 获取应用ID列表
     */
    public List<String> getAppIds() {
        return appIds;
    }
    
    /**
     * 设置应用ID列表
     */
    public void setAppIds(List<String> appIds) {
        this.appIds = appIds != null ? appIds : new ArrayList<>();
    }
    
    /**
     * 添加应用到分类
     */
    public void addApp(String appId) {
        if (appId != null && !appIds.contains(appId)) {
            appIds.add(appId);
        }
    }
    
    /**
     * 从分类中移除应用
     */
    public void removeApp(String appId) {
        appIds.remove(appId);
    }
    
    /**
     * 检查应用是否在该分类中
     */
    public boolean containsApp(String appId) {
        return appIds.contains(appId);
    }
    
    /**
     * 获取分类中的应用数量
     */
    public int getAppCount() {
        return appIds.size();
    }
    
    /**
     * 清空分类中的应用
     */
    public void clearApps() {
        appIds.clear();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        CategoryInfo that = (CategoryInfo) o;
        return id.equals(that.id);
    }
    
    @Override
    public int hashCode() {
        return id.hashCode();
    }
} 