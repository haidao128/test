package com.mobileplatform.creator.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.mobileplatform.creator.R;
import com.mobileplatform.creator.model.Category;

/**
 * 分类列表适配器
 */
public class CategoryAdapter extends ListAdapter<Category, CategoryAdapter.CategoryViewHolder> {

    private final OnCategoryClickListener listener;

    public CategoryAdapter(OnCategoryClickListener listener) {
        super(new DiffUtil.ItemCallback<Category>() {
            @Override
            public boolean areItemsTheSame(@NonNull Category oldItem, @NonNull Category newItem) {
                return oldItem.getId().equals(newItem.getId());
            }

            @Override
            public boolean areContentsTheSame(@NonNull Category oldItem, @NonNull Category newItem) {
                return oldItem.getName().equals(newItem.getName()) &&
                       oldItem.getDescription().equals(newItem.getDescription()) &&
                       oldItem.getAppCount() == newItem.getAppCount();
            }
        });
        this.listener = listener;
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_category, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        Category category = getItem(position);
        holder.bind(category, listener);
    }

    static class CategoryViewHolder extends RecyclerView.ViewHolder {
        private final TextView nameTextView;
        private final TextView descriptionTextView;
        private final TextView appCountTextView;
        private final ImageButton deleteButton;

        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.textView_category_name);
            descriptionTextView = itemView.findViewById(R.id.textView_category_description);
            appCountTextView = itemView.findViewById(R.id.textView_app_count);
            deleteButton = itemView.findViewById(R.id.button_delete_category);
        }

        public void bind(final Category category, final OnCategoryClickListener listener) {
            nameTextView.setText(category.getName());
            descriptionTextView.setText(category.getDescription());
            appCountTextView.setText(itemView.getContext().getString(
                    R.string.category_app_count, category.getAppCount()));

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onCategoryClick(category);
                }
            });

            deleteButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onCategoryDeleteClick(category);
                }
            });
        }
    }

    public interface OnCategoryClickListener {
        void onCategoryClick(Category category);
        void onCategoryDeleteClick(Category category);
    }
} 