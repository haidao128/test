"""
JavaScript运行时模块
===================

提供JavaScript程序的执行环境，包括代码加载、运行和API支持。

主要功能：
1. JavaScript代码加载与执行
2. 沙箱隔离与安全控制
3. API桥接与扩展
4. 资源管理与生命周期控制
"""

import logging
import os
import sys
import time
import json
import traceback
from typing import Dict, List, Any, Optional, Callable, Union, Set, Tuple
import threading
import tempfile

# 可选依赖，尝试导入不同的JavaScript引擎
try:
    import quickjs
    QUICKJS_AVAILABLE = True
except ImportError:
    QUICKJS_AVAILABLE = False

try:
    import PyV8
    V8_AVAILABLE = True
except ImportError:
    V8_AVAILABLE = False

try:
    from py_mini_racer import MiniRacer
    MINI_RACER_AVAILABLE = True
except ImportError:
    MINI_RACER_AVAILABLE = False

# 导入MPK包处理模块
from ..utils.mpk_package import MPKPackage

logger = logging.getLogger("mobile_platform_creator.core.js_runtime")

class JSException(Exception):
    """JavaScript执行异常"""
    pass

class JSRuntime:
    """JavaScript运行时环境"""
    
    def __init__(self, app_id: str, work_dir: Optional[str] = None, engine_type: str = "auto"):
        """
        初始化JavaScript运行时
        
        Args:
            app_id: 应用ID
            work_dir: 工作目录，默认为临时目录
            engine_type: JavaScript引擎类型，可选值：auto, quickjs, v8, miniracer
        """
        self.app_id = app_id
        self.engine_type = engine_type
        
        # 初始化工作目录
        if work_dir:
            self.work_dir = work_dir
        else:
            self.work_dir = tempfile.mkdtemp(prefix=f"js_runtime_{app_id}_")
        
        os.makedirs(self.work_dir, exist_ok=True)
        
        # JavaScript引擎实例
        self.js_engine = None
        
        # 运行状态
        self.is_running = False
        
        # 注册的API模块
        self.api_modules = {}
        
        # 事件监听器
        self.event_listeners = {}
        
        # 资源使用统计
        self.memory_usage = 0
        self.cpu_usage = 0
        
        # 初始化引擎
        self._init_engine()
        
        logger.info(f"JavaScript运行时初始化完成: {app_id}")
    
    def _init_engine(self):
        """初始化JavaScript引擎"""
        engine_type = self.engine_type
        
        # 自动选择可用的引擎
        if engine_type == "auto":
            if QUICKJS_AVAILABLE:
                engine_type = "quickjs"
            elif MINI_RACER_AVAILABLE:
                engine_type = "miniracer"
            elif V8_AVAILABLE:
                engine_type = "v8"
            else:
                raise JSException("没有可用的JavaScript引擎")
        
        # 根据引擎类型创建实例
        if engine_type == "quickjs" and QUICKJS_AVAILABLE:
            logger.info(f"使用QuickJS引擎: {self.app_id}")
            self.js_engine = self._create_quickjs_engine()
        elif engine_type == "v8" and V8_AVAILABLE:
            logger.info(f"使用V8引擎: {self.app_id}")
            self.js_engine = self._create_v8_engine()
        elif engine_type == "miniracer" and MINI_RACER_AVAILABLE:
            logger.info(f"使用MiniRacer引擎: {self.app_id}")
            self.js_engine = self._create_miniracer_engine()
        else:
            raise JSException(f"不支持的JavaScript引擎类型: {engine_type}")
    
    def _create_quickjs_engine(self):
        """创建QuickJS引擎"""
        # 创建引擎实例
        engine = quickjs.Context()
        
        # 注册基础API
        self._register_core_apis(engine)
        
        return engine
    
    def _create_v8_engine(self):
        """创建V8引擎"""
        # 创建引擎实例
        engine = PyV8.JSContext()
        engine.enter()
        
        # 注册基础API
        self._register_core_apis(engine)
        
        return engine
    
    def _create_miniracer_engine(self):
        """创建MiniRacer引擎"""
        # 创建引擎实例
        engine = MiniRacer()
        
        # 注册基础API
        self._register_core_apis(engine)
        
        return engine
    
    def _register_core_apis(self, engine):
        """注册核心API到引擎"""
        # 根据不同引擎类型注册API
        if self.engine_type == "quickjs":
            # QuickJS引擎API注册
            engine.globals.console = self._create_console_api()
            engine.globals.setTimeout = self._create_timeout_api()
            engine.globals.clearTimeout = self._create_clear_timeout_api()
            engine.globals.platform = self._create_platform_api()
            engine.globals.fs = self._create_fs_api()
        elif self.engine_type == "v8":
            # V8引擎API注册
            engine.glob.console = self._create_console_api()
            engine.glob.setTimeout = self._create_timeout_api()
            engine.glob.clearTimeout = self._create_clear_timeout_api()
            engine.glob.platform = self._create_platform_api()
            engine.glob.fs = self._create_fs_api()
        elif self.engine_type == "miniracer":
            # MiniRacer引擎API注册
            console_api = self._create_console_api_string()
            timeout_api = self._create_timeout_api_string()
            platform_api = self._create_platform_api_string()
            fs_api = self._create_fs_api_string()
            
            engine.eval(console_api + timeout_api + platform_api + fs_api)
    
    def _create_console_api(self):
        """创建控制台API对象"""
        # 实现因引擎而异
        class Console:
            def log(self, *args):
                message = " ".join(str(arg) for arg in args)
                logger.info(f"[JS:{self.app_id}] {message}")
            
            def error(self, *args):
                message = " ".join(str(arg) for arg in args)
                logger.error(f"[JS:{self.app_id}] {message}")
            
            def warn(self, *args):
                message = " ".join(str(arg) for arg in args)
                logger.warning(f"[JS:{self.app_id}] {message}")
            
            def info(self, *args):
                message = " ".join(str(arg) for arg in args)
                logger.info(f"[JS:{self.app_id}] {message}")
        
        return Console()
    
    def _create_console_api_string(self):
        """创建控制台API字符串（用于MiniRacer）"""
        return """
        console = {
            log: function() {
                var args = Array.prototype.slice.call(arguments);
                _console_log(args.join(' '));
            },
            error: function() {
                var args = Array.prototype.slice.call(arguments);
                _console_error(args.join(' '));
            },
            warn: function() {
                var args = Array.prototype.slice.call(arguments);
                _console_warn(args.join(' '));
            },
            info: function() {
                var args = Array.prototype.slice.call(arguments);
                _console_info(args.join(' '));
            }
        };
        """
    
    def load_from_mpk(self, mpk_file: Union[str, MPKPackage], main_script: Optional[str] = None) -> bool:
        """
        从MPK包加载JavaScript应用
        
        Args:
            mpk_file: MPK文件路径或MPK包对象
            main_script: 主脚本路径，默认为None（使用manifest中定义的入口点）
            
        Returns:
            bool: 是否成功加载
        """
        try:
            # 加载MPK包
            if isinstance(mpk_file, str):
                mpk = MPKPackage(mpk_file)
            else:
                mpk = mpk_file
            
            # 获取清单
            manifest = mpk.get_manifest()
            
            # 检查代码类型
            code_type = manifest.get("code_type", "")
            if code_type != "javascript":
                logger.error(f"不支持的代码类型: {code_type}")
                return False
            
            # 获取主脚本路径
            if not main_script:
                main_script = manifest.get("entry_point", "")
                if not main_script:
                    logger.error("未指定主脚本路径")
                    return False
            
            # 提取代码到工作目录
            code_dir = os.path.join(self.work_dir, "code")
            os.makedirs(code_dir, exist_ok=True)
            
            mpk.extract_directory("code", code_dir)
            
            # 提取资源到工作目录
            assets_dir = os.path.join(self.work_dir, "assets")
            os.makedirs(assets_dir, exist_ok=True)
            
            if "assets" in mpk.list_files():
                mpk.extract_directory("assets", assets_dir)
            
            # 设置全局变量
            self._setup_globals(manifest)
            
            # 加载主脚本
            main_script_path = os.path.join(code_dir, main_script)
            if not os.path.exists(main_script_path):
                logger.error(f"主脚本不存在: {main_script_path}")
                return False
            
            # 执行主脚本
            self.execute_file(main_script_path)
            
            self.is_running = True
            logger.info(f"JavaScript应用加载成功: {self.app_id}")
            return True
        except Exception as e:
            logger.error(f"加载JavaScript应用失败: {e}")
            logger.error(f"错误详情: {traceback.format_exc()}")
            return False
    
    def execute_file(self, file_path: str) -> Any:
        """
        执行JavaScript文件
        
        Args:
            file_path: 文件路径
            
        Returns:
            Any: 执行结果
        """
        try:
            with open(file_path, 'r', encoding='utf-8') as f:
                script = f.read()
            
            return self.execute_script(script, os.path.basename(file_path))
        except Exception as e:
            logger.error(f"执行JavaScript文件失败: {file_path} - {e}")
            raise JSException(f"执行JavaScript文件失败: {file_path} - {e}")
    
    def execute_script(self, script: str, file_name: str = "<script>") -> Any:
        """
        执行JavaScript脚本
        
        Args:
            script: JavaScript脚本
            file_name: 文件名（用于错误报告）
            
        Returns:
            Any: 执行结果
        """
        if not self.js_engine:
            raise JSException("JavaScript引擎未初始化")
        
        try:
            # 执行脚本，根据引擎类型调用不同的方法
            if self.engine_type == "quickjs":
                return self.js_engine.eval(script, file_name)
            elif self.engine_type == "v8":
                return self.js_engine.eval(script)
            elif self.engine_type == "miniracer":
                return self.js_engine.eval(script)
        except Exception as e:
            logger.error(f"执行JavaScript脚本失败: {file_name} - {e}")
            raise JSException(f"执行JavaScript脚本失败: {file_name} - {e}")
    
    def call_function(self, function_name: str, *args) -> Any:
        """
        调用JavaScript函数
        
        Args:
            function_name: 函数名
            *args: 参数
            
        Returns:
            Any: 函数返回值
        """
        if not self.js_engine:
            raise JSException("JavaScript引擎未初始化")
        
        try:
            # 调用函数，根据引擎类型使用不同的方法
            if self.engine_type == "quickjs":
                func = self.js_engine.eval(function_name)
                if callable(func):
                    return func(*args)
                else:
                    raise JSException(f"不是可调用的函数: {function_name}")
            elif self.engine_type == "v8":
                return self.js_engine.eval(f"{function_name}({','.join(repr(arg) for arg in args)})")
            elif self.engine_type == "miniracer":
                args_json = json.dumps(args)
                return self.js_engine.eval(f"{function_name}.apply(null, {args_json})")
        except Exception as e:
            logger.error(f"调用JavaScript函数失败: {function_name} - {e}")
            raise JSException(f"调用JavaScript函数失败: {function_name} - {e}")
    
    def _setup_globals(self, manifest: Dict[str, Any]):
        """设置全局变量"""
        # 设置APP_ID
        if self.engine_type == "quickjs":
            self.js_engine.globals.APP_ID = self.app_id
            self.js_engine.globals.APP_PATH = self.work_dir
        elif self.engine_type == "v8":
            self.js_engine.glob.APP_ID = self.app_id
            self.js_engine.glob.APP_PATH = self.work_dir
        elif self.engine_type == "miniracer":
            self.js_engine.eval(f"APP_ID = '{self.app_id}';")
            self.js_engine.eval(f"APP_PATH = '{self.work_dir.replace('\\', '\\\\')}';")
    
    def dispose(self):
        """释放资源"""
        if self.is_running:
            self.is_running = False
            
            # 释放JavaScript引擎
            if self.js_engine:
                if self.engine_type == "v8":
                    self.js_engine.leave()
                    
                self.js_engine = None
            
            # 清理工作目录
            # 注意：这里不删除工作目录，因为可能包含用户数据
            
            logger.info(f"JavaScript运行时已释放: {self.app_id}")
    
    def __del__(self):
        """析构函数"""
        self.dispose()

