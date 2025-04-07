package com.mobileplatform.creator.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mobileplatform.creator.data.model.CategoryInfo;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 应用分类管理器
 */
public class CategoryManager {
    
    private static final String TAG = "CategoryManager";
    
    // 单例实例
    private static CategoryManager instance;
    
    // 上下文
    private Context context;
    
    // SharedPreferences名称
    private static final String PREFS_NAME = "category_prefs";
    
    // 分类列表键
    private static final String KEY_CATEGORIES = "categories";
    
    // 分类映射 (ID -> CategoryInfo)
    private Map<String, CategoryInfo> categoryMap;
    
    // 默认分类ID
    public static final String DEFAULT_CATEGORY_ID = "default";
    
    // 系统分类ID
    public static final String SYSTEM_CATEGORY_ID = "system";
    
    // 游戏分类ID
    public static final String GAMES_CATEGORY_ID = "games";
    
    // 工具分类ID
    public static final String TOOLS_CATEGORY_ID = "tools";
    
    // 社交分类ID
    public static final String SOCIAL_CATEGORY_ID = "social";
    
    /**
     * 私有构造函数
     */
    private CategoryManager(Context context) {
        this.context = context.getApplicationContext();
        this.categoryMap = new HashMap<>();
        loadCategories();
    }
    
    /**
     * 获取单例实例
     */
    public static synchronized CategoryManager getInstance(Context context) {
        if (instance == null) {
            instance = new CategoryManager(context);
        }
        return instance;
    }
    
    /**
     * 加载分类列表
     */
    private void loadCategories() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String categoriesJson = prefs.getString(KEY_CATEGORIES, null);
        
