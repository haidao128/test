package com.mobileplatform.creator;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.mobileplatform.creator.ui.batch.BatchManagerActivity;
import com.mobileplatform.creator.ui.category.CategoryManagerActivity;
import com.mobileplatform.creator.ui.log.InstallLogActivity;
import com.mobileplatform.creator.ui.mpk.MPKCreatorActivity;
import com.mobileplatform.creator.update.UpdateCheckService; 

// TODO: 创建并替换为实际的应用信息 Adapter
import com.mobileplatform.creator.adapter.AppInfoAdapter; 

/**
 * 应用的主 Activity。
 * 这是应用的入口点和主要界面。
 */
public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerViewApps;
    private AppInfoAdapter appInfoAdapter; // TODO: 需要创建这个 Adapter 类

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 设置 Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // 初始化 RecyclerView
        recyclerViewApps = findViewById(R.id.recyclerView_apps);
        recyclerViewApps.setLayoutManager(new LinearLayoutManager(this));

        // TODO: 初始化并设置 Adapter
        // appInfoAdapter = new AppInfoAdapter(this, new ArrayList<>()); // 假设 Adapter 构造函数
        // recyclerViewApps.setAdapter(appInfoAdapter);
        // TODO: 加载应用列表数据并更新 Adapter

        // 设置 FAB 点击事件
        FloatingActionButton fab = findViewById(R.id.fab_create_mpk);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 跳转到 MPK 创建器 Activity
                Intent intent = new Intent(MainActivity.this, MPKCreatorActivity.class);
                startActivity(intent);
            }
        });

        // TODO: 实现应用列表项点击跳转到 AppDetailActivity 的逻辑
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // 加载菜单资源
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        // 处理菜单项点击事件
        if (id == R.id.action_install_log) {
            startActivity(new Intent(this, InstallLogActivity.class));
            return true;
        } else if (id == R.id.action_category_manager) {
            startActivity(new Intent(this, CategoryManagerActivity.class));
            return true;
        } else if (id == R.id.action_batch_manager) {
            startActivity(new Intent(this, BatchManagerActivity.class));
            return true;
        } else if (id == R.id.action_check_update) {
            // 启动更新检查服务
            Intent updateIntent = new Intent(this, UpdateCheckService.class);
            startService(updateIntent);
            // TODO: 可以加一个提示，比如 Toast
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
} 