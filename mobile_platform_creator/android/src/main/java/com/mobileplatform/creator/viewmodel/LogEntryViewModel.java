package com.mobileplatform.creator.viewmodel;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.mobileplatform.creator.data.AppDatabase;
import com.mobileplatform.creator.data.LogEntryDao;
import com.mobileplatform.creator.model.LogEntry;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 日志条目管理的ViewModel
 */
public class LogEntryViewModel extends AndroidViewModel {

    private final LogEntryDao logEntryDao;
    private final LiveData<List<LogEntry>> allLogs;
    private final ExecutorService executorService;

    public LogEntryViewModel(Application application) {
        super(application);
        AppDatabase db = AppDatabase.getDatabase(application);
        logEntryDao = db.logEntryDao();
        allLogs = logEntryDao.getAllLogs();
        executorService = Executors.newSingleThreadExecutor();
    }

    public LiveData<List<LogEntry>> getAllLogs() {
        return allLogs;
    }

    public LiveData<List<LogEntry>> getLogsByOperationType(String operationType) {
        return logEntryDao.getLogsByOperationType(operationType);
    }

    public LiveData<List<LogEntry>> getLogsByStatus(String status) {
        return logEntryDao.getLogsByStatus(status);
    }

    public LiveData<List<LogEntry>> getLogsByPackage(String packageName) {
        return logEntryDao.getLogsByPackage(packageName);
    }

    public void insert(LogEntry logEntry) {
        executorService.execute(() -> logEntryDao.insert(logEntry));
    }

    public void update(LogEntry logEntry) {
        executorService.execute(() -> logEntryDao.update(logEntry));
    }

    public void delete(LogEntry logEntry) {
        executorService.execute(() -> logEntryDao.delete(logEntry));
    }

    public void deleteAllLogs() {
        executorService.execute(() -> logEntryDao.deleteAllLogs());
    }

    public void deleteLogsByPackage(String packageName) {
        executorService.execute(() -> logEntryDao.deleteLogsByPackage(packageName));
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executorService.shutdown();
    }
} 