package com.mobileplatform.creator.mpk;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MPK 权限管理
 * 用于管理应用权限验证和请求
 */
public class MpkPermissionManager {
    private static final String TAG = "MpkPermissionManager";
    
    /**
     * 权限类型
     */
    public enum PermissionType {
        // 系统权限
        CAMERA("camera", android.Manifest.permission.CAMERA),
        MICROPHONE("microphone", android.Manifest.permission.RECORD_AUDIO),
        LOCATION("location", android.Manifest.permission.ACCESS_FINE_LOCATION),
        STORAGE("storage", android.Manifest.permission.WRITE_EXTERNAL_STORAGE),
        CONTACTS("contacts", android.Manifest.permission.READ_CONTACTS),
        CALENDAR("calendar", android.Manifest.permission.READ_CALENDAR),
        PHONE("phone", android.Manifest.permission.READ_PHONE_STATE),
        SMS("sms", android.Manifest.permission.READ_SMS),
        SENSORS("sensors", android.Manifest.permission.BODY_SENSORS),
        
        // 平台自定义权限
        NETWORK("network", "com.mobileplatform.permission.NETWORK"),
        BLUETOOTH("bluetooth", "com.mobileplatform.permission.BLUETOOTH"),
        NFC("nfc", "com.mobileplatform.permission.NFC"),
        FILE_ACCESS("file_access", "com.mobileplatform.permission.FILE_ACCESS"),
        NOTIFICATION("notification", "com.mobileplatform.permission.NOTIFICATION"),
        BACKGROUND_EXECUTION("background_execution", "com.mobileplatform.permission.BACKGROUND_EXECUTION"),
        SYSTEM_SETTINGS("system_settings", "com.mobileplatform.permission.SYSTEM_SETTINGS"),
        APP_MANAGEMENT("app_management", "com.mobileplatform.permission.APP_MANAGEMENT"),
        INTER_APP_COMMUNICATION("inter_app_communication", "com.mobileplatform.permission.INTER_APP_COMMUNICATION");
        
        private final String name;
        private final String value;
        
        PermissionType(String name, String value) {
            this.name = name;
            this.value = value;
        }
        
        public String getName() {
            return name;
        }
        
        public String getValue() {
            return value;
        }
        
        /**
         * 根据名称获取权限类型
         * 
         * @param name 权限名称
         * @return 权限类型
         */
        public static PermissionType fromName(String name) {
            for (PermissionType type : values()) {
                if (type.getName().equals(name)) {
                    return type;
                }
            }
            return null;
        }
        
        /**
         * 根据值获取权限类型
         * 
         * @param value 权限值
         * @return 权限类型
         */
        public static PermissionType fromValue(String value) {
            for (PermissionType type : values()) {
                if (type.getValue().equals(value)) {
                    return type;
                }
            }
            return null;
        }
    }
    
    /**
     * 权限回调接口
     */
    public interface PermissionCallback {
        /**
         * 权限授予回调
         * 
         * @param permissions 授予的权限
         */
        void onPermissionsGranted(List<String> permissions);
        
        /**
         * 权限拒绝回调
         * 
         * @param permissions 拒绝的权限
         * @param neverAskAgain 是否永久拒绝
         */
        void onPermissionsDenied(List<String> permissions, boolean neverAskAgain);
    }
    
    /**
     * 权限结果接口
     */
    public interface PermissionResult {
        /**
         * 全部授予
         * 
         * @return 是否全部授予
         */
        boolean areAllGranted();
        
        /**
         * 部分授予
         * 
         * @return 是否部分授予
         */
        boolean areSomeGranted();
        
        /**
         * 全部拒绝
         * 
         * @return 是否全部拒绝
         */
        boolean areAllDenied();
        
        /**
         * 获取已授予的权限
         * 
         * @return 已授予的权限
         */
        List<String> getGrantedPermissions();
        
        /**
         * 获取已拒绝的权限
         * 
         * @return 已拒绝的权限
         */
        List<String> getDeniedPermissions();
    }
    
    // 权限请求码范围
    private static final int REQUEST_CODE_START = 1000;
    private static final int REQUEST_CODE_END = 1999;
    
    // 当前使用的请求码
    private int currentRequestCode = REQUEST_CODE_START;
    
    // 上下文
    private final Context context;
    
    // 应用权限映射表
    private final Map<String, Set<String>> appPermissions;
    
