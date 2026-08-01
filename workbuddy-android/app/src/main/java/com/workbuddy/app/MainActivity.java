package com.workbuddy.app;

import android.os.Bundle;
import android.util.Log;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.appcompat.app.AppCompatActivity;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webview);
        swipeRefresh = findViewById(R.id.swiperefresh);

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
            settings.getUserAgentString() + " WorkBuddyApp/1.0"
        );

        // 注入 viewport meta + 移动端适配 CSS
        final String mobileCss = readRawText(R.raw.mobile_fix);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                injectMobileFix(view, mobileCss);
            }
        });
        webView.setWebChromeClient(new WebChromeClient());

        if (savedInstanceState == null) {
            webView.loadUrl("https://www.workbuddy.cn/app");
        } else {
            webView.restoreState(savedInstanceState);
        }

        swipeRefresh.setOnRefreshListener(() -> {
            webView.reload();
            swipeRefresh.setRefreshing(false);
        });
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
