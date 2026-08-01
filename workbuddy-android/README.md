# WorkBuddy Android App

基于 **WebView** 的 WorkBuddy 移动端封装应用，图标使用 WorkBuddy 官方 Logo。
打开即加载 `https://www.workbuddy.cn/app`，支持 JavaScript、下拉刷新、返回键网页后退。

> 本仓库同时附带已编译好的安装包（见 `安装包/` 目录），无需本地搭建 Android 环境即可直接安装测试。

---

## 目录结构

```
workbuddy-android/
├── app/                      # Android 模块
│   ├── build.gradle
│   ├── workbuddy.keystore    # 签名密钥（已 gitignore，本地保留）
│   └── src/main/             # Manifest / Java / 布局 / 图标 / 移动端适配 CSS
├── gradle/wrapper/           # Gradle Wrapper（支持本地构建）
├── build.gradle / settings.gradle / gradle.properties
└── README.md
```

---

## 配置

| 项目 | 值 |
|------|----|
| 包名 | `com.workbuddy.app` |
| 起始 URL | `https://www.workbuddy.cn/app` |
| minSdk | 21 |
| targetSdk / compileSdk | 35 |
| 图标 | WorkBuddy 官方 Logo（mipmap 各分辨率） |

---

## 构建（如需本地重新编译）

```bash
./gradlew assembleRelease   # Release 签名包
./gradlew assembleDebug     # Debug 包
```

产物路径：`app/build/outputs/apk/release/app-release.apk`

### 签名

当前使用临时生成的 `app/workbuddy.keystore`：

- 密码 / keyPassword：`workbuddy`
- keyAlias：`workbuddy`

> ⚠️ **正式发布前请替换为你的自有发布密钥**，不要把本仓库的 keystore 用于生产。

---

## 附：移动端网页问题报告与修复

在 `https://www.workbuddy.cn/app` 的手机端（视口 320–412px）发现两类布局问题，
详见随附文档《WorkBuddy-移动端问题报告.md》：

1. **右上角「登录」按钮被遮挡**：头部容器 `.cloud-welcome__header` 宽度为 540px，超出 390px 视口，
   登录按钮右边缘到达 x≈524px，超出视口约 134px，导致按钮被裁切/遮挡。
2. **「WorkBuddy，我帮你」标题未居中**：标题块的 `text-align` / 容器宽度未按移动端约束居中。

根因：页面本身**缺少 `<meta name="viewport">`** 且主布局固定 540px，在手机 WebView 中右侧被切掉。

> ✅ **已修复**（v1.1）：`MainActivity` 在 `onPageFinished` 时注入
> `width=device-width` 的 viewport meta 标签，并通过 `R.raw.mobile_fix` 注入移动端适配 CSS
> （`app/src/main/res/raw/mobile_fix.css`），把 540px 桌面布局在 ≤768px 视口下重排为单列、
> 标题居中、登录按钮保留。验证：390/375/412px 视口下 `scrollWidth == clientWidth`，无横向溢出。