    // 权限回调映射表
    private final Map<Integer, PermissionCallback> callbacks;
    
    // 权限请求映射表
    private final Map<Integer, List<String>> requestMap;
    
    // 已授予的权限缓存
    private final Map<String, Set<String>> grantedPermissionsCache;
    
    /**
     * 构造函数
     * 
     * @param context 上下文
     */
    public MpkPermissionManager(Context context) {
        this.context = context;
        this.appPermissions = new ConcurrentHashMap<>();
        this.callbacks = new HashMap<>();
        this.requestMap = new HashMap<>();
        this.grantedPermissionsCache = new ConcurrentHashMap<>();
    }
    
    /**
     * 注册应用权限
     * 
     * @param appId 应用 ID
     * @param permissions 权限列表
     * @return 是否成功注册
     */
    public boolean registerAppPermissions(String appId, List<String> permissions) {
        if (appId == null || appId.isEmpty()) {
            Log.e(TAG, "应用 ID 不能为空");
            return false;
        }
        
        if (permissions == null || permissions.isEmpty()) {
            Log.w(TAG, "权限列表为空: " + appId);
            return true;
        }
        
        Set<String> permissionSet = new HashSet<>(permissions);
        appPermissions.put(appId, permissionSet);
        
        Log.i(TAG, "应用权限已注册: " + appId + ", " + permissions);
        return true;
    }
    
    /**
     * 注销应用权限
     * 
     * @param appId 应用 ID
     * @return 是否成功注销
     */
    public boolean unregisterAppPermissions(String appId) {
        if (appId == null || appId.isEmpty()) {
            Log.e(TAG, "应用 ID 不能为空");
            return false;
        }
        
        if (!appPermissions.containsKey(appId)) {
            Log.w(TAG, "应用未注册权限: " + appId);
            return true;
        }
        
        appPermissions.remove(appId);
        grantedPermissionsCache.remove(appId);
        
        Log.i(TAG, "应用权限已注销: " + appId);
        return true;
    }
    
    /**
     * 应用是否注册了权限
     * 
     * @param appId 应用 ID
     * @return 是否注册了权限
     */
    public boolean hasRegisteredPermissions(String appId) {
        return appPermissions.containsKey(appId);
    }
    
    /**
     * 获取应用注册的权限
     * 
     * @param appId 应用 ID
     * @return 权限列表
     */
    public Set<String> getRegisteredPermissions(String appId) {
        Set<String> permissions = appPermissions.get(appId);
        return permissions != null ? Collections.unmodifiableSet(permissions) : Collections.emptySet();
    }
    
    /**
     * 添加应用权限
     * 
     * @param appId 应用 ID
     * @param permission 权限
     * @return 是否成功添加
     */
    public boolean addAppPermission(String appId, String permission) {
        if (appId == null || appId.isEmpty()) {
            Log.e(TAG, "应用 ID 不能为空");
            return false;
        }
        
        if (permission == null || permission.isEmpty()) {
            Log.e(TAG, "权限不能为空");
            return false;
        }
        
        Set<String> permissions = appPermissions.computeIfAbsent(appId, k -> new HashSet<>());
        boolean result = permissions.add(permission);
        
        if (result) {
            Log.i(TAG, "应用权限已添加: " + appId + ", " + permission);
        }
        
        return result;
    }
    
    /**
     * 移除应用权限
     * 
     * @param appId 应用 ID
     * @param permission 权限
     * @return 是否成功移除
     */
    public boolean removeAppPermission(String appId, String permission) {
        if (appId == null || appId.isEmpty()) {
            Log.e(TAG, "应用 ID 不能为空");
            return false;
        }
        
        if (permission == null || permission.isEmpty()) {
            Log.e(TAG, "权限不能为空");
            return false;
        }
        
        Set<String> permissions = appPermissions.get(appId);
        if (permissions == null) {
            return false;
        }
        
        boolean result = permissions.remove(permission);
        
        if (result) {
            Log.i(TAG, "应用权限已移除: " + appId + ", " + permission);
            
            // 从已授予的权限缓存中移除
            Set<String> grantedPermissions = grantedPermissionsCache.get(appId);
            if (grantedPermissions != null) {
                grantedPermissions.remove(permission);
            }
        }
        
        return result;
    }
    
