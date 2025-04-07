package com.mobileplatform.creator.sandbox;

import android.app.ActivityManager;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.LauncherApps;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Process;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.Nullable;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 沙箱服务，管理应用沙箱环境
 */
public class SandboxService extends Service {
    private static final String TAG = "SandboxService";
    
    // 安全级别常量
    public static final int SECURITY_LEVEL_STRICT = 2;
    public static final int SECURITY_LEVEL_STANDARD = 1;
    public static final int SECURITY_LEVEL_MINIMAL = 0;
    
    // 服务操作常量
    private static final String ACTION_START_SANDBOX = "com.mobileplatform.creator.sandbox.START";
    private static final String ACTION_STOP_SANDBOX = "com.mobileplatform.creator.sandbox.STOP";
    private static final String EXTRA_PACKAGE_NAME = "package_name";
    private static final String EXTRA_SECURITY_LEVEL = "security_level";
    
    // 资源监控周期(毫秒)
    private static final long MONITOR_INTERVAL = 2000;
    
    // 默认安全级别
    private static int defaultSecurityLevel = SECURITY_LEVEL_STANDARD;
    
    // 线程池
    private final Executor executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    
    // 本地Binder
    private final IBinder binder = new SandboxBinder();
    
    // 回调映射表
    private static final Map<String, SandboxCallback> callbackMap = new HashMap<>();
    
    // 当前运行的沙箱应用
    private String runningPackage = null;
    private boolean isRunning = false;
    private int runningAppPid = -1;
    
    // 资源监控线程
    private Thread monitorThread = null;
    private boolean monitorRunning = false;
    
    // Seccomp过滤器管理
    private SeccompManager seccompManager;
    
    /**
     * Binder类，用于本地绑定
     */
    public class SandboxBinder extends Binder {
        public SandboxService getService() {
            return SandboxService.this;
        }
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "沙箱服务已创建");
        
        // 初始化Seccomp管理器
        seccompManager = SeccompManager.getInstance(this);
        
