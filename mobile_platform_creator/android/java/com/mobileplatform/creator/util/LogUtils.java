package com.mobileplatform.creator.util;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import com.mobileplatform.creator.data.model.AppInfo;
import com.mobileplatform.creator.data.model.InstallLogEntry;
import com.mobileplatform.creator.data.repository.InstallLogRepository;

/**
 * 日志工具类，封装日志记录相关功能
 */
public class LogUtils {
    private static final String TAG = "LogUtils";
    
    /**
     * 记录应用安装成功日志
     * 
     * @param context 上下文
     * @param appInfo 应用信息
     */
    public static void logInstallSuccess(Context context, AppInfo appInfo) {
        if (context == null || appInfo == null) return;
        
        try {
            InstallLogRepository repository = InstallLogRepository.getInstance(context);
            repository.addSuccessLog(
                    appInfo.getId(),
                    appInfo.getName(),
                    appInfo.getPackageName(),
                    appInfo.getVersion(),
                    InstallLogEntry.OperationType.INSTALL
            );
            Log.d(TAG, "应用安装成功日志已记录: " + appInfo.getName());
        } catch (Exception e) {
            Log.e(TAG, "记录应用安装日志失败", e);
        }
    }
    
    /**
     * 记录应用安装失败日志
     * 
     * @param context 上下文
     * @param appInfo 应用信息
     * @param errorMessage 错误信息
     */
    public static void logInstallFailure(Context context, AppInfo appInfo, String errorMessage) {
        if (context == null || appInfo == null) return;
        
        try {
            InstallLogRepository repository = InstallLogRepository.getInstance(context);
            repository.addFailureLog(
                    appInfo.getId(),
                    appInfo.getName(),
                    appInfo.getPackageName(),
                    appInfo.getVersion(),
                    InstallLogEntry.OperationType.INSTALL,
                    errorMessage
            );
            Log.d(TAG, "应用安装失败日志已记录: " + appInfo.getName());
        } catch (Exception e) {
            Log.e(TAG, "记录应用安装失败日志失败", e);
        }
    }
    
    /**
     * 记录应用更新成功日志
     * 
     * @param context 上下文
     * @param appInfo 应用信息
     * @param oldVersion 旧版本号
     */
    public static void logUpdateSuccess(Context context, AppInfo appInfo, String oldVersion) {
        if (context == null || appInfo == null) return;
        
        try {
            InstallLogRepository repository = InstallLogRepository.getInstance(context);
            // 在附加信息中记录旧版本号
            String additionalInfo = "从版本" + oldVersion + "更新到" + appInfo.getVersion();
            
            InstallLogEntry logEntry = InstallLogEntry.createSuccessLog(
                    appInfo.getId(),
                    appInfo.getName(),
                    appInfo.getPackageName(),
                    appInfo.getVersion(),
                    InstallLogEntry.OperationType.UPDATE
            );
            logEntry.setAdditionalInfo(additionalInfo);
            
            repository.addLog(logEntry);
            Log.d(TAG, "应用更新成功日志已记录: " + appInfo.getName());
        } catch (Exception e) {
            Log.e(TAG, "记录应用更新日志失败", e);
        }
    }
    
    /**
     * 记录应用更新失败日志
     * 
     * @param context 上下文
     * @param appInfo 应用信息
     * @param errorMessage 错误信息
     */
    public static void logUpdateFailure(Context context, AppInfo appInfo, String errorMessage) {
        if (context == null || appInfo == null) return;
        
        try {
            InstallLogRepository repository = InstallLogRepository.getInstance(context);
            repository.addFailureLog(
                    appInfo.getId(),
                    appInfo.getName(),
                    appInfo.getPackageName(),
                    appInfo.getVersion(),
                    InstallLogEntry.OperationType.UPDATE,
                    errorMessage
            );
            Log.d(TAG, "应用更新失败日志已记录: " + appInfo.getName());
        } catch (Exception e) {
            Log.e(TAG, "记录应用更新失败日志失败", e);
        }
    }
    
    /**
     * 记录应用卸载成功日志
     * 
     * @param context 上下文
     * @param appInfo 应用信息
     */
    public static void logUninstallSuccess(Context context, AppInfo appInfo) {
        if (context == null || appInfo == null) return;
        
        try {
            InstallLogRepository repository = InstallLogRepository.getInstance(context);
            repository.addSuccessLog(
                    appInfo.getId(),
                    appInfo.getName(),
                    appInfo.getPackageName(),
                    appInfo.getVersion(),
                    InstallLogEntry.OperationType.UNINSTALL
            );
            Log.d(TAG, "应用卸载成功日志已记录: " + appInfo.getName());
        } catch (Exception e) {
            Log.e(TAG, "记录应用卸载日志失败", e);
        }
    }
    
    /**
     * 记录应用卸载失败日志
     * 
     * @param context 上下文
     * @param appInfo 应用信息
     * @param errorMessage 错误信息
     */
    public static void logUninstallFailure(Context context, AppInfo appInfo, String errorMessage) {
        if (context == null || appInfo == null) return;
        
        try {
            InstallLogRepository repository = InstallLogRepository.getInstance(context);
            repository.addFailureLog(
                    appInfo.getId(),
                    appInfo.getName(),
                    appInfo.getPackageName(),
                    appInfo.getVersion(),
                    InstallLogEntry.OperationType.UNINSTALL,
                    errorMessage
            );
            Log.d(TAG, "应用卸载失败日志已记录: " + appInfo.getName());
        } catch (Exception e) {
            Log.e(TAG, "记录应用卸载失败日志失败", e);
        }
    }
    
    /**
     * 根据包名获取应用版本
     * 
     * @param context 上下文
     * @param packageName 包名
     * @return 版本号，获取失败返回null
     */
    public static String getAppVersion(Context context, String packageName) {
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo info = pm.getPackageInfo(packageName, 0);
            return info.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "获取应用版本失败", e);
            return null;
        }
    }
} 