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
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.mobileplatform.creator.R;
import com.mobileplatform.creator.model.Category;
import com.mobileplatform.creator.ui.category.CategoryManagerActivity;
import com.mobileplatform.creator.viewmodel.AppCategoryViewModel;
import com.mobileplatform.creator.viewmodel.CategoryViewModel;

import java.util.ArrayList;
import java.util.List;

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
    private Button buttonAddToCategory;
    private Button buttonUninstallApp;

    private CategoryViewModel categoryViewModel;
    private AppCategoryViewModel appCategoryViewModel;
    private List<Category> categories = new ArrayList<>();

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
        buttonAddToCategory = findViewById(R.id.button_detail_add_to_category);
        buttonUninstallApp = findViewById(R.id.button_detail_uninstall);

        // 初始化ViewModel
        categoryViewModel = new ViewModelProvider(this).get(CategoryViewModel.class);
        appCategoryViewModel = new ViewModelProvider(this).get(AppCategoryViewModel.class);
        
        // 加载分类数据
        categoryViewModel.getAllCategories().observe(this, categoriesList -> {
            categories = categoriesList;
        });

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
        
        // 添加到分类按钮
        buttonAddToCategory.setOnClickListener(v -> {
            showCategorySelectionDialog();
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
    
    /**
     * 显示分类选择对话框
     */
    private void showCategorySelectionDialog() {
        if (categories.isEmpty()) {
            // 如果没有分类，提示用户并提供创建分类的选项
            new MaterialAlertDialogBuilder(this)
                    .setTitle("没有可用分类")
                    .setMessage("您还没有创建任何分类，是否现在创建？")
                    .setPositiveButton("创建分类", (dialog, which) -> {
                        // 跳转到分类管理界面
                        Intent intent = new Intent(this, CategoryManagerActivity.class);
                        startActivity(intent);
                    })
                    .setNegativeButton("取消", null)
                    .show();
            return;
        }
        
        String[] categoryNames = new String[categories.size()];
        for (int i = 0; i < categories.size(); i++) {
            categoryNames[i] = categories.get(i).getName();
        }
        
        new MaterialAlertDialogBuilder(this)
                .setTitle("选择分类")
                .setItems(categoryNames, (dialog, which) -> {
                    Category selectedCategory = categories.get(which);
                    
                    // 添加应用到分类
                    appCategoryViewModel.addAppToCategory(currentPackageName, selectedCategory.getId());
                    
                    // 显示成功提示
                    Toast.makeText(this, 
                            String.format("已将 %s 添加到分类: %s", 
                                    textViewAppName.getText(), selectedCategory.getName()),
                            Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("取消", null)
                .setNeutralButton("创建新分类", (dialog, which) -> {
                    // 跳转到分类管理界面
                    Intent intent = new Intent(this, CategoryManagerActivity.class);
                    startActivity(intent);
                })
                .show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        // 处理 ActionBar 的返回按钮
        onBackPressed();
        return true;
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        
        // 重新加载分类数据
        categoryViewModel.getAllCategories().observe(this, categoriesList -> {
            categories = categoriesList;
        });
        
        // 检查应用是否已被卸载
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
} 