package com.mobileplatform.creator.data.model;

import android.graphics.drawable.Drawable;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

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
    private static final String TAG = "MPKPackage";
    
    // 必要的清单字段
    private static final Set<String> REQUIRED_MANIFEST_FIELDS = new HashSet<String>() {{
        add("format_version");
        add("id");
        add("name");
        add("version");
        add("platform");
        add("min_platform_version");
        add("code_type");
        add("entry_point");
    }};
    
    // 包信息
    private String formatVersion;
    private String id;
    private String name;
    private Version version;
    private String platform;
    private String minPlatformVersion;
    private String description;
    private Author author;
    private String iconPath;
    private String splashPath;
    private List<String> permissions;
    private List<Dependency> dependencies;
    private SandboxConfig sandbox;
    private JSONObject manifest;
    private Drawable icon;
    
    // 包文件路径
    private String filePath;
    
    // 文件列表
    private List<String> fileList;
    
    // 附加元数据
    private Map<String, Object> metadata;
    
    /**
     * 版本信息类
     */
    public static class Version {
        public int code;
        public String name;
        
        public Version(int code, String name) {
            this.code = code;
            this.name = name;
        }
    }
    
    /**
     * 作者信息类
     */
    public static class Author {
        public String name;
        public String email;
        
        public Author(String name, String email) {
            this.name = name;
            this.email = email;
        }
    }
    
    /**
     * 依赖信息类
     */
    public static class Dependency {
        public String name;
        public String version;
        
        public Dependency(String name, String version) {
            this.name = name;
            this.version = version;
        }
    }
    
    /**
     * 沙箱配置类
     */
    public static class SandboxConfig {
        public long maxStorage;
        public int maxProcesses;
        public long maxMemory;
        
        public SandboxConfig(long maxStorage, int maxProcesses, long maxMemory) {
            this.maxStorage = maxStorage;
            this.maxProcesses = maxProcesses;
            this.maxMemory = maxMemory;
        }
    }
    
    /**
     * 构造空的MPK包对象
     */
    public MPKPackage() {
        this.fileList = new ArrayList<>();
        this.permissions = new ArrayList<>();
        this.dependencies = new ArrayList<>();
        this.metadata = new HashMap<>();
    }
    
    /**
     * 从文件加载MPK包
     * @param file MPK文件
     * @return MPK包对象
     * @throws IOException 如果文件读取失败
     * @throws JSONException 如果JSON解析失败
     */
    public static MPKPackage fromFile(File file) throws IOException, JSONException {
        MPKPackage mpk = new MPKPackage();
        mpk.filePath = file.getAbsolutePath();
        
        try (ZipFile zipFile = new ZipFile(file)) {
            // 读取清单文件
            ZipEntry manifestEntry = zipFile.getEntry("manifest.json");
            if (manifestEntry == null) {
                throw new IOException("缺少清单文件");
            }
            
            String manifestJson = new String(zipFile.getInputStream(manifestEntry).readAllBytes());
            mpk.manifest = new JSONObject(manifestJson);
            
            // 验证必需字段
            for (String field : REQUIRED_MANIFEST_FIELDS) {
                if (!mpk.manifest.has(field)) {
                    throw new IOException("清单文件缺少必需字段: " + field);
                }
            }
            
            // 解析清单文件
            mpk.formatVersion = mpk.manifest.getString("format_version");
            mpk.id = mpk.manifest.getString("id");
            mpk.name = mpk.manifest.getString("name");
            
            JSONObject versionObj = mpk.manifest.getJSONObject("version");
            mpk.version = new Version(
                versionObj.getInt("code"),
                versionObj.getString("name")
            );
            
            mpk.platform = mpk.manifest.getString("platform");
            mpk.minPlatformVersion = mpk.manifest.getString("min_platform_version");
            
            // 解析可选字段
            if (mpk.manifest.has("description")) {
                mpk.description = mpk.manifest.getString("description");
            }
            
            if (mpk.manifest.has("author")) {
                JSONObject authorObj = mpk.manifest.getJSONObject("author");
                mpk.author = new Author(
                    authorObj.getString("name"),
                    authorObj.getString("email")
                );
            }
            
            if (mpk.manifest.has("icon")) {
                mpk.iconPath = mpk.manifest.getString("icon");
            }
            
            if (mpk.manifest.has("splash")) {
                mpk.splashPath = mpk.manifest.getString("splash");
            }
            
            if (mpk.manifest.has("permissions")) {
                mpk.permissions.clear();
                for (int i = 0; i < mpk.manifest.getJSONArray("permissions").length(); i++) {
                    mpk.permissions.add(mpk.manifest.getJSONArray("permissions").getString(i));
                }
            }
            
            if (mpk.manifest.has("dependencies")) {
                mpk.dependencies.clear();
                for (int i = 0; i < mpk.manifest.getJSONArray("dependencies").length(); i++) {
                    JSONObject depObj = mpk.manifest.getJSONArray("dependencies").getJSONObject(i);
                    mpk.dependencies.add(new Dependency(
                        depObj.getString("name"),
                        depObj.getString("version")
                    ));
                }
            }
            
            if (mpk.manifest.has("sandbox")) {
                JSONObject sandboxObj = mpk.manifest.getJSONObject("sandbox");
                mpk.sandbox = new SandboxConfig(
                    sandboxObj.getLong("max_storage"),
                    sandboxObj.getInt("max_processes"),
                    sandboxObj.getLong("max_memory")
                );
            }
            
            // 收集文件列表
            mpk.fileList.clear();
            zipFile.stream().forEach(entry -> mpk.fileList.add(entry.getName()));
        }
        
        return mpk;
    }
    
    /**
     * 获取清单文件内容
     * @return 清单文件JSON对象
     */
    public JSONObject getManifest() {
        return manifest;
    }
    
    /**
     * 获取格式版本
     * @return 格式版本
     */
    public String getFormatVersion() {
        return formatVersion;
    }
    
    /**
     * 获取应用ID
     * @return 应用ID
     */
    public String getId() {
        return id;
    }
    
    /**
     * 获取应用名称
     * @return 应用名称
     */
    public String getName() {
        return name;
    }
    
    /**
     * 获取版本信息
     * @return 版本信息
     */
    public Version getVersion() {
        return version;
    }
    
    /**
     * 获取目标平台
     * @return 目标平台
     */
    public String getPlatform() {
        return platform;
    }
    
    /**
     * 获取最低平台版本要求
     * @return 最低平台版本
     */
    public String getMinPlatformVersion() {
        return minPlatformVersion;
    }
    
    /**
     * 获取应用描述
     * @return 应用描述
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * 获取作者信息
     * @return 作者信息
     */
    public Author getAuthor() {
        return author;
    }
    
    /**
     * 获取图标路径
     * @return 图标路径
     */
    public String getIconPath() {
        return iconPath;
    }
    
    /**
     * 获取启动画面路径
     * @return 启动画面路径
     */
    public String getSplashPath() {
        return splashPath;
    }
    
    /**
     * 获取权限列表
     * @return 权限列表
     */
    public List<String> getPermissions() {
        return permissions;
    }
    
    /**
     * 获取依赖列表
     * @return 依赖列表
     */
    public List<Dependency> getDependencies() {
        return dependencies;
    }
    
    /**
     * 获取沙箱配置
     * @return 沙箱配置
     */
    public SandboxConfig getSandbox() {
        return sandbox;
    }
    
    /**
     * 获取文件列表
     * @return 文件列表
     */
    public List<String> getFileList() {
        return fileList;
    }
    
    /**
     * 获取文件路径
     * @return 文件路径
     */
    public String getFilePath() {
        return filePath;
    }
    
    /**
     * 获取应用图标
     * @return 应用图标
     */
    public Drawable getIcon() {
        return icon;
    }
    
    /**
     * 设置应用图标
     * @param icon 应用图标
     */
    public void setIcon(Drawable icon) {
        this.icon = icon;
    }
    
    /**
     * 获取附加元数据
     * @return 附加元数据
     */
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    /**
     * 设置附加元数据
     * @param key 键
     * @param value 值
     */
    public void setMetadata(String key, Object value) {
        this.metadata.put(key, value);
    }
} 