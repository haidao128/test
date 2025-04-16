package com.mobileplatform.creator.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.mobileplatform.creator.model.Category;

import java.util.List;

/**
 * 分类数据访问对象
 */
@Dao
public interface CategoryDao {
    @Insert
    void insert(Category category);

    @Update
    void update(Category category);

    @Delete
    void delete(Category category);

    @Query("SELECT * FROM categories ORDER BY name ASC")
    LiveData<List<Category>> getAllCategories();

    @Query("SELECT * FROM categories ORDER BY name ASC")
    List<Category> getAllCategoriesSync();

    @Query("SELECT * FROM categories WHERE id = :categoryId")
    LiveData<Category> getCategoryById(String categoryId);

    @Query("SELECT COUNT(*) FROM categories")
    int getCategoryCount();

    @Query("UPDATE categories SET app_count = app_count + 1 WHERE id = :categoryId")
    void incrementAppCount(String categoryId);

    @Query("UPDATE categories SET app_count = app_count - 1 WHERE id = :categoryId AND app_count > 0")
    void decrementAppCount(String categoryId);
} 