package com.mobileplatform.creator.data.repository;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import com.mobileplatform.creator.data.AppDatabase;
import com.mobileplatform.creator.data.db.InstallLogDao;
import com.mobileplatform.creator.data.db.InstallLogEntity;
import com.mobileplatform.creator.data.model.InstallLogEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * 安装日志仓库类
 * 提供对安装日志的操作接口
 */
public class InstallLogRepository {
    private static final String TAG = "InstallLogRepository";
    
    // 单例实例
    private static InstallLogRepository instance;
    
    // 日志DAO
    private final InstallLogDao installLogDao;
    
    // 线程池
    private final Executor executor;
    
    /**
     * 获取单例实例
     * 
     * @param context 上下文
     * @return 安装日志仓库
     */
    public static synchronized InstallLogRepository getInstance(Context context) {
        if (instance == null) {
            instance = new InstallLogRepository(context.getApplicationContext());
        }
        return instance;
    }
    
    /**
     * 构造函数
     * 
     * @param context 上下文
     */
    private InstallLogRepository(Context context) {
        AppDatabase database = AppDatabase.getInstance(context);
        this.installLogDao = database.installLogDao();
        this.executor = Executors.newSingleThreadExecutor();
    }
    
    /**
     * 添加应用安装成功日志
     *
     * @param appId 应用ID
     * @param appName 应用名称
     * @param packageName 包名
     * @param version 版本
     * @param operationType 操作类型（安装/更新/卸载）
     */
    public void addSuccessLog(String appId, String appName, String packageName, String version, 
                          InstallLogEntry.OperationType operationType) {
        InstallLogEntry logEntry = InstallLogEntry.createSuccessLog(appId, appName, packageName, version, operationType);
        addLog(logEntry);
    }
    
    /**
     * 添加应用安装失败日志
     *
     * @param appId 应用ID
     * @param appName 应用名称
     * @param packageName 包名
     * @param version 版本
     * @param operationType 操作类型（安装/更新/卸载）
     * @param errorMessage 错误信息
     */
    public void addFailureLog(String appId, String appName, String packageName, String version, 
                           InstallLogEntry.OperationType operationType, String errorMessage) {
        InstallLogEntry logEntry = InstallLogEntry.createFailureLog(appId, appName, packageName, version, 
                                                              operationType, errorMessage);
        addLog(logEntry);
    }
    
    /**
     * 添加日志
     *
     * @param logEntry 日志条目
     */
    public void addLog(final InstallLogEntry logEntry) {
        if (logEntry.getId() == null || logEntry.getId().isEmpty()) {
            logEntry.setId(UUID.randomUUID().toString());
        }
        
        executor.execute(() -> {
            try {
                installLogDao.insert(InstallLogEntity.fromLogEntry(logEntry));
                Log.d(TAG, "安装日志添加成功: " + logEntry.getAppName() + ", 操作: " + 
                      logEntry.getOperationType().getDisplayName());
            } catch (Exception e) {
                Log.e(TAG, "安装日志添加失败", e);
            }
        });
    }
    
    /**
     * 删除日志
     *
     * @param logEntry 日志条目
     */
    public void deleteLog(final InstallLogEntry logEntry) {
        executor.execute(() -> {
            try {
                installLogDao.deleteById(logEntry.getId());
                Log.d(TAG, "安装日志删除成功: " + logEntry.getId());
            } catch (Exception e) {
                Log.e(TAG, "安装日志删除失败", e);
            }
        });
    }
    
    /**
     * 清空所有日志
     */
    public void clearAllLogs() {
        executor.execute(() -> {
            try {
                installLogDao.deleteAll();
                Log.d(TAG, "所有安装日志已清空");
            } catch (Exception e) {
                Log.e(TAG, "安装日志清空失败", e);
            }
        });
    }
    
    /**
     * 获取所有日志
     *
     * @param callback 回调
     */
    public void getAllLogs(final LogsCallback callback) {
        executor.execute(() -> {
            try {
                List<InstallLogEntity> entities = installLogDao.getAll();
                List<InstallLogEntry> logs = convertToLogEntries(entities);
                callback.onLogsLoaded(logs);
            } catch (Exception e) {
                Log.e(TAG, "获取安装日志失败", e);
                callback.onDataNotAvailable(e.getMessage());
            }
        });
    }
    
    /**
     * 获取所有日志（LiveData形式）
     *
     * @return 日志LiveData
     */
    public LiveData<List<InstallLogEntry>> getAllLogsLive() {
        return Transformations.map(installLogDao.getAllLive(), this::convertToLogEntries);
    }
    
    /**
     * 获取指定应用的日志
     *
     * @param appId 应用ID
     * @param callback 回调
     */
    public void getLogsByAppId(final String appId, final LogsCallback callback) {
        executor.execute(() -> {
            try {
                List<InstallLogEntity> entities = installLogDao.getByAppId(appId);
                List<InstallLogEntry> logs = convertToLogEntries(entities);
                callback.onLogsLoaded(logs);
            } catch (Exception e) {
                Log.e(TAG, "获取应用安装日志失败", e);
                callback.onDataNotAvailable(e.getMessage());
            }
        });
    }
    
