package com.mobileplatform.creator.update;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.mobileplatform.creator.data.model.AppInfo;
import com.mobileplatform.creator.data.model.StoreAppInfo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * 应用更新检查器
 * 
 * 负责检查已安装应用是否有新版本可用
 */
public class UpdateChecker {
    private static final String TAG = "UpdateChecker";
    
    // 单例实例
    private static UpdateChecker instance;
    
    // 上下文对象
    private Context context;
    
    // 线程池
    private final Executor executor = Executors.newCachedThreadPool();
    
    // 主线程处理器
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    
    // 更新检查监听器列表
    private final List<UpdateCheckListener> listeners = new ArrayList<>();
    
    // 更新检查缓存
    private final Map<String, StoreAppInfo> updateCache = new HashMap<>();
    
    /**
     * 私有构造函数
     */
    private UpdateChecker(Context context) {
        this.context = context.getApplicationContext();
    }
    
    /**
     * 获取单例实例
     */
    public static synchronized UpdateChecker getInstance(Context context) {
        if (instance == null) {
            instance = new UpdateChecker(context);
        }
        return instance;
    }
    
    /**
     * 添加更新检查监听器
     */
    public void addListener(UpdateCheckListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }
    
    /**
     * 移除更新检查监听器
     */
    public void removeListener(UpdateCheckListener listener) {
        listeners.remove(listener);
    }
    
