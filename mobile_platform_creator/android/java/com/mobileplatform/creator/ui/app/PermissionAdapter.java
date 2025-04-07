package com.mobileplatform.creator.ui.app;

import android.content.Context;
import android.content.pm.PermissionInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.mobileplatform.creator.R;

import java.util.ArrayList;
import java.util.List;

/**
 * 权限列表适配器
 */
public class PermissionAdapter extends RecyclerView.Adapter<PermissionAdapter.PermissionViewHolder> {
    
    private final Context context;
    private final List<PermissionInfo> permissions;
    
    /**
     * 构造函数
     */
    public PermissionAdapter(Context context) {
        this.context = context;
        this.permissions = new ArrayList<>();
    }
    
    /**
     * 设置权限列表
     */
    public void setPermissions(List<PermissionInfo> permissions) {
        this.permissions.clear();
        if (permissions != null) {
            this.permissions.addAll(permissions);
        }
        notifyDataSetChanged();
    }
    
    @NonNull
    @Override
    public PermissionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_permission, parent, false);
        return new PermissionViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull PermissionViewHolder holder, int position) {
        PermissionInfo permission = permissions.get(position);
        
        // 设置权限名称
        String permissionName = permission.name;
        if (permissionName.startsWith("android.permission.")) {
            permissionName = permissionName.substring("android.permission.".length());
        }
        holder.permissionName.setText(permissionName);
        
        // 设置权限状态
        String status;
        if (permission.protectionLevel == PermissionInfo.PROTECTION_DANGEROUS) {
            status = "危险";
            holder.permissionStatus.setTextColor(context.getResources().getColor(android.R.color.holo_red_dark));
        } else if (permission.protectionLevel == PermissionInfo.PROTECTION_NORMAL) {
            status = "普通";
            holder.permissionStatus.setTextColor(context.getResources().getColor(android.R.color.darker_gray));
        } else {
            status = "其他";
            holder.permissionStatus.setTextColor(context.getResources().getColor(android.R.color.darker_gray));
        }
        holder.permissionStatus.setText(status);
        
        // 设置权限图标
        if (permission.icon != 0) {
            holder.permissionIcon.setImageResource(permission.icon);
        } else {
            holder.permissionIcon.setImageResource(R.drawable.ic_permission);
        }
    }
    
    @Override
    public int getItemCount() {
        return permissions.size();
    }
    
    /**
     * 权限列表项视图持有者
     */
    static class PermissionViewHolder extends RecyclerView.ViewHolder {
        ImageView permissionIcon;
        TextView permissionName;
        TextView permissionStatus;
        
        PermissionViewHolder(@NonNull View itemView) {
            super(itemView);
            permissionIcon = itemView.findViewById(R.id.permission_icon);
            permissionName = itemView.findViewById(R.id.permission_name);
            permissionStatus = itemView.findViewById(R.id.permission_status);
        }
    }
} 