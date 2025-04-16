import unittest
import os
import tempfile
import shutil
import json
import sys
from io import StringIO
from unittest.mock import patch, MagicMock
from mobile_platform_creator.core.mpk.dev_tools import DevTools
from mobile_platform_creator.core.mpk import MPKFile

class TestDevTools(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        cls.temp_dir = tempfile.mkdtemp()
        cls.project_dir = os.path.join(cls.temp_dir, "dev_project")
        cls.output_dir = os.path.join(cls.temp_dir, "dev_dist")
        cls.analysis_dir = os.path.join(cls.temp_dir, "dev_analysis")
        cls.mpk_output_path = os.path.join(cls.output_dir, "com.dev.test_1.0.mpk")

        # Disable DevTools logging to avoid cluttering test output
        cls.patcher_log = patch('mobile_platform_creator.core.mpk.dev_tools.logging')
        cls.mock_logging = cls.patcher_log.start()

        # Mock Runtime singleton and its methods
        cls.patcher_runtime = patch('mobile_platform_creator.core.mpk.runtime.Runtime.get_instance')
        cls.mock_runtime_instance = MagicMock()
        # Mock methods used by existing tests
        cls.mock_runtime_instance.load_app.return_value = "com.dev.test_1.0"
        cls.mock_runtime_instance.run_app.return_value = True # Simulate test success
        cls.mock_runtime_instance.unload_app.return_value = None
        # Mock methods used by new tests
        cls.mock_runtime_instance.install_app.return_value = "com.install.test_1.0" # Simulate install success
        cls.mock_runtime_instance.uninstall_app.return_value = True # Simulate uninstall success
        cls.mock_runtime_instance.list_installed_apps.return_value = [
             {"package_name": "com.list.test", "version": "1.0", "app_name": "ListApp", "install_date": "2024-01-01T10:00:00"}
        ] # Simulate one installed app

        cls.mock_runtime = cls.patcher_runtime.start()
        cls.mock_runtime.return_value = cls.mock_runtime_instance

        # Create DevTools instance AFTER patching Runtime
        cls.dev_tools = DevTools.get_instance()

    @classmethod
    def tearDownClass(cls):
        shutil.rmtree(cls.temp_dir)
        cls.patcher_log.stop()
        cls.patcher_runtime.stop()

    def setUp(self):
        """Reset mocks before each test"""
        self.mock_runtime_instance.reset_mock()
        # Re-apply default mock behaviors if needed, as reset_mock clears them
        self.mock_runtime_instance.load_app.return_value = "com.dev.test_1.0"
        self.mock_runtime_instance.run_app.return_value = True
        self.mock_runtime_instance.unload_app.return_value = None
        self.mock_runtime_instance.install_app.return_value = "com.install.test_1.0"
        self.mock_runtime_instance.install_app.side_effect = None # Clear side effects like exceptions
        self.mock_runtime_instance.uninstall_app.return_value = True
        self.mock_runtime_instance.uninstall_app.side_effect = None
        self.mock_runtime_instance.list_installed_apps.return_value = [
             {"package_name": "com.list.test", "version": "1.0", "app_name": "ListApp", "install_date": "2024-01-01T10:00:00"}
        ]
        self.mock_runtime_instance.list_installed_apps.side_effect = None

    def _create_basic_config(self) -> Dict:
        return {
            "metadata": {
                "app_name": "DevApp",
                "package_name": "com.dev.test",
                "version": "1.0",
                "author": "DevTester"
            },
            "code": {
                "path": "src/main.py", # 相对路径
                "type": "script",
                "entry_point": "main"
            },
            "resources": [
                {"path": "resources/data.txt", "type": "text"} # 相对路径
            ]
            # 签名部分省略，简化测试
        }

    def test_01_get_instance(self):
        """测试获取DevTools单例"""
        instance1 = DevTools.get_instance()
        instance2 = DevTools.get_instance()
        self.assertIs(instance1, instance2)
        self.assertIs(self.dev_tools, instance1)

    def test_02_create_project(self):
        """测试创建项目结构"""
        config = self._create_basic_config()
        result = self.dev_tools.create_project(self.project_dir, config)
        self.assertTrue(result)

        # 检查目录和文件是否存在
        self.assertTrue(os.path.isdir(self.project_dir))
        self.assertTrue(os.path.exists(os.path.join(self.project_dir, "mpk_config.json")))
        self.assertTrue(os.path.isdir(os.path.join(self.project_dir, "src")))
        self.assertTrue(os.path.exists(os.path.join(self.project_dir, "src", "main.py")))
        self.assertTrue(os.path.isdir(os.path.join(self.project_dir, "resources")))
        self.assertTrue(os.path.isdir(os.path.join(self.project_dir, "tests")))

        # 检查配置文件内容
        with open(os.path.join(self.project_dir, "mpk_config.json"), "r", encoding="utf-8") as f:
            loaded_config = json.load(f)
        self.assertEqual(loaded_config["metadata"]["app_name"], "DevApp")

        # 检查示例代码内容
        with open(os.path.join(self.project_dir, "src", "main.py"), "r", encoding="utf-8") as f:
            code_content = f.read()
        self.assertIn("print(\"Hello, MPK!\")", code_content)

    def test_03_build_project(self):
        """测试构建项目生成MPK文件"""
        # 先确保项目已创建 (依赖 test_02_create_project)
        if not os.path.exists(self.project_dir):
            self.test_02_create_project()

        # 创建构建所需的资源文件
        resource_dir = os.path.join(self.project_dir, "resources")
        os.makedirs(resource_dir, exist_ok=True)
        with open(os.path.join(resource_dir, "data.txt"), "w") as f:
            f.write("Build resource")

        result = self.dev_tools.build_project(self.project_dir, self.output_dir)
        self.assertTrue(result)
        self.assertTrue(os.path.exists(self.mpk_output_path))

        # 尝试加载构建的MPK进行验证
        try:
            loaded_mpk = MPKFile.load(self.mpk_output_path)
            self.assertEqual(loaded_mpk.metadata.app_name, "DevApp")
            self.assertEqual(len(loaded_mpk.resource_sections), 1)
            self.assertEqual(loaded_mpk.resource_sections[0].resource_type, "text")
            self.assertEqual(loaded_mpk.resource_sections[0].resource_content, b"Build resource")
        except ValueError as e:
            self.fail(f"构建的MPK文件加载失败: {e}")

    def test_04_test_project(self):
        """测试运行项目测试 (依赖 mock Runtime)"""
        # 确保MPK文件已构建 (依赖 test_03_build_project)
        if not os.path.exists(self.mpk_output_path):
             self.test_03_build_project()

        result = self.dev_tools.test_project(self.mpk_output_path)
        self.assertTrue(result)
        # 验证 Runtime 的方法是否被调用
        self.mock_runtime_instance.load_app.assert_called_once()
        self.mock_runtime_instance.run_app.assert_called_once_with("com.dev.test_1.0")
        self.mock_runtime_instance.unload_app.assert_called_once_with("com.dev.test_1.0")

    def test_05_analyze_project(self):
        """测试分析项目生成报告和提取资源"""
         # 确保MPK文件已构建 (依赖 test_03_build_project)
        if not os.path.exists(self.mpk_output_path):
             self.test_03_build_project()

        result = self.dev_tools.analyze_project(self.mpk_output_path, self.analysis_dir)
        self.assertTrue(result)

        # 检查报告文件是否存在
        report_files = [f for f in os.listdir(self.analysis_dir) if f.startswith("mpk_report") and f.endswith(".json")]
        self.assertEqual(len(report_files), 1)
        report_path = os.path.join(self.analysis_dir, report_files[0])

        # 检查报告内容
        with open(report_path, "r", encoding="utf-8") as f:
            report_data = json.load(f)
        self.assertEqual(report_data["元数据"]["app_name"], "DevApp")

        # 检查资源提取目录是否存在且包含内容
        extracted_resource_dir = os.path.join(self.analysis_dir, "resources")
        self.assertTrue(os.path.isdir(extracted_resource_dir))
        extracted_files = os.listdir(extracted_resource_dir)
        self.assertEqual(len(extracted_files), 1)
        self.assertIn("resource_text_0.bin", extracted_files)
        with open(os.path.join(extracted_resource_dir, "resource_text_0.bin"), "r") as f:
            self.assertEqual(f.read(), "Build resource")

    # --- Test new methods directly ---
    def test_05a_install_package_success(self):
        """Test install_package method success"""
        mpk_path = "dummy/install/path.mpk"
        result = self.dev_tools.install_package(mpk_path)
        self.assertTrue(result)
        self.mock_runtime_instance.install_app.assert_called_once_with(mpk_path)

    def test_05b_install_package_fail_not_found(self):
        """Test install_package method file not found"""
        mpk_path = "dummy/notfound/path.mpk"
        self.mock_runtime_instance.install_app.side_effect = FileNotFoundError
        result = self.dev_tools.install_package(mpk_path)
        self.assertFalse(result)
        self.mock_runtime_instance.install_app.assert_called_once_with(mpk_path)

    def test_05c_install_package_fail_exists(self):
        """Test install_package method already exists"""
        mpk_path = "dummy/exists/path.mpk"
        self.mock_runtime_instance.install_app.side_effect = FileExistsError("Already installed")
        result = self.dev_tools.install_package(mpk_path)
        self.assertFalse(result)
        self.mock_runtime_instance.install_app.assert_called_once_with(mpk_path)

    def test_05d_uninstall_package_success(self):
        """Test uninstall_package method success"""
        app_id = "com.uninstall.test_1.0"
        result = self.dev_tools.uninstall_package(app_id)
        self.assertTrue(result)
        self.mock_runtime_instance.uninstall_app.assert_called_once_with(app_id)

    def test_05e_uninstall_package_fail(self):
        """Test uninstall_package method failure (e.g., not installed)"""
        app_id = "com.notinstalled.test_1.0"
        self.mock_runtime_instance.uninstall_app.return_value = False # Simulate failure
        result = self.dev_tools.uninstall_package(app_id)
        self.assertFalse(result)
        self.mock_runtime_instance.uninstall_app.assert_called_once_with(app_id)

    def test_05f_list_packages_success(self):
        """Test list_packages method success"""
        # Capture stdout to check the output format
        captured_output = StringIO()
        sys.stdout = captured_output
        result = self.dev_tools.list_packages()
        sys.stdout = sys.__stdout__ # Restore stdout

        self.assertTrue(result)
        self.mock_runtime_instance.list_installed_apps.assert_called_once()
        output = captured_output.getvalue()
        self.assertIn("已安装的应用包:", output)
        self.assertIn("App ID", output)
        self.assertIn("com.list.test_1.0", output)
        self.assertIn("ListApp", output)

    def test_05g_list_packages_empty(self):
        """Test list_packages method when no packages are installed"""
        self.mock_runtime_instance.list_installed_apps.return_value = [] # Simulate empty list
        captured_output = StringIO()
        sys.stdout = captured_output
        result = self.dev_tools.list_packages()
        sys.stdout = sys.__stdout__

        self.assertTrue(result)
        self.mock_runtime_instance.list_installed_apps.assert_called_once()
        self.assertIn("没有已安装的应用包.", captured_output.getvalue())

    # --- Test new commands via run_command ---
    @patch('sys.argv', ['prog_name', 'create', '', '--config', ''])
    def test_06_run_command_create(self, mock_argv):
        """测试命令行 - create"""
        test_proj_dir = os.path.join(self.temp_dir, "cmd_create")
        test_config_path = os.path.join(self.temp_dir, "cmd_config.json")
        config_data = {"metadata": {"app_name": "CmdCreateApp"}}
        with open(test_config_path, "w") as f: json.dump(config_data, f)

        mock_argv[2] = test_proj_dir
        mock_argv[4] = test_config_path

        # 模拟 DevTools 的方法
        with patch.object(self.dev_tools, 'create_project', return_value=True) as mock_create:
            result = self.dev_tools.run_command(sys.argv[1:])
            self.assertTrue(result)
            mock_create.assert_called_once_with(test_proj_dir, config_data)

    @patch('sys.argv', ['prog_name', 'build', '', '--output', ''])
    def test_07_run_command_build(self, mock_argv):
        """测试命令行 - build"""
        test_proj_dir = os.path.join(self.temp_dir, "cmd_build")
        test_output_dir = os.path.join(self.temp_dir, "cmd_dist")
        mock_argv[2] = test_proj_dir
        mock_argv[4] = test_output_dir

        with patch.object(self.dev_tools, 'build_project', return_value=True) as mock_build:
            result = self.dev_tools.run_command(sys.argv[1:])
            self.assertTrue(result)
            mock_build.assert_called_once_with(test_proj_dir, test_output_dir)

        # 测试不带 --output
        mock_build.reset_mock()
        mock_argv[4] = '' # 移除 --output
        default_output_dir = os.path.join(test_proj_dir, "dist")
        result = self.dev_tools.run_command(sys.argv[1:])
        self.assertTrue(result)
        mock_build.assert_called_once_with(test_proj_dir, default_output_dir)

    @patch('sys.argv', ['prog_name', 'test', ''])
    def test_08_run_command_test(self, mock_argv):
        """测试命令行 - test"""
        test_mpk_path = os.path.join(self.temp_dir, "cmd_test.mpk")
        mock_argv[2] = test_mpk_path

        with patch.object(self.dev_tools, 'test_project', return_value=True) as mock_test:
            result = self.dev_tools.run_command(sys.argv[1:])
            self.assertTrue(result)
            mock_test.assert_called_once_with(test_mpk_path)

    @patch('sys.argv', ['prog_name', 'analyze', '', '--output', ''])
    def test_09_run_command_analyze(self, mock_argv):
        """测试命令行 - analyze"""
        test_mpk_path = os.path.join(self.temp_dir, "cmd_analyze.mpk")
        test_analyze_dir = os.path.join(self.temp_dir, "cmd_analysis")
        mock_argv[2] = test_mpk_path
        mock_argv[4] = test_analyze_dir

        with patch.object(self.dev_tools, 'analyze_project', return_value=True) as mock_analyze:
            result = self.dev_tools.run_command(sys.argv[1:])
            self.assertTrue(result)
            mock_analyze.assert_called_once_with(test_mpk_path, test_analyze_dir)

        # 测试不带 --output
        mock_analyze.reset_mock()
        mock_argv[4] = '' # 移除 --output
        default_analyze_dir = os.path.join(os.path.dirname(test_mpk_path), "analysis")
        result = self.dev_tools.run_command(sys.argv[1:])
        self.assertTrue(result)
        mock_analyze.assert_called_once_with(test_mpk_path, default_analyze_dir)

    @patch('sys.argv', ['prog_name', 'install', ''])
    def test_11_run_command_install(self, mock_argv):
        """测试命令行 - install"""
        test_mpk_path = os.path.join(self.temp_dir, "cmd_install.mpk")
        mock_argv[2] = test_mpk_path

        # Use patch.object on the instance created in setUpClass
        with patch.object(self.dev_tools, 'install_package', return_value=True) as mock_install_pkg:
            result = self.dev_tools.run_command(sys.argv[1:])
            self.assertTrue(result)
            mock_install_pkg.assert_called_once_with(test_mpk_path)

    @patch('sys.argv', ['prog_name', 'uninstall', ''])
    def test_12_run_command_uninstall(self, mock_argv):
        """测试命令行 - uninstall"""
        test_app_id = "com.cmd.uninstall_1.0"
        mock_argv[2] = test_app_id

        with patch.object(self.dev_tools, 'uninstall_package', return_value=True) as mock_uninstall_pkg:
            result = self.dev_tools.run_command(sys.argv[1:])
            self.assertTrue(result)
            mock_uninstall_pkg.assert_called_once_with(test_app_id)

    @patch('sys.argv', ['prog_name', 'list'])
    def test_13_run_command_list(self, mock_argv):
        """测试命令行 - list"""
        with patch.object(self.dev_tools, 'list_packages', return_value=True) as mock_list_pkgs:
            result = self.dev_tools.run_command(sys.argv[1:])
            self.assertTrue(result)
            mock_list_pkgs.assert_called_once()

    @patch('sys.argv', ['prog_name', 'unknown_command'])
    @patch('argparse.ArgumentParser.print_help')
    def test_14_run_command_unknown(self, mock_print_help, mock_argv):
        """测试未知命令行命令"""
        # Need to allow SystemExit here because argparse raises it on unknown command
        with self.assertRaises(SystemExit):
            self.dev_tools.run_command(sys.argv[1:])
        # Depending on argparse behavior, print_help might not be called before SystemExit
        # mock_print_help.assert_called_once()


if __name__ == '__main__':
    unittest.main() 