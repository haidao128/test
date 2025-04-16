package com.mobileplatform.creator.data.repository;

import android.app.Application;
import androidx.lifecycle.LiveData;

import com.mobileplatform.creator.data.AppDatabase;
import com.mobileplatform.creator.data.LogEntryDao;
import com.mobileplatform.creator.model.LogEntry;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 数据仓库类，封装了对所有数据源（目前主要是Room数据库）的访问。
 * 为 ViewModel 提供统一的数据接口。
 */
public class AppRepository {

    private LogEntryDao logEntryDao;
    // TODO: 添加其他 DAO 成员变量
    private LiveData<List<LogEntry>> allLogs;
    
    // 使用线程池执行数据库操作，避免阻塞主线程
    private static final int NUMBER_OF_THREADS = 4;
    private final ExecutorService databaseWriteExecutor =
            Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    public AppRepository(Application application) {
        AppDatabase db = AppDatabase.getDatabase(application);
        logEntryDao = db.logEntryDao();
        allLogs = logEntryDao.getAllLogs();
        // TODO: 初始化其他 DAO 和 LiveData
    }

    // --- LogEntryDao 操作封装 ---
    public LiveData<List<LogEntry>> getAllLogs() {
        return allLogs;
    }
    
    public LiveData<List<LogEntry>> getLogsByOperationType(String type) {
        return logEntryDao.getLogsByOperationType(type);
    }
    
    public LiveData<List<LogEntry>> getLogsByStatus(String status) {
        return logEntryDao.getLogsByStatus(status);
    }

    public void insertLog(LogEntry logEntry) {
        databaseWriteExecutor.execute(() -> {
            logEntryDao.insert(logEntry);
        });
    }

    public void deleteLog(LogEntry logEntry) {
        databaseWriteExecutor.execute(() -> {
            logEntryDao.delete(logEntry);
        });
    }
    
    public void deleteAllLogs() {
        databaseWriteExecutor.execute(() -> {
            logEntryDao.deleteAllLogs();
        });
    }
    
    // --- 其他 DAO 操作封装 --- 
    // TODO: 添加获取 AppInfo, Category 等数据的封装方法

} 