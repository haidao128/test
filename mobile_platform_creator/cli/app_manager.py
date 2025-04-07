#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
应用管理命令行工具
==============

提供命令行界面，用于管理MPK包和应用，包括应用的创建、打包、安装、更新等功能。
"""

import os
import sys
import argparse
import logging
import json
import tempfile
import shutil
from typing import Dict, List, Any, Optional

# 确保模块可以被导入
project_root = os.path.abspath(os.path.join(os.path.dirname(__file__), '../..'))
sys.path.insert(0, project_root)

from mobile_platform_creator.utils.mpk_package import MPKPackage, is_valid_mpk
from mobile_platform_creator.utils.app_store import AppStoreClient, LocalAppRepository
from mobile_platform_creator.utils.app_updater import AppUpdater

# 配置日志
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)

logger = logging.getLogger('app-manager')


def create_mpk_command(args):
    """创建MPK包命令"""
    logger.info(f"正在创建MPK包: {args.output}")
    
    # 检查源目录是否存在
    if not os.path.exists(args.source_dir) or not os.path.isdir(args.source_dir):
        logger.error(f"源目录不存在: {args.source_dir}")
        return 1
    
    try:
        # 检查是否存在清单文件
        manifest_path = os.path.join(args.source_dir, "manifest.json")
        if os.path.exists(manifest_path):
            with open(manifest_path, 'r', encoding='utf-8') as f:
                manifest = json.load(f)
        else:
            # 创建默认清单
            manifest = {
                "id": args.id or "com.example.app",
                "name": args.name or "应用程序",
                "version": args.version or "1.0.0",
                "description": args.description or "MPK应用",
                "author": args.author or "开发者",
                "platform": args.platform or "desktop",
                "min_platform_version": "1.0.0",
                "permissions": []
            }
        
        # 使用命令行参数覆盖清单
        if args.id:
            manifest["id"] = args.id
        if args.name:
            manifest["name"] = args.name
        if args.version:
            manifest["version"] = args.version
        if args.description:
            manifest["description"] = args.description
        if args.author:
            manifest["author"] = args.author
        if args.platform:
            manifest["platform"] = args.platform
        
        # 确保输出目录存在
        os.makedirs(os.path.dirname(os.path.abspath(args.output)), exist_ok=True)
        
        # 创建MPK包
        package = MPKPackage.create_from_directory(args.source_dir, args.output, manifest)
        
        logger.info(f"已创建MPK包: {args.output}")
        
        # 显示包信息
        logger.info(f"应用ID: {manifest['id']}")
        logger.info(f"应用名称: {manifest['name']}")
        logger.info(f"版本: {manifest['version']}")
        
        return 0
    
    except Exception as e:
        logger.error(f"创建MPK包失败: {e}")
        return 1


def install_mpk_command(args):
    """安装MPK包命令"""
    logger.info(f"正在安装MPK包: {args.mpk_file}")
    
    # 检查MPK文件是否存在
    if not os.path.exists(args.mpk_file):
        logger.error(f"MPK文件不存在: {args.mpk_file}")
        return 1
    
    try:
        # 验证MPK文件
        if not is_valid_mpk(args.mpk_file):
            logger.error(f"无效的MPK文件: {args.mpk_file}")
            return 1
        
        # 创建客户端
        client = AppStoreClient()
        
        # 将应用添加到仓库
        repo = LocalAppRepository(client.repo_dir)
        app_info = repo.add_app(args.mpk_file)
        
        # 安装应用
        app_id = app_info["app_id"]
        install_result = client.install_app(app_id, args.target_dir)
        
        logger.info(f"已安装应用: {app_id}")
        logger.info(f"安装目录: {install_result['install_dir']}")
        
        return 0
    
    except Exception as e:
        logger.error(f"安装MPK包失败: {e}")
        return 1


def list_apps_command(args):
    """列出应用命令"""
    logger.info("正在列出应用...")
    
    try:
        # 创建客户端
        client = AppStoreClient()
        
        # 获取本地索引
        local_index = client.local_index
        
        # 列出应用
        print("\n已安装的应用:")
        print("-" * 60)
        print(f"{'应用ID':<30} {'名称':<20} {'版本':<10} {'状态':<10}")
        print("-" * 60)
        
        installed_count = 0
        for app_id, app_info in local_index.get("apps", {}).items():
            if app_info.get("installed", False):
                installed_count += 1
                print(f"{app_id:<30} {app_info.get('name', '未知'):<20} {app_info.get('version', '未知'):<10} {'已安装':<10}")
        
        if installed_count == 0:
            print("没有已安装的应用")
        
        print("\n已下载的应用:")
        print("-" * 60)
        print(f"{'应用ID':<30} {'名称':<20} {'版本':<10} {'状态':<10}")
        print("-" * 60)
        
        downloaded_count = 0
        for app_id, app_info in local_index.get("apps", {}).items():
            if not app_info.get("installed", False):
                downloaded_count += 1
                print(f"{app_id:<30} {app_info.get('name', '未知'):<20} {app_info.get('version', '未知'):<10} {'已下载':<10}")
        
        if downloaded_count == 0:
            print("没有已下载但未安装的应用")
        
        return 0
    
    except Exception as e:
        logger.error(f"列出应用失败: {e}")
        return 1


def update_apps_command(args):
    """更新应用命令"""
    logger.info("正在检查应用更新...")
    
    try:
        # 创建客户端
        client = AppStoreClient()
        
        # 检查更新
        updates = client.check_updates()
        
        if not updates:
            logger.info("所有应用均为最新版本")
            return 0
        
        # 显示可用更新
        print("\n可用更新:")
        print("-" * 70)
        print(f"{'应用ID':<30} {'名称':<20} {'当前版本':<10} {'最新版本':<10}")
        print("-" * 70)
        
        for app_id, update_info in updates.items():
            print(f"{app_id:<30} {update_info['name']:<20} {update_info['current_version']:<10} {update_info['latest_version']:<10}")
        
        # 更新所有应用或指定应用
        if args.app_id:
            # 更新指定应用
            if args.app_id in updates:
                logger.info(f"正在更新应用: {args.app_id}")
                result = client.update_app(args.app_id)
                logger.info(f"更新结果: {result}")
            else:
                logger.info(f"应用已是最新版本: {args.app_id}")
        elif args.all:
            # 更新所有应用
            logger.info("正在更新所有应用...")
            for app_id in updates:
                logger.info(f"正在更新应用: {app_id}")
                result = client.update_app(app_id)
                logger.info(f"更新结果: {result}")
            logger.info("所有应用更新完成")
        else:
            logger.info("要更新应用，请使用 --app-id 或 --all 参数")
        
        return 0
    
    except Exception as e:
        logger.error(f"更新应用失败: {e}")
        return 1


def uninstall_app_command(args):
    """卸载应用命令"""
    logger.info(f"正在卸载应用: {args.app_id}")
    
    try:
        # 创建客户端
        client = AppStoreClient()
        
        # 卸载应用
        result = client.uninstall_app(args.app_id)
        
        if result:
            logger.info(f"已卸载应用: {args.app_id}")
        else:
            logger.warning(f"卸载应用失败: {args.app_id}")
        
        return 0 if result else 1
    
    except Exception as e:
        logger.error(f"卸载应用失败: {e}")
        return 1


def search_apps_command(args):
    """搜索应用命令"""
    logger.info(f"正在搜索应用: {args.query}")
    
    try:
        # 创建客户端
        client = AppStoreClient()
        
        # 搜索应用
        results = client.search_apps(args.query, args.category, args.limit, args.offset)
        
        # 显示搜索结果
        if not results.get("results"):
            logger.info("没有找到匹配的应用")
            return 0
        
        print(f"\n搜索结果 - 共 {results.get('total', 0)} 个匹配项:")
        print("-" * 80)
        print(f"{'应用ID':<30} {'名称':<20} {'版本':<10} {'分类':<10} {'评分':<5}")
        print("-" * 80)
        
        for app in results.get("results", []):
            print(f"{app.get('id', ''):<30} {app.get('name', ''):<20} {app.get('version', ''):<10} {app.get('category', ''):<10} {app.get('rating', 0):<5.1f}")
        
        return 0
    
    except Exception as e:
        logger.error(f"搜索应用失败: {e}")
        return 1


def info_mpk_command(args):
    """显示MPK包信息命令"""
    logger.info(f"正在查看MPK包信息: {args.mpk_file}")
    
    # 检查MPK文件是否存在
    if not os.path.exists(args.mpk_file):
        logger.error(f"MPK文件不存在: {args.mpk_file}")
        return 1
    
    try:
        # 验证MPK文件
        if not is_valid_mpk(args.mpk_file):
            logger.error(f"无效的MPK文件: {args.mpk_file}")
            return 1
        
        # 解析MPK包
        package = MPKPackage(args.mpk_file)
        manifest = package.get_manifest()
        
        # 显示清单信息
        print("\nMPK包信息:")
        print("-" * 60)
        print(f"文件路径: {args.mpk_file}")
        print(f"文件大小: {os.path.getsize(args.mpk_file) / 1024:.2f} KB")
        print("-" * 60)
        print(f"应用ID: {manifest.get('id', '未知')}")
        print(f"应用名称: {manifest.get('name', '未知')}")
        print(f"版本: {manifest.get('version', '未知')}")
        print(f"描述: {manifest.get('description', '无')}")
        print(f"作者: {manifest.get('author', '未知')}")
        print(f"平台: {manifest.get('platform', '未知')}")
        print(f"最低平台版本: {manifest.get('min_platform_version', '未知')}")
        
        # 显示权限
        permissions = manifest.get('permissions', [])
        if permissions:
            print(f"权限: {', '.join(permissions)}")
        else:
            print("权限: 无")
        
        # 显示文件列表
        files = package.list_files()
        print(f"\n包含 {len(files)} 个文件:")
        print("-" * 60)
        
        # 按目录分组显示
        dirs = {}
        for file_path in files:
            dir_name = os.path.dirname(file_path) or "根目录"
            if dir_name not in dirs:
                dirs[dir_name] = []
            dirs[dir_name].append(os.path.basename(file_path))
        
        for dir_name, file_list in sorted(dirs.items()):
            print(f"{dir_name}/")
            for file_name in sorted(file_list):
                print(f"  - {file_name}")
        
        return 0
    
    except Exception as e:
        logger.error(f"显示MPK包信息失败: {e}")
        return 1


def main():
    """主函数"""
    parser = argparse.ArgumentParser(description="移动应用平台应用管理工具")
    subparsers = parser.add_subparsers(dest="command", help="子命令")
    
    # 创建MPK包命令
    create_parser = subparsers.add_parser("create", help="创建MPK包")
    create_parser.add_argument("source_dir", help="源目录")
    create_parser.add_argument("output", help="输出MPK文件路径")
    create_parser.add_argument("--id", help="应用ID")
    create_parser.add_argument("--name", help="应用名称")
    create_parser.add_argument("--version", help="应用版本")
    create_parser.add_argument("--description", help="应用描述")
    create_parser.add_argument("--author", help="应用作者")
    create_parser.add_argument("--platform", help="目标平台")
    
    # 安装MPK包命令
    install_parser = subparsers.add_parser("install", help="安装MPK包")
    install_parser.add_argument("mpk_file", help="MPK文件路径")
    install_parser.add_argument("--target-dir", help="安装目标目录")
    
    # 列出应用命令
    list_parser = subparsers.add_parser("list", help="列出应用")
    
    # 更新应用命令
    update_parser = subparsers.add_parser("update", help="更新应用")
    update_parser.add_argument("--app-id", help="应用ID")
    update_parser.add_argument("--all", action="store_true", help="更新所有应用")
    
    # 卸载应用命令
    uninstall_parser = subparsers.add_parser("uninstall", help="卸载应用")
    uninstall_parser.add_argument("app_id", help="应用ID")
    
    # 搜索应用命令
    search_parser = subparsers.add_parser("search", help="搜索应用")
    search_parser.add_argument("query", help="搜索关键词")
    search_parser.add_argument("--category", help="应用类别")
    search_parser.add_argument("--limit", type=int, default=20, help="返回结果数量限制")
    search_parser.add_argument("--offset", type=int, default=0, help="结果偏移量")
    
    # 显示MPK包信息命令
    info_parser = subparsers.add_parser("info", help="显示MPK包信息")
    info_parser.add_argument("mpk_file", help="MPK文件路径")
    
    # 解析命令行参数
    args = parser.parse_args()
    
    # 执行相应的命令
    if args.command == "create":
        return create_mpk_command(args)
    elif args.command == "install":
        return install_mpk_command(args)
    elif args.command == "list":
        return list_apps_command(args)
    elif args.command == "update":
        return update_apps_command(args)
    elif args.command == "uninstall":
        return uninstall_app_command(args)
    elif args.command == "search":
        return search_apps_command(args)
    elif args.command == "info":
        return info_mpk_command(args)
    else:
        parser.print_help()
        return 1


if __name__ == "__main__":
    sys.exit(main()) 