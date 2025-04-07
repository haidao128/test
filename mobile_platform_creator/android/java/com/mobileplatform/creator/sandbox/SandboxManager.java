package com.mobileplatform.creator.sandbox;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Process;
import android.util.Log;

import com.mobileplatform.creator.data.model.AppInfo;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * 沙箱管理器
 * 负责管理沙箱环境和应用运行
 */
public class SandboxManager {
    private static final String TAG = "SandboxManager";
    
    // 单例实例
    private static SandboxManager instance;
    
    // 上下文
    private final Context context;
    
    // 线程池
    private final Executor executor = Executors.newCachedThreadPool();
    
    // 主线程Handler
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    
    // 沙箱服务
    private SandboxService sandboxService;
    
    // 服务连接状态
    private boolean isBound = false;
    
    // 沙箱监听器列表
    private final List<SandboxListener> listeners = new CopyOnWriteArrayList<>();
    
    // 当前运行的应用
    private String runningSandboxApp = null;
    
    // 沙箱安全级别
    private int currentSecurityLevel = SandboxService.SECURITY_LEVEL_STANDARD;
    
    // 进程运行信息
    private final Map<String, ProcessInfo> runningProcesses = new HashMap<>();
    
    /**
     * 进程信息类
     */
    private static class ProcessInfo {
        String packageName;
        int pid;
        long startTime;
        int securityLevel;
        SandboxCallback callback;
        
        ProcessInfo(String packageName, int pid, int securityLevel, SandboxCallback callback) {
            this.packageName = packageName;
            this.pid = pid;
            this.startTime = System.currentTimeMillis();
            this.securityLevel = securityLevel;
            this.callback = callback;
        }
    }
    
    /**
     * 获取单例实例
     * 
     * @param context 上下文
     * @return 沙箱管理器实例
     */
    public static synchronized SandboxManager getInstance(Context context) {
        if (instance == null) {
            instance = new SandboxManager(context.getApplicationContext());
        }
        return instance;
    }
    
    /**
     * 私有构造函数
     * 
     * @param context 上下文
     */
    private SandboxManager(Context context) {
        this.context = context;
        bindSandboxService();
    }
    
    /**
     * 绑定沙箱服务
     */
    private void bindSandboxService() {
        Intent intent = new Intent(context, SandboxService.class);
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        
        // 同时启动服务，确保服务在后台运行
        context.startService(intent);
    }
    
    /**
     * 解绑沙箱服务
     */
    private void unbindSandboxService() {
        if (isBound) {
            context.unbindService(serviceConnection);
            isBound = false;
            sandboxService = null;
        }
    }
    
    /**
     * 服务连接
     */
    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            SandboxService.SandboxBinder binder = (SandboxService.SandboxBinder) service;
            sandboxService = binder.getService();
            isBound = true;
            Log.d(TAG, "已绑定沙箱服务");
            
