package com.mobileplatform.creator.ui.store;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.snackbar.Snackbar;
import com.mobileplatform.creator.R;
import com.mobileplatform.creator.data.StoreAppInfo;
import com.mobileplatform.creator.data.StoreRepository;
import com.mobileplatform.creator.download.DownloadManager;

import java.util.ArrayList;
import java.util.List;

/**
 * 应用商店Fragment
 * 用于展示可下载的应用列表和搜索功能
 */
public class StoreFragment extends Fragment {
    
    private SwipeRefreshLayout swipeRefreshLayout;
    private NestedScrollView nestedScrollView;
    private RecyclerView featuredAppsRecyclerView;
    private RecyclerView allAppsRecyclerView;
    private SearchView searchView;
    private TextView emptyView;
    
    private StoreAppAdapter featuredAdapter;
    private StoreAppAdapter allAppsAdapter;
    
    private List<StoreAppInfo> featuredApps = new ArrayList<>();
    private List<StoreAppInfo> allApps = new ArrayList<>();
    private List<StoreAppInfo> filteredApps = new ArrayList<>();
    
    private StoreRepository storeRepository;
    private Handler mainHandler;
    
    // 下载管理器
    private DownloadManager downloadManager;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_store, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // 初始化UI组件
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout);
        nestedScrollView = view.findViewById(R.id.nested_scroll_view);
        featuredAppsRecyclerView = view.findViewById(R.id.featured_apps_recycler_view);
        allAppsRecyclerView = view.findViewById(R.id.all_apps_recycler_view);
        searchView = view.findViewById(R.id.search_view);
        emptyView = view.findViewById(R.id.empty_view);
        
        // 初始化数据
        storeRepository = StoreRepository.getInstance();
        mainHandler = new Handler(Looper.getMainLooper());
        
        // 初始化下载管理器
        downloadManager = DownloadManager.getInstance(requireContext());
        
        // 设置SwipeRefreshLayout
        swipeRefreshLayout.setOnRefreshListener(this::loadStoreApps);
        swipeRefreshLayout.setColorSchemeResources(R.color.primary, R.color.accent);
        
        // 设置精选应用RecyclerView
        featuredAppsRecyclerView.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        featuredAdapter = new StoreAppAdapter(getContext(), featuredApps, true);
        featuredAdapter.setOnItemClickListener(appItemClickListener);
        featuredAppsRecyclerView.setAdapter(featuredAdapter);
        
        // 设置所有应用RecyclerView
        allAppsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        allAppsAdapter = new StoreAppAdapter(getContext(), filteredApps, false);
        allAppsAdapter.setOnItemClickListener(appItemClickListener);
        allAppsRecyclerView.setAdapter(allAppsAdapter);
        
        // 设置搜索功能
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchApps(query);
                return true;
            }
            
            @Override
            public boolean onQueryTextChange(String newText) {
                if (TextUtils.isEmpty(newText)) {
                    // 当搜索框清空时，恢复显示所有应用
                    filterApps("");
                }
                return true;
            }
        });
        
        // 加载应用数据
        loadStoreApps();
    }
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // ... existing code ...
        
        // 初始化下载管理器
        downloadManager = DownloadManager.getInstance(requireContext());
    }
    
    /**
     * 加载商店应用数据
     */
    private void loadStoreApps() {
        swipeRefreshLayout.setRefreshing(true);
        
        storeRepository.getStoreApps(new StoreRepository.AppListCallback() {
            @Override
            public void onSuccess(List<StoreAppInfo> apps) {
                mainHandler.post(() -> {
                    swipeRefreshLayout.setRefreshing(false);
                    allApps.clear();
                    allApps.addAll(apps);
                    
                    // 提取精选应用
                    featuredApps.clear();
                    for (StoreAppInfo app : apps) {
                        if (app.isFeatured()) {
                            featuredApps.add(app);
                        }
                    }
                    
                    // 更新UI
                    filterApps("");
                    featuredAdapter.notifyDataSetChanged();
                    updateUIState();
                });
            }
            
            @Override
            public void onError(Exception e) {
                mainHandler.post(() -> {
                    swipeRefreshLayout.setRefreshing(false);
                    showErrorMessage(R.string.network_error);
                });
            }
        });
    }
    
    /**
     * 搜索应用
     * @param query 搜索关键词
     */
    private void searchApps(String query) {
        if (TextUtils.isEmpty(query)) {
            filterApps("");
            return;
        }
        
        swipeRefreshLayout.setRefreshing(true);
        storeRepository.searchStoreApps(query, new StoreRepository.AppListCallback() {
            @Override
            public void onSuccess(List<StoreAppInfo> apps) {
                mainHandler.post(() -> {
                    swipeRefreshLayout.setRefreshing(false);
                    filteredApps.clear();
                    filteredApps.addAll(apps);
                    allAppsAdapter.notifyDataSetChanged();
                    updateUIState();
                });
            }
            
            @Override
            public void onError(Exception e) {
                mainHandler.post(() -> {
                    swipeRefreshLayout.setRefreshing(false);
                    showErrorMessage(R.string.network_error);
                });
            }
        });
    }
    
    /**
     * 根据关键词过滤应用列表
     * @param keyword 过滤关键词
     */
    private void filterApps(String keyword) {
        filteredApps.clear();
        
        if (TextUtils.isEmpty(keyword)) {
            filteredApps.addAll(allApps);
        } else {
            for (StoreAppInfo app : allApps) {
                if (app.getName().toLowerCase().contains(keyword.toLowerCase()) || 
                    app.getDeveloper().toLowerCase().contains(keyword.toLowerCase()) ||
                    app.getDescription().toLowerCase().contains(keyword.toLowerCase())) {
                    filteredApps.add(app);
                }
            }
        }
        
        allAppsAdapter.notifyDataSetChanged();
        updateUIState();
    }
    
    /**
     * 更新UI状态
     */
    private void updateUIState() {
        if (featuredApps.isEmpty() && filteredApps.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            featuredAppsRecyclerView.setVisibility(View.GONE);
            allAppsRecyclerView.setVisibility(View.GONE);
        } else {
            emptyView.setVisibility(View.GONE);
            
            if (featuredApps.isEmpty()) {
                featuredAppsRecyclerView.setVisibility(View.GONE);
                view.findViewById(R.id.featured_title).setVisibility(View.GONE);
            } else {
                featuredAppsRecyclerView.setVisibility(View.VISIBLE);
                view.findViewById(R.id.featured_title).setVisibility(View.VISIBLE);
            }
            
            if (filteredApps.isEmpty()) {
                allAppsRecyclerView.setVisibility(View.GONE);
                view.findViewById(R.id.all_apps_title).setVisibility(View.GONE);
            } else {
                allAppsRecyclerView.setVisibility(View.VISIBLE);
                view.findViewById(R.id.all_apps_title).setVisibility(View.VISIBLE);
            }
        }
    }
    
    // 修改应用适配器中的点击事件
    private StoreAppAdapter.OnItemClickListener appItemClickListener = new StoreAppAdapter.OnItemClickListener() {
        @Override
        public void onItemClick(StoreAppInfo appInfo) {
            // 显示应用详情
            showAppDetails(appInfo);
        }
        
        @Override
        public void onActionButtonClick(StoreAppInfo appInfo) {
            // 根据当前状态执行操作
            switch (appInfo.getDownloadStatus()) {
                case StoreAppInfo.DOWNLOAD_STATUS_NOT_DOWNLOAD:
                    // 开始下载
                    downloadApp(appInfo);
                    break;
                    
                case StoreAppInfo.DOWNLOAD_STATUS_DOWNLOADING:
                    // 跳转到下载管理页面
                    navigateToDownloadFragment();
                    break;
                    
                case StoreAppInfo.DOWNLOAD_STATUS_DOWNLOADED:
                    // 安装应用
                    installApp(appInfo);
                    break;
                    
                case StoreAppInfo.DOWNLOAD_STATUS_INSTALLED:
                    // 打开应用
                    openApp(appInfo);
                    break;
            }
        }
    };
    
    /**
     * 下载应用
     * 
     * @param appInfo 应用信息
     */
    private void downloadApp(StoreAppInfo appInfo) {
        if (appInfo == null || appInfo.getDownloadUrl() == null) {
            Toast.makeText(requireContext(), "下载链接不可用", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 更新下载状态
        appInfo.setDownloadStatus(StoreAppInfo.DOWNLOAD_STATUS_DOWNLOADING);
        
        // 添加下载任务
        downloadManager.addTask(appInfo);
        
        // 提示用户
        Toast.makeText(requireContext(), "开始下载：" + appInfo.getName(), Toast.LENGTH_SHORT).show();
    }
    
    /**
     * 跳转到下载管理页面
     */
    private void navigateToDownloadFragment() {
        // 使用 Navigation 或其他方式跳转到下载管理页面
        ((MainActivity) requireActivity()).navigateToDownload();
    }
    
    /**
     * 安装应用
     * 
     * @param appInfo 应用信息
     */
    private void installApp(StoreAppInfo appInfo) {
        // 这里可以实现安装逻辑
        Toast.makeText(requireContext(), "安装应用：" + appInfo.getName(), Toast.LENGTH_SHORT).show();
    }
    
    /**
     * 打开应用
     * 
     * @param appInfo 应用信息
     */
    private void openApp(StoreAppInfo appInfo) {
        // 这里可以实现打开应用的逻辑
        Toast.makeText(requireContext(), "打开应用：" + appInfo.getName(), Toast.LENGTH_SHORT).show();
    }
    
    /**
     * 显示应用详情
     */
    private void showAppDetails(StoreAppInfo app) {
        // 这里应该打开应用详情页面
        // 由于我们尚未实现应用详情页面，这里只显示一个提示
        showInfoMessage("应用详情：" + app.getName());
    }
    
    /**
     * 显示错误消息
     */
    private void showErrorMessage(int stringResId) {
        if (getView() != null) {
            Snackbar.make(getView(), stringResId, Snackbar.LENGTH_LONG)
                    .setBackgroundTint(getResources().getColor(R.color.error))
                    .setTextColor(getResources().getColor(R.color.surface))
                    .show();
        }
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
} 