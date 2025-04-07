package com.mobileplatform.creator.data;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.mobileplatform.creator.R;
import com.mobileplatform.creator.data.model.AppInfo;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * 应用仓库，提供应用数据访问功能
 */
public class AppRepository {
    private static final String TAG = "AppRepository";
    
    private static volatile AppRepository instance;
    private final Executor executor = Executors.newSingleThreadExecutor();
    
    // 缓存应用列表
    private final Map<String, AppInfo> appCache = new HashMap<>();
    
    /**
     * 私有构造函数，防止外部实例化
     */
    private AppRepository() {
    }
    
    /**
     * 获取AppRepository单例实例
     */
    public static AppRepository getInstance() {
        if (instance == null) {
            synchronized (AppRepository.class) {
                if (instance == null) {
                    instance = new AppRepository();
                }
            }
        }
        return instance;
    }
    
    /**
     * 获取已安装的应用列表
     */
    public void getInstalledApps(Context context, AppListCallback callback) {
        executor.execute(() -> {
            try {
                List<AppInfo> appList = new ArrayList<>();
                
                // 获取包管理器
                android.content.pm.PackageManager pm = context.getPackageManager();
                List<android.content.pm.ApplicationInfo> installedApps = pm.getInstalledApplications(android.content.pm.PackageManager.GET_META_DATA);
                
                // 清除缓存中不存在的应用
                appCache.entrySet().removeIf(entry -> !isAppInstalled(context, entry.getValue().getPackageName()));
                
                // 处理每个已安装的应用
                for (android.content.pm.ApplicationInfo appInfo : installedApps) {
                    // 排除系统应用（可选）
                    boolean isSystemApp = (appInfo.flags & android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0;
                    
                    // 仅包含普通应用和包含启动活动的应用
                    android.content.Intent launchIntent = pm.getLaunchIntentForPackage(appInfo.packageName);
                    if (launchIntent == null && isSystemApp) {
                        continue; // 跳过没有启动活动的系统应用
                    }
                    
                    String packageName = appInfo.packageName;
                    
                    // 检查缓存中是否已存在
                    if (appCache.containsKey(packageName)) {
                        appList.add(appCache.get(packageName));
                    } else {
                        try {
                            // 获取应用信息
                            String appName = appInfo.loadLabel(pm).toString();
                            Drawable appIcon = appInfo.loadIcon(pm);
                            
                            // 获取包信息
                            android.content.pm.PackageInfo packageInfo = pm.getPackageInfo(packageName, 0);
                            String versionName = packageInfo.versionName;
                            
                            // 计算应用大小
                            long appSize = 0;
                            try {
                                java.io.File appFile = new java.io.File(appInfo.sourceDir);
                                appSize = appFile.length();
                            } catch (Exception e) {
                                Log.e(TAG, "计算应用大小失败: " + packageName, e);
                            }
                            
                            // 创建应用信息
                            AppInfo app = new AppInfo(
                                    packageName, // 使用包名作为ID
                                    appName,
                                    packageName,
                                    versionName,
                                    appSize,
                                    appIcon,
                                    appInfo.sourceDir
                            );
                            
                            // 设置系统应用标志
                            app.setSystemApp(isSystemApp);
                            
                            // 添加到缓存和列表
                            appCache.put(packageName, app);
                            appList.add(app);
                        } catch (Exception e) {
                            Log.e(TAG, "处理应用信息失败: " + packageName, e);
                        }
                    }
                }
                
                // 返回应用列表
                if (callback != null) {
                    new Handler(Looper.getMainLooper()).post(() -> callback.onResult(appList));
                }
                
            } catch (Exception e) {
                Log.e(TAG, "获取应用列表失败", e);
                if (callback != null) {
                    new Handler(Looper.getMainLooper()).post(() -> callback.onResult(new ArrayList<>()));
                }
            }
        });
    }
    
    /**
     * 通过ID获取应用信息
     */
    public void getAppById(Context context, String appId, AppCallback callback) {
        executor.execute(() -> {
            try {
                // 在缓存中查找
                for (AppInfo app : appCache.values()) {
                    if (app.getId().equals(appId)) {
                        callback.onResult(app, null);
                        return;
                    }
                }
                
                // 如果缓存中没有，先加载所有应用
                getInstalledApps(context, (apps, error) -> {
                    if (error != null) {
                        callback.onResult(null, error);
                        return;
                    }
                    
                    if (apps != null) {
                        for (AppInfo app : apps) {
                            if (app.getId().equals(appId)) {
                                callback.onResult(app, null);
                                return;
                            }
                        }
                    }
                    
                    callback.onResult(null, "找不到应用ID: " + appId);
                });
                
            } catch (Exception e) {
                Log.e(TAG, "获取应用信息失败", e);
                callback.onResult(null, "获取应用信息失败: " + e.getMessage());
            }
        });
    }
    
    /**
     * 通过包名获取应用信息
     */
    public void getAppByPackageName(Context context, String packageName, AppCallback callback) {
        executor.execute(() -> {
            try {
                // 在缓存中查找
                if (appCache.containsKey(packageName)) {
                    callback.onResult(appCache.get(packageName), null);
                    return;
                }
                
                // 如果缓存中没有，先加载所有应用
                getInstalledApps(context, (apps, error) -> {
                    if (error != null) {
                        callback.onResult(null, error);
                        return;
                    }
                    
                    if (apps != null) {
                        for (AppInfo app : apps) {
                            if (app.getPackageName().equals(packageName)) {
                                callback.onResult(app, null);
                                return;
                            }
                        }
                    }
                    
                    callback.onResult(null, "找不到应用包名: " + packageName);
                });
                
            } catch (Exception e) {
                Log.e(TAG, "获取应用信息失败", e);
                callback.onResult(null, "获取应用信息失败: " + e.getMessage());
            }
        });
    }
    
    /**
     * 检查应用是否已安装
     */
    public boolean isAppInstalled(Context context, String packageName) {
        String appDirName = packageName.replace('.', '_');
        File appDir = new File(context.getExternalFilesDir(null), "apps/" + appDirName);
        return appDir.exists() && appDir.isDirectory();
    }
    
    /**
     * 计算目录大小
     */
    private long calculateDirSize(File dir) {
        long size = 0;
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        size += file.length();
                    } else {
                        size += calculateDirSize(file);
                    }
                }
            }
        }
        return size;
    }
    
    /**
     * 用于获取应用列表的回调接口
     */
    public interface AppListCallback {
        void onResult(List<AppInfo> apps);
    }
    
    /**
     * 用于获取单个应用的回调接口
     */
    public interface AppCallback {
        void onResult(AppInfo app, String error);
    }
} 