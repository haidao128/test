package com.mobileplatform.creator.ui.mpk;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputLayout;
import com.mobileplatform.creator.R;
import com.mobileplatform.creator.data.model.MPKPackage;
import com.mobileplatform.creator.mpk.MPKManager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * MPK应用包创建器
 * 
 * 允许用户创建自定义的MPK应用包
 */
public class MPKCreatorActivity extends AppCompatActivity {
    
    private static final int REQUEST_STORAGE_PERMISSION = 1001;
    
    // UI组件
    private TextInputLayout idLayout;
    private TextInputLayout nameLayout;
    private TextInputLayout versionLayout;
    private TextInputLayout descriptionLayout;
    private TextInputLayout authorLayout;
    private Spinner platformSpinner;
    private ChipGroup permissionsGroup;
    private Button btnAddPermission;
    private EditText permissionInput;
    private Button btnCreateMPK;
    private ProgressBar progressBar;
    private TextView statusText;
    
    // 权限列表
    private List<String> permissions = new ArrayList<>();
    
    // MPK包对象
    private MPKPackage mpkPackage;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mpk_creator);
        
        // 设置返回按钮
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.create_mpk_package);
        }
        
        // 初始化UI组件
        initViews();
        
        // 初始化MPK包对象
        mpkPackage = new MPKPackage();
        
        // 初始化权限列表
        initPermissionList();
        
        // 检查权限
        checkStoragePermission();
    }
    
    /**
     * 初始化UI组件
     */
    private void initViews() {
        idLayout = findViewById(R.id.package_id_layout);
        nameLayout = findViewById(R.id.package_name_layout);
        versionLayout = findViewById(R.id.package_version_layout);
        descriptionLayout = findViewById(R.id.package_description_layout);
        authorLayout = findViewById(R.id.package_author_layout);
        platformSpinner = findViewById(R.id.platform_spinner);
        permissionsGroup = findViewById(R.id.permissions_group);
        btnAddPermission = findViewById(R.id.btn_add_permission);
        permissionInput = findViewById(R.id.permission_input);
        btnCreateMPK = findViewById(R.id.btn_create_mpk);
        progressBar = findViewById(R.id.progress_bar);
        statusText = findViewById(R.id.status_text);
        
        // 设置添加权限按钮点击事件
        btnAddPermission.setOnClickListener(v -> addPermission());
        
        // 设置创建MPK按钮点击事件
        btnCreateMPK.setOnClickListener(v -> validateAndCreateMPK());
    }
    
    /**
     * 初始化权限列表
     */
    private void initPermissionList() {
        // 添加常用权限
        String[] commonPermissions = {
                "READ_EXTERNAL_STORAGE",
                "WRITE_EXTERNAL_STORAGE",
                "INTERNET",
                "CAMERA",
                "ACCESS_FINE_LOCATION"
        };
        
        for (String permission : commonPermissions) {
            addPermissionChip(permission);
        }
    }
    
    /**
     * 添加权限
     */
    private void addPermission() {
        String permission = permissionInput.getText().toString().trim();
        if (permission.isEmpty()) {
            Toast.makeText(this, "请输入权限名称", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 添加权限芯片
        addPermissionChip(permission);
        
        // 清空输入框
        permissionInput.setText("");
    }
    
    /**
     * 添加权限芯片
     */
    private void addPermissionChip(String permission) {
        if (permissions.contains(permission)) {
            return;
        }
        
        // 创建芯片
        Chip chip = new Chip(this);
        chip.setText(permission);
        chip.setCloseIconVisible(true);
        chip.setCheckable(false);
        
        // 设置关闭图标点击事件
        chip.setOnCloseIconClickListener(v -> {
            permissions.remove(permission);
            permissionsGroup.removeView(chip);
        });
        
        // 添加芯片到芯片组
        permissionsGroup.addView(chip);
        
        // 添加权限到列表
        permissions.add(permission);
    }
    
    /**
     * 验证并创建MPK包
     */
    private void validateAndCreateMPK() {
        // 获取输入值
        String id = idLayout.getEditText().getText().toString().trim();
        String name = nameLayout.getEditText().getText().toString().trim();
        String version = versionLayout.getEditText().getText().toString().trim();
        String description = descriptionLayout.getEditText().getText().toString().trim();
        String author = authorLayout.getEditText().getText().toString().trim();
        String platform = platformSpinner.getSelectedItem().toString();
        
        // 验证必填字段
        boolean isValid = true;
        
        if (id.isEmpty()) {
            idLayout.setError("包ID不能为空");
            isValid = false;
        } else {
            idLayout.setError(null);
        }
        
        if (name.isEmpty()) {
            nameLayout.setError("应用名称不能为空");
            isValid = false;
        } else {
            nameLayout.setError(null);
        }
        
        if (version.isEmpty()) {
            versionLayout.setError("版本号不能为空");
            isValid = false;
        } else {
            versionLayout.setError(null);
        }
        
        if (!isValid) {
            return;
        }
        
        // 更新MPK包对象
        mpkPackage.setId(id);
        mpkPackage.setName(name);
        mpkPackage.setVersion(version);
        mpkPackage.setDescription(description);
        mpkPackage.setAuthor(author);
        mpkPackage.setPlatform(platform.toLowerCase());
        mpkPackage.setPermissions(permissions);
        
        // 显示进度条
        progressBar.setVisibility(View.VISIBLE);
        btnCreateMPK.setEnabled(false);
        statusText.setText("正在创建MPK包...");
        
        // 创建MPK包
        createMPK();
    }
    
    /**
     * 创建MPK包
     */
    private void createMPK() {
        // 创建源目录
        File sourceDir = new File(getExternalFilesDir(null), "mpk_source/" + mpkPackage.getId());
        if (!sourceDir.exists()) {
            sourceDir.mkdirs();
        }
        
        // 创建必要的子目录
        File codeDir = new File(sourceDir, "code");
        File assetsDir = new File(sourceDir, "assets");
        File configDir = new File(sourceDir, "config");
        
        codeDir.mkdirs();
        assetsDir.mkdirs();
        configDir.mkdirs();
        
        // 创建输出文件
        File outputDir = new File(getExternalFilesDir(null), "mpk_output");
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }
        
        File outputFile = new File(outputDir, mpkPackage.getId() + "_" + mpkPackage.getVersion() + ".mpk");
        
        // 调用MPKManager创建MPK包
        MPKManager.getInstance(this).createMPK(sourceDir, outputFile, mpkPackage, new MPKManager.MPKCreateCallback() {
            @Override
            public void onSuccess(File mpkFile) {
                // 隐藏进度条
                progressBar.setVisibility(View.GONE);
                btnCreateMPK.setEnabled(true);
                
                // 显示成功消息
                statusText.setText("MPK包创建成功：" + mpkFile.getAbsolutePath());
                Toast.makeText(MPKCreatorActivity.this, "MPK包创建成功", Toast.LENGTH_SHORT).show();
            }
            
            @Override
            public void onError(String errorMessage) {
                // 隐藏进度条
                progressBar.setVisibility(View.GONE);
                btnCreateMPK.setEnabled(true);
                
                // 显示错误消息
                statusText.setText("创建失败：" + errorMessage);
                Toast.makeText(MPKCreatorActivity.this, "创建失败：" + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    /**
     * 检查存储权限
     */
    private void checkStoragePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_STORAGE_PERMISSION);
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "需要存储权限以创建MPK包", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }
    
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
} 