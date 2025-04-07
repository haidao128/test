package com.mobileplatform.creator.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.mobileplatform.creator.data.entity.LogEntry;

import java.util.List;

/**
 * 安装日志的数据访问对象 (DAO)。
 * 定义了对 install_logs 表的操作。
 */
@Dao
public interface LogDao {

    // 插入单条日志
    @Insert
    void insert(LogEntry logEntry);

    // 插入多条日志
    @Insert
    void insertAll(LogEntry... logEntries);

    // 更新日志条目
    @Update
    void update(LogEntry logEntry);

    // 删除单条日志
    @Delete
    void delete(LogEntry logEntry);

    // 删除所有日志
    @Query("DELETE FROM install_logs")
    void deleteAllLogs();

    // 查询所有日志，按时间戳降序排列
    @Query("SELECT * FROM install_logs ORDER BY timestamp DESC")
    LiveData<List<LogEntry>> getAllLogs();

    // 根据操作类型查询日志
    @Query("SELECT * FROM install_logs WHERE operationType = :operationType ORDER BY timestamp DESC")
    LiveData<List<LogEntry>> getLogsByOperationType(String operationType);
    
    // 根据操作状态查询日志 (例如查询所有失败的日志)
    @Query("SELECT * FROM install_logs WHERE status = :status ORDER BY timestamp DESC")
    LiveData<List<LogEntry>> getLogsByStatus(String status); 

    // 根据包名查询日志
    @Query("SELECT * FROM install_logs WHERE packageName = :packageName ORDER BY timestamp DESC")
    LiveData<List<LogEntry>> getLogsByPackageName(String packageName);

    // 根据 ID 查询单条日志（可能用于详情页）
    @Query("SELECT * FROM install_logs WHERE id = :logId")
    LiveData<LogEntry> getLogById(int logId);
} 