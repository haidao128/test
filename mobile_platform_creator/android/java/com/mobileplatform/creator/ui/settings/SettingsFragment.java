package com.mobileplatform.creator.ui.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.mobileplatform.creator.R;
import com.mobileplatform.creator.data.AppRepository;
import com.mobileplatform.creator.runtime.WasmRuntimeManager;
import com.mobileplatform.creator.sandbox.SandboxService;

/**
 * 设置Fragment，管理应用平台设置
 */
public class SettingsFragment extends Fragment {
    
    private static final String PREFS_NAME = "MobilePlatformPrefs";
    private static final String KEY_AUTO_UPDATE = "auto_update";
    private static final String KEY_DEFAULT_SECURITY = "default_security";
    private static final String KEY_WASM_MEMORY = "wasm_memory";
    private static final String KEY_DEVELOPER_MODE = "developer_mode";
    
    private Switch autoUpdateSwitch;
    private SeekBar securityLevelSeekBar;
    private TextView securityLevelText;
    private SeekBar wasmMemorySeekBar;
    private TextView wasmMemoryText;
    private Switch developerModeSwitch;
    private Button clearCacheButton;
    private Button resetSettingsButton;
    private Button aboutButton;
    
    private SharedPreferences prefs;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_settings, container, false);
        
        // 获取SharedPreferences
        prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        
        // 初始化视图
        autoUpdateSwitch = root.findViewById(R.id.switch_auto_update);
        securityLevelSeekBar = root.findViewById(R.id.seekbar_security);
        securityLevelText = root.findViewById(R.id.text_security_level);
        wasmMemorySeekBar = root.findViewById(R.id.seekbar_wasm_memory);
        wasmMemoryText = root.findViewById(R.id.text_wasm_memory);
        developerModeSwitch = root.findViewById(R.id.switch_developer_mode);
        clearCacheButton = root.findViewById(R.id.button_clear_cache);
        resetSettingsButton = root.findViewById(R.id.button_reset_settings);
        aboutButton = root.findViewById(R.id.button_about);
        
        // 设置初始值
        loadSettings();
        
        // 设置监听器
        autoUpdateSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            saveAutoUpdateSetting(isChecked);
        });
        
        securityLevelSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateSecurityLevelText(progress);
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // 不处理
            }
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                saveSecurityLevelSetting(seekBar.getProgress());
            }
        });
        
        wasmMemorySeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateWasmMemoryText(progress);
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // 不处理
            }
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                saveWasmMemorySetting(seekBar.getProgress());
            }
        });
        
        developerModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            saveDeveloperModeSetting(isChecked);
        });
        
        clearCacheButton.setOnClickListener(v -> clearCache());
        resetSettingsButton.setOnClickListener(v -> resetSettings());
        aboutButton.setOnClickListener(v -> showAboutDialog());
        
        return root;
    }
    
    /**
     * 加载保存的设置
     */
    private void loadSettings() {
        // 自动更新
        boolean autoUpdate = prefs.getBoolean(KEY_AUTO_UPDATE, true);
        autoUpdateSwitch.setChecked(autoUpdate);
        
        // 安全级别 (0=最小, 1=标准, 2=严格)
        int securityLevel = prefs.getInt(KEY_DEFAULT_SECURITY, 1);
        securityLevelSeekBar.setProgress(securityLevel);
        updateSecurityLevelText(securityLevel);
        
        // WASM内存限制 (50-500MB)
        int wasmMemory = prefs.getInt(KEY_WASM_MEMORY, 100);
        wasmMemorySeekBar.setProgress((wasmMemory - 50) / 10);
        updateWasmMemoryText((wasmMemory - 50) / 10);
        
        // 开发者模式
        boolean developerMode = prefs.getBoolean(KEY_DEVELOPER_MODE, false);
        developerModeSwitch.setChecked(developerMode);
    }
    
    /**
     * 更新安全级别文本显示
     */
    private void updateSecurityLevelText(int progress) {
        String[] levels = {"最小", "标准", "严格"};
        securityLevelText.setText("安全级别: " + levels[progress]);
    }
    
    /**
     * 更新WASM内存文本显示
     */
    private void updateWasmMemoryText(int progress) {
        int memoryMB = 50 + (progress * 10);
        wasmMemoryText.setText("WASM内存限制: " + memoryMB + "MB");
    }
    
    /**
     * 保存自动更新设置
     */
    private void saveAutoUpdateSetting(boolean enabled) {
        prefs.edit().putBoolean(KEY_AUTO_UPDATE, enabled).apply();
        AppRepository.getInstance().setAutoUpdateEnabled(enabled);
    }
    
    /**
     * 保存安全级别设置
     */
    private void saveSecurityLevelSetting(int level) {
        prefs.edit().putInt(KEY_DEFAULT_SECURITY, level).apply();
        
        // 将设置应用到沙箱服务
        int sandboxLevel;
        switch (level) {
            case 0:
                sandboxLevel = SandboxService.SECURITY_LEVEL_MINIMAL;
                break;
            case 2:
                sandboxLevel = SandboxService.SECURITY_LEVEL_STRICT;
                break;
            case 1:
            default:
                sandboxLevel = SandboxService.SECURITY_LEVEL_STANDARD;
                break;
        }
        SandboxService.setDefaultSecurityLevel(requireContext(), sandboxLevel);
    }
    
    /**
     * 保存WASM内存设置
     */
    private void saveWasmMemorySetting(int progress) {
        int memoryMB = 50 + (progress * 10);
        prefs.edit().putInt(KEY_WASM_MEMORY, memoryMB).apply();
        
        // 将设置应用到WASM运行时
        try {
            WasmRuntimeManager.getInstance().setMemoryLimit(memoryMB);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "应用WASM内存设置失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * 保存开发者模式设置
     */
    private void saveDeveloperModeSetting(boolean enabled) {
        prefs.edit().putBoolean(KEY_DEVELOPER_MODE, enabled).apply();
        
        // 应用开发者模式设置
        if (enabled) {
            Toast.makeText(requireContext(), "开发者模式已启用", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * 清除缓存
     */
    private void clearCache() {
        AppRepository.getInstance().clearCache(requireContext(), success -> {
            if (isAdded()) {
                if (success) {
                    Toast.makeText(requireContext(), "缓存已清除", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(requireContext(), "清除缓存失败", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
    
    /**
     * 重置所有设置
     */
    private void resetSettings() {
        // 重置SharedPreferences
        prefs.edit().clear().apply();
        
        // 重载设置
        loadSettings();
        
        // 应用默认设置
        saveAutoUpdateSetting(true);
        saveSecurityLevelSetting(1);
        saveWasmMemorySetting(5); // 对应100MB
        saveDeveloperModeSetting(false);
        
        Toast.makeText(requireContext(), "所有设置已重置为默认值", Toast.LENGTH_SHORT).show();
    }
    
    /**
     * 显示关于对话框
     */
    private void showAboutDialog() {
        // 创建并显示关于对话框
        AboutDialogFragment dialogFragment = new AboutDialogFragment();
        dialogFragment.show(getParentFragmentManager(), "AboutDialogFragment");
    }
} 