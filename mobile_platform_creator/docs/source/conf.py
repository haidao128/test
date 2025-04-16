# Configuration file for the Sphinx documentation builder.
#
# For the full list of built-in configuration values, see the documentation:
# https://www.sphinx-doc.org/en/master/usage/configuration.html

import os
import sys

# -- Path setup --------------------------------------------------------------
# 将项目根目录添加到 sys.path，以便 autodoc 可以找到模块
sys.path.insert(0, os.path.abspath('../../')) # 假设 conf.py 在 docs/source/ 下

# -- Project information -----------------------------------------------------
# https://www.sphinx-doc.org/en/master/usage/configuration.html#project-information

project = 'MPK框架'
copyright = '2024, MPC团队'
author = 'MPC团队'
release = '0.1.0' # 可以考虑从版本文件读取

# -- General configuration ---------------------------------------------------
# https://www.sphinx-doc.org/en/master/usage/configuration.html#general-configuration

extensions = [
    'sphinx.ext.autodoc',  # 从 docstrings 导入文档
    'sphinx.ext.napoleon', # 支持 Google 和 NumPy 风格的 docstrings
    'sphinx.ext.intersphinx', # 链接到其他项目的文档
    'sphinx.ext.viewcode', # 添加源码链接
    'sphinx.ext.githubpages', # 支持 GitHub Pages
]

templates_path = ['_templates']
exclude_patterns = ['_build', 'Thumbs.db', '.DS_Store']

language = 'zh_CN' # 设置文档语言为中文

# -- Options for HTML output -------------------------------------------------
# https://www.sphinx-doc.org/en/master/usage/configuration.html#options-for-html-output

html_theme = 'sphinx_rtd_theme' # 使用 Read the Docs 主题
html_static_path = ['_static']

# -- Options for autodoc -----------------------------------------------------
# https://www.sphinx-doc.org/en/master/usage/extensions/autodoc.html#configuration

autodoc_member_order = 'bysource' # 成员按源代码顺序排列
autodoc_default_options = {
    'members': True,
    'undoc-members': True, # 显示没有 docstring 的成员
    'private-members': False,
    'special-members': '__init__',
    'show-inheritance': True,
}

# -- Options for intersphinx extension --------------------------------------- 
# https://www.sphinx-doc.org/en/master/usage/extensions/intersphinx.html#configuration

intersphinx_mapping = {'python': ('https://docs.python.org/3', None)} 