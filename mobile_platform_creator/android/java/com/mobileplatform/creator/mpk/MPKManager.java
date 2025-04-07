package com.mobileplatform.creator.mpk;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.mobileplatform.creator.data.model.AppInfo;
import com.mobileplatform.creator.data.model.MPKPackage;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * MPK包管理器
 * 
 * 负责MPK应用包的创建、解析、安装、卸载等操作
 */
public class MPKManager {
    private static final String TAG = "MPKManager";
    
    // 单例实例
    private static MPKManager instance;
    
    // 上下文对象
    private Context context;
    
    // 线程池
    private final Executor executor = Executors.newCachedThreadPool();
    
    // 主线程处理器
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    
    // MPK包临时目录
    private File tempDir;
    
    // MPK包安装目录
    private File installDir;
    
    /**
     * 私有构造函数
     */
    private MPKManager(Context context) {
        this.context = context.getApplicationContext();
        this.tempDir = new File(context.getCacheDir(), "mpk_temp");
        this.installDir = new File(context.getFilesDir(), "mpk_installed");
        
        // 确保目录存在
        if (!tempDir.exists()) {
            tempDir.mkdirs();
        }
        
        if (!installDir.exists()) {
            installDir.mkdirs();
        }
    }
    
    /**
     * 获取单例实例
     */
    public static synchronized MPKManager getInstance(Context context) {
        if (instance == null) {
            instance = new MPKManager(context);
        }
        return instance;
    }
    
    /**
     * 解析MPK文件
     * 
     * @param mpkFile MPK文件
     * @param callback 回调接口
     */
    public void parseMPK(File mpkFile, MPKParseCallback callback) {
        if (mpkFile == null || !mpkFile.exists() || !mpkFile.isFile()) {
            if (callback != null) {
                mainHandler.post(() -> callback.onError("无效的MPK文件"));
            }
            return;
        }
        
        // 在后台线程执行解析
        executor.execute(() -> {
            try {
                // 创建临时目录
                File extractDir = new File(tempDir, UUID.randomUUID().toString());
                if (!extractDir.exists()) {
                    extractDir.mkdirs();
                }
                
                // 解压MPK文件
                extractMPK(mpkFile, extractDir);
                
                // 读取清单文件
                File manifestFile = new File(extractDir, "manifest.json");
                if (!manifestFile.exists()) {
                    if (callback != null) {
                        mainHandler.post(() -> callback.onError("MPK文件格式错误：缺少manifest.json"));
                    }
                    cleanupTempDir(extractDir);
                    return;
                }
                
                // 读取清单内容
                String manifestJson = readFile(manifestFile);
                try {
                    JSONObject manifest = new JSONObject(manifestJson);
                    
                    // 创建MPK对象
                    MPKPackage mpkPackage = new MPKPackage(manifest);
                    mpkPackage.setFilePath(mpkFile.getAbsolutePath());
                    
                    // 扫描文件列表
                    List<String> fileList = scanFiles(extractDir);
                    mpkPackage.setFileList(fileList);
                    
                    // 回调结果
                    if (callback != null) {
                        mainHandler.post(() -> callback.onSuccess(mpkPackage));
                    }
                    
                } catch (JSONException e) {
                    if (callback != null) {
                        mainHandler.post(() -> callback.onError("解析manifest.json失败: " + e.getMessage()));
                    }
                } finally {
                    cleanupTempDir(extractDir);
                }
                
            } catch (IOException e) {
                if (callback != null) {
                    mainHandler.post(() -> callback.onError("解压MPK文件失败: " + e.getMessage()));
                }
            }
        });
    }
    
