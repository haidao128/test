import os
import subprocess
import sys
import time
import win32gui
import win32con
from flask import Flask, render_template, request, jsonify, send_from_directory
from pathlib import Path

# 创建 Flask 应用实例
# __name__ 是 Python 的一个特殊变量，Flask 用它来确定应用根目录，以便查找资源文件（如模板和静态文件）
app = Flask(__name__)

# 定义程序存放的目录
PROGRAMS_DIR = 'programs'
# 确保程序目录存在，如果不存在则创建
if not os.path.exists(PROGRAMS_DIR):
    os.makedirs(PROGRAMS_DIR)

# 添加上传文件夹配置
UPLOAD_FOLDER = Path(__file__).parent / 'static' / 'program_icons'
ALLOWED_EXTENSIONS = {'png', 'jpg', 'jpeg', 'gif'}

# 确保上传文件夹存在
UPLOAD_FOLDER.mkdir(parents=True, exist_ok=True)

def allowed_file(filename):
    """检查文件扩展名是否允许"""
    return '.' in filename and filename.rsplit('.', 1)[1].lower() in ALLOWED_EXTENSIONS

# 路由: 网站主页
# 当用户访问网站根路径 ('/') 时，会调用这个函数
# 支持 GET 请求 (浏览器默认访问方式)
@app.route('/')
def index():
    """
    渲染主页面，并加载已有的程序列表。

    Returns:
        str: 渲染后的 HTML 页面内容。
    """
    programs_dir = Path(__file__).parent / 'programs'
    programs = []
    if programs_dir.exists():
        for program_file in programs_dir.glob('*.py'):
            program_name = program_file.stem
            # 检查是否有对应的图标
            icon_path = UPLOAD_FOLDER / f"{program_name}.png"
            if not icon_path.exists():
                icon_path = 'static/placeholder_icon.png'
            else:
                icon_path = f'static/program_icons/{program_name}.png'
            programs.append({
                'name': program_name,
                'icon': icon_path
            })
    # 使用 'index.html' 模板渲染页面，并将程序列表传递给模板
    return render_template('index.html', programs=programs)

# 路由: 添加新程序
# 当用户通过 POST 请求向 '/add_program' 提交数据时调用
@app.route('/add_program', methods=['POST'])
def add_program():
    """添加新程序"""
    try:
        data = request.form if request.form else request.get_json()
        program_name = data.get('name', '').strip()
        program_code = data.get('code', '').strip()
        
        if not program_name or not program_code:
            return jsonify({'status': 'error', 'message': '程序名和代码不能为空！'})
        
        if not program_name.isidentifier():
            return jsonify({'status': 'error', 'message': '程序名只能包含字母、数字和下划线！'})
        
        # 保存程序文件
        programs_dir = Path(__file__).parent / 'programs'
        programs_dir.mkdir(exist_ok=True)
        program_path = programs_dir / f"{program_name}.py"
        
        if program_path.exists():
            return jsonify({'status': 'error', 'message': '程序名已存在！'})

        # 处理代码格式
        try:
            # 1. 将代码字符串转换为行列表
            code_lines = program_code.replace('\r\n', '\n').split('\n')
            
            # 2. 检测并规范化缩进
            # 找到第一个非空行的缩进作为基准
            base_indent = None
            formatted_lines = []
            
            for line in code_lines:
                if not line.strip():  # 跳过空行
                    formatted_lines.append('')
                    continue
                    
                # 计算当前行的缩进
                indent = len(line) - len(line.lstrip())
                if base_indent is None and line.strip():
                    base_indent = indent
                
                # 如果是第一层缩进，使用4个空格
                if indent >= base_indent:
                    # 将制表符转换为空格，并确保缩进是4的倍数
                    spaces = (indent - base_indent) // 4 * 4
                    formatted_line = ' ' * spaces + line.lstrip()
                else:
                    formatted_line = line.lstrip()
                
                formatted_lines.append(formatted_line)
            
            # 3. 合并处理后的代码
            formatted_code = '\n'.join(formatted_lines)
            
            # 4. 验证代码语法
            compile(formatted_code, '<string>', 'exec')
            
            # 5. 保存格式化后的代码
            program_path.write_text(formatted_code, encoding='utf-8')
            
        except SyntaxError as e:
            return jsonify({
                'status': 'error',
                'message': f'Python代码语法错误：{str(e)}'
            })
        
        # 处理图标上传
        icon_path = None
        if 'icon' in request.files:
            icon_file = request.files['icon']
            if icon_file and icon_file.filename and allowed_file(icon_file.filename):
                # 保存图标文件，使用程序名作为文件名
                icon_ext = Path(icon_file.filename).suffix
                icon_path = UPLOAD_FOLDER / f"{program_name}{icon_ext}"
                icon_file.save(str(icon_path))
                icon_path = f'static/program_icons/{program_name}{icon_ext}'
        
        if not icon_path:
            icon_path = 'static/placeholder_icon.png'
                
        return jsonify({
            'status': 'success',
            'message': f'程序 {program_name} 添加成功！',
            'icon_path': icon_path
        })
        
    except Exception as e:
        return jsonify({'status': 'error', 'message': f'添加程序时出错：{str(e)}'})