    /**
     * 检查应用更新
     * 
     * @param appInfo 应用信息
     */
    public void checkUpdate(AppInfo appInfo) {
        if (appInfo == null) {
            notifyError("无效的应用信息");
            return;
        }
        
        // 在后台线程执行检查
        executor.execute(() -> {
            try {
                // 构建更新检查URL
                String updateUrl = buildUpdateCheckUrl(appInfo);
                
                // 发送HTTP请求
                HttpURLConnection connection = (HttpURLConnection) new URL(updateUrl).openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);
                
                // 读取响应
                String response = readResponse(connection);
                
                // 解析响应
                StoreAppInfo storeAppInfo = parseUpdateResponse(response);
                
                if (storeAppInfo != null) {
                    // 比较版本号
                    if (isNewVersionAvailable(appInfo.getVersion(), storeAppInfo.getVersion())) {
                        // 缓存更新信息
                        updateCache.put(appInfo.getId(), storeAppInfo);
                        
                        // 通知更新可用
                        notifyUpdateAvailable(appInfo, storeAppInfo);
                    } else {
                        // 通知无更新
                        notifyNoUpdate(appInfo);
                    }
                } else {
                    // 通知解析错误
                    notifyError("解析更新信息失败");
                }
                
            } catch (Exception e) {
                Log.e(TAG, "检查更新失败", e);
                notifyError("检查更新失败: " + e.getMessage());
            }
        });
    }
    
    /**
     * 批量检查应用更新
     * 
     * @param appList 应用列表
     */
    public void checkUpdates(List<AppInfo> appList) {
        if (appList == null || appList.isEmpty()) {
            notifyError("应用列表为空");
            return;
        }
        
        // 在后台线程执行批量检查
        executor.execute(() -> {
            int total = appList.size();
            int checked = 0;
            
            for (AppInfo appInfo : appList) {
                checkUpdate(appInfo);
                checked++;
                
                // 通知进度
                notifyProgress(checked, total);
            }
            
            // 通知完成
            notifyBatchComplete();
        });
    }
    
    /**
     * 构建更新检查URL
     */
    private String buildUpdateCheckUrl(AppInfo appInfo) {
        // TODO: 实现实际的更新检查URL构建逻辑
        return "https://api.example.com/updates/" + appInfo.getId();
    }
    
    /**
     * 读取HTTP响应
     */
    private String readResponse(HttpURLConnection connection) throws IOException {
        StringBuilder response = new StringBuilder();
        
        try (InputStream is = connection.getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        }
        
        return response.toString();
    }
    
    /**
     * 解析更新响应
     */
    private StoreAppInfo parseUpdateResponse(String response) {
        try {
            JSONObject json = new JSONObject(response);
            
            // 解析应用信息
            String id = json.getString("id");
            String name = json.getString("name");
            String version = json.getString("version");
            String description = json.getString("description");
            String downloadUrl = json.getString("download_url");
            long size = json.getLong("size");
            
            // 创建StoreAppInfo对象
            StoreAppInfo storeAppInfo = new StoreAppInfo(
                    id,
                    name,
                    version,
                    description,
                    downloadUrl,
                    size
            );
            
            // 解析更新日志
            if (json.has("changelog")) {
                JSONArray changelog = json.getJSONArray("changelog");
                List<String> changes = new ArrayList<>();
                for (int i = 0; i < changelog.length(); i++) {
                    changes.add(changelog.getString(i));
                }
                storeAppInfo.setChangelog(changes);
            }
            
            return storeAppInfo;
            
        } catch (JSONException e) {
            Log.e(TAG, "解析更新响应失败", e);
            return null;
        }
    }
    
    /**
     * 检查是否有新版本可用
     */
    private boolean isNewVersionAvailable(String currentVersion, String newVersion) {
        String[] current = currentVersion.split("\\.");
        String[] newer = newVersion.split("\\.");
        
        int length = Math.max(current.length, newer.length);
        for (int i = 0; i < length; i++) {
            int currentNum = i < current.length ? Integer.parseInt(current[i]) : 0;
            int newerNum = i < newer.length ? Integer.parseInt(newer[i]) : 0;
            
            if (newerNum > currentNum) {
                return true;
            } else if (newerNum < currentNum) {
                return false;
            }
        }
        
        return false;
    }
    
    /**
     * 获取缓存的更新信息
     */
    public StoreAppInfo getCachedUpdate(String appId) {
        return updateCache.get(appId);
    }
    
    /**
     * 清除更新缓存
     */
    public void clearUpdateCache() {
        updateCache.clear();
    }
    
    /**
     * 通知更新可用
     */
    private void notifyUpdateAvailable(AppInfo appInfo, StoreAppInfo updateInfo) {
        mainHandler.post(() -> {
            for (UpdateCheckListener listener : listeners) {
                listener.onUpdateAvailable(appInfo, updateInfo);
            }
        });
    }
    
    /**
     * 通知无更新
     */
    private void notifyNoUpdate(AppInfo appInfo) {
        mainHandler.post(() -> {
            for (UpdateCheckListener listener : listeners) {
                listener.onNoUpdate(appInfo);
            }
        });
    }
    
    /**
     * 通知错误
     */
    private void notifyError(String errorMessage) {
        mainHandler.post(() -> {
            for (UpdateCheckListener listener : listeners) {
                listener.onError(errorMessage);
            }
        });
    }
    
    /**
     * 通知进度
     */
    private void notifyProgress(int checked, int total) {
        mainHandler.post(() -> {
            for (UpdateCheckListener listener : listeners) {
                listener.onProgress(checked, total);
            }
        });
    }
    
    /**
     * 通知批量检查完成
     */
    private void notifyBatchComplete() {
        mainHandler.post(() -> {
            for (UpdateCheckListener listener : listeners) {
                listener.onBatchComplete();
            }
        });
    }
    
    /**
     * 更新检查监听器接口
     */
    public interface UpdateCheckListener {
        /**
         * 发现新版本
         */
        void onUpdateAvailable(AppInfo appInfo, StoreAppInfo updateInfo);
        
        /**
         * 无新版本
         */
        void onNoUpdate(AppInfo appInfo);
        
        /**
         * 检查出错
         */
        void onError(String errorMessage);
        
        /**
         * 批量检查进度
         */
        void onProgress(int checked, int total);
        
        /**
         * 批量检查完成
         */
        void onBatchComplete();
    }
} 