# JS运行时实例缓存
_js_runtime_instances = {}

def get_runtime(app_id: str, work_dir: Optional[str] = None, engine_type: str = "auto") -> JSRuntime:
    """
    获取JavaScript运行时实例
    
    Args:
        app_id: 应用ID
        work_dir: 工作目录
        engine_type: JavaScript引擎类型
        
    Returns:
        JSRuntime: JavaScript运行时实例
    """
    if app_id in _js_runtime_instances:
        return _js_runtime_instances[app_id]
    
    runtime = JSRuntime(app_id, work_dir, engine_type)
    _js_runtime_instances[app_id] = runtime
    return runtime

def dispose_runtime(app_id: str) -> bool:
    """
    释放JavaScript运行时实例
    
    Args:
        app_id: 应用ID
        
    Returns:
        bool: 是否成功释放
    """
    if app_id in _js_runtime_instances:
        runtime = _js_runtime_instances.pop(app_id)
        runtime.dispose()
        return True
    return False

def load_app_from_mpk(mpk_file: str, app_id: Optional[str] = None, 
                     work_dir: Optional[str] = None, engine_type: str = "auto") -> Optional[str]:
    """
    从MPK包加载并初始化JavaScript应用
    
    Args:
        mpk_file: MPK文件路径
        app_id: 应用ID，默认为None（从MPK包获取）
        work_dir: 工作目录
        engine_type: JavaScript引擎类型
        
    Returns:
        Optional[str]: 成功时返回应用ID，失败时返回None
    """
    try:
        # 加载MPK包
        mpk = MPKPackage(mpk_file)
        
        # 获取应用ID
        if not app_id:
            manifest = mpk.get_manifest()
            app_id = manifest.get("id", "")
            if not app_id:
                logger.error("MPK包未指定应用ID")
                return None
        
        # 获取或创建运行时
        runtime = get_runtime(app_id, work_dir, engine_type)
        
        # 加载应用
        if runtime.load_from_mpk(mpk):
            return app_id
        else:
            dispose_runtime(app_id)
            return None
    except Exception as e:
        logger.error(f"加载JavaScript应用失败: {e}")
        logger.error(f"错误详情: {traceback.format_exc()}")
        return None 