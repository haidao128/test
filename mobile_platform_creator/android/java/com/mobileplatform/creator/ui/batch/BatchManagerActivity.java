package com.mobileplatform.creator.ui.batch;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.mobileplatform.creator.R;
import com.mobileplatform.creator.data.model.AppInfo;
import com.mobileplatform.creator.data.model.StoreAppInfo;
import com.mobileplatform.creator.data.repository.AppRepository;
import com.mobileplatform.creator.ui.app.AppDetailActivity;
import com.mobileplatform.creator.ui.download.DownloadActivity;
import com.mobileplatform.creator.update.UpdateChecker;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 批量应用管理活动
 */
public class BatchManagerActivity extends AppCompatActivity implements UpdateChecker.UpdateCheckListener {
    
    // 视图组件
    private RecyclerView appList;
    private TextView emptyView;
    private ProgressBar loadingIndicator;
    private CheckBox selectAllCheckbox;
    private TextView selectionCount;
    private MaterialButton updateButton;
    private MaterialButton uninstallButton;
    private MaterialButton filterButton;
    private MaterialButton sortButton;
    
    // 适配器
    private BatchAppAdapter adapter;
    
    // 应用仓库
    private AppRepository appRepository;
    
    // 更新检查器
    private UpdateChecker updateChecker;
    
    // 可更新的应用集合
    private Set<String> updatableApps = new HashSet<>();
    
    // 排序选项
    private SortOption currentSortOption = SortOption.NAME_ASC;
    
    // 筛选选项
    private FilterOption currentFilterOption = FilterOption.ALL;
    
