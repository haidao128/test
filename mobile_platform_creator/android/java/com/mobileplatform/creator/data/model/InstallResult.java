package com.mobileplatform.creator.data.model;

import android.util.Log;

import java.io.File;

/**
 * 安装结果数据模型
 * 存储应用安装结果信息
 */
public class InstallResult {
    private static final String TAG = "InstallResult";
    
    // 安装状态常量
    public static final int STATUS_SUCCESS = 0;
    public static final int STATUS_FAILED = 1;
    public static final int STATUS_UPDATED = 2;
    public static final int STATUS_IN_PROGRESS = 3;
    
    // APK文件路径
    private String apkFile;
    
    // 解压目录路径
    private String extractDir;
    
    // 包名
    private String packageName;
    
    // 版本名称
    private String versionName;
    
    // 版本号
    private int versionCode;
    
    // 安装状态
    private int status = STATUS_IN_PROGRESS;
    
    // 错误信息
    private String errorMessage;

    /**
     * 默认构造函数
     */
    public InstallResult() {
        this.status = STATUS_IN_PROGRESS;
    }
    
    /**
     * 创建成功的安装结果
     * 
     * @param apkFile APK文件路径
     * @param extractDir 解压目录
     * @param packageName 包名
     * @param versionName 版本名称
     * @param versionCode 版本号
     * @return 安装结果
     */
    public static InstallResult success(String apkFile, String extractDir, String packageName, 
                                        String versionName, int versionCode) {
        InstallResult result = new InstallResult();
        result.apkFile = apkFile;
        result.extractDir = extractDir;
        result.packageName = packageName;
        result.versionName = versionName;
        result.versionCode = versionCode;
        result.status = STATUS_SUCCESS;
        return result;
    }
    
    /**
     * 创建更新的安装结果
     * 
     * @param apkFile APK文件路径
     * @param extractDir 解压目录
     * @param packageName 包名
     * @param versionName 版本名称
     * @param versionCode 版本号
     * @return 安装结果
     */
    public static InstallResult update(String apkFile, String extractDir, String packageName, 
                                        String versionName, int versionCode) {
        InstallResult result = new InstallResult();
        result.apkFile = apkFile;
        result.extractDir = extractDir;
        result.packageName = packageName;
        result.versionName = versionName;
        result.versionCode = versionCode;
        result.status = STATUS_UPDATED;
        return result;
    }
    
    /**
     * 创建失败的安装结果
     * 
     * @param apkFile APK文件路径
     * @param extractDir 解压目录
     * @param errorMessage 错误信息
     * @return 安装结果
     */
    public static InstallResult failure(String apkFile, String extractDir, String errorMessage) {
        InstallResult result = new InstallResult();
        result.apkFile = apkFile;
        result.extractDir = extractDir;
        result.status = STATUS_FAILED;
        result.errorMessage = errorMessage;
        return result;
    }

    /**
     * 获取APK文件路径
     * 
     * @return APK文件路径
     */
    public String getApkFile() {
        return apkFile;
    }

    /**
     * 设置APK文件路径
     * 
     * @param apkFile APK文件路径
     */
    public void setApkFile(String apkFile) {
        this.apkFile = apkFile;
    }

    /**
     * 获取解压目录路径
     * 
     * @return 解压目录路径
     */
    public String getExtractDir() {
        return extractDir;
    }

    /**
     * 设置解压目录路径
     * 
     * @param extractDir 解压目录路径
     */
    public void setExtractDir(String extractDir) {
        this.extractDir = extractDir;
    }

    /**
     * 获取包名
     * 
     * @return 包名
     */
    public String getPackageName() {
        return packageName;
    }

    /**
     * 设置包名
     * 
     * @param packageName 包名
     */
    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    /**
     * 获取版本名称
     * 
     * @return 版本名称
     */
    public String getVersionName() {
        return versionName;
    }

    /**
     * 设置版本名称
     * 
     * @param versionName 版本名称
     */
    public void setVersionName(String versionName) {
        this.versionName = versionName;
    }

    /**
     * 获取版本号
     * 
     * @return 版本号
     */
    public int getVersionCode() {
        return versionCode;
    }

    /**
     * 设置版本号
     * 
     * @param versionCode 版本号
     */
    public void setVersionCode(int versionCode) {
        this.versionCode = versionCode;
    }

    /**
     * 获取安装状态
     * 
     * @return 安装状态
     */
    public int getStatus() {
        return status;
    }

    /**
     * 设置安装状态
     * 
     * @param status 安装状态
     */
    public void setStatus(int status) {
        this.status = status;
    }

    /**
     * 获取错误信息
     * 
     * @return 错误信息
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * 设置错误信息
     * 
     * @param errorMessage 错误信息
     */
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    /**
     * 是否安装成功
     * 
     * @return 是否成功
     */
    public boolean isSuccess() {
        return status == STATUS_SUCCESS;
    }
    
    /**
     * 是否为更新安装
     * 
     * @return 是否为更新
     */
    public boolean isUpdate() {
        return status == STATUS_UPDATED;
    }
    
    /**
     * 是否安装失败
     * 
     * @return 是否失败
     */
    public boolean isFailed() {
        return status == STATUS_FAILED;
    }
    
    /**
     * 是否正在安装
     * 
     * @return 是否进行中
     */
    public boolean isInProgress() {
        return status == STATUS_IN_PROGRESS;
    }
    
    /**
     * 清理临时文件
     * 删除解压目录和APK文件
     */
    public void cleanup() {
        if (extractDir != null) {
            try {
                File dir = new File(extractDir);
                deleteRecursive(dir);
                Log.d(TAG, "已清理临时目录: " + extractDir);
            } catch (Exception e) {
                Log.e(TAG, "清理临时目录失败: " + e.getMessage());
            }
        }
    }
    
    /**
     * 递归删除文件或目录
     * 
     * @param fileOrDir 文件或目录
     * @return 是否成功删除
     */
    private boolean deleteRecursive(File fileOrDir) {
        if (fileOrDir.isDirectory()) {
            for (File child : fileOrDir.listFiles()) {
                deleteRecursive(child);
            }
        }
        return fileOrDir.delete();
    }
} 