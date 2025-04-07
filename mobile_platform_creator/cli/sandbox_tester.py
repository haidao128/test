#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
沙箱测试命令行工具
==============

用于测试沙箱功能的命令行工具，支持以不同安全级别运行应用。

用法：
  python sandbox_tester.py run <app_id> <app_path> [--level=<level>]
  python sandbox_tester.py info
  python sandbox_tester.py reset

选项：
  --level=<level>  沙箱安全级别 [默认: strict] (可选: minimal, standard, strict)
  -h, --help       显示帮助信息
  -v, --verbose    显示详细日志
"""

import os
import sys
import json
import logging
import argparse
from typing import Dict, Any, List, Optional

# 将项目根目录添加到模块搜索路径
project_root = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
sys.path.insert(0, project_root)

# 导入沙箱模块
from mobile_platform_creator.core.sandbox import init as init_sandbox
from mobile_platform_creator.core.sandbox import reset_sandbox, get_sandbox_info
from mobile_platform_creator.core.sandbox.sandbox_runner import run_app


def setup_logging(verbose: bool = False) -> None:
    """
    设置日志级别
    
    Args:
        verbose: 是否显示详细日志
    """
    log_level = logging.DEBUG if verbose else logging.INFO
    logging.basicConfig(
        level=log_level,
        format="%(asctime)s - %(name)s - %(levelname)s - %(message)s"
    )


def run_command(args: argparse.Namespace) -> int:
    """
    运行应用命令
    
    Args:
        args: 命令行参数
        
    Returns:
        int: 退出码
    """
    app_id = args.app_id
    app_path = args.app_path
    level = args.level
    
    # 初始化沙箱
    logging.info(f"初始化沙箱，安全级别: {level}")
    if not init_sandbox(level):
        logging.error("沙箱初始化失败")
        return 1
    
    # 运行应用
    logging.info(f"运行应用: {app_id}")
    result = run_app(app_id, app_path, level)
    
    # 输出结果
    if result["success"]:
        logging.info(f"应用运行成功，退出码: {result.get('exit_code', 0)}")
    else:
        logging.error(f"应用运行失败: {result.get('error', '未知错误')}")
    
    # 输出日志
    logging.info("\n===== 应用日志 =====")
    for log in result.get("logs", []):
        print(log)
    
    # 输出状态信息
    logging.info("\n===== 运行状态 =====")
    status = result.get("status", {})
    for key, value in status.items():
        if isinstance(value, dict):
            logging.info(f"{key}:")
            for k, v in value.items():
                logging.info(f"  {k}: {v}")
        else:
            logging.info(f"{key}: {value}")
    
    return 0 if result["success"] else 1


def info_command(args: argparse.Namespace) -> int:
    """
    显示沙箱信息命令
    
    Args:
        args: 命令行参数
        
    Returns:
        int: 退出码
    """
    # 初始化沙箱
    if not init_sandbox("minimal"):
        logging.error("沙箱初始化失败")
        return 1
    
    # 获取沙箱信息
    info = get_sandbox_info()
    
    # 以JSON格式输出
    print(json.dumps(info, indent=2, ensure_ascii=False))
    
    return 0


def reset_command(args: argparse.Namespace) -> int:
    """
    重置沙箱命令
    
    Args:
        args: 命令行参数
        
    Returns:
        int: 退出码
    """
    # 重置沙箱
    if reset_sandbox():
        logging.info("沙箱已重置")
        return 0
    else:
        logging.error("沙箱重置失败")
        return 1


def main() -> int:
    """
    主函数
    
    Returns:
        int: 退出码
    """
    # 创建解析器
    parser = argparse.ArgumentParser(description="沙箱测试命令行工具")
    parser.add_argument("-v", "--verbose", action="store_true", help="显示详细日志")
    
    # 创建子命令解析器
    subparsers = parser.add_subparsers(dest="command", help="命令")
    
    # run 命令
    run_parser = subparsers.add_parser("run", help="运行应用")
    run_parser.add_argument("app_id", help="应用ID")
    run_parser.add_argument("app_path", help="应用路径")
    run_parser.add_argument("--level", choices=["minimal", "standard", "strict"], 
                         default="strict", help="沙箱安全级别")
    
    # info 命令
    info_parser = subparsers.add_parser("info", help="显示沙箱信息")
    
    # reset 命令
    reset_parser = subparsers.add_parser("reset", help="重置沙箱")
    
    # 解析参数
    args = parser.parse_args()
    
    # 设置日志级别
    setup_logging(args.verbose)
    
    # 执行命令
    if args.command == "run":
        return run_command(args)
    elif args.command == "info":
        return info_command(args)
    elif args.command == "reset":
        return reset_command(args)
    else:
        parser.print_help()
        return 1


if __name__ == "__main__":
    sys.exit(main()) 