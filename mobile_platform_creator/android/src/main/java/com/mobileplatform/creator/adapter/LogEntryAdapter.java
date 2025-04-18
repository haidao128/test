package com.mobileplatform.creator.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.mobileplatform.creator.R;
import com.mobileplatform.creator.model.LogEntry;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 用于显示安装日志列表的 RecyclerView Adapter。
 * 使用 ListAdapter 和 DiffUtil 来提高性能。
 */
public class LogEntryAdapter extends ListAdapter<LogEntry, LogEntryAdapter.LogViewHolder> {

    private static final SimpleDateFormat DATE_FORMAT = 
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

    public LogEntryAdapter() {
        super(DIFF_CALLBACK);
    }

    private static final DiffUtil.ItemCallback<LogEntry> DIFF_CALLBACK = new DiffUtil.ItemCallback<LogEntry>() {
        @Override
        public boolean areItemsTheSame(@NonNull LogEntry oldItem, @NonNull LogEntry newItem) {
            return oldItem.getId() == newItem.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull LogEntry oldItem, @NonNull LogEntry newItem) {
            // 比较所有可能变化的字段
            return oldItem.getPackageName().equals(newItem.getPackageName()) &&
                   oldItem.getAppName().equals(newItem.getAppName()) &&
                   oldItem.getTimestamp() == newItem.getTimestamp() &&
                   oldItem.getOperationType().equals(newItem.getOperationType()) &&
                   oldItem.getStatus().equals(newItem.getStatus()) &&
                   (oldItem.getDetails() == null ? newItem.getDetails() == null : 
                    oldItem.getDetails().equals(newItem.getDetails()));
        }
    };

    @NonNull
    @Override
    public LogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_log_entry, parent, false);
        return new LogViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull LogViewHolder holder, int position) {
        LogEntry currentLog = getItem(position);
        holder.bind(currentLog);
    }

    class LogViewHolder extends RecyclerView.ViewHolder {
        private final ImageView statusIcon;
        private final TextView appNameTextView;
        private final TextView timestampTextView;
        private final TextView operationTextView;
        private final TextView detailsTextView;

        public LogViewHolder(@NonNull View itemView) {
            super(itemView);
            statusIcon = itemView.findViewById(R.id.imageView_log_status);
            appNameTextView = itemView.findViewById(R.id.textView_log_app_name);
            timestampTextView = itemView.findViewById(R.id.textView_log_timestamp);
            operationTextView = itemView.findViewById(R.id.textView_log_operation);
            detailsTextView = itemView.findViewById(R.id.textView_log_details);

            // 设置点击展开/隐藏详情
            itemView.setOnClickListener(v -> {
                int visibility = detailsTextView.getVisibility();
                detailsTextView.setVisibility(visibility == View.GONE ? View.VISIBLE : View.GONE);
            });
        }

        public void bind(LogEntry logEntry) {
            String appVersionInfo = logEntry.getAppName() + " (" + logEntry.getPackageName() + ")";
            appNameTextView.setText(appVersionInfo);
            timestampTextView.setText(DATE_FORMAT.format(new Date(logEntry.getTimestamp())));
            String operationInfo = "操作: " + logEntry.getOperationType() + " / " + logEntry.getStatus();
            operationTextView.setText(operationInfo);
            
            if (logEntry.getDetails() != null && !logEntry.getDetails().isEmpty()) {
                detailsTextView.setText(logEntry.getDetails());
            } else {
                 detailsTextView.setText("无详细信息"); 
            }
            detailsTextView.setVisibility(View.GONE); // 默认隐藏详情

            // 根据状态设置图标 (示例)
            if ("SUCCESS".equalsIgnoreCase(logEntry.getStatus())) {
                statusIcon.setImageResource(android.R.drawable.ic_dialog_info); 
                // 可以为不同操作类型设置不同颜色滤镜 statusIcon.setColorFilter(...)
            } else if ("FAILURE".equalsIgnoreCase(logEntry.getStatus())) {
                statusIcon.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
            } else {
                statusIcon.setImageResource(android.R.drawable.ic_menu_info_details);
            }
        }
    }
} 