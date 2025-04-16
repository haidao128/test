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
import com.mobileplatform.creator.model.LogEntry;
import com.mobileplatform.creator.viewmodel.LogEntryViewModel;

import java.util.Date;

public class InstallLogActivity extends AppCompatActivity {

    private LogEntryViewModel logEntryViewModel;
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
        logEntryViewModel = new ViewModelProvider(this).get(LogEntryViewModel.class);

        // 观察 LiveData 数据变化
        logEntryViewModel.getAllLogs().observe(this, logs -> {
            // 当日志数据更新时，提交给 Adapter
            adapter.submitList(logs);
        });
        
        // 添加一些示例日志数据 (仅用于测试)
        if (logEntryViewModel.getAllLogs().getValue() == null || logEntryViewModel.getAllLogs().getValue().isEmpty()) {
             addSampleLogs();
        }
    }
    
    // 添加示例日志的方法
    private void addSampleLogs() {
        LogEntry log1 = new LogEntry("测试应用1", "com.example.app1", "INSTALL", "SUCCESS", "安装成功");
        LogEntry log2 = new LogEntry("测试应用2", "com.example.app2", "UPDATE", "FAILURE", "更新失败：存储空间不足");
        LogEntry log3 = new LogEntry("测试应用1", "com.example.app1", "UNINSTALL", "SUCCESS", null);
        logEntryViewModel.insert(log1);
        logEntryViewModel.insert(log2);
        logEntryViewModel.insert(log3);
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
        if (type == null) {
            logEntryViewModel.getAllLogs().observe(this, logs -> {
                adapter.submitList(logs);
            });
        } else {
            logEntryViewModel.getLogsByOperationType(type).observe(this, logs -> {
                adapter.submitList(logs);
            });
        }
    }
    */

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
} 