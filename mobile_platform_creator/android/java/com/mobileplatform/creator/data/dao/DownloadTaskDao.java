package com.mobileplatform.creator.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.mobileplatform.creator.data.entity.DownloadTaskEntity;

import java.util.List;

/**
 * 下载任务数据访问对象接口
 * 用于数据库对下载任务的增删改查操作
 */
@Dao
public interface DownloadTaskDao {
    
    /**
     * 插入下载任务
     * 
     * @param task 下载任务
     * @return 插入的任务ID
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(DownloadTaskEntity task);
    
    /**
     * 更新下载任务
     * 
     * @param task 下载任务
     * @return 更新的行数
     */
    @Update
    int update(DownloadTaskEntity task);
    
    /**
     * 删除下载任务
     * 
     * @param task 下载任务
     * @return 删除的行数
     */
    @Delete
    int delete(DownloadTaskEntity task);
    
    /**
     * 通过ID获取下载任务
     * 
     * @param id 任务ID
     * @return 下载任务
     */
    @Query("SELECT * FROM download_tasks WHERE id = :id")
    DownloadTaskEntity getTaskById(String id);
    
    /**
     * 获取所有下载任务
     * 
     * @return 下载任务列表
     */
    @Query("SELECT * FROM download_tasks ORDER BY createTime DESC")
    List<DownloadTaskEntity> getAllTasks();
    
    /**
     * 获取所有下载任务（LiveData形式）
     * 
     * @return 下载任务LiveData列表
     */
    @Query("SELECT * FROM download_tasks ORDER BY createTime DESC")
    LiveData<List<DownloadTaskEntity>> getAllTasksLive();
    
    /**
     * 获取未完成的下载任务
     * 
     * @return 未完成的下载任务列表
     */
    @Query("SELECT * FROM download_tasks WHERE status IN (0, 1, 2) ORDER BY createTime ASC")
    List<DownloadTaskEntity> getUnfinishedTasks();
    
    /**
     * 获取正在下载和等待中的任务
     * 
     * @return 正在下载和等待中的任务列表
     */
    @Query("SELECT * FROM download_tasks WHERE status IN (0, 1, 2) ORDER BY createTime ASC")
    List<DownloadTaskEntity> getRunningAndPendingTasks();
    
    /**
     * 获取已完成的任务
     * 
     * @return 已完成的任务列表
     */
    @Query("SELECT * FROM download_tasks WHERE status = 3 ORDER BY createTime DESC")
    List<DownloadTaskEntity> getCompletedTasks();
    
    /**
     * 获取失败和取消的任务
     * 
     * @return 失败和取消的任务列表
     */
    @Query("SELECT * FROM download_tasks WHERE status IN (4, 5) ORDER BY createTime DESC")
    List<DownloadTaskEntity> getFailedAndCanceledTasks();
    
    /**
     * 通过URL查找任务
     * 
     * @param url 下载URL
     * @return 下载任务
     */
    @Query("SELECT * FROM download_tasks WHERE url = :url LIMIT 1")
    DownloadTaskEntity findTaskByUrl(String url);
    
    /**
     * 清除所有下载任务
     * 
     * @return 删除的行数
     */
    @Query("DELETE FROM download_tasks")
    int clearAll();
    
    /**
     * 删除已完成的下载任务
     * 
     * @return 删除的行数
     */
    @Query("DELETE FROM download_tasks WHERE status = 3")
    int clearCompletedTasks();
    
    /**
     * 删除失败的下载任务
     * 
     * @return 删除的行数
     */
    @Query("DELETE FROM download_tasks WHERE status IN (4, 5)")
    int clearFailedAndCanceledTasks();
} 