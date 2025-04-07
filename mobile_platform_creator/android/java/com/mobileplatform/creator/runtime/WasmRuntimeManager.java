package com.mobileplatform.creator.runtime;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebAssembly运行时管理器
 * 
 * 提供Java层对WebAssembly模块的管理，支持加载和执行WASM程序
 */
public class WasmRuntimeManager {
    private static final String TAG = "WasmRuntimeManager";
    
    // 单例实例
    private static WasmRuntimeManager sInstance;
    
    // 上下文
    private final Context mContext;
    
    // 默认内存限制 (100MB)
    private static final long DEFAULT_MEMORY_LIMIT = 100 * 1024 * 1024;
    
    // 已加载的模块集合
    private final Map<String, WasmModule> mModules = new ConcurrentHashMap<>();
    
    // 是否已初始化
    private boolean mInitialized = false;
    
    /**
     * 获取WasmRuntimeManager单例
     * 
     * @param context 应用上下文
     * @return WasmRuntimeManager实例
     */
    public static synchronized WasmRuntimeManager getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new WasmRuntimeManager(context.getApplicationContext());
        }
        return sInstance;
    }
    
    /**
     * 私有构造函数
     * 
     * @param context 应用上下文
     */
    private WasmRuntimeManager(Context context) {
        mContext = context;
        
        // 加载本地库
        try {
            System.loadLibrary("wasm_runtime");
            Log.i(TAG, "WASM运行时库加载成功");
        } catch (UnsatisfiedLinkError e) {
            Log.e(TAG, "加载WASM运行时库失败", e);
        }
    }
    
    /**
     * 初始化WASM运行时
     * 
     * @return 是否初始化成功
     */
    public boolean initialize() {
        return initialize(DEFAULT_MEMORY_LIMIT, null);
    }
    
    /**
     * 初始化WASM运行时
     * 
     * @param memoryLimit 内存限制（字节）
     * @param tempDir 临时目录路径，可为null（使用默认目录）
     * @return 是否初始化成功
     */
    public boolean initialize(long memoryLimit, String tempDir) {
        if (mInitialized) {
            Log.w(TAG, "WASM运行时已初始化");
            return true;
        }
        
        // 调用本地方法初始化运行时
        int result = nativeInitializeRuntime(memoryLimit, tempDir);
        if (result == 0) {
            mInitialized = true;
            Log.i(TAG, "WASM运行时初始化成功");
            return true;
        } else {
            Log.e(TAG, "WASM运行时初始化失败，错误码: " + result);
            return false;
        }
    }
    
    /**
     * 加载WASM模块
     * 
     * @param modulePath 模块文件路径
     * @return 模块对象，加载失败则返回null
     */
    public WasmModule loadModule(String modulePath) {
        if (!mInitialized) {
            Log.e(TAG, "WASM运行时未初始化");
            return null;
        }
        
        // 检查文件是否存在
        File moduleFile = new File(modulePath);
        if (!moduleFile.exists() || !moduleFile.isFile()) {
            Log.e(TAG, "模块文件不存在: " + modulePath);
            return null;
        }
        
        // 调用本地方法加载模块
        String moduleId = nativeLoadModule(modulePath);
        if (moduleId == null) {
            Log.e(TAG, "加载模块失败: " + modulePath);
            return null;
        }
        
        // 创建模块对象
        WasmModule module = new WasmModule(moduleId, modulePath);
        
        // 保存到模块集合
        mModules.put(moduleId, module);
        
        Log.i(TAG, "模块加载成功: " + modulePath + ", ID: " + moduleId);
        return module;
    }
    
    /**
     * 卸载WASM模块
     * 
     * @param moduleId 模块ID
     * @return 是否卸载成功
     */
    public boolean unloadModule(String moduleId) {
        if (!mInitialized) {
            Log.e(TAG, "WASM运行时未初始化");
            return false;
        }
        
        // 调用本地方法卸载模块
        int result = nativeUnloadModule(moduleId);
        
        // 从模块集合中移除
        if (result == 0) {
            mModules.remove(moduleId);
            Log.i(TAG, "模块卸载成功: " + moduleId);
            return true;
        } else {
            Log.e(TAG, "卸载模块失败: " + moduleId);
            return false;
        }
    }
    
    /**
     * 获取加载的模块
     * 
     * @param moduleId 模块ID
     * @return 模块对象，不存在则返回null
     */
    public WasmModule getModule(String moduleId) {
        return mModules.get(moduleId);
    }
    
    /**
     * 获取所有加载的模块
     * 
     * @return 模块集合
     */
    public Map<String, WasmModule> getAllModules() {
        return new HashMap<>(mModules);
    }
    
    /**
     * 终止WASM运行时
     * 
     * @return 是否终止成功
     */
    public boolean terminate() {
        if (!mInitialized) {
            Log.w(TAG, "WASM运行时未初始化");
            return true;
        }
        
        // 调用本地方法终止运行时
        int result = nativeTerminateRuntime();
        if (result == 0) {
            // 清空模块集合
            mModules.clear();
            mInitialized = false;
            Log.i(TAG, "WASM运行时终止成功");
            return true;
        } else {
            Log.e(TAG, "终止WASM运行时失败，错误码: " + result);
            return false;
        }
    }
    
    /**
     * WebAssembly模块类
     */
    public class WasmModule {
        private final String mId;
        private final String mPath;
        private boolean mInstantiated;
        
        /**
         * 构造函数
         * 
         * @param id 模块ID
         * @param path 模块文件路径
         */
        WasmModule(String id, String path) {
            mId = id;
            mPath = path;
            mInstantiated = false;
        }
        
        /**
         * 获取模块ID
         * 
         * @return 模块ID
         */
        public String getId() {
            return mId;
        }
        
        /**
         * 获取模块文件路径
         * 
         * @return 模块文件路径
         */
        public String getPath() {
            return mPath;
        }
        
        /**
         * 检查模块是否已实例化
         * 
         * @return 是否已实例化
         */
        public boolean isInstantiated() {
            return mInstantiated;
        }
        
        /**
         * 实例化模块
         * 
         * @return 是否实例化成功
         */
        public boolean instantiate() {
            if (mInstantiated) {
                Log.w(TAG, "模块已实例化: " + mId);
                return true;
            }
            
            // 调用本地方法实例化模块
            int result = nativeInstantiateModule(mId);
            if (result == 0) {
                mInstantiated = true;
                Log.i(TAG, "模块实例化成功: " + mId);
                return true;
            } else {
                Log.e(TAG, "实例化模块失败: " + mId);
                return false;
            }
        }
        
        /**
         * 调用模块中的函数
         * 
         * @param functionName 函数名称
         * @param args 参数列表
         * @return 函数返回值，调用失败则返回null
         */
        public String callFunction(String functionName, String... args) {
            if (!mInstantiated) {
                Log.e(TAG, "模块未实例化: " + mId);
                return null;
            }
            
            // 调用本地方法
            return nativeCallFunction(mId, functionName, args);
        }
    }
    
    // 本地方法
    private native int nativeInitializeRuntime(long memoryLimit, String tempDir);
    private native String nativeLoadModule(String modulePath);
    private native int nativeInstantiateModule(String moduleId);
    private native String nativeCallFunction(String moduleId, String functionName, String[] args);
    private native int nativeUnloadModule(String moduleId);
    private native int nativeTerminateRuntime();
} 