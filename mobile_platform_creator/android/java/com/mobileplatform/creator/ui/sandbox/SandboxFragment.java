package com.mobileplatform.creator.ui.sandbox;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.mobileplatform.creator.R;
import com.mobileplatform.creator.data.AppRepository;
import com.mobileplatform.creator.data.model.AppInfo;
import com.mobileplatform.creator.sandbox.SandboxManager;
import com.mobileplatform.creator.sandbox.SandboxService;

import java.util.ArrayList;
import java.util.List;

/**
 * 沙箱测试Fragment，用于测试应用在不同沙箱安全级别下的运行情况
 */
public class SandboxFragment extends Fragment implements SandboxManager.SandboxListener {
    
    private Spinner appSpinner;
    private Spinner securityLevelSpinner;
    private Button testButton;
    private Button stopButton;
    private TextView statusText;
    private TextView logText;
    private SwipeRefreshLayout swipeRefreshLayout;
    
    private List<AppInfo> appList = new ArrayList<>();
    private ArrayAdapter<String> appAdapter;
    private ArrayAdapter<String> securityAdapter;
    
    private AppInfo selectedApp;
    private String selectedSecurityLevel = "标准";
    private boolean isRunning = false;
    
    // 沙箱管理器
    private SandboxManager sandboxManager;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_sandbox, container, false);
        
        // 初始化视图
        appSpinner = root.findViewById(R.id.spinner_app);
        securityLevelSpinner = root.findViewById(R.id.spinner_security_level);
        testButton = root.findViewById(R.id.button_test);
        stopButton = root.findViewById(R.id.button_stop);
        statusText = root.findViewById(R.id.text_status);
        logText = root.findViewById(R.id.text_log);
        swipeRefreshLayout = root.findViewById(R.id.swipe_refresh);
        
        // 设置安全级别选择器
        String[] securityLevels = {"严格", "标准", "最小"};
        securityAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, securityLevels);
        securityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        securityLevelSpinner.setAdapter(securityAdapter);
        securityLevelSpinner.setSelection(1); // 默认选择标准级别
        
        // 设置应用选择器
        appAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, new ArrayList<>());
        appAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        appSpinner.setAdapter(appAdapter);
        
        // 初始化沙箱管理器
        sandboxManager = SandboxManager.getInstance(requireContext());
        sandboxManager.addSandboxListener(this);
        
        // 加载应用列表
        loadAppList();
        
        // 设置刷新监听器
        swipeRefreshLayout.setOnRefreshListener(this::loadAppList);
        
        // 设置应用选择监听器
        appSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && position < appList.size()) {
                    selectedApp = appList.get(position);
                }
            }
            
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedApp = null;
            }
        });
        
        // 设置安全级别选择监听器
        securityLevelSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedSecurityLevel = securityLevels[position];
            }
            
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedSecurityLevel = "标准";
            }
        });
        
        // 设置测试按钮点击监听器
        testButton.setOnClickListener(v -> startSandboxTest());
        
        // 设置停止按钮点击监听器
        stopButton.setOnClickListener(v -> stopSandboxTest());
        
        // 更新UI状态
        updateUI();
        
        return root;
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // 移除监听器
        sandboxManager.removeSandboxListener(this);
    }
    
    /**
     * 加载已安装应用列表
     */
    private void loadAppList() {
        AppRepository.getInstance().getInstalledApps(requireContext(), appInfoList -> {
            if (isAdded()) {
                requireActivity().runOnUiThread(() -> {
                    appList.clear();
                    appList.addAll(appInfoList);
                    
                    List<String> appNames = new ArrayList<>();
                    for (AppInfo appInfo : appList) {
                        appNames.add(appInfo.getName());
                    }
                    
                    appAdapter.clear();
                    appAdapter.addAll(appNames);
                    appAdapter.notifyDataSetChanged();
                    
                    if (!appList.isEmpty()) {
                        selectedApp = appList.get(0);
                    }
                    
                    // 完成刷新
                    swipeRefreshLayout.setRefreshing(false);
                });
            }
        });
    }
    
    /**
     * 开始沙箱测试
     */
    private void startSandboxTest() {
        if (selectedApp == null) {
            Toast.makeText(requireContext(), "请先选择一个应用", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 显示确认对话框
        new AlertDialog.Builder(requireContext())
                .setTitle("开始沙箱测试")
                .setMessage("确定要在沙箱环境中启动 " + selectedApp.getName() + " 吗？\n安全级别：" + selectedSecurityLevel)
                .setPositiveButton("确定", (dialog, which) -> {
                    isRunning = true;
                    updateUI();
                    
                    logText.setText("开始在" + selectedSecurityLevel + "安全级别下测试应用: " + selectedApp.getName() + "\n");
                    
                    // 通过沙箱管理器启动应用
                    boolean result = sandboxManager.startApp(
                            selectedApp.getPackageName(),
                            convertSecurityLevel(selectedSecurityLevel),
                            this
                    );
                    
                    if (!result) {
                        appendLog("启动沙箱环境失败");
                        isRunning = false;
                        updateUI();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }
    
    /**
     * 停止沙箱测试
     */
    private void stopSandboxTest() {
        sandboxManager.stopApp();
        appendLog("正在停止沙箱环境");
    }
    
    /**
     * 更新UI状态
     */
    private void updateUI() {
        if (isRunning) {
            testButton.setEnabled(false);
            stopButton.setEnabled(true);
            appSpinner.setEnabled(false);
            securityLevelSpinner.setEnabled(false);
            statusText.setText("状态: 运行中");
        } else {
            testButton.setEnabled(true);
            stopButton.setEnabled(false);
            appSpinner.setEnabled(true);
            securityLevelSpinner.setEnabled(true);
            statusText.setText("状态: 空闲");
        }
    }
    
    /**
     * 添加日志内容
     */
    private void appendLog(String message) {
        String currentLog = logText.getText().toString();
        logText.setText(currentLog + message + "\n");
    }
    
    /**
     * 转换安全级别为服务所需的常量
     */
    private int convertSecurityLevel(String level) {
        switch (level) {
            case "严格":
                return SandboxService.SECURITY_LEVEL_STRICT;
            case "最小":
                return SandboxService.SECURITY_LEVEL_MINIMAL;
            case "标准":
            default:
                return SandboxService.SECURITY_LEVEL_STANDARD;
        }
    }
    
    // 实现SandboxManager.SandboxListener接口
    
    @Override
    public void onSandboxStarted(String packageName) {
        if (isAdded()) {
            requireActivity().runOnUiThread(() -> {
                appendLog("沙箱环境启动成功，运行应用: " + packageName);
                statusText.setText("状态: 运行中");
            });
        }
    }
    
    @Override
    public void onSandboxStopped() {
        if (isAdded()) {
            requireActivity().runOnUiThread(() -> {
                appendLog("沙箱环境已停止");
                isRunning = false;
                updateUI();
            });
        }
    }
    
    @Override
    public void onSandboxError(String error) {
        if (isAdded()) {
            requireActivity().runOnUiThread(() -> {
                appendLog("错误: " + error);
                isRunning = false;
                updateUI();
            });
        }
    }
    
    @Override
    public void onSecurityViolation(String violation) {
        if (isAdded()) {
            requireActivity().runOnUiThread(() -> {
                appendLog("安全违规: " + violation);
            });
        }
    }
    
    @Override
    public void onResourceUsage(String resourceInfo) {
        if (isAdded()) {
            requireActivity().runOnUiThread(() -> {
                appendLog("资源使用: " + resourceInfo);
            });
        }
    }
    
    @Override
    public void onStatusChanged(String status) {
        if (isAdded()) {
            requireActivity().runOnUiThread(() -> {
                appendLog("状态: " + status);
            });
        }
    }
} 