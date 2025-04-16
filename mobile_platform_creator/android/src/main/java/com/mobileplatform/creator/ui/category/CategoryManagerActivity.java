package com.mobileplatform.creator.ui.category;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.mobileplatform.creator.R;
import com.mobileplatform.creator.adapter.CategoryAdapter;
import com.mobileplatform.creator.model.Category;
import com.mobileplatform.creator.viewmodel.CategoryViewModel;

import java.util.UUID;

/**
 * 分类管理界面
 */
public class CategoryManagerActivity extends AppCompatActivity implements CategoryAdapter.OnCategoryClickListener {

    private CategoryViewModel viewModel;
    private CategoryAdapter adapter;
    private RecyclerView recyclerView;
    private FloatingActionButton fabAddCategory;
    private View progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category_manager);

        // 设置工具栏
        Toolbar toolbar = findViewById(R.id.toolbar_category);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // 初始化视图
        recyclerView = findViewById(R.id.recyclerView_categories);
        fabAddCategory = findViewById(R.id.fab_add_category);
        progressBar = findViewById(R.id.progressBar_loading);

        // 设置RecyclerView
        adapter = new CategoryAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // 初始化ViewModel
        viewModel = new ViewModelProvider(this).get(CategoryViewModel.class);
        viewModel.getAllCategories().observe(this, categories -> {
            adapter.submitList(categories);
            progressBar.setVisibility(View.GONE);
        });

        // 设置添加分类按钮点击事件
        fabAddCategory.setOnClickListener(v -> showAddCategoryDialog());
    }

    @Override
    public void onCategoryClick(Category category) {
        showEditCategoryDialog(category);
    }

    @Override
    public void onCategoryDeleteClick(Category category) {
        showDeleteCategoryDialog(category);
    }

    private void showAddCategoryDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_category_edit, null);
        TextInputEditText nameInput = dialogView.findViewById(R.id.editText_category_name);
        TextInputEditText descriptionInput = dialogView.findViewById(R.id.editText_category_description);

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.category_add)
                .setView(dialogView)
                .setPositiveButton(R.string.action_save, (dialog, which) -> {
                    String name = nameInput.getText().toString().trim();
                    String description = descriptionInput.getText().toString().trim();

                    if (name.isEmpty()) {
                        Toast.makeText(this, R.string.category_error_empty_name, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Category category = new Category(UUID.randomUUID().toString(), name, description);
                    viewModel.insert(category);
                    Toast.makeText(this, R.string.category_add_success, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void showEditCategoryDialog(Category category) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_category_edit, null);
        TextInputEditText nameInput = dialogView.findViewById(R.id.editText_category_name);
        TextInputEditText descriptionInput = dialogView.findViewById(R.id.editText_category_description);

        nameInput.setText(category.getName());
        descriptionInput.setText(category.getDescription());

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.category_edit)
                .setView(dialogView)
                .setPositiveButton(R.string.action_save, (dialog, which) -> {
                    String name = nameInput.getText().toString().trim();
                    String description = descriptionInput.getText().toString().trim();

                    if (name.isEmpty()) {
                        Toast.makeText(this, R.string.category_error_empty_name, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    category.setName(name);
                    category.setDescription(description);
                    viewModel.update(category);
                    Toast.makeText(this, R.string.category_edit_success, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void showDeleteCategoryDialog(Category category) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.category_delete)
                .setMessage(R.string.category_delete_confirm)
                .setPositiveButton(R.string.action_confirm, (dialog, which) -> {
                    viewModel.delete(category);
                    Toast.makeText(this, R.string.category_delete_success, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
} 