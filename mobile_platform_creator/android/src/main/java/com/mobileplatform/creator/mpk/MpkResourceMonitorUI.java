package com.mobileplatform.creator.mpk;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * MPK 资源监控可视化界面
 * 用于显示和管理应用资源使用情况
 */
public class MpkResourceMonitorUI extends LinearLayout {
    private static final String TAG = "MpkResourceMonitorUI";
    
    // MPK 运行时
    private MpkRuntime runtime;
    
    // 应用 ID
    private String appId;
    
    // 资源使用状态
    private Map<String, Object> resourceStatus;
    
    // 资源类型颜色
    private static final int COLOR_STORAGE = Color.parseColor("#4CAF50");
    private static final int COLOR_PROCESS = Color.parseColor("#2196F3");
    private static final int COLOR_MEMORY = Color.parseColor("#FFC107");
    private static final int COLOR_CPU = Color.parseColor("#F44336");
    private static final int COLOR_NETWORK = Color.parseColor("#9C27B0");
    
    // 警告颜色
    private static final int COLOR_WARNING = Color.parseColor("#FF9800");
    private static final int COLOR_EXCEEDED = Color.parseColor("#F44336");
    private static final int COLOR_NORMAL = Color.parseColor("#4CAF50");
    
    // 界面元素
    private TextView appNameText;
    private TextView appVersionText;
    private TextView appStatusText;
    
    private ProgressBar storageProgressBar;
    private TextView storageText;
    private ProgressBar processProgressBar;
    private TextView processText;
    private ProgressBar memoryProgressBar;
    private TextView memoryText;
    private ProgressBar cpuProgressBar;
    private TextView cpuText;
    private ProgressBar networkProgressBar;
    private TextView networkText;
    
    private Button clearCacheButton;
    private Button clearTempButton;
    private Button stopAppButton;
    
    // 刷新间隔（毫秒）
    private static final long REFRESH_INTERVAL = 1000;
    
    // 调度器
    private ScheduledExecutorService scheduler;
    
    // 主线程处理器
    private Handler mainHandler;
    
    // 格式化工具
    private DecimalFormat percentFormat = new DecimalFormat("##0%");
    private DecimalFormat sizeFormat = new DecimalFormat("###,###.##");
    
    /**
     * 构造函数
     * @param context 上下文
     */
    public MpkResourceMonitorUI(Context context) {
        super(context);
        init(context);
    }
    
    /**
     * 构造函数
     * @param context 上下文
     * @param attrs 属性集
     */
    public MpkResourceMonitorUI(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }
    
    /**
     * 初始化
     * @param context 上下文
     */
    private void init(Context context) {
        setOrientation(VERTICAL);
        setPadding(16, 16, 16, 16);
        
        // 从布局文件加载视图
        // 注意：这里需要在项目中创建对应的布局文件
        // LayoutInflater.from(context).inflate(R.layout.mpk_resource_monitor, this, true);
        
        // 由于没有布局文件，这里直接创建界面元素
        createUIElements(context);
        
        // 初始化处理器
        mainHandler = new Handler(Looper.getMainLooper());
        
        // 初始化资源状态
        resourceStatus = new HashMap<>();
    }
    
    /**
     * 创建界面元素
     * @param context 上下文
     */
    @SuppressLint("SetTextI18n")
    private void createUIElements(Context context) {
        // 应用信息区域
        LinearLayout appInfoLayout = new LinearLayout(context);
        appInfoLayout.setOrientation(VERTICAL);
        appInfoLayout.setPadding(0, 0, 0, 16);
        
        appNameText = new TextView(context);
        appNameText.setTextSize(18);
        appNameText.setTypeface(null, Typeface.BOLD);
        appInfoLayout.addView(appNameText);
        
        appVersionText = new TextView(context);
        appVersionText.setTextSize(14);
        appInfoLayout.addView(appVersionText);
        
        appStatusText = new TextView(context);
        appStatusText.setTextSize(14);
        appInfoLayout.addView(appStatusText);
        
        addView(appInfoLayout);
        
        // 分隔线
        View divider = new View(context);
        divider.setBackgroundColor(Color.LTGRAY);
        LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 1);
        dividerParams.setMargins(0, 8, 0, 8);
        divider.setLayoutParams(dividerParams);
        addView(divider);
        
        // 资源使用区域
        addResourceRow(context, "存储空间:", COLOR_STORAGE);
        storageProgressBar = (ProgressBar) ((ViewGroup) getChildAt(2)).getChildAt(1);
        storageText = (TextView) ((ViewGroup) getChildAt(2)).getChildAt(2);
        