        // 初始化本地库
        try {
            System.loadLibrary("seccomp_filter");
            Log.d(TAG, "成功加载安全计算模式过滤器库");
        } catch (UnsatisfiedLinkError e) {
            Log.e(TAG, "加载安全计算模式过滤器库失败", e);
        }
    }
    
    @Override
    public void onDestroy() {
        // 停止当前运行的应用
        if (isRunning) {
            stopSandboxInternal();
        }
        
        super.onDestroy();
        Log.d(TAG, "沙箱服务已销毁");
    }
    
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (ACTION_START_SANDBOX.equals(action)) {
                String packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME);
                int securityLevel = intent.getIntExtra(EXTRA_SECURITY_LEVEL, defaultSecurityLevel);
                startSandboxInternal(packageName, securityLevel);
            } else if (ACTION_STOP_SANDBOX.equals(action)) {
                stopSandboxInternal();
            }
        }
        
        return START_NOT_STICKY;
    }
    
    /**
     * 内部启动沙箱方法
     */
    private void startSandboxInternal(String packageName, int securityLevel) {
        // 如果已有沙箱运行，先停止
        if (isRunning && runningPackage != null) {
            stopSandboxInternal();
        }
        
        Log.d(TAG, "启动沙箱环境: 包名=" + packageName + ", 安全级别=" + securityLevel);
        
        executor.execute(() -> {
            try {
                // 检查应用是否存在
                PackageManager pm = getPackageManager();
                ApplicationInfo appInfo;
                
                try {
                    appInfo = pm.getApplicationInfo(packageName, 0);
                } catch (PackageManager.NameNotFoundException e) {
                    throw new Exception("找不到应用: " + packageName);
                }
                
                // 记录当前运行的应用
                runningPackage = packageName;
                isRunning = true;
                
                // 配置沙箱环境
                configureSeccompFilter(securityLevel);
                
                // 启动应用
                launchApp(packageName);
                
                // 通知沙箱已启动
                SandboxCallback callback = callbackMap.get(packageName);
                if (callback != null) {
                    mainHandler.post(callback::onSandboxStarted);
                }
                
                // 开始资源监控
                startResourceMonitoring(packageName, callback);
                
            } catch (Exception e) {
                Log.e(TAG, "启动沙箱失败", e);
                isRunning = false;
                
                // 通知错误
                SandboxCallback callback = callbackMap.get(packageName);
                if (callback != null) {
                    String errorMsg = "启动沙箱失败: " + e.getMessage();
                    mainHandler.post(() -> callback.onSandboxError(errorMsg));
                }
            }
        });
    }
    
    /**
     * 内部停止沙箱方法
     */
    private void stopSandboxInternal() {
        if (!isRunning || runningPackage == null) {
            return;
        }
        
        Log.d(TAG, "停止沙箱环境: 包名=" + runningPackage);
        
        String packageToStop = runningPackage; // 保存一份引用，防止并发修改
        
        executor.execute(() -> {
            try {
                // 停止资源监控
                stopResourceMonitoring();
                
                // 停止应用程序
                stopApp(packageToStop);
                
                // 清理沙箱环境
                cleanupSandbox();
                
                // 通知沙箱已停止
                SandboxCallback callback = callbackMap.get(packageToStop);
                if (callback != null) {
                    mainHandler.post(callback::onSandboxStopped);
                }
                
            } catch (Exception e) {
                Log.e(TAG, "停止沙箱失败", e);
                
                // 通知错误
                SandboxCallback callback = callbackMap.get(packageToStop);
                if (callback != null) {
                    String errorMsg = "停止沙箱失败: " + e.getMessage();
                    mainHandler.post(() -> callback.onSandboxError(errorMsg));
                }
            } finally {
                // 无论如何，都标记为已停止
                isRunning = false;
                runningPackage = null;
                runningAppPid = -1;
            }
        });
    }
    
    /**
     * 配置Seccomp过滤器
     */
    private void configureSeccompFilter(int securityLevel) {
        // 根据安全级别配置不同的过滤器
        switch (securityLevel) {
            case SECURITY_LEVEL_STRICT:
                // 严格模式，只允许最基本的系统调用
                seccompManager.applyFilter(SeccompManager.SANDBOX_LEVEL_STRICT);
                break;
            case SECURITY_LEVEL_STANDARD:
                // 标准模式，允许常用系统调用
                seccompManager.applyFilter(SeccompManager.SANDBOX_LEVEL_STANDARD);
                break;
            case SECURITY_LEVEL_MINIMAL:
                // 最小限制模式，主要用于调试
                seccompManager.applyFilter(SeccompManager.SANDBOX_LEVEL_MINIMAL);
                break;
            default:
                // 默认使用标准模式
                seccompManager.applyFilter(SeccompManager.SANDBOX_LEVEL_STANDARD);
                break;
        }
    }
    
    /**
     * 启动应用
     */
    private void launchApp(String packageName) throws Exception {
        // 获取包信息
        PackageManager pm = getPackageManager();
        Intent launchIntent = pm.getLaunchIntentForPackage(packageName);
        
        if (launchIntent == null) {
            throw new Exception("无法获取应用启动Intent: " + packageName);
        }
        
        // 添加沙箱标识
        launchIntent.putExtra("sandbox_mode", true);
        launchIntent.putExtra("sandbox_level", defaultSecurityLevel);
        
        // 设置新任务标志
        launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        
        // 启动应用
        startActivity(launchIntent);
        
        // 获取应用的进程ID
        try {
            // 等待应用启动
            TimeUnit.SECONDS.sleep(1);
            
            // 查找进程ID
            runningAppPid = findAppProcessId(packageName);
            
            if (runningAppPid == -1) {
                throw new Exception("无法获取应用进程ID");
            }
            
            Log.d(TAG, "应用已启动，进程ID: " + runningAppPid);
        } catch (InterruptedException e) {
            Log.e(TAG, "等待应用启动被中断", e);
        }
    }
    
    /**
     * 查找应用的进程ID
     */
    private int findAppProcessId(String packageName) {
        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> processes = am.getRunningAppProcesses();
        
        for (ActivityManager.RunningAppProcessInfo process : processes) {
            if (process.processName.equals(packageName)) {
                return process.pid;
            }
        }
        
        return -1;
    }
    
    /**
     * 停止应用
     */
    private void stopApp(String packageName) {
        // 使用ActivityManager强制停止应用
        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        am.killBackgroundProcesses(packageName);
        
        // 如果进程ID有效，尝试直接终止进程
        if (runningAppPid > 0) {
            try {
                Process.killProcess(runningAppPid);
                Log.d(TAG, "终止进程: " + runningAppPid);
            } catch (Exception e) {
                Log.e(TAG, "终止进程失败: " + e.getMessage());
            }
        }
    }
    
    /**
     * 清理沙箱环境
     */
    private void cleanupSandbox() {
        // 重置过滤器
        seccompManager.resetFilter();
        
        // 清理临时文件
        if (runningPackage != null) {
            File sandboxDir = new File(getFilesDir(), "sandbox_" + runningPackage);
            if (sandboxDir.exists() && sandboxDir.isDirectory()) {
                deleteRecursive(sandboxDir);
            }
        }
    }
    
    /**
     * 递归删除文件夹
     */
    private void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory()) {
            for (File child : fileOrDirectory.listFiles()) {
                deleteRecursive(child);
            }
        }
        fileOrDirectory.delete();
    }
    
    /**
     * 开始资源监控
     */
    private void startResourceMonitoring(String packageName, SandboxCallback callback) {
        // 停止现有监控
        stopResourceMonitoring();
        
        // 启动新的监控线程
        monitorRunning = true;
        monitorThread = new Thread(() -> {
            try {
                while (monitorRunning && isRunning) {
                    // 检查应用是否还在运行
                    int pid = findAppProcessId(packageName);
                    if (pid == -1) {
                        // 应用不再运行
                        monitorRunning = false;
                        isRunning = false;
                        runningAppPid = -1;
                        
                        mainHandler.post(() -> {
                            if (callback != null) {
                                callback.onSandboxStopped();
                            }
                        });
                        
                        break;
                    }
                    
                    // 更新进程ID
                    runningAppPid = pid;
                    
                    // 监控资源使用
                    ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
                    ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
                    am.getMemoryInfo(memoryInfo);
                    
                    // 读取进程状态
                    String cpuUsage = readProcessCpuUsage(pid);
                    String memUsage = readProcessMemUsage(pid);
                    
                    // 构建资源信息
                    StringBuilder resourceInfo = new StringBuilder();
                    resourceInfo.append("CPU: ").append(cpuUsage).append("%");
                    resourceInfo.append(", 内存: ").append(memUsage).append("MB");
                    
                    // 通知资源使用情况
                    if (callback != null) {
                        String finalInfo = resourceInfo.toString();
                        mainHandler.post(() -> callback.onResourceUsage(finalInfo));
                    }
                    
                    // 检查安全违规
                    checkSecurityViolations(pid, packageName, callback);
                    
                    // 等待下一次监控
                    Thread.sleep(MONITOR_INTERVAL);
                }
            } catch (InterruptedException e) {
                // 线程被中断，停止监控
                Log.d(TAG, "资源监控线程已停止");
            }
        });
        
        monitorThread.start();
    }
    
    /**
     * 停止资源监控
     */
    private void stopResourceMonitoring() {
        monitorRunning = false;
        
        if (monitorThread != null && monitorThread.isAlive()) {
            try {
                monitorThread.interrupt();
                monitorThread.join(1000); // 等待最多1秒
            } catch (InterruptedException e) {
                Log.e(TAG, "停止监控线程被中断", e);
            }
            
            monitorThread = null;
        }
    }
    
    /**
     * 读取进程CPU使用率
     */
    private String readProcessCpuUsage(int pid) {
        try {
            // 读取/proc/[pid]/stat文件获取CPU使用情况
            BufferedReader reader = new BufferedReader(new FileReader("/proc/" + pid + "/stat"));
            String line = reader.readLine();
            reader.close();
            
            if (line != null) {
                String[] parts = line.split("\\s+");
                if (parts.length >= 15) {
                    long utime = Long.parseLong(parts[13]); // 用户态CPU时间
                    long stime = Long.parseLong(parts[14]); // 内核态CPU时间
                    long totalTime = utime + stime;
                    
                    // 简单计算CPU使用率
                    return String.format("%.1f", (totalTime / 100.0));
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "读取进程CPU使用率失败", e);
        }
        
        return "0.0";
    }
    
    /**
     * 读取进程内存使用
     */
    private String readProcessMemUsage(int pid) {
        try {
            // 读取/proc/[pid]/status文件获取内存使用情况
            BufferedReader reader = new BufferedReader(new FileReader("/proc/" + pid + "/status"));
            String line;
            long vmRSS = 0;
            
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("VmRSS:")) {
                    String[] parts = line.split("\\s+");
                    if (parts.length >= 2) {
                        vmRSS = Long.parseLong(parts[1]);
                        break;
                    }
                }
            }
            reader.close();
            
            // 转换为MB
            return String.format("%.1f", (vmRSS / 1024.0));
        } catch (IOException e) {
            Log.e(TAG, "读取进程内存使用失败", e);
        }
        
        return "0.0";
    }
    
    /**
     * 检查安全违规
     */
    private void checkSecurityViolations(int pid, String packageName, SandboxCallback callback) {
        // 这里可以检查安全违规，如网络访问、文件访问等
        // 目前只是模拟一些安全检查
        
        try {
            // 读取进程打开的文件
            File proc = new File("/proc/" + pid + "/fd");
            File[] files = proc.listFiles();
            
            if (files != null) {
                List<String> suspiciousFiles = new ArrayList<>();
                
                for (File file : files) {
                    try {
                        String target = file.getCanonicalPath();
                        
                        // 检查敏感目录访问
                        for (String path : getRestrictedPaths()) {
                            if (target.startsWith(path)) {
                                suspiciousFiles.add(target);
                                break;
                            }
                        }
                    } catch (IOException e) {
                        // 忽略无法解析的符号链接
                    }
                }
                
                // 报告可疑文件访问
                if (!suspiciousFiles.isEmpty() && callback != null) {
                    String violation = "可疑文件访问: " + String.join(", ", suspiciousFiles);
                    mainHandler.post(() -> callback.onSecurityViolation(violation));
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "检查安全违规失败", e);
        }
    }
    
    /**
     * 获取受限目录列表
     */
    private List<String> getRestrictedPaths() {
        List<String> paths = new ArrayList<>();
        
        // 系统关键目录
        paths.add("/system/bin");
        paths.add("/system/xbin");
        paths.add("/vendor");
        paths.add("/data/data"); // 其他应用数据
        
        // 用户敏感数据
        String externalStorage = Environment.getExternalStorageDirectory().getAbsolutePath();
        paths.add(externalStorage + "/DCIM");
        paths.add(externalStorage + "/Download");
        paths.add(externalStorage + "/Documents");
        
        return paths;
    }
    
    /**
     * 应用Seccomp过滤器（JNI方法）
     */
    private native boolean applySeccompFilter(int level);
    
    /**
     * 启动沙箱环境
     */
    public static void startSandbox(Context context, String packageName, int securityLevel, SandboxCallback callback) {
        // 存储回调对象
        callbackMap.put(packageName, callback);
        
        // 启动服务
        Intent intent = new Intent(context, SandboxService.class);
        intent.setAction(ACTION_START_SANDBOX);
        intent.putExtra(EXTRA_PACKAGE_NAME, packageName);
        intent.putExtra(EXTRA_SECURITY_LEVEL, securityLevel);
        context.startService(intent);
    }
    
    /**
     * 停止沙箱环境
     */
    public static void stopSandbox(Context context) {
        // 停止服务
        Intent intent = new Intent(context, SandboxService.class);
        intent.setAction(ACTION_STOP_SANDBOX);
        context.startService(intent);
    }
    
    /**
     * 设置默认安全级别
     */
    public static void setDefaultSecurityLevel(Context context, int securityLevel) {
        defaultSecurityLevel = securityLevel;
    }
    
    /**
     * 沙箱事件回调接口
     */
    public interface SandboxCallback {
        void onSandboxStarted();
        void onSandboxStopped();
        void onSandboxError(String error);
        void onSecurityViolation(String violation);
        void onResourceUsage(String resourceInfo);
    }
} 