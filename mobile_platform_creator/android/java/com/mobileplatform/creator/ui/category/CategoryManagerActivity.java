package com.mobileplatform.creator.ui.category;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.mobileplatform.creator.R;
import com.mobileplatform.creator.data.CategoryManager;
import com.mobileplatform.creator.data.model.CategoryInfo;
import com.skydoves.colorpickerview.ColorPickerDialog;
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener;

import java.util.List;

/**
 * 分类管理活动
 */
public class CategoryManagerActivity extends AppCompatActivity 
        implements CategoryAdapter.OnCategoryClickListener, CategoryAdapter.OnCategoryActionListener {
    
    // 视图组件
    private RecyclerView systemCategoriesList;
    private RecyclerView userCategoriesList;
    private TextView emptyView;
    private FloatingActionButton fab;
    
    // 适配器
    private CategoryAdapter systemAdapter;
    private CategoryAdapter userAdapter;
    
    // 分类管理器
    private CategoryManager categoryManager;
    
    // 当前编辑的分类
    private CategoryInfo currentEditCategory;
    
    // 当前选择的颜色
    private int currentSelectedColor = Color.parseColor("#2196F3");
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category_manager);
        
        // 初始化组件
        initializeComponents();
        
        // 设置工具栏
        setupToolbar();
        
        // 设置分类列表
        setupCategoryLists();
        
        // 设置添加按钮
        setupAddButton();
        
        // 加载分类数据
        loadCategories();
    }
    
    /**
     * 初始化组件
     */
    private void initializeComponents() {
        systemCategoriesList = findViewById(R.id.system_categories_list);
        userCategoriesList = findViewById(R.id.user_categories_list);
        emptyView = findViewById(R.id.empty_view);
        fab = findViewById(R.id.fab);
        
        categoryManager = CategoryManager.getInstance(this);
    }
    
    /**
     * 设置工具栏
     */
    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(true);
        }
    }
    
    /**
     * 设置分类列表
     */
    private void setupCategoryLists() {
        // 系统分类列表
        systemAdapter = new CategoryAdapter(this, true);
        systemAdapter.setOnCategoryClickListener(this);
        systemCategoriesList.setLayoutManager(new LinearLayoutManager(this));
        systemCategoriesList.setAdapter(systemAdapter);
        systemCategoriesList.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        
        // 用户分类列表
        userAdapter = new CategoryAdapter(this, false);
        userAdapter.setOnCategoryClickListener(this);
        userAdapter.setOnCategoryActionListener(this);
        userCategoriesList.setLayoutManager(new LinearLayoutManager(this));
        userCategoriesList.setAdapter(userAdapter);
        userCategoriesList.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
    }
    
    /**
     * 设置添加按钮
     */
    private void setupAddButton() {
        // FAB点击事件
        fab.setOnClickListener(v -> showCategoryDialog(null));
        
        // 标题栏添加按钮点击事件
        View addButton = findViewById(R.id.add_category_button);
        if (addButton != null) {
            addButton.setOnClickListener(v -> showCategoryDialog(null));
        }
    }
    
    /**
     * 加载分类数据
     */
    private void loadCategories() {
        // 加载系统分类
        List<CategoryInfo> systemCategories = categoryManager.getSystemCategories();
        systemAdapter.setCategories(systemCategories);
        
        // 加载用户分类
        List<CategoryInfo> userCategories = categoryManager.getUserCategories();
        userAdapter.setCategories(userCategories);
        
        // 设置空视图可见性
        if (userCategories.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            userCategoriesList.setVisibility(View.GONE);
        } else {
            emptyView.setVisibility(View.GONE);
            userCategoriesList.setVisibility(View.VISIBLE);
        }
    }
    
    /**
     * 显示分类对话框
     */
    private void showCategoryDialog(CategoryInfo category) {
        // 设置当前编辑的分类
        currentEditCategory = category;
        
        // 创建对话框视图
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_category, null);
        
        // 获取视图组件
        EditText nameInput = dialogView.findViewById(R.id.category_name_input);
        EditText descriptionInput = dialogView.findViewById(R.id.category_description_input);
        View colorPreview = dialogView.findViewById(R.id.color_preview);
        Button selectColorButton = dialogView.findViewById(R.id.select_color_button);
        
        // 如果是编辑模式，填充数据
        if (category != null) {
            nameInput.setText(category.getName());
            descriptionInput.setText(category.getDescription());
            currentSelectedColor = category.getColor();
        } else {
            currentSelectedColor = Color.parseColor("#2196F3");
        }
        
        // 设置颜色预览
        GradientDrawable colorDrawable = (GradientDrawable) colorPreview.getBackground();
        colorDrawable.setColor(currentSelectedColor);
        
        // 设置选择颜色按钮点击事件
        selectColorButton.setOnClickListener(v -> showColorPickerDialog((color) -> {
            currentSelectedColor = color;
            colorDrawable.setColor(currentSelectedColor);
        }));
        
        // 创建对话框
        String title = category == null ? "添加分类" : "编辑分类";
        
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(title)
                .setView(dialogView)
                .setPositiveButton("确定", null)
                .setNegativeButton("取消", null)
                .create();
        
        dialog.setOnShowListener(dialogInterface -> {
            Button positiveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(v -> {
                // 获取输入内容
                String name = nameInput.getText().toString().trim();
                String description = descriptionInput.getText().toString().trim();
                
                // 验证输入
                if (name.isEmpty()) {
                    nameInput.setError("请输入分类名称");
                    return;
                }
                
                // 保存分类
                if (category == null) {
                    // 添加新分类
                    CategoryInfo newCategory = new CategoryInfo();
                    newCategory.setName(name);
                    newCategory.setDescription(description);
                    newCategory.setColor(currentSelectedColor);
                    
                    if (categoryManager.addCategory(newCategory)) {
                        Snackbar.make(userCategoriesList, "分类添加成功", Snackbar.LENGTH_SHORT).show();
                    } else {
                        Snackbar.make(userCategoriesList, "分类添加失败", Snackbar.LENGTH_SHORT).show();
                    }
                } else {
                    // 更新现有分类
                    category.setName(name);
                    category.setDescription(description);
                    category.setColor(currentSelectedColor);
                    
                    if (categoryManager.updateCategory(category)) {
                        Snackbar.make(userCategoriesList, "分类更新成功", Snackbar.LENGTH_SHORT).show();
                    } else {
                        Snackbar.make(userCategoriesList, "分类更新失败", Snackbar.LENGTH_SHORT).show();
                    }
                }
                
                // 刷新列表
                loadCategories();
                
                // 关闭对话框
                dialog.dismiss();
            });
        });
        
        dialog.show();
    }
    
    /**
     * 显示颜色选择器对话框
     */
    private void showColorPickerDialog(ColorSelectedListener listener) {
        new ColorPickerDialog.Builder(this)
                .setTitle("选择颜色")
                .setPreferenceName("MyColorPickerDialog")
                .setPositiveButton("确定", (ColorEnvelopeListener) (envelope, fromUser) -> {
                    if (listener != null) {
                        listener.onColorSelected(envelope.getColor());
                    }
                })
                .setNegativeButton("取消", (dialogInterface, i) -> dialogInterface.dismiss())
                .attachAlphaSlideBar(false)
                .attachBrightnessSlideBar(true)
                .setBottomSpace(12)
                .show();
    }
    
    /**
     * 显示删除确认对话框
     */
    private void showDeleteConfirmDialog(CategoryInfo category) {
        new AlertDialog.Builder(this)
                .setTitle("删除分类")
                .setMessage("确定要删除分类\"" + category.getName() + "\"吗？其中的应用将移动到默认分类。")
                .setPositiveButton("确定", (dialog, which) -> {
                    if (categoryManager.deleteCategory(category.getId())) {
                        Snackbar.make(userCategoriesList, "分类已删除", Snackbar.LENGTH_SHORT).show();
                        loadCategories();
                    } else {
                        Snackbar.make(userCategoriesList, "删除分类失败", Snackbar.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }
    
    @Override
    public void onCategoryClick(CategoryInfo category) {
        // 显示分类详情
        Snackbar.make(userCategoriesList, "分类\"" + category.getName() + "\"包含 " + 
                category.getAppCount() + " 个应用", Snackbar.LENGTH_SHORT).show();
    }
    
    @Override
    public void onEditCategory(CategoryInfo category) {
        showCategoryDialog(category);
    }
    
    @Override
    public void onDeleteCategory(CategoryInfo category) {
        showDeleteConfirmDialog(category);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    /**
     * 颜色选择监听器接口
     */
    private interface ColorSelectedListener {
        void onColorSelected(int color);
    }
} 