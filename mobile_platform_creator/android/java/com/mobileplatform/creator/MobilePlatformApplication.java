package com.mobileplatform.creator;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.mobileplatform.creator.runtime.WasmRuntimeManager;
import com.mobileplatform.creator.update.UpdateCheckService;

/**
 * 移动平台应用的Application类，负责应用全局初始化
 */
public class MobilePlatformApplication extends Application {
    private static final String TAG = "MobilePlatformApp";
    
    private static MobilePlatformApplication instance;
    
    /**
     * 获取应用实例
     */
    public static MobilePlatformApplication getInstance() {
        return instance;
    }
    
    /**
     * 获取应用上下文
     */
    public static Context getAppContext() {
        return instance.getApplicationContext();
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        
        Log.d(TAG, "应用启动");
        
        // 初始化各组件
        initializeComponents();
        
        // 启动更新检查服务
        startUpdateCheckService();
    }
    
    /**
     * 初始化应用组件
     */
    private void initializeComponents() {
        Log.d(TAG, "初始化应用组件");
        
        // 初始化WASM运行时
        try {
            WasmRuntimeManager.getInstance().init(this);
            Log.d(TAG, "WASM运行时初始化成功");
        } catch (Exception e) {
            Log.e(TAG, "WASM运行时初始化失败", e);
        }
    }
    
    /**
     * 启动更新检查服务
     */
    private void startUpdateCheckService() {
        Intent serviceIntent = new Intent(this, UpdateCheckService.class);
        startService(serviceIntent);
    }
    
    @Override
    public void onTerminate() {
        Log.d(TAG, "应用终止");
        super.onTerminate();
    }
    
    @Override
    public void onLowMemory() {
        Log.d(TAG, "内存不足");
        super.onLowMemory();
    }
    
    @Override
    public void onTrimMemory(int level) {
        Log.d(TAG, "内存整理，级别: " + level);
        super.onTrimMemory(level);
    }
} 