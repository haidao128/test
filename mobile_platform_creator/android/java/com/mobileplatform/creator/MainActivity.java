package com.mobileplatform.creator;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.mobileplatform.creator.ui.ViewPagerAdapter;
import com.mobileplatform.creator.ui.apps.AppsFragment;
import com.mobileplatform.creator.ui.sandbox.SandboxFragment;
import com.mobileplatform.creator.ui.settings.SettingsFragment;
import com.mobileplatform.creator.ui.store.StoreFragment;

/**
 * 主活动，包含底部导航和主要界面框架
 */
public class MainActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private BottomNavigationView bottomNav;
    private FloatingActionButton fab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 设置工具栏
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // 初始化视图
        viewPager = findViewById(R.id.view_pager);
        bottomNav = findViewById(R.id.bottom_nav);
        fab = findViewById(R.id.fab);

        // 设置ViewPager
        setupViewPager();

        // 设置底部导航
        setupBottomNavigation();

        // 设置浮动按钮
        setupFloatingActionButton();
    }

    /**
     * 设置ViewPager和适配器
     */
    private void setupViewPager() {
        ViewPagerAdapter adapter = new ViewPagerAdapter(this);
        
        // 添加Fragment
        adapter.addFragment(new AppsFragment());
        adapter.addFragment(new StoreFragment());
        adapter.addFragment(new SandboxFragment());
        adapter.addFragment(new SettingsFragment());
        
        // 禁用ViewPager滑动（避免与内部滚动冲突）
        viewPager.setUserInputEnabled(false);
        
        // 设置适配器
        viewPager.setAdapter(adapter);
        
        // 页面变化监听器
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                // 更新底部导航选中项
                bottomNav.getMenu().getItem(position).setChecked(true);
                
                // 根据页面显示/隐藏FAB
                updateFabVisibility(position);
            }
        });
    }

    /**
     * 设置底部导航
     */
    private void setupBottomNavigation() {
        bottomNav.setOnNavigationItemSelectedListener(item -> {
            int position = 0;
            
            if (item.getItemId() == R.id.nav_apps) {
                position = 0;
            } else if (item.getItemId() == R.id.nav_store) {
                position = 1;
            } else if (item.getItemId() == R.id.nav_sandbox) {
                position = 2;
            } else if (item.getItemId() == R.id.nav_settings) {
                position = 3;
            }
            
            // 切换ViewPager页面
            viewPager.setCurrentItem(position, false);
            return true;
        });
    }

    /**
     * 设置浮动操作按钮
     */
    private void setupFloatingActionButton() {
        fab.setOnClickListener(v -> {
            int currentPage = viewPager.getCurrentItem();
            
            switch (currentPage) {
                case 0: // 应用页面
                    showAppOptions();
                    break;
                case 1: // 商店页面
                    // 在商店页面FAB不执行操作
                    break;
                case 2: // 沙箱页面
                    // 在沙箱页面FAB不执行操作
                    break;
                case 3: // 设置页面
                    // 在设置页面FAB不执行操作
                    break;
            }
        });
        
        // 初始更新FAB可见性
        updateFabVisibility(0);
    }
    
    /**
     * 根据页面位置更新FAB可见性
     */
    private void updateFabVisibility(int position) {
        if (position == 0) { // 仅在应用页面显示
            fab.show();
        } else {
            fab.hide();
        }
    }

    /**
     * 显示应用选项菜单
     */
    private void showAppOptions() {
        // 创建对话框
        String[] options = {"安装应用", "创建MPK应用包"};
        
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("应用操作")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0: // 安装应用
                            // TODO: 实现应用安装
                            Toast.makeText(this, "安装应用", Toast.LENGTH_SHORT).show();
                            break;
                        case 1: // 创建MPK应用包
                            navigateToMPKCreator();
                            break;
                    }
                })
                .show();
    }

    /**
     * 导航到MPK创建器
     */
    public void navigateToMPKCreator() {
        Intent intent = new Intent(this, com.mobileplatform.creator.ui.mpk.MPKCreatorActivity.class);
        startActivity(intent);
    }

    /**
     * 导航到下载管理页面
     */
    public void navigateToDownload() {
        // 切换到应用管理tab
        bottomNav.setSelectedItemId(R.id.nav_apps);
        
        // 启动下载管理Activity或Fragment
        Intent intent = new Intent(this, DownloadActivity.class);
        startActivity(intent);
    }

    /**
     * 导航到批量管理页面
     */
    public void navigateToBatchManager() {
        Intent intent = new Intent(this, BatchManagerActivity.class);
        startActivity(intent);
    }

    /**
     * 导航到分类管理页面
     */
    public void navigateToCategoryManager() {
        Intent intent = new Intent(this, com.mobileplatform.creator.ui.category.CategoryManagerActivity.class);
        startActivity(intent);
    }

    /**
     * 导航到安装日志页面
     */
    private void navigateToInstallLog() {
        Intent intent = new Intent(this, InstallLogActivity.class);
        startActivity(intent);
    }

    /**
     * 显示关于对话框
     */
    private void showAboutDialog() {
        new com.mobileplatform.creator.ui.settings.AboutDialogFragment()
                .show(getSupportFragmentManager(), "about");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_about) {
            // 显示关于信息
            showAboutDialog();
            return true;
        } else if (id == R.id.action_install_log) {
            // 跳转到安装日志页面
            navigateToInstallLog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
} 