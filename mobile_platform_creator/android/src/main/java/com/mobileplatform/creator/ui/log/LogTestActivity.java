package com.mobileplatform.creator.ui.log;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;

import com.mobileplatform.creator.R;
import com.mobileplatform.creator.model.LogEntry;
import com.mobileplatform.creator.viewmodel.LogEntryViewModel;

public class LogTestActivity extends AppCompatActivity {

    private LogEntryViewModel logEntryViewModel;
    private EditText packageNameEditText;
    private EditText appNameEditText;
    private EditText versionNameEditText;
    private EditText versionCodeEditText;
    private Spinner operationTypeSpinner;
    private Spinner statusSpinner;
    private EditText detailsEditText;
    private Button addLogButton;
    private Button viewLogsButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_test);

        // 设置 Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar_log_test);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            setTitle("日志测试");
        }

        // 初始化 ViewModel
        logEntryViewModel = new ViewModelProvider(this).get(LogEntryViewModel.class);

        // 初始化视图
        initViews();
        
        // 设置操作类型下拉列表
        setupOperationTypeSpinner();
        
        // 设置状态下拉列表
        setupStatusSpinner();
        
        // 设置按钮点击事件
        setupButtonListeners();
    }

    private void initViews() {
        packageNameEditText = findViewById(R.id.editText_package_name);
        appNameEditText = findViewById(R.id.editText_app_name);
        versionNameEditText = findViewById(R.id.editText_version_name);
        versionCodeEditText = findViewById(R.id.editText_version_code);
        operationTypeSpinner = findViewById(R.id.spinner_operation_type);
        statusSpinner = findViewById(R.id.spinner_status);
        detailsEditText = findViewById(R.id.editText_details);
        addLogButton = findViewById(R.id.button_add_log);
        viewLogsButton = findViewById(R.id.button_view_logs);
    }

    private void setupOperationTypeSpinner() {
        String[] operationTypes = {"INSTALL", "UPDATE", "UNINSTALL", "FAILED"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, operationTypes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        operationTypeSpinner.setAdapter(adapter);
    }

    private void setupStatusSpinner() {
        String[] statuses = {"SUCCESS", "FAILURE"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, statuses);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        statusSpinner.setAdapter(adapter);
    }

    private void setupButtonListeners() {
        addLogButton.setOnClickListener(v -> addLog());
        viewLogsButton.setOnClickListener(v -> viewLogs());
    }

    private void addLog() {
        // 获取输入值
        String packageName = packageNameEditText.getText().toString();
        String appName = appNameEditText.getText().toString();
        String versionName = versionNameEditText.getText().toString();
        String versionCodeStr = versionCodeEditText.getText().toString();
        String operationType = operationTypeSpinner.getSelectedItem().toString();
        String status = statusSpinner.getSelectedItem().toString();
        String details = detailsEditText.getText().toString();

        // 验证输入
        if (packageName.isEmpty() || appName.isEmpty()) {
            Toast.makeText(this, "请填写应用名称和包名", Toast.LENGTH_SHORT).show();
            return;
        }

        // 注: 虽然获取了版本信息，但当前 LogEntry 类不存储这些信息
        // 可以考虑增强 LogEntry 类以存储版本信息
        
        // 创建日志条目
        LogEntry logEntry = new LogEntry(
                appName,
                packageName,
                operationType,
                status,
                details.isEmpty() ? null : details + (versionName.isEmpty() ? "" : " (版本: " + versionName + ")")
        );

        // 插入日志
        logEntryViewModel.insert(logEntry);
        Toast.makeText(this, "日志已添加", Toast.LENGTH_SHORT).show();

        // 清空输入字段
        clearInputFields();
    }

    private void clearInputFields() {
        packageNameEditText.setText("");
        appNameEditText.setText("");
        versionNameEditText.setText("");
        versionCodeEditText.setText("");
        detailsEditText.setText("");
        operationTypeSpinner.setSelection(0);
        statusSpinner.setSelection(0);
    }

    private void viewLogs() {
        // 启动日志查看活动
        startActivity(new android.content.Intent(this, InstallLogActivity.class));
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
} 