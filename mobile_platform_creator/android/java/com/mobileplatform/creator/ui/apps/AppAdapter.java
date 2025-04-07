package com.mobileplatform.creator.ui.apps;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.mobileplatform.creator.R;
import com.mobileplatform.creator.data.model.AppInfo;
import com.mobileplatform.creator.utils.FileUtils;

import java.util.List;

/**
 * 应用列表适配器
 * 用于显示已安装的应用列表
 */
public class AppAdapter extends RecyclerView.Adapter<AppAdapter.ViewHolder> {
    
    // 操作类型常量
    public static final int ACTION_OPEN = 1;
    public static final int ACTION_UNINSTALL = 2;
    
    private final Context context;
    private final List<AppInfo> appList;
    private OnItemClickListener listener;
    
    /**
     * 构造函数
     * 
     * @param context 上下文
     * @param appList 应用列表
     */
    public AppAdapter(Context context, List<AppInfo> appList) {
        this.context = context;
        this.appList = appList;
    }
    
    /**
     * 设置项目点击监听器
     * 
     * @param listener 监听器
     */
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_app, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AppInfo app = appList.get(position);
        holder.bind(app, position);
    }
    
    @Override
    public int getItemCount() {
        return appList.size();
    }
    
    /**
     * 视图持有者
     */
    class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView appIcon;
        private final TextView appName;
        private final TextView appPackage;
        private final TextView appVersion;
        private final TextView appSize;
        private final Button btnOpen;
        private final Button btnUninstall;
        
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            appIcon = itemView.findViewById(R.id.app_icon);
            appName = itemView.findViewById(R.id.app_name);
            appPackage = itemView.findViewById(R.id.app_package);
            appVersion = itemView.findViewById(R.id.app_version);
            appSize = itemView.findViewById(R.id.app_size);
            btnOpen = itemView.findViewById(R.id.btn_open);
            btnUninstall = itemView.findViewById(R.id.btn_uninstall);
            
            // 设置项目点击事件
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onItemClick(appList.get(position));
                }
            });
            
            // 设置打开按钮点击事件
            btnOpen.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onActionClick(appList.get(position), position, ACTION_OPEN);
                }
            });
            
            // 设置卸载按钮点击事件
            btnUninstall.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onActionClick(appList.get(position), position, ACTION_UNINSTALL);
                }
            });
        }
        
        /**
         * 绑定数据到视图
         * 
         * @param app 应用信息
         * @param position 位置
         */
        public void bind(AppInfo app, int position) {
            // 设置应用图标
            if (app.getIcon() != null) {
                appIcon.setImageDrawable(app.getIcon());
            } else {
                appIcon.setImageResource(R.drawable.ic_launcher_foreground);
            }
            
            // 设置应用信息
            appName.setText(app.getName());
            appPackage.setText(app.getPackageName());
            appVersion.setText(String.format("%s (%d)", app.getVersionName(), app.getVersionCode()));
            appSize.setText(FileUtils.formatFileSize(app.getSize()));
            
            // 设置按钮状态
            btnOpen.setEnabled(true);
            btnUninstall.setEnabled(true);
        }
    }
    
    /**
     * 项目点击监听器接口
     */
    public interface OnItemClickListener {
        /**
         * 点击应用项
         * 
         * @param appInfo 应用信息
         */
        void onItemClick(AppInfo appInfo);
        
        /**
         * 点击操作按钮
         * 
         * @param appInfo 应用信息
         * @param position 位置
         * @param actionType 操作类型
         */
        void onActionClick(AppInfo appInfo, int position, int actionType);
    }
} 