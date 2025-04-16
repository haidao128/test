"""
JavaScript 引擎实现模块
=====================

提供多种 JavaScript 引擎的统一接口实现。

支持的引擎：
1. QuickJS - 轻量级嵌入式 JavaScript 引擎
2. V8 - Google 高性能 JavaScript 引擎（通过 PyV8 绑定）
3. MiniRacer - V8 的最小化绑定（通过 py-mini-racer 绑定）
4. 内置解释器 - 纯 Python 实现的简单 JavaScript 解释器，用于兼容性

"""

import os
import sys
import json
import time
import threading
import logging
from typing import Any, Dict, List, Callable, Optional, Union, Set

logger = logging.getLogger(__name__)

# 尝试导入可选的 JavaScript 引擎
try:
    import quickjs
    QUICKJS_AVAILABLE = True
except ImportError:
    QUICKJS_AVAILABLE = False
    logger.warning("QuickJS 引擎不可用，尝试运行 'pip install python-quickjs' 安装")

try:
    import PyV8
    V8_AVAILABLE = True
except ImportError:
    V8_AVAILABLE = False
    logger.warning("V8 引擎不可用，尝试运行 'pip install pyv8' 安装")

try:
    from py_mini_racer import MiniRacer
    MINI_RACER_AVAILABLE = True
except ImportError:
    MINI_RACER_AVAILABLE = False
    logger.warning("MiniRacer 引擎不可用，尝试运行 'pip install py-mini-racer' 安装")

class JSValue:
    """JavaScript 值的包装器"""
    
    def __init__(self, value, engine_type=None):
        self.value = value
        self.engine_type = engine_type
    
    def __repr__(self):
        return f"JSValue({repr(self.value)})"
    
    def to_py(self):
        """转换为 Python 值"""
        if self.engine_type == "quickjs" and QUICKJS_AVAILABLE:
            if isinstance(self.value, quickjs.Object):
                return {k: JSValue(self.value[k], self.engine_type).to_py() for k in self.value}
            elif isinstance(self.value, quickjs.Array):
                return [JSValue(x, self.engine_type).to_py() for x in self.value]
            elif isinstance(self.value, quickjs.Function):
                return lambda *args: JSValue(self.value(*args), self.engine_type).to_py()
            else:
                return self.value
        elif self.engine_type == "v8" and V8_AVAILABLE:
            if isinstance(self.value, PyV8.JSObject):
                result = {}
                for k in self.value.__dir__():
                    if not k.startswith('_'):
                        try:
                            result[k] = JSValue(self.value.__getattr__(k), self.engine_type).to_py()
                        except:
                            pass
                return result
            elif isinstance(self.value, PyV8.JSArray):
                return [JSValue(self.value[i], self.engine_type).to_py() for i in range(len(self.value))]
            elif isinstance(self.value, PyV8.JSFunction):
                return lambda *args: JSValue(self.value(*args), self.engine_type).to_py()
            else:
                return self.value
        else:
            # 对于 MiniRacer 或其它引擎，通常已经转换为 Python 值
            return self.value


class JSFunction:
    """JavaScript 函数的包装器"""
    
    def __init__(self, engine, name):
        self.engine = engine
        self.name = name
    
    def __call__(self, *args):
        return self.engine.call_function(self.name, *args)


