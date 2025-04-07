package com.mobileplatform.creator.ui.batch;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.mobileplatform.creator.R;
import com.mobileplatform.creator.data.model.AppInfo;
import com.mobileplatform.creator.data.model.StoreAppInfo;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 批量应用管理适配器
 */
public class BatchAppAdapter extends RecyclerView.Adapter<BatchAppAdapter.AppViewHolder> {
    
    private final Context context;
    private final List<AppInfo> appList;
    private final Set<String> selectedApps;
    private final Set<String> updatableApps;
    private OnItemSelectionChangeListener selectionChangeListener;
    private OnItemClickListener itemClickListener;
    
    /**
     * 构造函数
     */
    public BatchAppAdapter(Context context) {
        this.context = context;
        this.appList = new ArrayList<>();
        this.selectedApps = new HashSet<>();
        this.updatableApps = new HashSet<>();
    }
    
    /**
     * 设置应用列表
     */
    public void setAppList(List<AppInfo> appList) {
        this.appList.clear();
        if (appList != null) {
            this.appList.addAll(appList);
        }
        notifyDataSetChanged();
    }
    
    /**
     * 设置可更新的应用列表
     */
    public void setUpdatableApps(Set<String> appIds) {
        this.updatableApps.clear();
        if (appIds != null) {
            this.updatableApps.addAll(appIds);
        }
        notifyDataSetChanged();
    }
    
    /**
     * 设置选中的应用列表
     */
    public void setSelectedApps(Set<String> appIds) {
        this.selectedApps.clear();
        if (appIds != null) {
            this.selectedApps.addAll(appIds);
        }
        notifyDataSetChanged();
        
        // 通知选择变化
        if (selectionChangeListener != null) {
            selectionChangeListener.onSelectionChanged(selectedApps.size());
        }
    }
    
    /**
     * 获取选中的应用ID列表
     */
    public List<String> getSelectedAppIds() {
        return new ArrayList<>(selectedApps);
    }
    
    /**
     * 获取选中的应用信息列表
     */
    public List<AppInfo> getSelectedApps() {
        List<AppInfo> selected = new ArrayList<>();
        for (AppInfo app : appList) {
            if (selectedApps.contains(app.getId())) {
                selected.add(app);
            }
        }
        return selected;
    }
    
    /**
     * 获取可更新的已选应用信息列表
     */
    public List<AppInfo> getSelectedUpdatableApps() {
        List<AppInfo> selected = new ArrayList<>();
        for (AppInfo app : appList) {
            if (selectedApps.contains(app.getId()) && updatableApps.contains(app.getId())) {
                selected.add(app);
            }
        }
        return selected;
    }
    
    /**
     * 全选
     */
    public void selectAll() {
        selectedApps.clear();
        for (AppInfo app : appList) {
            selectedApps.add(app.getId());
        }
        notifyDataSetChanged();
        
        // 通知选择变化
        if (selectionChangeListener != null) {
            selectionChangeListener.onSelectionChanged(selectedApps.size());
        }
    }
    
    /**
     * 取消全选
     */
    public void clearSelection() {
        selectedApps.clear();
        notifyDataSetChanged();
        
        // 通知选择变化
        if (selectionChangeListener != null) {
            selectionChangeListener.onSelectionChanged(0);
        }
    }
    
    /**
     * 设置选择变化监听器
     */
    public void setOnItemSelectionChangeListener(OnItemSelectionChangeListener listener) {
        this.selectionChangeListener = listener;
    }
    
    /**
     * 设置项目点击监听器
     */
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.itemClickListener = listener;
    }
    
    @NonNull
    @Override
    public AppViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_batch_app, parent, false);
        return new AppViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull AppViewHolder holder, int position) {
        AppInfo app = appList.get(position);
        
        // 设置应用信息
        holder.appIcon.setImageDrawable(app.getIcon());
        holder.appName.setText(app.getName());
        holder.appVersion.setText("版本: " + app.getVersion());
        holder.appSize.setText("大小: " + app.getFormattedSize());
        
        // 设置选中状态
        holder.appCheckbox.setChecked(selectedApps.contains(app.getId()));
        
        // 设置更新指示器
        if (updatableApps.contains(app.getId())) {
            holder.updateIndicator.setVisibility(View.VISIBLE);
        } else {
            holder.updateIndicator.setVisibility(View.GONE);
        }
        
        // 设置点击事件
        holder.itemView.setOnClickListener(v -> {
            if (itemClickListener != null) {
                itemClickListener.onItemClick(app);
            }
        });
        
        // 设置复选框点击事件
        holder.appCheckbox.setOnClickListener(v -> {
            boolean isChecked = holder.appCheckbox.isChecked();
            if (isChecked) {
                selectedApps.add(app.getId());
            } else {
                selectedApps.remove(app.getId());
            }
            
            // 通知选择变化
            if (selectionChangeListener != null) {
                selectionChangeListener.onSelectionChanged(selectedApps.size());
            }
        });
    }
    
    @Override
    public int getItemCount() {
        return appList.size();
    }
    
    /**
     * 应用列表项视图持有者
     */
    static class AppViewHolder extends RecyclerView.ViewHolder {
        CheckBox appCheckbox;
        ImageView appIcon;
        TextView appName;
        TextView appVersion;
        TextView appSize;
        ImageView updateIndicator;
        
        AppViewHolder(@NonNull View itemView) {
            super(itemView);
            appCheckbox = itemView.findViewById(R.id.app_checkbox);
            appIcon = itemView.findViewById(R.id.app_icon);
            appName = itemView.findViewById(R.id.app_name);
            appVersion = itemView.findViewById(R.id.app_version);
            appSize = itemView.findViewById(R.id.app_size);
            updateIndicator = itemView.findViewById(R.id.update_indicator);
        }
    }
    
    /**
     * 项目选择变化监听器接口
     */
    public interface OnItemSelectionChangeListener {
        void onSelectionChanged(int count);
    }
    
    /**
     * 项目点击监听器接口
     */
    public interface OnItemClickListener {
        void onItemClick(AppInfo app);
    }
} 