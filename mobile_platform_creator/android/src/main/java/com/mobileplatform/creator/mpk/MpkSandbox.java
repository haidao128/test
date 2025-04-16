package com.mobileplatform.creator.mpk;

import android.app.ActivityManager;
import android.content.Context;
import android.net.TrafficStats;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * MPK 沙箱管理器
 * 用于管理应用沙箱环境
 */
public class MpkSandbox {
    private static final String TAG = "MpkSandbox";
    
    // 上下文
    private Context context;
    
    // 沙箱根目录
    private File sandboxRoot;
    
    // 应用沙箱映射表
    private final Map<String, SandboxEnvironment> sandboxes;
    
    // 资源监控器映射表
    private final Map<String, ResourceMonitor> resourceMonitors;
    
    // 资源使用情况映射表
    private final Map<String, ResourceUsage> resourceUsages;
    
    // 调度器
    private final ScheduledExecutorService scheduler;
    
    // 主线程处理器
    private final Handler mainHandler;
    
    // 事件监听器映射表
    private final Map<String, Set<SandboxEventListener>> eventListeners;
    
    // 警告阈值百分比
    private static final int WARNING_THRESHOLD = 80; // 80%
    
    // 默认资源限制
    private static final long DEFAULT_MAX_STORAGE = 100 * 1024 * 1024; // 100MB
    private static final int DEFAULT_MAX_PROCESSES = 5;
    private static final long DEFAULT_MAX_MEMORY = 256 * 1024 * 1024; // 256MB
    private static final long DEFAULT_MAX_CPU_USAGE = 50; // 50%
    private static final long DEFAULT_MAX_NETWORK_USAGE = 10 * 1024 * 1024; // 10MB
    private static final long DEFAULT_MONITOR_INTERVAL = 5000; // 5秒
    
    // 资源监控回调接口
    public interface ResourceMonitorCallback {
        void onResourceExceeded(String appId, ResourceExceededEvent event);
    }
    
    /**
     * 沙箱事件类型
     */
    public enum SandboxEventType {
        RESOURCE_WARNING,       // 资源接近限制警告
        RESOURCE_EXCEEDED,      // 资源超出限制
        SANDBOX_CREATED,        // 沙箱创建
        SANDBOX_DELETED,        // 沙箱删除
        RESOURCE_CLEARED        // 资源被清理
    }
    
    /**
     * 沙箱事件监听器接口
     */
    public interface SandboxEventListener {
        void onSandboxEvent(String appId, SandboxEvent event);
    }
    
    /**
     * 沙箱事件类
     */
    public static class SandboxEvent {
        private final SandboxEventType type;
        private final Map<String, Object> data;
        private final long timestamp;
        
        public SandboxEvent(SandboxEventType type) {
            this.type = type;
            this.data = new HashMap<>();
            this.timestamp = System.currentTimeMillis();
        }
        
        public SandboxEventType getType() {
            return type;
        }
        
        public void putData(String key, Object value) {
            data.put(key, value);
        }
        
        public Object getData(String key) {
            return data.get(key);
        }
        
        public Map<String, Object> getAllData() {
            return new HashMap<>(data);
        }
        
