package com.mobileplatform.creator.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.mobileplatform.creator.model.AppCategory;
import com.mobileplatform.creator.model.Category;

import java.util.List;

/**
 * 应用分类关联表的数据访问对象
 */
@Dao
public interface AppCategoryDao {
    
    /**
     * 添加应用到分类
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(AppCategory appCategory);
    
    /**
     * 批量添加应用到分类
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<AppCategory> appCategories);
    
    /**
     * 从分类中移除应用
     */
    @Delete
    void delete(AppCategory appCategory);
    
    /**
     * 删除所有分类中的指定应用
     */
    @Query("DELETE FROM app_categories WHERE package_name = :packageName")
    void deleteAppFromAllCategories(String packageName);
    
    /**
     * 删除指定分类中的所有应用
     */
    @Query("DELETE FROM app_categories WHERE category_id = :categoryId")
    void deleteAllAppsFromCategory(String categoryId);
    
    /**
     * 检查应用是否在指定分类中
     */
    @Query("SELECT COUNT(*) FROM app_categories WHERE package_name = :packageName AND category_id = :categoryId")
    int isAppInCategory(String packageName, String categoryId);
    
    /**
     * 获取指定应用所属的所有分类
     */
    @Query("SELECT c.* FROM categories c INNER JOIN app_categories ac ON c.id = ac.category_id WHERE ac.package_name = :packageName")
    LiveData<List<Category>> getCategoriesForApp(String packageName);
    
    /**
     * 获取指定分类中的所有应用包名
     */
    @Query("SELECT package_name FROM app_categories WHERE category_id = :categoryId")
    LiveData<List<String>> getAppsInCategory(String categoryId);
    
    /**
     * 获取指定分类中的应用数量
     */
    @Query("SELECT COUNT(*) FROM app_categories WHERE category_id = :categoryId")
    int getAppCountInCategory(String categoryId);
    
    /**
     * 更新分类中的应用数量
     */
    @Query("UPDATE categories SET app_count = (SELECT COUNT(*) FROM app_categories WHERE category_id = :categoryId) WHERE id = :categoryId")
    void updateCategoryAppCount(String categoryId);
} 