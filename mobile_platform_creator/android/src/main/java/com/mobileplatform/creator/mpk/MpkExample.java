package com.mobileplatform.creator.mpk;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.ViewGroup;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * MPK 示例应用
 * 用于演示如何创建和解析 MPK 文件，以及沙箱资源限制功能
 */
public class MpkExample {
    private static final String TAG = "MpkExample";
    
    /**
     * 运行 MPK 处理示例
     * @param context 应用上下文
     */
    public static void runExample(Context context) {
        File cacheDir = context.getCacheDir();
        File exampleMpk = new File(cacheDir, "example_app.mpk");

        try {
            // 1. 创建一个示例 MPK 文件 (如果需要)
            //    注意：MpkBuilder 需要适配新的 ZIP 格式或被替换
            //    这里我们假设 example_app.mpk 已存在或通过其他方式创建
            // createExampleMpk(exampleMpk);
            if (!exampleMpk.exists()) {
                 Log.e(TAG, "示例 MPK 文件不存在，请先创建: " + exampleMpk.getPath());
                 // 可以在这里调用一个更新后的创建方法
                 // createExampleZipMpk(exampleMpk);
                 return;
            }

            // 2. 解析 MPK 文件
            Log.i(TAG, "开始解析 MPK 文件: " + exampleMpk.getPath());
            MpkFile mpk = null;
            try {
                mpk = MpkFile.fromFile(exampleMpk);
                Log.i(TAG, "MPK 文件解析成功。");

                // 3. 打印 MPK 文件信息
                printMpkInfo(mpk);

                // 4. 读取入口点代码
                byte[] entryPointCode = mpk.getEntryPointCodeData();
                if (entryPointCode != null) {
                    Log.i(TAG, "入口点代码 ('" + mpk.getEntryPoint() + "') 大小: " + entryPointCode.length + " 字节");
                    // 可以进一步处理代码，例如加载到运行时
                } else {
                    Log.e(TAG, "无法读取入口点代码。");
                }

                // 5. 读取图标文件 (如果存在)
                if (mpk.getIconPath() != null && !mpk.getIconPath().isEmpty()) {
                    byte[] iconData = mpk.readFileBytes(mpk.getIconPath());
                    if (iconData != null) {
                         Log.i(TAG, "图标文件 ('" + mpk.getIconPath() + "') 大小: " + iconData.length + " 字节");
                    } else {
                         Log.w(TAG, "无法读取图标文件: " + mpk.getIconPath());
                    }
                }
                
                // 6. 读取签名文件
                byte[] signatureData = mpk.getSignatureData();
                if (signatureData != null) {
                    String signatureContent = new String(signatureData, StandardCharsets.UTF_8).trim();
                     Log.i(TAG, "签名文件 (signature.sig) 内容 (前100字节): " + 
                         (signatureContent.length() > 100 ? signatureContent.substring(0, 100) + "..." : signatureContent));
                    // 在实际应用中，这里会进行签名验证
                    // boolean isValid = verifySignature(mpk, publicKey); 
                } else {
                     Log.w(TAG, "无法读取签名文件 (signature.sig)。");
                }

            } finally {
                // 7. 关闭 MPK 文件
                if (mpk != null) {
                    mpk.close();
                    Log.i(TAG, "MPK 文件已关闭。");
                }
            }

        } catch (MpkException | IOException e) {
            Log.e(TAG, "MPK 处理示例失败: " + e.getMessage(), e);
        }
    }

