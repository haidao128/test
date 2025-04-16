"""
MPK开发工具

用于帮助开发者创建和测试MPK应用
"""

import os
import sys
import json
import shutil
import logging
import tempfile
import argparse
from typing import Dict, Any, List, Optional
from . import MPKFile, MPKPacker, MPKViewer, MPKLoader
from .runtime import Runtime
import traceback
from datetime import datetime
from .packer import MPKPacker
from .viewer import MPKViewer
from . import is_valid_mpk, is_compatible_with_android

class DevTools:
    def __init__(self):
        self.logger = self._setup_logger()
        self.runtime = Runtime.get_instance()
        
    def _setup_logger(self) -> logging.Logger:
        """设置日志记录器"""
        logger = logging.getLogger("mpk_dev_tools")
        logger.setLevel(logging.INFO)
        
        # 控制台处理器
        console_handler = logging.StreamHandler()
        console_handler.setLevel(logging.INFO)
        console_formatter = logging.Formatter(
            "%(asctime)s - %(name)s - %(levelname)s - %(message)s"
        )
        console_handler.setFormatter(console_formatter)
        logger.addHandler(console_handler)
        
        # 文件处理器
        log_dir = os.path.join(tempfile.gettempdir(), "mpk_logs")
        os.makedirs(log_dir, exist_ok=True)
        file_handler = logging.FileHandler(
            os.path.join(log_dir, "dev_tools.log"),
            encoding="utf-8"
        )
        file_handler.setLevel(logging.DEBUG)
        file_formatter = logging.Formatter(
            "%(asctime)s - %(name)s - %(levelname)s - %(message)s"
        )
        file_handler.setFormatter(file_formatter)
        logger.addHandler(file_handler)
        
        return logger
        
    def create_project(self, project_name: str, output_dir: str = None, 
                      template: str = "basic") -> Optional[str]:
        """创建新的MPK应用项目
        
        Args:
            project_name: 项目名称
            output_dir: 输出目录，默认为当前目录
            template: 项目模板，默认为basic
            
        Returns:
            str: 项目目录路径，失败则返回None
        """
        try:
            # 如果未指定输出目录，使用当前目录
            if not output_dir:
                output_dir = os.getcwd()
                
            # 创建项目目录
            project_dir = os.path.join(output_dir, project_name)
            if os.path.exists(project_dir):
                self.logger.error(f"项目目录已存在: {project_dir}")
                return None
                
            os.makedirs(project_dir)
            
            # 创建基本结构
            os.makedirs(os.path.join(project_dir, "code"))
            os.makedirs(os.path.join(project_dir, "assets"))
            os.makedirs(os.path.join(project_dir, "config"))
            
            # 创建清单文件
            manifest = {
                "app_name": project_name,
                "package_name": f"com.example.{project_name.lower()}",
                "version": "1.0.0",
                "author": os.environ.get("USER", "unknown"),
                "create_date": datetime.now().isoformat(),
                "min_platform_version": "1.0.0",
                "permissions": [],
                "platform": "all"  # 默认支持所有平台
            }
            
            with open(os.path.join(project_dir, "manifest.json"), "w", encoding="utf-8") as f:
                json.dump(manifest, f, ensure_ascii=False, indent=2)
                
            # 根据模板创建示例代码
            if template == "basic":
                # 创建基本的JavaScript示例
                with open(os.path.join(project_dir, "code", "main.js"), "w", encoding="utf-8") as f:
                    f.write("""// 示例应用主入口
console.log("Hello, MPK!");

// 显示一个简单的消息
function showMessage() {
    alert("Welcome to MPK!");
}

// 在页面加载时调用
window.onload = function() {
    showMessage();
};
""")
                    
                # 创建示例配置文件
                with open(os.path.join(project_dir, "config", "settings.json"), "w", encoding="utf-8") as f:
                    f.write("""{"theme": "light", "debug": false}""")
                    
                # 创建README文件
                with open(os.path.join(project_dir, "README.md"), "w", encoding="utf-8") as f:
                    f.write(f"""# {project_name}

这是一个MPK应用项目。

## 开发

1. 编辑 `code/main.js` 文件，实现应用逻辑
2. 在 `assets` 目录中添加资源文件
3. 在 `config` 目录中添加配置文件
4. 编辑 `manifest.json` 文件，更新应用信息

## 构建

使用MPK开发工具构建应用：

```bash
python -m mobile_platform_creator.cli.dev_tools build {project_dir}
```

## 安装

构建后，使用MPK运行时安装应用：

```bash
python -m mobile_platform_creator.cli.dev_tools install {project_dir}/dist/{project_name}.mpk
```
""")
                
            elif template == "android":
                # 创建专为Android设计的模板
                manifest["platform"] = "android"  # 指定平台为Android
                with open(os.path.join(project_dir, "manifest.json"), "w", encoding="utf-8") as f:
                    json.dump(manifest, f, ensure_ascii=False, indent=2)
                
                # 创建Android兼容的JavaScript示例
                with open(os.path.join(project_dir, "code", "main.js"), "w", encoding="utf-8") as f:
                    f.write("""// Android应用示例
console.log("Hello, Android!");

// 初始化Android接口
function initAndroid() {
    if (typeof Android !== "undefined") {
        console.log("Android接口已加载");
        return true;
    } else {
        console.log("Android接口不可用");
        return false;
    }
}

// 使用Android Toast
function showToast(message) {
    if (initAndroid() && Android.showToast) {
        Android.showToast(message);
    } else {
        alert(message);
    }
}

// 在页面加载时调用
window.onload = function() {
    showToast("Welcome to Android MPK!");
};
""")
                
                # 创建Android特定的配置
                with open(os.path.join(project_dir, "config", "android_config.json"), "w", encoding="utf-8") as f:
                    f.write("""{"orientation": "portrait", "fullscreen": true, "keep_screen_on": false}""")
                
                # 创建Android README
                with open(os.path.join(project_dir, "README.md"), "w", encoding="utf-8") as f:
                    f.write(f"""# {project_name} (Android)

这是一个专为Android设计的MPK应用项目。

## 开发

1. 编辑 `code/main.js` 文件，实现应用逻辑
2. 在 `assets` 目录中添加资源文件
3. 在 `config` 目录中添加配置文件
4. 编辑 `manifest.json` 文件，更新应用信息

## 构建

使用MPK开发工具构建应用：

```bash
python -m mobile_platform_creator.cli.dev_tools build {project_dir}
```

## 安装到Android

构建后，使用MPK Android工具安装应用：

```bash
python -m mobile_platform_creator.cli.android_tools install {project_dir}/dist/{project_name}.mpk
```

## 运行Android应用

```bash
python -m mobile_platform_creator.cli.android_tools run {manifest["package_name"]}_{manifest["version"]}
```
""")
                
            self.logger.info(f"项目创建成功: {project_dir}")
            return project_dir
        except Exception as e:
            self.logger.error(f"创建项目失败: {e}")
            # 清理可能创建的目录
            if 'project_dir' in locals() and os.path.exists(project_dir):
                try:
                    shutil.rmtree(project_dir)
                except Exception:
                    pass
            return None
            
    def build_project(self, project_dir: str, output_file: str = None) -> Optional[str]:
        """构建MPK应用
        
        Args:
            project_dir: 项目目录
            output_file: 输出文件路径，默认为 project_dir/dist/project_name.mpk
            
        Returns:
            str: MPK文件路径，失败则返回None
        """
        try:
            # 检查项目目录是否存在
            if not os.path.exists(project_dir):
                self.logger.error(f"项目目录不存在: {project_dir}")
                return None
                
            # 检查清单文件是否存在
            manifest_path = os.path.join(project_dir, "manifest.json")
            if not os.path.exists(manifest_path):
                self.logger.error(f"清单文件不存在: {manifest_path}")
                return None
                
            # 读取清单文件
            with open(manifest_path, "r", encoding="utf-8") as f:
                manifest = json.load(f)
                
            # 检查必要字段
            for field in ["app_name", "package_name", "version"]:
                if field not in manifest:
                    self.logger.error(f"清单文件缺少字段: {field}")
                    return None
                    
            # 如果未指定输出文件，使用默认路径
            if not output_file:
                dist_dir = os.path.join(project_dir, "dist")
                os.makedirs(dist_dir, exist_ok=True)
                output_file = os.path.join(dist_dir, f"{manifest['app_name']}.mpk")
                
            # 创建打包器
            packer = MPKPacker()
            
            # 设置元数据
            packer.set_metadata(
                app_name=manifest["app_name"],
                package_name=manifest["package_name"],
                version=manifest["version"],
                author=manifest.get("author", ""),
                min_platform_version=manifest.get("min_platform_version", "1.0.0"),
                permissions=manifest.get("permissions", []),
                platform=manifest.get("platform", "all")  # 添加平台信息
            )
            
            # 添加代码文件
            code_dir = os.path.join(project_dir, "code")
            if os.path.exists(code_dir):
                # 检测代码类型
                js_files = [f for f in os.listdir(code_dir) if f.endswith(".js")]
                py_files = [f for f in os.listdir(code_dir) if f.endswith(".py")]
                wasm_files = [f for f in os.listdir(code_dir) if f.endswith(".wasm")]
                
                if js_files:
                    code_type = "javascript"
                    main_file = "main.js" if "main.js" in js_files else js_files[0]
                    code_path = os.path.join(code_dir, main_file)
                elif py_files:
                    code_type = "python"
                    main_file = "main.py" if "main.py" in py_files else py_files[0]
                    code_path = os.path.join(code_dir, main_file)
                elif wasm_files:
                    code_type = "wasm"
                    main_file = "main.wasm" if "main.wasm" in wasm_files else wasm_files[0]
                    code_path = os.path.join(code_dir, main_file)
                else:
                    self.logger.error("未找到有效的代码文件")
                    return None
                    
                # 读取代码文件
                with open(code_path, "rb") as f:
                    code_content = f.read()
                    
                packer.set_code(code_content, code_type, main_file)
            else:
                self.logger.error(f"代码目录不存在: {code_dir}")
                return None
                
            # 添加资源文件
            assets_dir = os.path.join(project_dir, "assets")
            if os.path.exists(assets_dir):
                for root, _, files in os.walk(assets_dir):
                    for file in files:
                        file_path = os.path.join(root, file)
                        rel_path = os.path.relpath(file_path, assets_dir)
                        resource_type = os.path.splitext(file)[1][1:]  # 去掉点号的扩展名
                        
                        with open(file_path, "rb") as f:
                            content = f.read()
                            
                        packer.add_resource(content, resource_type, rel_path)
                        
            # 打包MPK文件
            packer.build(output_file)
            
            self.logger.info(f"项目构建成功: {output_file}")
            return output_file
        except Exception as e:
            self.logger.error(f"构建项目失败: {e}")
            return None
            
    def test_project(self, mpk_path: str) -> bool:
        """测试项目"""
        try:
            # 加载MPK文件
            mpk_file = MPKFile.load(mpk_path)
            
            # 加载应用
            app_id = self.runtime.load_app(mpk_file)
            
            # 运行应用
            result = self.runtime.run_app(app_id)
            
            # 卸载应用
            self.runtime.unload_app(app_id)
            
            self.logger.info(f"项目测试{'成功' if result else '失败'}")
            return result
            
        except Exception as e:
            self.logger.error(f"项目测试失败: {e}")
            return False
            
    def analyze_project(self, mpk_path: str, output_dir: str) -> bool:
        """分析项目"""
        try:
            # 加载MPK文件
            mpk_file = MPKFile.load(mpk_path)
            
            # 创建查看器
            viewer = MPKViewer(mpk_file)
            
            # 生成报告
            report_path = viewer.generate_report(output_dir)
            
            # 提取资源
            resource_dir = os.path.join(output_dir, "resources")
            viewer.extract_resources(resource_dir)
            
            self.logger.info(f"项目分析成功: {report_path}")
            return True
            
        except Exception as e:
            self.logger.error(f"项目分析失败: {e}")
            return False
            
    def install_package(self, mpk_path: str) -> bool:
        """安装 MPK 包"""
        try:
            self.logger.info(f"准备安装包: {mpk_path}")
            app_id = self.runtime.install_app(mpk_path)
            self.logger.info(f"包 '{app_id}' 安装成功.")
            return True
        except FileNotFoundError:
            self.logger.error(f"安装失败: MPK 文件未找到 {mpk_path}")
            return False
        except FileExistsError as e:
            self.logger.error(f"安装失败: {e}")
            return False
        except ValueError as e:
             self.logger.error(f"安装失败: MPK 文件无效或元数据不完整 - {e}")
             return False
        except Exception as e:
            self.logger.error(f"安装过程中发生未知错误: {e}")
            traceback.print_exc() # 打印详细错误堆栈
            return False

    def uninstall_package(self, app_id: str) -> bool:
        """卸载 MPK 包"""
        try:
            self.logger.info(f"准备卸载包: {app_id}")
            result = self.runtime.uninstall_app(app_id)
            if result:
                self.logger.info(f"包 '{app_id}' 卸载成功.")
            else:
                self.logger.error(f"卸载失败: 应用 '{app_id}' 可能未安装或卸载时出错.")
            return result
        except Exception as e:
            self.logger.error(f"卸载过程中发生未知错误: {e}")
            traceback.print_exc()
            return False

    def list_packages(self) -> bool:
        """列出已安装的 MPK 包"""
        try:
            installed_apps = self.runtime.list_installed_apps()
            if not installed_apps:
                print("没有已安装的应用包.")
                return True

            print("已安装的应用包:")
            print("--------------------------------------------------")
            print(f"{'App ID':<30} {'App Name':<20} {'Version':<10} {'Install Date':<25}")
            print("--------------------------------------------------")
            for app_info in installed_apps:
                app_id = f"{app_info.get('package_name', '?')}_{app_info.get('version', '?')}"
                print(f"{app_id:<30} {app_info.get('app_name', 'N/A'):<20} {app_info.get('version', 'N/A'):<10} {app_info.get('install_date', 'N/A'):<25}")
            print("--------------------------------------------------")
            return True
        except Exception as e:
            self.logger.error(f"列出包时出错: {e}")
            traceback.print_exc()
            return False

    def run_command(self, args: List[str]) -> bool:
        """运行命令"""
        parser = argparse.ArgumentParser(description="MPK开发工具")
        subparsers = parser.add_subparsers(dest="command", help="可用的命令")
        subparsers.required = True # 确保必须提供子命令

        # 创建项目命令
        create_parser = subparsers.add_parser("create", help="创建一个新的MPK项目框架")
        create_parser.add_argument("project_name", help="要创建的项目名称")
        create_parser.add_argument("--output-dir", "-o", help="输出目录")
        create_parser.add_argument("--template", "-t", choices=["basic", "android"], default="basic", help="项目模板")

        # 构建项目命令
        build_parser = subparsers.add_parser("build", help="构建指定项目目录，生成MPK文件")
        build_parser.add_argument("project_dir", help="包含 mpk_config.json 的项目目录")
        build_parser.add_argument("-o", "--output", help="MPK文件输出目录 (默认为项目目录下的 dist/)")

        # 测试项目命令 (加载并运行 MPK 文件)
        test_parser = subparsers.add_parser("test", help="加载并运行指定的MPK文件进行测试")
        test_parser.add_argument("mpk_path", help="要测试的MPK文件路径")

        # 分析项目命令
        analyze_parser = subparsers.add_parser("analyze", help="分析指定的MPK文件，生成报告并提取资源")
        analyze_parser.add_argument("mpk_path", help="要分析的MPK文件路径")
        analyze_parser.add_argument("-o", "--output", help="分析报告和提取资源的输出目录 (默认为MPK文件同目录下的 analysis/)")

        # --- 新增命令 ---
        # 安装包命令
        install_parser = subparsers.add_parser("install", help="将指定的MPK文件安装到系统中")
        install_parser.add_argument("mpk_path", help="要安装的MPK文件路径")

        # 卸载包命令
        uninstall_parser = subparsers.add_parser("uninstall", help="从系统中卸载指定App ID的应用")
        uninstall_parser.add_argument("app_id", help="要卸载的应用ID (格式: package_name_version)")

        # 列出已安装包命令
        list_parser = subparsers.add_parser("list", help="列出所有已安装的应用包")
        # -----------------

        # 解析参数
        # 如果没有提供参数，argparse默认会从sys.argv[1:]获取
        # 但为了清晰和单元测试方便，我们显式传递 args
        try:
            parsed_args = parser.parse_args(args)
        except SystemExit:
             # argparse 在打印帮助或出错时会 SystemExit，这里捕获避免测试中断
             # 在实际命令行使用中这通常是期望行为
             return False # 或者根据情况返回 True/False

        try:
            if parsed_args.command == "create":
                return self.create_project(parsed_args.project_name, parsed_args.output_dir, parsed_args.template)

            elif parsed_args.command == "build":
                output_file = parsed_args.output or os.path.join(parsed_args.project_dir, "dist")
                return self.build_project(parsed_args.project_dir, output_file)

            elif parsed_args.command == "test":
                return self.test_project(parsed_args.mpk_path)

            elif parsed_args.command == "analyze":
                output_dir = parsed_args.output or os.path.join(os.path.dirname(parsed_args.mpk_path), "analysis")
                return self.analyze_project(parsed_args.mpk_path, output_dir)

            elif parsed_args.command == "install":
                return self.install_package(parsed_args.mpk_path)

            elif parsed_args.command == "uninstall":
                return self.uninstall_package(parsed_args.app_id)

            elif parsed_args.command == "list":
                return self.list_packages()

            else:
                # 这部分理论上不会到达，因为 subparsers.required = True
                self.logger.error(f"未知的命令: {parsed_args.command}")
                parser.print_help()
                return False

        except Exception as e:
            self.logger.error(f"命令 '{parsed_args.command}' 执行失败: {e}")
            traceback.print_exc() # 打印详细堆栈到 stderr
            return False

    @classmethod
    def get_instance(cls) -> 'DevTools':
        """获取开发工具实例"""
        if not hasattr(cls, "_instance"):
            cls._instance = cls()
        return cls._instance 

    def view_mpk(self, mpk_file: str) -> None:
        """查看MPK文件内容
        
        Args:
            mpk_file: MPK文件路径
        """
        try:
            # 检查文件是否存在
            if not os.path.exists(mpk_file):
                self.logger.error(f"文件不存在: {mpk_file}")
                return
                
            # 检查文件是否是有效的MPK文件
            if not is_valid_mpk(mpk_file):
                self.logger.error(f"无效的MPK文件: {mpk_file}")
                return
                
            # 加载MPK文件
            mpk = MPKFile.load(mpk_file)
            
            # 使用查看器显示内容
            viewer = MPKViewer()
            viewer.view(mpk)
        except Exception as e:
            self.logger.error(f"查看MPK文件失败: {e}")
            
    def is_compatible_with_android(self, mpk_file: str) -> bool:
        """检查MPK文件是否与Android兼容
        
        Args:
            mpk_file: MPK文件路径
            
        Returns:
            bool: 是否兼容Android
        """
        try:
            # 检查文件是否存在
            if not os.path.exists(mpk_file):
                self.logger.error(f"文件不存在: {mpk_file}")
                return False
                
            # 检查文件是否是有效的MPK文件
            if not is_valid_mpk(mpk_file):
                self.logger.error(f"无效的MPK文件: {mpk_file}")
                return False
                
            # 加载MPK文件
            mpk = MPKFile.load(mpk_file)
            
            # 检查是否兼容Android
            return is_compatible_with_android(mpk)
        except Exception as e:
            self.logger.error(f"检查Android兼容性失败: {e}")
            return False
            
    def convert_to_android_package(self, mpk_file: str, output_file: str = None) -> Optional[str]:
        """将MPK文件转换为Android包格式
        
        Args:
            mpk_file: MPK文件路径
            output_file: 输出文件路径，默认为MPK文件同目录下的同名ZIP文件
            
        Returns:
            str: 输出文件路径，失败则返回None
        """
        try:
            # 检查文件是否存在
            if not os.path.exists(mpk_file):
                self.logger.error(f"文件不存在: {mpk_file}")
                return None
                
            # 检查文件是否是有效的MPK文件
            if not is_valid_mpk(mpk_file):
                self.logger.error(f"无效的MPK文件: {mpk_file}")
                return None
                
            # 加载MPK文件
            mpk = MPKFile.load(mpk_file)
            
            # 检查是否兼容Android
            if not is_compatible_with_android(mpk):
                self.logger.error(f"MPK文件与Android不兼容: {mpk_file}")
                return None
                
            # 如果未指定输出文件，使用默认路径
            if not output_file:
                base_name = os.path.basename(mpk_file)
                name_without_ext = os.path.splitext(base_name)[0]
                output_file = os.path.join(os.path.dirname(mpk_file), f"{name_without_ext}.zip")
                
            # 转换为Android包
            mpk.to_zip_file(output_file)
            
            self.logger.info(f"已生成Android包: {output_file}")
            return output_file
        except Exception as e:
            self.logger.error(f"生成Android包失败: {e}")
            return None
            
