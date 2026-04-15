# 🧭 TravelAgent - 智能旅行规划助手

基于多Agent协作的Android旅行规划应用，通过无障碍服务自动化操作手机中已安装的App（小红书、携程、12306、美团等），获取实时数据并结合Claude AI进行智能规划。

![Android](https://img.shields.io/badge/Android-26%2B-green)
![Kotlin](https://img.shields.io/badge/Kotlin-1.9-blue)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material3-purple)
![License](https://img.shields.io/badge/License-MIT-yellow)

## ✨ 功能特点

### 🤖 5个AI Agent协同工作

| Agent | 职责 | 数据来源 |
|-------|------|----------|
| 🎯 协调者 | 任务分解与结果整合 | Claude AI |
| 🚄 交通专家 | 火车/高铁票务查询 | 12306 App |
| 🏨 住宿顾问 | 酒店搜索与推荐 | 携程 App |
| 🗺️ 景点规划 | 景点攻略与行程安排 | 小红书 App |
| 🍜 美食探索 | 当地美食推荐 | 美团/大众点评 App |

### 🔧 核心技术

- **无障碍服务自动化**：无需API，直接操作用户已安装的App
- **多Agent并行执行**：使用Kotlin协程实现高效并发
- **Claude AI推理**：智能整合数据，生成个性化旅行方案
- **Jetpack Compose UI**：现代化Material 3设计

## 📱 界面预览

1. **权限引导页** - 开启无障碍服务，检查已安装App
2. **输入表单页** - 填写出发地、目的地、日期、预算等
3. **Agent执行页** - 实时展示5个Agent的工作状态和日志
4. **结果展示页** - 完整的旅行方案，包含交通、住宿、景点、美食

## 🚀 快速开始

### 方式一：下载APK

从 [Releases](../../releases) 页面下载最新APK直接安装。

### 方式二：GitHub Actions自动构建

1. Fork 本仓库到您的GitHub账号
2. 进入您的仓库 → Actions → Build Android APK
3. 点击 "Run workflow" 手动触发构建
4. 等待构建完成后，在 Artifacts 中下载APK

### 方式三：本地构建

1. **克隆仓库**
```bash
git clone https://github.com/YOUR_USERNAME/TravelAgent.git
cd TravelAgent
```

2. **配置Claude API Key**（可选）

在 `local.properties` 中添加：
```properties
CLAUDE_API_KEY=your_api_key_here
```

3. **构建APK**
```bash
./gradlew assembleDebug
```

APK输出路径：`app/build/outputs/apk/debug/app-debug.apk`

## 📋 使用说明

### 1. 安装必要的App

确保手机中已安装以下App（至少一个）：
- 铁路12306 - 用于查询火车票
- 携程旅行 - 用于搜索酒店
- 小红书 - 用于获取旅行攻略
- 美团/大众点评 - 用于美食推荐

### 2. 开启无障碍服务

首次使用需要开启无障碍服务：
1. 打开App，点击"前往设置"
2. 在系统设置中找到"无障碍"
3. 找到"旅行智能助手"并开启

### 3. 开始规划旅行

1. 填写出发地和目的地
2. 选择出行日期
3. 设置预算和人数
4. 选择旅行偏好（文化、美食、自然等）
5. 点击"开始智能规划"

### 4. 等待AI团队工作

App会自动：
- 打开12306搜索火车票
- 打开携程搜索酒店
- 打开小红书搜索攻略
- 打开美团搜索美食

整个过程约1-3分钟，请保持屏幕常亮。

## 🔐 隐私说明

- **本地运行**：所有自动化操作都在您的手机本地执行
- **无数据上传**：App不会将您的个人信息上传到任何服务器
- **API通信**：仅与Claude API通信用于AI推理
- **无障碍权限**：仅用于自动化操作其他App，不读取敏感信息

## 🛠️ 技术架构

```
TravelAgent/
├── app/
│   ├── src/main/
│   │   ├── java/com/travelagent/
│   │   │   ├── MainActivity.kt          # 主Activity
│   │   │   ├── TravelAgentApp.kt         # Application类
│   │   │   ├── agents/
│   │   │   │   └── AgentCoordinator.kt   # 多Agent协调器
│   │   │   ├── services/
│   │   │   │   ├── TravelAccessibilityService.kt  # 无障碍服务
│   │   │   │   ├── AppAutomationController.kt     # App自动化
│   │   │   │   └── AgentForegroundService.kt      # 前台服务
│   │   │   ├── data/
│   │   │   │   ├── models/Models.kt      # 数据模型
│   │   │   │   └── repository/ClaudeApiService.kt # Claude API
│   │   │   ├── di/
│   │   │   │   └── AppModule.kt          # Hilt依赖注入
│   │   │   └── ui/
│   │   │       ├── MainViewModel.kt      # ViewModel
│   │   │       ├── theme/Theme.kt        # 主题配置
│   │   │       └── screens/              # Compose UI页面
│   │   ├── res/                          # 资源文件
│   │   └── AndroidManifest.xml           # 清单文件
│   └── build.gradle.kts
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
```

## 🔧 自定义配置

### 添加Claude API Key

如果您有Claude API Key，可以获得更智能的AI推理：

1. 在 `gradle.properties` 中添加：
```properties
CLAUDE_API_KEY=sk-ant-xxxxx
```

2. 或在构建时传入：
```bash
./gradlew assembleDebug -PCLAUDE_API_KEY=sk-ant-xxxxx
```

### 修改支持的App

在 `Models.kt` 中修改 `AppPackages` 对象：

```kotlin
object AppPackages {
    const val XIAOHONGSHU = "com.xingin.xhs"
    const val CTRIP = "ctrip.android.view"
    // 添加更多App...
}
```

## 📝 注意事项

1. **自动化限制**：部分App可能有防自动化检测，建议保持App为最新版本
2. **屏幕常亮**：规划过程中请保持屏幕常亮，否则自动化可能中断
3. **网络连接**：确保手机网络畅通
4. **无障碍冲突**：如果已启用其他无障碍服务，可能会产生冲突

## 🤝 贡献

欢迎提交Issue和Pull Request！

## 📄 许可证

MIT License - 详见 [LICENSE](LICENSE) 文件

## 🙏 致谢

- [Anthropic Claude](https://www.anthropic.com/) - AI推理能力
- [Jetpack Compose](https://developer.android.com/jetpack/compose) - UI框架
- [Hilt](https://dagger.dev/hilt/) - 依赖注入
