import unittest
import os
import tempfile
import shutil
import sys
import ast
from unittest.mock import patch
from mobile_platform_creator.core.mpk import MPKFile, MPKPacker
from mobile_platform_creator.core.mpk.doc_generator import DocGenerator

class TestDocGenerator(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        cls.temp_dir = tempfile.mkdtemp()
        cls.mpk_path = os.path.join(cls.temp_dir, "doc_test.mpk")
        cls.output_md_path = os.path.join(cls.temp_dir, "doc_test.md")
        cls.output_cmd_md_path = os.path.join(cls.temp_dir, "doc_cmd_test.md")

        # 创建包含文档字符串的代码
        cls.code_path = os.path.join(cls.temp_dir, "doc_code.py")
        code_content = """
'''Module docstring.'''

class MyClass:
    '''Class docstring.'''
    def my_method(self):
        '''Method docstring.'''
        pass

def my_function():
    '''Function docstring.'''
    pass

def main(): # No docstring for main
    mc = MyClass()
    mc.my_method()
    my_function()
"""
        with open(cls.code_path, "w", encoding="utf-8") as f:
            f.write(code_content)

        # 创建MPK文件
        packer = MPKPacker()
        packer.set_metadata({
            "app_name": "DocGenApp", "package_name": "com.docgen.test",
            "version": "3.0", "author": "DocGenTester", "permissions": ["p1", "p2"],
            "dependencies": ["d1"], "描述": "这是一个用于测试文档生成器的应用。"
        })
        packer.add_code(cls.code_path, entry_point="main")
        res_path = os.path.join(cls.temp_dir, "doc_res.txt")
        with open(res_path, "w") as f: f.write("Doc resource")
        packer.add_resource(res_path, "config")
        packer.set_signature("MD5", b"docsign", "doccert")
        packer.pack(cls.mpk_path)

        cls.mpk_file = MPKFile.load(cls.mpk_path)
        cls.generator = DocGenerator(cls.mpk_file)

        # 禁用日志
        cls.patcher_log = patch('mobile_platform_creator.core.mpk.doc_generator.logging')
        cls.mock_logging = cls.patcher_log.start()

    @classmethod
    def tearDownClass(cls):
        shutil.rmtree(cls.temp_dir)
        cls.patcher_log.stop()

    def test_01_extract_docstrings(self):
        """测试提取文档字符串"""
        docstrings = self.generator._extract_docstrings(self.generator.mpk.code_section.code_content)
        self.assertIn("module", docstrings)
        self.assertEqual(docstrings["module"], "Module docstring.")
        self.assertIn("MyClass", docstrings)
        self.assertEqual(docstrings["MyClass"], "Class docstring.")
        self.assertIn("my_method", docstrings)
        self.assertEqual(docstrings["my_method"], "Method docstring.")
        self.assertIn("my_function", docstrings)
        self.assertEqual(docstrings["my_function"], "Function docstring.")
        self.assertNotIn("main", docstrings) # main 没有文档字符串

    def test_02_generate_markdown(self):
        """测试生成Markdown内容"""
        md_content = self.generator.generate_markdown()
        self.assertIsInstance(md_content, str)

        # 检查关键信息是否存在
        self.assertIn("# DocGenApp 3.0", md_content)
        self.assertIn("**包名:** com.docgen.test", md_content)
        self.assertIn("## 描述", md_content)
        self.assertIn("这是一个用于测试文档生成器的应用。", md_content)
        self.assertIn("`p1`", md_content)
        self.assertIn("`p2`", md_content)
        self.assertIn("`d1`", md_content)
        self.assertIn("- **入口点:** main", md_content)
        self.assertIn("### API 文档", md_content)
        self.assertIn("**模块文档:**", md_content)
        self.assertIn("Module docstring.", md_content)
        self.assertIn("**MyClass:**", md_content)
        self.assertIn("Class docstring.", md_content)
        self.assertIn("**my_method:**", md_content)
        self.assertIn("Method docstring.", md_content)
        self.assertIn("**my_function:**", md_content)
        self.assertIn("Function docstring.", md_content)
        self.assertIn("- **类型:** config", md_content)
        self.assertIn("- **签名算法:** MD5", md_content)
        self.assertIn("doccert", md_content)

    def test_03_save_markdown(self):
        """测试保存Markdown文件"""
        result = self.generator.save_markdown(self.output_md_path)
        self.assertTrue(result)
        self.assertTrue(os.path.exists(self.output_md_path))

        # 读取保存的文件内容进行验证
        with open(self.output_md_path, "r", encoding="utf-8") as f:
            saved_content = f.read()
        self.assertTrue(len(saved_content) > 100) # 确保不是空文件
        self.assertIn("# DocGenApp 3.0", saved_content)
        self.assertIn("Function docstring.", saved_content)

    def test_04_from_file(self):
        """测试从文件创建生成器"""
        gen_from_file = DocGenerator.from_file(self.mpk_path)
        self.assertIsInstance(gen_from_file, DocGenerator)
        md_content = gen_from_file.generate_markdown()
        self.assertIn("DocGenApp", md_content)

    @patch('sys.argv', ['prog_name', '', '--output', ''])
    def test_05_run_command(self, mock_argv):
        """测试命令行工具"""
        mock_argv[1] = self.mpk_path
        mock_argv[3] = self.output_cmd_md_path

        # 模拟 save_markdown
        with patch.object(DocGenerator, 'save_markdown', return_value=True) as mock_save:
             # 需要 patch DocGenerator.from_file 内部使用的 MPKFile.load，因为它在类方法中
            with patch('mobile_platform_creator.core.mpk.doc_generator.MPKFile.load', return_value=self.mpk_file):
                result = DocGenerator.run_command(sys.argv[1:])
                self.assertTrue(result)
                # 验证 save_markdown 被正确调用
                mock_save.assert_called_once()
                # 获取传递给 save_markdown 的第一个参数 (self 实例) 和第二个参数 (output_path)
                call_args, call_kwargs = mock_save.call_args
                self.assertEqual(call_args[1], self.output_cmd_md_path)

    @patch('sys.argv', ['prog_name', ''])
    def test_06_run_command_default_output(self, mock_argv):
        """测试命令行工具默认输出路径"""
        mock_argv[1] = self.mpk_path
        default_output_path = os.path.splitext(self.mpk_path)[0] + ".md"

        with patch.object(DocGenerator, 'save_markdown', return_value=True) as mock_save:
            with patch('mobile_platform_creator.core.mpk.doc_generator.MPKFile.load', return_value=self.mpk_file):
                result = DocGenerator.run_command(sys.argv[1:])
                self.assertTrue(result)
                mock_save.assert_called_once()
                call_args, call_kwargs = mock_save.call_args
                self.assertEqual(call_args[1], default_output_path)
                # 清理生成的默认文件
                if os.path.exists(default_output_path):
                    os.remove(default_output_path)


if __name__ == '__main__':
    unittest.main() 