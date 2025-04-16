package com.mobileplatform.creator.viewmodel;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.mobileplatform.creator.data.AppCategoryDao;
import com.mobileplatform.creator.data.AppDatabase;
import com.mobileplatform.creator.data.CategoryDao;
import com.mobileplatform.creator.model.AppCategory;
import com.mobileplatform.creator.model.Category;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 应用分类关联的ViewModel
 */
public class AppCategoryViewModel extends AndroidViewModel {

    private final AppCategoryDao appCategoryDao;
    private final CategoryDao categoryDao;
    private final ExecutorService executorService;

    public AppCategoryViewModel(Application application) {
        super(application);
        AppDatabase db = AppDatabase.getDatabase(application);
        appCategoryDao = db.appCategoryDao();
        categoryDao = db.categoryDao();
        executorService = Executors.newSingleThreadExecutor();
    }

    /**
     * 获取应用所属的所有分类
     */
    public LiveData<List<Category>> getCategoriesForApp(String packageName) {
        return appCategoryDao.getCategoriesForApp(packageName);
    }

    /**
     * 获取分类中的所有应用包名
     */
    public LiveData<List<String>> getAppsInCategory(String categoryId) {
        return appCategoryDao.getAppsInCategory(categoryId);
    }

    /**
     * 添加应用到分类
     */
    public void addAppToCategory(String packageName, String categoryId) {
        executorService.execute(() -> {
            AppCategory appCategory = new AppCategory(packageName, categoryId);
            appCategoryDao.insert(appCategory);
            // 更新分类中的应用计数
            appCategoryDao.updateCategoryAppCount(categoryId);
        });
    }

    /**
     * 批量添加应用到分类
     */
    public void addAppsToCategory(List<String> packageNames, String categoryId) {
        executorService.execute(() -> {
            List<AppCategory> appCategories = new ArrayList<>();
            for (String packageName : packageNames) {
                appCategories.add(new AppCategory(packageName, categoryId));
            }
            appCategoryDao.insertAll(appCategories);
            // 更新分类中的应用计数
            appCategoryDao.updateCategoryAppCount(categoryId);
        });
    }

    /**
     * 从分类中移除应用
     */
    public void removeAppFromCategory(String packageName, String categoryId) {
        executorService.execute(() -> {
            AppCategory appCategory = new AppCategory(packageName, categoryId);
            appCategoryDao.delete(appCategory);
            // 更新分类中的应用计数
            appCategoryDao.updateCategoryAppCount(categoryId);
        });
    }

    /**
     * 删除所有分类中的指定应用
     */
    public void removeAppFromAllCategories(String packageName) {
        executorService.execute(() -> {
            appCategoryDao.deleteAppFromAllCategories(packageName);
            // 更新所有分类的应用计数
            List<Category> categories = categoryDao.getAllCategoriesSync();
            for (Category category : categories) {
                appCategoryDao.updateCategoryAppCount(category.getId());
            }
        });
    }

    /**
     * 检查应用是否在指定分类中
     */
    public void isAppInCategory(String packageName, String categoryId, AppCategoryCallback callback) {
        executorService.execute(() -> {
            int count = appCategoryDao.isAppInCategory(packageName, categoryId);
            callback.onResult(count > 0);
        });
    }

    /**
     * 获取分类中的应用数量
     */
    public void getAppCountInCategory(String categoryId, AppCountCallback callback) {
        executorService.execute(() -> {
            int count = appCategoryDao.getAppCountInCategory(categoryId);
            callback.onCount(count);
        });
    }

    /**
     * 更新所有分类的应用计数
     */
    public void updateAllCategoryAppCounts() {
        executorService.execute(() -> {
            List<Category> categories = categoryDao.getAllCategoriesSync();
            for (Category category : categories) {
                appCategoryDao.updateCategoryAppCount(category.getId());
            }
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executorService.shutdown();
    }

    /**
     * 应用分类回调接口
     */
    public interface AppCategoryCallback {
        void onResult(boolean isInCategory);
    }

    /**
     * 应用计数回调接口
     */
    public interface AppCountCallback {
        void onCount(int count);
    }
} 