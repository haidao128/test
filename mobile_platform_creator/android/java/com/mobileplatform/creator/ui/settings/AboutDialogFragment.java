package com.mobileplatform.creator.ui.settings;

import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.mobileplatform.creator.BuildConfig;
import com.mobileplatform.creator.R;

/**
 * 关于对话框Fragment，显示应用信息
 */
public class AboutDialogFragment extends DialogFragment {
    
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_about, null);
        
        // 设置版本信息
        TextView versionText = view.findViewById(R.id.text_version);
        versionText.setText(String.format("版本 %s (%d)", BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE));
        
        // 设置访问网站按钮
        Button websiteButton = view.findViewById(R.id.button_website);
        websiteButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/mobile-platform-creator/android"));
            startActivity(intent);
        });
        
        // 设置开源许可按钮
        Button licenseButton = view.findViewById(R.id.button_licenses);
        licenseButton.setOnClickListener(v -> {
            showOpenSourceLicenses();
        });
        
        // 创建对话框
        return new AlertDialog.Builder(requireContext())
                .setTitle("关于")
                .setView(view)
                .setPositiveButton("确定", (dialog, which) -> dismiss())
                .create();
    }
    
    /**
     * 显示开源许可对话框
     */
    private void showOpenSourceLicenses() {
        new AlertDialog.Builder(requireContext())
                .setTitle("开源许可")
                .setMessage(
                        "本应用使用以下开源项目:\n\n" +
                        "- AndroidX (Apache License 2.0)\n" +
                        "- Material Components (Apache License 2.0)\n" +
                        "- Retrofit (Apache License 2.0)\n" +
                        "- OkHttp (Apache License 2.0)\n" +
                        "- Gson (Apache License 2.0)\n" +
                        "- Glide (BSD, MIT, Apache License 2.0)\n" +
                        "- JUnit (Eclipse Public License)\n" +
                        "- Espresso (Apache License 2.0)\n" +
                        "- WASM3 (MIT License)\n\n" +
                        "完整许可文本可在项目GitHub仓库中查阅。"
                )
                .setPositiveButton("确定", null)
                .show();
    }
} 