/**
 * @fileoverview 前端交互逻辑，处理程序运行和添加。
 */

// 等待整个 HTML 文档加载完成后再执行脚本
document.addEventListener('DOMContentLoaded', () => {

    // 获取页面上的重要元素
    const programsContainer = document.getElementById('programs-container'); // 修改为获取网格容器
    const addProgramSection = document.getElementById('add-program');
    const deleteProgramsSection = document.getElementById('delete-programs');
    const addProgramForm = document.getElementById('add-program-form');
    const deleteProgramsForm = document.getElementById('delete-programs-form');
    const showAddFormButton = document.getElementById('show-add-form-button');
    const showDeleteFormButton = document.getElementById('show-delete-form-button');
    const cancelAddButton = document.getElementById('cancel-add-button');
    const cancelDeleteButton = document.getElementById('cancel-delete-button');
    const selectAllButton = document.getElementById('select-all-button');
    const deselectAllButton = document.getElementById('deselect-all-button');
    // const outputArea = document.getElementById('output-area'); // 不再需要输出区域
    const addMessageDiv = document.getElementById('add-message');
    const deleteMessageDiv = document.getElementById('delete-message');
    const dropdown = document.querySelector('.dropdown');
    const menuButton = document.querySelector('.menu-button');

    /**
     * 统一的程序运行处理函数
     * @param {Event} event - 点击事件对象
     */
    async function handleProgramClick(event) {
        console.log('点击事件触发', event.target);
        
        // 只处理图标的点击
        if (!event.target.classList.contains('program-icon')) {
            console.log('非图标元素，忽略点击');
            return;
        }
        
        const programItem = event.target.closest('.program-item');
        if (!programItem) {
            console.log('找不到父级程序项元素');
            return;
        }

        const programName = programItem.dataset.programName;
        console.log('运行程序:', programName);
        
        if (programItem.getAttribute('data-processing') === 'true') {
            console.log('程序正在处理中，忽略重复点击');
            return; // 如果正在处理中，直接返回
        }
        
        programItem.setAttribute('data-processing', 'true');
        event.target.style.opacity = '0.7'; // 只对图标应用透明度

        try {
            const response = await fetch('/run_program', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ name: programName }),
            });
            const result = await response.json();
            
            // 只在出错时显示提示
            if (result.status === 'error') {
                alert(result.message);
            } else {
                console.log('程序启动成功');
            }
        } catch (error) {
            console.error('运行程序时出错:', error);
            alert(`运行程序时发生错误: ${error}`);
        } finally {
            event.target.style.opacity = '1'; // 恢复图标透明度
            programItem.removeAttribute('data-processing');
        }
    }

    // 如果容器存在，绑定点击事件
    if (programsContainer) {
        programsContainer.addEventListener('click', handleProgramClick);
    }

    /**
     * 处理下拉菜单的显示和隐藏
     */
    menuButton.addEventListener('click', (e) => {
        e.stopPropagation();
        dropdown.classList.toggle('active');
    });

    // 点击其他地方时关闭下拉菜单
    document.addEventListener('click', () => {
        dropdown.classList.remove('active');
    });

    // 防止点击下拉菜单内容时关闭菜单
    document.querySelector('.dropdown-content').addEventListener('click', (e) => {
        e.stopPropagation();
    });

    // 添加键盘快捷键支持
    document.addEventListener('keydown', (e) => {
        // Ctrl + N: 显示添加程序表单
        if (e.ctrlKey && e.key === 'n') {
            e.preventDefault(); // 阻止浏览器默认的新建窗口行为
            showAddForm();
        }
        // Ctrl + D: 显示删除程序表单
        else if (e.ctrlKey && e.key === 'd') {
            e.preventDefault(); // 阻止浏览器默认的收藏页面行为
            showDeleteForm();
        }
        // Esc: 关闭所有表单
        else if (e.key === 'Escape') {
            hideAllForms();
        }
    });

    /**
     * 显示添加程序表单
     */
    function showAddForm() {
        addProgramSection.classList.remove('hidden');
        deleteProgramsSection.classList.add('hidden');
        dropdown.classList.remove('active');
        addProgramForm.reset(); // 重置表单
        addMessageDiv.style.display = 'none'; // 隐藏消息
    }

    /**
     * 显示删除程序表单
     */
    function showDeleteForm() {
        deleteProgramsSection.classList.remove('hidden');
        addProgramSection.classList.add('hidden');
        dropdown.classList.remove('active');
        deleteProgramsForm.reset(); // 重置表单
        deleteMessageDiv.style.display = 'none'; // 隐藏消息
    }

    /**
     * 隐藏所有表单
     */
    function hideAllForms() {
        addProgramSection.classList.add('hidden');
        deleteProgramsSection.classList.add('hidden');
        dropdown.classList.remove('active');
        addProgramForm.reset();
        deleteProgramsForm.reset();
        addMessageDiv.style.display = 'none';
        deleteMessageDiv.style.display = 'none';
    }

    // 绑定按钮点击事件
    showAddFormButton.addEventListener('click', showAddForm);
    showDeleteFormButton.addEventListener('click', showDeleteForm);

    /**
     * 处理取消按钮的点击事件。
     */
    cancelAddButton.addEventListener('click', hideAllForms);

    /**
     * 处理取消删除程序的点击事件。
     */
    cancelDeleteButton.addEventListener('click', hideAllForms);

    /**
     * 处理全选按钮的点击事件。
     */
    selectAllButton.addEventListener('click', () => {
        const checkboxes = deleteProgramsForm.querySelectorAll('input[type="checkbox"]');
        checkboxes.forEach(checkbox => checkbox.checked = true);
    });

    /**
     * 处理取消全选按钮的点击事件。
     */
    deselectAllButton.addEventListener('click', () => {
        const checkboxes = deleteProgramsForm.querySelectorAll('input[type="checkbox"]');
        checkboxes.forEach(checkbox => checkbox.checked = false);
    });

    /**
     * 处理添加程序表单的提交事件。
     */
    addProgramForm.addEventListener('submit', async (event) => {
        event.preventDefault();
        const programNameInput = document.getElementById('program-name');
        const programCodeInput = document.getElementById('program-code');
        const programIconInput = document.getElementById('program-icon');
        
        const programName = programNameInput.value.trim();
        const programCode = programCodeInput.value;

        addMessageDiv.style.display = 'none';
        addMessageDiv.className = 'message';
        addMessageDiv.textContent = '';

        if (!programName || !programCode) {
            showAddMessage('程序名和代码不能为空！', 'error');
            return;
        }
        if (!/^[a-zA-Z0-9_]+$/.test(programName)) {
            showAddMessage('程序名只能包含字母、数字和下划线！', 'error');
            return;
        }

        try {
            // 使用 FormData 来处理文件上传
            const formData = new FormData();
            formData.append('name', programName);
            formData.append('code', programCode);
            
            // 如果选择了图标文件，添加到 FormData
            if (programIconInput.files.length > 0) {
                formData.append('icon', programIconInput.files[0]);
            }

            const response = await fetch('/add_program', {
                method: 'POST',
                body: formData // 不需要设置 Content-Type，浏览器会自动设置
            });

            const result = await response.json();
            showAddMessage(result.message, result.status);

            if (result.status === 'success') {
                addProgramForm.reset();
                addProgramSection.classList.add('hidden');
                showAddFormButton.classList.remove('hidden');
                // 使用返回的图标路径添加新程序
                addNewProgramToGrid(programName, result.icon_path);
            }
        } catch (error) {
            console.error('添加程序时出错:', error);
            showAddMessage(`添加程序时发生网络或脚本错误: ${error}`, 'error');
        }
    });

    /**
     * 处理批量删除表单的提交事件。
     */
    deleteProgramsForm.addEventListener('submit', async (event) => {
        event.preventDefault();
        
        const checkboxes = deleteProgramsForm.querySelectorAll('input[type="checkbox"]:checked');
        const selectedPrograms = Array.from(checkboxes).map(cb => cb.value);
        
        if (selectedPrograms.length === 0) {
            alert('请至少选择一个要删除的程序！');
            return;
        }

        if (!confirm(`确定要删除选中的 ${selectedPrograms.length} 个程序吗？此操作不可恢复！`)) {
            return;
        }

        try {
            const response = await fetch('/delete_programs', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ programs: selectedPrograms }),
            });

            const result = await response.json();
            
            if (result.status === 'success') {
                // 从页面中移除被删除的程序
                selectedPrograms.forEach(programName => {
                    const programItem = document.querySelector(`.program-item[data-program-name="${programName}"]`);
                    if (programItem) {
                        programItem.remove();
                    }
                });

                // 如果没有程序了，隐藏整个程序网格区域
                if (!programsContainer.children.length) {
                    const gridSection = document.getElementById('program-grid');
                    if (gridSection) {
                        gridSection.remove();
                    }
                }

                // 隐藏删除表单，显示成功消息
                deleteProgramsSection.classList.add('hidden');
                showDeleteFormButton.classList.remove('hidden');
                alert(result.message);
            } else {
                alert(result.message);
            }
        } catch (error) {
            console.error('批量删除程序时出错:', error);
            alert(`批量删除程序时发生错误: ${error}`);
        }
    });

    /**
     * 在页面上显示添加程序的结果消息。
     */
    function showAddMessage(message, type) {
        addMessageDiv.textContent = message;
        addMessageDiv.className = `message ${type}`; 
        addMessageDiv.style.display = 'block'; 
    }

    /**
     * 将新添加的程序动态添加到页面上的程序网格中。
     * @param {string} programName - 程序名称
     * @param {string} iconPath - 图标路径，如果未提供则使用默认图标
     */
    function addNewProgramToGrid(programName, iconPath = '/static/placeholder_icon.png') {
        let programGridSection = document.getElementById('program-grid');
        let container = document.getElementById('programs-container');

        // 检查网格区域和容器是否存在，如果不存在则创建
        if (!programGridSection) {
            const mainElement = document.querySelector('main');
            if (!mainElement) {
                console.error("找不到 main 元素！");
                return;
            }
            programGridSection = document.createElement('section');
            programGridSection.id = 'program-grid';
            
            container = document.createElement('div');
            container.id = 'programs-container';
            programGridSection.appendChild(container);
            
            const addSection = document.getElementById('add-program');
            if (addSection) {
                mainElement.insertBefore(programGridSection, addSection);
            } else {
                mainElement.appendChild(programGridSection);
            }
            
            // 绑定点击事件处理
            container.addEventListener('click', handleProgramClick);
        }
        
        if (!container) {
            console.error("找不到 #programs-container 元素！");
            return;
        }

        // 创建新的程序项
        const newItem = document.createElement('div');
        newItem.className = 'program-item';
        newItem.dataset.programName = programName;

        const iconImg = document.createElement('img');
        // 确保图标路径正确
        if (iconPath.startsWith('/static/')) {
            iconImg.src = iconPath;
        } else if (iconPath.startsWith('static/')) {
            iconImg.src = '/' + iconPath;
        } else {
            iconImg.src = '/static/' + iconPath;
        }
        iconImg.alt = `${programName} 图标`;
        iconImg.className = 'program-icon';
        // 确保图标的样式正确，可点击
        iconImg.style.cursor = 'pointer';

        const nameSpan = document.createElement('span');
        nameSpan.className = 'program-name';
        nameSpan.textContent = programName;
        // 确保文本不会干扰点击
        nameSpan.style.pointerEvents = 'none';

        newItem.appendChild(iconImg);
        newItem.appendChild(nameSpan);
        container.appendChild(newItem);
    }

    /**
     * 调整程序容器布局
     * 程序少于10个时居中显示，多于9个时从左下角开始显示
     */
    function adjustProgramsLayout() {
        const container = document.getElementById('programs-container');
        if (!container) return;
        
        const programItems = container.querySelectorAll('.program-item');
        const programCount = programItems.length;
        
        console.log(`调整布局，程序数量: ${programCount}`);
        
        if (programCount <= 9) {
            // 程序少于10个时，居中显示
            container.style.justifyContent = 'center';
            container.style.alignContent = 'center';
        } else {
            // 程序多于9个时，从左下角开始显示
            container.style.justifyContent = 'flex-start';
            container.style.alignContent = 'flex-end';
        }
    }
    
    // 页面加载时调整布局
    adjustProgramsLayout();
    
    // 窗口调整大小时重新调整布局
    window.addEventListener('resize', adjustProgramsLayout);
    
    // 修改addNewProgramToGrid函数，添加调整布局功能
    const originalAddNewProgramToGrid = addNewProgramToGrid;
    
    // 重新定义函数，添加布局调整功能
    window.addNewProgramToGrid = function(programName, iconPath) {
        originalAddNewProgramToGrid(programName, iconPath);
        adjustProgramsLayout();
    };

}); 