        public long getTimestamp() {
            return timestamp;
        }
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("SandboxEvent{type=").append(type);
            sb.append(", data=").append(data);
            sb.append(", timestamp=").append(timestamp);
            sb.append('}');
            return sb.toString();
        }
    }
    
    /**
     * 沙箱环境类
     */
    public static class SandboxEnvironment {
        // 沙箱目录
        public final File rootDir;
        // 数据目录
        public final File dataDir;
        // 缓存目录
        public final File cacheDir;
        // 临时目录
        public final File tempDir;
        // 共享目录
        public final File sharedDir;
        // 资源限制
        public final ResourceLimits limits;
        
        public SandboxEnvironment(File rootDir, ResourceLimits limits) {
            this.rootDir = rootDir;
            this.dataDir = new File(rootDir, "data");
            this.cacheDir = new File(rootDir, "cache");
            this.tempDir = new File(rootDir, "temp");
            this.sharedDir = new File(rootDir, "shared");
            this.limits = limits;
            
            // 创建目录
            dataDir.mkdirs();
            cacheDir.mkdirs();
            tempDir.mkdirs();
            sharedDir.mkdirs();
        }
    }
    
    /**
     * 资源限制类
     */
    public static class ResourceLimits {
        // 最大存储空间（字节）
        public final long maxStorage;
        // 最大进程数
        public final int maxProcesses;
        // 最大内存使用（字节）
        public final long maxMemory;
        // 最大CPU使用率（百分比）
        public final long maxCpuUsage;
        // 最大网络流量（字节/天）
        public final long maxNetworkUsage;
        // 监控间隔（毫秒）
        public final long monitorInterval;
        
        public ResourceLimits(long maxStorage, int maxProcesses, long maxMemory, 
                              long maxCpuUsage, long maxNetworkUsage, long monitorInterval) {
            this.maxStorage = maxStorage;
            this.maxProcesses = maxProcesses;
            this.maxMemory = maxMemory;
            this.maxCpuUsage = maxCpuUsage;
            this.maxNetworkUsage = maxNetworkUsage;
            this.monitorInterval = monitorInterval;
        }
        
        public static ResourceLimits getDefault() {
            return new ResourceLimits(
                DEFAULT_MAX_STORAGE, 
                DEFAULT_MAX_PROCESSES, 
                DEFAULT_MAX_MEMORY,
                DEFAULT_MAX_CPU_USAGE,
                DEFAULT_MAX_NETWORK_USAGE,
                DEFAULT_MONITOR_INTERVAL
            );
        }
        
        /**
         * 从JSON创建资源限制
         * @param json JSON对象
         * @return 资源限制
         */
        public static ResourceLimits fromJson(org.json.JSONObject json) {
            if (json == null) {
                return getDefault();
            }
            
            long maxStorage = json.optLong("max_storage", DEFAULT_MAX_STORAGE);
            int maxProcesses = json.optInt("max_processes", DEFAULT_MAX_PROCESSES);
            long maxMemory = json.optLong("max_memory", DEFAULT_MAX_MEMORY);
            long maxCpuUsage = json.optLong("max_cpu_usage", DEFAULT_MAX_CPU_USAGE);
            long maxNetworkUsage = json.optLong("max_network_usage", DEFAULT_MAX_NETWORK_USAGE);
            long monitorInterval = json.optLong("monitor_interval", DEFAULT_MONITOR_INTERVAL);
            
            return new ResourceLimits(
                maxStorage, 
                maxProcesses, 
                maxMemory,
                maxCpuUsage,
                maxNetworkUsage,
                monitorInterval
            );
        }
    }
    
    /**
     * 资源使用情况类
     */
    public static class ResourceUsage {
        // 存储使用情况
        private long storageUsage;
        // 进程数量
        private int processCount;
        // 内存使用情况
        private long memoryUsage;
        // CPU使用率
        private float cpuUsage;
        // 当日网络流量
        private long networkUsage;
        // 上次重置时间
        private long lastResetTime;
        // 上次监控时间
        private long lastMonitorTime;
        // 上次网络接收字节数
        private long lastRxBytes;
        // 上次网络发送字节数
        private long lastTxBytes;
        // 进程ID列表
        private final Map<Integer, Long> processPids;
        
        public ResourceUsage() {
            this.storageUsage = 0;
            this.processCount = 0;
            this.memoryUsage = 0;
            this.cpuUsage = 0;
            this.networkUsage = 0;
            this.lastResetTime = System.currentTimeMillis();
            this.lastMonitorTime = this.lastResetTime;
            this.lastRxBytes = TrafficStats.getTotalRxBytes();
            this.lastTxBytes = TrafficStats.getTotalTxBytes();
            this.processPids = new HashMap<>();
        }
        
        public long getStorageUsage() {
            return storageUsage;
        }
        
        public void setStorageUsage(long storageUsage) {
            this.storageUsage = storageUsage;
        }
        
        public int getProcessCount() {
            return processCount;
        }
        
        public void setProcessCount(int processCount) {
            this.processCount = processCount;
        }
        
        public long getMemoryUsage() {
            return memoryUsage;
        }
        
        public void setMemoryUsage(long memoryUsage) {
            this.memoryUsage = memoryUsage;
        }
        
        public float getCpuUsage() {
            return cpuUsage;
        }
        
        public void setCpuUsage(float cpuUsage) {
            this.cpuUsage = cpuUsage;
        }
        
        public long getNetworkUsage() {
            return networkUsage;
        }
        
        public void setNetworkUsage(long networkUsage) {
            this.networkUsage = networkUsage;
        }
        
        public long getLastResetTime() {
            return lastResetTime;
        }
        
        public void resetDaily() {
            // 检查是否需要重置日限额
            long currentTime = System.currentTimeMillis();
            long dayInMillis = 24 * 60 * 60 * 1000;
            if (currentTime - lastResetTime > dayInMillis) {
                this.networkUsage = 0;
                this.lastResetTime = currentTime;
            }
        }
        
        public void updateLastMonitorTime() {
            this.lastMonitorTime = System.currentTimeMillis();
        }
        
        public void updateNetwork(long rxBytes, long txBytes) {
            long currentRxBytes = TrafficStats.getTotalRxBytes();
            long currentTxBytes = TrafficStats.getTotalTxBytes();
            
            // 计算增量
            long rxDelta = currentRxBytes - lastRxBytes;
            long txDelta = currentTxBytes - lastTxBytes;
            
            // 更新网络使用情况
            this.networkUsage += (rxDelta + txDelta);
            
            // 更新上次值
            this.lastRxBytes = currentRxBytes;
            this.lastTxBytes = currentTxBytes;
        }
        
        public Map<Integer, Long> getProcessPids() {
            return processPids;
        }
        
        public void addProcessPid(int pid, long startTime) {
            processPids.put(pid, startTime);
        }
        
        public void removeProcessPid(int pid) {
            processPids.remove(pid);
        }
        
        public boolean hasProcess(int pid) {
            return processPids.containsKey(pid);
        }
    }
    
    /**
     * 资源监控器类
     */
    private class ResourceMonitor implements Runnable {
        private final String appId;
        private final ResourceLimits limits;
        private final ResourceUsage usage;
        private final ResourceMonitorCallback callback;
        private boolean running;
        
        // 记录上次发送的资源警告事件时间戳，防止过于频繁发送
        private final Map<ResourceExceededEvent.Type, Long> lastWarningTime = new HashMap<>();
        // 警告冷却时间（毫秒）
        private static final long WARNING_COOLDOWN = 60000; // 1分钟
        
        public ResourceMonitor(String appId, ResourceLimits limits, ResourceUsage usage, ResourceMonitorCallback callback) {
            this.appId = appId;
            this.limits = limits;
            this.usage = usage;
            this.callback = callback;
            this.running = true;
        }
        
        @Override
        public void run() {
            if (!running) {
                return;
            }
            
            try {
                // 更新资源使用情况
                updateResourceUsage();
                
                // 检查资源限制
                checkResourceLimits();
                
                // 更新监控时间
                usage.updateLastMonitorTime();
                
                // 检查是否需要重置日限额
                usage.resetDaily();
            } catch (Exception e) {
                Log.e(TAG, "资源监控失败: " + appId, e);
            }
        }
        
        private void updateResourceUsage() {
            // 更新存储使用情况
            usage.setStorageUsage(getStorageUsage(appId));
            
            // 更新进程数量
            ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            if (am != null) {
                // 获取所有运行进程
                for (ActivityManager.RunningAppProcessInfo process : am.getRunningAppProcesses()) {
                    if (process.processName.contains(appId)) {
                        usage.addProcessPid(process.pid, System.currentTimeMillis());
                    }
                }
                
                // 清理不存在的进程
                for (int pid : usage.getProcessPids().keySet().toArray(new Integer[0])) {
                    boolean exists = false;
                    for (ActivityManager.RunningAppProcessInfo process : am.getRunningAppProcesses()) {
                        if (process.pid == pid) {
                            exists = true;
                            break;
                        }
                    }
                    if (!exists) {
                        usage.removeProcessPid(pid);
                    }
                }
                
                // 更新进程数
                usage.setProcessCount(usage.getProcessPids().size());
                
                // 更新内存使用情况
                long totalMemory = 0;
                for (int pid : usage.getProcessPids().keySet()) {
                    try {
                        int[] pids = new int[]{pid};
                        android.os.Debug.MemoryInfo[] memoryInfos = am.getProcessMemoryInfo(pids);
                        if (memoryInfos != null && memoryInfos.length > 0) {
                            totalMemory += memoryInfos[0].getTotalPss() * 1024; // PSS in KB, convert to bytes
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "获取内存使用失败: " + pid, e);
                    }
                }
                usage.setMemoryUsage(totalMemory);
            }
            
            // 更新CPU使用情况
            updateCpuUsage();
            
            // 更新网络使用情况
            usage.updateNetwork(TrafficStats.getTotalRxBytes(), TrafficStats.getTotalTxBytes());
        }
        
        private void updateCpuUsage() {
            // 这里使用简化的CPU使用率计算
            // 完整实现需要读取/proc/stat和/proc/<pid>/stat
            // 并计算用户态和系统态CPU使用时间
            float totalCpuUsage = 0;
            int processCount = usage.getProcessCount();
            
            if (processCount > 0) {
                // 简化实现：假设每个进程平均使用10%CPU
                // 实际情况下应该通过读取/proc文件系统获取真实CPU使用率
                totalCpuUsage = processCount * 10.0f;
                
                // 限制最大值为100%
                if (totalCpuUsage > 100) {
                    totalCpuUsage = 100;
                }
            }
            
            usage.setCpuUsage(totalCpuUsage);
        }
        
        private void checkResourceLimits() {
            // 检查存储空间
            checkResourceLimit(ResourceExceededEvent.Type.STORAGE, usage.getStorageUsage(), limits.maxStorage);
            
            // 检查进程数
            checkResourceLimit(ResourceExceededEvent.Type.PROCESS, usage.getProcessCount(), limits.maxProcesses);
            
            // 检查内存使用
            checkResourceLimit(ResourceExceededEvent.Type.MEMORY, usage.getMemoryUsage(), limits.maxMemory);
            
            // 检查CPU使用
            checkResourceLimit(ResourceExceededEvent.Type.CPU, (long)usage.getCpuUsage(), limits.maxCpuUsage);
            
            // 检查网络使用
            checkResourceLimit(ResourceExceededEvent.Type.NETWORK, usage.getNetworkUsage(), limits.maxNetworkUsage);
        }
        
        private void checkResourceLimit(ResourceExceededEvent.Type type, long currentValue, long limitValue) {
            // 计算使用率百分比
            int percentage = (int)(currentValue * 100 / limitValue);
            
            // 如果超出限制，发送超限事件
            if (percentage >= 100) {
                ResourceExceededEvent event = new ResourceExceededEvent(type, currentValue, limitValue);
                notifyResourceExceeded(event);
                return;
            }
            
            // 如果接近限制但未超出，发送警告事件
            if (percentage >= WARNING_THRESHOLD) {
                // 检查是否在冷却期内
                Long lastTime = lastWarningTime.get(type);
                long currentTime = System.currentTimeMillis();
                
                if (lastTime == null || (currentTime - lastTime) >= WARNING_COOLDOWN) {
                    // 记录警告时间
                    lastWarningTime.put(type, currentTime);
                    
                    // 创建警告事件
                    SandboxEvent event = new SandboxEvent(SandboxEventType.RESOURCE_WARNING);
                    event.putData("type", type);
                    event.putData("currentValue", currentValue);
                    event.putData("limitValue", limitValue);
                    event.putData("percentage", percentage);
                    
                    // 触发事件
                    fireSandboxEvent(appId, event);
                    
                    Log.w(TAG, String.format("资源接近限制: %s, %d%% (%d/%d)", 
                        type.name(), percentage, currentValue, limitValue));
                }
            }
        }
        
        private void notifyResourceExceeded(ResourceExceededEvent event) {
            // 创建超限事件
            SandboxEvent sandboxEvent = new SandboxEvent(SandboxEventType.RESOURCE_EXCEEDED);
            sandboxEvent.putData("type", event.getType());
            sandboxEvent.putData("currentValue", event.getCurrentValue());
            sandboxEvent.putData("limitValue", event.getLimitValue());
            sandboxEvent.putData("percentage", event.getPercentage());
            
            // 触发事件
            fireSandboxEvent(appId, sandboxEvent);
            
            // 调用回调
            if (callback != null) {
                mainHandler.post(() -> callback.onResourceExceeded(appId, event));
            }
        }
        
        public void stop() {
            this.running = false;
        }
    }
    
    /**
     * 资源超限事件类
     */
    public static class ResourceExceededEvent {
        public enum Type {
            STORAGE, PROCESS, MEMORY, CPU, NETWORK
        }
        
        private final Type type;
        private final long currentValue;
        private final long limitValue;
        
        public ResourceExceededEvent(Type type, long currentValue, long limitValue) {
            this.type = type;
            this.currentValue = currentValue;
            this.limitValue = limitValue;
        }
        
        public Type getType() {
            return type;
        }
        
        public long getCurrentValue() {
            return currentValue;
        }
        
        public long getLimitValue() {
            return limitValue;
        }
        
        public int getPercentage() {
            if (limitValue <= 0) {
                return 100;
            }
            return (int) (currentValue * 100 / limitValue);
        }
        
        @Override
        public String toString() {
            String typeStr;
            String unitStr;
            
            switch (type) {
                case STORAGE:
                    typeStr = "存储空间";
                    unitStr = "字节";
                    break;
                case PROCESS:
                    typeStr = "进程数";
                    unitStr = "个";
                    break;
                case MEMORY:
                    typeStr = "内存使用";
                    unitStr = "字节";
                    break;
                case CPU:
                    typeStr = "CPU使用率";
                    unitStr = "%";
                    break;
                case NETWORK:
                    typeStr = "网络流量";
                    unitStr = "字节";
                    break;
                default:
                    typeStr = "未知资源";
                    unitStr = "";
                    break;
            }
            
            return typeStr + "超出限制: " + currentValue + unitStr + " > " + limitValue + unitStr + " (" + getPercentage() + "%)";
        }
    }
    
    /**
     * 创建一个新的沙箱管理器
     * @param context 上下文
     */
    public MpkSandbox(Context context) {
        this.context = context;
        this.sandboxRoot = new File(context.getFilesDir(), "sandbox");
        if (!this.sandboxRoot.exists()) {
            this.sandboxRoot.mkdirs();
        }
        this.sandboxes = new ConcurrentHashMap<>();
        this.resourceMonitors = new ConcurrentHashMap<>();
        this.resourceUsages = new ConcurrentHashMap<>();
        this.eventListeners = new ConcurrentHashMap<>();
        this.scheduler = Executors.newScheduledThreadPool(1);
        this.mainHandler = new Handler(Looper.getMainLooper());
    }
    
    /**
     * 创建应用沙箱
     * @param appId 应用 ID
     * @param limits 资源限制
     * @return 沙箱环境
     * @throws IOException 如果创建失败
     */
    public SandboxEnvironment createSandbox(String appId, ResourceLimits limits) throws IOException {
        // 检查是否已存在
        if (sandboxes.containsKey(appId)) {
            Log.w(TAG, "沙箱已存在: " + appId);
            return sandboxes.get(appId);
        }
        
        try {
            // 创建沙箱根目录
            File sandboxDir = new File(sandboxRoot, appId);
            if (!sandboxDir.exists() && !sandboxDir.mkdirs()) {
                throw new IOException("创建沙箱目录失败: " + sandboxDir);
            }
            
            // 创建沙箱环境
            SandboxEnvironment env = new SandboxEnvironment(sandboxDir, limits);
            
            // 设置目录权限
            setDirectoryPermissions(env);
            
            // 添加到沙箱映射表
            sandboxes.put(appId, env);
            
            // 创建资源使用情况
            ResourceUsage usage = new ResourceUsage();
            resourceUsages.put(appId, usage);
            
            // 触发沙箱创建事件
            SandboxEvent event = new SandboxEvent(SandboxEventType.SANDBOX_CREATED);
            event.putData("limits", limits);
            fireSandboxEvent(appId, event);
            
            Log.i(TAG, "创建沙箱成功: " + appId);
            return env;
        } catch (Exception e) {
            Log.e(TAG, "创建沙箱失败: " + appId, e);
            throw new IOException("创建沙箱失败: " + appId, e);
        }
    }
    
    /**
     * 删除应用沙箱
     * @param appId 应用 ID
     * @return 是否成功删除
     */
    public boolean deleteSandbox(String appId) {
        // 检查是否存在
        if (!sandboxes.containsKey(appId)) {
            Log.w(TAG, "沙箱不存在: " + appId);
            return false;
        }
        
        try {
            // 停止资源监控
            stopResourceMonitor(appId);
            
            // 获取沙箱环境
            SandboxEnvironment env = sandboxes.get(appId);
            
            // 删除沙箱目录
            deleteDirectory(env.rootDir);
            
            // 从沙箱映射表中移除
            sandboxes.remove(appId);
            
            // 移除资源使用情况
            resourceUsages.remove(appId);
            
            // 移除事件监听器
            eventListeners.remove(appId);
            
            // 触发沙箱删除事件
            SandboxEvent event = new SandboxEvent(SandboxEventType.SANDBOX_DELETED);
            fireSandboxEvent(appId, event);
            
            Log.i(TAG, "删除沙箱成功: " + appId);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "删除沙箱失败: " + appId, e);
            return false;
        }
    }
    
    /**
     * 获取应用沙箱
     * @param appId 应用 ID
     * @return 沙箱环境
     */
    public SandboxEnvironment getSandbox(String appId) {
        return sandboxes.get(appId);
    }
    
    /**
     * 检查应用沙箱是否存在
     * @param appId 应用 ID
     * @return 是否存在
     */
    public boolean hasSandbox(String appId) {
        return sandboxes.containsKey(appId);
    }
    
    /**
     * 获取所有应用沙箱
     * @return 沙箱映射表
     */
    public Map<String, SandboxEnvironment> getAllSandboxes() {
        return new HashMap<>(sandboxes);
    }
    
    /**
     * 检查存储空间使用
     * @param appId 应用 ID
     * @return 已使用空间（字节）
     */
    public long getStorageUsage(String appId) {
        SandboxEnvironment env = sandboxes.get(appId);
        if (env == null) {
            return 0;
        }
        
        return calculateDirectorySize(env.rootDir);
    }
    
    /**
     * 获取应用资源使用情况
     * @param appId 应用 ID
     * @return 资源使用情况
     */
    public ResourceUsage getResourceUsage(String appId) {
        return resourceUsages.get(appId);
    }
    
    /**
     * 启动资源监控
     * @param appId 应用 ID
     * @param callback 资源监控回调
     * @return 是否成功启动
     */
    public boolean startResourceMonitor(String appId, ResourceMonitorCallback callback) {
        // 检查是否已存在监控器
        if (resourceMonitors.containsKey(appId)) {
            Log.w(TAG, "资源监控器已存在: " + appId);
            return false;
        }
        
        // 检查是否存在沙箱
        SandboxEnvironment env = sandboxes.get(appId);
        if (env == null) {
            Log.e(TAG, "沙箱不存在: " + appId);
            return false;
        }
        
        // 获取资源使用情况
        ResourceUsage usage = resourceUsages.get(appId);
        if (usage == null) {
            usage = new ResourceUsage();
            resourceUsages.put(appId, usage);
        }
        
        // 创建监控器
        ResourceMonitor monitor = new ResourceMonitor(appId, env.limits, usage, callback);
        resourceMonitors.put(appId, monitor);
        
        // 启动监控
        scheduler.scheduleAtFixedRate(monitor, 0, env.limits.monitorInterval, TimeUnit.MILLISECONDS);
        
        Log.i(TAG, "启动资源监控成功: " + appId);
        return true;
    }
    
    /**
     * 停止资源监控
     * @param appId 应用 ID
     * @return 是否成功停止
     */
    public boolean stopResourceMonitor(String appId) {
        // 检查是否存在监控器
        ResourceMonitor monitor = resourceMonitors.get(appId);
        if (monitor == null) {
            return false;
        }
        
        // 停止监控
        monitor.stop();
        resourceMonitors.remove(appId);
        
        Log.i(TAG, "停止资源监控成功: " + appId);
        return true;
    }
    
    /**
     * 清理缓存
     * @param appId 应用 ID
     * @return 是否成功清理
     */
    public boolean clearCache(String appId) {
        SandboxEnvironment env = sandboxes.get(appId);
        if (env == null) {
            return false;
        }
        
        try {
            deleteDirectory(env.cacheDir);
            env.cacheDir.mkdirs();
            Log.i(TAG, "清理缓存成功: " + appId);
            
            // 更新存储使用情况
            ResourceUsage usage = resourceUsages.get(appId);
            if (usage != null) {
                usage.setStorageUsage(getStorageUsage(appId));
            }
            
            // 触发资源清理事件
            SandboxEvent event = new SandboxEvent(SandboxEventType.RESOURCE_CLEARED);
            event.putData("type", "cache");
            event.putData("path", env.cacheDir.getAbsolutePath());
            fireSandboxEvent(appId, event);
            
            return true;
        } catch (Exception e) {
            Log.e(TAG, "清理缓存失败: " + appId, e);
            return false;
        }
    }
    
    /**
     * 清理临时文件
     * @param appId 应用 ID
     * @return 是否成功清理
     */
    public boolean clearTemp(String appId) {
        SandboxEnvironment env = sandboxes.get(appId);
        if (env == null) {
            return false;
        }
        
        try {
            deleteDirectory(env.tempDir);
            env.tempDir.mkdirs();
            Log.i(TAG, "清理临时文件成功: " + appId);
            
            // 更新存储使用情况
            ResourceUsage usage = resourceUsages.get(appId);
            if (usage != null) {
                usage.setStorageUsage(getStorageUsage(appId));
            }
            
            // 触发资源清理事件
            SandboxEvent event = new SandboxEvent(SandboxEventType.RESOURCE_CLEARED);
            event.putData("type", "temp");
            event.putData("path", env.tempDir.getAbsolutePath());
            fireSandboxEvent(appId, event);
            
            return true;
        } catch (Exception e) {
            Log.e(TAG, "清理临时文件失败: " + appId, e);
            return false;
        }
    }
    
    /**
     * 设置目录权限
     * @param env 沙箱环境
     */
    private void setDirectoryPermissions(SandboxEnvironment env) {
        // 设置根目录权限
        env.rootDir.setReadable(true, false);
        env.rootDir.setWritable(true, false);
        env.rootDir.setExecutable(true, false);
        
        // 设置数据目录权限
        env.dataDir.setReadable(true, false);
        env.dataDir.setWritable(true, false);
        env.dataDir.setExecutable(true, false);
        
        // 设置缓存目录权限
        env.cacheDir.setReadable(true, false);
        env.cacheDir.setWritable(true, false);
        env.cacheDir.setExecutable(true, false);
        
        // 设置临时目录权限
        env.tempDir.setReadable(true, false);
        env.tempDir.setWritable(true, false);
        env.tempDir.setExecutable(true, false);
        
        // 设置共享目录权限
        env.sharedDir.setReadable(true, false);
        env.sharedDir.setWritable(true, false);
        env.sharedDir.setExecutable(true, false);
    }
    
    /**
     * 删除目录及其内容
     * @param dir 目录
     * @return 是否成功删除
     */
    private boolean deleteDirectory(File dir) {
        if (dir.exists()) {
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
        }
        return dir.delete();
    }
    
    /**
     * 计算目录大小
     * @param dir 目录
     * @return 目录大小（字节）
     */
    private long calculateDirectorySize(File dir) {
        long size = 0;
        if (dir.exists()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        size += calculateDirectorySize(file);
                    } else {
                        size += file.length();
                    }
                }
            }
        }
        return size;
    }
    
    /**
     * 添加沙箱事件监听器
     * @param appId 应用 ID
     * @param listener 监听器
     * @return 是否成功添加
     */
    public boolean addEventListener(String appId, SandboxEventListener listener) {
        if (!sandboxes.containsKey(appId)) {
            Log.e(TAG, "沙箱不存在: " + appId);
            return false;
        }
        
        Set<SandboxEventListener> listeners = eventListeners.get(appId);
        if (listeners == null) {
            listeners = new HashSet<>();
            eventListeners.put(appId, listeners);
        }
        
        listeners.add(listener);
        Log.d(TAG, "添加事件监听器: " + appId);
        return true;
    }
    
    /**
     * 移除沙箱事件监听器
     * @param appId 应用 ID
     * @param listener 监听器
     * @return 是否成功移除
     */
    public boolean removeEventListener(String appId, SandboxEventListener listener) {
        Set<SandboxEventListener> listeners = eventListeners.get(appId);
        if (listeners == null) {
            return false;
        }
        
        boolean result = listeners.remove(listener);
        if (listeners.isEmpty()) {
            eventListeners.remove(appId);
        }
        
        if (result) {
            Log.d(TAG, "移除事件监听器: " + appId);
        }
        
        return result;
    }
    
    /**
     * 触发沙箱事件
     * @param appId 应用 ID
     * @param event 事件
     */
    private void fireSandboxEvent(String appId, SandboxEvent event) {
        Set<SandboxEventListener> listeners = eventListeners.get(appId);
        if (listeners != null && !listeners.isEmpty()) {
            mainHandler.post(() -> {
                for (SandboxEventListener listener : listeners) {
                    try {
                        listener.onSandboxEvent(appId, event);
                    } catch (Exception e) {
                        Log.e(TAG, "事件监听器异常: " + appId, e);
                    }
                }
            });
        }
    }
    
    /**
     * 关闭沙箱管理器
     */
    public void shutdown() {
        // 停止所有监控器
        for (String appId : resourceMonitors.keySet().toArray(new String[0])) {
            stopResourceMonitor(appId);
        }
        
        // 清空事件监听器
        eventListeners.clear();
        
        // 关闭调度器
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        Log.i(TAG, "沙箱管理器已关闭");
    }
} 