# 路由: 运行程序
# 当用户通过 POST 请求向 '/run_program' 提交数据时调用
@app.route('/run_program', methods=['POST'])
def run_program():
    """运行指定的程序"""
    try:
        data = request.get_json()
        program_name = data.get('name')
        
        if not program_name:
            return jsonify({'status': 'error', 'message': '未指定要运行的程序！'})

        # 构建程序文件路径
        programs_dir = Path(__file__).parent / 'programs'
        program_path = programs_dir / f"{program_name}.py"

        if not program_path.exists():
            return jsonify({'status': 'error', 'message': f'程序 {program_name} 不存在！'})

        try:
            # 读取原始代码并进行语法检查
            with open(program_path, 'r', encoding='utf-8') as f:
                code = f.read()
            compile(code, program_path, 'exec')
            
            # 获取 Python 解释器路径并替换为 pythonw.exe
            python_exe = str(Path(os.path.dirname(sys.executable)) / 'pythonw.exe')
            
            # 创建 VBS 脚本来隐藏终端窗口运行程序
            vbs_content = f'''
Set WshShell = CreateObject("WScript.Shell")
WshShell.Run """{python_exe}"" ""{program_path}""", 0, False
'''
            vbs_path = programs_dir / f"run_{program_name}.vbs"
            with open(vbs_path, 'w') as f:
                f.write(vbs_content)
            
            # 使用 ctypes 调用 Windows Shell API 运行 VBS 脚本
            import ctypes
            
            # ShellExecute 参数
            SW_SHOWNORMAL = 1
            
            # 使用 ShellExecute 运行 VBS 脚本
            result = ctypes.windll.shell32.ShellExecuteW(
                None,                   # HWND (父窗口句柄)
                "open",                 # 操作
                str(vbs_path),          # 文件路径
                None,                   # 参数
                str(programs_dir),      # 工作目录
                SW_SHOWNORMAL           # 显示命令 (正常显示)
            )
            
            # 如果返回值 > 32，则命令成功执行
            if result > 32:
                return jsonify({'status': 'success'})
            else:
                return jsonify({
                    'status': 'error',
                    'message': f'启动程序失败，错误代码：{result}'
                })
            
        except SyntaxError as e:
            return jsonify({
                'status': 'error',
                'message': f'程序存在语法错误：{str(e)}'
            })
        except Exception as e:
            return jsonify({
                'status': 'error',
                'message': f'运行程序时出错：{str(e)}'
            })

    except Exception as e:
        return jsonify({
            'status': 'error',
            'message': f'处理运行请求时出错：{str(e)}'
        })

# 路由: 删除程序
# 当用户通过 POST 请求向 '/delete_program' 提交数据时调用
@app.route('/delete_program', methods=['POST'])
def delete_program():
    """
    处理删除程序的请求。
    删除程序文件和对应的图标文件。

    Returns:
        json: 操作结果 (包含 status 和 message)。
    """
    try:
        data = request.get_json()
        program_name = data.get('name')

        if not program_name:
            return jsonify({'status': 'error', 'message': '未指定要删除的程序名！'})

        # 删除程序文件
        programs_dir = Path(__file__).parent / 'programs'
        program_path = programs_dir / f"{program_name}.py"
        
        if not program_path.exists():
            return jsonify({'status': 'error', 'message': '程序不存在！'})

        # 删除程序文件
        program_path.unlink()

        # 删除程序图标（如果存在）
        for ext in ALLOWED_EXTENSIONS:
            icon_path = UPLOAD_FOLDER / f"{program_name}.{ext}"
            if icon_path.exists():
                icon_path.unlink()
                break

        return jsonify({
            'status': 'success',
            'message': f'程序 {program_name} 已删除！'
        })

    except Exception as e:
        return jsonify({'status': 'error', 'message': f'删除程序时出错：{str(e)}'})

# 路由: 批量删除程序
# 当用户通过 POST 请求向 '/delete_programs' 提交数据时调用
@app.route('/delete_programs', methods=['POST'])
def delete_programs():
    """
    批量删除程序的接口。
    删除多个程序文件及其对应的图标。

    Returns:
        json: 操作结果，包含状态和消息。
    """
    try:
        data = request.get_json()
        programs = data.get('programs', [])

        if not programs:
            return jsonify({'status': 'error', 'message': '未选择要删除的程序！'})

        programs_dir = Path(__file__).parent / 'programs'
        deleted_count = 0
        failed_programs = []

        for program_name in programs:
            try:
                # 删除程序文件
                program_path = programs_dir / f"{program_name}.py"
                if program_path.exists():
                    program_path.unlink()
                    deleted_count += 1

                    # 删除程序图标（如果存在）
                    for ext in ALLOWED_EXTENSIONS:
                        icon_path = UPLOAD_FOLDER / f"{program_name}.{ext}"
                        if icon_path.exists():
                            icon_path.unlink()
                            break
                else:
                    failed_programs.append(program_name)
            except Exception as e:
                failed_programs.append(program_name)
                print(f"删除程序 {program_name} 时出错: {e}")

        # 构建返回消息
        if failed_programs:
            if deleted_count > 0:
                message = f'成功删除 {deleted_count} 个程序，但以下程序删除失败：{", ".join(failed_programs)}'
                status = 'partial'
            else:
                message = f'所有程序删除失败：{", ".join(failed_programs)}'
                status = 'error'
        else:
            message = f'成功删除 {deleted_count} 个程序！'
            status = 'success'

        return jsonify({
            'status': status,
            'message': message,
            'deleted_count': deleted_count,
            'failed_programs': failed_programs
        })

    except Exception as e:
        return jsonify({'status': 'error', 'message': f'批量删除程序时出错：{str(e)}'})

# 当这个脚本被直接运行时 (而不是被导入时)
if __name__ == '__main__':
    # 启动 Flask 开发服务器
    # host='0.0.0.0' 让服务器可以被局域网内的其他设备访问 (包括手机)
    # debug=True 开启调试模式，代码修改后服务器会自动重启，并显示详细错误信息
    # 在生产环境中应关闭 debug 模式
    app.run(host='10.120.143.39', port=5000, debug=True) 