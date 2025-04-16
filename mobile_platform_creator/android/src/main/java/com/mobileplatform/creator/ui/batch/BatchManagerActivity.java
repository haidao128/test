package com.mobileplatform.creator.ui.batch;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.mobileplatform.creator.R;
import com.mobileplatform.creator.adapter.BatchAppAdapter;
import com.mobileplatform.creator.model.AppInfo;
import com.mobileplatform.creator.model.Category;
import com.mobileplatform.creator.model.LogEntry;
import com.mobileplatform.creator.viewmodel.AppCategoryViewModel;
import com.mobileplatform.creator.viewmodel.CategoryViewModel;
import com.mobileplatform.creator.viewmodel.LogEntryViewModel;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 批量管理活动
 * 用于批量操作应用，如批量导出、卸载等
 */
public class BatchManagerActivity extends AppCompatActivity implements BatchAppAdapter.OnAppSelectListener {

    private BatchAppAdapter batchAppAdapter;
    private RecyclerView recyclerView;
    private TextView selectedCountText;
    private Button applyFilterButton;
    private Button exportButton;
    private Button uninstallButton;
    private Button addToCategoryButton;
    private Button shareButton;
    private Chip selectAllChip;
    private Chip systemAppsChip;
    private Chip userAppsChip;
    private AutoCompleteTextView categoryDropdown;
    private View progressBar;

