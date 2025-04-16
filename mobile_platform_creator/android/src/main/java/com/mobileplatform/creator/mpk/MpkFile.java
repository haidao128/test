package com.mobileplatform.creator.mpk;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.stream.Collectors;

/**
 * MPK 文件模型 (v2.1)
 * 用于表示和解析基于标准 ZIP 格式的 .mpk 文件。
 * 此类直接处理 ZIP 文件，不再依赖 MpkParser。
 */
public class MpkFile {
    private static final String TAG = "MpkFile";
    private static final String MPK_FORMAT_VERSION = "2.1"; // 支持的格式版本

    // manifest.json 中必须包含的字段 (规范 v2.1)
    private static final Set<String> REQUIRED_MANIFEST_FIELDS = new HashSet<>(Arrays.asList(
            "format_version", "id", "name", "version", "platform",
            "min_platform_version", "code_type", "entry_point"
    ));

    // 清单文件内容
    private JSONObject manifest;

    // 从清单中解析的关键信息
    private String formatVersion;
    private String id;
    private String name;
    private String version; // 版本名称
    private int versionCode = -1; // 版本号 (可选)
    private String platform;
    private String minPlatformVersion;
    private String codeType;
    private String entryPoint; // 相对于包根目录的路径
    private List<String> permissions = new ArrayList<>();
    private String description;
    private JSONObject author;
    private String iconPath; // 相对于包根目录的路径
    private String splashPath; // 相对于包根目录的路径

    // 文件路径
    private String filePath;

    // MPK 包内的文件列表 (相对路径)
    private List<String> fileList;

    // 内部 ZipFile 引用，用于按需读取文件内容
    private transient ZipFile zipFile; // transient 防止序列化

    // 私有构造函数，防止直接实例化
    private MpkFile() {}

    /**
     * 从文件加载并解析 MPK 文件 (标准 ZIP 格式)。
     *
     * @param file MPK 文件 (.zip 或 .mpk)
     * @return 解析后的 MPK 文件对象
     * @throws IOException 如果文件读取或 ZIP 处理失败
     * @throws MpkException 如果文件格式无效或不符合规范
     */
    public static MpkFile fromFile(File file) throws IOException, MpkException {
        if (file == null || !file.exists() || !file.isFile()) {
            throw new MpkException("无效的文件路径或文件不存在: " + (file != null ? file.getPath() : "null"));
        }

        MpkFile mpkFile = new MpkFile();
        mpkFile.filePath = file.getAbsolutePath();

        try {
            mpkFile.zipFile = new ZipFile(file);

            // 获取文件列表
            mpkFile.fileList = mpkFile.zipFile.stream()
                                       .map(ZipEntry::getName)
                                       .collect(Collectors.toList());

            // 1. 检查 manifest.json 是否存在
            ZipEntry manifestEntry = mpkFile.zipFile.getEntry("manifest.json");
            if (manifestEntry == null) {
                mpkFile.close(); // 关闭 ZipFile
                throw new MpkException("MPK包缺少 manifest.json 文件");
            }

            // 2. 读取并解析 manifest.json
            try (InputStream manifestStream = mpkFile.zipFile.getInputStream(manifestEntry)) {
                String manifestJson = readStreamToString(manifestStream);
                mpkFile.manifest = new JSONObject(manifestJson);
            } catch (JSONException e) {
                mpkFile.close();
                throw new MpkException("解析 manifest.json 失败: " + e.getMessage(), e);
            } catch (IOException e) {
                mpkFile.close();
                throw new MpkException("读取 manifest.json 失败: " + e.getMessage(), e);
            }

            // 3. 验证 manifest.json 内容
            validateManifest(mpkFile);

            // 4. 检查代码入口点文件是否存在
            if (mpkFile.zipFile.getEntry(mpkFile.entryPoint) == null) {
                mpkFile.close();
                throw new MpkException("清单指定的入口点文件不存在: " + mpkFile.entryPoint);
            }

            // 5. 检查 code/ 目录是否存在且包含文件 (根据规范 code/ 是必需的)
            boolean hasCodeFiles = mpkFile.fileList.stream().anyMatch(name -> name.startsWith("code/") && !name.endsWith("/"));
            if (!hasCodeFiles) {
                 // 也可以检查入口点是否在 code/ 下
                 if (!mpkFile.entryPoint.startsWith("code/")) {
                    Log.w(TAG, "MPK包缺少 code/ 目录或该目录为空，但入口点不在 code/ 下。");
                 } else {
                    mpkFile.close();
                    throw new MpkException("MPK包缺少 code/ 目录或该目录为空");
                 }
            }


            // 6. 检查 signature.sig 是否存在
            if (mpkFile.zipFile.getEntry("signature.sig") == null) {
                Log.w(TAG, "MPK包缺少 signature.sig 文件，无法进行签名验证。");
                // 不抛出异常，但后续验证会失败
            }

            // 解析成功，保持 zipFile 打开状态以便后续读取文件内容
            return mpkFile;

        } catch (IOException e) {
            mpkFile.close(); // 确保在发生 IO 错误时关闭 ZipFile
            throw new MpkException("打开或处理 MPK (ZIP) 文件失败: " + e.getMessage(), e);
        } catch (MpkException e) {
             mpkFile.close(); // 确保在发生 MPK 格式错误时关闭 ZipFile
             throw e; // 重新抛出 MpkException
        } catch (Exception e) {
             mpkFile.close(); // 捕获其他意外错误
             throw new MpkException("解析 MPK 文件时发生意外错误: " + e.getMessage(), e);
        }
    }