    /**
     * 创建MPK包
     * 
     * @param sourceDir 源目录
     * @param outputFile 输出文件
     * @param mpkPackage MPK包对象
     * @param callback 回调接口
     */
    public void createMPK(File sourceDir, File outputFile, MPKPackage mpkPackage, MPKCreateCallback callback) {
        if (sourceDir == null || !sourceDir.exists() || !sourceDir.isDirectory()) {
            if (callback != null) {
                mainHandler.post(() -> callback.onError("无效的源目录"));
            }
            return;
        }
        
        // 在后台线程执行创建
        executor.execute(() -> {
            try {
                // 确保输出目录存在
                File parentDir = outputFile.getParentFile();
                if (parentDir != null && !parentDir.exists()) {
                    parentDir.mkdirs();
                }
                
                // 创建临时目录
                File tempPackageDir = new File(tempDir, UUID.randomUUID().toString());
                if (!tempPackageDir.exists()) {
                    tempPackageDir.mkdirs();
                }
                
                try {
                    // 复制源目录内容到临时目录
                    copyDirectory(sourceDir, tempPackageDir);
                    
                    // 创建/更新manifest.json
                    File manifestFile = new File(tempPackageDir, "manifest.json");
                    try (FileOutputStream fos = new FileOutputStream(manifestFile)) {
                        fos.write(mpkPackage.getManifest().toString(2).getBytes("UTF-8"));
                    }
                    
                    // 生成签名文件
                    File signatureFile = new File(tempPackageDir, "signature.sig");
                    generateSignature(tempPackageDir, signatureFile);
                    
                    // 打包为ZIP文件
                    zipDirectory(tempPackageDir, outputFile);
                    
                    // 更新MPK对象的文件路径
                    mpkPackage.setFilePath(outputFile.getAbsolutePath());
                    
                    // 回调成功
                    if (callback != null) {
                        mainHandler.post(() -> callback.onSuccess(outputFile));
                    }
                    
                } finally {
                    // 清理临时目录
                    cleanupTempDir(tempPackageDir);
                }
                
            } catch (Exception e) {
                Log.e(TAG, "创建MPK包失败", e);
                if (callback != null) {
                    mainHandler.post(() -> callback.onError("创建MPK包失败: " + e.getMessage()));
                }
            }
        });
    }
    
    /**
     * 安装MPK包
     * 
     * @param mpkFile MPK文件
     * @param callback 回调接口
     */
    public void installMPK(File mpkFile, MPKInstallCallback callback) {
        if (mpkFile == null || !mpkFile.exists() || !mpkFile.isFile()) {
            if (callback != null) {
                mainHandler.post(() -> callback.onError("无效的MPK文件"));
            }
            return;
        }
        
        // 在后台线程执行安装
        executor.execute(() -> {
            try {
                // 解析MPK文件
                parseMPK(mpkFile, new MPKParseCallback() {
                    @Override
                    public void onSuccess(MPKPackage mpkPackage) {
                        // 创建应用安装目录
                        File appDir = new File(installDir, mpkPackage.getId());
                        if (appDir.exists()) {
                            // 如果已存在，先删除
                            deleteDirectory(appDir);
                        }
                        appDir.mkdirs();
                        
                        try {
                            // 解压MPK文件到安装目录
                            extractMPK(mpkFile, appDir);
                            
                            // 创建应用信息对象
                            AppInfo appInfo = createAppInfoFromMPK(mpkPackage, appDir);
                            
                            // 回调成功
                            if (callback != null) {
                                mainHandler.post(() -> callback.onSuccess(appInfo));
                            }
                            
                        } catch (IOException e) {
                            Log.e(TAG, "解压MPK文件到安装目录失败", e);
                            if (callback != null) {
                                mainHandler.post(() -> callback.onError("安装失败: " + e.getMessage()));
                            }
                        }
                    }
                    
                    @Override
                    public void onError(String errorMessage) {
                        if (callback != null) {
                            mainHandler.post(() -> callback.onError("解析MPK失败: " + errorMessage));
                        }
                    }
                });
                
            } catch (Exception e) {
                Log.e(TAG, "安装MPK包失败", e);
                if (callback != null) {
                    mainHandler.post(() -> callback.onError("安装MPK包失败: " + e.getMessage()));
                }
            }
        });
    }
    
    /**
     * 卸载MPK包
     * 
     * @param packageId 包ID
     * @param callback 回调接口
     */
    public void uninstallMPK(String packageId, MPKUninstallCallback callback) {
        if (packageId == null || packageId.isEmpty()) {
            if (callback != null) {
                mainHandler.post(() -> callback.onError("无效的包ID"));
            }
            return;
        }
        
        // 在后台线程执行卸载
        executor.execute(() -> {
            try {
                // 获取应用目录
                File appDir = new File(installDir, packageId);
                if (!appDir.exists() || !appDir.isDirectory()) {
                    if (callback != null) {
                        mainHandler.post(() -> callback.onError("应用未安装"));
                    }
                    return;
                }
                
                // 删除应用目录
                boolean success = deleteDirectory(appDir);
                
                if (success) {
                    // 回调成功
                    if (callback != null) {
                        mainHandler.post(() -> callback.onSuccess(packageId));
                    }
                } else {
                    // 回调失败
                    if (callback != null) {
                        mainHandler.post(() -> callback.onError("删除应用目录失败"));
                    }
                }
                
            } catch (Exception e) {
                Log.e(TAG, "卸载MPK包失败", e);
                if (callback != null) {
                    mainHandler.post(() -> callback.onError("卸载MPK包失败: " + e.getMessage()));
                }
            }
        });
    }
    
