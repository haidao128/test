package com.mobileplatform.creator.ui.app;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import com.mobileplatform.creator.R;

public class AppDetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // TODO: 设置布局文件 activity_app_detail.xml
        // setContentView(R.layout.activity_app_detail);
        setTitle("应用详情");

        // 获取从 MainActivity 传递过来的包名
        String packageName = getIntent().getStringExtra("PACKAGE_NAME");

        // TODO: 创建布局并在 TextView 中显示包名，或者根据包名加载更详细的应用信息
        // TextView textView = findViewById(R.id.textView_detail_package_name);
        // if (textView != null && packageName != null) {
        //     textView.setText("Package: " + packageName);
        // }
        
        // 临时显示包名
         if (packageName != null) {
             setTitle("详情: " + packageName);
         } else {
             setTitle("应用详情 (包名未传递)");
         }
    }
} 