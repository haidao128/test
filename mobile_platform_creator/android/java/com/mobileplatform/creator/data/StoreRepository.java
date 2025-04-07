package com.mobileplatform.creator.data;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.mobileplatform.creator.R;
import com.mobileplatform.creator.data.model.StoreAppInfo;
import com.mobileplatform.creator.utils.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * 商店仓库，提供应用商店数据访问功能
 */
public class StoreRepository {
    private static final String TAG = "StoreRepository";
    
    private static volatile StoreRepository instance;
    
    private final Executor executor = Executors.newCachedThreadPool();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    
    // 缓存应用列表
    private final Map<String, StoreAppInfo> appCache = new HashMap<>();
    private final Map<String, DownloadTask> downloadTasks = new HashMap<>();
    
    /**
     * 私有构造函数，防止外部实例化
     */
    private StoreRepository() {
    }
    
    /**
     * 获取StoreRepository单例实例
     */
    public static StoreRepository getInstance() {
        if (instance == null) {
            synchronized (StoreRepository.class) {
                if (instance == null) {
                    instance = new StoreRepository();
                }
            }
        }
        return instance;
    }
    
    /**
     * 获取商店应用列表
     */
    public void getStoreApps(Context context, StoreAppListCallback callback) {
        executor.execute(() -> {
            try {
                if (appCache.isEmpty()) {
                    // 加载示例应用数据
                    loadSampleApps(context);
                }
                
                // 检查安装状态
                AppRepository appRepo = AppRepository.getInstance();
                for (StoreAppInfo app : appCache.values()) {
                    app.setInstalled(appRepo.isAppInstalled(context, app.getPackageName()));
                }
                
                // 返回应用列表
                List<StoreAppInfo> appList = new ArrayList<>(appCache.values());
                mainHandler.post(() -> callback.onResult(appList, null));
                
            } catch (Exception e) {
                Log.e(TAG, "获取商店应用列表失败", e);
                mainHandler.post(() -> callback.onResult(null, "获取商店应用列表失败: " + e.getMessage()));
            }
        });
    }
    
    /**
     * 搜索商店应用
     */
    public void searchStoreApps(Context context, String query, StoreAppListCallback callback) {
        executor.execute(() -> {
            try {
                // 确保应用数据已加载
                if (appCache.isEmpty()) {
                    loadSampleApps(context);
                }
                
                // 过滤应用
                List<StoreAppInfo> results = new ArrayList<>();
                String lowerQuery = query.toLowerCase();
                
                for (StoreAppInfo app : appCache.values()) {
                    // 检查名称、描述和开发者是否包含查询文本
                    if (app.getName().toLowerCase().contains(lowerQuery) ||
                            app.getDescription().toLowerCase().contains(lowerQuery) ||
                            app.getDeveloper().toLowerCase().contains(lowerQuery)) {
                        results.add(app);
                    }
                }
                
                // 返回搜索结果
                mainHandler.post(() -> callback.onResult(results, null));
                
            } catch (Exception e) {
                Log.e(TAG, "搜索商店应用失败", e);
                mainHandler.post(() -> callback.onResult(null, "搜索商店应用失败: " + e.getMessage()));
            }
        });
    }
    
