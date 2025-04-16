package com.mobileplatform.creator.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.mobileplatform.creator.R;
import com.mobileplatform.creator.model.AppInfo;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 批量管理的应用列表适配器
 */
public class BatchAppAdapter extends RecyclerView.Adapter<BatchAppAdapter.BatchAppViewHolder> {

    private List<AppInfo> appList;
    private Set<String> selectedPackages;
    private OnAppSelectListener listener;

    public interface OnAppSelectListener {
        void onAppSelected(AppInfo app, boolean isSelected);
        void onAppRemoved(AppInfo app);
    }

    public BatchAppAdapter(OnAppSelectListener listener) {
        this.appList = new ArrayList<>();
        this.selectedPackages = new HashSet<>();
        this.listener = listener;
    }

    @NonNull
    @Override
    public BatchAppViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_batch_app, parent, false);
        return new BatchAppViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BatchAppViewHolder holder, int position) {
        AppInfo app = appList.get(position);
        holder.bind(app, selectedPackages.contains(app.getPackageName()));
    }

    @Override
    public int getItemCount() {
        return appList.size();
    }

    public void setAppList(List<AppInfo> apps) {
        this.appList = new ArrayList<>(apps);
        notifyDataSetChanged();
    }

    public void addApp(AppInfo app) {
        if (!containsApp(app.getPackageName())) {
            this.appList.add(app);
            notifyItemInserted(appList.size() - 1);
        }
    }

    public void removeApp(String packageName) {
        for (int i = 0; i < appList.size(); i++) {
            if (appList.get(i).getPackageName().equals(packageName)) {
                appList.remove(i);
                selectedPackages.remove(packageName);
                notifyItemRemoved(i);
                break;
            }
        }
    }

    public boolean containsApp(String packageName) {
        for (AppInfo app : appList) {
            if (app.getPackageName().equals(packageName)) {
                return true;
            }
        }
        return false;
    }

    public void selectAll() {
        for (AppInfo app : appList) {
            selectedPackages.add(app.getPackageName());
            if (listener != null) {
                listener.onAppSelected(app, true);
            }
        }
        notifyDataSetChanged();
    }

    public void clearSelection() {
        selectedPackages.clear();
        notifyDataSetChanged();
        for (AppInfo app : appList) {
            if (listener != null) {
                listener.onAppSelected(app, false);
            }
        }
    }

    public List<AppInfo> getSelectedApps() {
        List<AppInfo> selected = new ArrayList<>();
        for (AppInfo app : appList) {
            if (selectedPackages.contains(app.getPackageName())) {
                selected.add(app);
            }
        }
        return selected;
    }

    public int getSelectedCount() {
        return selectedPackages.size();
    }

    class BatchAppViewHolder extends RecyclerView.ViewHolder {
        private CheckBox checkBox;
        private ImageView appIconView;
        private TextView appNameView;
        private TextView packageNameView;
        private ImageButton removeButton;

        public BatchAppViewHolder(@NonNull View itemView) {
            super(itemView);
            checkBox = itemView.findViewById(R.id.checkbox_app);
            appIconView = itemView.findViewById(R.id.image_app_icon);
            appNameView = itemView.findViewById(R.id.text_app_name);
            packageNameView = itemView.findViewById(R.id.text_app_package);
            removeButton = itemView.findViewById(R.id.button_remove_app);
        }

        public void bind(final AppInfo app, boolean isSelected) {
            appNameView.setText(app.getAppName());
            packageNameView.setText(app.getPackageName());
            appIconView.setImageDrawable(app.getIcon());
            checkBox.setChecked(isSelected);

            // 设置选中状态改变的监听器
            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    selectedPackages.add(app.getPackageName());
                } else {
                    selectedPackages.remove(app.getPackageName());
                }
                if (listener != null) {
                    listener.onAppSelected(app, isChecked);
                }
            });

            // 设置移除按钮的点击事件
            removeButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onAppRemoved(app);
                }
                removeApp(app.getPackageName());
            });

            // 设置整个条目的点击事件
            itemView.setOnClickListener(v -> {
                boolean newState = !checkBox.isChecked();
                checkBox.setChecked(newState);
            });
        }
    }
} 