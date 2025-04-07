package com.mobileplatform.creator.utils;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.webkit.MimeTypeMap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;

import com.mobileplatform.creator.MobilePlatformApplication;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.util.UUID;

/**
 * 文件工具类，提供文件操作相关的实用方法
 */
public class FileUtils {
    private static final String TAG = "FileUtils";
    
    private static final String FILE_PROVIDER_AUTHORITY = "com.mobileplatform.creator.fileprovider";
    
    /**
     * 获取应用包目录
     */
    public static File getAppPackagesDir(Context context) {
        File packagesDir = new File(context.getExternalFilesDir(null), "packages");
        if (!packagesDir.exists()) {
            packagesDir.mkdirs();
        }
        return packagesDir;
    }
    
    /**
     * 获取应用缓存目录
     */
    public static File getAppCacheDir(Context context) {
        File cacheDir = context.getExternalCacheDir();
        if (cacheDir == null) {
            cacheDir = context.getCacheDir();
        }
        if (!cacheDir.exists()) {
            cacheDir.mkdirs();
        }
        return cacheDir;
    }
    
    /**
     * 获取临时文件
     */
    public static File getTempFile(Context context, String prefix, String extension) {
        File tempDir = new File(getAppCacheDir(context), "temp");
        if (!tempDir.exists()) {
            tempDir.mkdirs();
        }
        
        String fileName = prefix + "_" + UUID.randomUUID().toString() + "." + extension;
        return new File(tempDir, fileName);
    }
    
    /**
     * 根据文件名获取MIME类型
     */
    public static String getMimeType(String fileName) {
        String extension = getFileExtension(fileName);
        if (extension != null && !extension.isEmpty()) {
            return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.toLowerCase());
        }
        return "application/octet-stream";
    }
    
    /**
     * 获取文件扩展名
     */
    public static String getFileExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "";
        }
        
        int lastDotIndex = fileName.lastIndexOf(".");
        if (lastDotIndex != -1 && lastDotIndex < fileName.length() - 1) {
            return fileName.substring(lastDotIndex + 1);
        }
        return "";
    }
    
    /**
     * 格式化文件大小
     */
    public static String formatFileSize(long size) {
        if (size <= 0) {
            return "0 B";
        }
        
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        
        // 限制在可用单位范围内
        digitGroups = Math.min(digitGroups, units.length - 1);
        
        DecimalFormat df = new DecimalFormat("#,##0.##");
        return df.format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }
    
    /**
     * 复制文件
     */
    public static boolean copyFile(File source, File destination) {
        try (FileInputStream in = new FileInputStream(source);
             FileOutputStream out = new FileOutputStream(destination)) {
            
            byte[] buffer = new byte[4096];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            return true;
        } catch (IOException e) {
            Log.e(TAG, "复制文件失败", e);
            return false;
        }
    }
    
    /**
     * 删除文件或目录
     */
    public static boolean deleteFileOrDir(File fileOrDir) {
        if (fileOrDir == null || !fileOrDir.exists()) {
            return true;
        }
        
        if (fileOrDir.isDirectory()) {
            File[] files = fileOrDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    deleteFileOrDir(file);
                }
            }
        }
        
        return fileOrDir.delete();
    }
    
    /**
     * 创建与文件共享的URI
     */
    public static Uri getShareableUri(Context context, File file) {
        return FileProvider.getUriForFile(context, FILE_PROVIDER_AUTHORITY, file);
    }
    
    /**
     * 将文件保存到下载目录
     */
    public static Uri saveToDownloads(Context context, File sourceFile, String fileName, String mimeType) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return saveToDownloadsAPI29(context, sourceFile, fileName, mimeType);
        } else {
            return saveToDownloadsLegacy(context, sourceFile, fileName);
        }
    }
    
    /**
     * Android 10及以上，使用MediaStore保存文件到下载目录
     */
    private static Uri saveToDownloadsAPI29(Context context, File sourceFile, String fileName, String mimeType) {
        ContentResolver resolver = context.getContentResolver();
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
        contentValues.put(MediaStore.Downloads.MIME_TYPE, mimeType);
        contentValues.put(MediaStore.Downloads.SIZE, sourceFile.length());
        
        Uri downloadUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues);
        if (downloadUri != null) {
            try (InputStream in = new FileInputStream(sourceFile);
                 OutputStream out = resolver.openOutputStream(downloadUri)) {
                
                if (out != null) {
                    byte[] buffer = new byte[4096];
                    int read;
                    while ((read = in.read(buffer)) != -1) {
                        out.write(buffer, 0, read);
                    }
                    return downloadUri;
                }
            } catch (IOException e) {
                Log.e(TAG, "保存到下载目录失败", e);
            }
        }
        return null;
    }
    
    /**
     * Android 9及以下，直接保存文件到下载目录
     */
    private static Uri saveToDownloadsLegacy(Context context, File sourceFile, String fileName) {
        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        if (!downloadsDir.exists()) {
            downloadsDir.mkdirs();
        }
        
        File destFile = new File(downloadsDir, fileName);
        
        if (copyFile(sourceFile, destFile)) {
            return Uri.fromFile(destFile);
        }
        
        return null;
    }
    
    /**
     * 清除应用缓存
     */
    public static boolean clearCache(Context context) {
        try {
            File cacheDir = context.getCacheDir();
            File externalCacheDir = context.getExternalCacheDir();
            
            boolean internalResult = deleteFileOrDir(cacheDir);
            boolean externalResult = deleteFileOrDir(externalCacheDir);
            
            return internalResult && externalResult;
        } catch (Exception e) {
            Log.e(TAG, "清除缓存失败", e);
            return false;
        }
    }
} 