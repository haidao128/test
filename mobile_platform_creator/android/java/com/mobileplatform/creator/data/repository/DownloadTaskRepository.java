package com.mobileplatform.creator.data.repository;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.mobileplatform.creator.data.AppDatabase;
import com.mobileplatform.creator.data.dao.DownloadTaskDao;
import com.mobileplatform.creator.data.entity.DownloadTaskEntity;
import com.mobileplatform.creator.data.model.DownloadTask;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * 下载任务仓库
 * 负责下载任务的数据操作
 */
public class DownloadTaskRepository {
    private static final String TAG = "DownloadTaskRepository";
    
    // 单例实例
    private static DownloadTaskRepository instance;
    
    // 下载任务DAO
    private final DownloadTaskDao downloadTaskDao;
    
    // 线程池
    private final Executor executor;
    
    /**
     * 获取单例实例
     * 
     * @param context 上下文
     * @return 下载任务仓库
     */
    public static synchronized DownloadTaskRepository getInstance(Context context) {
        if (instance == null) {
            instance = new DownloadTaskRepository(context.getApplicationContext());
        }
        return instance;
    }
    
    /**
     * 构造函数
     * 
     * @param context 上下文
     */
    private DownloadTaskRepository(Context context) {
        AppDatabase database = AppDatabase.getInstance(context);
        this.downloadTaskDao = database.downloadTaskDao();
        this.executor = Executors.newSingleThreadExecutor();
    }
    
    /**
     * 保存下载任务
     * 
     * @param task 下载任务
     */
    public void saveTask(DownloadTask task) {
        if (task == null) {
            return;
        }
        
        executor.execute(() -> {
            try {
                DownloadTaskEntity entity = new DownloadTaskEntity(task);
                long id = downloadTaskDao.insert(entity);
                Log.d(TAG, "保存下载任务: " + task.getId() + ", 结果: " + id);
            } catch (Exception e) {
                Log.e(TAG, "保存下载任务失败", e);
            }
        });
    }
    
    /**
     * 更新下载任务
     * 
     * @param task 下载任务
     */
    public void updateTask(DownloadTask task) {
        if (task == null) {
            return;
        }
        
        executor.execute(() -> {
            try {
                DownloadTaskEntity entity = new DownloadTaskEntity(task);
                int rows = downloadTaskDao.update(entity);
                Log.d(TAG, "更新下载任务: " + task.getId() + ", 影响行数: " + rows);
            } catch (Exception e) {
                Log.e(TAG, "更新下载任务失败", e);
            }
        });
    }
    
    /**
     * 删除下载任务
     * 
     * @param task 下载任务
     */
    public void deleteTask(DownloadTask task) {
        if (task == null) {
            return;
        }
        
        executor.execute(() -> {
            try {
                DownloadTaskEntity entity = new DownloadTaskEntity(task);
                int rows = downloadTaskDao.delete(entity);
                Log.d(TAG, "删除下载任务: " + task.getId() + ", 影响行数: " + rows);
            } catch (Exception e) {
                Log.e(TAG, "删除下载任务失败", e);
            }
        });
    }
    
    /**
     * 获取所有下载任务
     * 
     * @param callback 回调
     */
    public void getAllTasks(TaskListCallback callback) {
        executor.execute(() -> {
            try {
                List<DownloadTaskEntity> entities = downloadTaskDao.getAllTasks();
                List<DownloadTask> tasks = entitiesToTasks(entities);
                if (callback != null) {
                    callback.onResult(tasks);
                }
            } catch (Exception e) {
                Log.e(TAG, "获取所有下载任务失败", e);
                if (callback != null) {
                    callback.onResult(new ArrayList<>());
                }
            }
        });
    }
    
    /**
     * 获取未完成的下载任务
     * 
     * @param callback 回调
     */
    public void getUnfinishedTasks(TaskListCallback callback) {
        executor.execute(() -> {
            try {
                List<DownloadTaskEntity> entities = downloadTaskDao.getUnfinishedTasks();
                List<DownloadTask> tasks = entitiesToTasks(entities);
                if (callback != null) {
                    callback.onResult(tasks);
                }
            } catch (Exception e) {
                Log.e(TAG, "获取未完成的下载任务失败", e);
                if (callback != null) {
                    callback.onResult(new ArrayList<>());
                }
            }
        });
    }
    
