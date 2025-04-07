package com.mobileplatform.creator.update;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import com.mobileplatform.creator.data.model.AppInfo;
import com.mobileplatform.creator.data.repository.AppRepository;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 更新检查服务
 * 
 * 定期检查已安装应用是否有新版本可用
 */
public class UpdateCheckService extends Service {
    private static final String TAG = "UpdateCheckService";
    
    // 检查间隔（毫秒）
    private static final long CHECK_INTERVAL = TimeUnit.HOURS.toMillis(12);
    
    // 更新检查器
    private UpdateChecker updateChecker;
    
    // 更新通知管理器
    private UpdateNotificationManager notificationManager;
    
    // 应用仓库
    private AppRepository appRepository;
    
    // 主线程处理器
    private Handler mainHandler;
    
    // 更新检查任务
    private Runnable checkTask;
    
    // 更新检查监听器
    private final UpdateChecker.UpdateCheckListener checkListener = new UpdateChecker.UpdateCheckListener() {
        @Override
        public void onUpdateAvailable(AppInfo appInfo, StoreAppInfo updateInfo) {
            // 显示更新通知
            notificationManager.showUpdateNotification(appInfo, updateInfo);
        }
        
        @Override
        public void onNoUpdate(AppInfo appInfo) {
            // 无更新，不做处理
        }
        
        @Override
        public void onError(String errorMessage) {
            Log.e(TAG, "更新检查错误: " + errorMessage);
        }
        
        @Override
        public void onProgress(int checked, int total) {
            // 更新进度，不做处理
        }
        
        @Override
        public void onBatchComplete() {
            // 批量检查完成，不做处理
        }
    };
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        // 初始化组件
        updateChecker = UpdateChecker.getInstance(this);
        notificationManager = UpdateNotificationManager.getInstance(this);
        appRepository = AppRepository.getInstance(this);
        mainHandler = new Handler(Looper.getMainLooper());
        
        // 添加更新检查监听器
        updateChecker.addListener(checkListener);
        
        // 创建更新检查任务
        checkTask = new Runnable() {
            @Override
            public void run() {
                // 执行更新检查
                checkUpdates();
                
                // 安排下一次检查
                mainHandler.postDelayed(this, CHECK_INTERVAL);
            }
        };
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // 开始定期检查
        mainHandler.post(checkTask);
        
        // 立即执行一次检查
        checkUpdates();
        
        return START_STICKY;
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        
        // 移除更新检查监听器
        updateChecker.removeListener(checkListener);
        
        // 取消更新检查任务
        mainHandler.removeCallbacks(checkTask);
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    /**
     * 检查应用更新
     */
    private void checkUpdates() {
        // 获取已安装应用列表
        appRepository.getInstalledApps(this, appList -> {
            if (appList != null && !appList.isEmpty()) {
                // 执行批量更新检查
                updateChecker.checkUpdates(appList);
            }
        });
    }
    
    /**
     * 手动触发更新检查
     */
    public void triggerUpdateCheck() {
        // 取消当前检查任务
        mainHandler.removeCallbacks(checkTask);
        
        // 立即执行检查
        checkUpdates();
        
        // 重新安排定期检查
        mainHandler.postDelayed(checkTask, CHECK_INTERVAL);
    }
} 