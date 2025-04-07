package com.mobileplatform.creator.ui.apps;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.mobileplatform.creator.R;
import com.mobileplatform.creator.data.AppRepository;
import com.mobileplatform.creator.data.model.AppInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * 应用列表Fragment，显示已安装的应用
 */
public class AppsFragment extends Fragment {

    private RecyclerView recyclerView;
    private TextView emptyView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private AppListAdapter adapter;
    private List<AppInfo> appList = new ArrayList<>();
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_apps, container, false);
        
        // 初始化视图
        recyclerView = root.findViewById(R.id.recycler_view);
        emptyView = root.findViewById(R.id.empty_view);
        swipeRefreshLayout = root.findViewById(R.id.swipe_refresh);
        
        // 设置布局管理器
        int spanCount = 2; // 每行显示的应用数量
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), spanCount));
        
        // 设置适配器
        adapter = new AppListAdapter(getContext(), appList);
        recyclerView.setAdapter(adapter);
        
        // 设置点击监听器
        adapter.setOnItemClickListener(new AppListAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(AppInfo appInfo) {
                showAppDetails(appInfo);
            }
            
            @Override
            public void onActionClick(AppInfo appInfo, int actionType) {
                switch (actionType) {
                    case AppListAdapter.ACTION_OPEN:
                        openApp(appInfo);
                        break;
                    case AppListAdapter.ACTION_UNINSTALL:
                        uninstallApp(appInfo);
                        break;
                }
            }
        });
        
        // 设置下拉刷新
        swipeRefreshLayout.setOnRefreshListener(this::loadApps);
        swipeRefreshLayout.setColorSchemeResources(
                R.color.primary,
                R.color.accent,
                R.color.primary_dark
        );
        
        return root;
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // 加载应用列表
        loadApps();
    }
    
    /**
     * 加载应用列表
     */
    private void loadApps() {
        swipeRefreshLayout.setRefreshing(true);
        
        AppRepository.getInstance().getInstalledApps(getContext(), (apps, error) -> {
            if (getActivity() == null) return;
            
            getActivity().runOnUiThread(() -> {
                swipeRefreshLayout.setRefreshing(false);
                
                if (error != null) {
                    // 显示错误提示
                    showError(error);
                    return;
                }
                
                // 更新应用列表
                appList.clear();
                if (apps != null) {
                    appList.addAll(apps);
                }
                adapter.notifyDataSetChanged();
                
                // 更新空视图
                updateEmptyView();
            });
        });
    }
    
    /**
     * 更新空视图
     */
    private void updateEmptyView() {
        if (appList.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
        }
    }
    
    /**
     * 显示应用详情
     */
    private void showAppDetails(AppInfo appInfo) {
        // TODO: 打开应用详情页面
    }
    
    /**
     * 打开应用
     */
    private void openApp(AppInfo appInfo) {
        // TODO: 调用沙箱服务打开应用
    }
    
    /**
     * 卸载应用
     */
    private void uninstallApp(AppInfo appInfo) {
        // TODO: 显示卸载确认对话框
    }
    
    /**
     * 显示错误提示
     */
    private void showError(String message) {
        // TODO: 显示错误提示对话框或Snackbar
    }
} 