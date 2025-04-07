package com.mobileplatform.creator.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.mobileplatform.creator.R;
import com.mobileplatform.creator.model.AppInfo;
import com.mobileplatform.creator.ui.app.AppDetailActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * 用于在 MainActivity 中显示应用列表的 RecyclerView Adapter。
 */
public class AppInfoAdapter extends RecyclerView.Adapter<AppInfoAdapter.AppInfoViewHolder> {

    private Context context;
    private List<AppInfo> appInfoList;

    public AppInfoAdapter(Context context, List<AppInfo> appInfoList) {
        this.context = context;
        this.appInfoList = (appInfoList == null) ? new ArrayList<>() : appInfoList;
    }

    @NonNull
    @Override
    public AppInfoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_app_info, parent, false);
        return new AppInfoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AppInfoViewHolder holder, int position) {
        AppInfo currentApp = appInfoList.get(position);

        holder.appName.setText(currentApp.getAppName());
        holder.packageName.setText(currentApp.getPackageName());

        Glide.with(context)
             .load(currentApp.getIcon())
             .placeholder(R.mipmap.ic_launcher)
             .error(R.mipmap.ic_launcher)
             .into(holder.appIcon);

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, AppDetailActivity.class);
            intent.putExtra("PACKAGE_NAME", currentApp.getPackageName());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return appInfoList.size();
    }

    public void updateData(List<AppInfo> newAppInfoList) {
        this.appInfoList.clear();
        if (newAppInfoList != null) {
             this.appInfoList.addAll(newAppInfoList);
        }
        notifyDataSetChanged();
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