package com.mobileplatform.creator.mpk;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * MPK 进程管理器
 * 用于管理应用进程的创建、运行和终止
 */
public class MpkProcessManager {
    private static final String TAG = "MpkProcessManager";
    
    // 上下文
    private Context context;
    
    // 应用进程表
    private Map<String, List<MpkProcess>> appProcesses;
    
    // 进程ID计数器
    private AtomicInteger processIdCounter;
    
    // 进程表
    private Map<Integer, MpkProcess> processes;
    
    // 进程监控器
    private ScheduledExecutorService processMonitor;
    
    // 主线程处理器
    private Handler mainHandler;
    
    // 监控间隔（毫秒）
    private static final long MONITOR_INTERVAL = 5000;
    
    // 最大进程数
    private static final int MAX_TOTAL_PROCESSES = 100;
    
    /**
     * 进程回调接口
     */
    public interface ProcessCallback {
        void onProcessStarted(MpkProcess process);
        void onProcessStopped(MpkProcess process, int exitCode);
        void onProcessFailed(MpkProcess process, Exception error);
    }
    
    /**
     * MPK 进程类
     */
    public static class MpkProcess {
        // 进程ID
        private int pid;
        
        // 进程名称
        private String name;
        
        // 应用ID
        private String appId;
        
        // 进程类型
        private ProcessType type;
        
        // 进程状态
        private ProcessState state;
        
        // 启动时间
        private long startTime;
        
        // 停止时间
        private long stopTime;
        
        // 退出代码
        private int exitCode;
        
        // 进程优先级
        private int priority;
        
        // 进程工作目录
        private File workingDir;
        
        // 进程环境变量
        private Map<String, String> environmentVars;
        
        // 回调
        private ProcessCallback callback;
        
        // 系统进程（如果有）
        private java.lang.Process systemProcess;
        
        // 内存使用量（字节）
        private long memoryUsage;
        
        // CPU使用率（百分比）
        private float cpuUsage;
        
        // 命令行
        private List<String> command;
        
        /**
         * 构造函数
         * 
         * @param pid 进程ID
         * @param name 进程名称
         * @param appId 应用ID
         * @param type 进程类型
         */
        public MpkProcess(int pid, String name, String appId, ProcessType type) {
            this.pid = pid;
            this.name = name;
            this.appId = appId;
            this.type = type;
            this.state = ProcessState.CREATED;
            this.startTime = 0;
            this.stopTime = 0;
            this.exitCode = 0;
            this.priority = Process.THREAD_PRIORITY_DEFAULT;
            this.environmentVars = new HashMap<>();
            this.memoryUsage = 0;
            this.cpuUsage = 0;
            this.command = new ArrayList<>();
        }
        
        /**
         * 获取进程ID
         * 
         * @return 进程ID
         */
        public int getPid() {
            return pid;
        }
        
        /**
         * 获取进程名称
         * 
         * @return 进程名称
         */
        public String getName() {
            return name;
        }
        
        /**
         * 获取应用ID
         * 
         * @return 应用ID
         */
        public String getAppId() {
            return appId;
        }
        
        /**
         * 获取进程类型
         * 
         * @return 进程类型
         */
        public ProcessType getType() {
            return type;
        }
        
        /**
         * 获取进程状态
         * 
         * @return 进程状态
         */
        public ProcessState getState() {
            return state;
        }
        
        /**
         * 获取启动时间
         * 
         * @return 启动时间（毫秒）
         */
        public long getStartTime() {
            return startTime;
        }
        
        /**
         * 获取停止时间
         * 
         * @return 停止时间（毫秒）
         */
        public long getStopTime() {
            return stopTime;
        }
        
        /**
         * 获取运行时间
         * 
         * @return 运行时间（毫秒）
         */
        public long getRunningTime() {
            if (state == ProcessState.RUNNING) {
                return System.currentTimeMillis() - startTime;
            } else if (state == ProcessState.STOPPED) {
                return stopTime - startTime;
            } else {
                return 0;
            }
        }
        
        /**
         * 获取退出代码
         * 
         * @return 退出代码
         */
        public int getExitCode() {
            return exitCode;
        }
        
        /**
         * 获取进程优先级
         * 
         * @return 进程优先级
         */
        public int getPriority() {
            return priority;
        }
        