def main() -> int:
    """命令行入口函数"""
    parser = argparse.ArgumentParser(description="MPK应用开发工具")
    subparsers = parser.add_subparsers(dest="command", help="命令")
    
    # create命令
    create_parser = subparsers.add_parser("create", help="创建新的MPK应用项目")
    create_parser.add_argument("project_name", help="项目名称")
    create_parser.add_argument("--output-dir", "-o", help="输出目录")
    create_parser.add_argument("--template", "-t", choices=["basic", "android"], default="basic", help="项目模板")
    
    # build命令
    build_parser = subparsers.add_parser("build", help="构建MPK应用")
    build_parser.add_argument("project_dir", help="项目目录")
    build_parser.add_argument("--output", "-o", help="输出文件路径")
    
    # install命令
    install_parser = subparsers.add_parser("install", help="安装MPK应用")
    install_parser.add_argument("mpk_file", help="MPK文件路径")
    
    # uninstall命令
    uninstall_parser = subparsers.add_parser("uninstall", help="卸载MPK应用")
    uninstall_parser.add_argument("app_id", help="应用ID")
    
    # list命令
    list_parser = subparsers.add_parser("list", help="列出已安装的应用")
    
    # view命令
    view_parser = subparsers.add_parser("view", help="查看MPK文件内容")
    view_parser.add_argument("mpk_file", help="MPK文件路径")
    
    # check-android命令
    check_android_parser = subparsers.add_parser("check-android", help="检查MPK文件是否与Android兼容")
    check_android_parser.add_argument("mpk_file", help="MPK文件路径")
    
    # android-package命令
    android_package_parser = subparsers.add_parser("android-package", help="将MPK文件转换为Android包格式")
    android_package_parser.add_argument("mpk_file", help="MPK文件路径")
    android_package_parser.add_argument("--output", "-o", help="输出文件路径")
    
    args = parser.parse_args()
    
    # 初始化开发工具
    dev_tools = DevTools()
    
    # 根据命令执行相应的操作
    if args.command == "create":
        if dev_tools.create_project(args.project_name, args.output_dir, args.template):
            return 0
        else:
            return 1
    elif args.command == "build":
        if dev_tools.build_project(args.project_dir, args.output):
            return 0
        else:
            return 1
    elif args.command == "install":
        if dev_tools.install_package(args.mpk_file):
            return 0
        else:
            return 1
    elif args.command == "uninstall":
        if dev_tools.uninstall_package(args.app_id):
            return 0
        else:
            return 1
    elif args.command == "list":
        apps = dev_tools.list_packages()
        for app in apps:
            print(f"{app['package_name']}_{app['version']}: {app['app_name']}")
        return 0
    elif args.command == "view":
        dev_tools.view_mpk(args.mpk_file)
        return 0
    elif args.command == "check-android":
        if dev_tools.is_compatible_with_android(args.mpk_file):
            print(f"MPK文件 '{args.mpk_file}' 与Android兼容")
            return 0
        else:
            print(f"MPK文件 '{args.mpk_file}' 与Android不兼容")
            return 1
    elif args.command == "android-package":
        if dev_tools.convert_to_android_package(args.mpk_file, args.output):
            return 0
        else:
            return 1
    else:
        parser.print_help()
        return 0
        
if __name__ == "__main__":
    sys.exit(main()) 