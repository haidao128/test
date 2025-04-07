package com.mobileplatform.creator.ui.download;

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
import com.mobileplatform.creator.data.model.DownloadTask;
import com.mobileplatform.creator.utils.FileUtils;

import java.util.List;

/**
 * 下载任务适配器
 */
public class DownloadAdapter extends RecyclerView.Adapter<DownloadAdapter.ViewHolder> {
    
    // 上下文
    private final Context context;
    
    // 下载任务列表
    private List<DownloadTask> downloadTasks;
    
    // 任务操作监听器
    private OnItemActionListener actionListener;
    
    /**
     * 构造函数
     * 
     * @param context 上下文
     * @param downloadTasks 下载任务列表
     */
    public DownloadAdapter(Context context, List<DownloadTask> downloadTasks) {
        this.context = context;
        this.downloadTasks = downloadTasks;
    }
    
    /**
     * 设置任务操作监听器
     * 
     * @param listener 监听器
     */
    public void setOnItemActionListener(OnItemActionListener listener) {
        this.actionListener = listener;
    }
    
    /**
     * 更新数据
     * 
     * @param tasks 任务列表
     */
    public void updateData(List<DownloadTask> tasks) {
        this.downloadTasks = tasks;
        notifyDataSetChanged();
    }
    
    /**
     * 更新特定任务
     * 
     * @param task 下载任务
     */
    public void updateTask(DownloadTask task) {
        if (task == null) {
            return;
        }
        
        for (int i = 0; i < downloadTasks.size(); i++) {
            if (downloadTasks.get(i).getId().equals(task.getId())) {
                downloadTasks.set(i, task);
                notifyItemChanged(i);
                return;
            }
        }
        
        // 如果任务不在列表中，添加到列表
        downloadTasks.add(task);
        notifyItemInserted(downloadTasks.size() - 1);
    }
    
    /**
     * 移除任务
     * 
     * @param task 下载任务
     */
    public void removeTask(DownloadTask task) {
        if (task == null) {
            return;
        }
        
        for (int i = 0; i < downloadTasks.size(); i++) {
            if (downloadTasks.get(i).getId().equals(task.getId())) {
                downloadTasks.remove(i);
                notifyItemRemoved(i);
                return;
            }
        }
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_download, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DownloadTask task = downloadTasks.get(position);
        holder.bind(task);
    }
    
    @Override
    public int getItemCount() {
        return downloadTasks.size();
    }
    
    /**
     * ViewHolder类
     */
    class ViewHolder extends RecyclerView.ViewHolder {
        
        // UI组件
        private final ImageView appIcon;
        private final TextView appName;
        private final TextView fileSize;
        private final TextView downloadStatus;
        private final ProgressBar progressBar;
        private final Button actionButton;
        private final Button cancelButton;
        
        /**
         * 构造函数
         * 
         * @param itemView 视图
         */
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            
            // 初始化UI组件
            appIcon = itemView.findViewById(R.id.app_icon);
            appName = itemView.findViewById(R.id.app_name);
            fileSize = itemView.findViewById(R.id.file_size);
            downloadStatus = itemView.findViewById(R.id.download_status);
            progressBar = itemView.findViewById(R.id.progress_bar);
            actionButton = itemView.findViewById(R.id.action_button);
            cancelButton = itemView.findViewById(R.id.cancel_button);
        }
        
        /**
         * 绑定数据
         * 
         * @param task 下载任务
         */
        public void bind(DownloadTask task) {
            // 设置应用图标
            if (task.getAppInfo() != null && task.getAppInfo().getIcon() != null) {
                appIcon.setImageBitmap(task.getAppInfo().getIcon());
            } else {
                appIcon.setImageResource(android.R.drawable.sym_def_app_icon);
            }
            
            // 设置应用名称
            String name = task.getAppInfo() != null ? task.getAppInfo().getName() : task.getFileName();
            appName.setText(name);
            
            // 设置文件大小
            String downloadedSize = FileUtils.formatFileSize(task.getDownloadedSize());
            String totalSize = FileUtils.formatFileSize(task.getTotalSize());
            fileSize.setText(downloadedSize + " / " + totalSize);
            
            // 设置进度条
            progressBar.setProgress(task.getProgress());
            
            // 设置状态和操作按钮
            setupStatusAndActions(task);
        }
        
