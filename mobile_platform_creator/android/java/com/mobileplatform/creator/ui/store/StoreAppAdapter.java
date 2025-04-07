package com.mobileplatform.creator.ui.store;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.mobileplatform.creator.R;
import com.mobileplatform.creator.data.StoreAppInfo;

import java.util.List;

/**
 * 商店应用适配器，用于展示应用列表
 * 支持普通应用项和精选应用项两种布局
 */
public class StoreAppAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    // 视图类型常量
    private static final int VIEW_TYPE_NORMAL = 0;
    private static final int VIEW_TYPE_FEATURED = 1;
    
    private final Context context;
    private final List<StoreAppInfo> appList;
    private final boolean isFeatured;
    private OnItemClickListener listener;
    
    /**
     * 构造函数
     * @param context 上下文
     * @param appList 应用列表
     * @param isFeatured 是否为精选应用列表
     */
    public StoreAppAdapter(Context context, List<StoreAppInfo> appList, boolean isFeatured) {
        this.context = context;
        this.appList = appList;
        this.isFeatured = isFeatured;
    }
    
    /**
     * 设置项目点击监听器
     * @param listener 监听器实例
     */
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }
    
    @Override
    public int getItemViewType(int position) {
        if (isFeatured) {
            return VIEW_TYPE_FEATURED;
        } else {
            return VIEW_TYPE_NORMAL;
        }
    }
    
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_FEATURED) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_featured_app, parent, false);
            return new FeaturedViewHolder(view);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.item_store_app, parent, false);
            return new NormalViewHolder(view);
        }
    }
    
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        StoreAppInfo app = appList.get(position);
        if (holder instanceof NormalViewHolder) {
            ((NormalViewHolder) holder).bind(app);
        } else if (holder instanceof FeaturedViewHolder) {
            ((FeaturedViewHolder) holder).bind(app);
        }
    }
    
    @Override
    public int getItemCount() {
        return appList.size();
    }
    
    /**
     * 普通应用视图持有者
     */
    class NormalViewHolder extends RecyclerView.ViewHolder {
        private final ImageView appIcon;
        private final TextView appName;
        private final TextView appDeveloper;
        private final TextView appSize;
        private final TextView appDesc;
        private final ProgressBar downloadProgress;
        private final Button btnAction;
        
        public NormalViewHolder(@NonNull View itemView) {
            super(itemView);
            appIcon = itemView.findViewById(R.id.app_icon);
            appName = itemView.findViewById(R.id.app_name);
            appDeveloper = itemView.findViewById(R.id.app_developer);
            appSize = itemView.findViewById(R.id.app_size);
            appDesc = itemView.findViewById(R.id.app_desc);
            downloadProgress = itemView.findViewById(R.id.download_progress);
            btnAction = itemView.findViewById(R.id.btn_action);
            
            // 设置整个项目的点击事件
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onItemClick(appList.get(position));
                }
            });
            
            // 设置操作按钮的点击事件
            btnAction.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onActionButtonClick(appList.get(position), position);
                }
            });
        }
        
        /**
         * 绑定数据到视图
         * @param app 应用信息
         */
        public void bind(StoreAppInfo app) {
            appName.setText(app.getName());
            appDeveloper.setText(app.getDeveloper());
            appSize.setText(app.getFormattedSize());
            appDesc.setText(app.getDescription());
            
            // 设置应用图标
            if (app.getIcon() != null) {
                appIcon.setImageBitmap(app.getIcon());
            } else {
                appIcon.setImageResource(R.drawable.ic_launcher_foreground);
            }
            
            // 根据下载状态更新UI
            updateDownloadState(app, btnAction, downloadProgress);
        }
    }
    
    /**
     * 精选应用视图持有者
     */
    class FeaturedViewHolder extends RecyclerView.ViewHolder {
        private final ImageView appIcon;
        private final ImageView appBanner;
        private final TextView appName;
        private final TextView appDeveloper;
        private final TextView appDesc;
        private final ProgressBar downloadProgress;
        private final Button btnAction;
        
        public FeaturedViewHolder(@NonNull View itemView) {
            super(itemView);
            appIcon = itemView.findViewById(R.id.app_icon);
            appBanner = itemView.findViewById(R.id.app_banner);
            appName = itemView.findViewById(R.id.app_name);
            appDeveloper = itemView.findViewById(R.id.app_developer);
            appDesc = itemView.findViewById(R.id.app_desc);
            downloadProgress = itemView.findViewById(R.id.download_progress);
            btnAction = itemView.findViewById(R.id.btn_action);
            
            // 设置整个项目的点击事件
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onItemClick(appList.get(position));
                }
            });
            
            // 设置操作按钮的点击事件
            btnAction.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onActionButtonClick(appList.get(position), position);
                }
            });
        }
        
        /**
         * 绑定数据到视图
         * @param app 应用信息
         */
        public void bind(StoreAppInfo app) {
            appName.setText(app.getName());
            appDeveloper.setText(app.getDeveloper());
            appDesc.setText(app.getDescription());
            
            // 设置应用图标
            if (app.getIcon() != null) {
                appIcon.setImageBitmap(app.getIcon());
            } else {
                appIcon.setImageResource(R.drawable.ic_launcher_foreground);
            }
            
            // 设置应用横幅
            if (app.getBanner() != null) {
                appBanner.setImageBitmap(app.getBanner());
            } else {
                appBanner.setImageResource(R.drawable.ic_launcher_background);
            }
            
            // 根据下载状态更新UI
            updateDownloadState(app, btnAction, downloadProgress);
        }
    }
    
    /**
     * 更新下载状态UI
     * @param app 应用信息
     * @param btnAction 操作按钮
     * @param downloadProgress 下载进度条
     */
    private void updateDownloadState(StoreAppInfo app, Button btnAction, ProgressBar downloadProgress) {
        btnAction.setText(app.getDownloadStatusText());
        
        if (app.getDownloadStatus() == 1) { // 下载中
            downloadProgress.setVisibility(View.VISIBLE);
            downloadProgress.setProgress(app.getDownloadProgress());
            btnAction.setEnabled(false);
        } else {
            downloadProgress.setVisibility(View.GONE);
            btnAction.setEnabled(true);
        }
    }
    
    /**
     * 项目点击事件监听器接口
     */
    public interface OnItemClickListener {
        /**
         * 当应用项被点击时调用
         * @param app 被点击的应用信息
         */
        void onItemClick(StoreAppInfo app);
        
        /**
         * 当应用项的操作按钮被点击时调用
         * @param app 相关的应用信息
         * @param position 在列表中的位置
         */
        void onActionButtonClick(StoreAppInfo app, int position);
    }
} 