        /**
         * 设置进程优先级
         * 
         * @param priority 进程优先级
         */
        public void setPriority(int priority) {
            this.priority = priority;
            
            // 如果是系统进程，尝试设置优先级
            if (systemProcess != null) {
                try {
                    // 获取系统进程的PID
                    int sysPid = getSystemProcessPid();
                    if (sysPid > 0) {
                        Process.setThreadPriority(sysPid, priority);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "设置进程优先级失败: " + name, e);
                }
            }
        }
        
        /**
         * 获取系统进程PID
         * 
         * @return 系统进程PID
         */
        private int getSystemProcessPid() {
            try {
                // 这里使用反射尝试获取系统进程的PID
                // 注意：这种方式可能不适用于所有Android版本
                java.lang.reflect.Field f = systemProcess.getClass().getDeclaredField("pid");
                f.setAccessible(true);
                return (int) f.get(systemProcess);
            } catch (Exception e) {
                Log.e(TAG, "获取系统进程PID失败: " + name, e);
                return -1;
            }
        }
        
        /**
         * 获取工作目录
         * 
         * @return 工作目录
         */
        public File getWorkingDir() {
            return workingDir;
        }
        
        /**
         * 设置工作目录
         * 
         * @param workingDir 工作目录
         */
        public void setWorkingDir(File workingDir) {
            this.workingDir = workingDir;
        }
        
        /**
         * 获取环境变量
         * 
         * @return 环境变量
         */
        public Map<String, String> getEnvironmentVars() {
            return new HashMap<>(environmentVars);
        }
        
        /**
         * 设置环境变量
         * 
         * @param name 变量名
         * @param value 变量值
         */
        public void setEnvironmentVar(String name, String value) {
            environmentVars.put(name, value);
        }
        
        /**
         * 设置回调
         * 
         * @param callback 回调
         */
        public void setCallback(ProcessCallback callback) {
            this.callback = callback;
        }
        
        /**
         * 启动进程
         * 
         * @return 是否成功启动
         * @throws IOException 如果启动失败
         */
        public boolean start() throws IOException {
            if (state != ProcessState.CREATED && state != ProcessState.STOPPED) {
                Log.w(TAG, "进程已启动或正在启动: " + name);
                return false;
            }
            
            try {
                switch (type) {
                    case NATIVE:
                        startNativeProcess();
                        break;
                    case JAVASCRIPT:
                        startJavaScriptProcess();
                        break;
                    case VIRTUAL:
                        startVirtualProcess();
                        break;
                    default:
                        throw new IllegalStateException("不支持的进程类型: " + type);
                }
                
                state = ProcessState.RUNNING;
                startTime = System.currentTimeMillis();
                stopTime = 0;
                
                if (callback != null) {
                    callback.onProcessStarted(this);
                }
                
                Log.i(TAG, "进程已启动: " + name + " (pid=" + pid + ")");
                return true;
            } catch (Exception e) {
                state = ProcessState.FAILED;
                
                if (callback != null) {
                    callback.onProcessFailed(this, e);
                }
                
                Log.e(TAG, "启动进程失败: " + name, e);
                throw new IOException("启动进程失败: " + name, e);
            }
        }
        
        /**
         * 启动原生进程
         */
        private void startNativeProcess() {
            try {
                // 如果未设置命令，则创建默认命令
                if (command == null || command.isEmpty()) {
                    command = new ArrayList<>();
                    command.add("/system/bin/sh");
                    command.add("-c");
                    command.add("echo 'Hello from " + name + "' && sleep 10");
                }
                
                // 创建进程构建器
                ProcessBuilder pb = new ProcessBuilder(command);
                
                // 设置工作目录
                if (workingDir != null) {
                    pb.directory(workingDir);
                }
                
                // 设置环境变量
                Map<String, String> env = pb.environment();
                env.putAll(environmentVars);
                
                // 启动进程
                java.lang.Process proc = pb.start();
                systemProcess = proc;
                
                // 设置优先级
                setPriority(priority);
            } catch (IOException e) {
                Log.e(TAG, "启动进程失败: " + e.getMessage(), e);
                if (callback != null) {
                    callback.onProcessFailed(this, e);
                }
            }
        }
        
        /**
         * 启动JavaScript进程
         */
        private void startJavaScriptProcess() {
            // JavaScript进程在JavaScript运行时中运行
            // 这里只是模拟一个进程状态
            Log.i(TAG, "启动JavaScript进程: " + name);
        }
        
