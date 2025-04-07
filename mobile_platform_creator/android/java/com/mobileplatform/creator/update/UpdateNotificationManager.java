package com.mobileplatform.creator.update;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.mobileplatform.creator.R;
import com.mobileplatform.creator.data.model.AppInfo;
import com.mobileplatform.creator.data.model.StoreAppInfo;
import com.mobileplatform.creator.ui.download.DownloadActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * 更新通知管理器
 * 
 * 负责显示应用更新相关的通知
 */
public class UpdateNotificationManager {
    private static final String TAG = "UpdateNotificationManager";
    
    // 通知渠道ID
    private static final String CHANNEL_ID = "app_updates";
    
    // 通知渠道名称
    private static final String CHANNEL_NAME = "应用更新";
    
    // 通知渠道描述
    private static final String CHANNEL_DESCRIPTION = "显示应用更新相关的通知";
    
    // 单例实例
    private static UpdateNotificationManager instance;
    
    // 上下文对象
    private Context context;
    
    // 通知管理器
    private NotificationManager notificationManager;
    
    // 待处理的更新列表
    private final List<UpdateInfo> pendingUpdates = new ArrayList<>();
    
    /**
     * 更新信息类
     */
    private static class UpdateInfo {
        AppInfo appInfo;
        StoreAppInfo updateInfo;
        int notificationId;
        
        UpdateInfo(AppInfo appInfo, StoreAppInfo updateInfo, int notificationId) {
            this.appInfo = appInfo;
            this.updateInfo = updateInfo;
            this.notificationId = notificationId;
        }
    }
    
    /**
     * 私有构造函数
     */
    private UpdateNotificationManager(Context context) {
        this.context = context.getApplicationContext();
        this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        
        // 创建通知渠道
        createNotificationChannel();
    }
    
    /**
     * 获取单例实例
     */
    public static synchronized UpdateNotificationManager getInstance(Context context) {
        if (instance == null) {
            instance = new UpdateNotificationManager(context);
        }
        return instance;
    }
    
    /**
     * 创建通知渠道
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription(CHANNEL_DESCRIPTION);
            channel.enableLights(true);
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{100, 200, 300, 400, 500});
            
            notificationManager.createNotificationChannel(channel);
        }
    }
    
    /**
     * 显示更新通知
     */
    public void showUpdateNotification(AppInfo appInfo, StoreAppInfo updateInfo) {
        // 创建通知ID
        int notificationId = (int) System.currentTimeMillis();
        
        // 创建更新信息对象
        UpdateInfo update = new UpdateInfo(appInfo, updateInfo, notificationId);
        
        // 添加到待处理列表
        pendingUpdates.add(update);
        
        // 创建通知
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_update)
                .setContentTitle(appInfo.getName() + " 有新版本")
                .setContentText("版本 " + updateInfo.getVersion() + " 可用")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setVibrate(new long[]{100, 200, 300, 400, 500});
        
        // 创建更新意图
        Intent updateIntent = new Intent(context, DownloadActivity.class);
        updateIntent.putExtra("app_id", appInfo.getId());
        updateIntent.putExtra("update_version", updateInfo.getVersion());
        updateIntent.putExtra("download_url", updateInfo.getDownloadUrl());
        
        PendingIntent updatePendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                updateIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        // 添加更新按钮
        builder.addAction(
                R.drawable.ic_download,
                "立即更新",
                updatePendingIntent
        );
        
        // 显示通知
        notificationManager.notify(notificationId, builder.build());
    }
    
    /**
     * 显示批量更新通知
     */
    public void showBatchUpdateNotification(int updateCount) {
        // 创建通知
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_update)
                .setContentTitle("发现多个应用更新")
                .setContentText(updateCount + " 个应用有新版本可用")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setVibrate(new long[]{100, 200, 300, 400, 500});
        
        // 创建查看更新意图
        Intent viewIntent = new Intent(context, DownloadActivity.class);
        viewIntent.putExtra("show_updates", true);
        
        PendingIntent viewPendingIntent = PendingIntent.getActivity(
                context,
                (int) System.currentTimeMillis(),
                viewIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        // 添加查看按钮
        builder.addAction(
                R.drawable.ic_list,
                "查看更新",
                viewPendingIntent
        );
        
        // 显示通知
        notificationManager.notify(0, builder.build());
    }
    
    /**
     * 取消更新通知
     */
    public void cancelUpdateNotification(String appId) {
        // 查找并移除更新信息
        UpdateInfo update = null;
        for (UpdateInfo info : pendingUpdates) {
            if (info.appInfo.getId().equals(appId)) {
                update = info;
                break;
            }
        }
        
        if (update != null) {
            pendingUpdates.remove(update);
            notificationManager.cancel(update.notificationId);
        }
    }
    
    /**
     * 取消所有更新通知
     */
    public void cancelAllUpdateNotifications() {
        // 取消所有通知
        for (UpdateInfo update : pendingUpdates) {
            notificationManager.cancel(update.notificationId);
        }
        
        // 清空待处理列表
        pendingUpdates.clear();
    }
    
    /**
     * 获取待处理的更新列表
     */
    public List<UpdateInfo> getPendingUpdates() {
        return new ArrayList<>(pendingUpdates);
    }
} 