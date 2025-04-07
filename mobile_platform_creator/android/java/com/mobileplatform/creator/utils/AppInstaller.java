package com.mobileplatform.creator.utils;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;

import com.mobileplatform.creator.data.model.InstallResult;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * 应用安装工具类
 * 提供应用安装、更新和卸载功能
 */
public class AppInstaller {
    private static final String TAG = "AppInstaller";
    private static final String FILE_PROVIDER_AUTHORITY = "com.mobileplatform.creator.fileprovider";
    private static final String MPK_EXTENSION = ".mpk";
    private static final String APK_EXTENSION = ".apk";
    
    // 安装任务缓存
    private static final Map<String, InstallTask> installTasks = new HashMap<>();
    
    // 线程池
    private static final ExecutorService executor = Executors.newCachedThreadPool();
    
    // 主线程Handler
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());
    
    /**
     * 安装MPK包
     * 
     * @param context 上下文
     * @param mpkFile MPK文件
     * @param callback 安装回调
     */
    public static void installMpkPackage(@NonNull Context context, @NonNull File mpkFile, 
                                          @NonNull InstallCallback callback) {
        // 检查文件是否存在
        if (!mpkFile.exists() || !mpkFile.getName().endsWith(MPK_EXTENSION)) {
            callback.onInstallFailed(mpkFile.getAbsolutePath(), "文件不存在或格式错误");
            return;
        }
        
        // 检查是否已有此文件的安装任务
        if (installTasks.containsKey(mpkFile.getAbsolutePath())) {
            Log.w(TAG, "已存在安装任务：" + mpkFile.getName());
            callback.onInstallFailed(mpkFile.getAbsolutePath(), "已存在相同的安装任务");
            return;
        }
        
        // 创建安装任务
        InstallTask task = new InstallTask(context, mpkFile, callback);
        installTasks.put(mpkFile.getAbsolutePath(), task);
        
        // 后台执行安装
        executor.execute(() -> {
            try {
                InstallResult result = parseMpkPackage(context, mpkFile);
                if (result != null && result.getApkFile() != null) {
                    // 继续安装APK
                    installApkFile(context, new File(result.getApkFile()), result.getExtractDir(), callback);
                } else {
                    // 解析失败
                    callback.onInstallFailed(mpkFile.getAbsolutePath(), "MPK包解析失败");
                    // 清理任务
                    installTasks.remove(mpkFile.getAbsolutePath());
                }
            } catch (Exception e) {
                Log.e(TAG, "安装MPK时发生错误：" + e.getMessage());
                callback.onInstallFailed(mpkFile.getAbsolutePath(), "安装过程中发生错误：" + e.getMessage());
                // 清理任务
                installTasks.remove(mpkFile.getAbsolutePath());
            }
        });
    }
    
    /**
     * 安装APK文件
     * 
     * @param context 上下文
     * @param apkFile APK文件
     * @param extractDir 解压目录，如果直接安装APK则为null
     * @param callback 安装回调
     */
    public static void installApkFile(@NonNull Context context, @NonNull File apkFile, 
                                     @Nullable String extractDir, @NonNull InstallCallback callback) {
        // 检查文件是否存在
        if (!apkFile.exists() || !apkFile.getName().endsWith(APK_EXTENSION)) {
            callback.onInstallFailed(apkFile.getAbsolutePath(), "APK文件不存在或格式错误");
            return;
        }
        
        try {
            // 获取包信息
            PackageInfo packageInfo = getApkPackageInfo(context, apkFile);
            if (packageInfo == null) {
                callback.onInstallFailed(apkFile.getAbsolutePath(), "无法获取APK包信息");
                return;
            }
            
            String packageName = packageInfo.packageName;
            
            // 检查是否已安装此应用
            AppVersion installedVersion = getAppVersion(context, packageName);
            
            // 创建安装Intent
            Intent intent = createInstallIntent(context, apkFile);
            context.startActivity(intent);
            
            // 延迟检查安装结果
            final String apkPath = apkFile.getAbsolutePath();
            mainHandler.postDelayed(() -> {
                // 安装完成后检查是否成功
                AppVersion newVersion = getAppVersion(context, packageName);
                
                // 从任务列表中移除
                if (extractDir != null) {
                    installTasks.remove(extractDir);
                }
                
                if (newVersion != null) {
                    // 安装成功
                    if (installedVersion == null) {
                        // 首次安装
                        callback.onInstallSuccess(apkPath, packageName, newVersion.versionName, newVersion.versionCode);
                    } else if (newVersion.versionCode > installedVersion.versionCode) {
                        // 已更新版本
                        callback.onInstallUpdated(apkPath, packageName, newVersion.versionName, newVersion.versionCode);
                    } else {
                        // 相同版本
                        callback.onInstallSuccess(apkPath, packageName, newVersion.versionName, newVersion.versionCode);
                    }
                } else {
                    // 可能安装失败或用户取消
                    callback.onInstallFailed(apkPath, "安装失败，可能被用户取消");
                }
            }, 5000); // 延迟5秒检查，给用户安装界面时间
            
        } catch (Exception e) {
            Log.e(TAG, "安装APK时发生错误：" + e.getMessage());
            callback.onInstallFailed(apkFile.getAbsolutePath(), "安装过程中发生错误：" + e.getMessage());
            
            // 清理任务
            if (extractDir != null) {
                installTasks.remove(extractDir);
            }
        }
    }
    
    /**
     * 卸载应用
     * 
     * @param context 上下文
     * @param packageName 包名
     * @param callback 卸载回调
     */
    public static void uninstallApp(@NonNull Context context, @NonNull String packageName,
                                   @NonNull UninstallCallback callback) {
        // 检查应用是否已安装
        if (!isAppInstalled(context, packageName)) {
            callback.onUninstallFailed(packageName, "应用未安装");
            return;
        }
        
        try {
            // 创建卸载Intent
            Intent intent = new Intent(Intent.ACTION_UNINSTALL_PACKAGE);
            intent.setData(Uri.parse("package:" + packageName));
            intent.putExtra(Intent.EXTRA_RETURN_RESULT, true);
            context.startActivity(intent);
            
            // 延迟检查卸载结果
            mainHandler.postDelayed(() -> {
                // 检查是否卸载成功
                if (!isAppInstalled(context, packageName)) {
                    // 卸载成功
                    callback.onUninstallSuccess(packageName);
                } else {
                    // 可能卸载失败或用户取消
                    callback.onUninstallFailed(packageName, "卸载失败，可能被用户取消");
                }
            }, 3000); // 延迟3秒检查，给用户卸载界面时间
            
        } catch (Exception e) {
            Log.e(TAG, "卸载应用时发生错误：" + e.getMessage());
            callback.onUninstallFailed(packageName, "卸载过程中发生错误：" + e.getMessage());
        }
    }
    
    /**
     * 检查应用是否已安装
     * 
     * @param context 上下文
     * @param packageName 包名
     * @return 是否已安装
     */
    public static boolean isAppInstalled(@NonNull Context context, @NonNull String packageName) {
        try {
            context.getPackageManager().getPackageInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }
    
    /**
     * 获取已安装应用版本信息
     * 
     * @param context 上下文
     * @param packageName 包名
     * @return 应用版本信息，未安装返回null
     */
    @Nullable
    public static AppVersion getAppVersion(@NonNull Context context, @NonNull String packageName) {
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(packageName, 0);
            return new AppVersion(packageInfo.versionName, packageInfo.versionCode);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }
    
    /**
     * 获取APK包信息
     * 
     * @param context 上下文
     * @param apkFile APK文件
     * @return 包信息，错误返回null
     */
    @Nullable
    public static PackageInfo getApkPackageInfo(@NonNull Context context, @NonNull File apkFile) {
        PackageManager pm = context.getPackageManager();
        return pm.getPackageArchiveInfo(apkFile.getAbsolutePath(), PackageManager.GET_ACTIVITIES);
    }
    
    /**
     * 创建安装Intent
     * 
     * @param context 上下文
     * @param apkFile APK文件
     * @return 安装Intent
     */
    @NonNull
    private static Intent createInstallIntent(@NonNull Context context, @NonNull File apkFile) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        
        // Android 7.0及以上需要使用FileProvider
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Uri apkUri = FileProvider.getUriForFile(context, FILE_PROVIDER_AUTHORITY, apkFile);
            intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } else {
            intent.setDataAndType(Uri.fromFile(apkFile), "application/vnd.android.package-archive");
        }
        
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }
    
    /**
     * 解析MPK包
     * 
     * @param context 上下文
     * @param mpkFile MPK文件
     * @return 安装结果，错误返回null
     */
    @Nullable
    private static InstallResult parseMpkPackage(@NonNull Context context, @NonNull File mpkFile) {
        // 创建临时提取目录
        Random random = new Random();
        String extractDirName = "mpk_" + System.currentTimeMillis() + "_" + random.nextInt(1000);
        File extractDir = new File(context.getCacheDir(), extractDirName);
        
        if (!extractDir.mkdirs()) {
            Log.e(TAG, "无法创建提取目录：" + extractDir.getAbsolutePath());
            return null;
        }
        
        ZipInputStream zipIn = null;
        File apkFile = null;
        
        try {
            // 解压MPK文件
            zipIn = new ZipInputStream(new BufferedInputStream(new java.io.FileInputStream(mpkFile)));
            ZipEntry entry;
            
            while ((entry = zipIn.getNextEntry()) != null) {
                String name = entry.getName();
                
                // 找到APK文件
                if (name.toLowerCase().endsWith(APK_EXTENSION)) {
                    apkFile = new File(extractDir, name);
                    
                    // 确保父目录存在
                    File parent = apkFile.getParentFile();
                    if (parent != null && !parent.exists()) {
                        parent.mkdirs();
                    }
                    
                    // 提取APK文件
                    OutputStream out = new BufferedOutputStream(new FileOutputStream(apkFile));
                    byte[] buffer = new byte[4096];
                    int count;
                    
                    while ((count = zipIn.read(buffer)) != -1) {
                        out.write(buffer, 0, count);
                    }
                    
                    out.close();
                    break; // 找到APK后停止
                }
                
                zipIn.closeEntry();
            }
            
            // 检查是否找到APK
            if (apkFile == null || !apkFile.exists()) {
                Log.e(TAG, "MPK包中未找到APK文件");
                FileUtils.deleteDir(extractDir);
                return null;
            }
            
            // 获取APK包信息
            PackageInfo packageInfo = getApkPackageInfo(context, apkFile);
            if (packageInfo == null) {
                Log.e(TAG, "无法获取APK包信息");
                FileUtils.deleteDir(extractDir);
                return null;
            }
            
            // 创建安装结果
            InstallResult result = new InstallResult();
            result.setApkFile(apkFile.getAbsolutePath());
            result.setExtractDir(extractDir.getAbsolutePath());
            result.setPackageName(packageInfo.packageName);
            result.setVersionName(packageInfo.versionName);
            result.setVersionCode(packageInfo.versionCode);
            
            return result;
            
        } catch (IOException e) {
            Log.e(TAG, "解析MPK包时发生错误：" + e.getMessage());
            FileUtils.deleteDir(extractDir);
            return null;
        } finally {
            if (zipIn != null) {
                try {
                    zipIn.close();
                } catch (IOException e) {
                    Log.e(TAG, "关闭ZIP流时发生错误：" + e.getMessage());
                }
            }
        }
    }
    
    /**
     * 安装任务内部类
     */
    private static class InstallTask {
        private final Context context;
        private final File file;
        private final InstallCallback callback;
        
        public InstallTask(Context context, File file, InstallCallback callback) {
            this.context = context;
            this.file = file;
            this.callback = callback;
        }
    }
    
    /**
     * 应用版本信息内部类
     */
    public static class AppVersion {
        public final String versionName;
        public final int versionCode;
        
        public AppVersion(String versionName, int versionCode) {
            this.versionName = versionName;
            this.versionCode = versionCode;
        }
    }
    
    /**
     * 安装回调接口
     */
    public interface InstallCallback {
        /**
         * 安装成功
         * 
         * @param filePath 文件路径
         * @param packageName 包名
         * @param versionName 版本名称
         * @param versionCode 版本号
         */
        void onInstallSuccess(String filePath, String packageName, String versionName, int versionCode);
        
        /**
         * 安装更新成功
         * 
         * @param filePath 文件路径
         * @param packageName 包名
         * @param versionName 版本名称
         * @param versionCode 版本号
         */
        void onInstallUpdated(String filePath, String packageName, String versionName, int versionCode);
        
        /**
         * 安装失败
         * 
         * @param filePath 文件路径
         * @param errorMsg 错误信息
         */
        void onInstallFailed(String filePath, String errorMsg);
    }
    
    /**
     * 卸载回调接口
     */
    public interface UninstallCallback {
        /**
         * 卸载成功
         * 
         * @param packageName 包名
         */
        void onUninstallSuccess(String packageName);
        
        /**
         * 卸载失败
         * 
         * @param packageName 包名
         * @param errorMsg 错误信息
         */
        void onUninstallFailed(String packageName, String errorMsg);
    }
} 