        /**
         * 启动虚拟进程
         */
        private void startVirtualProcess() {
            // 虚拟进程不是真正的系统进程，只是在MPK进程管理器中的一个抽象
            Log.i(TAG, "启动虚拟进程: " + name);
        }
        
        /**
         * 停止进程
         * 
         * @return 是否成功停止
         */
        public boolean stop() {
            return stop(0);
        }
        
        /**
         * 停止进程
         * 
         * @param exitCode 退出代码
         * @return 是否成功停止
         */
        public boolean stop(int exitCode) {
            if (state != ProcessState.RUNNING) {
                Log.w(TAG, "进程未运行: " + name);
                return false;
            }
            
            try {
                switch (type) {
                    case NATIVE:
                        stopNativeProcess();
                        break;
                    case JAVASCRIPT:
                        stopJavaScriptProcess();
                        break;
                    case VIRTUAL:
                        stopVirtualProcess();
                        break;
                }
                
                state = ProcessState.STOPPED;
                stopTime = System.currentTimeMillis();
                this.exitCode = exitCode;
                
                if (callback != null) {
                    callback.onProcessStopped(this, exitCode);
                }
                
                Log.i(TAG, "进程已停止: " + name + " (pid=" + pid + ", exitCode=" + exitCode + ")");
                return true;
            } catch (Exception e) {
                Log.e(TAG, "停止进程失败: " + name, e);
                return false;
            }
        }
        
        /**
         * 停止原生进程
         */
        private void stopNativeProcess() {
            if (systemProcess != null) {
                systemProcess.destroy();
                systemProcess = null;
            }
        }
        
        /**
         * 停止JavaScript进程
         */
        private void stopJavaScriptProcess() {
            // JavaScript进程在JavaScript运行时中运行
            // 这里只是模拟一个进程状态
            Log.i(TAG, "停止JavaScript进程: " + name);
        }
        
        /**
         * 停止虚拟进程
         */
        private void stopVirtualProcess() {
            // 虚拟进程不是真正的系统进程，只是在MPK进程管理器中的一个抽象
            Log.i(TAG, "停止虚拟进程: " + name);
        }
        
        /**
         * 是否在运行
         * 
         * @return 是否在运行
         */
        public boolean isRunning() {
            if (state != ProcessState.RUNNING) {
                return false;
            }
            
            if (type == ProcessType.NATIVE && systemProcess != null) {
                try {
                    // 检查系统进程是否在运行
                    int exitValue = systemProcess.exitValue();
                    
                    // 如果能获取退出值，说明进程已经结束
                    state = ProcessState.STOPPED;
                    stopTime = System.currentTimeMillis();
                    this.exitCode = exitValue;
                    
                    if (callback != null) {
                        callback.onProcessStopped(this, exitValue);
                    }
                    
                    return false;
                } catch (IllegalThreadStateException e) {
                    // 如果抛出异常，说明进程仍在运行
                    return true;
                }
            }
            
            return true;
        }
        
        /**
         * 获取内存使用量
         * 
         * @return 内存使用量（字节）
         */
        public long getMemoryUsage() {
            return memoryUsage;
        }
        
        /**
         * 设置内存使用量
         * 
         * @param memoryUsage 内存使用量（字节）
         */
        public void setMemoryUsage(long memoryUsage) {
            this.memoryUsage = memoryUsage;
        }
        
        /**
         * 获取CPU使用率
         * 
         * @return CPU使用率（百分比）
         */
        public float getCpuUsage() {
            return cpuUsage;
        }
        
        /**
         * 设置CPU使用率
         * 
         * @param cpuUsage CPU使用率（百分比）
         */
        public void setCpuUsage(float cpuUsage) {
            this.cpuUsage = cpuUsage;
        }
        
        @Override
        public String toString() {
            return "MpkProcess{" +
                    "pid=" + pid +
                    ", name='" + name + '\'' +
                    ", appId='" + appId + '\'' +
                    ", type=" + type +
                    ", state=" + state +
                    ", priority=" + priority +
                    '}';
        }
    }
    
    /**
     * 进程类型
     */
    public enum ProcessType {
        // 原生进程
        NATIVE,
        
        // JavaScript进程
        JAVASCRIPT,
        
        // 虚拟进程
        VIRTUAL
    }
    
    /**
     * 进程状态
     */
    public enum ProcessState {
        // 已创建
        CREATED,
        
        // 正在运行
        RUNNING,
        
        // 已停止
        STOPPED,
        