    private List<AppInfo> allApps = new ArrayList<>();
    private List<Category> categories = new ArrayList<>();
    private ExecutorService executorService;
    private CategoryViewModel categoryViewModel;
    private LogEntryViewModel logEntryViewModel;
    private AppCategoryViewModel appCategoryViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_batch_manager);

        // 初始化工具栏
        Toolbar toolbar = findViewById(R.id.toolbar_batch);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // 初始化视图
        initViews();
        
        // 初始化线程池和ViewModel
        executorService = Executors.newFixedThreadPool(2);
        categoryViewModel = new ViewModelProvider(this).get(CategoryViewModel.class);
        logEntryViewModel = new ViewModelProvider(this).get(LogEntryViewModel.class);
        appCategoryViewModel = new ViewModelProvider(this).get(AppCategoryViewModel.class);
        
        // 初始化分类下拉列表
        categoryViewModel.getAllCategories().observe(this, categories -> {
            this.categories = categories;
            updateCategoryDropdown();
        });
        
        // 加载所有应用
        loadInstalledApps();
        
        // 设置按钮点击监听器
        setupButtonListeners();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recycler_selected_apps);
        selectedCountText = findViewById(R.id.text_selected_count);
        selectAllChip = findViewById(R.id.chip_select_all);
        systemAppsChip = findViewById(R.id.chip_select_system);
        userAppsChip = findViewById(R.id.chip_select_user);
        categoryDropdown = findViewById(R.id.dropdown_category);
        applyFilterButton = findViewById(R.id.button_apply_filter);
        exportButton = findViewById(R.id.button_batch_export);
        uninstallButton = findViewById(R.id.button_batch_uninstall);
        addToCategoryButton = findViewById(R.id.button_add_to_category);
        shareButton = findViewById(R.id.button_batch_share);
        progressBar = findViewById(R.id.progress_bar);
        
        // 添加管理分类按钮
        Button manageCategoriesButton = findViewById(R.id.button_manage_categories);
        manageCategoriesButton.setOnClickListener(v -> {
            // 跳转到分类管理界面
            Intent intent = new Intent(this, com.mobileplatform.creator.ui.category.CategoryManagerActivity.class);
            startActivity(intent);
        });

        // 初始化RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        batchAppAdapter = new BatchAppAdapter(this);
        recyclerView.setAdapter(batchAppAdapter);
    }

    private void setupButtonListeners() {
        // 筛选按钮
        selectAllChip.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                batchAppAdapter.selectAll();
                updateSelectedCount();
            } else {
                batchAppAdapter.clearSelection();
                updateSelectedCount();
            }
        });

        // 应用过滤按钮
        applyFilterButton.setOnClickListener(v -> applyFilters());

        // 批量导出按钮
        exportButton.setOnClickListener(v -> {
            List<AppInfo> selectedApps = batchAppAdapter.getSelectedApps();
            if (selectedApps.isEmpty()) {
                Toast.makeText(this, "请先选择要导出的应用", Toast.LENGTH_SHORT).show();
                return;
            }
            exportSelectedApps(selectedApps);
        });

        // 批量卸载按钮
        uninstallButton.setOnClickListener(v -> {
            List<AppInfo> selectedApps = batchAppAdapter.getSelectedApps();
            if (selectedApps.isEmpty()) {
                Toast.makeText(this, "请先选择要卸载的应用", Toast.LENGTH_SHORT).show();
                return;
            }
            confirmUninstall(selectedApps);
        });

        // 添加到分类按钮
        addToCategoryButton.setOnClickListener(v -> {
            List<AppInfo> selectedApps = batchAppAdapter.getSelectedApps();
            if (selectedApps.isEmpty()) {
                Toast.makeText(this, "请先选择要添加的应用", Toast.LENGTH_SHORT).show();
                return;
            }
            showCategorySelectionDialog(selectedApps);
        });

        // 批量分享按钮
        shareButton.setOnClickListener(v -> {
            List<AppInfo> selectedApps = batchAppAdapter.getSelectedApps();
            if (selectedApps.isEmpty()) {
                Toast.makeText(this, "请先选择要分享的应用", Toast.LENGTH_SHORT).show();
                return;
            }
            shareSelectedApps(selectedApps);
        });
    }

    private void loadInstalledApps() {
        showProgress(true);
        executorService.submit(() -> {
            PackageManager pm = getPackageManager();
            List<PackageInfo> packages = pm.getInstalledPackages(PackageManager.GET_META_DATA);
            allApps.clear();

            for (PackageInfo packageInfo : packages) {
                try {
                    String appName = packageInfo.applicationInfo.loadLabel(pm).toString();
                    String packageName = packageInfo.packageName;
                    String versionName = packageInfo.versionName;
                    int versionCode = packageInfo.versionCode;
                    String appPath = packageInfo.applicationInfo.sourceDir;
                    
                    AppInfo appInfo = new AppInfo(
                            appName, 
                            packageName, 
                            versionName, 
                            versionCode, 
                            packageInfo.applicationInfo.loadIcon(pm),
                            appPath
                    );
                    allApps.add(appInfo);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            runOnUiThread(() -> {
                showProgress(false);
                // 默认选择用户应用
                userAppsChip.setChecked(true);
                applyFilters();
            });
        });
    }

    private void updateCategoryDropdown() {
        List<String> categoryNames = new ArrayList<>();
        categoryNames.add("所有分类");
        for (Category category : categories) {
            categoryNames.add(category.getName());
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, 
                android.R.layout.simple_dropdown_item_1line, 
                categoryNames
        );
        categoryDropdown.setAdapter(adapter);
        categoryDropdown.setText("所有分类", false);
    }

    private void applyFilters() {
        boolean showSystemApps = systemAppsChip.isChecked();
        boolean showUserApps = userAppsChip.isChecked();
        String selectedCategory = categoryDropdown.getText().toString();
        
        showProgress(true);
        executorService.submit(() -> {
            List<AppInfo> filteredApps = new ArrayList<>();
            PackageManager pm = getPackageManager();
            
            // 如果选择了特定分类且不是"所有分类"
            if (!selectedCategory.equals("所有分类") && !selectedCategory.isEmpty()) {
                // 找到对应的分类ID
                String categoryId = null;
                for (Category category : categories) {
                    if (category.getName().equals(selectedCategory)) {
                        categoryId = category.getId();
                        break;
                    }
                }
                
                if (categoryId != null) {
                    // 获取分类中的应用包名
                    List<String> packageNamesInCategory = appCategoryViewModel.getAppsInCategory(categoryId).getValue();
                    
                    if (packageNamesInCategory != null) {
                        for (AppInfo app : allApps) {
                            try {
                                PackageInfo packageInfo = pm.getPackageInfo(app.getPackageName(), 0);
                                boolean isSystemApp = (packageInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
                                
                                // 按系统/用户应用筛选
                                boolean shouldShowByAppType = (isSystemApp && showSystemApps) || (!isSystemApp && showUserApps);
                                
                                // 按分类筛选
                                boolean isInSelectedCategory = packageNamesInCategory.contains(app.getPackageName());
                                
                                if (shouldShowByAppType && isInSelectedCategory) {
                                    filteredApps.add(app);
                                }
                            } catch (PackageManager.NameNotFoundException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            } else {
                // 只按系统/用户应用筛选
                for (AppInfo app : allApps) {
                    try {
                        PackageInfo packageInfo = pm.getPackageInfo(app.getPackageName(), 0);
                        boolean isSystemApp = (packageInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
                        
                        if ((isSystemApp && showSystemApps) || (!isSystemApp && showUserApps)) {
                            filteredApps.add(app);
                        }
                    } catch (PackageManager.NameNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            }
            
            runOnUiThread(() -> {
                showProgress(false);
                batchAppAdapter.setAppList(filteredApps);
                updateSelectedCount();
            });
        });
    }

    private void updateSelectedCount() {
        int count = batchAppAdapter.getSelectedCount();
        selectedCountText.setText(String.format("已选择 %d 个应用", count));
    }

    private void exportSelectedApps(List<AppInfo> apps) {
        showProgress(true);
        executorService.submit(() -> {
            int successCount = 0;
            for (AppInfo app : apps) {
                boolean success = exportApk(app);
                if (success) {
                    successCount++;
                    // 记录日志
                    runOnUiThread(() -> {
                        logEntryViewModel.insert(new LogEntry(
                                app.getAppName(),
                                app.getPackageName(),
                                "EXPORT",
                                "SUCCESS",
                                "应用已导出至: /sdcard/Download/Apps/"
                        ));
                    });
                }
            }
            
            int finalSuccessCount = successCount;
            runOnUiThread(() -> {
                showProgress(false);
                Toast.makeText(this, 
                        String.format("已成功导出 %d/%d 个应用", finalSuccessCount, apps.size()),
                        Toast.LENGTH_LONG).show();
            });
        });
    }

    private boolean exportApk(AppInfo app) {
        File srcFile = new File(app.getAppPath());
        if (!srcFile.exists()) {
            return false;
        }
        
        try {
            // 创建导出目录
            File exportDir = new File("/sdcard/Download/Apps/");
            if (!exportDir.exists()) {
                boolean dirCreated = exportDir.mkdirs();
                if (!dirCreated) {
                    return false;
                }
            }
            
            // 创建目标文件
            String exportName = app.getAppName().replaceAll("[^a-zA-Z0-9._-]", "_")
                    + "_" + app.getVersionName() + ".apk";
            File destFile = new File(exportDir, exportName);
            
            // 复制文件
            try (FileChannel source = new FileInputStream(srcFile).getChannel();
                 FileChannel destination = new FileOutputStream(destFile).getChannel()) {
                destination.transferFrom(source, 0, source.size());
            }
            
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void confirmUninstall(List<AppInfo> apps) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("批量卸载")
                .setMessage(String.format("确定要卸载选中的 %d 个应用吗？", apps.size()))
                .setPositiveButton("卸载", (dialog, which) -> uninstallSelectedApps(apps))
                .setNegativeButton("取消", null)
                .show();
    }

    private void uninstallSelectedApps(List<AppInfo> apps) {
        for (AppInfo app : apps) {
            try {
                Intent intent = new Intent(Intent.ACTION_UNINSTALL_PACKAGE);
                intent.setData(Uri.parse("package:" + app.getPackageName()));
                intent.putExtra(Intent.EXTRA_RETURN_RESULT, true);
                startActivity(intent);
                
                // 记录日志
                logEntryViewModel.insert(new LogEntry(
                        app.getAppName(),
                        app.getPackageName(),
                        "UNINSTALL",
                        "INITIATED",
                        "已发起卸载请求"
                ));
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, 
                        "卸载 " + app.getAppName() + " 失败: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showCategorySelectionDialog(List<AppInfo> apps) {
        if (categories.isEmpty()) {
            // 如果没有分类，提示用户并提供创建分类的选项
            new MaterialAlertDialogBuilder(this)
                    .setTitle("没有可用分类")
                    .setMessage("您还没有创建任何分类，是否现在创建？")
                    .setPositiveButton("创建分类", (dialog, which) -> {
                        // 跳转到分类管理界面
                        Intent intent = new Intent(this, com.mobileplatform.creator.ui.category.CategoryManagerActivity.class);
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
                    
                    // 获取所有选中应用的包名
                    List<String> packageNames = new ArrayList<>();
                    for (AppInfo app : apps) {
                        packageNames.add(app.getPackageName());
                    }
                    
                    // 批量添加应用到分类
                    appCategoryViewModel.addAppsToCategory(packageNames, selectedCategory.getId());
                    
                    // 显示成功提示
                    Toast.makeText(this, 
                            String.format("已将 %d 个应用添加到分类: %s", 
                                    apps.size(), selectedCategory.getName()),
                            Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("取消", null)
                .setNeutralButton("创建新分类", (dialog, which) -> {
                    // 跳转到分类管理界面
                    Intent intent = new Intent(this, com.mobileplatform.creator.ui.category.CategoryManagerActivity.class);
                    startActivity(intent);
                })
                .show();
    }

    private void shareSelectedApps(List<AppInfo> apps) {
        if (apps.size() > 1) {
            // 多个应用分享
            ArrayList<Uri> uris = new ArrayList<>();
            for (AppInfo app : apps) {
                File apkFile = new File(app.getAppPath());
                if (apkFile.exists()) {
                    Uri uri = FileProvider.getUriForFile(
                            this,
                            "com.mobileplatform.creator.fileprovider",
                            apkFile
                    );
                    uris.add(uri);
                }
            }
            
            if (uris.isEmpty()) {
                Toast.makeText(this, "没有可分享的应用", Toast.LENGTH_SHORT).show();
                return;
            }
            
            Intent shareIntent = new Intent();
            shareIntent.setAction(Intent.ACTION_SEND_MULTIPLE);
            shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
            shareIntent.setType("application/vnd.android.package-archive");
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            
            startActivity(Intent.createChooser(shareIntent, "分享到"));
        } else if (apps.size() == 1) {
            // 单个应用分享
            AppInfo app = apps.get(0);
            File apkFile = new File(app.getAppPath());
            if (!apkFile.exists()) {
                Toast.makeText(this, "应用文件不存在", Toast.LENGTH_SHORT).show();
                return;
            }
            
            Uri uri = FileProvider.getUriForFile(
                    this,
                    "com.mobileplatform.creator.fileprovider",
                    apkFile
            );
            
            Intent shareIntent = new Intent();
            shareIntent.setAction(Intent.ACTION_SEND);
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
            shareIntent.setType("application/vnd.android.package-archive");
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            
            startActivity(Intent.createChooser(shareIntent, "分享到"));
        }
    }

    private void showProgress(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onAppSelected(AppInfo app, boolean isSelected) {
        updateSelectedCount();
    }

    @Override
    public void onAppRemoved(AppInfo app) {
        updateSelectedCount();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 当从分类管理界面返回时，重新加载分类数据
        categoryViewModel.getAllCategories().observe(this, categories -> {
            this.categories = categories;
            updateCategoryDropdown();
        });
    }
} 