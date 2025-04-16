"""
MPK 文件工具

此模块实现了 MPK 文件工具，用于提供命令行界面，方便用户使用 MPK 文件管理器。
"""

import os
import sys
import argparse
from typing import Dict, List, Any, Optional, Tuple, Set, Union, BinaryIO, Callable
from pathlib import Path
from .mpk_manager import MPKManager

def create_mpk(args: argparse.Namespace) -> None:
    """
    创建 MPK 文件
    
    Args:
        args: 命令行参数
    """
    # 创建 MPK 文件管理器
    manager = MPKManager(args.output)
    
    # 创建 MPK 文件
    is_created = manager.create(args.input)
    
    # 输出结果
    if is_created:
        print(f"创建 MPK 文件成功：{args.output}")
    else:
        print(f"创建 MPK 文件失败：{args.output}")
        sys.exit(1)

def unpack_mpk(args: argparse.Namespace) -> None:
    """
    解包 MPK 文件
    
    Args:
        args: 命令行参数
    """
    # 创建 MPK 文件管理器
    manager = MPKManager(args.input)
    
    # 解包 MPK 文件
    is_unpacked = manager.unpack(args.output)
    
    # 输出结果
    if is_unpacked:
        print(f"解包 MPK 文件成功：{args.output}")
    else:
        print(f"解包 MPK 文件失败：{args.output}")
        sys.exit(1)

def validate_mpk(args: argparse.Namespace) -> None:
    """
    验证 MPK 文件
    
    Args:
        args: 命令行参数
    """
    # 创建 MPK 文件管理器
    manager = MPKManager(args.input)
    
    # 验证 MPK 文件
    is_valid = manager.validate()
    
    # 输出结果
    if is_valid:
        print(f"验证 MPK 文件成功：{args.input}")
    else:
        print(f"验证 MPK 文件失败：{args.input}")
        sys.exit(1)

def show_metadata(args: argparse.Namespace) -> None:
    """
    显示元数据
    
    Args:
        args: 命令行参数
    """
    # 创建 MPK 文件管理器
    manager = MPKManager(args.input)
    
    # 获取元数据
    metadata = manager.get_metadata()
    
    # 输出结果
    if metadata:
        print("元数据:")
        for key, value in metadata.items():
            print(f"  {key}: {value}")
    else:
        print(f"获取元数据失败：{args.input}")
        sys.exit(1)

def show_code(args: argparse.Namespace) -> None:
    """
    显示代码
    
    Args:
        args: 命令行参数
    """
    # 创建 MPK 文件管理器
    manager = MPKManager(args.input)
    
    # 获取代码
    code = manager.get_code()
    
    # 输出结果
    if code:
        print("代码:")
        print(code.decode('utf-8'))
    else:
        print(f"获取代码失败：{args.input}")
        sys.exit(1)

def show_resources(args: argparse.Namespace) -> None:
    """
    显示资源
    
    Args:
        args: 命令行参数
    """
    # 创建 MPK 文件管理器
    manager = MPKManager(args.input)
    
    # 获取资源
    resources = manager.get_resources()
    
    # 输出结果
    if resources:
        print("资源:")
        for path, data in resources.items():
            print(f"  {path}: {len(data)} 字节")
    else:
        print(f"获取资源失败：{args.input}")
        sys.exit(1)

def show_signature(args: argparse.Namespace) -> None:
    """
    显示签名
    
    Args:
        args: 命令行参数
    """
    # 创建 MPK 文件管理器
    manager = MPKManager(args.input)
    
    # 获取签名
    signature = manager.get_signature()
    
    # 输出结果
    if signature:
        print("签名:")
        print(signature.hex())
    else:
        print(f"获取签名失败：{args.input}")
        sys.exit(1)

def show_certificate(args: argparse.Namespace) -> None:
    """
    显示证书
    
    Args:
        args: 命令行参数
    """
    # 创建 MPK 文件管理器
    manager = MPKManager(args.input)
    
    # 获取证书
    certificate = manager.get_certificate()
    
    # 输出结果
    if certificate:
        print("证书:")
        print(certificate.decode('utf-8'))
    else:
        print(f"获取证书失败：{args.input}")
        sys.exit(1)

def show_checksum(args: argparse.Namespace) -> None:
    """
    显示校验和
    
    Args:
        args: 命令行参数
    """
    # 创建 MPK 文件管理器
    manager = MPKManager(args.input)
    
    # 获取校验和
    checksum = manager.get_checksum()
    
    # 输出结果
    if checksum:
        print("校验和:")
        print(checksum.hex())
    else:
        print(f"获取校验和失败：{args.input}")
        sys.exit(1)

def show_version(args: argparse.Namespace) -> None:
    """
    显示版本
    
    Args:
        args: 命令行参数
    """
    # 创建 MPK 文件管理器
    manager = MPKManager(args.input)
    
    # 获取版本
    version = manager.get_version()
    
    # 输出结果
    if version is not None:
        print("版本:")
        print(version)
    else:
        print(f"获取版本失败：{args.input}")
        sys.exit(1)

def show_checksum_algorithm(args: argparse.Namespace) -> None:
    """
    显示校验和算法
    
    Args:
        args: 命令行参数
    """
    # 创建 MPK 文件管理器
    manager = MPKManager(args.input)
    
    # 获取校验和算法
    checksum_algorithm = manager.get_checksum_algorithm()
    
    # 输出结果
    if checksum_algorithm is not None:
        print("校验和算法:")
        print(checksum_algorithm)
    else:
        print(f"获取校验和算法失败：{args.input}")
        sys.exit(1)