        // 启动失败
        FAILED
    }
    
    /**
     * 构造函数
     */
    public MpkProcessManager() {
        this(null);
    }
    
    /**
     * 构造函数
     * 
     * @param context 上下文
     */
    public MpkProcessManager(Context context) {
        this.context = context;
        this.appProcesses = new ConcurrentHashMap<>();
        this.processIdCounter = new AtomicInteger(1);
        this.processes = new ConcurrentHashMap<>();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }
    
    /**
     * 启动进程监控
     */
    public void startProcessMonitor() {
        if (processMonitor == null || processMonitor.isShutdown()) {
            processMonitor = Executors.newSingleThreadScheduledExecutor();
            processMonitor.scheduleAtFixedRate(this::monitorProcesses, 0, MONITOR_INTERVAL, TimeUnit.MILLISECONDS);
            Log.i(TAG, "进程监控已启动");
        }
    }
    
    /**
     * 停止进程监控
     */
    public void stopProcessMonitor() {
        if (processMonitor != null && !processMonitor.isShutdown()) {
            processMonitor.shutdown();
            try {
                if (!processMonitor.awaitTermination(5, TimeUnit.SECONDS)) {
                    processMonitor.shutdownNow();
                }
            } catch (InterruptedException e) {
                processMonitor.shutdownNow();
                Thread.currentThread().interrupt();
            }
            Log.i(TAG, "进程监控已停止");
        }
    }
    
