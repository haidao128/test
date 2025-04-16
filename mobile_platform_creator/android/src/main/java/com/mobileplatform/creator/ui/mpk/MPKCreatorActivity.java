package com.mobileplatform.creator.ui.mpk;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.mobileplatform.creator.R;
import com.mobileplatform.creator.mpk.MpkException;
import com.mobileplatform.creator.mpk.MpkFile;
import com.mobileplatform.creator.mpk.MpkRuntime;
import com.mobileplatform.creator.util.FileUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class MPKCreatorActivity extends AppCompatActivity {
    private static final String TAG = "MPKCreatorActivity";
    private static final int REQUEST_STORAGE_PERMISSION = 100;
    private static final int REQUEST_PICK_ENTRY_FILE = 101;
    private static final int REQUEST_PICK_ICON_FILE = 102;
    private static final int REQUEST_PICK_RESOURCE_FILE = 103;
    
    // UI组件
    private TextInputEditText etAppId, etAppName, etVersionName, etVersionCode, etDescription;
    private RadioGroup rgCodeType;
    private TextView tvEntryFile, tvIconFile, tvResourcesCount;
    private Button btnSelectEntry, btnSelectIcon, btnManageResources;
    private CheckBox cbPermissionInternet, cbPermissionStorage, cbPermissionLocation;
    private Button btnMorePermissions, btnCancel, btnCreate;
    
    // 数据
    private String entryFilePath = null;
    private String iconFilePath = null;
    private Map<String, String> resourceFiles = new HashMap<>();
    private List<String> permissions = new ArrayList<>();
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mpk_creator);
        setTitle("创建 MPK 包");
        
        // 启用返回按钮
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        
        // 初始化UI组件
        initViews();
        
        // 设置事件监听器
        setupListeners();
        
        // 初始化权限列表
        initPermissionsList();
    }
    
    private void initViews() {
        // 基本信息字段
        etAppId = findViewById(R.id.et_app_id);
        etAppName = findViewById(R.id.et_app_name);
        etVersionName = findViewById(R.id.et_version_name);
        etVersionCode = findViewById(R.id.et_version_code);
        etDescription = findViewById(R.id.et_description);
        
        // 代码类型和资源区域
        rgCodeType = findViewById(R.id.rg_code_type);
        tvEntryFile = findViewById(R.id.tv_entry_file);
        tvIconFile = findViewById(R.id.tv_icon_file);
        tvResourcesCount = findViewById(R.id.tv_resources_count);
        btnSelectEntry = findViewById(R.id.btn_select_entry);
        btnSelectIcon = findViewById(R.id.btn_select_icon);
        btnManageResources = findViewById(R.id.btn_manage_resources);
        
        // 权限区域
        cbPermissionInternet = findViewById(R.id.cb_permission_internet);
        cbPermissionStorage = findViewById(R.id.cb_permission_storage);
        cbPermissionLocation = findViewById(R.id.cb_permission_location);
        btnMorePermissions = findViewById(R.id.btn_more_permissions);
        
        // 按钮区域
        btnCancel = findViewById(R.id.btn_cancel);
        btnCreate = findViewById(R.id.btn_create);
        
        // 设置默认值
        etVersionName.setText("1.0.0");
        etVersionCode.setText("1");
    }
    
    private void setupListeners() {
        // 入口文件选择按钮
        btnSelectEntry.setOnClickListener(v -> {
            checkPermissionAndPickFile(REQUEST_PICK_ENTRY_FILE, "选择入口文件");
        });
        
        // 图标文件选择按钮
        btnSelectIcon.setOnClickListener(v -> {
            checkPermissionAndPickFile(REQUEST_PICK_ICON_FILE, "选择图标文件");
        });
        
        // 资源文件管理按钮
        btnManageResources.setOnClickListener(v -> {
            showResourcesDialog();
        });
        
        // 更多权限按钮
        btnMorePermissions.setOnClickListener(v -> {
            showMorePermissionsDialog();
        });
        
        // 取消按钮
        btnCancel.setOnClickListener(v -> finish());
        
        // 创建MPK按钮
        btnCreate.setOnClickListener(v -> {
            if (validateInputs()) {
                createMpkPackage();
            }
        });
    }
    
    private void initPermissionsList() {
        // 添加默认权限
        if (cbPermissionInternet.isChecked()) {
            permissions.add("android.permission.INTERNET");
        }
        if (cbPermissionStorage.isChecked()) {
            permissions.add("android.permission.READ_EXTERNAL_STORAGE");
            permissions.add("android.permission.WRITE_EXTERNAL_STORAGE");
        }
        if (cbPermissionLocation.isChecked()) {
            permissions.add("android.permission.ACCESS_FINE_LOCATION");
            permissions.add("android.permission.ACCESS_COARSE_LOCATION");
        }
        
        // 监听权限复选框变化
        cbPermissionInternet.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if (!permissions.contains("android.permission.INTERNET")) {
                    permissions.add("android.permission.INTERNET");
                }
            } else {
                permissions.remove("android.permission.INTERNET");
            }
        });
        
        cbPermissionStorage.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if (!permissions.contains("android.permission.READ_EXTERNAL_STORAGE")) {
                    permissions.add("android.permission.READ_EXTERNAL_STORAGE");
                }
                if (!permissions.contains("android.permission.WRITE_EXTERNAL_STORAGE")) {
                    permissions.add("android.permission.WRITE_EXTERNAL_STORAGE");
                }
            } else {
                permissions.remove("android.permission.READ_EXTERNAL_STORAGE");
                permissions.remove("android.permission.WRITE_EXTERNAL_STORAGE");
            }
        });
        
        cbPermissionLocation.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if (!permissions.contains("android.permission.ACCESS_FINE_LOCATION")) {
                    permissions.add("android.permission.ACCESS_FINE_LOCATION");
                }
                if (!permissions.contains("android.permission.ACCESS_COARSE_LOCATION")) {
                    permissions.add("android.permission.ACCESS_COARSE_LOCATION");
                }
            } else {
                permissions.remove("android.permission.ACCESS_FINE_LOCATION");
                permissions.remove("android.permission.ACCESS_COARSE_LOCATION");
            }
        });
    }
    
    private void checkPermissionAndPickFile(int requestCode, String title) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQUEST_STORAGE_PERMISSION);
                return;
            }
        }
        
        pickFile(requestCode, title);
    }
    
    private void pickFile(int requestCode, String title) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, title), requestCode);
    }
    
    private boolean validateInputs() {
        // 检查应用ID
        String appId = etAppId.getText().toString().trim();
        if (TextUtils.isEmpty(appId)) {
            etAppId.setError("应用ID不能为空");
            return false;
        }
        if (!appId.matches("^[a-z]+(\\.[a-z0-9]+)+$")) {
            etAppId.setError("应用ID格式不正确，应为小写字母和数字，如com.example.app");
            return false;
        }
        
        // 检查应用名称
        String appName = etAppName.getText().toString().trim();
        if (TextUtils.isEmpty(appName)) {
            etAppName.setError("应用名称不能为空");
            return false;
        }
        
        // 检查版本名称
        String versionName = etVersionName.getText().toString().trim();
        if (TextUtils.isEmpty(versionName)) {
            etVersionName.setError("版本名称不能为空");
            return false;
        }
        
        // 检查版本号
        String versionCodeStr = etVersionCode.getText().toString().trim();
        if (TextUtils.isEmpty(versionCodeStr)) {
            etVersionCode.setError("版本号不能为空");
            return false;
        }
        
        try {
            int versionCode = Integer.parseInt(versionCodeStr);
            if (versionCode <= 0) {
                etVersionCode.setError("版本号必须大于0");
                return false;
            }
        } catch (NumberFormatException e) {
            etVersionCode.setError("版本号必须是数字");
            return false;
        }
        
        // 检查入口文件
        if (TextUtils.isEmpty(entryFilePath)) {
            Toast.makeText(this, "请选择入口文件", Toast.LENGTH_SHORT).show();
            return false;
        }
        
        return true;
    }
    
    private void createMpkPackage() {
        File tempDir = null;
        File outputFile = null;
        
        try {
            // 创建临时目录
            tempDir = createTempDirectory("mpk_create");
            
            // 准备清单数据
            JSONObject manifest = new JSONObject();
            String appId = etAppId.getText().toString().trim();
            String appName = etAppName.getText().toString().trim();
            String versionName = etVersionName.getText().toString().trim();
            int versionCode = Integer.parseInt(etVersionCode.getText().toString().trim());
            String description = etDescription.getText().toString().trim();
            
            // 获取代码类型
            String codeType = "javascript";
            int selectedRadioButtonId = rgCodeType.getCheckedRadioButtonId();
            if (selectedRadioButtonId == R.id.rb_python) {
                codeType = "python";
            } else if (selectedRadioButtonId == R.id.rb_binary) {
                codeType = "binary";
            }
            
            // 设置相对路径
            String entryRelativePath = addFileToPackage(tempDir, entryFilePath, "code");
            String iconRelativePath = null;
            if (!TextUtils.isEmpty(iconFilePath)) {
                iconRelativePath = addFileToPackage(tempDir, iconFilePath, "assets");
            }
            
            // 添加清单内容
            manifest.put("format_version", "1.0");
            manifest.put("id", appId);
            manifest.put("name", appName);
            manifest.put("version", versionName);
            manifest.put("version_code", versionCode);
            manifest.put("platform", "android");
            manifest.put("min_platform_version", "1.0.0");
            manifest.put("code_type", codeType);
            manifest.put("entry_point", entryRelativePath);
            
            if (!TextUtils.isEmpty(description)) {
                manifest.put("description", description);
            }
            
            if (iconRelativePath != null) {
                manifest.put("icon", iconRelativePath);
            }
            
            // 添加权限
            if (!permissions.isEmpty()) {
                JSONArray permissionsArray = new JSONArray();
                for (String permission : permissions) {
                    permissionsArray.put(permission);
                }
                manifest.put("permissions", permissionsArray);
            }
            
            // 添加资源文件
            for (Map.Entry<String, String> entry : resourceFiles.entrySet()) {
                addFileToPackage(tempDir, entry.getValue(), "assets");
            }
            
            // 将清单写入文件
            File manifestFile = new File(tempDir, "manifest.json");
            try (FileWriter writer = new FileWriter(manifestFile)) {
                writer.write(manifest.toString(2));
            }
            
            // 创建签名文件（未签名）
            File signatureFile = new File(tempDir, "signature.sig");
            try (FileWriter writer = new FileWriter(signatureFile)) {
                writer.write("unsigned");
            }
            
            // 创建MPK文件
            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            outputFile = new File(downloadsDir, appId + "_" + versionName + ".mpk");
            
            // 打包目录为ZIP
            zipDirectory(tempDir, outputFile);
            
            // 显示成功消息
            showSuccessDialog(outputFile.getAbsolutePath());
            
            Log.i(TAG, "MPK包创建成功: " + outputFile.getAbsolutePath());
            
        } catch (IOException | JSONException e) {
            Log.e(TAG, "创建MPK包失败", e);
            Toast.makeText(this, "创建MPK包失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
            
            // 删除不完整的文件
            if (outputFile != null && outputFile.exists()) {
                outputFile.delete();
            }
        } finally {
            // 清理临时目录
            if (tempDir != null && tempDir.exists()) {
                FileUtils.deleteDirectory(tempDir);
            }
        }
    }
    
    private void showSuccessDialog(String mpkPath) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("创建成功")
                .setMessage("MPK包已创建成功:\n" + mpkPath)
                .setPositiveButton("安装", (dialog, which) -> {
                    installMpkPackage(mpkPath);
                })
                .setNegativeButton("关闭", (dialog, which) -> finish())
                .show();
    }
    
    private void installMpkPackage(String mpkPath) {
        try {
            // 获取MPK运行时
            MpkRuntime runtime = new MpkRuntime(this);
            
            // 解析MPK文件
            MpkFile mpk = MpkFile.fromFile(new File(mpkPath));
            
            // 安装应用
            String appId = runtime.loadApp(new File(mpkPath));
            
            Toast.makeText(this, "应用安装成功: " + mpk.getName(), Toast.LENGTH_SHORT).show();
            finish();
        } catch (MpkException | IOException e) {
            Log.e(TAG, "安装MPK包失败", e);
            Toast.makeText(this, "安装失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    private String addFileToPackage(File targetDir, String sourceFilePath, String subDir) throws IOException {
        File source = new File(sourceFilePath);
        
        // 创建子目录
        File subDirectory = new File(targetDir, subDir);
        if (!subDirectory.exists()) {
            subDirectory.mkdirs();
        }
        
        // 复制文件
        File target = new File(subDirectory, source.getName());
        FileUtils.copyFile(source, target);
        
        // 返回相对路径
        return subDir + "/" + source.getName();
    }
    
    private void showResourcesDialog() {
        // 创建一个对话框来查看当前资源列表并添加更多资源
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_resources, null);
        // TODO: 您需要创建dialog_resources.xml布局文件
        
        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle("管理资源文件")
                .setView(dialogView)
                .setPositiveButton("确定", null)
                .setNeutralButton("添加资源", (dialogInterface, i) -> {
                    checkPermissionAndPickFile(REQUEST_PICK_RESOURCE_FILE, "选择资源文件");
                })
                .create();
        
        // 在这里您可以添加RecyclerView列出所有资源
        // 这里只是简单的实现
        
        dialog.show();
    }
    
    private void showMorePermissionsDialog() {
        // 创建常用权限列表
        String[] commonPermissions = new String[]{
                "android.permission.CAMERA",
                "android.permission.RECORD_AUDIO",
                "android.permission.ACCESS_WIFI_STATE",
                "android.permission.CHANGE_WIFI_STATE",
                "android.permission.BLUETOOTH",
                "android.permission.BLUETOOTH_ADMIN",
                "android.permission.VIBRATE",
                "android.permission.WAKE_LOCK"
        };
        
        // 创建对话框来显示和选择更多权限
        boolean[] checkedItems = new boolean[commonPermissions.length];
        
        // 检查哪些权限已经选择
        for (int i = 0; i < commonPermissions.length; i++) {
            checkedItems[i] = permissions.contains(commonPermissions[i]);
        }
        
        new MaterialAlertDialogBuilder(this)
                .setTitle("选择权限")
                .setMultiChoiceItems(commonPermissions, checkedItems, (dialog, which, isChecked) -> {
                    String permission = commonPermissions[which];
                    if (isChecked) {
                        if (!permissions.contains(permission)) {
                            permissions.add(permission);
                        }
                    } else {
                        permissions.remove(permission);
                    }
                })
                .setPositiveButton("确定", null)
                .show();
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 权限已授予，继续操作
                Toast.makeText(this, "存储权限已授予", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "需要存储权限才能选择文件", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                try {
                    String filePath = FileUtils.getPathFromUri(this, uri);
                    
                    if (requestCode == REQUEST_PICK_ENTRY_FILE) {
                        entryFilePath = filePath;
                        tvEntryFile.setText(new File(filePath).getName());
                    } else if (requestCode == REQUEST_PICK_ICON_FILE) {
                        iconFilePath = filePath;
                        tvIconFile.setText(new File(filePath).getName());
                    } else if (requestCode == REQUEST_PICK_RESOURCE_FILE) {
                        File resourceFile = new File(filePath);
                        resourceFiles.put(resourceFile.getName(), filePath);
                        tvResourcesCount.setText(resourceFiles.size() + "个文件");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "处理选择的文件时出错", e);
                    Toast.makeText(this, "无法处理选择的文件", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    /**
     * 创建临时目录
     * @param prefix 目录前缀
     * @return 创建的临时目录
     * @throws IOException 如果创建失败
     */
    private File createTempDirectory(String prefix) throws IOException {
        Path tempDirPath = Files.createTempDirectory(prefix);
        return tempDirPath.toFile();
    }
    
    /**
     * 将目录打包为ZIP文件
     * @param sourceDir 源目录
     * @param destFile 目标ZIP文件
     * @throws IOException 如果打包过程出错
     */
    private void zipDirectory(File sourceDir, File destFile) throws IOException {
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
    private void zipFiles(File rootDir, File sourceFile, ZipOutputStream zos) throws IOException {
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
} 