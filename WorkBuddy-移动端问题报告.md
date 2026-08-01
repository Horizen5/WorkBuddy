# WorkBuddy 移动端问题排查与 APK 构建报告

> 排查时间：2026-08-01  
> 测试页面：`https://www.workbuddy.cn/app`  
> 测试视口：iPhone 13（390×844）、iPhone SE（375×667）、Pixel 7（412×915）

---

## 1. 发现的问题

### 问题 A：`WorkBuddy，我帮你` 标题没有居中

在 `/app` 的手机端视图中，首屏大标题 `WorkBuddy，我帮你` 明显偏左，右侧被截断，没有做到水平居中。

**截图证据：**

- 文件：`workbuddy-mobile-screenshot-iphone13.png` / `workbuddy-mobile-screenshot-iphonese.png`（同目录）
- 现象：标题左侧留空较多，右侧文字被切出屏幕，整体视觉不居中。

**可能原因：**

- 标题容器宽度未与视口对齐，或缺少 `text-align: center` / `justify-content: center`。
- 外层 `.cloud-welcome` / grid item 被设定了比视口更宽的最小宽度（见问题 B），连带使标题偏移。

**修复建议：**

```css
/* 为标题容器增加居中对齐 */
.welcome-title, .cloud-welcome__title {
  text-align: center;
  width: 100%;
  box-sizing: border-box;
  padding: 0 16px;
}

/* 若使用 flex 布局 */
.cloud-welcome__hero {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}
```

---

### 问题 B：右上角「登录」按钮被半边遮住

在未登录状态下，`/app` 页面顶部右上角的「登录」按钮会部分甚至完全溢出可视区域。

**DOM 几何数据证据（390px 视口）：**

| 元素 | `x` | `width` | `right` |
|------|-----|---------|---------|
| `.cloud-welcome__header` | 0 | **540** | 540 |
| `.cloud-welcome__header-content` | 16 | 508 | 524 |
| `.cloud-welcome__actions` | 452 | 72 | 524 |
| `.cloud-welcome__login-btn`（登录） | 452 | 72 | 524 |

- 视口宽度为 **390px**。
- 但 `.cloud-welcome__header` 实际宽度为 **540px**，比视口宽出 150px。
- 登录按钮右边缘到达 `x=524`，已超出 390px 视口，因此只能看到左侧一小部分或完全看不到。

**可能原因：**

- 外层网格项 / flex 容器设置了固定宽度（约 540px），未对移动端做响应式收缩。
- 或 `min-width: 540px` 等规则限制了容器宽度，导致水平溢出。

**修复建议：**

```css
/* 让外层容器在移动端跟随视口宽度 */
.cloud-welcome,
.cloud-welcome__header,
.cloud-welcome__header-content,
.teams-content-wrapper,
.teams-main-content,
[class*="gridViewItem"] {
  width: 100% !important;
  min-width: auto !important;
  max-width: 100vw !important;
  box-sizing: border-box;
}

/* 登录按钮区域不被挤压出屏幕 */
.cloud-welcome__actions {
  flex-shrink: 0;
  margin-left: auto;
}

/* 在窄屏下隐藏桌面导航，保留登录按钮 */
@media (max-width: 768px) {
  .cloud-welcome__nav {
    display: none;
  }
}
```

---

## 2. 已构建的 Android APK

由于当前沙箱环境无法直接访问 GitHub（`api.github.com` / `github.com` 均被网络策略阻断），无法自动推送到你的仓库。因此我把源码和 APK 都留在了 `/workspace`，你可以直接下载或手动 push。

### 2.1 交付文件

| 文件 | 说明 | 大小 |
|------|------|------|
| `安装包/WorkBuddy-v1.1-release.apk` | **推荐安装** 签名 Release 包 | 4.6 MB |
| `安装包/WorkBuddy-v1.1-release.apk` | 调试用 Debug 包 | 5.6 MB |
| `/workspace/workbuddy-android/` | 完整 Android Studio / Gradle 源码 | - |

### 2.2 应用信息

- **包名：** `com.workbuddy.app`
- **应用名称：** WorkBuddy
- **版本：** 1.0（versionCode 1）
- **图标：** 使用网站 `logo.svg` 生成的多分辨率 PNG
- **功能：** 基于 WebView 加载 `https://www.workbuddy.cn/app`
  - 启用 JavaScript、DOM Storage、数据库缓存
  - 支持下拉刷新
  - 支持返回键回退网页
  - 屏幕旋转/尺寸变化时不重载

### 2.3 自行编译

```bash
cd /workspace/workbuddy-android
# Linux / macOS
./gradlew assembleRelease

# 或 Windows
gradlew.bat assembleRelease
```

> 当前 Release 签名使用的是沙箱内临时生成的 `workbuddy.keystore`，密码均为 `workbuddy`。正式上线前，请替换为你自己的发布密钥。

---

## 3. GitHub 上传说明

我尝试使用你提供的 Token（`ghp_…uE9`）推送，但沙箱对 GitHub 域名完全阻断：

```
curl: (35) OpenSSL SSL_error_SYSCALL in connection to api.github.com:443
```

因此无法自动上传。你可以按以下步骤手动推送到已有仓库：

```bash
# 1. 在你的本地克隆仓库
# 2. 把 /workspace/workbuddy-android/ 的内容复制进去
# 3. 提交并推送
git add workbuddy-android/ *.apk
git commit -m "Add WorkBuddy Android wrapper v1.1"
git push origin main
```

**重要：** 你此前把 GitHub Token 以明文形式发给了我，该 Token 已不再安全。请在 GitHub 上立即：

1. 进入 **Settings → Developer settings → Personal access tokens**
2. 找到该 Token 并 **Revoke**
3. 重新生成一个新的 Token，并通过更安全的方式发送

---

## 4. 测试环境

- Android SDK 35
- Build Tools 35.0.0
- Gradle 8.11
- Android Gradle Plugin 8.8.0
- Java 20 (Azul Zulu)

---

## 5. 下一步建议

1. **优先修复网页 CSS**：登录按钮被遮挡和标题不居中都是网页本身的问题，APK 只是套壳 WebView，打开后仍会复现。
2. **替换签名密钥**：上线前把 `app/workbuddy.keystore` 替换为你的正式密钥。
3. ** revoke 旧 Token**：按第 3 节说明处理。
4. 如需我帮你把网页 CSS 修复写成可部署的补丁，或把 GitHub 仓库地址发给我尝试其他上传方式，可以继续告诉我。
