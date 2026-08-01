# WorkBuddy

一个基于 **WebView** 的 WorkBuddy 移动端封装应用。打开即加载 `https://www.workbuddy.cn/app`，支持 JavaScript、下拉刷新、返回键网页后退，图标使用 WorkBuddy 官方 Logo。

### 下载

**[WorkBuddy v1.1.0](https://github.com/Horizen5/WorkBuddy/releases/latest)** · Release APK · Android 5.0+

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
- **UA 标识**：User-Agent 追加 `WorkBuddyApp/1.0`，便于服务端识别
- **全分辨率图标**：mdpi 到 xxxhdpi 五套启动图标

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
│   │           ├── mipmap-*/ic_launcher.png
│   │           └── values/strings.xml
│   ├── apk/                     # 已编译安装包
│   │   ├── WorkBuddy-v1.0-release.apk
│   │   └── WorkBuddy-v1.0-debug.apk
│   ├── build.gradle / settings.gradle / gradle.properties
│   └── README.md
├── 安装包/                       # 独立安装包（与 apk/ 内容相同）
│   ├── WorkBuddy-v1.0-release.apk
│   └── WorkBuddy-v1.0-debug.apk
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

## 五、技术笔记（来自 [dev-craft](https://github.com/Horizen5/dev-craft)）

以下是本项目中用到的工程实践笔记，提炼自真实项目的踩坑经验。

### 5.1 WebView 性能：列表即缓存，深度即懒加载

> **一句话方法**：列表层只做轻量展示与跳转，把重的 IO 和分析推到点击时，并用「PM 同步 + 缓存 + 广播」保证又快又一致。

应用列表页是用户**每次进入都要等**的页面，理应最快。常见错误写法：

```kotlin
// 慢：列表阶段就把所有重活干完了
pm.getInstalledPackages(0)
    .mapNotNull { pi ->
        val icon = ai.loadIcon(pm).toBitmap()   // 同步解码 Bitmap
        AppItem(pkg, label, version, icon, isSystem)
    }
    .sortedBy { it.label.lowercase() }
```

正确架构——四层保证又快又一致：

```
① 启动同步   getInstalledApplications(0)    保证「打开不漏」
② 数据库缓存  Room app_cache                 避免重复解析
③ 实时广播   PACKAGE_ADDED/REMOVED          保证「运行中也不 stale」
④ 深度懒加载  点击才全量探针                  重的活推到最后
```

关键代码骨架：

```kotlin
class AppRepository(private val app: Context) {
    // 快列表：零额外 flag，不碰组件、不解码图标
    suspend fun fastList(): List<FastApp> = withContext(Dispatchers.IO) {
        pm.getInstalledApplications(0)
            .filter { it.packageName != app.packageName }
            .map { ai ->
                FastApp(ai.packageName,
                        pm.getApplicationLabel(ai).toString(),
                        ai.sourceDir ?: "",
                        (ai.flags and ApplicationInfo.FLAG_SYSTEM) != 0)
            }
    }

    // 仅富化缓存缺失 / APK 变化的项
    suspend fun enrich(fast: FastApp): Meta = withContext(Dispatchers.IO) {
        val cached = dao.get(fast.pkg)
        if (cached != null && cached.apkPath == fast.apkPath) return@withContext cached.toMeta()
        val info = pm.getPackageInfo(fast.pkg, COMPONENT_FLAGS)
        dao.put(info.toMeta().toEntity())
        info.toMeta()
    }
}
```

图标缓存——内存 LRU，只解可见项：

```kotlin
object IconCache {
    private val mem = LruCache<String, Bitmap>(400)
    suspend fun load(pm: PackageManager, pkg: String): Bitmap? {
        mem.get(pkg)?.let { return it }
        return withContext(Dispatchers.IO) {
            runCatching {
                pm.getApplicationInfo(pkg, 0).loadIcon(pm).toBitmap()
            }.getOrNull()?.also { mem.put(pkg, it) }
        }
    }
}
```

实时层——监听包变化广播：

```kotlin
private val packageWatcher = object : BroadcastReceiver() {
    override fun onReceive(c: Context?, intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_PACKAGE_ADDED,
            Intent.ACTION_PACKAGE_REMOVED,
            Intent.ACTION_PACKAGE_REPLACED -> viewModelScope.launch { refreshAppList() }
        }
    }
}
val filter = IntentFilter().apply {
    addAction(Intent.ACTION_PACKAGE_ADDED)
    addAction(Intent.ACTION_PACKAGE_REMOVED)
    addAction(Intent.ACTION_PACKAGE_REPLACED)
    addDataScheme("package")   // 必须加，否则收不到包级广播
}
app.registerReceiver(packageWatcher, filter)
```

**两个容易踩的坑**：

1. 用 `getInstalledApplications(0)`，不要 `GET_META_DATA`——后者会读 APK 的 `<meta-data>`，反而更慢
2. 图标只进内存 LRU，不要落磁盘——随 APK 版本变、只占一屏，磁盘缓存是负优化

> 适用任何「列表 + 详情」结构：文件管理器、会话列表、已装应用、音乐/视频库……

### 5.2 Xposed/LSPosed 模块「已激活」状态检测

> **方法**：激活检测不要只依赖一条路径。三个隐式前置条件任一失败都静默误报，加两条不依赖 Hook 的兜底检测更可靠。

**三个失效点**（任一失败都显示"未激活"）：

| 失效点 | 原因 | 解法 |
|--------|------|------|
| 入口没处理模块自身 | `if (packageName != TARGET) return` 把自己过滤掉了 | 入口里先处理 `MODULE_PKG` |
| 模块不在作用域 | LSPosed 只给作用域内应用注入 | `scope.list` 加上模块自身包名 |
| 方法被编译器内联 | `private fun isModuleEnabled() = false` 被内联成常量 | 改为 `public` + 读 `@Volatile` 字段 |

正确写法——方法本身杜绝内联：

```kotlin
@Volatile
private var hookedFlag = false

// public + 读字段，两条都是防内联的关键
fun isModuleEnabled(): Boolean = hookedFlag
```

两条不依赖 Hook 的兜底检测：

```kotlin
/**
 * XposedBridge 是 compileOnly，不会打进 APK。
 * 加载得到 = 这个进程确实被注入了。
 */
private fun xposedInjected(): Boolean = try {
    Class.forName("de.robv.android.xposed.XposedBridge", false, classLoader)
    true
} catch (t: Throwable) {
    false
}

/**
 * MODE_WORLD_READABLE 在原生 Android 上会抛 SecurityException，
 * 只有 LSPosed 专门为模块放开了它。
 */
private var prefWritable = false
sp = try {
    getSharedPreferences(PREF_NAME, Context.MODE_WORLD_READABLE).also { prefWritable = true }
} catch (t: Throwable) {
    prefWritable = false
    getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
}

// 三选一命中即可
val active = isModuleEnabled() || xposedInjected() || prefWritable
```

状态条显示命中了哪几项——出问题时能一眼看出是哪一环断的：

```kotlin
text = if (active) {
    val how = buildList {
        if (isModuleEnabled()) add("Hook")
        if (xposedInjected()) add("注入")
        if (prefWritable) add("配置可写")
    }.joinToString("/")
    "模块已激活（$how）"
} else {
    "模块未激活 · 请在 LSPosed 中启用，作用域勾选目标应用和本模块，然后重启手机"
}
```

> 完整文章见 [dev-craft 仓库](https://github.com/Horizen5/dev-craft)。

---

## 六、移动端网页问题报告

在 `https://www.workbuddy.cn/app` 的手机端（视口 320-412px）发现两类布局问题：

1. **右上角「登录」按钮被遮挡**：头部容器宽度 540px 超出 390px 视口，登录按钮被裁切
2. **标题未居中**：`text-align` / 容器宽度未按移动端约束居中

详细修复建议见随附文档 `WorkBuddy-移动端问题报告.md`。

> ✅ **已修复（v1.1）**：根因是页面缺少 `<meta name="viewport">` 且主布局固定 540px。
> `workbuddy-android` 已在 `MainActivity` 注入 viewport meta + `res/raw/mobile_fix.css`
> （媒体查询重排为单列、标题居中、登录按钮保留），手机视口下已无横向溢出。

---

## 七、更新日志

### v1.0.0

- 首个版本
- WebView 封装 WorkBuddy 网页应用
- 支持 JavaScript、DOM 存储、下拉刷新、返回键后退
- 全分辨率启动图标（mdpi - xxxhdpi）
- 临时签名密钥 `workbuddy.keystore`

---

## 八、免责

- 本应用仅为 WorkBuddy 网页的 WebView 封装，不修改任何服务端逻辑
- 签名密钥为临时生成，正式发布前请替换为自有密钥
- 移动端布局问题需服务端配合修复，本应用无法在客户端侧解决
