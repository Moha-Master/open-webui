# Open WebUI Android 应用

这个Android应用提供了一个精简的WebView界面来访问OpenWebUI实例，解决了Android Edge浏览器中输入框跳动的问题。

## 项目特点

- 简单的URL输入界面，可连接到任何OpenWebUI实例
- 本地存储实例URL，方便下次使用
- 完整的WebView体验，避免浏览器问题
- 为OpenWebUI界面进行了优化

## 项目结构

```
android-app/
├── app/
│   ├── src/main/
│   │   ├── java/com/openwebui/MainActivity.java  # 主Activity，包含WebView和URL输入逻辑
│   │   ├── res/
│   │   │   ├── layout/activity_main.xml          # 主界面布局
│   │   │   ├── values/strings.xml                # 应用字符串资源
│   │   │   └── mipmap-*/ic_launcher.png          # 应用图标
│   │   └── AndroidManifest.xml                   # 应用配置
├── build.gradle                                  # 模块级构建配置
├── settings.gradle                               # 项目设置
├── gradle.properties                             # Gradle属性
├── gradle/wrapper/gradle-wrapper.properties      # Gradle Wrapper配置
├── gradlew, gradlew.bat                          # Gradle Wrapper脚本
└── README.md                                     # 项目说明
```

## 功能说明

### MainActivity.java
- 提供输入OpenWebUI实例URL的界面
- 使用SharedPreferences保存URL
- 配置WebView以获得最佳体验
- 自动在URL上添加https://协议前缀（如果未提供）

### WebView优化
- 启用JavaScript和DOM存储
- 优化视口以适应移动设备
- 启用缩放功能
- 使用WebViewClient处理页面导航
- 完全全屏体验，无状态栏和标题栏
- 支持文件访问和内容访问
- 设置UTF-8文本编码

## 如何构建项目

### 方法1：使用Android Studio（推荐）
1. 打开Android Studio
2. 选择"Open an existing project"
3. 导航到此目录并选择android-app文件夹
4. Android Studio会自动同步项目
5. 点击"Run"按钮在设备或模拟器上构建和安装应用

### 方法2：使用命令行
1. 确保已安装Android SDK和构建工具
2. 在项目根目录运行以下命令：
```bash
./gradlew assembleDebug
```
3. 生成的APK文件将位于 `android-app/app/build/outputs/apk/debug/app-debug.apk`

### 注意事项
由于项目配置使用了Gradle 8.5和Java 11兼容设置，如果在同步项目时遇到任何版本错误，请检查：
1. Android Studio版本是否为最新
2. Java Development Kit (JDK) 版本是否为11或更高版本
3. Gradle版本是否与项目指定的版本兼容

## 使用说明

1. 在Android设备上安装应用
2. 输入你的OpenWebUI实例URL（例如：https://your-openwebui-instance.com）
3. 点击"保存并打开"按钮连接到你的实例
4. 应用将记住你的URL供将来使用

## 权限要求

- `INTERNET` - 连接到OpenWebUI实例
- `ACCESS_NETWORK_STATE` - 检查网络连接状态

## 技术说明

此应用使用Android WebView组件来显示OpenWebUI界面，这避免了Android浏览器中的已知问题，特别是Edge浏览器中输入框跳动的问题。WebView提供了更稳定的体验，特别是在处理表单输入时。

## 最低系统要求

- Android 7.0 (API level 24) 或更高版本
- 互联网连接以访问OpenWebUI实例