    /**
     * 应用是否具有权限
     * 
     * @param appId 应用 ID
     * @param permission 权限
     * @return 是否具有权限
     */
    public boolean hasPermission(String appId, String permission) {
        if (appId == null || appId.isEmpty()) {
            return false;
        }
        
        if (permission == null || permission.isEmpty()) {
            return false;
        }
        
        Set<String> permissions = appPermissions.get(appId);
        return permissions != null && permissions.contains(permission);
    }
    
    /**
     * 应用是否具有权限类型
     * 
     * @param appId 应用 ID
     * @param type 权限类型
     * @return 是否具有权限类型
     */
    public boolean hasPermissionType(String appId, PermissionType type) {
        return hasPermission(appId, type.getName());
    }
    
    /**
     * 检查应用权限是否已授予
     * 
     * @param appId 应用 ID
     * @param permission 权限
     * @return 是否已授予权限
     */
    public boolean checkPermission(String appId, String permission) {
        if (appId == null || appId.isEmpty()) {
            Log.e(TAG, "应用 ID 不能为空");
            return false;
        }
        
        if (permission == null || permission.isEmpty()) {
            Log.e(TAG, "权限不能为空");
            return false;
        }
        
        // 检查应用是否注册了该权限
        if (!hasPermission(appId, permission)) {
            Log.w(TAG, "应用未注册权限: " + appId + ", " + permission);
            return false;
        }
        
        // 检查缓存
        Set<String> grantedPermissions = grantedPermissionsCache.get(appId);
        if (grantedPermissions != null && grantedPermissions.contains(permission)) {
            return true;
        }
        
        // 获取权限类型
        PermissionType type = PermissionType.fromName(permission);
        if (type == null) {
            Log.w(TAG, "未知权限类型: " + permission);
            return false;
        }
        
        // 检查权限是否已授予
        boolean granted = false;
        if (type.getValue().startsWith("android.permission.")) {
            // 系统权限
            granted = ContextCompat.checkSelfPermission(context, type.getValue()) == PackageManager.PERMISSION_GRANTED;
        } else {
            // 平台自定义权限
            // TODO: 实现平台自定义权限检查
            granted = true;
        }
        
        // 更新缓存
        if (granted) {
            grantedPermissions = grantedPermissionsCache.computeIfAbsent(appId, k -> new HashSet<>());
            grantedPermissions.add(permission);
        }
        
        return granted;
    }
    
    /**
     * 检查应用权限类型是否已授予
     * 
     * @param appId 应用 ID
     * @param type 权限类型
     * @return 是否已授予权限
     */
    public boolean checkPermissionType(String appId, PermissionType type) {
        return checkPermission(appId, type.getName());
    }
    
    /**
     * 检查应用是否具有多个权限
     * 
     * @param appId 应用 ID
     * @param permissions 权限列表
     * @return 权限结果
     */
    public PermissionResult checkPermissions(String appId, List<String> permissions) {
        if (appId == null || appId.isEmpty()) {
            Log.e(TAG, "应用 ID 不能为空");
            return createPermissionResult(Collections.emptyList(), permissions);
        }
        
        if (permissions == null || permissions.isEmpty()) {
            Log.w(TAG, "权限列表为空: " + appId);
            return createPermissionResult(Collections.emptyList(), Collections.emptyList());
        }
        
        List<String> grantedList = new ArrayList<>();
        List<String> deniedList = new ArrayList<>();
        
        for (String permission : permissions) {
            if (checkPermission(appId, permission)) {
                grantedList.add(permission);
            } else {
                deniedList.add(permission);
            }
        }
        
        return createPermissionResult(grantedList, deniedList);
    }
    