    /**
     * 下载应用
     */
    public void downloadApp(Context context, String appId, DownloadCallback callback) {
        executor.execute(() -> {
            try {
                // 获取应用信息
                StoreAppInfo appInfo = appCache.get(appId);
                if (appInfo == null) {
                    throw new Exception("找不到应用: " + appId);
                }
                
                // 检查是否已在下载队列中
                if (downloadTasks.containsKey(appId)) {
                    throw new Exception("应用已在下载队列中");
                }
                
                // 创建下载任务
                DownloadTask task = new DownloadTask(context, appInfo, callback);
                downloadTasks.put(appId, task);
                
                // 开始下载
                task.start();
                
            } catch (Exception e) {
                Log.e(TAG, "下载应用失败", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }
    
    /**
     * 取消下载
     */
    public void cancelDownload(String appId) {
        DownloadTask task = downloadTasks.get(appId);
        if (task != null) {
            task.cancel();
            downloadTasks.remove(appId);
        }
    }
    
    /**
     * 获取厂商推荐应用
     */
    public void getFeaturedApps(Context context, StoreAppListCallback callback) {
        executor.execute(() -> {
            try {
                // 确保应用数据已加载
                if (appCache.isEmpty()) {
                    loadSampleApps(context);
                }
                
                // 选择前3个应用作为推荐应用
                List<StoreAppInfo> featuredApps = new ArrayList<>();
                int count = 0;
                for (StoreAppInfo app : appCache.values()) {
                    featuredApps.add(app);
                    count++;
                    if (count >= 3) break;
                }
                
                // 返回推荐应用列表
                mainHandler.post(() -> callback.onResult(featuredApps, null));
                
            } catch (Exception e) {
                Log.e(TAG, "获取推荐应用失败", e);
                mainHandler.post(() -> callback.onResult(null, "获取推荐应用失败: " + e.getMessage()));
            }
        });
    }
    
    /**
     * 加载示例应用数据
     */
    private void loadSampleApps(Context context) {
        // 清空缓存
        appCache.clear();
        
        // 加载示例图标
        Drawable defaultIcon = ContextCompat.getDrawable(context, R.drawable.ic_app_placeholder);
        
        // 添加示例应用
        addSampleApp(
                "store1",
                "计算器",
                "com.example.calculator",
                "1.0.0",
                1024 * 1024, // 1MB
                "简单易用的计算器应用，支持基础运算和科学计算。",
                "Example Dev",
                "https://example.com/apps/calculator.mpk",
                defaultIcon
        );
        
        addSampleApp(
                "store2",
                "笔记本",
                "com.example.notes",
                "2.1.0",
                2 * 1024 * 1024, // 2MB
                "功能丰富的笔记应用，支持文本、图片和录音。",
                "Note Studio",
                "https://example.com/apps/notes.mpk",
                defaultIcon
        );
        
        addSampleApp(
                "store3",
                "天气预报",
                "com.example.weather",
                "1.5.2",
                3 * 1024 * 1024, // 3MB
                "准确的天气预报应用，提供未来一周的天气预测。",
                "Weather Labs",
                "https://example.com/apps/weather.mpk",
                defaultIcon
        );
        
        addSampleApp(
                "store4",
                "图片浏览器",
                "com.example.gallery",
                "3.0.1",
                4 * 1024 * 1024, // 4MB
                "强大的图片浏览和编辑工具，支持多种滤镜和特效。",
                "Picture Tools",
                "https://example.com/apps/gallery.mpk",
                defaultIcon
        );
        
        addSampleApp(
                "store5",
                "音乐播放器",
                "com.example.music",
                "2.3.0",
                5 * 1024 * 1024, // 5MB
                "高品质音乐播放器，支持多种音频格式和均衡器设置。",
                "Music World",
                "https://example.com/apps/music.mpk",
                defaultIcon
        );
    }
    
    /**
     * 添加示例应用
     */
    private void addSampleApp(String id, String name, String packageName, String version,
                              long size, String description, String developer,
                              String downloadUrl, Drawable icon) {
        StoreAppInfo app = new StoreAppInfo(
                id, name, packageName, version, size, description, developer, downloadUrl, icon
        );
        appCache.put(id, app);
    }
    
    /**
     * 下载任务
     */
    private class DownloadTask {
        private final Context context;
        private final StoreAppInfo appInfo;
        private final DownloadCallback callback;
        private boolean isCancelled;
        
        public DownloadTask(Context context, StoreAppInfo appInfo, DownloadCallback callback) {
            this.context = context;
            this.appInfo = appInfo;
            this.callback = callback;
            this.isCancelled = false;
        }
        
        /**
         * 开始下载
         */
        public void start() {
            // 更新状态
            appInfo.setDownloading(true);
            appInfo.setDownloadProgress(0);
            
            // 通知下载开始
            mainHandler.post(() -> callback.onStart(appInfo.getId()));
            
            // 创建下载目录
            File packagesDir = FileUtils.getAppPackagesDir(context);
            String fileName = appInfo.getPackageName() + "_" + appInfo.getVersion() + ".mpk";
            File outputFile = new File(packagesDir, fileName);
            
            try {
                // 在实际应用中这里会进行实际的网络下载
                // 这里模拟下载过程
                simulateDownload(outputFile);
                
                // 下载完成
                if (!isCancelled) {
                    appInfo.setDownloading(false);
                    appInfo.setDownloadProgress(100);
                    
                    // 通知下载完成
                    mainHandler.post(() -> callback.onComplete(appInfo.getId(), outputFile.getAbsolutePath()));
                }
                
            } catch (Exception e) {
                if (!isCancelled) {
                    appInfo.setDownloading(false);
                    appInfo.setDownloadProgress(0);
                    
                    Log.e(TAG, "下载失败", e);
                    mainHandler.post(() -> callback.onError("下载失败: " + e.getMessage()));
                }
            } finally {
                downloadTasks.remove(appInfo.getId());
            }
        }
        
        /**
         * 模拟下载过程
         */
        private void simulateDownload(File outputFile) throws IOException, InterruptedException {
            // 创建空文件
            if (!outputFile.exists()) {
                outputFile.createNewFile();
            }
            
            // 模拟写入数据
            try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                byte[] buffer = new byte[4096];
                long fileSize = appInfo.getSize();
                long bytesWritten = 0;
                
                while (bytesWritten < fileSize) {
                    // 检查是否取消
                    if (isCancelled) {
                        outputFile.delete();
                        return;
                    }
                    
                    // 写入一部分数据
                    int chunkSize = (int) Math.min(buffer.length, fileSize - bytesWritten);
                    fos.write(buffer, 0, chunkSize);
                    bytesWritten += chunkSize;
                    
                    // 更新进度
                    int progress = (int) (bytesWritten * 100 / fileSize);
                    appInfo.setDownloadProgress(progress);
                    
                    // 通知进度更新
                    final int currentProgress = progress;
                    mainHandler.post(() -> callback.onProgress(appInfo.getId(), currentProgress));
                    
                    // 延迟一段时间
                    Thread.sleep(200);
                }
            }
        }
        
        /**
         * 取消下载
         */
        public void cancel() {
            isCancelled = true;
            appInfo.setDownloading(false);
            appInfo.setDownloadProgress(0);
            
            // 通知取消
            mainHandler.post(() -> callback.onCancel(appInfo.getId()));
        }
    }
    
    /**
     * 商店应用列表回调接口
     */
    public interface StoreAppListCallback {
        void onResult(List<StoreAppInfo> apps, String error);
    }
    
    /**
     * 下载回调接口
     */
    public interface DownloadCallback {
        void onStart(String appId);
        void onProgress(String appId, int progress);
        void onComplete(String appId, String filePath);
        void onCancel(String appId);
        void onError(String error);
    }
} 