    /**
     * 打印 MPK 文件信息
     * @param mpk 已解析的 MpkFile 对象
     */
    public static void printMpkInfo(MpkFile mpk) {
        if (mpk == null) return;

        Log.i(TAG, "--- MPK 文件信息 ---");
        Log.i(TAG, "  文件路径: " + mpk.getFilePath());
        Log.i(TAG, "  格式版本: " + mpk.getFormatVersion());
        Log.i(TAG, "  文件列表 (部分): " + mpk.getFileList().stream().limit(5).collect(Collectors.joining(", ")) + (mpk.getFileList().size() > 5 ? "..." : ""));
        Log.i(TAG, "--- 应用清单信息 ---");
        Log.i(TAG, "  应用 ID: " + mpk.getId());
        Log.i(TAG, "  应用名称: " + mpk.getName());
        Log.i(TAG, "  版本名称: " + mpk.getVersion());
        if (mpk.getVersionCode() != -1) {
             Log.i(TAG, "  版本号: " + mpk.getVersionCode());
        }
        Log.i(TAG, "  目标平台: " + mpk.getPlatform());
        Log.i(TAG, "  最低平台版本: " + mpk.getMinPlatformVersion());
        Log.i(TAG, "  代码类型: " + mpk.getCodeType());
        Log.i(TAG, "  入口点: " + mpk.getEntryPoint());
        if (mpk.getDescription() != null) {
            Log.i(TAG, "  描述: " + mpk.getDescription());
        }
        if (mpk.getAuthor() != null) {
             Log.i(TAG, "  作者: " + mpk.getAuthor().toString());
        }
         if (mpk.getIconPath() != null) {
            Log.i(TAG, "  图标路径: " + mpk.getIconPath());
        }
         if (mpk.getSplashPath() != null) {
            Log.i(TAG, "  启动画面路径: " + mpk.getSplashPath());
        }
        if (!mpk.getPermissions().isEmpty()) {
             Log.i(TAG, "  权限: " + String.join(", ", mpk.getPermissions()));
        }
        Log.i(TAG, "--------------------");
    }
    
    /**
     * 创建一个示例MPK文件（压缩格式）
     * @param destinationPath 目标路径
     * @return 创建的MPK文件路径
     * @throws MpkException 如果创建过程出错
     */
    public static String createExampleZipMpk(String destinationPath) throws MpkException {
        File tempDir = null;
        File outputFile = null;
        
        try {
            // 创建临时目录
            tempDir = createTempDirectory("mpk_example");
            
            // 创建清单文件
            JSONObject manifest = new JSONObject();
            manifest.put("format_version", "1.0");
            manifest.put("id", "com.example.hello");
            manifest.put("name", "Hello World");
            manifest.put("version", "1.0.0");
            manifest.put("version_code", 1);
            manifest.put("platform", "android");
            manifest.put("min_platform_version", "1.0.0");
            manifest.put("code_type", "javascript");
            manifest.put("entry_point", "code/main.js");
            manifest.put("description", "A simple hello world application");
            manifest.put("icon", "assets/icon.png");
            
            // 将清单写入文件
            File manifestFile = new File(tempDir, "manifest.json");
            FileWriter writer = new FileWriter(manifestFile);
            writer.write(manifest.toString(2));
            writer.close();
            
            // 创建代码目录
            File codeDir = new File(tempDir, "code");
            codeDir.mkdir();
            
            // 创建示例JavaScript文件
            File mainJs = new File(codeDir, "main.js");
            writer = new FileWriter(mainJs);
            writer.write("console.log('Hello, MPK World!');");
            writer.close();
            
            // 创建资源目录
            File assetsDir = new File(tempDir, "assets");
            assetsDir.mkdir();
            
            // 创建一个空的图标文件
            File iconFile = new File(assetsDir, "icon.png");
            new FileOutputStream(iconFile).close();
            
            // 创建签名文件（未签名）
            File signatureFile = new File(tempDir, "signature.sig");
            writer = new FileWriter(signatureFile);
            writer.write("unsigned");
            writer.close();
            
            // 创建ZIP文件
            outputFile = new File(destinationPath);
            zipDirectory(tempDir, outputFile);
            
            return outputFile.getAbsolutePath();
            
        } catch (JSONException e) {
            // 删除不完整的文件
            if (outputFile != null && outputFile.exists()) {
                outputFile.delete();
            }
            throw new MpkException("创建示例MPK清单文件时出错: " + e.getMessage(), e);
        } catch (IOException e) {
            // 删除不完整的文件
            if (outputFile != null && outputFile.exists()) {
                outputFile.delete();
            }
            throw new MpkException("创建示例MPK文件时出错: " + e.getMessage(), e);
        } finally {
            // 清理临时目录
            if (tempDir != null && tempDir.exists()) {
                deleteDirectory(tempDir);
            }
        }
    }