        addResourceRow(context, "进程:", COLOR_PROCESS);
        processProgressBar = (ProgressBar) ((ViewGroup) getChildAt(3)).getChildAt(1);
        processText = (TextView) ((ViewGroup) getChildAt(3)).getChildAt(2);
        
        addResourceRow(context, "内存:", COLOR_MEMORY);
        memoryProgressBar = (ProgressBar) ((ViewGroup) getChildAt(4)).getChildAt(1);
        memoryText = (TextView) ((ViewGroup) getChildAt(4)).getChildAt(2);
        
        addResourceRow(context, "CPU:", COLOR_CPU);
        cpuProgressBar = (ProgressBar) ((ViewGroup) getChildAt(5)).getChildAt(1);
        cpuText = (TextView) ((ViewGroup) getChildAt(5)).getChildAt(2);
        
        addResourceRow(context, "网络:", COLOR_NETWORK);
        networkProgressBar = (ProgressBar) ((ViewGroup) getChildAt(6)).getChildAt(1);
        networkText = (TextView) ((ViewGroup) getChildAt(6)).getChildAt(2);
        
        // 分隔线
        View divider2 = new View(context);
        divider2.setBackgroundColor(Color.LTGRAY);
        LinearLayout.LayoutParams dividerParams2 = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 1);
        dividerParams2.setMargins(0, 8, 0, 8);
        divider2.setLayoutParams(dividerParams2);
        addView(divider2);
        
        // 操作按钮区域
        LinearLayout buttonLayout = new LinearLayout(context);
        buttonLayout.setOrientation(HORIZONTAL);
        buttonLayout.setPadding(0, 8, 0, 0);
        
        clearCacheButton = new Button(context);
        clearCacheButton.setText("清理缓存");
        clearCacheButton.setOnClickListener(v -> clearCache());
        buttonLayout.addView(clearCacheButton);
        
        clearTempButton = new Button(context);
        clearTempButton.setText("清理临时文件");
        clearTempButton.setOnClickListener(v -> clearTemp());
        buttonLayout.addView(clearTempButton);
        
        stopAppButton = new Button(context);
        stopAppButton.setText("停止应用");
        stopAppButton.setOnClickListener(v -> stopApp());
        buttonLayout.addView(stopAppButton);
        
        addView(buttonLayout);
    }
    
    /**
     * 添加资源行
     * @param context 上下文
     * @param label 标签
     * @param color 颜色
     */
    private void addResourceRow(Context context, String label, int color) {
        LinearLayout rowLayout = new LinearLayout(context);
        rowLayout.setOrientation(HORIZONTAL);
        rowLayout.setPadding(0, 4, 0, 4);
        
        TextView labelText = new TextView(context);
        labelText.setText(label);
        labelText.setWidth(100);
        rowLayout.addView(labelText);
        
        ProgressBar progressBar = new ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);
        progressBar.setProgress(0);
        LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        progressBar.setLayoutParams(progressParams);
        progressBar.getProgressDrawable().setColorFilter(color, android.graphics.PorterDuff.Mode.SRC_IN);
        rowLayout.addView(progressBar);
        
        TextView valueText = new TextView(context);
        valueText.setText("0%");
        valueText.setPadding(8, 0, 0, 0);
        valueText.setWidth(120);
        rowLayout.addView(valueText);
        
        addView(rowLayout);
    }
    
    /**
     * 设置 MPK 运行时
     * @param runtime MPK 运行时
     */
    public void setRuntime(MpkRuntime runtime) {
        this.runtime = runtime;
    }
    
    /**
     * 设置应用 ID
     * @param appId 应用 ID
     */
    public void setAppId(String appId) {
        this.appId = appId;
        updateAppInfo();
    }
    
    /**
     * 启动监控
     */
    public void startMonitoring() {
        if (runtime == null || appId == null) {
            Log.e(TAG, "运行时或应用 ID 为空");
            return;
        }
        
        // 更新应用信息
        updateAppInfo();
        
        // 启动刷新任务
        if (scheduler == null || scheduler.isShutdown()) {
            scheduler = Executors.newSingleThreadScheduledExecutor();
            scheduler.scheduleAtFixedRate(this::refreshResourceStatus, 0, REFRESH_INTERVAL, TimeUnit.MILLISECONDS);
        }
    }
    
    /**
     * 停止监控
     */
    public void stopMonitoring() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(500, TimeUnit.MILLISECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
    
    /**
     * 更新应用信息
     */
    private void updateAppInfo() {
        if (runtime == null || appId == null) {
            return;
        }
        
        try {
            MpkFile mpk = runtime.getLoadedApp(appId);
            if (mpk != null) {
                mainHandler.post(() -> {
                    appNameText.setText(mpk.getName());
                    appVersionText.setText("版本: " + mpk.getVersion() + " (" + mpk.getVersionCode() + ")");
                    
                    boolean isRunning = runtime.isAppRunning(appId);
                    appStatusText.setText("状态: " + (isRunning ? "运行中" : "已停止"));
                    appStatusText.setTextColor(isRunning ? COLOR_NORMAL : COLOR_EXCEEDED);
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "更新应用信息失败", e);
        }
    }
    
    /**
     * 刷新资源状态
     */
    private void refreshResourceStatus() {
        if (runtime == null || appId == null) {
            return;
        }
        
        try {
            Map<String, Object> status = runtime.getAppStatus(appId);
            if (status != null && status.containsKey("resources")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> resources = (Map<String, Object>) status.get("resources");
                
                resourceStatus.putAll(resources);
                
                mainHandler.post(() -> {
                    updateStorageUI(resources);
                    updateProcessUI(resources);
                    updateMemoryUI(resources);
                    updateCpuUI(resources);
                    updateNetworkUI(resources);
                    
                    boolean isRunning = (boolean) status.get("isRunning");
                    appStatusText.setText("状态: " + (isRunning ? "运行中" : "已停止"));
                    appStatusText.setTextColor(isRunning ? COLOR_NORMAL : COLOR_EXCEEDED);
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "刷新资源状态失败", e);
        }
    }
    
    /**
     * 更新存储空间 UI
     * @param resources 资源状态
     */
    @SuppressLint("SetTextI18n")
    private void updateStorageUI(Map<String, Object> resources) {
        long storageUsage = (long) resources.get("storageUsage");
        long maxStorage = (long) resources.get("maxStorage");
        int percentage = (int) resources.get("storagePercentage");
        
        storageProgressBar.setProgress(percentage);
        if (percentage >= 90) {
            storageProgressBar.getProgressDrawable().setColorFilter(COLOR_EXCEEDED, android.graphics.PorterDuff.Mode.SRC_IN);
        } else if (percentage >= 70) {
            storageProgressBar.getProgressDrawable().setColorFilter(COLOR_WARNING, android.graphics.PorterDuff.Mode.SRC_IN);
        } else {
            storageProgressBar.getProgressDrawable().setColorFilter(COLOR_STORAGE, android.graphics.PorterDuff.Mode.SRC_IN);
        }
        
        String text = percentage + "% (" + formatSize(storageUsage) + "/" + formatSize(maxStorage) + ")";
        storageText.setText(text);
    }
    
    /**
     * 更新进程 UI
     * @param resources 资源状态
     */
    @SuppressLint("SetTextI18n")
    private void updateProcessUI(Map<String, Object> resources) {
        int processCount = (int) resources.get("processCount");
        int maxProcesses = (int) resources.get("maxProcesses");
        int percentage = (int) resources.get("processPercentage");
        
        processProgressBar.setProgress(percentage);
        if (percentage >= 90) {
            processProgressBar.getProgressDrawable().setColorFilter(COLOR_EXCEEDED, android.graphics.PorterDuff.Mode.SRC_IN);
        } else if (percentage >= 70) {
            processProgressBar.getProgressDrawable().setColorFilter(COLOR_WARNING, android.graphics.PorterDuff.Mode.SRC_IN);
        } else {
            processProgressBar.getProgressDrawable().setColorFilter(COLOR_PROCESS, android.graphics.PorterDuff.Mode.SRC_IN);
        }
        
        String text = percentage + "% (" + processCount + "/" + maxProcesses + ")";
        processText.setText(text);
    }
    
    /**
     * 更新内存 UI
     * @param resources 资源状态
     */
    @SuppressLint("SetTextI18n")
    private void updateMemoryUI(Map<String, Object> resources) {
        long memoryUsage = (long) resources.get("memoryUsage");
        long maxMemory = (long) resources.get("maxMemory");
        int percentage = (int) resources.get("memoryPercentage");
        
        memoryProgressBar.setProgress(percentage);
        if (percentage >= 90) {
            memoryProgressBar.getProgressDrawable().setColorFilter(COLOR_EXCEEDED, android.graphics.PorterDuff.Mode.SRC_IN);
        } else if (percentage >= 70) {
            memoryProgressBar.getProgressDrawable().setColorFilter(COLOR_WARNING, android.graphics.PorterDuff.Mode.SRC_IN);
        } else {
            memoryProgressBar.getProgressDrawable().setColorFilter(COLOR_MEMORY, android.graphics.PorterDuff.Mode.SRC_IN);
        }
        
        String text = percentage + "% (" + formatSize(memoryUsage) + "/" + formatSize(maxMemory) + ")";
        memoryText.setText(text);
    }
    
    /**
     * 更新 CPU UI
     * @param resources 资源状态
     */
    @SuppressLint("SetTextI18n")
    private void updateCpuUI(Map<String, Object> resources) {
        float cpuUsage = ((Number) resources.get("cpuUsage")).floatValue();
        float maxCpuUsage = ((Number) resources.get("maxCpuUsage")).floatValue();
        int percentage = (int) resources.get("cpuPercentage");
        
        cpuProgressBar.setProgress(percentage);
        if (percentage >= 90) {
            cpuProgressBar.getProgressDrawable().setColorFilter(COLOR_EXCEEDED, android.graphics.PorterDuff.Mode.SRC_IN);
        } else if (percentage >= 70) {
            cpuProgressBar.getProgressDrawable().setColorFilter(COLOR_WARNING, android.graphics.PorterDuff.Mode.SRC_IN);
        } else {
            cpuProgressBar.getProgressDrawable().setColorFilter(COLOR_CPU, android.graphics.PorterDuff.Mode.SRC_IN);
        }
        
        String text = percentage + "% (" + cpuUsage + "%" + "/" + maxCpuUsage + "%)";
        cpuText.setText(text);
    }
    
    /**
     * 更新网络 UI
     * @param resources 资源状态
     */
    @SuppressLint("SetTextI18n")
    private void updateNetworkUI(Map<String, Object> resources) {
        long networkUsage = (long) resources.get("networkUsage");
        long maxNetworkUsage = (long) resources.get("maxNetworkUsage");
        int percentage = (int) resources.get("networkPercentage");
        
        networkProgressBar.setProgress(percentage);
        if (percentage >= 90) {
            networkProgressBar.getProgressDrawable().setColorFilter(COLOR_EXCEEDED, android.graphics.PorterDuff.Mode.SRC_IN);
        } else if (percentage >= 70) {
            networkProgressBar.getProgressDrawable().setColorFilter(COLOR_WARNING, android.graphics.PorterDuff.Mode.SRC_IN);
        } else {
            networkProgressBar.getProgressDrawable().setColorFilter(COLOR_NETWORK, android.graphics.PorterDuff.Mode.SRC_IN);
        }
        
        String text = percentage + "% (" + formatSize(networkUsage) + "/" + formatSize(maxNetworkUsage) + ")";
        networkText.setText(text);
    }
    
    /**
     * 格式化大小
     * @param bytes 字节数
     * @return 格式化后的大小
     */
    private String formatSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return sizeFormat.format(bytes / 1024.0) + " KB";
        } else if (bytes < 1024 * 1024 * 1024) {
            return sizeFormat.format(bytes / (1024.0 * 1024)) + " MB";
        } else {
            return sizeFormat.format(bytes / (1024.0 * 1024 * 1024)) + " GB";
        }
    }
    
    /**
     * 清理缓存
     */
    private void clearCache() {
        if (runtime != null && appId != null) {
            new Thread(() -> {
                try {
                    runtime.clearCache(appId);
                    mainHandler.post(() -> {
                        if (getContext() != null) {
                            // 这里可以添加提示信息
                            Log.i(TAG, "已清理缓存: " + appId);
                        }
                    });
                } catch (Exception e) {
                    Log.e(TAG, "清理缓存失败", e);
                }
            }).start();
        }
    }
    
    /**
     * 清理临时文件
     */
    private void clearTemp() {
        if (runtime != null && appId != null) {
            new Thread(() -> {
                try {
                    runtime.clearTemp(appId);
                    mainHandler.post(() -> {
                        if (getContext() != null) {
                            // 这里可以添加提示信息
                            Log.i(TAG, "已清理临时文件: " + appId);
                        }
                    });
                } catch (Exception e) {
                    Log.e(TAG, "清理临时文件失败", e);
                }
            }).start();
        }
    }
    
    /**
     * 停止应用
     */
    private void stopApp() {
        if (runtime != null && appId != null) {
            new Thread(() -> {
                try {
                    runtime.stopApp(appId);
                    mainHandler.post(() -> {
                        if (getContext() != null) {
                            // 这里可以添加提示信息
                            Log.i(TAG, "已停止应用: " + appId);
                        }
                    });
                } catch (Exception e) {
                    Log.e(TAG, "停止应用失败", e);
                }
            }).start();
        }
    }
    
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopMonitoring();
    }
} 