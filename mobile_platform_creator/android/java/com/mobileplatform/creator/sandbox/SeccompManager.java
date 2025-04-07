package com.mobileplatform.creator.sandbox;

import android.content.Context;
import android.util.Log;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Seccomp过滤器管理类
 * 
 * 提供Java层对Seccomp过滤器的管理和调用，实现Android平台上的沙箱隔离
 */
public class SeccompManager {
    private static final String TAG = "SeccompManager";
    
    // 安全级别常量
    public static final int SANDBOX_LEVEL_MINIMAL = 0;
    public static final int SANDBOX_LEVEL_STANDARD = 1;
    public static final int SANDBOX_LEVEL_STRICT = 2;
    
    // 系统调用操作类型
    private static final int SYSCALL_OP_ALLOW = 0;
    private static final int SYSCALL_OP_DENY = 1;
    private static final int SYSCALL_OP_KILL = 2;
    
    // 单例实例
    private static SeccompManager sInstance;
    
    // 上下文
    private final Context mContext;
    
    // 当前安全级别
    private int mCurrentLevel = SANDBOX_LEVEL_STRICT;
    
    // 是否已初始化
    private boolean mInitialized = false;
    
    // 系统调用名称到编号的映射
    private final Map<String, Integer> mSyscallMap = new HashMap<>();
    
    // 已加载的规则集
    private final Map<Integer, Set<String>> mAllowedSyscalls = new HashMap<>();
    private final Map<Integer, Set<String>> mDeniedSyscalls = new HashMap<>();
    
