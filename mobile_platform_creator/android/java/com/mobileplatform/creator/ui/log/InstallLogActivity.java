package com.mobileplatform.creator.ui.log;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import com.mobileplatform.creator.R;
import com.mobileplatform.creator.data.model.InstallLogEntry;
import com.mobileplatform.creator.data.repository.InstallLogRepository;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 安装日志活动
 * 显示应用安装、更新、卸载的日志记录
 */
public class InstallLogActivity extends AppCompatActivity implements 
        InstallLogAdapter.OnLogItemClickListener,
        InstallLogAdapter.OnLogActionListener {
    
    private static final String TAG = "InstallLogActivity";
    private static final int PERMISSION_REQUEST_WRITE_STORAGE = 1001;
    private static final int PAGE_SIZE = 20; // 每页加载的日志数量
    private int currentPage = 0; // 当前页码
    
    // UI组件
    private RecyclerView recyclerLogs;
    private ProgressBar progressBar;
    private LinearLayout layoutEmpty;
    private TextView tvEmptyText;
    private TabLayout tabLayout;
    private SwipeRefreshLayout swipeRefreshLayout;
    
    // 数据
    private InstallLogRepository logRepository;
    private InstallLogAdapter adapter;
    private List<InstallLogEntry> logEntries = new ArrayList<>();
    
    // 当前选中的过滤类型
    private FilterType currentFilter = FilterType.ALL;
    
    // 是否正在加载数据
    private boolean isLoading = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_install_log);
        
        // 初始化UI组件
        initViews();
        
        // 初始化数据
        initData();
        
        // 加载日志
        loadLogs();
    }
    
    /**
     * 初始化视图
     */
    private void initViews() {
        // 设置工具栏
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("安装日志");
        }
        
        // 初始化下拉刷新
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        swipeRefreshLayout.setColorSchemeResources(
                R.color.colorPrimary,
                R.color.colorInstall,
                R.color.colorUpdate);
        swipeRefreshLayout.setOnRefreshListener(this::refreshLogs);
        
        // 初始化RecyclerView
        recyclerLogs = findViewById(R.id.recycler_logs);
        recyclerLogs.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        
        // 添加滚动监听，支持分页加载
        recyclerLogs.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                // 到达底部时加载更多数据
                if (!recyclerView.canScrollVertically(1) && !isLoading) {
                    loadMoreLogs();
                }
            }
        });
        
        // 初始化空视图和进度条
        progressBar = findViewById(R.id.progress_bar);
        layoutEmpty = findViewById(R.id.layout_empty);
        tvEmptyText = findViewById(R.id.tv_empty_text);
        
        // 初始化清空按钮
        FloatingActionButton fabClearLogs = findViewById(R.id.fab_clear_logs);
        fabClearLogs.setOnClickListener(v -> showClearConfirmDialog());
        
        // 初始化选项卡
        tabLayout = findViewById(R.id.tab_layout);
        setupTabs();
    }
    
    /**
     * 设置过滤选项卡
     */
    private void setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText("全部"));
        tabLayout.addTab(tabLayout.newTab().setText("安装"));
        tabLayout.addTab(tabLayout.newTab().setText("更新"));
        tabLayout.addTab(tabLayout.newTab().setText("卸载"));
        tabLayout.addTab(tabLayout.newTab().setText("失败"));
        
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int position = tab.getPosition();
                switch (position) {
                    case 0:
                        currentFilter = FilterType.ALL;
                        break;
                    case 1:
                        currentFilter = FilterType.INSTALL;
                        break;
                    case 2:
                        currentFilter = FilterType.UPDATE;
                        break;
                    case 3:
                        currentFilter = FilterType.UNINSTALL;
                        break;
                    case 4:
                        currentFilter = FilterType.FAILED;
                        break;
                }
                loadLogs();
            }
            
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }
            
            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                // 重新选中时刷新当前数据
                refreshLogs();
            }
        });
    }
    
    /**
     * 初始化数据
     */
    private void initData() {
        logRepository = InstallLogRepository.getInstance(this);
        adapter = new InstallLogAdapter(this, logEntries);
        adapter.setOnLogItemClickListener(this);
        adapter.setOnLogActionListener(this);
        recyclerLogs.setAdapter(adapter);
    }
    
    /**
     * 加载日志
     */
    private void loadLogs() {
        showProgress(true);
        isLoading = true;
        currentPage = 0; // 重置页码
        
        // 创建通用回调
        InstallLogRepository.LogsCallback callback = new InstallLogRepository.LogsCallback() {
            @Override
            public void onLogsLoaded(List<InstallLogEntry> logs) {
                updateLogs(logs);
                isLoading = false;
            }
            
            @Override
            public void onDataNotAvailable(String errorMessage) {
                handleDataLoadError(errorMessage);
                isLoading = false;
            }
        };
        
        // 根据不同的过滤类型加载日志
        switch (currentFilter) {
            case ALL:
                logRepository.getAllLogs(callback);
                break;
            case INSTALL:
                logRepository.getLogsByOperationType(InstallLogEntry.OperationType.INSTALL, callback);
                break;
            case UPDATE:
                logRepository.getLogsByOperationType(InstallLogEntry.OperationType.UPDATE, callback);
                break;
            case UNINSTALL:
                logRepository.getLogsByOperationType(InstallLogEntry.OperationType.UNINSTALL, callback);
                break;
            case FAILED:
                logRepository.getFailureLogs(callback);
                break;
        }
    }
    
    /**
     * 刷新日志
     */
    private void refreshLogs() {
        swipeRefreshLayout.setRefreshing(true);
        loadLogs();
    }
    
    /**
     * 更新日志列表
     * 
     * @param logs 日志列表
     */
    private void updateLogs(List<InstallLogEntry> logs) {
        showProgress(false);
        swipeRefreshLayout.setRefreshing(false);
        
        if (logs != null) {
            adapter.setLogEntries(logs);
        }
        
        // 更新空视图
        updateEmptyView();
    }
    
    /**
     * 更新空视图状态
     */
    private void updateEmptyView() {
        if (adapter.getItemCount() == 0) {
            layoutEmpty.setVisibility(View.VISIBLE);
            String emptyText = "暂无" + 
                    (currentFilter == FilterType.ALL ? "" : currentFilter.getDisplayName()) + 
                    "日志记录";
            tvEmptyText.setText(emptyText);
        } else {
            layoutEmpty.setVisibility(View.GONE);
        }
    }
    
    /**
     * 处理数据加载错误
     * 
     * @param errorMessage 错误信息
     */
    private void handleDataLoadError(String errorMessage) {
        showProgress(false);
        swipeRefreshLayout.setRefreshing(false);
        Toast.makeText(this, "加载日志失败: " + errorMessage, Toast.LENGTH_SHORT).show();
        updateEmptyView();
    }
    
    /**
     * 显示/隐藏进度条
     * 
     * @param show 是否显示
     */
    private void showProgress(boolean show) {
        progressBar.setVisibility(show && !swipeRefreshLayout.isRefreshing() ? 
                View.VISIBLE : View.GONE);
        if (show) {
            layoutEmpty.setVisibility(View.GONE);
        }
    }
    
    /**
     * 显示清空确认对话框
     */
    private void showClearConfirmDialog() {
        new AlertDialog.Builder(this)
                .setTitle("清空日志")
                .setMessage("确定要清空所有日志记录吗？此操作不可恢复。")
                .setPositiveButton("清空", (dialog, which) -> clearAllLogs())
                .setNegativeButton("取消", null)
                .show();
    }
    
    /**
     * 清空所有日志
     */
    private void clearAllLogs() {
        showProgress(true);
        
        // 备份当前日志以便撤销
        final List<InstallLogEntry> backupLogs = new ArrayList<>();
        for (int i = 0; i < adapter.getItemCount(); i++) {
            backupLogs.add(adapter.getItem(i));
        }
        
        logRepository.clearAllLogs();
        
        // 清空后重新加载
        adapter.setLogEntries(new ArrayList<>());
        updateEmptyView();
        showProgress(false);
        
        Snackbar.make(recyclerLogs, "已清空所有日志", Snackbar.LENGTH_LONG)
                .setAction("撤销", v -> {
                    showProgress(true);
                    // 恢复所有被清空的日志
                    for (InstallLogEntry entry : backupLogs) {
                        logRepository.addLog(entry);
                    }
                    // 刷新显示
                    refreshLogs();
                    Toast.makeText(this, "已恢复" + backupLogs.size() + "条日志记录", Toast.LENGTH_SHORT).show();
                })
                .show();
    }
    
    /**
     * 删除日志
     * 
     * @param logEntry 日志条目
     */
    private void deleteLog(InstallLogEntry logEntry) {
        if (logEntry != null) {
            final InstallLogEntry deletedEntry = logEntry;
            
            // 从数据库中删除
            logRepository.deleteLog(logEntry);
            
            // 从适配器中移除
            List<InstallLogEntry> currentLogs = new ArrayList<>();
            for (int i = 0; i < adapter.getItemCount(); i++) {
                InstallLogEntry entry = adapter.getItem(i);
                if (!entry.getId().equals(logEntry.getId())) {
                    currentLogs.add(entry);
                }
            }
            adapter.setLogEntries(currentLogs);
            updateEmptyView();
            
            // 显示撤销选项
            Snackbar.make(recyclerLogs, "已删除日志记录", Snackbar.LENGTH_LONG)
                    .setAction("撤销", v -> {
                        // 恢复删除的日志
                        logRepository.addLog(deletedEntry);
                        refreshLogs();
                    })
                    .show();
        }
    }
    
    /**
     * 显示日志详情对话框
     * 
     * @param logEntry 日志条目
     */
    private void showLogDetailDialog(InstallLogEntry logEntry) {
        StringBuilder message = new StringBuilder();
        message.append("应用名称: ").append(logEntry.getAppName()).append("\n\n");
        message.append("包名: ").append(logEntry.getPackageName()).append("\n\n");
        message.append("版本: ").append(logEntry.getVersion()).append("\n\n");
        message.append("操作: ").append(logEntry.getOperationType().getDisplayName()).append("\n\n");
        message.append("时间: ").append(logEntry.getFormattedOperationTime()).append("\n\n");
        message.append("状态: ").append(logEntry.getStatusText()).append("\n\n");
        
        if (!logEntry.isSuccess() && logEntry.getErrorMessage() != null && !logEntry.getErrorMessage().isEmpty()) {
            message.append("错误信息: ").append(logEntry.getErrorMessage()).append("\n\n");
        }
        
        if (logEntry.getAdditionalInfo() != null && !logEntry.getAdditionalInfo().isEmpty()) {
            message.append("附加信息: ").append(logEntry.getAdditionalInfo());
        }
        
        new AlertDialog.Builder(this)
                .setTitle("日志详情")
                .setMessage(message.toString())
                .setPositiveButton("确定", null)
                .show();
    }
    
    /**
     * 导出日志
     */
    private void exportLogs() {
        // 检查权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        PERMISSION_REQUEST_WRITE_STORAGE);
                return;
            }
        }
        
        // 执行导出
        performExportLogs();
    }
    
    /**
     * 执行日志导出
     */
    private void performExportLogs() {
        showProgress(true);
        
        // 获取当前筛选的日志
        logRepository.getAllLogs(new InstallLogRepository.LogsCallback() {
            @Override
            public void onLogsLoaded(List<InstallLogEntry> logs) {
                showProgress(false);
                
                if (logs == null || logs.isEmpty()) {
                    Toast.makeText(InstallLogActivity.this, "没有日志可导出", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                exportLogsToCSV(logs);
            }
            
            @Override
            public void onDataNotAvailable(String errorMessage) {
                showProgress(false);
                Toast.makeText(InstallLogActivity.this, 
                        "获取日志失败: " + errorMessage, 
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    /**
     * 将日志导出为CSV文件
     * 
     * @param logs 要导出的日志列表
     */
    private void exportLogsToCSV(List<InstallLogEntry> logs) {
        try {
            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs();
            }
            
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
            String fileName = "install_logs_" + dateFormat.format(new Date()) + ".csv";
            File exportFile = new File(downloadsDir, fileName);
            
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(exportFile))) {
                // 写入CSV表头
                writer.write("ID,应用名称,包名,版本,操作类型,操作时间,状态,错误信息,附加信息");
                writer.newLine();
                
                // 写入数据
                for (InstallLogEntry log : logs) {
                    writer.write(String.format(Locale.getDefault(),
                            "%s,%s,%s,%s,%s,%s,%s,%s,%s",
                            log.getId(),
                            escapeCsvField(log.getAppName()),
                            escapeCsvField(log.getPackageName()),
                            escapeCsvField(log.getVersion()),
                            log.getOperationType() != null ? log.getOperationType().getDisplayName() : "",
                            log.getFormattedOperationTime(),
                            log.isSuccess() ? "成功" : "失败",
                            escapeCsvField(log.getErrorMessage()),
                            escapeCsvField(log.getAdditionalInfo())));
                    writer.newLine();
                }
            }
            
            Snackbar.make(recyclerLogs, 
                    "日志已导出到: " + exportFile.getAbsolutePath(), 
                    Snackbar.LENGTH_LONG).show();
            
        } catch (IOException e) {
            Log.e(TAG, "导出日志失败", e);
            Toast.makeText(InstallLogActivity.this, 
                    "导出日志失败: " + e.getMessage(), 
                    Toast.LENGTH_LONG).show();
        }
    }
    
    /**
     * 转义CSV字段
     */
    private String escapeCsvField(String field) {
        if (field == null) return "";
        if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
            return "\"" + field.replace("\"", "\"\"") + "\"";
        }
        return field;
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_WRITE_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                performExportLogs();
            } else {
                Toast.makeText(this, "需要存储权限才能导出日志", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    @Override
    public void onLogItemClick(InstallLogEntry logEntry, int position) {
        showLogDetailDialog(logEntry);
    }
    
    @Override
    public void onLogAction(InstallLogEntry logEntry, int position, View actionView) {
        // 显示弹出菜单
        PopupMenu popupMenu = new PopupMenu(this, actionView);
        popupMenu.inflate(R.menu.menu_log_item);
        popupMenu.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.action_delete_log) {
                deleteLog(logEntry);
                return true;
            } else if (itemId == R.id.action_view_log_detail) {
                showLogDetailDialog(logEntry);
                return true;
            }
            return false;
        });
        popupMenu.show();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_install_log, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
            return true;
        } else if (id == R.id.action_refresh) {
            refreshLogs();
            return true;
        } else if (id == R.id.action_export_log) {
            exportLogs();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    /**
     * 过滤类型枚举
     */
    private enum FilterType {
        ALL("全部"),
        INSTALL("安装"),
        UPDATE("更新"),
        UNINSTALL("卸载"),
        FAILED("失败");
        
        private final String displayName;
        
        FilterType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    private void loadMoreLogs() {
        if (isLoading) return;
        
        isLoading = true;
        currentPage++;
        
        // 创建加载更多回调
        InstallLogRepository.LogsCallback callback = new InstallLogRepository.LogsCallback() {
            @Override
            public void onLogsLoaded(List<InstallLogEntry> logs) {
                if (logs != null && !logs.isEmpty()) {
                    // 追加加载更多的日志
                    List<InstallLogEntry> currentLogs = new ArrayList<>(adapter.getLogEntries());
                    currentLogs.addAll(logs);
                    adapter.setLogEntries(currentLogs);
                }
                isLoading = false;
            }
            
            @Override
            public void onDataNotAvailable(String errorMessage) {
                Toast.makeText(InstallLogActivity.this, 
                        "加载更多日志失败: " + errorMessage, 
                        Toast.LENGTH_SHORT).show();
                isLoading = false;
            }
        };
        
        // 根据当前过滤类型加载分页数据
        int offset = currentPage * PAGE_SIZE;
        int limit = PAGE_SIZE;
        
        switch (currentFilter) {
            case ALL:
                logRepository.getPagedLogs(offset, limit, callback);
                break;
            case INSTALL:
                logRepository.getPagedLogsByType(InstallLogEntry.OperationType.INSTALL, offset, limit, callback);
                break;
            case UPDATE:
                logRepository.getPagedLogsByType(InstallLogEntry.OperationType.UPDATE, offset, limit, callback);
                break;
            case UNINSTALL:
                logRepository.getPagedLogsByType(InstallLogEntry.OperationType.UNINSTALL, offset, limit, callback);
                break;
            case FAILED:
                logRepository.getPagedFailureLogs(offset, limit, callback);
                break;
        }
    }
} 