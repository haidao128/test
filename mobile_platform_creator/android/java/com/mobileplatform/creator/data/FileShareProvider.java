package com.mobileplatform.creator.data;

import androidx.core.content.FileProvider;

/**
 * 文件共享提供者，扩展自FileProvider，用于安全地向其他应用共享文件
 */
public class FileShareProvider extends FileProvider {
    // 继承自FileProvider，不需要添加额外代码
    // 在AndroidManifest.xml中配置了authorities和文件路径
} 