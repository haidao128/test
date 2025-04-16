package com.mobileplatform.creator.viewmodel;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.mobileplatform.creator.data.AppDatabase;
import com.mobileplatform.creator.data.CategoryDao;
import com.mobileplatform.creator.model.Category;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 分类管理的ViewModel
 */
public class CategoryViewModel extends AndroidViewModel {

    private final CategoryDao categoryDao;
    private final LiveData<List<Category>> allCategories;
    private final ExecutorService executorService;

    public CategoryViewModel(Application application) {
        super(application);
        AppDatabase db = AppDatabase.getDatabase(application);
        categoryDao = db.categoryDao();
        allCategories = categoryDao.getAllCategories();
        executorService = Executors.newSingleThreadExecutor();
    }

    public LiveData<List<Category>> getAllCategories() {
        return allCategories;
    }

    public void insert(Category category) {
        executorService.execute(() -> categoryDao.insert(category));
    }

    public void update(Category category) {
        executorService.execute(() -> categoryDao.update(category));
    }

    public void delete(Category category) {
        executorService.execute(() -> categoryDao.delete(category));
    }

    public void incrementAppCount(String categoryId) {
        executorService.execute(() -> categoryDao.incrementAppCount(categoryId));
    }

    public void decrementAppCount(String categoryId) {
        executorService.execute(() -> categoryDao.decrementAppCount(categoryId));
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executorService.shutdown();
    }
} 