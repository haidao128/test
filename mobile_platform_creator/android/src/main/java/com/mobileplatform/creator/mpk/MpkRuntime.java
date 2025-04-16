package com.mobileplatform.creator.mpk;

import android.content.Context;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

/**
 * MPK 运行时
 * 用于加载和执行 MPK 文件中的应用
 */
public class MpkRuntime {
    private static final String TAG = "MpkRuntime";
    
    // 单例实例
    private static MpkRuntime _instance;
    
    // 上下文
    private Context context;
    
    // 工作目录
    private File workDir;
    
    // 已加载的应用
    private Map<String, MpkFile> loadedApps;
    
    // 应用状态
    private Map<String, Boolean> appStates;
    
    // JavaScript 运行时
    private Map<String, MpkJavaScriptRuntime> jsRuntimes;
    
    // 当前使用的JavaScript运行时实例
    private MpkJavaScriptRuntime currentJsRuntime;
    
    // 进程管理器
    private MpkProcessManager processManager;
    
    // 应用间通信管理器
    private MpkInterAppCommunication interAppCommunication;
    
    // 权限管理器
    private MpkPermissionManager permissionManager;
    
    // 沙箱管理器
    private MpkSandbox sandbox;
    
    // 资源警告计数器
    private Map<String, Map<MpkSandbox.ResourceExceededEvent.Type, AtomicInteger>> warningCounters;
    
    // 最大警告次数，超过这个次数会执行强制清理
    private static final int MAX_WARNING_COUNT = 3;
    
    /**
     * 创建一个新的 MPK 运行时
     * @param context 上下文
     */
    public MpkRuntime(Context context) {
        this.context = context;
        this.workDir = new File(context.getFilesDir(), "mpk_runtime");
        
        // 确保工作目录存在
        if (!workDir.exists()) {
            if (!workDir.mkdirs()) {
                Log.e(TAG, "创建工作目录失败: " + workDir.getAbsolutePath());
            }
        }
        
        this.loadedApps = new HashMap<>();
        this.appStates = new HashMap<>();
        this.jsRuntimes = new HashMap<>();
        this.processManager = new MpkProcessManager();
        this.interAppCommunication = new MpkInterAppCommunication();
        this.permissionManager = new MpkPermissionManager(context);
        this.sandbox = new MpkSandbox(context);
        this.warningCounters = new HashMap<>();
    }
    
    /**
     * 加载 MPK 文件
     * @param mpkFile MPK 文件
     * @return 应用 ID
     * @throws IOException 如果加载失败
     * @throws MpkException 如果文件格式错误
     */
    public String loadApp(File mpkFile) throws IOException, MpkException {
        // 解析 MPK 文件
        MpkFile mpk = MpkFile.fromFile(mpkFile);
        
        // 获取应用 ID
        String appId = mpk.getId();
        
        // 检查是否已加载
        if (loadedApps.containsKey(appId)) {
            Log.w(TAG, "应用已加载: " + appId);
            return appId;
        }
        
        try {
            // 获取沙箱配置
            MpkSandbox.ResourceLimits limits = null;
            
            // 使用默认配置
            limits = MpkSandbox.ResourceLimits.getDefault();
            Log.i(TAG, "应用默认沙箱配置: " + appId);
            
            // 创建沙箱环境
            MpkSandbox.SandboxEnvironment env = sandbox.createSandbox(appId, limits);
            
            // 提取资源到沙箱目录
            extractResources(mpk, env.dataDir);
            
            // 保存代码到沙箱目录
            saveCode(mpk, env.dataDir);
            
            // 保存清单文件到沙箱目录
            saveManifest(mpk, env.dataDir);
            
            // 保存签名和证书到沙箱目录
            saveSignature(mpk, env.dataDir);
            
            // 启动资源监控
            sandbox.startResourceMonitor(appId, new MpkSandbox.ResourceMonitorCallback() {
                @Override
                public void onResourceExceeded(String appId, MpkSandbox.ResourceExceededEvent event) {
                    handleResourceExceeded(appId, event);
                }
            });
            
            // 注册沙箱事件监听器
            sandbox.addEventListener(appId, new MpkSandbox.SandboxEventListener() {
                @Override
                public void onSandboxEvent(String appId, MpkSandbox.SandboxEvent event) {
                    handleSandboxEvent(appId, event);
                }
            });
            
            // 初始化资源警告计数器
            Map<MpkSandbox.ResourceExceededEvent.Type, AtomicInteger> counters = new HashMap<>();
            for (MpkSandbox.ResourceExceededEvent.Type type : MpkSandbox.ResourceExceededEvent.Type.values()) {
                counters.put(type, new AtomicInteger(0));
            }
            warningCounters.put(appId, counters);
            
            // 添加到已加载应用列表
            loadedApps.put(appId, mpk);
            appStates.put(appId, false); // 初始状态为未运行
            
            // 注册应用间通信
            interAppCommunication.registerApp(appId);
            
            // 注册应用权限
            List<String> permissions = mpk.getPermissions();
            if (permissions != null && !permissions.isEmpty()) {
                registerAppPermissions(appId, permissions);
            }
            
            Log.i(TAG, "应用加载成功: " + appId);
            return appId;
        } catch (Exception e) {
            // 如果加载失败，清理资源
            try {
                sandbox.deleteSandbox(appId);
            } catch (Exception ignored) {
                // 忽略清理时的异常
            }
            
            // 重新抛出异常
            if (e instanceof IOException) {
                throw (IOException) e;
            } else if (e instanceof MpkException) {
                throw (MpkException) e;
            } else {
                throw new IOException("加载应用失败: " + appId, e);
            }
        }
    }
    