    /**
     * 从MPK包创建应用信息对象
     */
    private AppInfo createAppInfoFromMPK(MPKPackage mpkPackage, File appDir) {
        AppInfo appInfo = new AppInfo(
                mpkPackage.getId(),
                mpkPackage.getName(),
                mpkPackage.getId(),
                mpkPackage.getVersion(),
                calculateDirSize(appDir),
                null,
                appDir.getAbsolutePath()
        );
        
        // 设置安装和更新时间
        long currentTime = System.currentTimeMillis();
        appInfo.setInstallTime(currentTime);
        appInfo.setUpdateTime(currentTime);
        
        return appInfo;
    }
    
    /**
     * 解压MPK文件
     */
    private void extractMPK(File mpkFile, File targetDir) throws IOException {
        try (ZipFile zipFile = new ZipFile(mpkFile)) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                File entryFile = new File(targetDir, entry.getName());
                
                if (entry.isDirectory()) {
                    entryFile.mkdirs();
                } else {
                    // 确保父目录存在
                    File parent = entryFile.getParentFile();
                    if (parent != null && !parent.exists()) {
                        parent.mkdirs();
                    }
                    
                    // 解压文件
                    try (InputStream in = new BufferedInputStream(zipFile.getInputStream(entry));
                         OutputStream out = new BufferedOutputStream(new FileOutputStream(entryFile))) {
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = in.read(buffer)) != -1) {
                            out.write(buffer, 0, bytesRead);
                        }
                    }
                }
            }
        }
    }
    
    /**
     * 打包目录为ZIP文件
     */
    private void zipDirectory(File sourceDir, File zipFile) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
            addDirToZip(zos, sourceDir, sourceDir.getPath());
        }
    }
    
    /**
     * 添加目录到ZIP流
     */
    private void addDirToZip(ZipOutputStream zos, File sourceDir, String basePath) throws IOException {
        File[] files = sourceDir.listFiles();
        if (files == null) return;
        
        byte[] buffer = new byte[4096];
        
        for (File file : files) {
            if (file.isDirectory()) {
                addDirToZip(zos, file, basePath);
            } else {
                String entryName = file.getPath().substring(basePath.length() + 1).replace('\\', '/');
                ZipEntry entry = new ZipEntry(entryName);
                zos.putNextEntry(entry);
                
                try (FileInputStream fis = new FileInputStream(file)) {
                    int bytesRead;
                    while ((bytesRead = fis.read(buffer)) != -1) {
                        zos.write(buffer, 0, bytesRead);
                    }
                }
                
                zos.closeEntry();
            }
        }
    }
    
    /**
     * 生成签名文件
     */
    private void generateSignature(File sourceDir, File signatureFile) throws IOException, NoSuchAlgorithmException {
        // 计算目录内容的SHA-256哈希
        String hash = calculateDirectoryHash(sourceDir);
        
        // 保存哈希值到文件
        try (FileOutputStream fos = new FileOutputStream(signatureFile)) {
            fos.write(hash.getBytes("UTF-8"));
        }
    }
    
    /**
     * 计算目录的哈希值
     */
    private String calculateDirectoryHash(File dir) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        
        // 获取所有文件，排除signature.sig
        List<File> files = new ArrayList<>();
        collectFiles(dir, files, "signature.sig");
        
        // 按路径排序，确保结果一致
        files.sort((f1, f2) -> f1.getPath().compareTo(f2.getPath()));
        
        // 计算每个文件的哈希，并更新摘要
        byte[] buffer = new byte[4096];
        for (File file : files) {
            String relativePath = file.getPath().substring(dir.getPath().length() + 1);
            digest.update(relativePath.getBytes("UTF-8"));
            
            try (FileInputStream fis = new FileInputStream(file)) {
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    digest.update(buffer, 0, bytesRead);
                }
            }
        }
        
        // 转换为十六进制字符串
        byte[] hashBytes = digest.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : hashBytes) {
            sb.append(String.format("%02x", b));
        }
        
        return sb.toString();
    }
    
    /**
     * 收集目录中的所有文件
     */
    private void collectFiles(File dir, List<File> files, String... excludes) {
        File[] dirFiles = dir.listFiles();
        if (dirFiles == null) return;
        
        for (File file : dirFiles) {
            // 检查是否在排除列表中
            boolean skip = false;
            for (String exclude : excludes) {
                if (file.getName().equals(exclude)) {
                    skip = true;
                    break;
                }
            }
            
            if (skip) continue;
            
            if (file.isDirectory()) {
                collectFiles(file, files, excludes);
            } else {
                files.add(file);
            }
        }
    }
    
    /**
     * 扫描目录中的所有文件
     */
    private List<String> scanFiles(File dir) {
        List<String> fileList = new ArrayList<>();
        scanFilesRecursive(dir, fileList, dir.getPath());
        return fileList;
    }
    
    /**
     * 递归扫描目录中的文件
     */
    private void scanFilesRecursive(File dir, List<String> fileList, String basePath) {
        File[] files = dir.listFiles();
        if (files == null) return;
        
        for (File file : files) {
            if (file.isDirectory()) {
                scanFilesRecursive(file, fileList, basePath);
            } else {
                String relativePath = file.getPath().substring(basePath.length() + 1);
                fileList.add(relativePath.replace('\\', '/'));
            }
        }
    }
    
    /**
     * 复制目录
     */
    private void copyDirectory(File sourceDir, File targetDir) throws IOException {
        if (!targetDir.exists()) {
            targetDir.mkdirs();
        }
        
        File[] files = sourceDir.listFiles();
        if (files == null) return;
        
        for (File file : files) {
            File destFile = new File(targetDir, file.getName());
            
            if (file.isDirectory()) {
                copyDirectory(file, destFile);
            } else {
                copyFile(file, destFile);
            }
        }
    }
    
    /**
     * 复制文件
     */
    private void copyFile(File sourceFile, File targetFile) throws IOException {
        try (FileInputStream fis = new FileInputStream(sourceFile);
             FileOutputStream fos = new FileOutputStream(targetFile)) {
            
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
        }
    }
    
    /**
     * 删除目录
     */
    private boolean deleteDirectory(File dir) {
        if (dir == null || !dir.exists()) {
            return true;
        }
        
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        
        return dir.delete();
    }
    
    /**
     * 清理临时目录
     */
    private void cleanupTempDir(File dir) {
        if (dir != null && dir.exists()) {
            deleteDirectory(dir);
        }
    }
    
    /**
     * 读取文件内容
     */
    private String readFile(File file) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                sb.append(new String(buffer, 0, bytesRead, "UTF-8"));
            }
        }
        return sb.toString();
    }
    
    /**
     * 计算目录大小
     */
    private long calculateDirSize(File dir) {
        long size = 0;
        
        File[] files = dir.listFiles();
        if (files == null) return 0;
        
        for (File file : files) {
            if (file.isDirectory()) {
                size += calculateDirSize(file);
            } else {
                size += file.length();
            }
        }
        
        return size;
    }
    
    /**
     * MPK解析回调接口
     */
    public interface MPKParseCallback {
        /**
         * 解析成功
         */
        void onSuccess(MPKPackage mpkPackage);
        
        /**
         * 解析失败
         */
        void onError(String errorMessage);
    }
    
    /**
     * MPK创建回调接口
     */
    public interface MPKCreateCallback {
        /**
         * 创建成功
         */
        void onSuccess(File mpkFile);
        
        /**
         * 创建失败
         */
        void onError(String errorMessage);
    }
    
    /**
     * MPK安装回调接口
     */
    public interface MPKInstallCallback {
        /**
         * 安装成功
         */
        void onSuccess(AppInfo appInfo);
        
        /**
         * 安装失败
         */
        void onError(String errorMessage);
    }
    
    /**
     * MPK卸载回调接口
     */
    public interface MPKUninstallCallback {
        /**
         * 卸载成功
         */
        void onSuccess(String packageId);
        
        /**
         * 卸载失败
         */
        void onError(String errorMessage);
    }
} 