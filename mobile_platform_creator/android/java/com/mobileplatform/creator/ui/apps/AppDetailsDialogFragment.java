package com.mobileplatform.creator.ui.apps;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.mobileplatform.creator.R;
import com.mobileplatform.creator.data.model.AppInfo;
import com.mobileplatform.creator.utils.FileUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 应用详情对话框
 * 显示应用详细信息和操作选项
 */
public class AppDetailsDialogFragment extends DialogFragment {
    
    private static final String ARG_APP_INFO = "app_info";
    private AppInfo appInfo;
    private AppActionListener listener;
    
    /**
     * 创建应用详情对话框实例
     * 
     * @param appInfo 应用信息
     * @return 对话框实例
     */
    public static AppDetailsDialogFragment newInstance(AppInfo appInfo) {
        AppDetailsDialogFragment fragment = new AppDetailsDialogFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_APP_INFO, appInfo);
        fragment.setArguments(args);
        return fragment;
    }
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, R.style.AppTheme_Dialog);
        
        if (getArguments() != null) {
            appInfo = getArguments().getParcelable(ARG_APP_INFO);
        }
    }
    
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
        builder.setTitle(R.string.app_details);
        
        View view = createDialogView();
        builder.setView(view);
        
        return builder.create();
    }
    
    /**
     * 创建对话框视图
     * 
     * @return 视图
     */
    private View createDialogView() {
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_app_details, null);
        
        if (appInfo == null) {
            return view;
        }
        
        // 初始化视图
        ImageView appIcon = view.findViewById(R.id.app_icon);
        TextView appName = view.findViewById(R.id.app_name);
        TextView appPackage = view.findViewById(R.id.app_package);
        TextView appVersion = view.findViewById(R.id.app_version);
        TextView appSize = view.findViewById(R.id.app_size);
        TextView appInstallDate = view.findViewById(R.id.app_install_date);
        TextView appLastUpdate = view.findViewById(R.id.app_last_update);
        TextView appInstallPath = view.findViewById(R.id.app_install_path);
        
        Button btnOpen = view.findViewById(R.id.btn_open);
        Button btnShare = view.findViewById(R.id.btn_share);
        Button btnUninstall = view.findViewById(R.id.btn_uninstall);
        
        // 设置应用信息
        if (appInfo.getIcon() != null) {
            appIcon.setImageDrawable(appInfo.getIcon());
        }
        
        appName.setText(appInfo.getName());
        appPackage.setText(appInfo.getPackageName());
        appVersion.setText(String.format("%s (%d)", appInfo.getVersionName(), appInfo.getVersionCode()));
        appSize.setText(FileUtils.formatFileSize(appInfo.getSize()));
        
        // 显示安装日期
        if (appInfo.getFirstInstallTime() > 0) {
            appInstallDate.setText(formatDate(appInfo.getFirstInstallTime()));
        } else {
            appInstallDate.setText(R.string.unknown);
        }
        
        // 显示最后更新时间
        if (appInfo.getLastUpdateTime() > 0) {
            appLastUpdate.setText(formatDate(appInfo.getLastUpdateTime()));
        } else {
            appLastUpdate.setText(R.string.unknown);
        }
        
        // 显示安装路径
        if (appInfo.getSourceDir() != null) {
            appInstallPath.setText(appInfo.getSourceDir());
        } else {
            appInstallPath.setText(R.string.unknown);
        }
        
        // 设置按钮点击事件
        btnOpen.setOnClickListener(v -> {
            if (listener != null) {
                listener.onOpenApp(appInfo);
                dismiss();
            }
        });
        
        btnShare.setOnClickListener(v -> {
            if (listener != null) {
                listener.onShareApp(appInfo);
                dismiss();
            }
        });
        
        btnUninstall.setOnClickListener(v -> {
            showUninstallConfirmation();
        });
        
        return view;
    }
    
    /**
     * 显示卸载确认对话框
     */
    private void showUninstallConfirmation() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.confirm_uninstall)
                .setMessage(getString(R.string.confirm_uninstall_message, appInfo.getName()))
                .setPositiveButton(R.string.uninstall, (dialog, which) -> {
                    if (listener != null) {
                        listener.onUninstallApp(appInfo);
                        dismiss();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
    
    /**
     * 格式化日期
     * 
     * @param timestamp 时间戳
     * @return 格式化后的日期字符串
     */
    private String formatDate(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }
    
    /**
     * 设置应用操作监听器
     * 
     * @param listener 监听器
     */
    public void setAppActionListener(AppActionListener listener) {
        this.listener = listener;
    }
    
    /**
     * 应用操作监听器接口
     */
    public interface AppActionListener {
        /**
         * 打开应用
         * 
         * @param appInfo 应用信息
         */
        void onOpenApp(AppInfo appInfo);
        
        /**
         * 分享应用
         * 
         * @param appInfo 应用信息
         */
        void onShareApp(AppInfo appInfo);
        
        /**
         * 卸载应用
         * 
         * @param appInfo 应用信息
         */
        void onUninstallApp(AppInfo appInfo);
    }
} 