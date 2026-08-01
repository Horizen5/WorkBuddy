# WorkBuddy 完整交付包

本压缩包包含 **WorkBuddy 手机端 App 的全部交付物**：完整可编译的 Android 工程、已签名的安装包、移动端问题报告，以及推送到 GitHub 的说明与一键脚本。

## 目录结构

```
WorkBuddy-完整交付包/
├── workbuddy-android/                # 完整 Android Studio 工程（可直接打开编译）
│   ├── app/
│   │   ├── src/main/                 # 源码：MainActivity / Manifest / 布局 / 图标
│   │   │   ├── java/com/workbuddy/app/MainActivity.java
│   │   │   ├── res/layout/activity_main.xml
│   │   │   ├── res/mipmap-*/ic_launcher.png   # WorkBuddy Logo 各分辨率图标
│   │   │   └── res/values/strings.xml
│   │   ├── build.gradle
│   │   └── workbuddy.keystore        # 签名密钥（本地保留，已 gitignore）
│   ├── apk/                          # 已编译安装包
│   │   ├── WorkBuddy-v1.0-release.apk   # 正式签名包（推荐安装）
│   │   └── WorkBuddy-v1.0-debug.apk     # 调试包
│   ├── push_to_github.sh             # 一键推送到你的 GitHub 仓库（本地运行）
│   ├── build.gradle / settings.gradle / gradle.properties
│   ├── .gitignore
│   └── README.md
├── 安装包/                            # 顶层便于直接取用的 APK
│   ├── WorkBuddy-v1.0-release.apk
│   └── WorkBuddy-v1.0-debug.apk
├── WorkBuddy-移动端问题报告.md        # 手机端 bug（登录遮挡 / 标题未居中）+ CSS 修复建议
└── 交付说明与下一步.md                # 连通性说明 + GitHub 推送步骤 + 安全告警
```

## 你要做什么

### 1. 安装测试（最快）
直接用 `安装包/WorkBuddy-v1.0-release.apk` 在安卓手机上安装即可（WebView 封装，打开即加载 `https://www.workbuddy.cn/app`）。

### 2. 推送到 GitHub（本沙盒无法直连，需你本地执行）
> 当前编译环境（沙盒）网络出口封锁 GitHub，且可用代理均不转发鉴权，无法自动推送。
> 请在你本地能访问 GitHub 的机器上操作：

```bash
cd workbuddy-android
# 编辑 push_to_github.sh，把 REPO_URL 改成你的仓库，例如：
#   REPO_URL="https://github.com/Horizen5/WorkBuddy.git"
bash push_to_github.sh
```

或手动：
```bash
git init && git add . && git commit -m "Add WorkBuddy Android app v1.0"
git branch -M main
git remote add origin https://github.com/Horizen5/WorkBuddy.git
git push -u origin main
```

> 注意：你的 GitHub 仓库 `Horizen5/WorkBuddy` 已有一份工程，但**缺少 `res/layout/activity_main.xml` 和启动图标**，
> 本包里的 `workbuddy-android/` 是完整可编译版本，直接用它覆盖即可。

### 3. ⚠️ 安全：立即 revoke 之前明文发在对话里的两个 GitHub Token
- `ghp_XXXX...（已撤销，已脱敏）`
- `ghp_XXXX...（已撤销，已脱敏）`

路径：GitHub → Settings → Developer settings → Personal access tokens，全部撤销。

## App 技术参数
- 包名 `com.workbuddy.app` ｜ 起始页 `https://www.workbuddy.cn/app`
- minSdk 21 ｜ targetSdk / compileSdk 35
- 启用：JavaScript、DOM/数据库存储、下拉刷新、返回键后退、UA 追加 `WorkBuddyApp/1.0`
- 图标：WorkBuddy 官方 Logo 渲染为 mdpi→xxxhdpi