    /**
     * 获取正在下载和等待中的任务
     * 
     * @param callback 回调
     */
    public void getRunningAndPendingTasks(TaskListCallback callback) {
        executor.execute(() -> {
            try {
                List<DownloadTaskEntity> entities = downloadTaskDao.getRunningAndPendingTasks();
                List<DownloadTask> tasks = entitiesToTasks(entities);
                if (callback != null) {
                    callback.onResult(tasks);
                }
            } catch (Exception e) {
                Log.e(TAG, "获取正在下载和等待中的任务失败", e);
                if (callback != null) {
                    callback.onResult(new ArrayList<>());
                }
            }
        });
    }
    
    /**
     * 获取已完成的任务
     * 
     * @param callback 回调
     */
    public void getCompletedTasks(TaskListCallback callback) {
        executor.execute(() -> {
            try {
                List<DownloadTaskEntity> entities = downloadTaskDao.getCompletedTasks();
                List<DownloadTask> tasks = entitiesToTasks(entities);
                if (callback != null) {
                    callback.onResult(tasks);
                }
            } catch (Exception e) {
                Log.e(TAG, "获取已完成的任务失败", e);
                if (callback != null) {
                    callback.onResult(new ArrayList<>());
                }
            }
        });
    }
    
    /**
     * 获取失败和取消的任务
     * 
     * @param callback 回调
     */
    public void getFailedAndCanceledTasks(TaskListCallback callback) {
        executor.execute(() -> {
            try {
                List<DownloadTaskEntity> entities = downloadTaskDao.getFailedAndCanceledTasks();
                List<DownloadTask> tasks = entitiesToTasks(entities);
                if (callback != null) {
                    callback.onResult(tasks);
                }
            } catch (Exception e) {
                Log.e(TAG, "获取失败和取消的任务失败", e);
                if (callback != null) {
                    callback.onResult(new ArrayList<>());
                }
            }
        });
    }
    
    /**
     * 通过URL查找任务
     * 
     * @param url 下载URL
     * @param callback 回调
     */
    public void findTaskByUrl(String url, TaskCallback callback) {
        if (url == null || url.isEmpty()) {
            if (callback != null) {
                callback.onResult(null);
            }
            return;
        }
        
        executor.execute(() -> {
            try {
                DownloadTaskEntity entity = downloadTaskDao.findTaskByUrl(url);
                DownloadTask task = entity != null ? entity.toDownloadTask() : null;
                if (callback != null) {
                    callback.onResult(task);
                }
            } catch (Exception e) {
                Log.e(TAG, "通过URL查找任务失败", e);
                if (callback != null) {
                    callback.onResult(null);
                }
            }
        });
    }
    
    /**
     * 清除所有下载任务
     */
    public void clearAllTasks() {
        executor.execute(() -> {
            try {
                int rows = downloadTaskDao.clearAll();
                Log.d(TAG, "清除所有下载任务, 影响行数: " + rows);
            } catch (Exception e) {
                Log.e(TAG, "清除所有下载任务失败", e);
            }
        });
    }
    
    /**
     * 清除已完成的下载任务
     */
    public void clearCompletedTasks() {
        executor.execute(() -> {
            try {
                int rows = downloadTaskDao.clearCompletedTasks();
                Log.d(TAG, "清除已完成的下载任务, 影响行数: " + rows);
            } catch (Exception e) {
                Log.e(TAG, "清除已完成的下载任务失败", e);
            }
        });
    }
    
    /**
     * 清除失败和取消的下载任务
     */
    public void clearFailedAndCanceledTasks() {
        executor.execute(() -> {
            try {
                int rows = downloadTaskDao.clearFailedAndCanceledTasks();
                Log.d(TAG, "清除失败和取消的下载任务, 影响行数: " + rows);
            } catch (Exception e) {
                Log.e(TAG, "清除失败和取消的下载任务失败", e);
            }
        });
    }
    
    /**
     * 将实体列表转换为任务列表
     * 
     * @param entities 实体列表
     * @return 任务列表
     */
    private List<DownloadTask> entitiesToTasks(List<DownloadTaskEntity> entities) {
        List<DownloadTask> tasks = new ArrayList<>();
        if (entities != null) {
            for (DownloadTaskEntity entity : entities) {
                tasks.add(entity.toDownloadTask());
            }
        }
        return tasks;
    }
    
    /**
     * 下载任务列表回调接口
     */
    public interface TaskListCallback {
        /**
         * 结果回调
         * 
         * @param tasks 下载任务列表
         */
        void onResult(List<DownloadTask> tasks);
    }
    
    /**
     * 下载任务回调接口
     */
    public interface TaskCallback {
        /**
         * 结果回调
         * 
         * @param task 下载任务
         */
        void onResult(DownloadTask task);
    }
} 