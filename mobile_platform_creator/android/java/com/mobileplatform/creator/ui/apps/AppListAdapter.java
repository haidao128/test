package com.mobileplatform.creator.ui.apps;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.mobileplatform.creator.R;
import com.mobileplatform.creator.data.model.AppInfo;
import com.mobileplatform.creator.utils.FileUtils;

import java.util.List;

/**
 * 应用列表适配器，用于在RecyclerView中显示应用卡片
 */
public class AppListAdapter extends RecyclerView.Adapter<AppListAdapter.AppViewHolder> {

    // 操作类型常量
    public static final int ACTION_OPEN = 1;
    public static final int ACTION_UNINSTALL = 2;
    
    private final Context context;
    private final List<AppInfo> appList;
    private OnItemClickListener listener;
    
    /**
     * 创建应用列表适配器
     */
    public AppListAdapter(Context context, List<AppInfo> appList) {
        this.context = context;
        this.appList = appList;
    }
    
    /**
     * 设置项目点击监听器
     */
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public AppViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_app, parent, false);
        return new AppViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull AppViewHolder holder, int position) {
        AppInfo appInfo = appList.get(position);
        holder.bind(appInfo);
    }
    
    @Override
    public int getItemCount() {
        return appList.size();
    }
    
    /**
     * 应用视图持有者
     */
    class AppViewHolder extends RecyclerView.ViewHolder {
        private final CardView cardView;
        private final ImageView appIcon;
        private final TextView appName;
        private final TextView appVersion;
        private final TextView appSize;
        private final MaterialButton btnOpen;
        private final MaterialButton btnUninstall;
        
        public AppViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.card_view);
            appIcon = itemView.findViewById(R.id.app_icon);
            appName = itemView.findViewById(R.id.app_name);
            appVersion = itemView.findViewById(R.id.app_version);
            appSize = itemView.findViewById(R.id.app_size);
            btnOpen = itemView.findViewById(R.id.btn_open);
            btnUninstall = itemView.findViewById(R.id.btn_uninstall);
        }
        
        public void bind(AppInfo appInfo) {
            // 设置应用信息
            appName.setText(appInfo.getName());
            appVersion.setText(appInfo.getVersion());
            appSize.setText(FileUtils.formatFileSize(appInfo.getSize()));
            
            // 设置应用图标
            Drawable icon = appInfo.getIcon();
            if (icon != null) {
                appIcon.setImageDrawable(icon);
            } else {
                appIcon.setImageResource(R.drawable.ic_app_placeholder);
            }
            
            // 设置卡片点击事件
            cardView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(appInfo);
                }
            });
            
            // 设置操作按钮点击事件
            btnOpen.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onActionClick(appInfo, ACTION_OPEN);
                }
            });
            
            btnUninstall.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onActionClick(appInfo, ACTION_UNINSTALL);
                }
            });
        }
    }
    
    /**
     * 项目点击监听器接口
     */
    public interface OnItemClickListener {
        /**
         * 项目点击事件
         */
        void onItemClick(AppInfo appInfo);
        
        /**
         * 操作按钮点击事件
         * @param actionType 操作类型，可能是ACTION_OPEN或ACTION_UNINSTALL
         */
        void onActionClick(AppInfo appInfo, int actionType);
    }
} 