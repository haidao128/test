package com.mobileplatform.creator;

import android.app.Application;

/**
 * 应用程序的全局 Application 类
 * 用于初始化全局组件和配置
 */
public class MobilePlatformApplication extends Application {
    
    @Override
    public void onCreate() {
        super.onCreate();
        // 在这里进行全局初始化
        // 例如：初始化数据库、配置日志系统等
    }
} 