package com.mobileplatform.creator.ui.app;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.bumptech.glide.Glide;
import com.mobileplatform.creator.R;

public class AppDetailActivity extends AppCompatActivity {

    private static final String TAG = "AppDetailActivity";
    private String currentPackageName;
    private PackageInfo currentPackageInfo;

    private ImageView imageViewIcon;
    private TextView textViewAppName;
    private TextView textViewPackageName;
    private TextView textViewVersionName;
    private TextView textViewVersionCode;
    private TextView textViewAppPath;
    private Button buttonOpenApp;
    private Button buttonUninstallApp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_detail);

        // 设置 Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar_detail);
        setSupportActionBar(toolbar);

        // 启用 ActionBar 的返回按钮
        if (getSupportActionBar() != null) {
             getSupportActionBar().setDisplayHomeAsUpEnabled(true);
             getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        // 初始化视图
        imageViewIcon = findViewById(R.id.imageView_detail_icon);
        textViewAppName = findViewById(R.id.textView_detail_app_name);
        textViewPackageName = findViewById(R.id.textView_detail_package_name);
        textViewVersionName = findViewById(R.id.textView_detail_version_name);
        textViewVersionCode = findViewById(R.id.textView_detail_version_code);
        textViewAppPath = findViewById(R.id.textView_detail_app_path);
        buttonOpenApp = findViewById(R.id.button_detail_open);
        buttonUninstallApp = findViewById(R.id.button_detail_uninstall);

        // 获取传递过来的包名
        currentPackageName = getIntent().getStringExtra("PACKAGE_NAME");

        if (currentPackageName != null && !currentPackageName.isEmpty()) {
            loadAppDetails(currentPackageName);
        } else {
            Log.e(TAG, "Package name not received!");
            Toast.makeText(this, "无法加载应用详情，包名丢失", Toast.LENGTH_SHORT).show();
            finish(); // 关闭当前 Activity
        }
        
        setupButtons();
    }

    /**
     * 根据包名加载应用的详细信息并更新 UI。
     * @param packageName 应用包名
     */
    private void loadAppDetails(String packageName) {
        PackageManager pm = getPackageManager();
        try {
            currentPackageInfo = pm.getPackageInfo(packageName, 0); // 0表示不需要额外信息
            ApplicationInfo appInfo = currentPackageInfo.applicationInfo;

            // 设置标题为应用名
            setTitle(appInfo.loadLabel(pm).toString());

            // 更新 UI
            Glide.with(this).load(appInfo.loadIcon(pm)).into(imageViewIcon);
            textViewAppName.setText(appInfo.loadLabel(pm).toString());
            textViewPackageName.setText("包名: " + packageName);
            textViewVersionName.setText("版本名称: " + currentPackageInfo.versionName);
            textViewVersionCode.setText("版本号: " + currentPackageInfo.versionCode);
            textViewAppPath.setText("路径: " + appInfo.sourceDir);

        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "App not found: " + packageName, e);
            Toast.makeText(this, "找不到应用: " + packageName, Toast.LENGTH_SHORT).show();
            finish();
        }
    }
    
    /**
     * 设置按钮的点击事件。
     */
    private void setupButtons() {
        // 打开应用按钮
        buttonOpenApp.setOnClickListener(v -> {
            if (currentPackageName != null) {
                Intent launchIntent = getPackageManager().getLaunchIntentForPackage(currentPackageName);
                if (launchIntent != null) {
                    startActivity(launchIntent);
                } else {
                    Toast.makeText(this, "无法启动该应用", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // 卸载应用按钮
        buttonUninstallApp.setOnClickListener(v -> {
            if (currentPackageName != null) {
                Uri packageURI = Uri.parse("package:" + currentPackageName);
                Intent uninstallIntent = new Intent(Intent.ACTION_DELETE, packageURI);
                try {
                    startActivity(uninstallIntent);
                    // 卸载操作由系统处理，我们可以在 onResume 中检查应用是否还存在
                } catch (Exception e) {
                    Log.e(TAG, "Failed to start uninstall intent for " + currentPackageName, e);
                    Toast.makeText(this, "启动卸载失败", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        // 处理 ActionBar 的返回按钮
        onBackPressed();
        return true;
    }
    
     // TODO: 可以在 onResume 中检查应用是否已被卸载，如果卸载了可以 finish() 当前 Activity
     /*
     @Override
     protected void onResume() {
         super.onResume();
         if (currentPackageName != null) {
             try {
                 getPackageManager().getPackageInfo(currentPackageName, 0);
                 // 应用仍然存在
             } catch (PackageManager.NameNotFoundException e) {
                 // 应用已被卸载
                 Toast.makeText(this, "应用已被卸载", Toast.LENGTH_SHORT).show();
                 finish();
             }
         }
     }
     */
} 