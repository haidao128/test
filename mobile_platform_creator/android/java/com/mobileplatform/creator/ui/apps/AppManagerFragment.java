package com.mobileplatform.creator.ui.apps;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.mobileplatform.creator.R;
import com.mobileplatform.creator.data.AppRepository;
import com.mobileplatform.creator.data.model.AppInfo;
import com.mobileplatform.creator.utils.AppInstaller;
import com.mobileplatform.creator.utils.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 应用管理Fragment
 * 显示已安装的应用列表，提供应用安装、卸载、启动等功能
 */
public class AppManagerFragment extends Fragment implements AppAdapter.OnItemClickListener {
    
    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView emptyView;
    private FloatingActionButton fabInstall;
    
    private AppAdapter adapter;
    private List<AppInfo> appList = new ArrayList<>();
    
    private AppRepository appRepository;
    private Handler mainHandler;
    
    // 文件选择请求码
    private static final int REQUEST_SELECT_FILE = 1001;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_app_manager, container, false);
        
        // 初始化视图
        recyclerView = view.findViewById(R.id.recycler_view);
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout);
        emptyView = view.findViewById(R.id.empty_view);
        fabInstall = view.findViewById(R.id.fab_install);
        
        // 初始化数据
        appRepository = AppRepository.getInstance();
        mainHandler = new Handler(Looper.getMainLooper());
        
        // 设置RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new AppAdapter(getContext(), appList);
        adapter.setOnItemClickListener(this);
        recyclerView.setAdapter(adapter);
        
        // 设置下拉刷新
        swipeRefreshLayout.setOnRefreshListener(this::loadInstalledApps);
        swipeRefreshLayout.setColorSchemeResources(R.color.primary, R.color.accent);
        
        // 设置安装按钮
        fabInstall.setOnClickListener(v -> openFilePicker());
        
        return view;
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        loadInstalledApps();
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // 每次返回这个页面时刷新应用列表
        loadInstalledApps();
    }
    
    /**
     * 加载已安装应用列表
     */
    private void loadInstalledApps() {
        swipeRefreshLayout.setRefreshing(true);
        
        appRepository.getInstalledApps(getContext(), new AppRepository.AppListCallback() {
            @Override
            public void onSuccess(List<AppInfo> apps) {
                mainHandler.post(() -> {
                    swipeRefreshLayout.setRefreshing(false);
                    appList.clear();
                    
                    if (apps != null) {
                        appList.addAll(apps);
                    }
                    
                    adapter.notifyDataSetChanged();
                    updateEmptyView();
                });
            }
            
            @Override
            public void onError(Exception e) {
                mainHandler.post(() -> {
                    swipeRefreshLayout.setRefreshing(false);
                    showErrorMessage(e.getMessage());
                    updateEmptyView();
                });
            }
        });
    }
    
    /**
     * 更新空视图状态
     */
    private void updateEmptyView() {
        if (appList.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }
    
    /**
     * 打开文件选择器
     */
    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        
        try {
            startActivityForResult(
                    Intent.createChooser(intent, "选择应用安装包"),
                    REQUEST_SELECT_FILE);
        } catch (android.content.ActivityNotFoundException ex) {
            showErrorMessage("请安装文件管理器应用");
        }
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == REQUEST_SELECT_FILE && resultCode == getActivity().RESULT_OK) {
            if (data != null && data.getData() != null) {
                Uri fileUri = data.getData();
                String filePath = FileUtils.getPathFromUri(getContext(), fileUri);
                
                if (filePath != null && (filePath.endsWith(".mpk") || filePath.endsWith(".apk"))) {
                    installApplication(filePath);
                } else {
                    showErrorMessage("不支持的文件格式");
                }
            }
        }
    }
    
    /**
     * 安装应用
     */
    private void installApplication(String filePath) {
        showInfoMessage("开始安装应用...");
        
        // 根据文件后缀选择安装方式
        if (filePath.endsWith(".mpk")) {
            installMpkPackage(filePath);
        } else if (filePath.endsWith(".apk")) {
            installApkFile(filePath);
        } else {
            showErrorMessage("不支持的文件格式");
        }
    }
    
    /**
     * 安装MPK包
     */
    private void installMpkPackage(String filePath) {
        AppInstaller.installMpkPackage(getContext(), filePath, new AppInstaller.InstallCallback() {
            @Override
            public void onInstalled(AppInfo appInfo) {
                mainHandler.post(() -> {
                    showSuccessMessage(getString(R.string.install_success));
                    loadInstalledApps();
                });
            }
            
            @Override
            public void onUpdated(AppInfo appInfo) {
                mainHandler.post(() -> {
                    showSuccessMessage("应用 " + appInfo.getName() + " 已更新");
                    loadInstalledApps();
                });
            }
            
            @Override
            public void onError(String error) {
                mainHandler.post(() -> {
                    showErrorMessage(getString(R.string.install_failed) + ": " + error);
                });
            }
        });
    }
    
    /**
     * 安装APK文件
     */
    private void installApkFile(String filePath) {
        File apkFile = new File(filePath);
        if (!apkFile.exists()) {
            showErrorMessage("文件不存在");
            return;
        }
        
        AppInstaller.installApkFile(getContext(), apkFile, new AppInstaller.InstallCallback() {
            @Override
            public void onInstalled(AppInfo appInfo) {
                mainHandler.post(() -> {
                    showSuccessMessage(getString(R.string.install_success));
                    loadInstalledApps();
                });
            }
            
            @Override
            public void onUpdated(AppInfo appInfo) {
                mainHandler.post(() -> {
                    showSuccessMessage("应用 " + appInfo.getName() + " 已更新");
                    loadInstalledApps();
                });
            }
            
            @Override
            public void onError(String error) {
                mainHandler.post(() -> {
                    showErrorMessage(getString(R.string.install_failed) + ": " + error);
                });
            }
        });
    }
    
    /**
     * 打开应用
     */
    private void openApp(AppInfo appInfo) {
        try {
            Intent launchIntent = getContext().getPackageManager().getLaunchIntentForPackage(appInfo.getPackageName());
            if (launchIntent != null) {
                startActivity(launchIntent);
            } else {
                showErrorMessage("无法启动应用，未找到入口活动");
            }
        } catch (Exception e) {
            showErrorMessage("启动应用失败: " + e.getMessage());
        }
    }
    
    /**
     * 分享应用
     */
    private void shareApp(AppInfo appInfo) {
        try {
            File appFile = new File(appInfo.getInstallPath());
            if (!appFile.exists()) {
                showErrorMessage("应用文件不存在");
                return;
            }
            
            Uri fileUri = FileProvider.getUriForFile(
                    getContext(), 
                    getContext().getPackageName() + ".fileprovider",
                    appFile);
            
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("application/vnd.android.package-archive");
            shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            
            startActivity(Intent.createChooser(shareIntent, "分享应用"));
            
        } catch (Exception e) {
            showErrorMessage("分享应用失败: " + e.getMessage());
        }
    }
    
    /**
     * 卸载应用
     */
    private void uninstallApp(AppInfo appInfo) {
        AppInstaller.uninstallApp(getContext(), appInfo.getPackageName(), new AppInstaller.UninstallCallback() {
            @Override
            public void onUninstalled(String packageName) {
                mainHandler.post(() -> {
                    showSuccessMessage(getString(R.string.uninstall_success));
                    loadInstalledApps();
                });
            }
            
            @Override
            public void onError(String error) {
                mainHandler.post(() -> {
                    showErrorMessage(getString(R.string.uninstall_failed) + ": " + error);
                });
            }
        });
    }
    
    /**
     * 显示应用详情对话框
     */
    private void showAppDetails(AppInfo appInfo) {
        AppDetailsDialogFragment dialog = AppDetailsDialogFragment.newInstance(appInfo);
        dialog.setAppActionListener(new AppDetailsDialogFragment.AppActionListener() {
            @Override
            public void onOpenApp(AppInfo appInfo) {
                openApp(appInfo);
            }
            
            @Override
            public void onShareApp(AppInfo appInfo) {
                shareApp(appInfo);
            }
            
            @Override
            public void onUninstallApp(AppInfo appInfo) {
                uninstallApp(appInfo);
            }
        });
        dialog.show(getChildFragmentManager(), "app_details");
    }
    
    /**
     * 显示成功消息
     */
    private void showSuccessMessage(String message) {
        if (getView() != null) {
            Snackbar.make(getView(), message, Snackbar.LENGTH_SHORT)
                    .setBackgroundTint(getResources().getColor(R.color.success))
                    .setTextColor(getResources().getColor(R.color.surface))
                    .show();
        }
    }
    
    /**
     * 显示错误消息
     */
    private void showErrorMessage(String message) {
        if (getView() != null) {
            Snackbar.make(getView(), message, Snackbar.LENGTH_LONG)
                    .setBackgroundTint(getResources().getColor(R.color.error))
                    .setTextColor(getResources().getColor(R.color.surface))
                    .show();
        }
    }
    
    /**
     * 显示信息消息
     */
    private void showInfoMessage(String message) {
        if (getView() != null) {
            Snackbar.make(getView(), message, Snackbar.LENGTH_SHORT)
                    .setBackgroundTint(getResources().getColor(R.color.info))
                    .setTextColor(getResources().getColor(R.color.surface))
                    .show();
        }
    }
    
    @Override
    public void onItemClick(AppInfo appInfo) {
        showAppDetails(appInfo);
    }
    
    @Override
    public void onActionClick(AppInfo appInfo, int position, int actionType) {
        switch (actionType) {
            case AppAdapter.ACTION_OPEN:
                openApp(appInfo);
                break;
                
            case AppAdapter.ACTION_UNINSTALL:
                uninstallApp(appInfo);
                break;
        }
    }
} 