class JSEngineBase:
    """JavaScript 引擎基类"""
    
    def __init__(self):
        self.engine_name = "base"
        self.globals = {}
        self._timers = {}
        self._timer_counter = 0
        self._timer_lock = threading.Lock()
    
    def init(self):
        """初始化引擎"""
        raise NotImplementedError("子类必须实现此方法")
    
    def eval(self, code, filename="<eval>"):
        """
        执行 JavaScript 代码
        
        Args:
            code: JavaScript 代码
            filename: 文件名（用于错误报告）
            
        Returns:
            执行结果
        """
        raise NotImplementedError("子类必须实现此方法")
    
    def call_function(self, name, *args):
        """
        调用 JavaScript 函数
        
        Args:
            name: 函数名
            *args: 参数
            
        Returns:
            函数返回值
        """
        raise NotImplementedError("子类必须实现此方法")
    
    def set_global(self, name, value):
        """
        设置全局变量
        
        Args:
            name: 变量名
            value: 变量值
        """
        raise NotImplementedError("子类必须实现此方法")
    
    def get_global(self, name):
        """
        获取全局变量
        
        Args:
            name: 变量名
            
        Returns:
            变量值
        """
        raise NotImplementedError("子类必须实现此方法")
    
    def register_function(self, name, func):
        """
        注册 JavaScript 函数
        
        Args:
            name: 函数名
            func: 函数对象
        """
        raise NotImplementedError("子类必须实现此方法")
    
    def register_console(self):
        """注册控制台对象"""
        def log(*args):
            logger.info(' '.join(str(arg) for arg in args))
        
        def error(*args):
            logger.error(' '.join(str(arg) for arg in args))
        
        def warn(*args):
            logger.warning(' '.join(str(arg) for arg in args))
        
        def debug(*args):
            logger.debug(' '.join(str(arg) for arg in args))
        
        console = {
            'log': log,
            'error': error,
            'warn': warn,
            'debug': debug,
            'info': log
        }
        
        self.register_global('console', console)
    
    def register_timer_functions(self):
        """注册定时器相关函数"""
        def set_timeout(callback, timeout):
            with self._timer_lock:
                timer_id = self._timer_counter
                self._timer_counter += 1
            
            def timer_func():
                try:
                    time.sleep(timeout / 1000.0)
                    callback()
                    with self._timer_lock:
                        if timer_id in self._timers:
                            del self._timers[timer_id]
                except Exception as e:
                    logger.error(f"Timer error: {e}")
            
            timer = threading.Timer(timeout / 1000.0, timer_func)
            timer.daemon = True
            
            with self._timer_lock:
                self._timers[timer_id] = timer
            
            timer.start()
            return timer_id
        
        def clear_timeout(timer_id):
            with self._timer_lock:
                if timer_id in self._timers:
                    self._timers[timer_id].cancel()
                    del self._timers[timer_id]
        
        self.register_function('setTimeout', set_timeout)
        self.register_function('clearTimeout', clear_timeout)
        self.register_function('setInterval', lambda callback, interval: None)  # 暂不实现
        self.register_function('clearInterval', lambda timer_id: None)  # 暂不实现
    
    def register_global(self, name, value):
        """
        注册全局对象
        
        Args:
            name: 对象名称
            value: 对象值
        """
        self.globals[name] = value
        self.set_global(name, value)
    
    def cleanup(self):
        """清理资源"""
        with self._timer_lock:
            for timer in self._timers.values():
                timer.cancel()
            self._timers.clear()


class QuickJSEngine(JSEngineBase):
    """QuickJS 引擎实现"""
    
    def __init__(self):
        super().__init__()
        self.engine_name = "quickjs"
        self.context = None
    
    def init(self):
        """初始化引擎"""
        if not QUICKJS_AVAILABLE:
            raise ImportError("QuickJS 引擎不可用")
        
        self.context = quickjs.Context()
        self.register_console()
        self.register_timer_functions()
        return True
    
    def eval(self, code, filename="<eval>"):
        """执行 JavaScript 代码"""
        if not self.context:
            self.init()
        
        try:
            result = self.context.eval(code, filename)
            return JSValue(result, self.engine_name).to_py()
        except Exception as e:
            logger.error(f"执行 JavaScript 代码出错: {e}")
            raise
    
    def call_function(self, name, *args):
        """调用 JavaScript 函数"""
        if not self.context:
            self.init()
        
        try:
            # 支持点号表示法的函数调用，如 console.log
            parts = name.split('.')
            obj = self.context.globals
            for part in parts[:-1]:
                obj = obj[part]
            
            func = obj[parts[-1]]
            # 转换 Python 参数为 JavaScript 参数
            js_args = [self._py_to_js(arg) for arg in args]
            result = func(*js_args)
            return JSValue(result, self.engine_name).to_py()
        except Exception as e:
            logger.error(f"调用函数 {name} 出错: {e}")
            raise
    
    def _py_to_js(self, value):
        """将 Python 值转换为 JavaScript 值"""
        if isinstance(value, (str, int, float, bool, type(None))):
            return value
        elif isinstance(value, (list, tuple)):
            arr = self.context.eval("[]")
            for i, item in enumerate(value):
                arr[i] = self._py_to_js(item)
            return arr
        elif isinstance(value, dict):
            obj = self.context.eval("({})")
            for k, v in value.items():
                obj[k] = self._py_to_js(v)
            return obj
        elif callable(value):
            def callback(*args):
                return value(*[JSValue(arg, self.engine_name).to_py() for arg in args])
            return self.context.function(callback)
        else:
            return str(value)
    
    def set_global(self, name, value):
        """设置全局变量"""
        if not self.context:
            self.init()
        
        try:
            self.context.globals[name] = self._py_to_js(value)
        except Exception as e:
            logger.error(f"设置全局变量 {name} 失败: {e}")
            raise
    
    def get_global(self, name):
        """获取全局变量"""
        if not self.context:
            self.init()
        
        try:
            parts = name.split('.')
            obj = self.context.globals
            for part in parts:
                obj = obj[part]
            return JSValue(obj, self.engine_name).to_py()
        except Exception as e:
            logger.error(f"获取全局变量 {name} 失败: {e}")
            raise
    
    def register_function(self, name, func):
        """注册 JavaScript 函数"""
        if not self.context:
            self.init()
        
        try:
            js_func = self.context.function(lambda *args: func(*[JSValue(arg, self.engine_name).to_py() for arg in args]))
            self.context.globals[name] = js_func
        except Exception as e:
            logger.error(f"注册函数 {name} 失败: {e}")
            raise
    
    def cleanup(self):
        """清理资源"""
        super().cleanup()
        self.context = None


