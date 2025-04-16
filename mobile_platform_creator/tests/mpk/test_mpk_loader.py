import unittest
import os
import tempfile
import shutil
import sys
from io import StringIO
from unittest.mock import patch, MagicMock
from mobile_platform_creator.core.mpk import MPKLoader, MPKFile, MPKPacker

class TestMPKLoader(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        cls.temp_dir = tempfile.mkdtemp()
        cls.mpk_valid_path = os.path.join(cls.temp_dir, "valid.mpk")
        cls.mpk_invalid_path = os.path.join(cls.temp_dir, "invalid.mpk")
        cls.mpk_nodeps_path = os.path.join(cls.temp_dir, "nodeps.mpk")
        cls.mpk_badperm_path = os.path.join(cls.temp_dir, "badperm.mpk")
        cls.mpk_noentry_path = os.path.join(cls.temp_dir, "noentry.mpk")
        cls.mpk_runtime_error_path = os.path.join(cls.temp_dir, "runtime_error.mpk")

        # 创建有效的 MPK
        packer_valid = MPKPacker()
        packer_valid.set_metadata({
            "app_name": "LoaderApp", "package_name": "com.loader.test",
            "version": "1.0", "dependencies": ["os"], # os 总是存在
            "permissions": ["file.read"]
        })
        packer_valid.add_code(os.path.join(cls.temp_dir, "code_valid.py"), entry_point="main")
        with open(os.path.join(cls.temp_dir, "code_valid.py"), "w") as f:
            f.write("import os\ndef main():\n    print(f'MPK_APP_NAME={os.getenv("MPK_APP_NAME")}')")
        packer_valid.add_resource(os.path.join(cls.temp_dir, "res_valid.txt"), "config")
        with open(os.path.join(cls.temp_dir, "res_valid.txt"), "w") as f:
            f.write("Valid resource")
        packer_valid.pack(cls.mpk_valid_path)

        # 创建缺少依赖的 MPK
        packer_nodeps = MPKPacker()
        packer_nodeps.set_metadata({"app_name": "NoDepsApp", "package_name": "com.nodeps", "version": "1.0", "dependencies": ["non_existent_module"]})
        packer_nodeps.add_code(os.path.join(cls.temp_dir, "code_nodeps.py"), entry_point="main")
        with open(os.path.join(cls.temp_dir, "code_nodeps.py"), "w") as f: f.write("def main(): pass")
        packer_nodeps.pack(cls.mpk_nodeps_path)

        # 创建无效权限的 MPK
        packer_badperm = MPKPacker()
        packer_badperm.set_metadata({"app_name": "BadPermApp", "package_name": "com.badperm", "version": "1.0", "permissions": ["super_admin_root"]})
        packer_badperm.add_code(os.path.join(cls.temp_dir, "code_badperm.py"), entry_point="main")
        with open(os.path.join(cls.temp_dir, "code_badperm.py"), "w") as f: f.write("def main(): pass")
        packer_badperm.pack(cls.mpk_badperm_path)

        # 创建缺少入口点的 MPK
        packer_noentry = MPKPacker()
        packer_noentry.set_metadata({"app_name": "NoEntryApp", "package_name": "com.noentry", "version": "1.0"})
        packer_noentry.add_code(os.path.join(cls.temp_dir, "code_noentry.py"), entry_point="non_existent_main")
        with open(os.path.join(cls.temp_dir, "code_noentry.py"), "w") as f: f.write("def real_main(): pass")
        packer_noentry.pack(cls.mpk_noentry_path)

        # 创建运行时错误的 MPK
        packer_runtime_error = MPKPacker()
        packer_runtime_error.set_metadata({"app_name": "ErrorApp", "package_name": "com.error", "version": "1.0"})
        packer_runtime_error.add_code(os.path.join(cls.temp_dir, "code_error.py"), entry_point="main")
        with open(os.path.join(cls.temp_dir, "code_error.py"), "w") as f: f.write("def main(): raise ValueError('Intentional Error')")
        packer_runtime_error.pack(cls.mpk_runtime_error_path)

        # 创建无效的 MPK (例如，通过破坏文件)
        shutil.copy(cls.mpk_valid_path, cls.mpk_invalid_path)
        with open(cls.mpk_invalid_path, "r+b") as f:
            f.seek(10)
            f.write(b"INVALID")

    @classmethod
    def tearDownClass(cls):
        shutil.rmtree(cls.temp_dir)

    def test_01_init_from_file_and_bytes(self):
        """测试从文件和字节初始化加载器"""
        loader_file = MPKLoader.from_file(self.mpk_valid_path)
        self.assertIsInstance(loader_file, MPKLoader)
        self.assertEqual(loader_file.mpk.metadata.app_name, "LoaderApp")

        with open(self.mpk_valid_path, "rb") as f:
            data = f.read()
        loader_bytes = MPKLoader.from_bytes(data)
        self.assertIsInstance(loader_bytes, MPKLoader)
        self.assertEqual(loader_bytes.mpk.metadata.package_name, "com.loader.test")

    def test_02_verify_valid_and_invalid(self):
        """测试验证有效和无效的MPK"""
        loader_valid = MPKLoader.from_file(self.mpk_valid_path)
        self.assertTrue(loader_valid.verify())

        # 加载被破坏的文件
        loader_invalid = MPKLoader.from_file(self.mpk_invalid_path)
        self.assertFalse(loader_invalid.verify())

    def test_03_check_dependencies_success_and_fail(self):
        """测试依赖检查成功和失败"""
        loader_valid = MPKLoader.from_file(self.mpk_valid_path)
        self.assertTrue(loader_valid.check_dependencies()) # os 应该存在

        loader_nodeps = MPKLoader.from_file(self.mpk_nodeps_path)
        # 捕获标准输出以检查错误消息
        captured_output = StringIO()
        sys.stdout = captured_output
        self.assertFalse(loader_nodeps.check_dependencies())
        sys.stdout = sys.__stdout__ # 恢复标准输出
        self.assertIn("缺少依赖: non_existent_module", captured_output.getvalue())

    def test_04_check_permissions_success_and_fail(self):
        """测试权限检查成功和失败"""
        loader_valid = MPKLoader.from_file(self.mpk_valid_path)
        self.assertTrue(loader_valid.check_permissions())

        loader_badperm = MPKLoader.from_file(self.mpk_badperm_path)
        captured_output = StringIO()
        sys.stdout = captured_output
        self.assertFalse(loader_badperm.check_permissions())
        sys.stdout = sys.__stdout__
        self.assertIn("不允许的权限: super_admin_root", captured_output.getvalue())

    def test_05_extract_resources(self):
        """测试提取资源"""
        loader = MPKLoader.from_file(self.mpk_valid_path)
        resource_dir = loader.extract_resources()
        self.assertTrue(os.path.isdir(resource_dir))
        self.assertTrue(resource_dir.startswith(tempfile.gettempdir())) # 确认在临时目录中
        extracted_files = os.listdir(resource_dir)
        self.assertEqual(len(extracted_files), 1)
        self.assertIn("resource_config_0.bin", extracted_files)
        with open(os.path.join(resource_dir, "resource_config_0.bin"), "r") as f:
            self.assertEqual(f.read(), "Valid resource")
        # 确保重复调用返回相同目录且不重复提取
        self.assertEqual(loader.extract_resources(), resource_dir)
        self.assertEqual(len(os.listdir(resource_dir)), 1)
        loader.cleanup()
        self.assertFalse(os.path.exists(resource_dir))

    def test_06_load_code_success_and_fail(self):
        """测试加载代码成功和失败"""
        loader_valid = MPKLoader.from_file(self.mpk_valid_path)
        self.assertTrue(loader_valid.load_code())
        self.assertTrue(hasattr(loader_valid.module, "main"))

        # 创建一个包含语法错误的代码的 MPK
        syntax_error_code_path = os.path.join(self.temp_dir, "syntax_error.py")
        with open(syntax_error_code_path, "w") as f:
            f.write("def main(): print('Hello\nThis is a syntax error")
        syntax_error_mpk_path = os.path.join(self.temp_dir, "syntax_error.mpk")
        packer_syntax_error = MPKPacker()
        packer_syntax_error.set_metadata({"app_name": "SyntaxErrorApp"})
        packer_syntax_error.add_code(syntax_error_code_path)
        packer_syntax_error.pack(syntax_error_mpk_path)

        loader_syntax_error = MPKLoader.from_file(syntax_error_mpk_path)
        captured_output = StringIO()
        sys.stdout = captured_output
        self.assertFalse(loader_syntax_error.load_code())
        sys.stdout = sys.__stdout__
        self.assertIn("加载代码失败", captured_output.getvalue())

    def test_07_setup_environment(self):
        """测试设置运行环境"""
        loader = MPKLoader.from_file(self.mpk_valid_path)
        loader.setup_environment()
        self.assertIn(loader.temp_dir, sys.path) # 检查资源目录是否在 sys.path
        self.assertEqual(os.getenv("MPK_RESOURCE_DIR"), loader.temp_dir)
        self.assertEqual(os.getenv("MPK_APP_NAME"), "LoaderApp")
        self.assertEqual(os.getenv("MPK_VERSION"), "1.0")
        loader.cleanup() # 清理创建的临时目录
        # 清理环境变量和 sys.path (手动，因为测试环境共享)
        if loader.temp_dir in sys.path: sys.path.remove(loader.temp_dir)
        if "MPK_RESOURCE_DIR" in os.environ: del os.environ["MPK_RESOURCE_DIR"]
        if "MPK_APP_NAME" in os.environ: del os.environ["MPK_APP_NAME"]
        if "MPK_VERSION" in os.environ: del os.environ["MPK_VERSION"]

    def test_08_run_success(self):
        """测试成功运行应用"""
        loader = MPKLoader.from_file(self.mpk_valid_path)
        captured_output = StringIO()
        sys.stdout = captured_output
        # 使用 with 语句确保 cleanup 被调用
        with loader:
            self.assertTrue(loader.run())
        sys.stdout = sys.__stdout__
        self.assertIn("MPK_APP_NAME=LoaderApp", captured_output.getvalue())
        # 检查临时目录是否被清理
        self.assertFalse(os.path.exists(loader.temp_dir))

    def test_09_run_fail_verify(self):
        """测试因验证失败而运行失败"""
        loader = MPKLoader.from_file(self.mpk_invalid_path)
        captured_output = StringIO()
        sys.stdout = captured_output
        with loader:
            self.assertFalse(loader.run())
        sys.stdout = sys.__stdout__
        self.assertIn("MPK文件验证失败", captured_output.getvalue())

    def test_10_run_fail_deps(self):
        """测试因依赖失败而运行失败"""
        loader = MPKLoader.from_file(self.mpk_nodeps_path)
        captured_output = StringIO()
        sys.stdout = captured_output
        with loader:
            self.assertFalse(loader.run())
        sys.stdout = sys.__stdout__
        self.assertIn("依赖检查失败", captured_output.getvalue())

    def test_11_run_fail_perms(self):
        """测试因权限失败而运行失败"""
        loader = MPKLoader.from_file(self.mpk_badperm_path)
        captured_output = StringIO()
        sys.stdout = captured_output
        with loader:
            self.assertFalse(loader.run())
        sys.stdout = sys.__stdout__
        self.assertIn("权限检查失败", captured_output.getvalue())

    def test_12_run_fail_no_entry(self):
        """测试因找不到入口点而运行失败"""
        loader = MPKLoader.from_file(self.mpk_noentry_path)
        captured_output = StringIO()
        sys.stdout = captured_output
        with loader:
            self.assertFalse(loader.run())
        sys.stdout = sys.__stdout__
        self.assertIn("找不到入口函数: non_existent_main", captured_output.getvalue())

    def test_13_run_fail_runtime_error(self):
        """测试因代码运行时错误而运行失败"""
        loader = MPKLoader.from_file(self.mpk_runtime_error_path)
        captured_output = StringIO()
        sys.stdout = captured_output
        with loader:
            self.assertFalse(loader.run())
        sys.stdout = sys.__stdout__
        self.assertIn("运行失败: ValueError('Intentional Error')", captured_output.getvalue())

    # Sandbox 功能是独立的，Loader 本身不直接调用 Sandbox 方法，其测试应在 Sandbox 测试中

if __name__ == '__main__':
    unittest.main() 