    /**
     * 获取指定应用的日志（LiveData形式）
     *
     * @param appId 应用ID
     * @return 日志LiveData
     */
    public LiveData<List<InstallLogEntry>> getLogsByAppIdLive(String appId) {
        return Transformations.map(installLogDao.getByAppIdLive(appId), this::convertToLogEntries);
    }
    
    /**
     * 获取指定操作类型的日志
     *
     * @param operationType 操作类型
     * @param callback 回调
     */
    public void getLogsByOperationType(final InstallLogEntry.OperationType operationType, 
                                     final LogsCallback callback) {
        executor.execute(() -> {
            try {
                List<InstallLogEntity> entities = installLogDao.getByOperationType(operationType.ordinal());
                List<InstallLogEntry> logs = convertToLogEntries(entities);
                callback.onLogsLoaded(logs);
            } catch (Exception e) {
                Log.e(TAG, "获取操作类型日志失败", e);
                callback.onDataNotAvailable(e.getMessage());
            }
        });
    }
    
    /**
     * 获取成功的日志
     *
     * @param callback 回调
     */
    public void getSuccessLogs(final LogsCallback callback) {
        executor.execute(() -> {
            try {
                List<InstallLogEntity> entities = installLogDao.getSuccessLogs();
                List<InstallLogEntry> logs = convertToLogEntries(entities);
                callback.onLogsLoaded(logs);
            } catch (Exception e) {
                Log.e(TAG, "获取成功日志失败", e);
                callback.onDataNotAvailable(e.getMessage());
            }
        });
    }
    
    /**
     * 获取失败的日志
     *
     * @param callback 回调
     */
    public void getFailureLogs(final LogsCallback callback) {
        executor.execute(() -> {
            try {
                List<InstallLogEntity> entities = installLogDao.getFailureLogs();
                List<InstallLogEntry> logs = convertToLogEntries(entities);
                callback.onLogsLoaded(logs);
            } catch (Exception e) {
                Log.e(TAG, "获取失败日志失败", e);
                callback.onDataNotAvailable(e.getMessage());
            }
        });
    }
    
    /**
     * 获取特定时间范围的日志
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param callback 回调
     */
    public void getLogsByTimeRange(final long startTime, final long endTime, final LogsCallback callback) {
        executor.execute(() -> {
            try {
                List<InstallLogEntity> entities = installLogDao.getByTimeRange(startTime, endTime);
                List<InstallLogEntry> logs = convertToLogEntries(entities);
                callback.onLogsLoaded(logs);
            } catch (Exception e) {
                Log.e(TAG, "获取时间范围日志失败", e);
                callback.onDataNotAvailable(e.getMessage());
            }
        });
    }
    
    /**
     * 分页获取所有日志
     *
     * @param offset 偏移量
     * @param limit 限制数量
     * @param callback 回调
     */
    public void getPagedLogs(final int offset, final int limit, final LogsCallback callback) {
        executor.execute(() -> {
            try {
                List<InstallLogEntity> entities = installLogDao.getPagedLogs(offset, limit);
                List<InstallLogEntry> logs = convertToLogEntries(entities);
                callback.onLogsLoaded(logs);
            } catch (Exception e) {
                Log.e(TAG, "分页获取日志失败", e);
                callback.onDataNotAvailable(e.getMessage());
            }
        });
    }
    
    /**
     * 分页获取指定操作类型的日志
     *
     * @param operationType 操作类型
     * @param offset 偏移量
     * @param limit 限制数量
     * @param callback 回调
     */
    public void getPagedLogsByType(final InstallLogEntry.OperationType operationType, 
                                  final int offset, final int limit, 
                                  final LogsCallback callback) {
        executor.execute(() -> {
            try {
                List<InstallLogEntity> entities = 
                    installLogDao.getPagedLogsByType(operationType.ordinal(), offset, limit);
                List<InstallLogEntry> logs = convertToLogEntries(entities);
                callback.onLogsLoaded(logs);
            } catch (Exception e) {
                Log.e(TAG, "分页获取指定类型日志失败", e);
                callback.onDataNotAvailable(e.getMessage());
            }
        });
    }
    
    /**
     * 分页获取失败的日志
     *
     * @param offset 偏移量
     * @param limit 限制数量
     * @param callback 回调
     */
    public void getPagedFailureLogs(final int offset, final int limit, final LogsCallback callback) {
        executor.execute(() -> {
            try {
                List<InstallLogEntity> entities = installLogDao.getPagedFailureLogs(offset, limit);
                List<InstallLogEntry> logs = convertToLogEntries(entities);
                callback.onLogsLoaded(logs);
            } catch (Exception e) {
                Log.e(TAG, "分页获取失败日志失败", e);
                callback.onDataNotAvailable(e.getMessage());
            }
        });
    }
    
    /**
     * 将实体列表转换为日志条目列表
     *
     * @param entities 实体列表
     * @return 日志条目列表
     */
    private List<InstallLogEntry> convertToLogEntries(List<InstallLogEntity> entities) {
        List<InstallLogEntry> result = new ArrayList<>();
        if (entities != null) {
            for (InstallLogEntity entity : entities) {
                result.add(entity.toLogEntry());
            }
        }
        return result;
    }
    
    /**
     * 日志加载回调接口
     */
    public interface LogsCallback {
        void onLogsLoaded(List<InstallLogEntry> logs);
        void onDataNotAvailable(String errorMessage);
    }
} 