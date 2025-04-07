package com.mobileplatform.creator.data.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

/**
 * 安装日志数据访问对象
 */
@Dao
public interface InstallLogDao {
    
    /**
     * 插入日志
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(InstallLogEntity log);
    
    /**
     * 批量插入日志
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<InstallLogEntity> logs);
    
    /**
     * 更新日志
     */
    @Update
    void update(InstallLogEntity log);
    
    /**
     * 删除日志
     */
    @Delete
    void delete(InstallLogEntity log);
    
    /**
     * 根据ID删除日志
     */
    @Query("DELETE FROM install_logs WHERE id = :id")
    void deleteById(String id);
    
    /**
     * 清空所有日志
     */
    @Query("DELETE FROM install_logs")
    void deleteAll();
    
    /**
     * 根据ID获取日志
     */
    @Query("SELECT * FROM install_logs WHERE id = :id")
    InstallLogEntity getById(String id);
    
    /**
     * 获取所有日志
     */
    @Query("SELECT * FROM install_logs ORDER BY operationTime DESC")
    List<InstallLogEntity> getAll();
    
    /**
     * 获取所有日志（LiveData）
     */
    @Query("SELECT * FROM install_logs ORDER BY operationTime DESC")
    LiveData<List<InstallLogEntity>> getAllLive();
    
    /**
     * 获取特定应用的日志
     */
    @Query("SELECT * FROM install_logs WHERE appId = :appId ORDER BY operationTime DESC")
    List<InstallLogEntity> getByAppId(String appId);
    
    /**
     * 获取特定应用的日志（LiveData）
     */
    @Query("SELECT * FROM install_logs WHERE appId = :appId ORDER BY operationTime DESC")
    LiveData<List<InstallLogEntity>> getByAppIdLive(String appId);
    
    /**
     * 获取特定操作类型的日志
     */
    @Query("SELECT * FROM install_logs WHERE operationType = :operationType ORDER BY operationTime DESC")
    List<InstallLogEntity> getByOperationType(int operationType);
    
    /**
     * 获取成功的日志
     */
    @Query("SELECT * FROM install_logs WHERE success = 1 ORDER BY operationTime DESC")
    List<InstallLogEntity> getSuccessLogs();
    
    /**
     * 获取失败的日志
     */
    @Query("SELECT * FROM install_logs WHERE success = 0 ORDER BY operationTime DESC")
    List<InstallLogEntity> getFailureLogs();
    
    /**
     * 获取特定时间范围内的日志
     */
    @Query("SELECT * FROM install_logs WHERE operationTime BETWEEN :startTime AND :endTime ORDER BY operationTime DESC")
    List<InstallLogEntity> getByTimeRange(long startTime, long endTime);
    
    /**
     * 获取日志数量
     */
    @Query("SELECT COUNT(*) FROM install_logs")
    int getCount();
    
    /**
     * 分页获取所有日志
     */
    @Query("SELECT * FROM install_logs ORDER BY operationTime DESC LIMIT :limit OFFSET :offset")
    List<InstallLogEntity> getPagedLogs(int offset, int limit);
    
    /**
     * 分页获取特定操作类型的日志
     */
    @Query("SELECT * FROM install_logs WHERE operationType = :operationType ORDER BY operationTime DESC LIMIT :limit OFFSET :offset")
    List<InstallLogEntity> getPagedLogsByType(int operationType, int offset, int limit);
    
    /**
     * 分页获取失败的日志
     */
    @Query("SELECT * FROM install_logs WHERE success = 0 ORDER BY operationTime DESC LIMIT :limit OFFSET :offset")
    List<InstallLogEntity> getPagedFailureLogs(int offset, int limit);
} 