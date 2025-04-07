package com.mobileplatform.creator.data.model;

import android.graphics.drawable.Drawable;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * MPK应用包数据模型
 * 
 * MPK包是移动平台创建器定义的自定义应用包格式，用于应用分发与安装
 * MPK包结构：
 * - manifest.json：应用元数据，如ID、名称、版本等
 * - code/：应用代码目录
 * - assets/：应用资源目录
 * - signature.sig：数字签名文件
 * - config/：可选的配置目录
 */
public class MPKPackage {
    // 必要的清单字段
    private static final Set<String> REQUIRED_MANIFEST_FIELDS = new HashSet<String>() {{
        add("id");
        add("name");
        add("version");
        add("platform");
        add("min_platform_version");
    }};
    
    // 包信息
    private String id;
    private String name;
    private String version;
    private String platform;
    private String minPlatformVersion;
    private String description;
    private String author;
    private String iconPath;
    private List<String> permissions;
    private JSONObject manifest;
    private Drawable icon;
    
    // 包文件路径
    private String filePath;
    
    // 文件列表
    private List<String> fileList;
    
    // 附加元数据
    private Map<String, Object> metadata;
    
    /**
     * 构造空的MPK包对象
     */
    public MPKPackage() {
        this.id = "";
        this.name = "";
        this.version = "1.0.0";
        this.platform = "android";
        this.minPlatformVersion = "1.0.0";
        this.description = "";
        this.author = "";
        this.iconPath = "assets/icon.png";
        this.permissions = new ArrayList<>();
        this.fileList = new ArrayList<>();
        this.metadata = new HashMap<>();
        initManifest();
    }
    
    /**
     * 使用基本信息构造MPK包对象
     * 
     * @param id 包ID
     * @param name 应用名称
     * @param version 版本号
     */
    public MPKPackage(String id, String name, String version) {
        this();
        this.id = id;
        this.name = name;
        this.version = version;
        initManifest();
    }
    
    /**
     * 使用JSON清单构造MPK包对象
     * 
     * @param manifest 清单JSON对象
     * @throws IllegalArgumentException 如果清单缺少必要字段
     */
    public MPKPackage(JSONObject manifest) throws IllegalArgumentException {
        this();
        
        try {
            // 验证必要字段
            for (String field : REQUIRED_MANIFEST_FIELDS) {
                if (!manifest.has(field)) {
                    throw new IllegalArgumentException("清单缺少必要字段: " + field);
                }
            }
            
            this.manifest = manifest;
            this.id = manifest.getString("id");
            this.name = manifest.getString("name");
            this.version = manifest.getString("version");
            this.platform = manifest.getString("platform");
            this.minPlatformVersion = manifest.getString("min_platform_version");
            
            if (manifest.has("description")) {
                this.description = manifest.getString("description");
            }
            
            if (manifest.has("author")) {
                this.author = manifest.getString("author");
            }
            
            if (manifest.has("icon")) {
                this.iconPath = manifest.getString("icon");
            }
            
            if (manifest.has("permissions")) {
                this.permissions = new ArrayList<>();
                for (int i = 0; i < manifest.getJSONArray("permissions").length(); i++) {
                    this.permissions.add(manifest.getJSONArray("permissions").getString(i));
                }
            }
        } catch (JSONException e) {
            throw new IllegalArgumentException("解析清单文件失败: " + e.getMessage());
        }
    }
    
