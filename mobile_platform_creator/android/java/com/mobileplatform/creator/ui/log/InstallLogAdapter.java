package com.mobileplatform.creator.ui.log;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.mobileplatform.creator.R;
import com.mobileplatform.creator.data.model.InstallLogEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 安装日志适配器
 * 用于在RecyclerView中显示安装日志列表
 */
public class InstallLogAdapter extends RecyclerView.Adapter<InstallLogAdapter.LogViewHolder> {
    
    private final Context context;
    private List<InstallLogEntry> logEntries;
    private OnLogItemClickListener logItemClickListener;
    private OnLogActionListener logActionListener;
    
    /**
     * 构造函数
     * 
     * @param context 上下文
     * @param logEntries 日志条目列表
     */
    public InstallLogAdapter(Context context, List<InstallLogEntry> logEntries) {
        this.context = context;
        this.logEntries = new ArrayList<>(logEntries);
    }
    
    /**
     * 设置日志条目列表
     * 
     * @param newLogEntries 日志条目列表
     */
    public void setLogEntries(List<InstallLogEntry> newLogEntries) {
        if (newLogEntries == null) {
            newLogEntries = new ArrayList<>();
        }
        
        // 使用DiffUtil计算列表差异，高效更新
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return logEntries.size();
            }

            @Override
            public int getNewListSize() {
                return newLogEntries.size();
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                return Objects.equals(
                        logEntries.get(oldItemPosition).getId(),
                        newLogEntries.get(newItemPosition).getId());
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                InstallLogEntry oldItem = logEntries.get(oldItemPosition);
                InstallLogEntry newItem = newLogEntries.get(newItemPosition);
                
                return oldItem.isSuccess() == newItem.isSuccess() &&
                       Objects.equals(oldItem.getOperationType(), newItem.getOperationType()) &&
                       Objects.equals(oldItem.getAppName(), newItem.getAppName()) &&
                       Objects.equals(oldItem.getPackageName(), newItem.getPackageName()) &&
                       Objects.equals(oldItem.getVersion(), newItem.getVersion()) &&
                       Objects.equals(oldItem.getOperationTime(), newItem.getOperationTime()) &&
                       Objects.equals(oldItem.getErrorMessage(), newItem.getErrorMessage());
            }
        });
        
        this.logEntries = new ArrayList<>(newLogEntries);
        diffResult.dispatchUpdatesTo(this);
    }
    
    /**
     * 设置日志项点击监听器
     * 
     * @param listener 监听器
     */
    public void setOnLogItemClickListener(OnLogItemClickListener listener) {
        this.logItemClickListener = listener;
    }
    
    /**
     * 设置日志操作监听器
     * 
     * @param listener 监听器
     */
    public void setOnLogActionListener(OnLogActionListener listener) {
        this.logActionListener = listener;
    }
    
    @NonNull
    @Override
    public LogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_install_log, parent, false);
        return new LogViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull LogViewHolder holder, int position) {
        InstallLogEntry logEntry = logEntries.get(position);
        
        // 设置应用名称
        holder.tvAppName.setText(logEntry.getAppName());
        
        // 设置操作类型
        if (logEntry.getOperationType() != null) {
            holder.tvOperationType.setText(logEntry.getOperationType().getDisplayName());
            
            // 根据操作类型设置不同的背景颜色
            int bgColor;
            switch (logEntry.getOperationType()) {
                case INSTALL:
                    bgColor = ContextCompat.getColor(context, R.color.colorInstall);
                    break;
                case UPDATE:
                    bgColor = ContextCompat.getColor(context, R.color.colorUpdate);
                    break;
                case UNINSTALL:
                    bgColor = ContextCompat.getColor(context, R.color.colorUninstall);
                    break;
                case FAILED:
                default:
                    bgColor = ContextCompat.getColor(context, R.color.colorError);
                    break;
            }
            
            // 使用GradientDrawable设置圆角背景
            GradientDrawable drawable = new GradientDrawable();
            drawable.setColor(bgColor);
            drawable.setCornerRadius(context.getResources().getDimensionPixelSize(R.dimen.corner_radius_small));
            holder.tvOperationType.setBackground(drawable);
            
            holder.tvOperationType.setVisibility(View.VISIBLE);
        } else {
            holder.tvOperationType.setVisibility(View.GONE);
        }
        
        // 设置包名
        holder.tvPackageName.setText(logEntry.getPackageName());
        
        // 设置版本
        holder.tvVersion.setText("v" + logEntry.getVersion());
        
        // 设置操作时间
        holder.tvOperationTime.setText(logEntry.getFormattedOperationTime());
        
        // 设置状态
        holder.tvStatus.setText(logEntry.getStatusText());
        
        // 根据操作成功与否设置状态颜色
        int statusColor = logEntry.isSuccess() ? 
                ContextCompat.getColor(context, R.color.colorSuccess) : 
                ContextCompat.getColor(context, R.color.colorError);
        holder.tvStatus.setTextColor(statusColor);
        holder.viewStatusIndicator.setBackgroundColor(statusColor);
        
        // 设置错误信息
        if (logEntry.isSuccess() || logEntry.getErrorMessage() == null || logEntry.getErrorMessage().isEmpty()) {
            holder.tvErrorMessage.setVisibility(View.GONE);
        } else {
            holder.tvErrorMessage.setVisibility(View.VISIBLE);
            holder.tvErrorMessage.setText(logEntry.getErrorMessage());
        }
        
        // 设置点击监听器
        if (logItemClickListener != null) {
            holder.itemView.setOnClickListener(v -> 
                    logItemClickListener.onLogItemClick(logEntry, holder.getBindingAdapterPosition()));
        }
        
        // 设置更多操作按钮点击监听器
        if (logActionListener != null) {
            holder.btnMore.setOnClickListener(v -> 
                    logActionListener.onLogAction(logEntry, holder.getBindingAdapterPosition(), v));
        }
    }
    
    @Override
    public int getItemCount() {
        return logEntries != null ? logEntries.size() : 0;
    }
    
    /**
     * 获取指定位置的日志条目
     * 
     * @param position 位置
     * @return 日志条目
     */
    public InstallLogEntry getItem(int position) {
        if (logEntries != null && position >= 0 && position < logEntries.size()) {
            return logEntries.get(position);
        }
        return null;
    }
    
    /**
     * 获取当前所有日志条目
     * 
     * @return 日志条目列表
     */
    public List<InstallLogEntry> getLogEntries() {
        return new ArrayList<>(logEntries);
    }
    
    /**
     * 日志视图持有者
     */
    static class LogViewHolder extends RecyclerView.ViewHolder {
        View viewStatusIndicator;
        TextView tvAppName;
        TextView tvOperationType;
        TextView tvPackageName;
        TextView tvVersion;
        TextView tvOperationTime;
        TextView tvStatus;
        TextView tvErrorMessage;
        ImageButton btnMore;
        
        public LogViewHolder(@NonNull View itemView) {
            super(itemView);
            viewStatusIndicator = itemView.findViewById(R.id.view_status_indicator);
            tvAppName = itemView.findViewById(R.id.tv_app_name);
            tvOperationType = itemView.findViewById(R.id.tv_operation_type);
            tvPackageName = itemView.findViewById(R.id.tv_package_name);
            tvVersion = itemView.findViewById(R.id.tv_version);
            tvOperationTime = itemView.findViewById(R.id.tv_operation_time);
            tvStatus = itemView.findViewById(R.id.tv_status);
            tvErrorMessage = itemView.findViewById(R.id.tv_error_message);
            btnMore = itemView.findViewById(R.id.btn_more);
        }
    }
    
    /**
     * 日志项点击监听器
     */
    public interface OnLogItemClickListener {
        void onLogItemClick(InstallLogEntry logEntry, int position);
    }
    
    /**
     * 日志操作监听器
     */
    public interface OnLogActionListener {
        void onLogAction(InstallLogEntry logEntry, int position, View actionView);
    }
} 