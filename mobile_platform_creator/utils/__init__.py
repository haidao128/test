"""
工具模块
======

该模块包含了移动应用平台的各种工具函数和类。
主要包括：

1. MPK应用包格式处理模块 (mpk_package.py)
2. 应用商店管理模块 (app_store.py)
3. 应用更新管理模块 (app_updater.py)
"""

import os
import sys
import logging

logger = logging.getLogger(__name__)

"""
通用工具模块
=========

提供应用平台使用的各种工具函数，包括:
- 文件操作
- 网络通信
- 配置管理
- 加密解密
- 日志处理
""" 