package com.mobileplatform.creator.ui.category;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.mobileplatform.creator.R;
import com.mobileplatform.creator.data.model.CategoryInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * 分类列表适配器
 */
public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {
    
    // 上下文
    private final Context context;
    
    // 分类列表
    private final List<CategoryInfo> categories;
    
    // 是否为系统分类
    private final boolean isSystemCategory;
    
    // 分类点击监听器
    private OnCategoryClickListener clickListener;
    
    // 分类操作监听器
    private OnCategoryActionListener actionListener;
    
    /**
     * 构造函数
     */
    public CategoryAdapter(Context context, boolean isSystemCategory) {
        this.context = context;
        this.categories = new ArrayList<>();
        this.isSystemCategory = isSystemCategory;
    }
    
    /**
     * 设置分类列表
     */
    public void setCategories(List<CategoryInfo> categories) {
        this.categories.clear();
        if (categories != null) {
            this.categories.addAll(categories);
        }
        notifyDataSetChanged();
    }
    
    /**
     * 设置分类点击监听器
     */
    public void setOnCategoryClickListener(OnCategoryClickListener listener) {
        this.clickListener = listener;
    }
    
    /**
     * 设置分类操作监听器
     */
    public void setOnCategoryActionListener(OnCategoryActionListener listener) {
        this.actionListener = listener;
    }
    
    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_category, parent, false);
        return new CategoryViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        CategoryInfo category = categories.get(position);
        
        // 设置分类名称
        holder.categoryName.setText(category.getName());
        
        // 设置应用数量
        String appCountText = category.getAppCount() + " 个应用";
        holder.appCount.setText(appCountText);
        
        // 设置分类颜色
        GradientDrawable drawable = (GradientDrawable) holder.categoryColor.getBackground();
        drawable.setColor(category.getColor());
        
        // 设置编辑和删除按钮可见性
        if (isSystemCategory) {
            holder.editButton.setVisibility(View.GONE);
            holder.deleteButton.setVisibility(View.GONE);
        } else {
            holder.editButton.setVisibility(View.VISIBLE);
            holder.deleteButton.setVisibility(View.VISIBLE);
        }
        
        // 设置项目点击事件
        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onCategoryClick(category);
            }
        });
        
        // 设置编辑按钮点击事件
        holder.editButton.setOnClickListener(v -> {
            if (actionListener != null) {
                actionListener.onEditCategory(category);
            }
        });
        
        // 设置删除按钮点击事件
        holder.deleteButton.setOnClickListener(v -> {
            if (actionListener != null) {
                actionListener.onDeleteCategory(category);
            }
        });
    }
    
    @Override
    public int getItemCount() {
        return categories.size();
    }
    
    /**
     * 分类视图持有者
     */
    static class CategoryViewHolder extends RecyclerView.ViewHolder {
        final View categoryColor;
        final TextView categoryName;
        final TextView appCount;
        final ImageButton editButton;
        final ImageButton deleteButton;
        
        CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            categoryColor = itemView.findViewById(R.id.category_color);
            categoryName = itemView.findViewById(R.id.category_name);
            appCount = itemView.findViewById(R.id.app_count);
            editButton = itemView.findViewById(R.id.edit_button);
            deleteButton = itemView.findViewById(R.id.delete_button);
        }
    }
    
    /**
     * 分类点击监听器接口
     */
    public interface OnCategoryClickListener {
        void onCategoryClick(CategoryInfo category);
    }
    
    /**
     * 分类操作监听器接口
     */
    public interface OnCategoryActionListener {
        void onEditCategory(CategoryInfo category);
        void onDeleteCategory(CategoryInfo category);
    }
} 