    /**
     * 请求应用权限
     * 
     * @param activity 活动
     * @param appId 应用 ID
     * @param permissions 权限列表
     * @param callback 权限回调
     * @return 请求码
     */
    public int requestPermissions(Activity activity, String appId, List<String> permissions, PermissionCallback callback) {
        if (activity == null) {
            Log.e(TAG, "活动不能为空");
            return -1;
        }
        
        if (appId == null || appId.isEmpty()) {
            Log.e(TAG, "应用 ID 不能为空");
            return -1;
        }
        
        if (permissions == null || permissions.isEmpty()) {
            Log.w(TAG, "权限列表为空: " + appId);
            return -1;
        }
        
        // 检查应用是否注册了权限
        List<String> requestPermissions = new ArrayList<>();
        List<String> systemPermissions = new ArrayList<>();
        
        for (String permission : permissions) {
            if (!hasPermission(appId, permission)) {
                Log.w(TAG, "应用未注册权限: " + appId + ", " + permission);
                continue;
            }
            
            // 检查权限是否已授予
            if (checkPermission(appId, permission)) {
                continue;
            }
            
            requestPermissions.add(permission);
            
            // 获取权限类型
            PermissionType type = PermissionType.fromName(permission);
            if (type != null && type.getValue().startsWith("android.permission.")) {
                systemPermissions.add(type.getValue());
            }
        }
        
        if (requestPermissions.isEmpty()) {
            Log.i(TAG, "所有权限已授予: " + appId);
            
            if (callback != null) {
                callback.onPermissionsGranted(permissions);
            }
            
            return -1;
        }
        
        // 获取请求码
        int requestCode = getNextRequestCode();
        
        // 保存回调和请求权限
        callbacks.put(requestCode, callback);
        requestMap.put(requestCode, requestPermissions);
        
        // 请求系统权限
        if (!systemPermissions.isEmpty()) {
            String[] permissionArray = systemPermissions.toArray(new String[0]);
            ActivityCompat.requestPermissions(activity, permissionArray, requestCode);
        }
        
        Log.i(TAG, "请求权限: " + appId + ", " + requestPermissions + ", 请求码: " + requestCode);
        return requestCode;
    }
    
    /**
     * 请求应用权限类型
     * 
     * @param activity 活动
     * @param appId 应用 ID
     * @param types 权限类型列表
     * @param callback 权限回调
     * @return 请求码
     */
    public int requestPermissionTypes(Activity activity, String appId, List<PermissionType> types, PermissionCallback callback) {
        List<String> permissions = new ArrayList<>();
        for (PermissionType type : types) {
            permissions.add(type.getName());
        }
        
        return requestPermissions(activity, appId, permissions, callback);
    }
    
    /**
     * 处理权限请求结果
     * 
     * @param requestCode 请求码
     * @param permissions 权限
     * @param grantResults 授权结果
     */
    public void handlePermissionResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        // 检查请求码是否有效
        if (requestCode < REQUEST_CODE_START || requestCode > REQUEST_CODE_END) {
            Log.w(TAG, "无效请求码: " + requestCode);
            return;
        }
        
        // 检查请求是否存在
        List<String> requestPermissions = requestMap.get(requestCode);
        if (requestPermissions == null) {
            Log.w(TAG, "请求不存在: " + requestCode);
            return;
        }
        
        // 检查回调是否存在
        PermissionCallback callback = callbacks.get(requestCode);
        if (callback == null) {
            Log.w(TAG, "回调不存在: " + requestCode);
            return;
        }
        
        // 处理结果
        List<String> grantedList = new ArrayList<>();
        List<String> deniedList = new ArrayList<>();
        boolean neverAskAgain = false;
        