class V8Engine(JSEngineBase):
    """V8 引擎实现"""
    
    def __init__(self):
        super().__init__()
        self.engine_name = "v8"
        self.context = None
    
    def init(self):
        """初始化引擎"""
        if not V8_AVAILABLE:
            raise ImportError("V8 引擎不可用")
        
        self.context = PyV8.JSContext()
        self.context.enter()
        self.register_console()
        self.register_timer_functions()
        return True
    
    def eval(self, code, filename="<eval>"):
        """执行 JavaScript 代码"""
        if not self.context:
            self.init()
        
        try:
            result = self.context.eval(code)
            return JSValue(result, self.engine_name).to_py()
        except Exception as e:
            logger.error(f"执行 JavaScript 代码出错: {e}")
            raise
    
    def call_function(self, name, *args):
        """调用 JavaScript 函数"""
        if not self.context:
            self.init()
        
        try:
            # 支持点号表示法的函数调用，如 console.log
            parts = name.split('.')
            obj = self.context.locals
            for part in parts[:-1]:
                obj = obj[part]
            
            func = obj[parts[-1]]
            # 转换 Python 参数为 JavaScript 参数
            js_args = [self._py_to_js(arg) for arg in args]
            result = func(*js_args)
            return JSValue(result, self.engine_name).to_py()
        except Exception as e:
            logger.error(f"调用函数 {name} 出错: {e}")
            raise
    
    def _py_to_js(self, value):
        """将 Python 值转换为 JavaScript 值"""
        if isinstance(value, (str, int, float, bool, type(None))):
            return value
        elif isinstance(value, (list, tuple)):
            result = []
            for item in value:
                result.append(self._py_to_js(item))
            return result
        elif isinstance(value, dict):
            obj = PyV8.JSObject()
            for k, v in value.items():
                obj[k] = self._py_to_js(v)
            return obj
        elif callable(value):
            def callback(*args):
                return value(*[JSValue(arg, self.engine_name).to_py() for arg in args])
            return PyV8.JSFunction(callback)
        else:
            return str(value)
    
    def set_global(self, name, value):
        """设置全局变量"""
        if not self.context:
            self.init()
        
        try:
            parts = name.split('.')
            if len(parts) > 1:
                obj = self.context.locals
                for part in parts[:-1]:
                    if part not in obj:
                        obj[part] = PyV8.JSObject()
                    obj = obj[part]
                obj[parts[-1]] = self._py_to_js(value)
            else:
                self.context.locals[name] = self._py_to_js(value)
        except Exception as e:
            logger.error(f"设置全局变量 {name} 失败: {e}")
            raise
    
    def get_global(self, name):
        """获取全局变量"""
        if not self.context:
            self.init()
        
        try:
            parts = name.split('.')
            obj = self.context.locals
            for part in parts:
                obj = obj[part]
            return JSValue(obj, self.engine_name).to_py()
        except Exception as e:
            logger.error(f"获取全局变量 {name} 失败: {e}")
            raise
    
    def register_function(self, name, func):
        """注册 JavaScript 函数"""
        if not self.context:
            self.init()
        
        try:
            js_func = PyV8.JSFunction(lambda *args: func(*[JSValue(arg, self.engine_name).to_py() for arg in args]))
            parts = name.split('.')
            if len(parts) > 1:
                obj = self.context.locals
                for part in parts[:-1]:
                    if part not in obj:
                        obj[part] = PyV8.JSObject()
                    obj = obj[part]
                obj[parts[-1]] = js_func
            else:
                self.context.locals[name] = js_func
        except Exception as e:
            logger.error(f"注册函数 {name} 失败: {e}")
            raise
    
    def cleanup(self):
        """清理资源"""
        super().cleanup()
        if self.context:
            self.context.leave()
            self.context = None


