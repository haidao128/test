package com.mobileplatform.creator.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.mobileplatform.creator.model.LogEntry;

import java.util.List;

/**
 * 日志条目数据访问对象
 */
@Dao
public interface LogEntryDao {
    @Insert
    void insert(LogEntry logEntry);

    @Update
    void update(LogEntry logEntry);

    @Delete
    void delete(LogEntry logEntry);

    @Query("SELECT * FROM log_entries ORDER BY timestamp DESC")
    LiveData<List<LogEntry>> getAllLogs();

    @Query("SELECT * FROM log_entries WHERE package_name = :packageName ORDER BY timestamp DESC")
    LiveData<List<LogEntry>> getLogsByPackage(String packageName);

    @Query("SELECT * FROM log_entries WHERE operation_type = :operationType ORDER BY timestamp DESC")
    LiveData<List<LogEntry>> getLogsByOperationType(String operationType);

    @Query("SELECT * FROM log_entries WHERE status = :status ORDER BY timestamp DESC")
    LiveData<List<LogEntry>> getLogsByStatus(String status);

    @Query("DELETE FROM log_entries")
    void deleteAllLogs();

    @Query("DELETE FROM log_entries WHERE package_name = :packageName")
    void deleteLogsByPackage(String packageName);
} 