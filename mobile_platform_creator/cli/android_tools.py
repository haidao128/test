#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
Android命令行工具

提供Android应用管理相关的命令行工具，包括应用的安装、卸载、列表和运行。
"""

import os
import sys
import argparse
import logging
from typing import List, Dict, Any, Optional

# 导入相关模块
from ..core.mpk import MPKFile, is_valid_mpk, is_compatible_with_android
from ..core.mpk.runtime import Runtime

# 配置日志
logger = logging.getLogger("mobile_platform_creator.cli.android_tools")

def setup_logging(debug: bool = False) -> None:
    """
    设置日志记录
    
    Args:
        debug: 是否启用调试日志
    """
    level = logging.DEBUG if debug else logging.INFO
    
    handler = logging.StreamHandler()
    formatter = logging.Formatter('%(asctime)s - %(name)s - %(levelname)s - %(message)s')
    handler.setFormatter(formatter)
    
    logger.setLevel(level)
    logger.addHandler(handler)
    
    # 设置根日志记录器
    root_logger = logging.getLogger("mobile_platform_creator")
    root_logger.setLevel(level)
    
    # 如果根日志记录器没有处理器，添加一个
    if not root_logger.handlers:
        root_logger.addHandler(handler)
        
def install_app(args) -> int:
    """
    安装应用到Android设备
    
    Args:
        args: 命令行参数
        
    Returns:
        int: 退出码
    """
    mpk_path = args.mpk_file
    
    # 检查文件是否存在
    if not os.path.exists(mpk_path):
        logger.error(f"文件未找到: {mpk_path}")
        return 1
        
    # 检查文件是否是有效的MPK文件
    if not is_valid_mpk(mpk_path):
        logger.error(f"无效的MPK文件: {mpk_path}")
        return 1
        
    # 加载MPK文件
    try:
        mpk_file = MPKFile.load(mpk_path)
    except Exception as e:
        logger.error(f"加载MPK文件失败: {e}")
        return 1
        
    # 检查MPK文件是否与Android兼容
    if not is_compatible_with_android(mpk_file):
        logger.error(f"MPK文件与Android不兼容: {mpk_path}")
        return 1
        
    # 获取运行时实例并安装应用
    try:
        runtime = Runtime()
        app_id = runtime.install_app(mpk_path)
        logger.info(f"应用 '{app_id}' 安装成功")
        return 0
    except Exception as e:
        logger.error(f"安装应用失败: {e}")
        return 1
        
def uninstall_app(args) -> int:
    """
    从Android设备卸载应用
    
    Args:
        args: 命令行参数
        
    Returns:
        int: 退出码
    """
    app_id = args.app_id
    
    # 获取运行时实例并卸载应用
    try:
        runtime = Runtime()
        
        # 检查应用是否已安装
        if app_id not in runtime.get_android_installed_app_ids():
            logger.error(f"应用 '{app_id}' 未安装到Android")
            return 1
            
        # 卸载应用
        success = runtime.uninstall_app(app_id)
        if success:
            logger.info(f"应用 '{app_id}' 卸载成功")
            return 0
        else:
            logger.error(f"卸载应用 '{app_id}' 失败")
            return 1
    except Exception as e:
        logger.error(f"卸载应用失败: {e}")
        return 1
        
def list_apps(args) -> int:
    """
    列出已安装到Android的应用
    
    Args:
        args: 命令行参数
        
    Returns:
        int: 退出码
    """
    # 获取运行时实例并列出应用
    try:
        runtime = Runtime()
        apps = runtime.list_android_installed_apps()
        
        if not apps:
            print("未安装任何Android应用")
            return 0
            
        print(f"已安装的Android应用 ({len(apps)}):")
        for app in apps:
            package_name = app.get("package_name", "")
            app_name = app.get("app_name", "")
            version = app.get("version", "")
            install_date = app.get("install_date", "").split("T")[0]  # 只显示日期部分
            
            print(f"  {package_name}_{version}: {app_name} (安装日期: {install_date})")
            
        return 0
    except Exception as e:
        logger.error(f"列出应用失败: {e}")
        return 1
        
def run_app(args) -> int:
    """
    在Android设备上运行应用
    
    Args:
        args: 命令行参数
        
    Returns:
        int: 退出码
    """
    app_id = args.app_id
    
    # 获取运行时实例并运行应用
    try:
        runtime = Runtime()
        
        # 检查应用是否已安装
        if app_id not in runtime.get_android_installed_app_ids():
            logger.error(f"应用 '{app_id}' 未安装到Android")
            return 1
            
        # 运行应用
        success = runtime.run_on_android(app_id)
        if success:
            logger.info(f"应用 '{app_id}' 已在Android上启动")
            return 0
        else:
            logger.error(f"启动应用 '{app_id}' 失败")
            return 1
    except Exception as e:
        logger.error(f"运行应用失败: {e}")
        return 1
        
def check_compatibility(args) -> int:
    """
    检查MPK文件是否与Android兼容
    
    Args:
        args: 命令行参数
        
    Returns:
        int: 退出码
    """
    mpk_path = args.mpk_file
    
    # 检查文件是否存在
    if not os.path.exists(mpk_path):
        logger.error(f"文件未找到: {mpk_path}")
        return 1
        
    # 检查文件是否是有效的MPK文件
    if not is_valid_mpk(mpk_path):
        logger.error(f"无效的MPK文件: {mpk_path}")
        return 1
        
    # 加载MPK文件
    try:
        mpk_file = MPKFile.load(mpk_path)
    except Exception as e:
        logger.error(f"加载MPK文件失败: {e}")
        return 1
        
    # 检查MPK文件是否与Android兼容
    if is_compatible_with_android(mpk_file):
        print(f"MPK文件 '{mpk_path}' 与Android兼容")
        return 0
    else:
        print(f"MPK文件 '{mpk_path}' 与Android不兼容")
        return 1
        
def create_android_package(args) -> int:
    """
    将MPK文件转换为Android包格式
    
    Args:
        args: 命令行参数
        
    Returns:
        int: 退出码
    """
    mpk_path = args.mpk_file
    output_path = args.output
    
    # 如果未指定输出路径，使用默认路径
    if not output_path:
        base_name = os.path.basename(mpk_path)
        name_without_ext = os.path.splitext(base_name)[0]
        output_path = f"{name_without_ext}.zip"
    
    # 检查文件是否存在
    if not os.path.exists(mpk_path):
        logger.error(f"文件未找到: {mpk_path}")
        return 1
        
    # 检查文件是否是有效的MPK文件
    if not is_valid_mpk(mpk_path):
        logger.error(f"无效的MPK文件: {mpk_path}")
        return 1
        
    # 加载MPK文件
    try:
        mpk_file = MPKFile.load(mpk_path)
    except Exception as e:
        logger.error(f"加载MPK文件失败: {e}")
        return 1
        
    # 检查MPK文件是否与Android兼容
    if not is_compatible_with_android(mpk_file):
        logger.error(f"MPK文件与Android不兼容: {mpk_path}")
        return 1
        
    # 转换为Android包
    try:
        mpk_file.to_zip_file(output_path)
        logger.info(f"已生成Android包: {output_path}")
        return 0
    except Exception as e:
        logger.error(f"生成Android包失败: {e}")
        return 1

def main() -> int:
    """
    主函数
    
    Returns:
        int: 退出码
    """
    # 创建参数解析器
    parser = argparse.ArgumentParser(description="Android应用管理工具")
    parser.add_argument("--debug", action="store_true", help="启用调试日志")
    
    # 创建子命令
    subparsers = parser.add_subparsers(dest="command", help="命令")
    
    # install命令
    install_parser = subparsers.add_parser("install", help="安装应用到Android设备")
    install_parser.add_argument("mpk_file", help="MPK文件路径")
    install_parser.set_defaults(func=install_app)
    
    # uninstall命令
    uninstall_parser = subparsers.add_parser("uninstall", help="从Android设备卸载应用")
    uninstall_parser.add_argument("app_id", help="应用ID")
    uninstall_parser.set_defaults(func=uninstall_app)
    
    # list命令
    list_parser = subparsers.add_parser("list", help="列出已安装到Android的应用")
    list_parser.set_defaults(func=list_apps)
    
    # run命令
    run_parser = subparsers.add_parser("run", help="在Android设备上运行应用")
    run_parser.add_argument("app_id", help="应用ID")
    run_parser.set_defaults(func=run_app)
    
    # check命令
    check_parser = subparsers.add_parser("check", help="检查MPK文件是否与Android兼容")
    check_parser.add_argument("mpk_file", help="MPK文件路径")
    check_parser.set_defaults(func=check_compatibility)
    
    # create命令
    create_parser = subparsers.add_parser("create", help="将MPK文件转换为Android包格式")
    create_parser.add_argument("mpk_file", help="MPK文件路径")
    create_parser.add_argument("--output", "-o", help="输出文件路径")
    create_parser.set_defaults(func=create_android_package)
    
    # 解析参数
    args = parser.parse_args()
    
    # 设置日志
    setup_logging(args.debug)
    
    # 如果没有指定命令，显示帮助信息
    if not args.command:
        parser.print_help()
        return 0
        
    # 执行命令
    return args.func(args)
    
if __name__ == "__main__":
    sys.exit(main()) 