class MiniRacerEngine(JSEngineBase):
    """MiniRacer 引擎实现"""
    
    def __init__(self):
        super().__init__()
        self.engine_name = "miniracer"
        self.context = None
        self.global_objects = {}
    
    def init(self):
        """初始化引擎"""
        if not MINI_RACER_AVAILABLE:
            raise ImportError("MiniRacer 引擎不可用")
        
        self.context = MiniRacer()
        
        # 注册控制台函数
        self.context.eval("""
        var console = {
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
        """)
        
        # 注册控制台方法
        self.context.eval("""
        var setTimeout = function(callback, timeout) {
            return _setTimeout(callback.toString(), timeout);
        };
        
        var clearTimeout = function(id) {
            return _clearTimeout(id);
        };
        """)
        
        def console_log(message):
            logger.info(message)
        
        def console_error(message):
            logger.error(message)
        
        def console_warn(message):
            logger.warning(message)
        
        def console_info(message):
            logger.info(message)
        
        def set_timeout_handler(callback_str, timeout):
            callback_id = str(hash(callback_str + str(time.time())))
            
            def timer_func():
                try:
                    time.sleep(timeout / 1000.0)
                    self.context.eval(f"({callback_str})()")
                except Exception as e:
                    logger.error(f"Timer error: {e}")
                finally:
                    with self._timer_lock:
                        if callback_id in self._timers:
                            del self._timers[callback_id]
            
            timer = threading.Timer(timeout / 1000.0, timer_func)
            timer.daemon = True
            
            with self._timer_lock:
                self._timers[callback_id] = timer
            
            timer.start()
            return callback_id
        
        def clear_timeout_handler(timer_id):
            with self._timer_lock:
                if timer_id in self._timers:
                    self._timers[timer_id].cancel()
                    del self._timers[timer_id]
        
        self.context.eval("var global = this;")
        self.context.attach("_console_log", console_log)
        self.context.attach("_console_error", console_error)
        self.context.attach("_console_warn", console_warn)
        self.context.attach("_console_info", console_info)
        self.context.attach("_setTimeout", set_timeout_handler)
        self.context.attach("_clearTimeout", clear_timeout_handler)
        
        return True
    
    def eval(self, code, filename="<eval>"):
        """执行 JavaScript 代码"""
        if not self.context:
            self.init()
        
        try:
            result = self.context.eval(code)
            return result
        except Exception as e:
            logger.error(f"执行 JavaScript 代码出错: {e}")
            raise
    
    def call_function(self, name, *args):
        """调用 JavaScript 函数"""
        if not self.context:
            self.init()
        
        try:
            # 将参数转换为 JS 字符串形式
            args_str = []
            for arg in args:
                if isinstance(arg, str):
                    args_str.append(f"'{arg}'")
                elif isinstance(arg, (int, float, bool)) or arg is None:
                    args_str.append(str(arg).lower() if isinstance(arg, bool) else 'null' if arg is None else str(arg))
                elif isinstance(arg, (list, dict)):
                    args_str.append(json.dumps(arg))
                else:
                    args_str.append(f"'{str(arg)}'")
            
            # 构建函数调用代码
            call_code = f"{name}({', '.join(args_str)})"
            result = self.context.eval(call_code)
            return result
        except Exception as e:
            logger.error(f"调用函数 {name} 出错: {e}")
            raise
    
    def set_global(self, name, value):
        """设置全局变量"""
        if not self.context:
            self.init()
        
        try:
            if isinstance(value, str):
                self.context.eval(f"var {name} = '{value.replace("'", "\\'")}'")
            elif isinstance(value, (int, float, bool)) or value is None:
                if isinstance(value, bool):
                    value_str = str(value).lower()
                elif value is None:
                    value_str = "null"
                else:
                    value_str = str(value)
                self.context.eval(f"var {name} = {value_str}")
            elif isinstance(value, (dict, list)):
                self.context.eval(f"var {name} = {json.dumps(value)}")
            elif callable(value):
                # 存储函数，以便稍后在 JavaScript 回调时调用
                fn_name = f"_py_func_{len(self.global_objects)}"
                self.global_objects[fn_name] = value
                
                def callback(*args):
                    return self.global_objects[fn_name](*args)
                
                self.context.attach(fn_name, callback)
                self.context.eval(f"var {name} = function() {{ return {fn_name}.apply(null, arguments); }}")
            else:
                self.context.eval(f"var {name} = '{str(value)}'")
        except Exception as e:
            logger.error(f"设置全局变量 {name} 失败: {e}")
            raise
    
    def get_global(self, name):
        """获取全局变量"""
        if not self.context:
            self.init()
        
        try:
            return self.context.eval(name)
        except Exception as e:
            logger.error(f"获取全局变量 {name} 失败: {e}")
            raise
    
    def register_function(self, name, func):
        """注册 JavaScript 函数"""
        fn_name = f"_py_func_{len(self.global_objects)}"
        self.global_objects[fn_name] = func
        
        def callback(*args):
            return func(*args)
        
        self.context.attach(fn_name, callback)
        self.context.eval(f"function {name}() {{ return {fn_name}.apply(null, Array.prototype.slice.call(arguments)); }}")
    
    def cleanup(self):
        """清理资源"""
        super().cleanup()
        self.global_objects.clear()
        self.context = None