    /**
     * 获取SeccompManager单例
     * 
     * @param context 应用上下文
     * @return SeccompManager实例
     */
    public static synchronized SeccompManager getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new SeccompManager(context.getApplicationContext());
        }
        return sInstance;
    }
    
    /**
     * 私有构造函数
     * 
     * @param context 应用上下文
     */
    private SeccompManager(Context context) {
        mContext = context;
        
        // 加载本地库
        try {
            System.loadLibrary("seccomp_filter");
            Log.i(TAG, "Seccomp过滤器库加载成功");
        } catch (UnsatisfiedLinkError e) {
            Log.e(TAG, "加载Seccomp过滤器库失败", e);
        }
        
        // 初始化系统调用映射
        initSyscallMap();
        
        // 初始化安全级别规则
        initSecurityLevelRules();
    }
    
    /**
     * 初始化系统调用映射
     */
    private void initSyscallMap() {
        // 常用系统调用编号
        mSyscallMap.put("read", 0);
        mSyscallMap.put("write", 1);
        mSyscallMap.put("open", 2);
        mSyscallMap.put("close", 3);
        mSyscallMap.put("stat", 4);
        mSyscallMap.put("fstat", 5);
        mSyscallMap.put("lstat", 6);
        mSyscallMap.put("poll", 7);
        mSyscallMap.put("mmap", 9);
        mSyscallMap.put("mprotect", 10);
        mSyscallMap.put("brk", 12);
        mSyscallMap.put("sigaction", 13);
        mSyscallMap.put("ioctl", 16);
        mSyscallMap.put("access", 21);
        mSyscallMap.put("dup", 32);
        mSyscallMap.put("dup2", 33);
        mSyscallMap.put("socket", 41);
        mSyscallMap.put("connect", 42);
        mSyscallMap.put("accept", 43);
        mSyscallMap.put("sendto", 44);
        mSyscallMap.put("recvfrom", 45);
        mSyscallMap.put("execve", 59);
        mSyscallMap.put("readlink", 85);
        mSyscallMap.put("chmod", 90);
        mSyscallMap.put("chown", 92);
        mSyscallMap.put("getuid", 102);
        mSyscallMap.put("getgid", 104);
        
        // 注意: 实际的系统调用号在不同的CPU架构上可能不同，这里仅用于示例
    }
    
    /**
     * 初始化安全级别规则
     */
    private void initSecurityLevelRules() {
        // 最小限制规则
        Set<String> minimalAllowed = new HashSet<>();
        // 允许所有基本系统调用
        minimalAllowed.add("read");
        minimalAllowed.add("write");
        minimalAllowed.add("open");
        minimalAllowed.add("close");
        minimalAllowed.add("stat");
        minimalAllowed.add("fstat");
        minimalAllowed.add("lstat");
        minimalAllowed.add("poll");
        minimalAllowed.add("mmap");
        minimalAllowed.add("mprotect");
        minimalAllowed.add("munmap");
        minimalAllowed.add("brk");
        minimalAllowed.add("sigaction");
        minimalAllowed.add("ioctl");
        minimalAllowed.add("access");
        minimalAllowed.add("dup");
        minimalAllowed.add("dup2");
        minimalAllowed.add("socket");
        minimalAllowed.add("connect");
        minimalAllowed.add("accept");
        minimalAllowed.add("sendto");
        minimalAllowed.add("recvfrom");
        minimalAllowed.add("readlink");
        minimalAllowed.add("chmod");
        minimalAllowed.add("chown");
        minimalAllowed.add("getuid");
        minimalAllowed.add("getgid");
        minimalAllowed.add("execve");
        mAllowedSyscalls.put(SANDBOX_LEVEL_MINIMAL, minimalAllowed);
        
        // 标准限制规则
        Set<String> standardAllowed = new HashSet<>(minimalAllowed);
        standardAllowed.remove("execve"); // 不允许执行其他程序
        mAllowedSyscalls.put(SANDBOX_LEVEL_STANDARD, standardAllowed);
        
        Set<String> standardDenied = new HashSet<>();
        standardDenied.add("execve");
        mDeniedSyscalls.put(SANDBOX_LEVEL_STANDARD, standardDenied);
        
        // 严格限制规则
        Set<String> strictAllowed = new HashSet<>();
        strictAllowed.add("read");
        strictAllowed.add("write");
        strictAllowed.add("open");
        strictAllowed.add("close");
        strictAllowed.add("stat");
        strictAllowed.add("fstat");
        strictAllowed.add("lstat");
        strictAllowed.add("poll");
        strictAllowed.add("mmap");
        strictAllowed.add("mprotect");
        strictAllowed.add("munmap");
        strictAllowed.add("brk");
        strictAllowed.add("sigaction");
        mAllowedSyscalls.put(SANDBOX_LEVEL_STRICT, strictAllowed);
        
        Set<String> strictDenied = new HashSet<>();
        strictDenied.add("socket");
        strictDenied.add("connect");
        strictDenied.add("accept");
        strictDenied.add("sendto");
        strictDenied.add("recvfrom");
        strictDenied.add("execve");
        mDeniedSyscalls.put(SANDBOX_LEVEL_STRICT, strictDenied);
    }
    
    /**
     * 应用过滤器
     * 
     * @param level 安全级别
     * @return 是否应用成功
     */
    public boolean applyFilter(int level) {
        if (level < SANDBOX_LEVEL_MINIMAL || level > SANDBOX_LEVEL_STRICT) {
            Log.e(TAG, "无效的安全级别: " + level);
            return false;
        }
        
        Log.d(TAG, "应用Seccomp过滤器，安全级别: " + level);
        
        // 保存当前级别
        mCurrentLevel = level;
        
        // 获取允许的系统调用
        Set<String> allowedCalls = mAllowedSyscalls.get(level);
        int[] allowedSyscalls = new int[allowedCalls.size()];
        int i = 0;
        for (String syscall : allowedCalls) {
            Integer syscallNumber = mSyscallMap.get(syscall);
            if (syscallNumber != null) {
                allowedSyscalls[i++] = syscallNumber;
            }
        }
        
        // 获取禁止的系统调用
        Set<String> deniedCalls = mDeniedSyscalls.get(level);
        int[] deniedSyscalls = null;
        if (deniedCalls != null && !deniedCalls.isEmpty()) {
            deniedSyscalls = new int[deniedCalls.size()];
            i = 0;
            for (String syscall : deniedCalls) {
                Integer syscallNumber = mSyscallMap.get(syscall);
                if (syscallNumber != null) {
                    deniedSyscalls[i++] = syscallNumber;
                }
            }
        }
        
        try {
            boolean result = nativeApplySeccompFilter(level, allowedSyscalls, deniedSyscalls);
            mInitialized = result;
            Log.d(TAG, "应用Seccomp过滤器" + (result ? "成功" : "失败"));
            return result;
        } catch (Exception e) {
            Log.e(TAG, "应用Seccomp过滤器异常", e);
            return false;
        }
    }
    
    /**
     * 重置过滤器
     * 
     * @return 是否重置成功
     */
    public boolean resetFilter() {
        if (!mInitialized) {
            return true;
        }
        
        try {
            boolean result = nativeResetSeccompFilter();
            Log.d(TAG, "重置Seccomp过滤器" + (result ? "成功" : "失败"));
            mInitialized = !result;
            return result;
        } catch (Exception e) {
            Log.e(TAG, "重置Seccomp过滤器异常", e);
            return false;
        }
    }
    
    /**
     * 获取当前安全级别
     * 
     * @return 安全级别
     */
    public int getCurrentLevel() {
        return mCurrentLevel;
    }
    
    /**
     * 检查是否支持Seccomp过滤器
     * 
     * @return 是否支持
     */
    public boolean isSupported() {
        return nativeIsSeccompSupported();
    }
    
    /**
     * 应用Seccomp过滤器（本地方法）
     * 
     * @param level 安全级别
     * @param allowedSyscalls 允许的系统调用列表
     * @param deniedSyscalls 禁止的系统调用列表
     * @return 是否成功
     */
    private native boolean nativeApplySeccompFilter(int level, int[] allowedSyscalls, int[] deniedSyscalls);
    
    /**
     * 重置Seccomp过滤器（本地方法）
     * 
     * @return 是否成功
     */
    private native boolean nativeResetSeccompFilter();
    
    /**
     * 检查是否支持Seccomp过滤器（本地方法）
     * 
     * @return 是否支持
     */
    private native boolean nativeIsSeccompSupported();
} 