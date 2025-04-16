package com.mobileplatform.creator;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.mobileplatform.creator.adapter.AppInfoAdapter;
import com.mobileplatform.creator.model.AppInfo;
import com.mobileplatform.creator.ui.batch.BatchManagerActivity;
import com.mobileplatform.creator.ui.category.CategoryManagerActivity;
import com.mobileplatform.creator.ui.log.InstallLogActivity;
import com.mobileplatform.creator.ui.log.LogTestActivity;
import com.mobileplatform.creator.ui.mpk.MPKCreatorActivity;
import com.mobileplatform.creator.update.UpdateCheckService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 应用的主 Activity。
 * 这是应用的入口点和主要界面。
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private RecyclerView recyclerViewApps;
    private AppInfoAdapter appInfoAdapter;
    private ProgressBar progressBar;
    private List<AppInfo> appList = new ArrayList<>();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 设置 Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // 初始化 ProgressBar
        progressBar = findViewById(R.id.progressBar_loading);

        // 初始化 RecyclerView
        recyclerViewApps = findViewById(R.id.recyclerView_apps);
        recyclerViewApps.setLayoutManager(new LinearLayoutManager(this));

        // 初始化 Adapter (传入空的列表先)
        appInfoAdapter = new AppInfoAdapter(this, appList);
        recyclerViewApps.setAdapter(appInfoAdapter);

        // 设置 FAB 点击事件
        FloatingActionButton fab = findViewById(R.id.fab_create_mpk);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, MPKCreatorActivity.class);
                startActivity(intent);
            }
        });

        // 异步加载应用列表
        loadInstalledApps();
    }

    /**
     * 使用后台线程加载设备上已安装的应用信息。
     */
    private void loadInstalledApps() {
        showLoading(true);
        executorService.execute(() -> {
            PackageManager pm = getPackageManager();
            // 获取所有已安装应用的信息，可以添加 FLAG 来过滤，例如只获取非系统应用
            List<PackageInfo> packages = pm.getInstalledPackages(PackageManager.GET_META_DATA);
            final List<AppInfo> loadedApps = new ArrayList<>();

            for (PackageInfo packageInfo : packages) {
                // --- 暂时移除过滤条件，显示所有应用 ---
                // if ((packageInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) { 
                    try {
                        String appName = packageInfo.applicationInfo.loadLabel(pm).toString();
                        String packageName = packageInfo.packageName;
                        String versionName = packageInfo.versionName;
                        int versionCode = packageInfo.versionCode;
                        Drawable icon = packageInfo.applicationInfo.loadIcon(pm);
                        
                        AppInfo appInfo = new AppInfo(appName, packageName, versionName, versionCode, icon, packageInfo.applicationInfo.sourceDir);
                        loadedApps.add(appInfo);
                    } catch (Exception e) {
                        Log.e(TAG, "Error loading app info: " + e.getMessage());
                    }
                // }
            }

            runOnUiThread(() -> {
                appList.clear();
                appList.addAll(loadedApps);
                appInfoAdapter.notifyDataSetChanged();
                showLoading(false);
            });
        });
    }

    /**
     * 控制加载指示器的显示与隐藏。
     */
    private void showLoading(boolean isLoading) {
        if (progressBar != null) {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
        if (recyclerViewApps != null) {
             recyclerViewApps.setVisibility(isLoading ? View.GONE : View.VISIBLE);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_batch_manager) {
            startActivity(new Intent(this, BatchManagerActivity.class));
            return true;
        } else if (id == R.id.action_category_manager) {
            Intent intent = new Intent(this, CategoryManagerActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_install_log) {
            startActivity(new Intent(this, InstallLogActivity.class));
            return true;
        } else if (id == R.id.action_log_test) {
            startActivity(new Intent(this, LogTestActivity.class));
            return true;
        } else if (id == R.id.action_check_update) {
            Intent updateIntent = new Intent(this, UpdateCheckService.class);
            startService(updateIntent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 关闭线程池，防止内存泄漏
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
} 