def show_signature_algorithm(args: argparse.Namespace) -> None:
    """
    显示签名算法
    
    Args:
        args: 命令行参数
    """
    # 创建 MPK 文件管理器
    manager = MPKManager(args.input)
    
    # 获取签名算法
    signature_algorithm = manager.get_signature_algorithm()
    
    # 输出结果
    if signature_algorithm is not None:
        print("签名算法:")
        print(signature_algorithm)
    else:
        print(f"获取签名算法失败：{args.input}")
        sys.exit(1)

def show_code_type(args: argparse.Namespace) -> None:
    """
    显示代码类型
    
    Args:
        args: 命令行参数
    """
    # 创建 MPK 文件管理器
    manager = MPKManager(args.input)
    
    # 获取代码类型
    code_type = manager.get_code_type()
    
    # 输出结果
    if code_type is not None:
        print("代码类型:")
        print(code_type)
    else:
        print(f"获取代码类型失败：{args.input}")
        sys.exit(1)

def show_resource_types(args: argparse.Namespace) -> None:
    """
    显示资源类型
    
    Args:
        args: 命令行参数
    """
    # 创建 MPK 文件管理器
    manager = MPKManager(args.input)
    
    # 获取资源类型
    resource_types = manager.get_resource_types()
    
    # 输出结果
    if resource_types:
        print("资源类型:")
        for path, resource_type in resource_types.items():
            print(f"  {path}: {resource_type}")
    else:
        print(f"获取资源类型失败：{args.input}")
        sys.exit(1)

def main() -> None:
    """
    主函数
    """
    # 创建命令行参数解析器
    parser = argparse.ArgumentParser(description="MPK 文件工具")
    subparsers = parser.add_subparsers(dest="command", help="命令")
    
    # 创建 MPK 文件命令
    create_parser = subparsers.add_parser("create", help="创建 MPK 文件")
    create_parser.add_argument("input", help="输入目录")
    create_parser.add_argument("output", help="输出文件")
    create_parser.set_defaults(func=create_mpk)
    
    # 解包 MPK 文件命令
    unpack_parser = subparsers.add_parser("unpack", help="解包 MPK 文件")
    unpack_parser.add_argument("input", help="输入文件")
    unpack_parser.add_argument("output", help="输出目录")
    unpack_parser.set_defaults(func=unpack_mpk)
    
    # 验证 MPK 文件命令
    validate_parser = subparsers.add_parser("validate", help="验证 MPK 文件")
    validate_parser.add_argument("input", help="输入文件")
    validate_parser.set_defaults(func=validate_mpk)
    
    # 显示元数据命令
    metadata_parser = subparsers.add_parser("metadata", help="显示元数据")
    metadata_parser.add_argument("input", help="输入文件")
    metadata_parser.set_defaults(func=show_metadata)
    
    # 显示代码命令
    code_parser = subparsers.add_parser("code", help="显示代码")
    code_parser.add_argument("input", help="输入文件")
    code_parser.set_defaults(func=show_code)
    
    # 显示资源命令
    resources_parser = subparsers.add_parser("resources", help="显示资源")
    resources_parser.add_argument("input", help="输入文件")
    resources_parser.set_defaults(func=show_resources)
    
    # 显示签名命令
    signature_parser = subparsers.add_parser("signature", help="显示签名")
    signature_parser.add_argument("input", help="输入文件")
    signature_parser.set_defaults(func=show_signature)
    
    # 显示证书命令
    certificate_parser = subparsers.add_parser("certificate", help="显示证书")
    certificate_parser.add_argument("input", help="输入文件")
    certificate_parser.set_defaults(func=show_certificate)
    
    # 显示校验和命令
    checksum_parser = subparsers.add_parser("checksum", help="显示校验和")
    checksum_parser.add_argument("input", help="输入文件")
    checksum_parser.set_defaults(func=show_checksum)
    
    # 显示版本命令
    version_parser = subparsers.add_parser("version", help="显示版本")
    version_parser.add_argument("input", help="输入文件")
    version_parser.set_defaults(func=show_version)
    
    # 显示校验和算法命令
    checksum_algorithm_parser = subparsers.add_parser("checksum-algorithm", help="显示校验和算法")
    checksum_algorithm_parser.add_argument("input", help="输入文件")
    checksum_algorithm_parser.set_defaults(func=show_checksum_algorithm)
    
    # 显示签名算法命令
    signature_algorithm_parser = subparsers.add_parser("signature-algorithm", help="显示签名算法")
    signature_algorithm_parser.add_argument("input", help="输入文件")
    signature_algorithm_parser.set_defaults(func=show_signature_algorithm)
    
    # 显示代码类型命令
    code_type_parser = subparsers.add_parser("code-type", help="显示代码类型")
    code_type_parser.add_argument("input", help="输入文件")
    code_type_parser.set_defaults(func=show_code_type)
    
    # 显示资源类型命令
    resource_types_parser = subparsers.add_parser("resource-types", help="显示资源类型")
    resource_types_parser.add_argument("input", help="输入文件")
    resource_types_parser.set_defaults(func=show_resource_types)
    
    # 解析命令行参数
    args = parser.parse_args()
    
    # 执行命令
    if args.command:
        args.func(args)
    else:
        parser.print_help()

if __name__ == "__main__":
    main() 