        if (categoriesJson != null) {
            try {
                Gson gson = new Gson();
                Type type = new TypeToken<List<CategoryInfo>>() {}.getType();
                List<CategoryInfo> categories = gson.fromJson(categoriesJson, type);
                
                // 将列表转换为映射
                categoryMap.clear();
                for (CategoryInfo category : categories) {
                    categoryMap.put(category.getId(), category);
                }
                
                Log.d(TAG, "已加载 " + categoryMap.size() + " 个分类");
            } catch (Exception e) {
                Log.e(TAG, "加载分类失败: " + e.getMessage());
                createDefaultCategories();
            }
        } else {
            // 如果没有保存的分类，创建默认分类
            createDefaultCategories();
        }
    }
    
    /**
     * 保存分类列表
     */
    private void saveCategories() {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            Gson gson = new Gson();
            List<CategoryInfo> categories = new ArrayList<>(categoryMap.values());
            String categoriesJson = gson.toJson(categories);
            
            prefs.edit().putString(KEY_CATEGORIES, categoriesJson).apply();
            Log.d(TAG, "已保存 " + categories.size() + " 个分类");
        } catch (Exception e) {
            Log.e(TAG, "保存分类失败: " + e.getMessage());
        }
    }
    
    /**
     * 创建默认分类
     */
    private void createDefaultCategories() {
        categoryMap.clear();
        
        // 默认分类
        CategoryInfo defaultCategory = new CategoryInfo(
                DEFAULT_CATEGORY_ID,
                "未分类",
                Color.parseColor("#607D8B"),
                "未分类应用",
                "ic_category_default",
                0,
                true
        );
        
        // 系统分类
        CategoryInfo systemCategory = new CategoryInfo(
                SYSTEM_CATEGORY_ID,
                "系统应用",
                Color.parseColor("#F44336"),
                "系统应用",
                "ic_category_system",
                1,
                true
        );
        
        // 游戏分类
        CategoryInfo gamesCategory = new CategoryInfo(
                GAMES_CATEGORY_ID,
                "游戏",
                Color.parseColor("#4CAF50"),
                "游戏应用",
                "ic_category_games",
                2,
                true
        );
        
        // 工具分类
        CategoryInfo toolsCategory = new CategoryInfo(
                TOOLS_CATEGORY_ID,
                "工具",
                Color.parseColor("#2196F3"),
                "工具应用",
                "ic_category_tools",
                3,
                true
        );
        
        // 社交分类
        CategoryInfo socialCategory = new CategoryInfo(
                SOCIAL_CATEGORY_ID,
                "社交",
                Color.parseColor("#9C27B0"),
                "社交应用",
                "ic_category_social",
                4,
                true
        );
        
        // 添加到映射
        categoryMap.put(defaultCategory.getId(), defaultCategory);
        categoryMap.put(systemCategory.getId(), systemCategory);
        categoryMap.put(gamesCategory.getId(), gamesCategory);
        categoryMap.put(toolsCategory.getId(), toolsCategory);
        categoryMap.put(socialCategory.getId(), socialCategory);
        
        // 保存分类
        saveCategories();
    }
    
    /**
     * 获取所有分类
     */
    public List<CategoryInfo> getAllCategories() {
        List<CategoryInfo> categories = new ArrayList<>(categoryMap.values());
        Collections.sort(categories, (c1, c2) -> Integer.compare(c1.getOrder(), c2.getOrder()));
        return categories;
    }
    
    /**
     * 获取用户分类
     */
    public List<CategoryInfo> getUserCategories() {
        List<CategoryInfo> userCategories = new ArrayList<>();
        for (CategoryInfo category : categoryMap.values()) {
            if (!category.isSystemCategory()) {
                userCategories.add(category);
            }
        }
        Collections.sort(userCategories, (c1, c2) -> Integer.compare(c1.getOrder(), c2.getOrder()));
        return userCategories;
    }
    
    /**
     * 获取系统分类
     */
    public List<CategoryInfo> getSystemCategories() {
        List<CategoryInfo> systemCategories = new ArrayList<>();
        for (CategoryInfo category : categoryMap.values()) {
            if (category.isSystemCategory()) {
                systemCategories.add(category);
            }
        }
        Collections.sort(systemCategories, (c1, c2) -> Integer.compare(c1.getOrder(), c2.getOrder()));
        return systemCategories;
    }
    
    /**
     * 获取分类信息
     */
    public CategoryInfo getCategory(String categoryId) {
        if (categoryId == null) {
            return categoryMap.get(DEFAULT_CATEGORY_ID);
        }
        return categoryMap.get(categoryId);
    }
    
    /**
     * 添加分类
     */
    public boolean addCategory(CategoryInfo category) {
        if (category == null || categoryMap.containsKey(category.getId())) {
            return false;
        }
        
        categoryMap.put(category.getId(), category);
        saveCategories();
        return true;
    }
    
    /**
     * 更新分类
     */
    public boolean updateCategory(CategoryInfo category) {
        if (category == null || !categoryMap.containsKey(category.getId())) {
            return false;
        }
        
        // 系统分类不允许更新
        CategoryInfo oldCategory = categoryMap.get(category.getId());
        if (oldCategory.isSystemCategory()) {
            return false;
        }
        
        categoryMap.put(category.getId(), category);
        saveCategories();
        return true;
    }
    
    /**
     * 删除分类
     */
    public boolean deleteCategory(String categoryId) {
        if (categoryId == null || !categoryMap.containsKey(categoryId)) {
            return false;
        }
        
        // 系统分类不允许删除
        CategoryInfo category = categoryMap.get(categoryId);
        if (category.isSystemCategory()) {
            return false;
        }
        
        // 将该分类中的应用移动到默认分类
        CategoryInfo defaultCategory = categoryMap.get(DEFAULT_CATEGORY_ID);
        if (defaultCategory != null) {
            for (String appId : category.getAppIds()) {
                defaultCategory.addApp(appId);
            }
        }
        
        categoryMap.remove(categoryId);
        saveCategories();
        return true;
    }
    
    /**
     * 添加应用到分类
     */
    public boolean addAppToCategory(String appId, String categoryId) {
        if (appId == null || categoryId == null || !categoryMap.containsKey(categoryId)) {
            return false;
        }
        
        CategoryInfo category = categoryMap.get(categoryId);
        category.addApp(appId);
        saveCategories();
        return true;
    }
    
    /**
     * 从分类中移除应用
     */
    public boolean removeAppFromCategory(String appId, String categoryId) {
        if (appId == null || categoryId == null || !categoryMap.containsKey(categoryId)) {
            return false;
        }
        
        CategoryInfo category = categoryMap.get(categoryId);
        if (!category.containsApp(appId)) {
            return false;
        }
        
        category.removeApp(appId);
        saveCategories();
        return true;
    }
    
    /**
     * 获取应用所属的分类
     */
    public List<CategoryInfo> getCategoriesForApp(String appId) {
        if (appId == null) {
            return Collections.emptyList();
        }
        
        List<CategoryInfo> categories = new ArrayList<>();
        for (CategoryInfo category : categoryMap.values()) {
            if (category.containsApp(appId)) {
                categories.add(category);
            }
        }
        
        // 如果应用没有分类，返回默认分类
        if (categories.isEmpty()) {
            CategoryInfo defaultCategory = categoryMap.get(DEFAULT_CATEGORY_ID);
            if (defaultCategory != null) {
                categories.add(defaultCategory);
            }
        }
        
        return categories;
    }
    
    /**
     * 清空分类中的所有应用
     */
    public boolean clearCategory(String categoryId) {
        if (categoryId == null || !categoryMap.containsKey(categoryId)) {
            return false;
        }
        
        CategoryInfo category = categoryMap.get(categoryId);
        category.clearApps();
        saveCategories();
        return true;
    }
    
    /**
     * 设置分类顺序
     */
    public boolean setCategoryOrder(String categoryId, int order) {
        if (categoryId == null || !categoryMap.containsKey(categoryId)) {
            return false;
        }
        
        CategoryInfo category = categoryMap.get(categoryId);
        category.setOrder(order);
        saveCategories();
        return true;
    }
    
    /**
     * 重排所有分类顺序
     */
    public void reorderCategories(List<CategoryInfo> categories) {
        if (categories == null) {
            return;
        }
        
        for (int i = 0; i < categories.size(); i++) {
            CategoryInfo category = categories.get(i);
            if (categoryMap.containsKey(category.getId())) {
                categoryMap.get(category.getId()).setOrder(i);
            }
        }
        
        saveCategories();
    }
} 