    /**
     * 初始化清单对象
     */
    private void initManifest() {
        try {
            manifest = new JSONObject();
            manifest.put("id", id);
            manifest.put("name", name);
            manifest.put("version", version);
            manifest.put("platform", platform);
            manifest.put("min_platform_version", minPlatformVersion);
            manifest.put("description", description);
            manifest.put("author", author);
            manifest.put("icon", iconPath);
            
            // 添加权限
            manifest.put("permissions", new org.json.JSONArray(permissions));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * 更新清单对象
     */
    private void updateManifest() {
        try {
            manifest.put("id", id);
            manifest.put("name", name);
            manifest.put("version", version);
            manifest.put("platform", platform);
            manifest.put("min_platform_version", minPlatformVersion);
            manifest.put("description", description);
            manifest.put("author", author);
            manifest.put("icon", iconPath);
            
            // 添加权限
            org.json.JSONArray permArray = new org.json.JSONArray();
            for (String perm : permissions) {
                permArray.put(perm);
            }
            manifest.put("permissions", permArray);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * 获取ID
     */
    public String getId() {
        return id;
    }
    
    /**
     * 设置ID
     */
    public void setId(String id) {
        this.id = id;
        try {
            manifest.put("id", id);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * 获取应用名称
     */
    public String getName() {
        return name;
    }
    
    /**
     * 设置应用名称
     */
    public void setName(String name) {
        this.name = name;
        try {
            manifest.put("name", name);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * 获取版本号
     */
    public String getVersion() {
        return version;
    }
    
    /**
     * 设置版本号
     */
    public void setVersion(String version) {
        this.version = version;
        try {
            manifest.put("version", version);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * 获取平台类型
     */
    public String getPlatform() {
        return platform;
    }
    
    /**
     * 设置平台类型
     */
    public void setPlatform(String platform) {
        this.platform = platform;
        try {
            manifest.put("platform", platform);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * 获取最低平台版本
     */
    public String getMinPlatformVersion() {
        return minPlatformVersion;
    }
    
    /**
     * 设置最低平台版本
     */
    public void setMinPlatformVersion(String minPlatformVersion) {
        this.minPlatformVersion = minPlatformVersion;
        try {
            manifest.put("min_platform_version", minPlatformVersion);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * 获取应用描述
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * 设置应用描述
     */
    public void setDescription(String description) {
        this.description = description;
        try {
            manifest.put("description", description);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * 获取作者信息
     */
    public String getAuthor() {
        return author;
    }
    
    /**
     * 设置作者信息
     */
    public void setAuthor(String author) {
        this.author = author;
        try {
            manifest.put("author", author);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * 获取权限列表
     */
    public List<String> getPermissions() {
        return new ArrayList<>(permissions);
    }
    
    /**
     * 设置权限列表
     */
    public void setPermissions(List<String> permissions) {
        this.permissions = new ArrayList<>(permissions);
        updateManifest();
    }
    
    /**
     * 添加权限
     */
    public void addPermission(String permission) {
        if (!this.permissions.contains(permission)) {
            this.permissions.add(permission);
            updateManifest();
        }
    }
    
    /**
     * 获取图标
     */
    public Drawable getIcon() {
        return icon;
    }
    
    /**
     * 设置图标
     */
    public void setIcon(Drawable icon) {
        this.icon = icon;
    }
    
    /**
     * 获取图标路径
     */
    public String getIconPath() {
        return iconPath;
    }
    
    /**
     * 设置图标路径
     */
    public void setIconPath(String iconPath) {
        this.iconPath = iconPath;
        try {
            manifest.put("icon", iconPath);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * 获取文件路径
     */
    public String getFilePath() {
        return filePath;
    }
    
    /**
     * 设置文件路径
     */
    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
    
    /**
     * 获取文件列表
     */
    public List<String> getFileList() {
        return new ArrayList<>(fileList);
    }
    
    /**
     * 设置文件列表
     */
    public void setFileList(List<String> fileList) {
        this.fileList = new ArrayList<>(fileList);
    }
    
    /**
     * 添加文件到列表
     */
    public void addFile(String filePath) {
        if (!this.fileList.contains(filePath)) {
            this.fileList.add(filePath);
        }
    }
    
    /**
     * 获取清单对象
     */
    public JSONObject getManifest() {
        return manifest;
    }
    
    /**
     * 将元数据值存储到包中
     * 
     * @param key 键
     * @param value 值
     */
    public void putMetadata(String key, Object value) {
        metadata.put(key, value);
    }
    
    /**
     * 获取元数据值
     * 
     * @param key 键
     * @return 值，如果不存在则返回null
     */
    public Object getMetadata(String key) {
        return metadata.get(key);
    }
    
    /**
     * 检查是否为有效的MPK文件
     * 
     * @param file MPK文件
     * @return 是否有效
     */
    public static boolean isValidMPK(File file) {
        // TODO: 实现MPK文件验证逻辑
        return file != null && file.exists() && file.getName().endsWith(".mpk");
    }
    
    @Override
    public String toString() {
        return name + " (" + version + ")";
    }
} 