            // 恢复运行中的应用
            if (runningSandboxApp != null) {
                startApp(runningSandboxApp, currentSecurityLevel, null);
            }
        }
        
        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
            sandboxService = null;
            Log.d(TAG, "沙箱服务已断开");
        }
    };
    
    /**
     * 启动应用
     * 
     * @param packageName 包名
     * @param securityLevel 安全级别
     * @param listener 监听器
     * @return 是否成功启动
     */
    public boolean startApp(String packageName, int securityLevel, SandboxListener listener) {
        if (packageName == null || packageName.isEmpty()) {
            Log.e(TAG, "启动应用失败：包名为空");
            return false;
        }
        
        // 添加监听器
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
        
        // 检查应用
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(packageName, 0);
            if (packageInfo == null) {
                Log.e(TAG, "启动应用失败：找不到应用 " + packageName);
                notifyError("找不到应用: " + packageName);
                return false;
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "启动应用失败：找不到应用 " + packageName, e);
            notifyError("找不到应用: " + packageName);
            return false;
        }
        
        // 保存当前运行信息
        runningSandboxApp = packageName;
        currentSecurityLevel = securityLevel;
        
        // 创建回调
        SandboxCallback callback = createSandboxCallback(packageName);
        
        // 启动沙箱服务
        if (isBound && sandboxService != null) {
            // 直接启动服务
            SandboxService.startSandbox(context, packageName, securityLevel, callback);
            return true;
        } else {
            // 重新绑定服务
            bindSandboxService();
            notifyStatus("正在连接沙箱服务...");
            return true;
        }
    }
    
    /**
     * 停止应用
     */
    public void stopApp() {
        if (runningSandboxApp == null) {
            // 没有运行中的应用
            return;
        }
        
        // 停止沙箱服务
        SandboxService.stopSandbox(context);
        
        // 清除运行状态
        runningSandboxApp = null;
        
        // 通知停止
        notifyStop();
    }
    
    /**
     * 创建沙箱回调
     * 
     * @param packageName 包名
     * @return 沙箱回调
     */
    private SandboxCallback createSandboxCallback(String packageName) {
        return new SandboxCallback() {
            @Override
            public void onSandboxStarted() {
                notifyStart(packageName);
            }
            
            @Override
            public void onSandboxStopped() {
                // 清除运行状态
                runningSandboxApp = null;
                notifyStop();
            }
            
            @Override
            public void onSandboxError(String error) {
                notifyError(error);
            }
            
            @Override
            public void onSecurityViolation(String violation) {
                notifySecurityViolation(violation);
            }
            
            @Override
            public void onResourceUsage(String resourceInfo) {
                notifyResourceUsage(resourceInfo);
            }
        };
    }
    
    /**
     * 获取当前运行的应用包名
     * 
     * @return 包名，如果没有则返回null
     */
    public String getRunningAppPackage() {
        return runningSandboxApp;
    }
    
    /**
     * 获取当前安全级别
     * 
     * @return 安全级别
     */
    public int getCurrentSecurityLevel() {
        return currentSecurityLevel;
    }
    
    /**
     * 添加沙箱监听器
     * 
     * @param listener 监听器
     */
    public void addSandboxListener(SandboxListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }
    
    /**
     * 移除沙箱监听器
     * 
     * @param listener 监听器
     */
    public void removeSandboxListener(SandboxListener listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }
    
    /**
     * 通知应用启动
     * 
     * @param packageName 包名
     */
    private void notifyStart(String packageName) {
        mainHandler.post(() -> {
            for (SandboxListener listener : listeners) {
                listener.onSandboxStarted(packageName);
            }
        });
    }
    
    /**
     * 通知应用停止
     */
    private void notifyStop() {
        mainHandler.post(() -> {
            for (SandboxListener listener : listeners) {
                listener.onSandboxStopped();
            }
        });
    }
    
    /**
     * 通知沙箱错误
     * 
     * @param error 错误信息
     */
    private void notifyError(String error) {
        mainHandler.post(() -> {
            for (SandboxListener listener : listeners) {
                listener.onSandboxError(error);
            }
        });
    }
    
    /**
     * 通知安全违规
     * 
     * @param violation 违规信息
     */
    private void notifySecurityViolation(String violation) {
        mainHandler.post(() -> {
            for (SandboxListener listener : listeners) {
                listener.onSecurityViolation(violation);
            }
        });
    }
    
    /**
     * 通知资源使用
     * 
     * @param resourceInfo 资源使用信息
     */
    private void notifyResourceUsage(String resourceInfo) {
        mainHandler.post(() -> {
            for (SandboxListener listener : listeners) {
                listener.onResourceUsage(resourceInfo);
            }
        });
    }
    
    /**
     * 通知状态信息
     * 
     * @param status 状态信息
     */
    private void notifyStatus(String status) {
        mainHandler.post(() -> {
            for (SandboxListener listener : listeners) {
                listener.onStatusChanged(status);
            }
        });
    }
    
    /**
     * 沙箱监听器接口
     */
    public interface SandboxListener {
        // 沙箱启动
        void onSandboxStarted(String packageName);
        
        // 沙箱停止
        void onSandboxStopped();
        
        // 沙箱错误
        void onSandboxError(String error);
        
        // 安全违规
        void onSecurityViolation(String violation);
        
        // 资源使用
        void onResourceUsage(String resourceInfo);
        
        // 状态变化
        void onStatusChanged(String status);
    }
    
    /**
     * 沙箱回调接口
     */
    public interface SandboxCallback extends SandboxService.SandboxCallback {
    }
} 