    /**
     * 创建临时目录
     * @param prefix 目录前缀
     * @return 创建的临时目录
     * @throws IOException 如果创建失败
     */
    private static File createTempDirectory(String prefix) throws IOException {
        Path tempDirPath = Files.createTempDirectory(prefix);
        return tempDirPath.toFile();
    }

    /**
     * 删除目录及其所有内容
     * @param directory 要删除的目录
     * @return 是否成功删除
     */
    private static boolean deleteDirectory(File directory) {
        if (directory == null || !directory.exists()) {
            return false;
        }
        
        File[] contents = directory.listFiles();
        if (contents != null) {
            for (File file : contents) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        
        return directory.delete();
    }
    
    /**
     * 将目录打包为ZIP文件
     * @param sourceDir 源目录
     * @param destFile 目标ZIP文件
     * @throws IOException 如果打包过程出错
     */
    private static void zipDirectory(File sourceDir, File destFile) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(destFile);
             ZipOutputStream zos = new ZipOutputStream(fos)) {
            
            zipFiles(sourceDir, sourceDir, zos);
        }
    }
    
    /**
     * 递归压缩文件和目录到ZIP输出流
     * @param rootDir 根目录
     * @param sourceFile 当前源文件或目录
     * @param zos ZIP输出流
     * @throws IOException 如果压缩过程出错
     */
    private static void zipFiles(File rootDir, File sourceFile, ZipOutputStream zos) throws IOException {
        // 计算相对路径
        String relativePath = sourceFile.equals(rootDir) ? "" : 
                              sourceFile.getPath().substring(rootDir.getPath().length() + 1);
        
        if (sourceFile.isDirectory()) {
            // 确保目录路径以/结尾
            if (!relativePath.isEmpty() && !relativePath.endsWith("/")) {
                relativePath += "/";
                ZipEntry entry = new ZipEntry(relativePath);
                zos.putNextEntry(entry);
                zos.closeEntry();
            }
            
            // 递归处理子目录和文件
            File[] children = sourceFile.listFiles();
            if (children != null) {
                for (File child : children) {
                    zipFiles(rootDir, child, zos);
                }
            }
        } else if (sourceFile.isFile()) {
            // 添加文件
            byte[] buffer = new byte[8192];
            try (InputStream in = Files.newInputStream(sourceFile.toPath())) {
                ZipEntry entry = new ZipEntry(relativePath);
                zos.putNextEntry(entry);
                
                int len;
                while ((len = in.read(buffer)) > 0) {
                    zos.write(buffer, 0, len);
                }
                
                zos.closeEntry();
            }
        }
    }

    // --- 废弃的旧方法 --- 

    /**
     * @deprecated 旧的二进制格式创建方法，不再适用。
     */
    @Deprecated
    public static void createExampleMpk(File outputFile) throws IOException, MpkException {
        throw new UnsupportedOperationException("createExampleMpk 已过时，请使用基于 ZIP 的创建方式。");
    }

    /**
     * @deprecated 旧的二进制格式解析方法，不再适用。
     */
    @Deprecated
    public static void parseAndPrintMpk(File mpkFile) throws IOException, MpkException {
         throw new UnsupportedOperationException("parseAndPrintMpk 已过时，请使用 MpkFile.fromFile() 和新的 printMpkInfo()。");
    }

    /**
     * @deprecated 旧的二进制格式解析方法，不再适用。
     */
    @Deprecated
    public static MpkFile parseMpkFromStream(InputStream is) throws IOException, MpkException {
        throw new UnsupportedOperationException("parseMpkFromStream 已过时。");
    }

    /**
     * 创建一个简单的 MPK 文件
     * @param context 上下文
     * @param outputFile 输出文件
     * @throws IOException 如果创建失败
     * @throws MpkException 如果构建失败
     */
    public static void createSimpleMpk(Context context, File outputFile) throws IOException, MpkException {
        // 创建 MPK 构建器
        MpkBuilder builder = new MpkBuilder();
        
        // 设置应用信息
        JSONObject manifest = new JSONObject();
        try {
            manifest.put("format_version", "2.1");
            manifest.put("id", "com.example.simple");
            manifest.put("name", "示例应用");
            manifest.put("version", "1.0.0");
            manifest.put("version_code", 1);
            manifest.put("platform", "all");
            manifest.put("min_platform_version", "1.0.0");
            manifest.put("code_type", "javascript");
            manifest.put("entry_point", "main.js");
            manifest.put("description", "这是一个简单的示例应用");
            manifest.put("icon", "res/icon.png");
            manifest.put("splash", "res/splash.png");
            
            JSONObject author = new JSONObject();
            author.put("name", "张三");
            author.put("email", "zhangsan@example.com");
            manifest.put("author", author);
            
            JSONArray permissions = new JSONArray();
            permissions.put("network");
            permissions.put("storage");
            manifest.put("permissions", permissions);
            
            builder.setManifest(manifest);
        } catch (JSONException e) {
            throw new MpkException("创建 manifest JSON 失败", e);
        }
        
        // 设置代码数据
        String code = "console.log('Hello, MPK!');";
        builder.setCode("javascript", "main.js", code.getBytes("UTF-8"));
        
        // 设置资源数据
        byte[] resources = createSimpleResources();
        builder.setResources(resources);
        
        // 设置签名数据（这里使用简单的测试签名）
        byte[] signature = createTestSignature();
        byte[] certificate = createTestCertificate();
        builder.setSignature(signature, certificate);
        
        // 构建 MPK 文件
        builder.build(outputFile);
        
        Log.i(TAG, "MPK 文件创建成功: " + outputFile.getAbsolutePath());
    }
    
    /**
     * 创建简单的资源数据
     * @return 资源数据
     * @throws IOException 如果创建失败
     */
    private static byte[] createSimpleResources() throws IOException {
        // 这里简单地创建一个包含文本文件的资源
        String content = "这是一个简单的资源文件。";
        return content.getBytes("UTF-8");
    }
    
    /**
     * 创建测试签名
     * @return 签名数据
     */
    private static byte[] createTestSignature() {
        // 这里简单地创建一个测试签名
        return "TEST_SIGNATURE".getBytes();
    }
    
    /**
     * 创建测试证书
     * @return 证书数据
     */
    private static byte[] createTestCertificate() {
        // 这里简单地创建一个测试证书
        return "TEST_CERTIFICATE".getBytes();
    }
    
    /**
     * 从资源文件加载 MPK 文件
     * @param context 上下文
     * @param resourceId 资源 ID
     * @return MPK 文件对象
     * @throws IOException 如果加载失败
     * @throws MpkException 如果文件格式错误
     */
    public static MpkFile loadMpkFromResource(Context context, int resourceId) throws IOException, MpkException {
        try (InputStream is = context.getResources().openRawResource(resourceId)) {
            return MpkParser.fromInputStream(is);
        }
    }
    
    /**
     * 将 MPK 文件保存到内部存储
     * @param context 上下文
     * @param mpkFile MPK 文件对象
     * @param fileName 文件名
     * @return 保存的文件
     * @throws IOException 如果保存失败
     */
    public static File saveMpkToInternalStorage(Context context, MpkFile mpkFile, String fileName) throws IOException {
        File outputDir = new File(context.getFilesDir(), "mpk");
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }
        
        File outputFile = new File(outputDir, fileName);
        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            // 这里需要实现将 MpkFile 对象写入文件的功能
            // 由于 MpkFile 类目前没有提供序列化方法，这里只是一个示例
            fos.write("MPK1".getBytes());
            fos.write(new byte[20]); // 占位符
            
            Log.i(TAG, "MPK 文件保存成功: " + outputFile.getAbsolutePath());
            return outputFile;
        }
    }
    
    /**
     * 计算数据的 SHA-256 哈希值
     * @param data 数据
     * @return 哈希值
     * @throws NoSuchAlgorithmException 如果算法不可用
     */
    public static byte[] calculateSha256(byte[] data) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return digest.digest(data);
    }
    
    /**
     * 创建一个带有自定义沙箱配置的 MPK 文件
     * @param context 上下文
     * @param outputFile 输出文件
     * @throws IOException 如果创建失败
     * @throws JSONException 如果 JSON 构建失败
     * @throws MpkException 如果 MPK 文件创建失败
     */
    public static void createSandboxTestMpk(Context context, File outputFile) throws IOException, JSONException, MpkException {
        // 创建清单文件
        JSONObject manifest = new JSONObject();
        manifest.put("format_version", "2.0");
        manifest.put("id", "com.example.sandboxtest");
        manifest.put("name", "沙箱测试应用");
        
        // 版本信息
        JSONObject version = new JSONObject();
        version.put("name", "1.0.0");
        version.put("code", 1);
        manifest.put("version", version);
        
        manifest.put("platform", "android");
        manifest.put("min_platform_version", "1.0.0");
        manifest.put("code_type", "javascript");
        manifest.put("entry_point", "main.js");
        
        // 作者信息
        manifest.put("author", "沙箱测试团队");
        manifest.put("description", "用于测试沙箱资源限制功能的示例应用");
        
        // 权限
        JSONArray permissions = new JSONArray();
        permissions.put("INTERNET");
        permissions.put("READ_EXTERNAL_STORAGE");
        manifest.put("permissions", permissions);
        
        // 自定义沙箱配置
        JSONObject sandbox = new JSONObject();
        sandbox.put("max_storage", 50 * 1024 * 1024); // 50MB
        sandbox.put("max_processes", 3);
        sandbox.put("max_memory", 128 * 1024 * 1024); // 128MB
        sandbox.put("max_cpu_usage", 40); // 40%
        sandbox.put("max_network_usage", 5 * 1024 * 1024); // 5MB
        sandbox.put("monitor_interval", 2000); // 2秒
        manifest.put("sandbox", sandbox);
        
        // 创建 ZIP 文件
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(outputFile))) {
            // 添加清单文件
            zos.putNextEntry(new ZipEntry("manifest.json"));
            zos.write(manifest.toString(4).getBytes("UTF-8"));
            zos.closeEntry();
            
            // 添加主 JavaScript 文件
            zos.putNextEntry(new ZipEntry("code/main.js"));
            String mainJs = "// 沙箱测试应用\n" +
                    "console.log('沙箱测试应用启动');\n" +
                    "\n" +
                    "// 创建 UI\n" +
                    "document.body.innerHTML = `\n" +
                    "    <h1>沙箱测试应用</h1>\n" +
                    "    <div id=\"status\">状态：正在运行</div>\n" +
                    "    <div>\n" +
                    "        <button id=\"testStorage\">测试存储空间</button>\n" +
                    "        <button id=\"testMemory\">测试内存使用</button>\n" +
                    "        <button id=\"testCpu\">测试 CPU 使用</button>\n" +
                    "        <button id=\"testNetwork\">测试网络使用</button>\n" +
                    "    </div>\n" +
                    "    <div id=\"result\"></div>\n" +
                    "`;\n" +
                    "\n" +
                    "// 获取沙箱配置\n" +
                    "const sandboxConfig = JSON.parse(mpk.getManifest()).sandbox;\n" +
                    "console.log('沙箱配置：', sandboxConfig);\n" +
                    "\n" +
                    "// 添加测试按钮事件\n" +
                    "document.getElementById('testStorage').addEventListener('click', function() {\n" +
                    "    testStorage();\n" +
                    "});\n" +
                    "\n" +
                    "document.getElementById('testMemory').addEventListener('click', function() {\n" +
                    "    testMemory();\n" +
                    "});\n" +
                    "\n" +
                    "document.getElementById('testCpu').addEventListener('click', function() {\n" +
                    "    testCpu();\n" +
                    "});\n" +
                    "\n" +
                    "document.getElementById('testNetwork').addEventListener('click', function() {\n" +
                    "    testNetwork();\n" +
                    "});\n" +
                    "\n" +
                    "// 测试存储空间\n" +
                    "function testStorage() {\n" +
                    "    const result = document.getElementById('result');\n" +
                    "    result.innerHTML = '正在测试存储空间限制...';\n" +
                    "    \n" +
                    "    // 获取数据路径\n" +
                    "    const dataPath = mpk.getDataPath();\n" +
                    "    console.log('数据路径：', dataPath);\n" +
                    "    \n" +
                    "    result.innerHTML += '<br>数据路径：' + dataPath;\n" +
                    "}\n" +
                    "\n" +
                    "// 测试内存使用\n" +
                    "function testMemory() {\n" +
                    "    const result = document.getElementById('result');\n" +
                    "    result.innerHTML = '正在测试内存限制...';\n" +
                    "    \n" +
                    "    // 创建大型数组占用内存\n" +
                    "    const memoryData = [];\n" +
                    "    \n" +
                    "    function allocateMemory() {\n" +
                    "        // 每次分配 10MB 内存\n" +
                    "        const tenMB = 10 * 1024 * 1024;\n" +
                    "        const chunk = new Array(tenMB);\n" +
                    "        for (let i = 0; i < tenMB; i++) {\n" +
                    "            chunk[i] = i;\n" +
                    "        }\n" +
                    "        memoryData.push(chunk);\n" +
                    "        \n" +
                    "        const allocated = memoryData.length * 10;\n" +
                    "        result.innerHTML = `已分配内存：${allocated} MB`;\n" +
                    "        \n" +
                    "        if (memoryData.length < 20) { // 最多分配 200MB\n" +
                    "            setTimeout(allocateMemory, 1000);\n" +
                    "        }\n" +
                    "    }\n" +
                    "    \n" +
                    "    allocateMemory();\n" +
                    "}\n" +
                    "\n" +
                    "// 测试 CPU 使用\n" +
                    "function testCpu() {\n" +
                    "    const result = document.getElementById('result');\n" +
                    "    result.innerHTML = '正在测试 CPU 限制...';\n" +
                    "    \n" +
                    "    let counter = 0;\n" +
                    "    const startTime = Date.now();\n" +
                    "    \n" +
                    "    function heavyCalculation() {\n" +
                    "        // 执行密集计算\n" +
                    "        for (let i = 0; i < 10000000; i++) {\n" +
                    "            counter++;\n" +
                    "        }\n" +
                    "        \n" +
                    "        const elapsed = Date.now() - startTime;\n" +
                    "        result.innerHTML = `已运行：${elapsed / 1000} 秒，计数：${counter}`;\n" +
                    "        \n" +
                    "        setTimeout(heavyCalculation, 10);\n" +
                    "    }\n" +
                    "    \n" +
                    "    heavyCalculation();\n" +
                    "}\n" +
                    "\n" +
                    "// 测试网络使用\n" +
                    "function testNetwork() {\n" +
                    "    const result = document.getElementById('result');\n" +
                    "    result.innerHTML = '正在测试网络限制...';\n" +
                    "    \n" +
                    "    let downloadCount = 0;\n" +
                    "    \n" +
                    "    function downloadData() {\n" +
                    "        // 下载大文件\n" +
                    "        fetch('https://picsum.photos/1024/1024')\n" +
                    "            .then(response => response.blob())\n" +
                    "            .then(blob => {\n" +
                    "                downloadCount++;\n" +
                    "                result.innerHTML = '已下载：' + downloadCount + ' 个文件，约 ' + downloadCount + ' MB';\n" +
                    "                \n" +
                    "                if (downloadCount < 10) { // 最多下载 10 个文件\n" +
                    "                    setTimeout(downloadData, 1000);\n" +
                    "                }\n" +
                    "            })\n" +
                    "            .catch(error => {\n" +
                    "                result.innerHTML = '下载出错：' + error.message;\n" +
                    "            });\n" +
                    "    }\n" +
                    "    \n" +
                    "    downloadData();\n" +
                    "}\n";
            zos.write(mainJs.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
            
            // 添加示例资源文件 assets/icon.png (空文件示意)
            zos.putNextEntry(new ZipEntry("assets/icon.png"));
            // 写入空内容或实际图片数据
            zos.closeEntry();
            
            // 添加示例签名文件 signature.sig (标记为未签名)
            zos.putNextEntry(new ZipEntry("signature.sig"));
            zos.write("UNSIGNED".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            Log.i(TAG, "示例 MPK 文件创建完成。");

        } catch (IOException | JSONException e) {
            Log.e(TAG, "创建示例 MPK 文件失败", e);
             // 删除可能不完整的文件
            if (outputFile.exists()) {
                outputFile.delete();
            }
            if (e instanceof IOException) throw (IOException) e;
            if (e instanceof JSONException) throw new MpkException("JSON错误", e);
        }
    }
}