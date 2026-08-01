# WorkBuddy Android App

基于 **WebView** 的 WorkBuddy 移动端封装应用，图标使用 WorkBuddy 官方 Logo。
打开即加载 `https://www.workbuddy.cn/app`，支持 JavaScript、下拉刷新、返回键网页后退。

> 本仓库同时附带已编译好的安装包（见 `apk/` 目录），无需本地搭建 Android 环境即可直接安装测试。

---

## 目录结构

```
workbuddy-android/
├── app/                      # Android 模块
│   ├── build.gradle
│   ├── workbuddy.keystore    # 签名密钥（已 gitignore，本地保留）
│   └── src/main/             # Manifest / Java / 布局 / 各分辨率图标
├── apk/                      # 已编译安装包（可直接安装）
│   ├── WorkBuddy-v1.0-release.apk   # 正式签名包（推荐）
│   └── WorkBuddy-v1.0-debug.apk     # 调试包
├── push_to_github.sh         # 一键推送到你的 GitHub 仓库
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

## 推送到 GitHub

> 因当前编译环境（沙盒）的网络出口封锁了 `github.com`，无法从沙盒内自动推送。
> 请在**你本地能访问 GitHub 的机器**上，用下面的方式推送：

### 方式一：一键脚本（推荐）

1. 用编辑器打开 `push_to_github.sh`
2. 把顶部的 `REPO_URL` 改成你的仓库地址，例如：
   ```bash
   REPO_URL="https://github.com/你的用户名/你的仓库名.git"
   ```
3. 运行：
   ```bash
   bash push_to_github.sh
   ```

### 方式二：手动命令

```bash
git init
git add .
git commit -m "Add WorkBuddy Android WebView app (v1.0)"
git branch -M main
git remote add origin https://github.com/你的用户名/你的仓库名.git
git push -u origin main
```

> `.gitignore` 已自动排除 `build/`、`local.properties` 和签名密钥 `app/workbuddy.keystore`，
> 避免把私钥和本地配置误提交到公开仓库。

---

## 附：移动端网页问题报告

在 `https://www.workbuddy.cn/app` 的手机端（视口 320–412px）发现两类布局问题，
详见随附文档《WorkBuddy-移动端问题报告.md》：

1. **右上角「登录」按钮被遮挡**：头部容器 `.cloud-welcome__header` 宽度为 540px，超出 390px 视口，
   登录按钮右边缘到达 x≈524px，超出视口约 134px，导致按钮被裁切/遮挡。
2. **「WorkBuddy，我帮你」标题未居中**：标题块的 `text-align` / 容器宽度未按移动端约束居中。

报告内已给出对应的 CSS 修复建议（媒体查询 + 弹性布局 + 标题居中）。