        /**
         * 设置状态和操作按钮
         * 
         * @param task 下载任务
         */
        private void setupStatusAndActions(DownloadTask task) {
            // 设置状态文本
            if (task.isRunning()) {
                // 正在下载
                String speed = FileUtils.formatFileSize(task.getSpeed()) + "/s";
                downloadStatus.setText("下载速度: " + speed);
                
                // 暂停按钮
                actionButton.setText("暂停");
                actionButton.setOnClickListener(v -> {
                    if (actionListener != null) {
                        actionListener.onPauseClick(task);
                    }
                });
                
                // 显示取消按钮
                cancelButton.setVisibility(View.VISIBLE);
                cancelButton.setOnClickListener(v -> {
                    if (actionListener != null) {
                        actionListener.onCancelClick(task);
                    }
                });
                
            } else if (task.isPaused()) {
                // 已暂停
                downloadStatus.setText("已暂停");
                
                // 继续按钮
                actionButton.setText("继续");
                actionButton.setOnClickListener(v -> {
                    if (actionListener != null) {
                        actionListener.onResumeClick(task);
                    }
                });
                
                // 显示取消按钮
                cancelButton.setVisibility(View.VISIBLE);
                cancelButton.setOnClickListener(v -> {
                    if (actionListener != null) {
                        actionListener.onCancelClick(task);
                    }
                });
                
            } else if (task.isPending()) {
                // 等待中
                downloadStatus.setText("等待下载");
                
                // 取消按钮
                actionButton.setText("取消");
                actionButton.setOnClickListener(v -> {
                    if (actionListener != null) {
                        actionListener.onCancelClick(task);
                    }
                });
                
                // 隐藏取消按钮
                cancelButton.setVisibility(View.GONE);
                
            } else if (task.isCompleted()) {
                // 已完成
                downloadStatus.setText("下载完成");
                
                // 安装按钮
                actionButton.setText("安装");
                actionButton.setOnClickListener(v -> {
                    if (actionListener != null) {
                        actionListener.onInstallClick(task);
                    }
                });
                
                // 隐藏取消按钮
                cancelButton.setVisibility(View.GONE);
                
            } else if (task.isFailed()) {
                // 已失败
                downloadStatus.setText("下载失败: " + task.getErrorMessage());
                
                // 重试按钮
                actionButton.setText("重试");
                actionButton.setOnClickListener(v -> {
                    if (actionListener != null) {
                        actionListener.onRetryClick(task);
                    }
                });
                
                // 显示取消按钮
                cancelButton.setVisibility(View.VISIBLE);
                cancelButton.setOnClickListener(v -> {
                    if (actionListener != null) {
                        actionListener.onCancelClick(task);
                    }
                });
                
            } else if (task.isCancelled()) {
                // 已取消
                downloadStatus.setText("已取消");
                
                // 重试按钮
                actionButton.setText("重试");
                actionButton.setOnClickListener(v -> {
                    if (actionListener != null) {
                        actionListener.onRetryClick(task);
                    }
                });
                
                // 隐藏取消按钮
                cancelButton.setVisibility(View.GONE);
            }
        }
    }
    
    /**
     * 任务操作监听器接口
     */
    public interface OnItemActionListener {
        // 暂停点击
        void onPauseClick(DownloadTask task);
        
        // 恢复点击
        void onResumeClick(DownloadTask task);
        
        // 取消点击
        void onCancelClick(DownloadTask task);
        
        // 重试点击
        void onRetryClick(DownloadTask task);
        
        // 安装点击
        void onInstallClick(DownloadTask task);
    }
} 