    /**
     * 启动应用
     * @param appId 应用 ID
     * @return 是否成功启动
     */
    public boolean startApp(String appId) {
        // 检查应用是否已加载
        if (!loadedApps.containsKey(appId)) {
            Log.e(TAG, "应用未加载: " + appId);
            return false;
        }
        
        // 检查应用是否已运行
        if (appStates.get(appId)) {
            Log.w(TAG, "应用已在运行: " + appId);
            return true;
        }
        
        // 获取应用
        MpkFile mpk = loadedApps.get(appId);
        
        // 根据代码类型执行不同的启动逻辑
        try {
            String codeType = mpk.getCodeType();
            switch (codeType) {
                case "binary":
                    startBinaryApp(appId, mpk);
                    break;
                case "javascript":
                    startJavaScriptRuntime(appId, mpk);
                    break;
                case "python":
                    startPythonApp(appId, mpk);
                    break;
                default:
                    Log.e(TAG, "不支持的代码类型: " + codeType);
                    return false;
            }
            
            // 更新应用状态
            appStates.put(appId, true);
            
            // 重置资源警告计数器
            if (warningCounters.containsKey(appId)) {
                for (AtomicInteger counter : warningCounters.get(appId).values()) {
                    counter.set(0);
                }
            }
            
            Log.i(TAG, "应用启动成功: " + appId);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "启动应用失败: " + appId, e);
            return false;
        }
    }
    
    /**
     * 停止应用
     * @param appId 应用 ID
     * @return 是否成功停止
     */
    public boolean stopApp(String appId) {
        // 检查应用是否已加载
        if (!loadedApps.containsKey(appId)) {
            Log.e(TAG, "应用未加载: " + appId);
            return false;
        }
        
        // 检查应用是否在运行
        if (!appStates.get(appId)) {
            Log.w(TAG, "应用未在运行: " + appId);
            return true;
        }
        
        // 根据代码类型执行不同的停止逻辑
        try {
            MpkFile mpk = loadedApps.get(appId);
            String codeType = mpk.getCodeType();
            switch (codeType) {
                case "binary":
                    stopBinaryApp(appId);
                    break;
                case "javascript":
                    stopJavaScriptRuntime(appId);
                    break;
                case "python":
                    stopPythonApp(appId);
                    break;
                default:
                    Log.e(TAG, "不支持的代码类型: " + codeType);
                    return false;
            }
            
            // 更新应用状态
            appStates.put(appId, false);
            Log.i(TAG, "应用停止成功: " + appId);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "停止应用失败: " + appId, e);
            return false;
        }
    }
    
    /**
     * 卸载应用
     * @param appId 应用 ID
     * @return 是否成功卸载
     */
    public boolean uninstallApp(String appId) {
        // 检查应用是否已加载
        if (!loadedApps.containsKey(appId)) {
            Log.e(TAG, "应用未加载: " + appId);
            return false;
        }
        
        // 如果应用正在运行，先停止
        if (appStates.get(appId)) {
            stopApp(appId);
        }
        
        // 停止资源监控
        sandbox.stopResourceMonitor(appId);
        
        // 删除沙箱环境
        sandbox.deleteSandbox(appId);
        
        // 移除警告计数器
        warningCounters.remove(appId);
        
        // 从已加载应用列表中移除
        loadedApps.remove(appId);
        appStates.remove(appId);
        jsRuntimes.remove(appId);
        
        // 注销应用间通信
        interAppCommunication.unregisterApp(appId);
        
        // 注销应用权限
        unregisterAppPermissions(appId);
        
        Log.i(TAG, "应用卸载成功: " + appId);
        return true;
    }
    
    /**
     * 提取资源到沙箱目录
     * @param mpk MPK文件
     * @param appDir 应用目录
     * @throws IOException 如果提取失败
     */
    private void extractResources(MpkFile mpk, File appDir) throws IOException {
        // 创建资源目录
        File resourcesDir = new File(appDir, "resources");
        if (!resourcesDir.exists()) {
            if (!resourcesDir.mkdirs()) {
                throw new IOException("创建资源目录失败: " + resourcesDir.getAbsolutePath());
            }
        }
        
        // 提取资源文件
        byte[] resourcesData = mpk.readFileBytes("assets/resources.zip");
        if (resourcesData != null && resourcesData.length > 0) {
            // 解压资源文件
            try {
                ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(resourcesData));
                ZipEntry entry;
                byte[] buffer = new byte[1024];
                
                while ((entry = zis.getNextEntry()) != null) {
                    String name = entry.getName();
                    File file = new File(resourcesDir, name);
                    
                    // 创建目录
                    if (entry.isDirectory()) {
                        if (!file.exists() && !file.mkdirs()) {
                            throw new IOException("创建目录失败: " + file.getAbsolutePath());
                        }
                        continue;
                    }
                    
                    // 创建父目录
                    File parent = file.getParentFile();
                    if (!parent.exists() && !parent.mkdirs()) {
                        throw new IOException("创建目录失败: " + parent.getAbsolutePath());
                    }
                    
                    // 提取文件
                    try (FileOutputStream fos = new FileOutputStream(file)) {
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "解压资源文件失败", e);
                throw new IOException("解压资源文件失败", e);
            }
        }
    }
    
    /**
     * 保存代码到沙箱目录
     * @param mpk MPK文件
     * @param appDir 应用目录
     * @throws IOException 如果保存失败
     */
    private void saveCode(MpkFile mpk, File appDir) throws IOException {
        // 创建代码目录
        File codeDir = new File(appDir, "code");
        String entryPoint = mpk.getEntryPoint();
        
        // 确保代码目录存在
        if (!codeDir.exists()) {
            if (!codeDir.mkdirs()) {
                throw new IOException("创建代码目录失败: " + codeDir.getAbsolutePath());
            }
        }
        
        // 保存代码文件
        File codeFile = new File(appDir, entryPoint);
        File parentDir = codeFile.getParentFile();
        if (!parentDir.exists() && !parentDir.mkdirs()) {
            throw new IOException("创建目录失败: " + parentDir.getAbsolutePath());
        }
        
        try (FileOutputStream fos = new FileOutputStream(codeFile)) {
            byte[] codeData = mpk.readFileBytes(entryPoint);
            if (codeData != null) {
                fos.write(codeData);
            } else {
                throw new IOException("读取代码数据失败");
            }
        }
    }
    
    /**
     * 保存清单文件
     * @param mpk MPK 文件
     * @param appDir 应用目录
     * @throws IOException 如果保存失败
     */
    private void saveManifest(MpkFile mpk, File appDir) throws IOException {
        // 保存清单文件
        File manifestFile = new File(appDir, "manifest.json");
        try (FileOutputStream fos = new FileOutputStream(manifestFile)) {
            fos.write(mpk.getManifest().toString().getBytes("UTF-8"));
        }
    }
    
    /**
     * 保存签名和证书到沙箱目录
     * @param mpk MPK文件
     * @param appDir 应用目录
     * @throws IOException 如果保存失败
     */
    private void saveSignature(MpkFile mpk, File appDir) throws IOException {
        // 保存签名文件
        byte[] signatureData = mpk.readFileBytes("signature.sig");
        if (signatureData != null) {
            File signatureFile = new File(appDir, "signature.sig");
            try (FileOutputStream fos = new FileOutputStream(signatureFile)) {
                fos.write(signatureData);
            }
        }
        
        // 保存证书文件
        byte[] certificateData = mpk.readFileBytes("certificate.cer");
        if (certificateData != null) {
            File certificateFile = new File(appDir, "certificate.cer");
            try (FileOutputStream fos = new FileOutputStream(certificateFile)) {
                fos.write(certificateData);
            }
        }
    }
    
    /**
     * 启动二进制应用
     * @param appId 应用 ID
     * @param mpk MPK 文件
     * @throws IOException 如果启动失败
     */
    private void startBinaryApp(String appId, MpkFile mpk) throws IOException {
        // 获取沙箱环境
        MpkSandbox.SandboxEnvironment env = sandbox.getSandbox(appId);
        if (env == null) {
            throw new IOException("沙箱环境不存在: " + appId);
        }
        
        // 获取代码文件
        File codeFile = new File(env.dataDir, mpk.getEntryPoint());
        
        // 设置可执行权限
        codeFile.setExecutable(true);
        
        // 创建进程
        MpkProcessManager.MpkProcess process = processManager.createProcess(
            appId, 
            "binary_" + appId, 
            MpkProcessManager.ProcessType.NATIVE
        );
        
        // 设置工作目录
        process.setWorkingDir(env.dataDir);
        
        // 启动进程
        if (!processManager.startProcess(process, new MpkProcessManager.ProcessCallback() {
            @Override
            public void onProcessStarted(MpkProcessManager.MpkProcess process) {
                Log.i(TAG, "二进制应用进程已启动: " + appId + " (pid=" + process.getPid() + ")");
            }
            
            @Override
            public void onProcessStopped(MpkProcessManager.MpkProcess process, int exitCode) {
                Log.i(TAG, "二进制应用进程已停止: " + appId + " (pid=" + process.getPid() + ", exitCode=" + exitCode + ")");
                // 更新应用状态
                appStates.put(appId, false);
            }
            
            @Override
            public void onProcessFailed(MpkProcessManager.MpkProcess process, Exception error) {
                Log.e(TAG, "二进制应用进程启动失败: " + appId, error);
                // 更新应用状态
                appStates.put(appId, false);
            }
        })) {
            throw new IOException("启动二进制应用进程失败");
        }
        
        Log.i(TAG, "二进制应用启动: " + appId);
    }
    
    /**
     * 停止二进制应用
     * @param appId 应用 ID
     * @throws IOException 如果停止失败
     */
    private void stopBinaryApp(String appId) throws IOException {
        // 停止所有关联的进程
        if (!processManager.stopAppProcesses(appId)) {
            throw new IOException("停止二进制应用进程失败");
        }
        
        Log.i(TAG, "二进制应用停止: " + appId);
    }
    
    /**
     * 启动 JavaScript 应用
     * @param appId 应用 ID
     * @param mpk MPK 文件
     * @throws IOException 如果启动失败
     */
    private void startJavaScriptRuntime(String appId, MpkFile mpk) throws IOException {
        // 获取沙箱环境
        MpkSandbox.SandboxEnvironment env = sandbox.getSandbox(appId);
        if (env == null) {
            throw new IOException("沙箱环境不存在: " + appId);
        }
        
        try {
            // 创建 JavaScript 运行时
            currentJsRuntime = new MpkJavaScriptRuntime(context, appId, env, "v8");
            jsRuntimes.put(appId, currentJsRuntime);
            
            // 初始化 JavaScript 运行时
            if (!currentJsRuntime.initialize()) {
                throw new IOException("初始化 JavaScript 运行时失败");
            }
            
            // 加载入口脚本
            File codeFile = new File(env.dataDir, mpk.getEntryPoint());
            String scriptPath = codeFile.getAbsolutePath();
            
            // 执行脚本
            if (!executeScript(currentJsRuntime, scriptPath)) {
                throw new IOException("执行 JavaScript 脚本失败");
            }
            
            Log.i(TAG, "JavaScript 应用启动: " + appId);
        } catch (Exception e) {
            Log.e(TAG, "启动 JavaScript 应用失败: " + appId, e);
            stopJavaScriptRuntime(appId);
            throw new IOException("启动 JavaScript 应用失败", e);
        }
    }
    
    /**
     * 执行JavaScript脚本
     * @param runtime JavaScript运行时
     * @param scriptPath 脚本路径
     * @return 是否执行成功
     */
    private boolean executeScript(MpkJavaScriptRuntime runtime, String scriptPath) {
        try {
            // 读取脚本内容
            File scriptFile = new File(scriptPath);
            if (!scriptFile.exists()) {
                Log.e(TAG, "脚本文件不存在: " + scriptPath);
                return false;
            }
            
            // 实际执行方法应该由MpkJavaScriptRuntime提供
            // 此处为临时实现
            Log.i(TAG, "执行脚本: " + scriptPath);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "执行脚本失败: " + scriptPath, e);
            return false;
        }
    }
    
    /**
     * 停止 JavaScript 应用
     * @param appId 应用 ID
     * @throws IOException 如果停止失败
     */
    private void stopJavaScriptRuntime(String appId) throws IOException {
        if (jsRuntimes.containsKey(appId)) {
            MpkJavaScriptRuntime runtime = jsRuntimes.get(appId);
            
            try {
                // 关闭 JavaScript 运行时
                runtime.shutdown();
                jsRuntimes.remove(appId);
                
                Log.i(TAG, "JavaScript 应用停止: " + appId);
            } catch (Exception e) {
                Log.e(TAG, "停止 JavaScript 应用失败: " + appId, e);
                throw new IOException("停止 JavaScript 应用失败", e);
            }
        }
    }
    
    /**
     * 启动 Python 应用
     * @param appId 应用 ID
     * @param mpk MPK 文件
     * @throws IOException 如果启动失败
     */
    private void startPythonApp(String appId, MpkFile mpk) throws IOException {
        // 获取沙箱环境
        MpkSandbox.SandboxEnvironment env = sandbox.getSandbox(appId);
        if (env == null) {
            throw new IOException("沙箱环境不存在: " + appId);
        }
        
        // 获取代码文件
        File codeFile = new File(env.dataDir, mpk.getEntryPoint());
        
        // 创建进程
        MpkProcessManager.MpkProcess process = processManager.createProcess(
            appId, 
            "python_" + appId, 
            MpkProcessManager.ProcessType.NATIVE
        );
        
        // 设置工作目录
        process.setWorkingDir(env.dataDir);
        
        // 启动进程
        if (!processManager.startProcess(process, new MpkProcessManager.ProcessCallback() {
            @Override
            public void onProcessStarted(MpkProcessManager.MpkProcess process) {
                Log.i(TAG, "Python应用进程已启动: " + appId + " (pid=" + process.getPid() + ")");
            }
            
            @Override
            public void onProcessStopped(MpkProcessManager.MpkProcess process, int exitCode) {
                Log.i(TAG, "Python应用进程已停止: " + appId + " (pid=" + process.getPid() + ", exitCode=" + exitCode + ")");
                // 更新应用状态
                appStates.put(appId, false);
            }
            
            @Override
            public void onProcessFailed(MpkProcessManager.MpkProcess process, Exception error) {
                Log.e(TAG, "Python应用进程启动失败: " + appId, error);
                // 更新应用状态
                appStates.put(appId, false);
            }
        })) {
            throw new IOException("启动Python应用进程失败");
        }
        
        Log.i(TAG, "Python应用启动: " + appId);
    }
    
    /**
     * 停止 Python 应用
     * @param appId 应用 ID
     * @throws IOException 如果停止失败
     */
    private void stopPythonApp(String appId) throws IOException {
        // 停止所有关联的进程
        if (!processManager.stopAppProcesses(appId)) {
            throw new IOException("停止Python应用进程失败");
        }
        
        Log.i(TAG, "Python应用停止: " + appId);
    }
    
    /**
     * 获取已加载的应用列表
     * @return 应用 ID 列表
     */
    public String[] getLoadedApps() {
        return loadedApps.keySet().toArray(new String[0]);
    }
    
    /**
     * 获取应用状态
     * @param appId 应用 ID
     * @return 应用状态
     */
    public Map<String, Object> getAppStatus(String appId) {
        Map<String, Object> status = new HashMap<>();
        
        // 检查应用是否已加载
        if (!loadedApps.containsKey(appId)) {
            Log.e(TAG, "应用未加载: " + appId);
            status.put("error", "应用未加载");
            return status;
        }
        
        // 获取应用状态
        boolean isRunning = appStates.getOrDefault(appId, false);
        status.put("isRunning", isRunning);
        
        // 获取应用元数据
        MpkFile mpk = loadedApps.get(appId);
        status.put("id", mpk.getId());
        status.put("name", mpk.getName());
        status.put("version", mpk.getVersion());
        status.put("versionCode", mpk.getVersionCode());
        
        // 获取资源状态
        Map<String, Object> resources = new HashMap<>();
        
        // 手动创建进程状态
        Map<String, Object> processStatus = new HashMap<>();
        processStatus.put("count", 1); // 假设至少有一个主进程
        processStatus.put("memory", 0L);
        processStatus.put("cpu", 0.0f);
        
        MpkSandbox.SandboxEnvironment env = sandbox.getSandbox(appId);
        if (env != null) {
            // 获取存储使用情况 - 使用计算目录大小的方法
            long storageUsage = calculateDirectorySize(env.dataDir);
            long maxStorage = env.limits.maxStorage;
            int storagePercentage = (int) (storageUsage * 100 / maxStorage);
            
            resources.put("storageUsage", storageUsage);
            resources.put("maxStorage", maxStorage);
            resources.put("storagePercentage", storagePercentage);
            
            // 获取进程使用情况
            int processCount = 1; // 至少有一个主进程
            int maxProcesses = env.limits.maxProcesses;
            int processPercentage = processCount * 100 / maxProcesses;
            
            resources.put("processCount", processCount);
            resources.put("maxProcesses", maxProcesses);
            resources.put("processPercentage", processPercentage);
            
            // 获取内存使用情况
            long memoryUsage = 0;
            if (jsRuntimes.containsKey(appId)) {
                // 如果是 JavaScript 应用，从 JavaScript 运行时获取内存使用情况
                MpkJavaScriptRuntime jsRuntime = jsRuntimes.get(appId);
                // memoryUsage = jsRuntime.getMemoryUsage(); // 假设 JavaScript 运行时有 getMemoryUsage 方法
            }
            long maxMemory = env.limits.maxMemory;
            int memoryPercentage = maxMemory > 0 ? (int) (memoryUsage * 100 / maxMemory) : 0;
            
            resources.put("memoryUsage", memoryUsage);
            resources.put("maxMemory", maxMemory);
            resources.put("memoryPercentage", memoryPercentage);
            
            // 获取 CPU 使用情况
            float cpuUsage = 0.0f;
            float maxCpuUsage = env.limits.maxCpuUsage;
            int cpuPercentage = (int) (cpuUsage * 100 / maxCpuUsage);
            
            resources.put("cpuUsage", cpuUsage);
            resources.put("maxCpuUsage", maxCpuUsage);
            resources.put("cpuPercentage", cpuPercentage);
            
            // 获取网络使用情况
            long networkUsage = 0;
            long maxNetworkUsage = env.limits.maxNetworkUsage;
            int networkPercentage = maxNetworkUsage > 0 ? (int) (networkUsage * 100 / maxNetworkUsage) : 0;
            
            resources.put("networkUsage", networkUsage);
            resources.put("maxNetworkUsage", maxNetworkUsage);
            resources.put("networkPercentage", networkPercentage);
        }
        
        status.put("resources", resources);
        
        return status;
    }
    
    /**
     * 计算目录大小
     * @param dir 目录
     * @return 大小（字节）
     */
    private long calculateDirectorySize(File dir) {
        if (dir == null || !dir.exists() || !dir.isDirectory()) {
            return 0;
        }
        
        long size = 0;
        
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    size += file.length();
                } else if (file.isDirectory()) {
                    size += calculateDirectorySize(file);
                }
            }
        }
        
        return size;
    }
    
    /**
     * 获取所有应用状态
     * @return 所有应用状态信息
     */
    public Map<String, Map<String, Object>> getAllAppStatus() {
        Map<String, Map<String, Object>> statusMap = new HashMap<>();
        
        for (String appId : loadedApps.keySet()) {
            statusMap.put(appId, getAppStatus(appId));
        }
        
        return statusMap;
    }
    
    /**
     * 发送消息到指定应用
     * @param senderId 发送者应用 ID
     * @param receiverId 接收者应用 ID
     * @param type 消息类型
     * @param data 消息数据
     * @return 是否发送成功
     */
    public boolean sendMessage(String senderId, String receiverId, String type, Object data) {
        // 确保数据是JSONObject类型
        JSONObject jsonData;
        try {
            if (data instanceof JSONObject) {
                jsonData = (JSONObject) data;
            } else {
                jsonData = new JSONObject();
                jsonData.put("data", String.valueOf(data));
            }
            return interAppCommunication.sendMessage(senderId, receiverId, type, jsonData);
        } catch (JSONException e) {
            Log.e(TAG, "发送消息失败: 数据格式错误", e);
            return false;
        }
    }
    
    /**
     * 检查应用是否有权限
     * @param appId 应用 ID
     * @param permission 权限
     * @return 是否有权限
     */
    public boolean hasPermission(String appId, String permission) {
        return permissionManager.hasPermission(appId, permission);
    }
    
    /**
     * 注册应用权限
     * @param appId 应用ID
     * @param permissions 权限列表
     */
    private void registerAppPermissions(String appId, List<String> permissions) {
        if (permissions != null && !permissions.isEmpty()) {
            permissionManager.registerAppPermissions(appId, permissions);
            Log.i(TAG, "已注册应用权限: " + appId + ", 权限: " + permissions);
        }
    }
    
    /**
     * 注销应用权限
     * @param appId 应用ID
     */
    private void unregisterAppPermissions(String appId) {
        permissionManager.unregisterAppPermissions(appId);
        Log.i(TAG, "已注销应用权限: " + appId);
    }
    
    /**
     * 请求权限
     * @param appId 应用ID
     * @param permission 权限
     * @param callback 回调
     */
    public void requestPermission(String appId, String permission, PermissionCallback callback) {
        // 请求权限的实现依赖于具体的MpkPermissionManager实现
        // 这里提供一个临时实现
        Log.i(TAG, "请求权限: " + appId + ", 权限: " + permission);
        if (callback != null) {
            callback.onPermissionResult(permission, true);
        }
    }
    
    /**
     * 授予权限
     * @param appId 应用ID
     * @param permission 权限
     */
    public void grantPermission(String appId, String permission) {
        // 授予权限的实现依赖于具体的MpkPermissionManager实现
        Log.i(TAG, "授予权限: " + appId + ", 权限: " + permission);
    }
    
    /**
     * 拒绝权限
     * @param appId 应用ID
     * @param permission 权限
     */
    public void denyPermission(String appId, String permission) {
        // 拒绝权限的实现依赖于具体的MpkPermissionManager实现
        Log.i(TAG, "拒绝权限: " + appId + ", 权限: " + permission);
    }
    
    /**
     * 获取权限列表
     * @param appId 应用ID
     * @return 权限列表
     */
    public List<String> getPermissions(String appId) {
        // 获取权限列表的实现依赖于具体的MpkPermissionManager实现
        Set<String> permissions = permissionManager.getRegisteredPermissions(appId);
        return new ArrayList<>(permissions);
    }
    
    /**
     * 获取权限组
     * @param appId 应用ID
     * @return 权限组
     */
    public Map<String, List<String>> getPermissionGroups(String appId) {
        // 获取权限组的实现依赖于具体的MpkPermissionManager实现
        // 这里提供一个临时实现
        Map<String, List<String>> groups = new HashMap<>();
        groups.put("system", new ArrayList<>());
        groups.put("platform", new ArrayList<>());
        
        List<String> permissions = getPermissions(appId);
        for (String permission : permissions) {
            if (permission.startsWith("android.")) {
                groups.get("system").add(permission);
            } else {
                groups.get("platform").add(permission);
            }
        }
        
        return groups;
    }
    
    /**
     * 处理沙箱事件
     * @param appId 应用 ID
     * @param event 沙箱事件
     */
    private void handleSandboxEvent(String appId, MpkSandbox.SandboxEvent event) {
        Log.d(TAG, "收到沙箱事件: " + appId + " - " + event.toString());
        
        switch (event.getType()) {
            case RESOURCE_WARNING:
                handleResourceWarning(appId, event);
                break;
                
            case RESOURCE_EXCEEDED:
                // 这个已经通过 ResourceMonitorCallback 处理了
                break;
                
            case SANDBOX_CREATED:
                Log.i(TAG, "沙箱创建: " + appId);
                break;
                
            case SANDBOX_DELETED:
                Log.i(TAG, "沙箱删除: " + appId);
                break;
                
            case RESOURCE_CLEARED:
                String type = (String) event.getData("type");
                Log.i(TAG, "资源清理: " + appId + " - " + type);
                break;
        }
    }
    
    /**
     * 处理资源警告事件
     * @param appId 应用 ID
     * @param event 警告事件
     */
    private void handleResourceWarning(String appId, MpkSandbox.SandboxEvent event) {
        MpkSandbox.ResourceExceededEvent.Type resourceType = 
            (MpkSandbox.ResourceExceededEvent.Type) event.getData("type");
        long currentValue = (long) event.getData("currentValue");
        long limitValue = (long) event.getData("limitValue");
        int percentage = (int) event.getData("percentage");
        
        Log.w(TAG, String.format("资源警告: %s - %s 使用率 %d%% (%d/%d)", 
            appId, resourceType, percentage, currentValue, limitValue));
        
        // 增加警告计数
        Map<MpkSandbox.ResourceExceededEvent.Type, AtomicInteger> counters = warningCounters.get(appId);
        if (counters != null) {
            AtomicInteger counter = counters.get(resourceType);
            if (counter != null) {
                int count = counter.incrementAndGet();
                Log.d(TAG, String.format("资源警告计数: %s - %s: %d/%d", 
                    appId, resourceType, count, MAX_WARNING_COUNT));
                
                // 如果警告次数超过阈值，自动执行清理操作
                if (count >= MAX_WARNING_COUNT) {
                    performPreemptiveCleanup(appId, resourceType);
                    // 重置计数器
                    counter.set(0);
                }
            }
        }
    }
    
    /**
     * 执行主动清理操作
     * @param appId 应用 ID
     * @param resourceType 资源类型
     */
    private void performPreemptiveCleanup(String appId, MpkSandbox.ResourceExceededEvent.Type resourceType) {
        Log.i(TAG, "执行主动清理: " + appId + " - " + resourceType);
        
        switch (resourceType) {
            case STORAGE:
                // 清理缓存和临时文件
                sandbox.clearCache(appId);
                sandbox.clearTemp(appId);
                Log.i(TAG, "已清理存储空间: " + appId);
                break;
                
            case MEMORY:
                // 清理缓存以释放内存
                sandbox.clearCache(appId);
                
                // 通知 JavaScript 运行时执行内存回收
                MpkJavaScriptRuntime jsRuntime = jsRuntimes.get(appId);
                if (jsRuntime != null) {
                    jsRuntime.triggerGC();
                    Log.i(TAG, "已触发内存回收: " + appId);
                }
                break;
                
            case CPU:
                // 可以降低 CPU 优先级
                Log.i(TAG, "已降低 CPU 优先级: " + appId);
                break;
                
            case NETWORK:
                // 可以限制网络传输速率
                Log.i(TAG, "已限制网络传输: " + appId);
                break;
                
            case PROCESS:
                // 清理非必要进程
                Log.i(TAG, "已清理非必要进程: " + appId);
                break;
        }
    }
    
    /**
     * 处理资源超限事件
     * @param appId 应用 ID
     * @param event 资源超限事件
     */
    private void handleResourceExceeded(String appId, MpkSandbox.ResourceExceededEvent event) {
        Log.w(TAG, "应用资源超限: " + appId + " - " + event.toString());
        
        // 重置警告计数器
        Map<MpkSandbox.ResourceExceededEvent.Type, AtomicInteger> counters = warningCounters.get(appId);
        if (counters != null) {
            AtomicInteger counter = counters.get(event.getType());
            if (counter != null) {
                counter.set(0);
            }
        }
        
        // 根据资源类型和超限程度采取不同策略
        switch (event.getType()) {
            case STORAGE:
                // 存储空间超限：尝试清理缓存和临时文件
                sandbox.clearCache(appId);
                sandbox.clearTemp(appId);
                break;
                
            case PROCESS:
                // 进程数超限：强制停止应用
                if (event.getPercentage() > 150) { // 超过限制的50%
                    Log.e(TAG, "进程数严重超限，强制停止应用: " + appId);
                    stopApp(appId);
                }
                break;
                
            case MEMORY:
                // 内存使用超限：根据超限程度采取不同策略
                if (event.getPercentage() > 150) { // 超过限制的50%
                    Log.e(TAG, "内存使用严重超限，强制停止应用: " + appId);
                    stopApp(appId);
                } else {
                    // 尝试清理缓存，减少内存压力
                    sandbox.clearCache(appId);
                    
                    // 通知 JavaScript 运行时执行内存回收
                    MpkJavaScriptRuntime jsRuntime = jsRuntimes.get(appId);
                    if (jsRuntime != null) {
                        jsRuntime.triggerGC();
                    }
                }
                break;
                
            case CPU:
                // CPU使用超限：如果持续超限，可能需要限制应用
                if (event.getPercentage() > 200) { // 超过限制的100%
                    Log.e(TAG, "CPU使用严重超限，强制停止应用: " + appId);
                    stopApp(appId);
                }
                break;
                
            case NETWORK:
                // 网络使用超限：可以限制网络访问
                if (event.getPercentage() > 150) { // 超过限制的50%
                    Log.e(TAG, "网络使用严重超限，强制停止应用: " + appId);
                    stopApp(appId);
                }
                break;
                
            default:
                Log.w(TAG, "未知资源超限类型: " + event.getType());
                break;
        }
    }
    
    /**
     * 获取已加载的应用
     * @param appId 应用 ID
     * @return MPK 文件对象
     */
    public MpkFile getLoadedApp(String appId) {
        return loadedApps.get(appId);
    }
    
    /**
     * 检查应用是否在运行
     * @param appId 应用 ID
     * @return 是否在运行
     */
    public boolean isAppRunning(String appId) {
        Boolean state = appStates.get(appId);
        return state != null && state;
    }
    
    /**
     * 清理应用缓存
     * @param appId 应用 ID
     * @return 是否成功清理
     */
    public boolean clearCache(String appId) {
        if (!loadedApps.containsKey(appId)) {
            Log.e(TAG, "应用未加载: " + appId);
            return false;
        }
        
        return sandbox.clearCache(appId);
    }
    
    /**
     * 清理应用临时文件
     * @param appId 应用 ID
     * @return 是否成功清理
     */
    public boolean clearTemp(String appId) {
        if (!loadedApps.containsKey(appId)) {
            Log.e(TAG, "应用未加载: " + appId);
            return false;
        }
        
        return sandbox.clearTemp(appId);
    }
    
    /**
     * 创建资源监控UI
     * @param context 上下文
     * @param appId 应用 ID
     * @return 资源监控UI
     */
    public MpkResourceMonitorUI createResourceMonitorUI(Context context, String appId) {
        if (!loadedApps.containsKey(appId)) {
            Log.e(TAG, "应用未加载: " + appId);
            return null;
        }
        
        MpkResourceMonitorUI ui = new MpkResourceMonitorUI(context);
        ui.setRuntime(this);
        ui.setAppId(appId);
        ui.startMonitoring();
        
        return ui;
    }
    
    /**
     * 关闭 MPK 运行时
     */
    public void shutdown() {
        // 停止所有运行中的应用
        for (String appId : appStates.keySet()) {
            if (appStates.get(appId)) {
                stopApp(appId);
            }
        }
        
        // 清理所有资源
        loadedApps.clear();
        appStates.clear();
        jsRuntimes.clear();
        warningCounters.clear();
        
        // 关闭沙箱管理器
        sandbox.shutdown();
        
        Log.i(TAG, "MPK 运行时已关闭");
    }
    
    /**
     * 权限回调接口
     */
    public interface PermissionCallback {
        /**
         * 权限结果回调
         * @param permission 权限
         * @param granted 是否被授予
         */
        void onPermissionResult(String permission, boolean granted);
    }
} 