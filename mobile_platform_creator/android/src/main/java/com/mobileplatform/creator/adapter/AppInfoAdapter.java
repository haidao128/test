package com.mobileplatform.creator.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.mobileplatform.creator.R;
// TODO: 替换为您的实际应用信息数据模型类
// import com.mobileplatform.creator.model.AppInfo;
import java.util.List;

/**
 * 用于在 MainActivity 中显示应用列表的 RecyclerView Adapter。
 * TODO: 实现具体的数据绑定和点击事件处理逻辑。
 */
public class AppInfoAdapter extends RecyclerView.Adapter<AppInfoAdapter.AppInfoViewHolder> {

    private Context context;
    private List<?> appInfoList; // TODO: 将 '?' 替换为您的 AppInfo 类
    private OnItemClickListener listener;

    // TODO: 定义您的 AppInfo 数据模型类
    // public static class AppInfo { ... }

    public interface OnItemClickListener {
        void onItemClick(Object appInfo); // TODO: 将 'Object' 替换为您的 AppInfo 类
    }

    public AppInfoAdapter(Context context, List<?> appInfoList, OnItemClickListener listener) {
        this.context = context;
        this.appInfoList = appInfoList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public AppInfoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_app_info, parent, false);
        return new AppInfoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AppInfoViewHolder holder, int position) {
        Object currentApp = appInfoList.get(position); // TODO: 替换为 AppInfo 类
        // TODO: 在这里将 appInfo 数据绑定到 ViewHolder 的视图上
        // holder.appName.setText(currentApp.getName());
        // holder.packageName.setText(currentApp.getPackageName());
        // 使用 Glide 或其他库加载图标： Glide.with(context).load(currentApp.getIcon()).into(holder.appIcon);
        
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(currentApp);
            }
        });
    }

    @Override
    public int getItemCount() {
        return appInfoList == null ? 0 : appInfoList.size();
    }

    // 更新数据的方法
    public void updateData(List<?> newAppInfoList) {
        this.appInfoList = newAppInfoList;
        notifyDataSetChanged(); // TODO: 考虑使用 DiffUtil 提高效率
    }

    static class AppInfoViewHolder extends RecyclerView.ViewHolder {
        ImageView appIcon;
        TextView appName;
        TextView packageName;

        public AppInfoViewHolder(@NonNull View itemView) {
            super(itemView);
            appIcon = itemView.findViewById(R.id.imageView_app_icon);
            appName = itemView.findViewById(R.id.textView_app_name);
            packageName = itemView.findViewById(R.id.textView_package_name);
        }
    }
} 