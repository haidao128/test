/**
 * 移动平台创建器 - JavaScript示例应用
 * 
 * 这是一个简单的JavaScript应用示例，用于测试JavaScript运行时。
 * 它演示了如何使用提供的API进行各种操作。
 */

// 当应用启动时执行
function main() {
    console.log("Hello from JavaScript应用!");
    console.log("应用ID:", APP_ID);
    console.log("工作目录:", APP_PATH);
    
    // 获取平台信息
    const platformInfo = system.getPlatformInfo();
    console.log("平台信息:", JSON.stringify(platformInfo, null, 2));
    
    // 测试文件操作
    testFileOperations();
    
    // 测试存储功能
    testStorage();
    
    // 测试网络功能
    testNetwork();
    
    console.log("应用初始化完成!");
}

// 测试文件操作
function testFileOperations() {
    console.log("\n--- 文件操作测试 ---");
    
    // 创建目录
    if (!fs.exists("data")) {
        fs.mkdir("data");
        console.log("创建data目录成功");
    }
    
    // 写入文件
    const content = "这是一个测试文件\n创建于 " + new Date().toISOString();
    fs.writeFile("data/test.txt", content);
    console.log("写入文件成功");
    
    // 读取文件
    const readContent = fs.readFile("data/test.txt");
    console.log("读取文件内容:", readContent);
    
    // 获取文件信息
    const fileInfo = fs.stat("data/test.txt");
    console.log("文件信息:", JSON.stringify(fileInfo, null, 2));
    
    // 列出目录内容
    const files = fs.readdir("data");
    console.log("data目录中的文件:", files);
}

// 测试存储功能
function testStorage() {
    console.log("\n--- 存储功能测试 ---");
    
    // 存储数据
    storage.setItem("lastRun", new Date().toISOString());
    storage.setItem("counter", (storage.getItem("counter") || 0) + 1);
    
    // 读取数据
    const lastRun = storage.getItem("lastRun");
    const counter = storage.getItem("counter");
    
    console.log("上次运行时间:", lastRun);
    console.log("运行次数:", counter);
    
    // 获取所有键
    const keys = storage.keys();
    console.log("存储中的所有键:", keys);
}

// 测试网络功能
function testNetwork() {
    console.log("\n--- 网络功能测试 ---");
    
    // 发送GET请求
    console.log("正在发送HTTP请求...");
    
    try {
        // 注意: 这是异步操作的模拟，实际实现取决于JavaScript运行时
        const response = network.httpGet("https://httpbin.org/get");
        console.log("HTTP响应状态:", response.status);
        console.log("响应数据:", JSON.stringify(response.data || response.text, null, 2));
    } catch (error) {
        console.error("HTTP请求失败:", error);
    }
}

// 处理系统事件
function onSystemEvent(event) {
    console.log("收到系统事件:", event);
}

// 定义一个可导出的API，供其他代码调用
function getAppStatus() {
    return {
        appId: APP_ID,
        uptime: Math.floor((new Date() - startTime) / 1000),
        memoryUsage: system.getMemoryUsage()
    };
}

// 记录应用启动时间
const startTime = new Date();

// 调用主函数
main(); 