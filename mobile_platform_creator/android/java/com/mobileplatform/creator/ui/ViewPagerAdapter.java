package com.mobileplatform.creator.ui;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * ViewPager2适配器，用于管理主界面的Fragment
 */
public class ViewPagerAdapter extends FragmentStateAdapter {
    
    private final List<Fragment> fragments = new ArrayList<>();
    
    /**
     * 创建ViewPager适配器
     */
    public ViewPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }
    
    /**
     * 添加Fragment到适配器
     */
    public void addFragment(Fragment fragment) {
        fragments.add(fragment);
    }
    
    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return fragments.get(position);
    }
    
    @Override
    public int getItemCount() {
        return fragments.size();
    }
} 