    // 应用列表
    private List<AppInfo> originalAppList = new ArrayList<>();
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_batch_manager);
        
        // 初始化组件
        initializeComponents();
        
        // 设置工具栏
        setupToolbar();
        
        // 设置列表
        setupRecyclerView();
        
        // 设置按钮点击事件
        setupButtonClickListeners();
        
        // 加载应用列表
        loadAppList();
    }
    
    /**
     * 初始化组件
     */
    private void initializeComponents() {
        appList = findViewById(R.id.app_list);
        emptyView = findViewById(R.id.empty_view);
        loadingIndicator = findViewById(R.id.loading_indicator);
        selectAllCheckbox = findViewById(R.id.select_all);
        selectionCount = findViewById(R.id.selection_count);
        updateButton = findViewById(R.id.update_button);
        uninstallButton = findViewById(R.id.uninstall_button);
        filterButton = findViewById(R.id.filter_button);
        sortButton = findViewById(R.id.sort_button);
        
        appRepository = AppRepository.getInstance(this);
        updateChecker = UpdateChecker.getInstance(this);
        
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
            getSupportActionBar().setDisplayShowTitleEnabled(true);
        }
    }
    
    /**
     * 设置RecyclerView
     */
    private void setupRecyclerView() {
        adapter = new BatchAppAdapter(this);
        appList.setLayoutManager(new LinearLayoutManager(this));
        appList.setAdapter(adapter);
        
        // 设置选择变化监听器
        adapter.setOnItemSelectionChangeListener(count -> {
            selectionCount.setText("已选择 " + count + " 个应用");
            updateButton.setEnabled(count > 0);
            uninstallButton.setEnabled(count > 0);
        });
        
        // 设置项目点击监听器
        adapter.setOnItemClickListener(app -> {
            // 打开应用详情页
            Intent intent = new Intent(this, AppDetailActivity.class);
            intent.putExtra(AppDetailActivity.EXTRA_APP_ID, app.getId());
            startActivity(intent);
        });
    }
    
    /**
     * 设置按钮点击事件
     */
    private void setupButtonClickListeners() {
        // 全选复选框
        selectAllCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                adapter.selectAll();
            } else {
                adapter.clearSelection();
            }
        });
        
        // 更新按钮
        updateButton.setOnClickListener(v -> {
            performBatchUpdate();
        });
        
        // 卸载按钮
        uninstallButton.setOnClickListener(v -> {
            confirmBatchUninstall();
        });
        
        // 筛选按钮
        filterButton.setOnClickListener(v -> {
            showFilterOptions();
        });
        
        // 排序按钮
        sortButton.setOnClickListener(v -> {
            showSortOptions();
        });
    }
    
    /**
     * 加载应用列表
     */
    private void loadAppList() {
        // 显示加载指示器
        loadingIndicator.setVisibility(View.VISIBLE);
        emptyView.setVisibility(View.GONE);
        
        // 从仓库加载应用列表
        appRepository.getInstalledApps(this, appInfoList -> {
            // 更新原始列表
            originalAppList.clear();
            if (appInfoList != null) {
                originalAppList.addAll(appInfoList);
            }
            
            // 应用筛选和排序
            List<AppInfo> filteredList = applyFilterAndSort(originalAppList);
            
            // 更新适配器
            adapter.setAppList(filteredList);
            
            // 更新UI状态
            loadingIndicator.setVisibility(View.GONE);
            if (filteredList.isEmpty()) {
                emptyView.setVisibility(View.VISIBLE);
                appList.setVisibility(View.GONE);
            } else {
                emptyView.setVisibility(View.GONE);
                appList.setVisibility(View.VISIBLE);
            }
            
            // 检查更新
            updateChecker.checkUpdates(originalAppList);
        });
    }
    
    /**
     * 应用筛选和排序
     */
    private List<AppInfo> applyFilterAndSort(List<AppInfo> appList) {
        List<AppInfo> result = new ArrayList<>();
        
        // 应用筛选
        for (AppInfo app : appList) {
            switch (currentFilterOption) {
                case ALL:
                    result.add(app);
                    break;
                case SYSTEM:
                    if (app.isSystemApp()) {
                        result.add(app);
                    }
                    break;
                case USER:
                    if (!app.isSystemApp()) {
                        result.add(app);
                    }
                    break;
                case UPDATABLE:
                    if (updatableApps.contains(app.getId())) {
                        result.add(app);
                    }
                    break;
            }
        }
        
        // 应用排序
        switch (currentSortOption) {
            case NAME_ASC:
                result.sort(Comparator.comparing(AppInfo::getName));
                break;
            case NAME_DESC:
                result.sort(Comparator.comparing(AppInfo::getName).reversed());
                break;
            case SIZE_ASC:
                result.sort(Comparator.comparing(AppInfo::getSize));
                break;
            case SIZE_DESC:
                result.sort(Comparator.comparing(AppInfo::getSize).reversed());
                break;
            case INSTALL_TIME_ASC:
                result.sort(Comparator.comparing(AppInfo::getInstallTime));
                break;
            case INSTALL_TIME_DESC:
                result.sort(Comparator.comparing(AppInfo::getInstallTime).reversed());
                break;
        }
        
        return result;
    }
    
    /**
     * 显示筛选选项
     */
    private void showFilterOptions() {
        final String[] options = {"全部应用", "系统应用", "用户应用", "可更新应用"};
        int selectedIndex = currentFilterOption.ordinal();
        
        new AlertDialog.Builder(this)
                .setTitle("筛选应用")
                .setSingleChoiceItems(options, selectedIndex, (dialog, which) -> {
                    currentFilterOption = FilterOption.values()[which];
                    List<AppInfo> filteredList = applyFilterAndSort(originalAppList);
                    adapter.setAppList(filteredList);
                    dialog.dismiss();
                    
                    // 更新UI状态
                    if (filteredList.isEmpty()) {
                        emptyView.setVisibility(View.VISIBLE);
                        appList.setVisibility(View.GONE);
                    } else {
                        emptyView.setVisibility(View.GONE);
                        appList.setVisibility(View.VISIBLE);
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }
    
    /**
     * 显示排序选项
     */
    private void showSortOptions() {
        final String[] options = {
                "名称 (A-Z)",
                "名称 (Z-A)",
                "大小 (小-大)",
                "大小 (大-小)",
                "安装时间 (早-晚)",
                "安装时间 (晚-早)"
        };
        int selectedIndex = currentSortOption.ordinal();
        
        new AlertDialog.Builder(this)
                .setTitle("排序应用")
                .setSingleChoiceItems(options, selectedIndex, (dialog, which) -> {
                    currentSortOption = SortOption.values()[which];
                    List<AppInfo> filteredList = applyFilterAndSort(originalAppList);
                    adapter.setAppList(filteredList);
                    dialog.dismiss();
                })
                .setNegativeButton("取消", null)
                .show();
    }
    
    /**
     * 执行批量更新
     */
    private void performBatchUpdate() {
        List<AppInfo> selectedUpdatableApps = adapter.getSelectedUpdatableApps();
        if (selectedUpdatableApps.isEmpty()) {
            Snackbar.make(appList, "没有可更新的应用", Snackbar.LENGTH_SHORT).show();
            return;
        }
        
        // 启动下载活动
        Intent intent = new Intent(this, DownloadActivity.class);
        intent.putExtra("batch_update", true);
        startActivity(intent);
        
        // 显示通知
        Snackbar.make(appList, "已添加 " + selectedUpdatableApps.size() + " 个应用到更新队列", Snackbar.LENGTH_SHORT).show();
    }
    
    /**
     * 确认批量卸载
     */
    private void confirmBatchUninstall() {
        List<AppInfo> selectedApps = adapter.getSelectedApps();
        if (selectedApps.isEmpty()) {
            Snackbar.make(appList, "未选择应用", Snackbar.LENGTH_SHORT).show();
            return;
        }
        
        new AlertDialog.Builder(this)
                .setTitle("批量卸载")
                .setMessage("确定要卸载选中的 " + selectedApps.size() + " 个应用吗？")
                .setPositiveButton("确定", (dialog, which) -> {
                    performBatchUninstall(selectedApps);
                })
                .setNegativeButton("取消", null)
                .show();
    }
    
    /**
     * 执行批量卸载
     */
    private void performBatchUninstall(List<AppInfo> apps) {
        for (AppInfo app : apps) {
            // 启动卸载意图
            Intent intent = new Intent(Intent.ACTION_DELETE);
            intent.setData(android.net.Uri.parse("package:" + app.getPackageName()));
            startActivity(intent);
        }
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_batch_manager, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        
        if (id == android.R.id.home) {
            finish();
            return true;
        } else if (id == R.id.action_refresh) {
            loadAppList();
            return true;
        } else if (id == R.id.action_export) {
            // TODO: 实现导出功能
            Snackbar.make(appList, "导出功能尚未实现", Snackbar.LENGTH_SHORT).show();
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    public void onUpdateAvailable(AppInfo appInfo, StoreAppInfo updateInfo) {
        // 添加到可更新集合
        updatableApps.add(appInfo.getId());
        adapter.setUpdatableApps(updatableApps);
    }
    
    @Override
    public void onNoUpdate(AppInfo appInfo) {
        // 不做处理
    }
    
    @Override
    public void onError(String errorMessage) {
        // 不做处理
    }
    
    @Override
    public void onProgress(int checked, int total) {
        // 不做处理
    }
    
    @Override
    public void onBatchComplete() {
        // 更新适配器
        adapter.setUpdatableApps(updatableApps);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        // 移除更新检查监听器
        updateChecker.removeListener(this);
    }
    
    /**
     * 排序选项枚举
     */
    private enum SortOption {
        NAME_ASC,
        NAME_DESC,
        SIZE_ASC,
        SIZE_DESC,
        INSTALL_TIME_ASC,
        INSTALL_TIME_DESC
    }
    
    /**
     * 筛选选项枚举
     */
    private enum FilterOption {
        ALL,
        SYSTEM,
        USER,
        UPDATABLE
    }
} 