        // 检查是否有权限被授予
        for (int i = 0; i < permissions.length; i++) {
            String permission = permissions[i];
            int grantResult = grantResults[i];
            
            PermissionType type = PermissionType.fromValue(permission);
            if (type == null) {
                Log.w(TAG, "未知权限: " + permission);
                continue;
            }
            
            String permissionName = type.getName();
            
            if (grantResult == PackageManager.PERMISSION_GRANTED) {
                grantedList.add(permissionName);
            } else {
                deniedList.add(permissionName);
                
                // 检查是否永久拒绝
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    Activity activity = getActivity();
                    if (activity != null && !ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
                        neverAskAgain = true;
                    }
                }
            }
        }
        
        // 回调
        if (!grantedList.isEmpty()) {
            callback.onPermissionsGranted(grantedList);
        }
        
        if (!deniedList.isEmpty()) {
            callback.onPermissionsDenied(deniedList, neverAskAgain);
        }
        
        // 清理
        callbacks.remove(requestCode);
        requestMap.remove(requestCode);
        
        Log.i(TAG, "处理权限结果: 请求码=" + requestCode + ", 授予=" + grantedList + ", 拒绝=" + deniedList);
    }
    
    /**
     * 获取活动
     * 
     * @return 活动
     */
    @Nullable
    private Activity getActivity() {
        if (context instanceof Activity) {
            return (Activity) context;
        }
        return null;
    }
    
    /**
     * 获取下一个请求码
     * 
     * @return 请求码
     */
    private synchronized int getNextRequestCode() {
        if (currentRequestCode > REQUEST_CODE_END) {
            currentRequestCode = REQUEST_CODE_START;
        }
        return currentRequestCode++;
    }
    
    /**
     * 创建权限结果
     * 
     * @param grantedList 已授予的权限列表
     * @param deniedList 已拒绝的权限列表
     * @return 权限结果
     */
    private PermissionResult createPermissionResult(List<String> grantedList, List<String> deniedList) {
        return new PermissionResult() {
            @Override
            public boolean areAllGranted() {
                return !grantedList.isEmpty() && deniedList.isEmpty();
            }
            
            @Override
            public boolean areSomeGranted() {
                return !grantedList.isEmpty() && !deniedList.isEmpty();
            }
            
            @Override
            public boolean areAllDenied() {
                return grantedList.isEmpty() && !deniedList.isEmpty();
            }
            
            @Override
            public List<String> getGrantedPermissions() {
                return Collections.unmodifiableList(grantedList);
            }
            
            @Override
            public List<String> getDeniedPermissions() {
                return Collections.unmodifiableList(deniedList);
            }
        };
    }
    
    /**
     * 从 JSON 对象获取权限列表
     * 
     * @param json JSON 对象
     * @return 权限列表
     */
    public static List<String> getPermissionsFromJson(JSONObject json) {
        if (json == null || !json.has("permissions")) {
            return Collections.emptyList();
        }
        
        try {
            JSONArray permissionsArray = json.getJSONArray("permissions");
            List<String> permissions = new ArrayList<>();
            
            for (int i = 0; i < permissionsArray.length(); i++) {
                String permission = permissionsArray.getString(i);
                permissions.add(permission);
            }
            
            return permissions;
        } catch (JSONException e) {
            Log.e(TAG, "解析权限 JSON 失败", e);
            return Collections.emptyList();
        }
    }
    
    /**
     * 将权限列表转换为 JSON 对象
     * 
     * @param permissions 权限列表
     * @return JSON 对象
     */
    public static JSONObject permissionsToJson(List<String> permissions) {
        JSONObject json = new JSONObject();
        
        try {
            JSONArray permissionsArray = new JSONArray();
            for (String permission : permissions) {
                permissionsArray.put(permission);
            }
            
            json.put("permissions", permissionsArray);
        } catch (JSONException e) {
            Log.e(TAG, "转换权限为 JSON 失败", e);
        }
        
        return json;
    }
    
    /**
     * 从 JSON 对象获取权限类型列表
     * 
     * @param json JSON 对象
     * @return 权限类型列表
     */
    public static List<PermissionType> getPermissionTypesFromJson(JSONObject json) {
        List<String> permissions = getPermissionsFromJson(json);
        List<PermissionType> types = new ArrayList<>();
        
        for (String permission : permissions) {
            PermissionType type = PermissionType.fromName(permission);
            if (type != null) {
                types.add(type);
            }
        }
        
        return types;
    }
    
    /**
     * 获取所有权限类型名称
     * 
     * @return 权限类型名称列表
     */
    public static List<String> getAllPermissionNames() {
        List<String> names = new ArrayList<>();
        for (PermissionType type : PermissionType.values()) {
            names.add(type.getName());
        }
        return names;
    }
    
    /**
     * 获取所有权限类型
     * 
     * @return 权限类型列表
     */
    public static List<PermissionType> getAllPermissionTypes() {
        return Arrays.asList(PermissionType.values());
    }
    
    /**
     * 获取所有系统权限类型
     * 
     * @return 系统权限类型列表
     */
    public static List<PermissionType> getSystemPermissionTypes() {
        List<PermissionType> types = new ArrayList<>();
        for (PermissionType type : PermissionType.values()) {
            if (type.getValue().startsWith("android.permission.")) {
                types.add(type);
            }
        }
        return types;
    }
    
    /**
     * 获取所有平台自定义权限类型
     * 
     * @return 平台自定义权限类型列表
     */
    public static List<PermissionType> getPlatformPermissionTypes() {
        List<PermissionType> types = new ArrayList<>();
        for (PermissionType type : PermissionType.values()) {
            if (type.getValue().startsWith("com.mobileplatform.permission.")) {
                types.add(type);
            }
        }
        return types;
    }
} 