    /**
     * 验证解析后的 manifest 内容。
     *
     * @param mpkFile 包含 manifest 的 MpkFile 实例
     * @throws MpkException 如果验证失败
     */
    private static void validateManifest(MpkFile mpkFile) throws MpkException {
        JSONObject manifest = mpkFile.manifest;

        // 检查必需字段
        Set<String> missingFields = new HashSet<>(REQUIRED_MANIFEST_FIELDS);
        Iterator<String> keys = manifest.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            missingFields.remove(key);
        }
        if (!missingFields.isEmpty()) {
            throw new MpkException("manifest.json 缺少必需字段: " + String.join(", ", missingFields));
        }

        try {
            // 检查并存储字段值
            mpkFile.formatVersion = manifest.getString("format_version");
            mpkFile.id = manifest.getString("id");
            mpkFile.name = manifest.getString("name");
            mpkFile.version = manifest.getString("version"); // 版本名称
            mpkFile.platform = manifest.getString("platform");
            mpkFile.minPlatformVersion = manifest.getString("min_platform_version");
            mpkFile.codeType = manifest.getString("code_type");
            mpkFile.entryPoint = manifest.getString("entry_point").replace('\\', '/').replaceAll("^/+", ""); // 规范化路径

            // 检查格式版本
            if (!MPK_FORMAT_VERSION.equals(mpkFile.formatVersion)) {
                Log.w(TAG, "MPK包格式版本 (" + mpkFile.formatVersion + ") 与当前支持的版本 (" + MPK_FORMAT_VERSION + ") 不匹配。");
                // 根据兼容性策略决定是否抛出异常
                // throw new MpkException("不支持的 MPK 格式版本: " + mpkFile.formatVersion);
            }

            // 检查 entry_point 是否为空
            if (mpkFile.entryPoint.isEmpty()) {
                 throw new MpkException("manifest.json 中的 entry_point 不能为空");
            }

            // 解析可选字段
            if (manifest.has("version_code")) {
                mpkFile.versionCode = manifest.getInt("version_code");
            }
            if (manifest.has("description")) {
                mpkFile.description = manifest.getString("description");
            }
            if (manifest.has("author")) {
                mpkFile.author = manifest.getJSONObject("author");
            }
            if (manifest.has("icon")) {
                mpkFile.iconPath = manifest.getString("icon").replace('\\', '/').replaceAll("^/+", "");
            }
             if (manifest.has("splash")) {
                mpkFile.splashPath = manifest.getString("splash").replace('\\', '/').replaceAll("^/+", "");
            }
            if (manifest.has("permissions")) {
                JSONArray perms = manifest.getJSONArray("permissions");
                for (int i = 0; i < perms.length(); i++) {
                    mpkFile.permissions.add(perms.getString(i));
                }
            }

        } catch (JSONException e) {
            throw new MpkException("解析 manifest.json 字段失败: " + e.getMessage(), e);
        }
    }

    /**
     * 读取 ZipEntry 的内容作为字节数组。
     *
     * @param relativePath 文件在 ZIP 中的相对路径 (使用 '/')
     * @return 文件的字节数组，如果文件不存在或读取失败则返回 null
     */
    public byte[] readFileBytes(String relativePath) {
        if (zipFile == null) {
            Log.e(TAG, "无法读取文件，MPK (ZIP) 文件未打开或已关闭。");
            return null;
        }
        String normalizedPath = relativePath.replace('\\', '/').replaceAll("^/+", "");
        ZipEntry entry = zipFile.getEntry(normalizedPath);
        if (entry == null) {
            Log.w(TAG, "文件在 MPK 包中不存在: " + normalizedPath);
            return null;
        }
        if (entry.isDirectory()) {
             Log.w(TAG, "尝试读取目录作为文件: " + normalizedPath);
             return null;
        }

        try (InputStream inputStream = zipFile.getInputStream(entry)) {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            int nRead;
            byte[] data = new byte[16384]; // 16KB buffer
            while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            buffer.flush();
            return buffer.toByteArray();
        } catch (IOException e) {
            Log.e(TAG, "读取 MPK 包内文件失败: " + normalizedPath, e);
            return null;
        }
    }

    /**
     * 获取指定 ZipEntry 的输入流。调用者负责关闭流。
     *
     * @param relativePath 文件在 ZIP 中的相对路径 (使用 '/')
     * @return 文件的 InputStream，如果文件不存在或打开失败则返回 null
     */
    public InputStream getFileInputStream(String relativePath) {
        if (zipFile == null) {
            Log.e(TAG, "无法获取文件流，MPK (ZIP) 文件未打开或已关闭。");
            return null;
        }
        String normalizedPath = relativePath.replace('\\', '/').replaceAll("^/+", "");
        ZipEntry entry = zipFile.getEntry(normalizedPath);
        if (entry == null) {
             Log.w(TAG, "文件在 MPK 包中不存在: " + normalizedPath);
            return null;
        }
         if (entry.isDirectory()) {
             Log.w(TAG, "尝试获取目录的输入流: " + normalizedPath);
             return null;
        }

        try {
            return zipFile.getInputStream(entry);
        } catch (IOException e) {
             Log.e(TAG, "打开 MPK 包内文件流失败: " + normalizedPath, e);
            return null;
        }
    }


    /**
     * 关闭底层的 ZipFile。在完成 MPK 文件操作后应调用此方法。
     */
    public void close() {
        if (zipFile != null) {
            try {
                zipFile.close();
                zipFile = null; // 标记为已关闭
                Log.d(TAG, "MPK (ZIP) 文件已关闭: " + filePath);
            } catch (IOException e) {
                Log.e(TAG, "关闭 MPK (ZIP) 文件失败: " + filePath, e);
            }
        }
    }

    // --- Getter 方法 ---

    /**
     * 获取清单文件内容的 JSON 对象副本。
     * @return 清单文件 JSON 对象，如果解析失败则返回 null
     */
    public JSONObject getManifest() {
        // 返回副本以防止外部修改
        if (manifest == null) return null;
        try {
            return new JSONObject(manifest.toString());
        } catch (JSONException e) {
            Log.e(TAG, "复制 manifest 时出错", e);
            return null; // 不太可能发生
        }
    }

    /** 获取 MPK 格式版本号 */
    public String getFormatVersion() { return formatVersion; }
    /** 获取应用 ID */
    public String getId() { return id; }
    /** 获取应用名称 */
    public String getName() { return name; }
    /** 获取应用版本名称 (字符串) */
    public String getVersion() { return version; }
    /** 获取应用版本号 (整数，如果未定义则为 -1) */
    public int getVersionCode() { return versionCode; }
    /** 获取目标平台 */
    public String getPlatform() { return platform; }
    /** 获取最低平台版本要求 */
    public String getMinPlatformVersion() { return minPlatformVersion; }
    /** 获取代码类型 */
    public String getCodeType() { return codeType; }
    /** 获取应用入口点路径 (相对于包根目录) */
    public String getEntryPoint() { return entryPoint; }
    /** 获取应用描述 */
    public String getDescription() { return description; }
    /** 获取作者信息 JSON 对象 */
    public JSONObject getAuthor() { return author; } // 可能为 null
    /** 获取应用图标路径 (相对于包根目录) */
    public String getIconPath() { return iconPath; } // 可能为 null
     /** 获取启动画面路径 (相对于包根目录) */
    public String getSplashPath() { return splashPath; } // 可能为 null
    /** 获取权限列表 */
    public List<String> getPermissions() { return new ArrayList<>(permissions); } // 返回副本
    /** 获取原始 MPK 文件路径 */
    public String getFilePath() { return filePath; }
    /** 获取包内所有文件的相对路径列表 */
    public List<String> getFileList() { return new ArrayList<>(fileList); } // 返回副本

    /**
     * 读取代码入口点文件内容。
     * @return 入口点文件的字节数组，如果读取失败则返回 null
     */
    public byte[] getEntryPointCodeData() {
        if (entryPoint == null || entryPoint.isEmpty()) {
            Log.e(TAG, "无法读取入口点代码，清单中未定义 entry_point。");
            return null;
        }
        return readFileBytes(entryPoint);
    }

     /**
     * 读取签名文件 (signature.sig) 内容。
     * @return 签名字节数组，如果文件不存在或读取失败则返回 null
     */
    public byte[] getSignatureData() {
        return readFileBytes("signature.sig");
    }

    // --- 辅助方法 ---

    /**
     * 从输入流读取所有内容并转换为 UTF-8 字符串。
     * @param inputStream 输入流
     * @return 字符串内容
     * @throws IOException 如果读取失败
     */
    private static String readStreamToString(InputStream inputStream) throws IOException {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        while ((length = inputStream.read(buffer)) != -1) {
            result.write(buffer, 0, length);
        }
        return result.toString(StandardCharsets.UTF_8.name());
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            close(); // 确保在对象被垃圾回收时关闭 ZipFile
        } finally {
            super.finalize();
        }
    }
} 