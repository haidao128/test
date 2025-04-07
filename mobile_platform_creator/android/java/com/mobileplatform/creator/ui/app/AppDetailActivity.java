package com.mobileplatform.creator.ui.app;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.mobileplatform.creator.R;
import com.mobileplatform.creator.data.model.AppInfo;
import com.mobileplatform.creator.data.model.StoreAppInfo;
import com.mobileplatform.creator.data.repository.AppRepository;
import com.mobileplatform.creator.update.UpdateChecker;
import com.mobileplatform.creator.update.UpdateChecker.UpdateCheckListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 应用详情活动
 */
public class AppDetailActivity extends AppCompatActivity implements UpdateCheckListener {
    
    // 应用ID参数
    public static final String EXTRA_APP_ID = "app_id";
    
    // 日期格式化器
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    
    // 应用信息
    private AppInfo appInfo;
    
    // 更新信息
    private StoreAppInfo updateInfo;
    
    // 更新检查器
    private UpdateChecker updateChecker;
    
    // 应用仓库
    private AppRepository appRepository;
    
    // 权限适配器
    private PermissionAdapter permissionAdapter;
    
    // 视图组件
    private ImageView appIcon;
    private TextView appName;
    private TextView appVersion;
    private MaterialCardView updateCard;
    private TextView updateVersion;
    private TextView updateSize;
    private TextView updateDescription;
    private MaterialButton updateButton;
    private TextView appPackage;
    private TextView appSize;
    private TextView appInstallTime;
    private TextView appUpdateTime;
    private RecyclerView permissionsList;
    private MaterialButton launchButton;
    private MaterialButton uninstallButton;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_detail);
        
        // 初始化组件
        initializeComponents();
        
        // 设置工具栏
        setupToolbar();
        
        // 设置权限列表
        setupPermissionsList();
        
        // 设置按钮点击事件
        setupButtonClickListeners();
        
        // 获取应用ID
        String appId = getIntent().getStringExtra(EXTRA_APP_ID);
        if (appId != null) {
            // 加载应用信息
            loadAppInfo(appId);
        }
    }
    
    /**
     * 初始化组件
     */
    private void initializeComponents() {
        // 获取视图组件
        appIcon = findViewById(R.id.app_icon);
        appName = findViewById(R.id.app_name);
        appVersion = findViewById(R.id.app_version);
        updateCard = findViewById(R.id.update_card);
        updateVersion = findViewById(R.id.update_version);
        updateSize = findViewById(R.id.update_size);
        updateDescription = findViewById(R.id.update_description);
        updateButton = findViewById(R.id.update_button);
        appPackage = findViewById(R.id.app_package);
        appSize = findViewById(R.id.app_size);
        appInstallTime = findViewById(R.id.app_install_time);
        appUpdateTime = findViewById(R.id.app_update_time);
        permissionsList = findViewById(R.id.permissions_list);
        launchButton = findViewById(R.id.launch_button);
        uninstallButton = findViewById(R.id.uninstall_button);
        
        // 初始化组件
        updateChecker = UpdateChecker.getInstance(this);
        appRepository = AppRepository.getInstance(this);
        permissionAdapter = new PermissionAdapter(this);
        
        // 添加更新检查监听器
        updateChecker.addListener(this);
    }
    
    /**
     * 设置工具栏
     */
    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
    }
    
    /**
     * 设置权限列表
     */
    private void setupPermissionsList() {
        permissionsList.setLayoutManager(new LinearLayoutManager(this));
        permissionsList.setAdapter(permissionAdapter);
    }
    
    /**
     * 设置按钮点击事件
     */
    private void setupButtonClickListeners() {
        // 更新按钮
        updateButton.setOnClickListener(v -> {
            if (updateInfo != null) {
                // 启动下载活动
                Intent intent = new Intent(this, DownloadActivity.class);
                intent.putExtra("app_id", appInfo.getId());
                intent.putExtra("update_version", updateInfo.getVersion());
                intent.putExtra("download_url", updateInfo.getDownloadUrl());
                startActivity(intent);
            }
        });
        
        // 启动按钮
        launchButton.setOnClickListener(v -> {
            if (appInfo != null) {
                // 启动应用
                Intent intent = getPackageManager().getLaunchIntentForPackage(appInfo.getPackageName());
                if (intent != null) {
                    startActivity(intent);
                }
            }
        });
        
        // 卸载按钮
        uninstallButton.setOnClickListener(v -> {
            if (appInfo != null) {
                // 卸载应用
                Intent intent = new Intent(Intent.ACTION_DELETE);
                intent.setData(Uri.parse("package:" + appInfo.getPackageName()));
                startActivity(intent);
            }
        });
    }
    
    /**
     * 加载应用信息
     */
    private void loadAppInfo(String appId) {
        appRepository.getAppInfo(appId, info -> {
            if (info != null) {
                appInfo = info;
                updateUI();
                
                // 检查更新
                updateChecker.checkUpdate(appInfo);
            }
        });
    }
    
    /**
     * 更新UI
     */
    private void updateUI() {
        // 设置基本信息
        appIcon.setImageDrawable(appInfo.getIcon());
        appName.setText(appInfo.getName());
        appVersion.setText("版本 " + appInfo.getVersion());
        appPackage.setText("包名: " + appInfo.getPackageName());
        appSize.setText("大小: " + appInfo.getFormattedSize());
        appInstallTime.setText("安装时间: " + DATE_FORMAT.format(new Date(appInfo.getInstallTime())));
        appUpdateTime.setText("更新时间: " + DATE_FORMAT.format(new Date(appInfo.getUpdateTime())));
        
        // 设置权限列表
        try {
            List<PermissionInfo> permissions = getPackageManager().queryPermissionsByGroup(
                    null,
                    PackageManager.GET_META_DATA
            );
            permissionAdapter.setPermissions(permissions);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // 设置按钮状态
        launchButton.setEnabled(true);
        uninstallButton.setEnabled(true);
    }
    
    @Override
    public void onUpdateAvailable(AppInfo appInfo, StoreAppInfo updateInfo) {
        if (this.appInfo != null && this.appInfo.getId().equals(appInfo.getId())) {
            this.updateInfo = updateInfo;
            
            // 显示更新信息
            updateCard.setVisibility(View.VISIBLE);
            updateVersion.setText("新版本: " + updateInfo.getVersion());
            updateSize.setText("大小: " + updateInfo.getFormattedSize());
            updateDescription.setText(updateInfo.getDescription());
            updateButton.setEnabled(true);
        }
    }
    
    @Override
    public void onNoUpdate(AppInfo appInfo) {
        if (this.appInfo != null && this.appInfo.getId().equals(appInfo.getId())) {
            // 隐藏更新信息
            updateCard.setVisibility(View.GONE);
        }
    }
    
    @Override
    public void onError(String errorMessage) {
        // 显示错误提示
        // TODO: 实现错误提示
    }
    
    @Override
    public void onProgress(int checked, int total) {
        // 更新进度，不做处理
    }
    
    @Override
    public void onBatchComplete() {
        // 批量检查完成，不做处理
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        // 移除更新检查监听器
        updateChecker.removeListener(this);
    }
} 