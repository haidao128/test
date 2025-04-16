package com.mobileplatform.creator.ui.log;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.mobileplatform.creator.model.LogEntry;
import com.mobileplatform.creator.data.repository.AppRepository;

import java.util.List;

/**
 * InstallLogActivity 的 ViewModel。
 * 负责准备和管理日志数据。
 */
public class InstallLogViewModel extends AndroidViewModel {

    private AppRepository repository;
    private LiveData<List<LogEntry>> allLogs;

    public InstallLogViewModel(@NonNull Application application) {
        super(application);
        repository = new AppRepository(application);
        allLogs = repository.getAllLogs();
    }

    LiveData<List<LogEntry>> getAllLogs() {
        return allLogs;
    }

    // 根据类型获取日志
    LiveData<List<LogEntry>> getLogsByType(String type) {
        if (type == null || type.equalsIgnoreCase("ALL")) {
            return allLogs;
        }
        return repository.getLogsByOperationType(type);
    }
    
    // 根据状态获取日志
     LiveData<List<LogEntry>> getLogsByStatus(String status) {
         if (status == null || status.isEmpty()) {
             return allLogs; 
         }
         return repository.getLogsByStatus(status);
     }

    // 插入日志 (可选，也可以直接在需要记录日志的地方调用 Repository)
    public void insert(LogEntry logEntry) {
        repository.insertLog(logEntry);
    }

    // 删除日志 (可选)
    public void delete(LogEntry logEntry) {
        repository.deleteLog(logEntry);
    }
    
    // 清空日志 (可选)
    public void deleteAllLogs() {
        repository.deleteAllLogs();
    }
} 