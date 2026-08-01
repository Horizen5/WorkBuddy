# WorkBuddy

一个基于 **WebView** 的 WorkBuddy 移动端封装应用。打开即加载 `https://www.workbuddy.cn/app`，支持 JavaScript、下拉刷新、返回键网页后退，图标使用 WorkBuddy 官方 Logo。

### 下载

**[WorkBuddy v1.4.0](https://github.com/Horizen5/WorkBuddy/releases/latest)** · Release APK · Android 5.0+

[![Release](https://img.shields.io/github/v/release/Horizen5/WorkBuddy?label=%E6%9C%80%E6%96%B0%E7%89%88%E6%9C%AC)](https://github.com/Horizen5/WorkBuddy/releases/latest)
[![Downloads](https://img.shields.io/github/downloads/Horizen5/WorkBuddy/total?label=%E4%B8%8B%E8%BD%BD%E9%87%8F)](https://github.com/Horizen5/WorkBuddy/releases)
[![Platform](https://img.shields.io/badge/platform-Android-3DDC84?logo=android&logoColor=white)](https://github.com/Horizen5/WorkBuddy/releases)
[![minSdk](https://img.shields.io/badge/minSdk-21-EF6C00?logo=android&logoColor=white)](https://github.com/Horizen5/WorkBuddy)

---

## 一、功能

- **WebView 封装**：将 WorkBuddy 网页应用封装为原生 Android 应用
- **JavaScript 支持**：完整启用 WebView 的 JavaScript 执行环境
- **DOM/数据库存储**：支持 localStorage 和 IndexedDB
- **下拉刷新**：原生手势下拉刷新网页内容
- **返回键后退**：物理返回键触发网页后退而非退出 App
- **UA 标识**：User-Agent 追加 `WorkBuddyApp/1.4.0`，便于服务端识别
- **状态栏相反色**：状态栏为深色实色 + 浅色系统图标，与浅色页面顶栏形成对比（不再把软件顶栏染成黑灰）
- **右上角刷新按钮**：登录后显示在右上角，未登录时隐藏，便于随时刷新页面
- **文件上传优化**：调起系统文件管理器更稳定，规避卡死/黑屏
- **下拉刷新防误触**：仅当页面滚动到顶部时可下拉刷新；页面已下滑时，下拉用于浏览内容而非刷新
- **全分辨率图标**：mdpi 到 xxxhdpi 五套启动图标
- **移动端布局修复**：自动注入 viewport meta + CSS，解决手机端标题不居中、登录按钮被遮挡问题

---

## 二、技术参数

| 项目 | 值 |
|------|-----|
| 包名 | `com.workbuddy.app` |
| 起始 URL | `https://www.workbuddy.cn/app` |
| minSdk | 21（Android 5.0） |
| targetSdk / compileSdk | 35 |
| 图标 | WorkBuddy 官方 Logo（mipmap 各分辨率） |
| 签名 | `workbuddy.keystore`（临时密钥，正式发布请替换） |

---

## 三、目录结构

```
WorkBuddy/
├── workbuddy-android/           # Android 工程
│   ├── app/
│   │   ├── build.gradle
│   │   ├── workbuddy.keystore   # 签名密钥
│   │   └── src/main/
│   │       ├── AndroidManifest.xml
│   │       ├── java/com/workbuddy/app/MainActivity.java
│   │       └── res/
│   │           ├── layout/activity_main.xml
│   │           ├── raw/mobile_fix.css    # 移动端适配 CSS
│   │           ├── mipmap-*/ic_launcher.png
│   │           └── values/strings.xml
│   ├── gradle/wrapper/          # Gradle Wrapper（支持本地构建）
│   ├── build.gradle / settings.gradle / gradle.properties
│   └── README.md
├── 安装包/                       # 发布的安装包
│   └── WorkBuddy-v1.4-release.apk
├── README-打包说明.md
├── WorkBuddy-移动端问题报告.md
└── 交付说明与下一步.md
```

---

## 四、构建（如需本地重新编译）

```bash
./gradlew assembleRelease   # Release 签名包
./gradlew assembleDebug     # Debug 包
```

产物路径：`app/build/outputs/apk/release/app-release.apk`

### 签名

当前使用临时生成的 `app/workbuddy.keystore`：

- 密码 / keyPassword：`workbuddy`
- keyAlias：`workbuddy`

> **正式发布前请替换为你的自有发布密钥**，不要把本仓库的 keystore 用于生产。

---

## 五、更新日志

### v1.4.0

- **状态栏与页面顶栏相反色**：状态栏改为深色实色（`0xFF0F1419`）+ 浅色系统图标，页面顶栏为浅色实底，二者形成清晰对比；不再把软件顶部染成黑灰色
- **内容不再贴顶**：内容不再延伸到状态栏下方（`WindowCompat.setDecorFitsSystemWindows(true)`），顶栏增加固定上间距，避免内容顶到屏幕最上沿
- **右上角刷新按钮（登录后显示）**：新增顶部右侧刷新按钮，通过 JS 桥接检测登录态——未登录 / 加载中隐藏，登录后显示
- **修复文件选择器卡死 / 黑屏**：调起系统文件管理器前禁用下拉刷新手势，补充 `CATEGORY_OPENABLE`，失败时回退 `ACTION_GET_CONTENT`，结果回调稳妥收尾
- **修复下拉被误判为刷新**：给 WebView 增加滚动监听，仅在页面滚动到顶部时启用下拉刷新，已下滑时下拉用于浏览内容
- **UA 标识升级**：User-Agent 追加 `WorkBuddyApp/1.4.0`
- versionCode 5 / versionName 1.4.0

### v1.3.0

- **状态栏/导航条暗色磨砂底**：针对浅色页面把状态栏、导航栏改为深色半透明（`0xB3000000`），并强制系统图标为浅色，解决浅色背景下状态栏文字/图标看不清的问题
- **文件上传支持**：`WebChromeClient.onShowFileChooser` 接入 Android 原生文件选择器（`ACTION_OPEN_DOCUMENT`/`ACTION_GET_CONTENT`），网页里的 `<input type="file">` 会自动调起系统文件管理器
- **页面更协调**：为 body 设置浅灰底色 `#f2f3f5`，白色主面板带圆角与轻阴影；取消主内容区过度居中的 flex 布局，改为自然拉伸排列，减少空白感
- **UA 标识升级**：User-Agent 追加 `WorkBuddyApp/1.3.0`
- versionCode 4 / versionName 1.3.0

### v1.2.0

- **恢复左上角汉堡菜单（Toggle Sidebar）按钮**：此前误将含该按钮的顶栏 `.teams-top-bar` 整条隐藏，现已保留并适配移动端
- **全面屏 / 刘海适配**：原生侧透明状态栏 + `LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES`，网页侧用 `env(safe-area-inset-*)` 为顶/底栏留白，避免内容钻到刘海底下
- **UA 标识升级**：User-Agent 追加 `WorkBuddyApp/1.2.0`（服务端可按版本识别）
- 手机视口（320–412px）下已无横向溢出
- versionCode 3 / versionName 1.2.0

### v1.1.0

- **修复移动端布局**：`MainActivity` 注入 `viewport` meta + `res/raw/mobile_fix.css`，将固定 540px 桌面布局重排为单列、标题居中、登录按钮不再被裁切
- 手机视口（320–412px）下已无横向溢出
- 添加 Gradle Wrapper（`gradlew`），便于本地构建
- 验证截图：`verify_before.png` / `verify_after.png`
- versionCode 2 / versionName 1.1.0

### v1.0.0

- 首个版本
- WebView 封装 WorkBuddy 网页应用
- 支持 JavaScript、DOM 存储、下拉刷新、返回键后退
- 全分辨率启动图标（mdpi - xxxhdpi）
- 临时签名密钥 `workbuddy.keystore`

---

## 六、免责

- 本应用仅为 WorkBuddy 网页的 WebView 封装，不修改任何服务端逻辑
- 签名密钥为临时生成，正式发布前请替换为自有密钥
- 移动端布局问题已在客户端通过注入 viewport + `mobile_fix.css` 修复，无需服务端改动