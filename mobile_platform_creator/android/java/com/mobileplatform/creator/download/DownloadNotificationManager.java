package com.mobileplatform.creator.download;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.mobileplatform.creator.R;
import com.mobileplatform.creator.MainActivity;
import com.mobileplatform.creator.data.model.DownloadTask;
import com.mobileplatform.creator.utils.FileUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * 下载通知管理器
 * 显示下载进度通知
 */
public class DownloadNotificationManager {
    private static final String TAG = "DownloadNotificationMgr";
    
    // 通知渠道ID
    private static final String CHANNEL_ID = "download_channel";
    
    // 通知渠道名称
    private static final String CHANNEL_NAME = "下载任务";
    
    // 通知ID基础值
    private static final int NOTIFICATION_ID_BASE = 1000;
    
    // 上下文
    private final Context context;
    
    // 通知管理器
    private final NotificationManager notificationManager;
    
    // 通知ID映射表
    private final Map<String, Integer> notificationIds;
    
    /**
     * 构造函数
     * 
     * @param context 上下文
     */
    public DownloadNotificationManager(Context context) {
        this.context = context;
        this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        this.notificationIds = new HashMap<>();
        
        // 创建通知渠道
        createNotificationChannel();
    }
    
    /**
     * 创建通知渠道
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_LOW);
            
            channel.setDescription("用于显示应用下载进度");
            channel.enableVibration(false);
            channel.enableLights(false);
            channel.setSound(null, null);
            
            notificationManager.createNotificationChannel(channel);
        }
    }
    
    /**
     * 获取通知ID
     * 
     * @param taskId 任务ID
     * @return 通知ID
     */
    private int getNotificationId(String taskId) {
        if (notificationIds.containsKey(taskId)) {
            return notificationIds.get(taskId);
        }
        
        int notificationId = NOTIFICATION_ID_BASE + notificationIds.size();
        notificationIds.put(taskId, notificationId);
        return notificationId;
    }
    
    /**
     * 显示通知
     * 
     * @param task 下载任务
     */
    public void showNotification(DownloadTask task) {
        if (task == null) {
            return;
        }
        
        String appName = task.getAppInfo() != null ? task.getAppInfo().getName() : task.getFileName();
        
        // 创建基本通知
        NotificationCompat.Builder builder = createBaseNotificationBuilder(task, appName);
        
        // 设置为等待状态
        builder.setContentText("准备下载");
        builder.setProgress(100, 0, true);
        
        // 显示通知
        int notificationId = getNotificationId(task.getId());
        notificationManager.notify(notificationId, builder.build());
        
        Log.d(TAG, "显示下载通知: " + appName);
    }
    
    /**
     * 更新通知
     * 
     * @param task 下载任务
     */
    public void updateNotification(DownloadTask task) {
        if (task == null) {
            return;
        }
        
        String appName = task.getAppInfo() != null ? task.getAppInfo().getName() : task.getFileName();
        
        // 创建基本通知
        NotificationCompat.Builder builder = createBaseNotificationBuilder(task, appName);
        
        // 设置进度
        int progress = task.getProgress();
        
        if (task.isRunning()) {
            // 显示下载大小和速度
            String downloadedSize = FileUtils.formatFileSize(task.getDownloadedSize());
            String totalSize = FileUtils.formatFileSize(task.getTotalSize());
            String speed = FileUtils.formatFileSize(task.getSpeed()) + "/s";
            
            builder.setContentText(downloadedSize + "/" + totalSize + " - " + speed);
            builder.setProgress(100, progress, false);
            
        } else if (task.isPaused()) {
            // 显示已暂停
            builder.setContentText("已暂停 - " + progress + "%");
            builder.setProgress(100, progress, false);
            
        } else if (task.isPending()) {
            // 显示等待中
            builder.setContentText("等待中");
            builder.setProgress(100, 0, true);
        }
        
        // 显示通知
        int notificationId = getNotificationId(task.getId());
        notificationManager.notify(notificationId, builder.build());
    }
    
    /**
     * 完成通知
     * 
     * @param task 下载任务
     */
    public void completeNotification(DownloadTask task) {
        if (task == null) {
            return;
        }
        
        String appName = task.getAppInfo() != null ? task.getAppInfo().getName() : task.getFileName();
        
        // 创建基本通知
        NotificationCompat.Builder builder = createBaseNotificationBuilder(task, appName);
        
        // 设置为完成状态
        builder.setContentText("下载完成");
        builder.setProgress(0, 0, false);
        builder.setAutoCancel(true);
        
        // 创建安装意图
        Intent installIntent = new Intent(context, MainActivity.class);
        installIntent.setAction("com.mobileplatform.creator.action.INSTALL");
        installIntent.putExtra("task_id", task.getId());
        
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                getNotificationId(task.getId()),
                installIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        
        builder.setContentIntent(pendingIntent);
        
        // 显示通知
        int notificationId = getNotificationId(task.getId());
        notificationManager.notify(notificationId, builder.build());
        
        Log.d(TAG, "下载完成通知: " + appName);
    }
    
    /**
     * 失败通知
     * 
     * @param task 下载任务
     * @param error 错误信息
     */
    public void failNotification(DownloadTask task, String error) {
        if (task == null) {
            return;
        }
        
        String appName = task.getAppInfo() != null ? task.getAppInfo().getName() : task.getFileName();
        
        // 创建基本通知
        NotificationCompat.Builder builder = createBaseNotificationBuilder(task, appName);
        
        // 设置为失败状态
        builder.setContentText("下载失败: " + error);
        builder.setProgress(0, 0, false);
        builder.setAutoCancel(true);
        
        // 创建重试意图
        Intent retryIntent = new Intent(context, MainActivity.class);
        retryIntent.setAction("com.mobileplatform.creator.action.RETRY");
        retryIntent.putExtra("task_id", task.getId());
        
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                getNotificationId(task.getId()),
                retryIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        
        builder.setContentIntent(pendingIntent);
        
        // 显示通知
        int notificationId = getNotificationId(task.getId());
        notificationManager.notify(notificationId, builder.build());
        
        Log.d(TAG, "下载失败通知: " + appName);
    }
    
    /**
     * 取消通知
     * 
     * @param task 下载任务
     */
    public void cancelNotification(DownloadTask task) {
        if (task == null) {
            return;
        }
        
        Integer notificationId = notificationIds.get(task.getId());
        if (notificationId != null) {
            notificationManager.cancel(notificationId);
            notificationIds.remove(task.getId());
            
            Log.d(TAG, "取消下载通知: " + task.getId());
        }
    }
    
    /**
     * 创建基本通知构建器
     * 
     * @param task 下载任务
     * @param appName 应用名称
     * @return 通知构建器
     */
    private NotificationCompat.Builder createBaseNotificationBuilder(DownloadTask task, String appName) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentTitle(appName)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_PROGRESS);
        
        return builder;
    }
} 