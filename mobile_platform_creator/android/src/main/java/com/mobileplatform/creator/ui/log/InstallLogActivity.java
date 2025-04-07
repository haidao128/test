package com.mobileplatform.creator.ui.log;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider; 
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.mobileplatform.creator.R;
import com.mobileplatform.creator.adapter.LogEntryAdapter;
import com.mobileplatform.creator.data.entity.LogEntry; // 用于示例
import java.util.Date; // 用于示例

public class InstallLogActivity extends AppCompatActivity {

    private InstallLogViewModel installLogViewModel;
    private LogEntryAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_install_log);

        // 设置 Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar_log);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
             getSupportActionBar().setDisplayShowHomeEnabled(true);
            setTitle("安装日志"); // 可以在这里设置标题
        }

        // 初始化 RecyclerView
        RecyclerView recyclerView = findViewById(R.id.recyclerView_logs);
        adapter = new LogEntryAdapter();
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // 获取 ViewModel
        installLogViewModel = new ViewModelProvider(this).get(InstallLogViewModel.class);

        // 观察 LiveData 数据变化
        installLogViewModel.getAllLogs().observe(this, logs -> {
            // 当日志数据更新时，提交给 Adapter
            adapter.submitList(logs);
        });
        
        // 添加一些示例日志数据 (仅用于测试)
        if (installLogViewModel.getAllLogs().getValue() == null || installLogViewModel.getAllLogs().getValue().isEmpty()) {
             addSampleLogs();
        }
    }
    
    // 添加示例日志的方法
    private void addSampleLogs() {
        LogEntry log1 = new LogEntry("com.example.app1", "测试应用1", "1.0", 1, new Date().getTime(), "INSTALL", "SUCCESS", "安装成功");
        LogEntry log2 = new LogEntry("com.example.app2", "测试应用2", "1.1", 2, new Date().getTime() - 10000, "UPDATE", "FAILURE", "更新失败：存储空间不足");
        LogEntry log3 = new LogEntry("com.example.app1", "测试应用1", "1.0", 1, new Date().getTime() - 20000, "UNINSTALL", "SUCCESS", null);
        installLogViewModel.insert(log1);
        installLogViewModel.insert(log2);
        installLogViewModel.insert(log3);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // TODO: 可以添加过滤日志的菜单项
        // getMenuInflater().inflate(R.menu.menu_log, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        // TODO: 处理过滤菜单项的点击事件
        /*
        int id = item.getItemId();
        if (id == R.id.action_filter_all) {
            observeLogs(null);
            return true;
        } else if (id == R.id.action_filter_install) {
             observeLogs("INSTALL");
             return true;
        } // ... 其他过滤 ...
        */
        return super.onOptionsItemSelected(item);
    }
    
    // 观察指定类型的日志
    /*
    private void observeLogs(String type) {
        installLogViewModel.getLogsByType(type).observe(this, logs -> {
            adapter.submitList(logs);
        });
    }
    */

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
} 