package com.workbuddy.app;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "WorkBuddy";
    private static final String INJECT_STYLE_ID = "workbuddy-mobile-fix";

    private WebView webView;
    private SwipeRefreshLayout swipeRefresh;
    private ImageButton refreshButton;

    private ValueCallback<Uri[]> filePathCallback;
    private WebChromeClient.FileChooserParams fileChooserParams;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final ActivityResultLauncher<Intent> filePickerLauncher = registerForActivityResult(
        new ActivityResultContracts.StartActivityForResult(),
        result -> {
            if (filePathCallback == null || fileChooserParams == null) return;
            try {
                Uri[] results = fileChooserParams.parseResult(result.getResultCode(), result.getData());
                filePathCallback.onReceiveValue(results);
            } catch (Exception e) {
                Log.e(TAG, "parse file chooser result failed", e);
                filePathCallback.onReceiveValue(null);
            } finally {
                filePathCallback = null;
                fileChooserParams = null;
                // 文件选择器关闭后，根据当前滚动位置恢复下拉刷新开关
                syncSwipeRefreshEnabled();
            }
        }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 状态栏 / 导航栏：改为深色实色，且内容不再延伸到状态栏下方。
        // 这样页面顶栏（浅色）与状态栏（深色）形成“相反色”对比，
        // 既不会被染成黑灰色，内容也不会贴到屏幕最顶端。
        WindowCompat.setDecorFitsSystemWindows(getWindow(), true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(0xFF0F1419);
            getWindow().setNavigationBarColor(0xFF0F1419);
        }

        // 深色背景上使用浅色（白色）系统图标，保证状态栏可读
        WindowInsetsControllerCompat insetsController =
            new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        insetsController.setAppearanceLightStatusBars(false);
        insetsController.setAppearanceLightNavigationBars(false);

        webView = findViewById(R.id.webview);
        swipeRefresh = findViewById(R.id.swiperefresh);
        refreshButton = findViewById(R.id.refresh_button);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);

        // 关键：开启宽视口 + overview 缩放，让页面能按移动端宽度重新布局
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);

        settings.setUserAgentString(
            settings.getUserAgentString() + " WorkBuddyApp/1.4.0"
        );

        // 注入登录态桥接：网页 JS 检测登录态后回调，控制右上角刷新按钮显隐
        webView.addJavascriptInterface(new LoginStateBridge(), "WorkBuddyBridge");

        final String mobileCss = readRawText(R.raw.mobile_fix);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                // 页面加载完成后收起下拉刷新动画
                swipeRefresh.setRefreshing(false);
                injectMobileFix(view, mobileCss);
                injectLoginObserver(view);
            }
        });
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView view,
                                             ValueCallback<Uri[]> callback,
                                             WebChromeClient.FileChooserParams params) {
                // 先清理上一次可能残留的回调，避免卡死
                if (filePathCallback != null) {
                    filePathCallback.onReceiveValue(null);
                    filePathCallback = null;
                    fileChooserParams = null;
                }
                filePathCallback = callback;
                fileChooserParams = params;

                // 打开系统文件管理器前禁用下拉刷新，防止其手势与文件选择器冲突导致黑屏/卡死
                swipeRefresh.setEnabled(false);

                try {
                    Intent intent = params.createIntent();
                    if (intent.getCategories() == null
                            || !intent.getCategories().contains(Intent.CATEGORY_OPENABLE)) {
                        intent.addCategory(Intent.CATEGORY_OPENABLE);
                    }
                    filePickerLauncher.launch(intent);
                    return true;
                } catch (Exception e) {
                    Log.e(TAG, "open file chooser failed, try fallback", e);
                    try {
                        Intent fallback = new Intent(Intent.ACTION_GET_CONTENT);
                        fallback.addCategory(Intent.CATEGORY_OPENABLE);
                        fallback.setType("*/*");
                        if (params.getAcceptTypes() != null && params.getAcceptTypes().length > 0) {
                            fallback.setType(params.getAcceptTypes()[0]);
                        }
                        filePickerLauncher.launch(fallback);
                        return true;
                    } catch (Exception e2) {
                        Log.e(TAG, "fallback file chooser failed", e2);
                        filePathCallback.onReceiveValue(null);
                        filePathCallback = null;
                        fileChooserParams = null;
                        syncSwipeRefreshEnabled();
                        return false;
                    }
                }
            }
        });

        if (savedInstanceState == null) {
            webView.loadUrl("https://www.workbuddy.cn/app");
        } else {
            webView.restoreState(savedInstanceState);
        }

        // 下拉刷新：仅当 WebView 已滚动到页面顶部时启用，避免下滑浏览被误判为刷新
        webView.getViewTreeObserver().addOnScrollChangedListener(
            new ViewTreeObserver.OnScrollChangedListener() {
                @Override
                public void onScrollChanged() {
                    syncSwipeRefreshEnabled();
                }
            }
        );

        swipeRefresh.setOnRefreshListener(() -> {
            webView.reload();
            // 刷新完成后在 onPageFinished 收起动画
        });

        // 右上角刷新按钮：点击重新加载当前页面（默认 GONE，登录后由网页控制显示）
        refreshButton.setOnClickListener(v -> {
            swipeRefresh.setRefreshing(true);
            webView.reload();
        });
    }

    /**
     * 根据 WebView 是否处在页面顶部，同步 SwipeRefreshLayout 的可用状态。
     * 仅顶部可下拉刷新；页面已向下滚动时禁用，保证下滑用于浏览内容而非触发刷新。
     */
    private void syncSwipeRefreshEnabled() {
        if (webView != null && swipeRefresh != null) {
            swipeRefresh.setEnabled(webView.getScrollY() <= 0);
        }
    }

    /**
     * 登录态桥接：网页通过 window.WorkBuddyBridge.setLoggedIn(boolean) 通知原生层。
     * 登录后显示右上角刷新按钮，未登录/加载中隐藏。
     */
    private class LoginStateBridge {
        @JavascriptInterface
        public void setLoggedIn(boolean loggedIn) {
            mainHandler.post(() -> {
                if (refreshButton != null) {
                    refreshButton.setVisibility(loggedIn ? View.VISIBLE : View.GONE);
                }
            });
        }
    }

    /**
     * 注入 viewport meta 标签与移动端 CSS。
     * 页面本身缺少 viewport meta，且主布局固定 540px，导致手机右侧被切掉；
     * 通过 JS 在 onPageFinished 时强制 width=device-width 并写入覆盖样式。
     */
    private void injectMobileFix(WebView view, String css) {
        if (css == null || css.isEmpty()) {
            Log.w(TAG, "mobile_fix.css is empty, skip injection");
            return;
        }

        // 用反引号包裹 CSS，避免转义引号；CSS 中不含反引号与 ${}，安全。
        String js = "(function() {" +
            "  if (document.getElementById('" + INJECT_STYLE_ID + "')) return;" +
            "  var meta = document.querySelector('meta[name=\"viewport\"]');" +
            "  if (!meta) {" +
            "    meta = document.createElement('meta');" +
            "    meta.name = 'viewport';" +
            "    document.head.appendChild(meta);" +
            "  }" +
            "  meta.content = 'width=device-width, initial-scale=1.0, viewport-fit=cover';" +
            "  var style = document.createElement('style');" +
            "  style.id = '" + INJECT_STYLE_ID + "';" +
            "  style.textContent = `" + css + "`;" +
            "  document.head.appendChild(style);" +
            "})();";

        view.evaluateJavascript(js, null);
    }

    /**
     * 注入登录态检测脚本：
     * - 未登录：DOM 中存在 .cloud-welcome__login-btn 登录按钮 → 隐藏刷新按钮
     * - 加载中：存在 skeleton 骨架 → 隐藏刷新按钮
     * - 已登录：登录按钮消失且骨架消失 → 显示刷新按钮
     * 通过 MutationObserver + 定时轮询覆盖 SPA 路由切换（登录/登出）场景。
     */
    private void injectLoginObserver(WebView view) {
        String js = "(function() {" +
            "  if (window.__wbLoginInstalled) return;" +
            "  window.__wbLoginInstalled = true;" +
            "  function wbReport() {" +
            "    try {" +
            "      var loginBtn = document.querySelector(" +
            "        '.cloud-welcome__sso-login-btn, .cloud-welcome__login-btn, [class*=\"login-btn\"]');" +
            "      var skeleton = document.querySelector('[class*=\"skeleton\"]');" +
            "      var loggedIn = !loginBtn && !skeleton;" +
            "      if (window.WorkBuddyBridge) window.WorkBuddyBridge.setLoggedIn(loggedIn);" +
            "    } catch (e) {}" +
            "  }" +
            "  wbReport();" +
            "  if (window.MutationObserver) {" +
            "    var mo = new MutationObserver(wbReport);" +
            "    mo.observe(document.documentElement, {" +
            "      childList: true, subtree: true, attributes: true," +
            "      attributeFilter: ['class', 'style'] });" +
            "  }" +
            "  setInterval(wbReport, 1500);" +
            "})();";

        view.evaluateJavascript(js, null);
    }

    private String readRawText(int resId) {
        StringBuilder sb = new StringBuilder();
        try (InputStream is = getResources().openRawResource(resId);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
        } catch (IOException e) {
            Log.e(TAG, "Failed to read raw resource " + resId, e);
        }
        return sb.toString();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (webView != null) {
            webView.saveState(outState);
        }
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