    /**
     * 监控进程
     */
    private void monitorProcesses() {
        try {
            for (MpkProcess process : processes.values()) {
                if (process.getState() == ProcessState.RUNNING) {
                    // 检查进程是否仍在运行
                    if (!process.isRunning()) {
                        Log.i(TAG, "进程已结束: " + process.getName() + " (exitCode=" + process.getExitCode() + ")");
                    } else {
                        // 更新进程资源使用情况
                        updateProcessResourceUsage(process);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "监控进程失败", e);
        }
    }
    
    /**
     * 更新进程资源使用情况
     * 
     * @param process 进程
     */
    private void updateProcessResourceUsage(MpkProcess process) {
        if (context == null) {
            return;
        }
        
        try {
            // 获取进程统计信息
            ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            
            if (process.getType() == ProcessType.NATIVE && process.systemProcess != null) {
                int sysPid = process.getSystemProcessPid();
                if (sysPid > 0) {
                    // 获取系统进程的内存信息
                    int[] pids = new int[]{sysPid};
                    android.os.Debug.MemoryInfo[] memoryInfos = am.getProcessMemoryInfo(pids);
                    if (memoryInfos != null && memoryInfos.length > 0) {
                        android.os.Debug.MemoryInfo memoryInfo = memoryInfos[0];
                        long totalPss = memoryInfo.getTotalPss() * 1024L; // 转换为字节
                        process.setMemoryUsage(totalPss);
                    }
                    
                    // 获取系统进程的CPU信息
                    // 注意：实际获取CPU使用率需要多次采样和计算
                    // 这里只是简单模拟一个CPU使用率
                    float cpuUsage = (float) (Math.random() * 10.0); // 0-10%
                    process.setCpuUsage(cpuUsage);
                }
            } else if (process.getType() == ProcessType.JAVASCRIPT) {
                // 对于JavaScript进程，可以从JavaScript运行时获取资源使用情况
                // 这里只是简单模拟
                process.setMemoryUsage((long) (Math.random() * 10 * 1024 * 1024)); // 0-10MB
                process.setCpuUsage((float) (Math.random() * 5.0)); // 0-5%
            }
        } catch (Exception e) {
            Log.e(TAG, "更新进程资源使用情况失败: " + process.getName(), e);
        }
    }
    
    /**
     * 创建进程
     * 
     * @param appId 应用ID
     * @param processName 进程名称
     * @param type 进程类型
     * @return 进程对象
     */
    public MpkProcess createProcess(String appId, String processName, ProcessType type) {
        // 检查进程总数是否超过限制
        if (processes.size() >= MAX_TOTAL_PROCESSES) {
            Log.e(TAG, "进程数超过限制: " + MAX_TOTAL_PROCESSES);
            return null;
        }
        
        // 生成进程ID
        int pid = processIdCounter.getAndIncrement();
        
        // 创建进程对象
        MpkProcess process = new MpkProcess(pid, processName, appId, type);
        
        // 添加到进程表
        processes.put(pid, process);
        
        // 添加到应用进程表
        List<MpkProcess> appProcessList = appProcesses.computeIfAbsent(appId, k -> new CopyOnWriteArrayList<>());
        appProcessList.add(process);
        
        Log.i(TAG, "创建进程: " + processName + " (pid=" + pid + ", appId=" + appId + ", type=" + type + ")");
        return process;
    }
    
    /**
     * 启动进程
     * 
     * @param process 进程对象
     * @param callback 回调
     * @return 是否成功启动
     * @throws IOException 如果启动失败
     */
    public boolean startProcess(MpkProcess process, ProcessCallback callback) throws IOException {
        if (process == null) {
            return false;
        }
        
        // 设置回调
        process.setCallback(callback);
        
        // 启动进程
        return process.start();
    }
    
    /**
     * 停止进程
     * 
     * @param pid 进程ID
     * @return 是否成功停止
     */
    public boolean stopProcess(int pid) {
        MpkProcess process = processes.get(pid);
        if (process == null) {
            Log.w(TAG, "进程不存在: " + pid);
            return false;
        }
        
        return process.stop();
    }
    
    /**
     * 设置进程优先级
     * 
     * @param pid 进程ID
     * @param priority 优先级
     * @return 是否成功设置
     */
    public boolean setProcessPriority(int pid, int priority) {
        MpkProcess process = processes.get(pid);
        if (process == null) {
            Log.w(TAG, "进程不存在: " + pid);
            return false;
        }
        
        process.setPriority(priority);
        return true;
    }
    
    /**
     * 获取应用的所有进程
     * 
     * @param appId 应用ID
     * @return 进程列表
     */
    public List<MpkProcess> getAppProcesses(String appId) {
        List<MpkProcess> processes = appProcesses.get(appId);
        return processes != null ? new ArrayList<>(processes) : new ArrayList<>();
    }
    
    /**
     * 获取应用的进程数量
     * 
     * @param appId 应用ID
     * @return 进程数量
     */
    public int getAppProcessCount(String appId) {
        List<MpkProcess> processes = appProcesses.get(appId);
        return processes != null ? processes.size() : 0;
    }
    
    /**
     * 停止应用的所有进程
     * 
     * @param appId 应用ID
     * @return 是否全部成功停止
     */
    public boolean stopAppProcesses(String appId) {
        List<MpkProcess> processes = appProcesses.get(appId);
        if (processes == null || processes.isEmpty()) {
            return true;
        }
        
        boolean allStopped = true;
        for (MpkProcess process : processes) {
            if (!process.stop()) {
                allStopped = false;
            }
        }
        
        return allStopped;
    }
    
    /**
     * 获取进程
     * 
     * @param pid 进程ID
     * @return 进程对象
     */
    public MpkProcess getProcess(int pid) {
        return processes.get(pid);
    }
    
    /**
     * 获取所有进程
     * 
     * @return 进程列表
     */
    public List<MpkProcess> getAllProcesses() {
        return new ArrayList<>(processes.values());
    }
    
    /**
     * 获取运行中的进程数
     * 
     * @return 运行中的进程数
     */
    public int getRunningProcessCount() {
        int count = 0;
        for (MpkProcess process : processes.values()) {
            if (process.getState() == ProcessState.RUNNING) {
                count++;
            }
        }
        return count;
    }
    
    /**
     * 清理已停止的进程
     * 
     * @return 清理的进程数
     */
    public int cleanupStoppedProcesses() {
        int count = 0;
        
        for (Map.Entry<Integer, MpkProcess> entry : processes.entrySet()) {
            MpkProcess process = entry.getValue();
            if (process.getState() == ProcessState.STOPPED || process.getState() == ProcessState.FAILED) {
                int pid = entry.getKey();
                processes.remove(pid);
                
                // 从应用进程表中移除
                List<MpkProcess> appProcessList = appProcesses.get(process.getAppId());
                if (appProcessList != null) {
                    appProcessList.remove(process);
                }
                
                count++;
            }
        }
        
        return count;
    }
    
    /**
     * 关闭进程管理器
     */
    public void shutdown() {
        // 停止进程监控
        stopProcessMonitor();
        
        // 停止所有进程
        for (MpkProcess process : processes.values()) {
            process.stop();
        }
        
        // 清理资源
        processes.clear();
        appProcesses.clear();
        
        Log.i(TAG, "进程管理器已关闭");
    }
} 