"""
MPK文档生成器

用于生成MPK应用的文档
"""

import os
import json
import logging
import tempfile
import argparse
import inspect
import ast
from typing import Dict, Any, List, Optional
from . import MPKFile, MPKViewer

class DocGenerator:
    def __init__(self, mpk_file: MPKFile):
        self.mpk = mpk_file
        self.viewer = MPKViewer(mpk_file)
        self.logger = self._setup_logger()
        
    def _setup_logger(self) -> logging.Logger:
        """设置日志记录器"""
        logger = logging.getLogger("mpk_doc_generator")
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
            os.path.join(log_dir, "doc_generator.log"),
            encoding="utf-8"
        )
        file_handler.setLevel(logging.DEBUG)
        file_formatter = logging.Formatter(
            "%(asctime)s - %(name)s - %(levelname)s - %(message)s"
        )
        file_handler.setFormatter(file_formatter)
        logger.addHandler(file_handler)
        
        return logger
        
    def _extract_docstrings(self, code_content: bytes) -> Dict[str, str]:
        """从代码中提取文档字符串"""
        docstrings = {}
        try:
            tree = ast.parse(code_content.decode("utf-8"))
            for node in ast.walk(tree):
                if isinstance(node, (ast.FunctionDef, ast.ClassDef, ast.Module)):
                    docstring = ast.get_docstring(node)
                    if docstring:
                        if hasattr(node, 'name'):
                            docstrings[node.name] = docstring
                        elif isinstance(node, ast.Module):
                            docstrings["module"] = docstring
        except Exception as e:
            self.logger.warning(f"提取文档字符串失败: {e}")
        return docstrings

    def generate_markdown(self) -> str:
        """生成Markdown格式的文档"""
        metadata = self.viewer.get_metadata()
        code_info = self.viewer.get_code_info()
        resources_info = self.viewer.get_resources_info()
        signature_info = self.viewer.get_signature_info()
        
        md = f"# {metadata['应用名称']} {metadata['版本']}\n\n"
        md += f"**包名:** {metadata['包名']}\n"
        md += f"**版本名称:** {metadata['版本名称']}\n"
        md += f"**作者:** {metadata['作者']}\n"
        md += f"**创建日期:** {metadata['创建日期']}\n"
        md += f"**最低平台版本:** {metadata['最低平台版本']}\n\n"
        
        md += "## 描述\n\n"
        md += f"{metadata.get('描述', '暂无描述')}\n\n"
        
        md += "## 权限列表\n\n"
        if metadata['权限列表']:
            for perm in metadata['权限列表']:
                md += f"- `{perm}`\n"
        else:
            md += "无\n"
        md += "\n"
        
        md += "## 依赖项\n\n"
        if metadata['依赖项']:
            for dep in metadata['依赖项']:
                md += f"- `{dep}`\n"
        else:
            md += "无\n"
        md += "\n"
        
        md += "## 代码信息\n\n"
        md += f"- **类型:** {code_info['代码类型']}\n"
        md += f"- **大小:** {code_info['代码大小']}\n"
        md += f"- **入口点:** {code_info['入口点']}\n\n"

        # 提取代码文档字符串
        docstrings = self._extract_docstrings(self.mpk.code_section.code_content)
        if docstrings:
            md += "### API 文档\n\n"
            if "module" in docstrings:
                md += f"**模块文档:**\n```\n{docstrings['module']}\n```\n\n"
            for name, docstring in docstrings.items():
                if name != "module":
                    md += f"**{name}:**\n```\n{docstring}\n```\n\n"
            
        md += "## 资源信息\n\n"
        if resources_info:
            for resource in resources_info:
                md += f"- **类型:** {resource['资源类型']}\n"
                md += f"- **大小:** {resource['资源大小']}\n\n"
        else:
            md += "无\n"
        md += "\n"
        
        md += "## 签名信息\n\n"
        md += f"- **签名算法:** {signature_info['签名算法']}\n"
        # 不显示完整的签名数据和证书，避免过长
        md += f"- **证书:** {signature_info['证书'][:100]}...\n"
        
        return md

    def save_markdown(self, output_path: str) -> bool:
        """保存Markdown文档到文件"""
        try:
            md_content = self.generate_markdown()
            with open(output_path, "w", encoding="utf-8") as f:
                f.write(md_content)
            self.logger.info(f"文档保存成功: {output_path}")
            return True
        except Exception as e:
            self.logger.error(f"文档保存失败: {e}")
            return False

    @classmethod
    def from_file(cls, file_path: str) -> 'DocGenerator':
        """从文件创建文档生成器"""
        return cls(MPKFile.load(file_path))
        
    @staticmethod
    def run_command(args: List[str]) -> bool:
        """运行命令"""
        parser = argparse.ArgumentParser(description="MPK文档生成器")
        parser.add_argument("mpk_path", help="MPK文件路径")
        parser.add_argument("--output", help="输出Markdown文件路径")
        
        # 解析参数
        args = parser.parse_args(args)
        
        try:
            generator = DocGenerator.from_file(args.mpk_path)
            output_path = args.output or os.path.splitext(args.mpk_path)[0] + ".md"
            return generator.save_markdown(output_path)
        except Exception as e:
            logging.error(f"命令执行失败: {e}")
            return False 