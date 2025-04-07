package com.mobileplatform.creator.ui.download;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.tabs.TabLayout;
import com.mobileplatform.creator.R;
import com.mobileplatform.creator.data.model.DownloadTask;
import com.mobileplatform.creator.download.DownloadManager;

import java.util.ArrayList;
import java.util.List;

/**
 * 下载管理界面Fragment
 */
public class DownloadFragment extends Fragment implements DownloadManager.DownloadListener {
    private static final String TAG = "DownloadFragment";
    
    // Tab 位置常量
    private static final int TAB_ALL = 0;
    private static final int TAB_DOWNLOADING = 1;
    private static final int TAB_COMPLETED = 2;
    private static final int TAB_FAILED = 3;
    
    // UI组件
    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView recyclerView;
    private TextView emptyView;
    private TabLayout tabLayout;
    
    // 适配器
    private DownloadAdapter adapter;
    
    // 当前选中的Tab
    private int currentTab = TAB_ALL;
    
    // 下载管理器
    private DownloadManager downloadManager;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_download, container, false);
        
        // 初始化UI组件
        swipeRefreshLayout = root.findViewById(R.id.swipe_refresh_layout);
        recyclerView = root.findViewById(R.id.recycler_view);
        emptyView = root.findViewById(R.id.empty_view);
        tabLayout = root.findViewById(R.id.tab_layout);
        
        // 设置下拉刷新
        swipeRefreshLayout.setOnRefreshListener(this::refreshDownloadList);
        swipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary, R.color.colorAccent);
        
        // 设置RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new DownloadAdapter(requireContext(), new ArrayList<>());
        adapter.setOnItemActionListener(new DownloadAdapter.OnItemActionListener() {
            @Override
            public void onPauseClick(DownloadTask task) {
                pauseDownload(task);
            }
            
            @Override
            public void onResumeClick(DownloadTask task) {
                resumeDownload(task);
            }
            
            @Override
            public void onCancelClick(DownloadTask task) {
                cancelDownload(task);
            }
            
            @Override
            public void onRetryClick(DownloadTask task) {
                retryDownload(task);
            }
            
            @Override
            public void onInstallClick(DownloadTask task) {
                installDownload(task);
            }
        });
        recyclerView.setAdapter(adapter);
        
        // 设置Tab
        setupTabLayout();
        
        // 获取下载管理器
        downloadManager = DownloadManager.getInstance(requireContext());
        downloadManager.addDownloadListener(this);
        
        return root;
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // 刷新下载列表
        refreshDownloadList();
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        // 移除下载监听器
        if (downloadManager != null) {
            downloadManager.removeDownloadListener(this);
        }
    }
    
    /**
     * 设置Tab布局
     */
    private void setupTabLayout() {
        // 添加Tab
        tabLayout.addTab(tabLayout.newTab().setText("全部"));
        tabLayout.addTab(tabLayout.newTab().setText("下载中"));
        tabLayout.addTab(tabLayout.newTab().setText("已完成"));
        tabLayout.addTab(tabLayout.newTab().setText("已失败"));
        
        // 设置Tab选择监听器
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentTab = tab.getPosition();
                refreshDownloadList();
            }
            
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                // 未选中Tab
            }
            
            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                // 再次选择Tab
            }
        });
    }
    
    /**
     * 刷新下载列表
     */
    private void refreshDownloadList() {
        if (downloadManager == null) {
            return;
        }
        
        // 显示刷新指示器
        swipeRefreshLayout.setRefreshing(true);
        
        // 根据当前选中的Tab获取任务列表
        List<DownloadTask> tasks;
        switch (currentTab) {
            case TAB_DOWNLOADING:
                tasks = downloadManager.getRunningAndPendingTasks();
                break;
            case TAB_COMPLETED:
                tasks = downloadManager.getCompletedTasks();
                break;
            case TAB_FAILED:
                tasks = downloadManager.getFailedTasks();
                break;
            default:
                tasks = downloadManager.getAllTasks();
                break;
        }
        
        // 更新适配器数据
        adapter.updateData(tasks);
        
        // 更新空视图
        updateEmptyView(tasks.isEmpty());
        
        // 隐藏刷新指示器
        swipeRefreshLayout.setRefreshing(false);
        
        Log.d(TAG, "刷新下载列表: " + tasks.size() + " 个任务");
    }
    
    /**
     * 更新空视图
     * 
     * @param isEmpty 是否为空
     */
    private void updateEmptyView(boolean isEmpty) {
        if (isEmpty) {
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
            
            // 根据当前Tab更新空视图文本
            switch (currentTab) {
                case TAB_DOWNLOADING:
                    emptyView.setText("没有正在下载的任务");
                    break;
                case TAB_COMPLETED:
                    emptyView.setText("没有已完成的下载任务");
                    break;
                case TAB_FAILED:
                    emptyView.setText("没有失败的下载任务");
                    break;
                default:
                    emptyView.setText("没有下载任务");
                    break;
            }
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
        }
    }
    
    /**
     * 暂停下载
     * 
     * @param task 下载任务
     */
    private void pauseDownload(DownloadTask task) {
        if (downloadManager != null && task != null) {
            downloadManager.pauseTask(task.getId());
            Log.d(TAG, "暂停下载: " + task.getFileName());
        }
    }
    
    /**
     * 恢复下载
     * 
     * @param task 下载任务
     */
    private void resumeDownload(DownloadTask task) {
        if (downloadManager != null && task != null) {
            downloadManager.resumeTask(task.getId());
            Log.d(TAG, "恢复下载: " + task.getFileName());
        }
    }
    
    /**
     * 取消下载
     * 
     * @param task 下载任务
     */
    private void cancelDownload(DownloadTask task) {
        if (downloadManager != null && task != null) {
            downloadManager.cancelTask(task.getId());
            Log.d(TAG, "取消下载: " + task.getFileName());
        }
    }
    
    /**
     * 重试下载
     * 
     * @param task 下载任务
     */
    private void retryDownload(DownloadTask task) {
        if (downloadManager != null && task != null) {
            downloadManager.retryTask(task.getId());
            Log.d(TAG, "重试下载: " + task.getFileName());
        }
    }
    
    /**
     * 安装下载
     * 
     * @param task 下载任务
     */
    private void installDownload(DownloadTask task) {
        if (downloadManager != null && task != null && task.isCompleted()) {
            downloadManager.installTask(task.getId());
            Log.d(TAG, "安装下载: " + task.getFileName());
        }
    }
    
    @Override
    public void onDownloadAdded(DownloadTask task) {
        // 如果当前Tab是全部或下载中，则刷新列表
        if (currentTab == TAB_ALL || currentTab == TAB_DOWNLOADING) {
            requireActivity().runOnUiThread(this::refreshDownloadList);
        }
    }
    
    @Override
    public void onDownloadUpdated(DownloadTask task) {
        // 通知适配器更新特定位置
        requireActivity().runOnUiThread(() -> {
            adapter.updateTask(task);
        });
    }
    
    @Override
    public void onDownloadPaused(DownloadTask task) {
        // 通知适配器更新特定位置
        requireActivity().runOnUiThread(() -> {
            adapter.updateTask(task);
        });
    }
    
    @Override
    public void onDownloadResumed(DownloadTask task) {
        // 通知适配器更新特定位置
        requireActivity().runOnUiThread(() -> {
            adapter.updateTask(task);
        });
    }
    
    @Override
    public void onDownloadCompleted(DownloadTask task) {
        // 如果当前Tab不是已完成，则可能需要从列表中移除
        requireActivity().runOnUiThread(() -> {
            if (currentTab == TAB_DOWNLOADING) {
                adapter.removeTask(task);
                updateEmptyView(adapter.getItemCount() == 0);
            } else {
                adapter.updateTask(task);
            }
        });
    }
    
    @Override
    public void onDownloadFailed(DownloadTask task) {
        // 如果当前Tab不是已失败，则可能需要从列表中移除
        requireActivity().runOnUiThread(() -> {
            if (currentTab == TAB_DOWNLOADING) {
                adapter.removeTask(task);
                updateEmptyView(adapter.getItemCount() == 0);
            } else {
                adapter.updateTask(task);
            }
        });
    }
    
    @Override
    public void onDownloadCancelled(DownloadTask task) {
        // 从列表中移除取消的任务
        requireActivity().runOnUiThread(() -> {
            adapter.removeTask(task);
            updateEmptyView(adapter.getItemCount() == 0);
        });
    }
} 