class DummyEngine(JSEngineBase):
    """简单的 JavaScript 引擎模拟实现"""
    
    def __init__(self):
        super().__init__()
        self.engine_name = "dummy"
        self.globals = {}
        self.functions = {}
    
    def init(self):
        """初始化引擎"""
        self.register_console()
        self.register_timer_functions()
        logger.warning("使用虚拟 JavaScript 引擎，功能极其有限")
        return True
    
    def eval(self, code, filename="<eval>"):
        """执行 JavaScript 代码"""
        logger.warning(f"虚拟引擎不支持执行 JavaScript 代码，代码将被忽略: {code[:50]}...")
        return None
    
    def call_function(self, name, *args):
        """调用 JavaScript 函数"""
        parts = name.split('.')
        if len(parts) > 1:
            obj = self.globals
            for part in parts[:-1]:
                if part not in obj:
                    raise ValueError(f"未找到全局对象 {part}")
                obj = obj[part]
            
            if parts[-1] not in obj:
                raise ValueError(f"未找到方法 {parts[-1]}")
            
            func = obj[parts[-1]]
            if callable(func):
                return func(*args)
            return None
        elif name in self.functions:
            return self.functions[name](*args)
        else:
            raise ValueError(f"未找到函数 {name}")
    
    def set_global(self, name, value):
        """设置全局变量"""
        parts = name.split('.')
        if len(parts) > 1:
            obj = self.globals
            for i, part in enumerate(parts[:-1]):
                if part not in obj:
                    obj[part] = {}
                if i < len(parts) - 2 and not isinstance(obj[part], dict):
                    obj[part] = {}
                obj = obj[part]
            obj[parts[-1]] = value
        else:
            self.globals[name] = value
    
    def get_global(self, name):
        """获取全局变量"""
        parts = name.split('.')
        if len(parts) > 1:
            obj = self.globals
            for part in parts:
                if part not in obj:
                    return None
                obj = obj[part]
            return obj
        return self.globals.get(name)
    
    def register_function(self, name, func):
        """注册 JavaScript 函数"""
        parts = name.split('.')
        if len(parts) > 1:
            obj = self.globals
            for i, part in enumerate(parts[:-1]):
                if part not in obj:
                    obj[part] = {}
                obj = obj[part]
            obj[parts[-1]] = func
        else:
            self.functions[name] = func


def create_engine(engine_type="auto"):
    """
    创建 JavaScript 引擎实例
    
    Args:
        engine_type: 引擎类型，可选值为 "quickjs"、"v8"、"miniracer"、"auto"
        
    Returns:
        JSEngineBase: JavaScript 引擎实例
    """
    if engine_type == "auto":
        # 按优先级尝试不同的引擎
        if QUICKJS_AVAILABLE:
            engine_type = "quickjs"
        elif V8_AVAILABLE:
            engine_type = "v8"
        elif MINI_RACER_AVAILABLE:
            engine_type = "miniracer"
        else:
            engine_type = "dummy"
    
    if engine_type == "quickjs" and QUICKJS_AVAILABLE:
        engine = QuickJSEngine()
    elif engine_type == "v8" and V8_AVAILABLE:
        engine = V8Engine()
    elif engine_type == "miniracer" and MINI_RACER_AVAILABLE:
        engine = MiniRacerEngine()
    else:
        engine = DummyEngine()
    
    engine.init()
    return engine 