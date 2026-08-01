package com.workbuddy.app;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.appcompat.app.AppCompatActivity;

import java.io.ByteArrayInputStream;

/**
 * WorkBuddy 手机适配壳
 *
 * 目标页面 https://www.workbuddy.cn/app 是固定 1280px 桌面布局，
 * 在手机浏览器中右侧内容被裁切。本 Activity 用 WebView 加载该页面，
 * 并通过 overview 缩放 + CSS 注入强制整页适配手机屏幕宽度。
 *
 * 适配策略：
 * 1. setUseWideViewPort + setLoadWithOverviewMode：按 viewport meta 自动缩放，整页可见
 * 2. 开启缩放控件：用户可双指放大查看细节
 * 3. 注入 CSS：强制 html/body/#root 宽度 = 100vw 并禁止横向滚动，
 *    兜底防止页面动态改动宽度导致右侧再次被裁
 */
public class MainActivity extends AppCompatActivity {

    private static final String TARGET_URL = "https://www.workbuddy.cn/app";

    private WebView webView;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 沉浸式状态栏，最大化可视区域
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        getWindow().setStatusBarColor(Color.TRANSPARENT);

        webView = new WebView(this);
        setContentView(webView);

        WebSettings s = webView.getSettings();
        // 必备：SPA 需要 JS + DOM Storage
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        // 核心：让 WebView 按 viewport 缩放，使 1280 宽页面整体适配屏幕宽度
        s.setUseWideViewPort(true);
        s.setLoadWithOverviewMode(true);
        // 允许用户双指缩放查看细节
        s.setSupportZoom(true);
        s.setBuiltInZoomControls(true);
        s.setDisplayZoomControls(false);
        // 缓存与兼容
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        s.setAllowFileAccess(true);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        s.setUserAgentString(s.getUserAgentString() + " WorkBuddyApp/1.0");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                // 注入 CSS 兜底：强制内容宽度跟随视口，禁止横向溢出
                String css = "html,body{width:100vw!important;max-width:100vw!important;"
                        + "overflow-x:hidden!important;margin:0!important;padding:0!important;}"
                        + "#root,#__next,.app,.container{width:100vw!important;max-width:100vw!important;}";
                String js = "(function(){var s=document.createElement('style');"
                        + "s.textContent=" + jsString(css) + ";"
                        + "(document.head||document.documentElement).appendChild(s);})();";
                view.evaluateJavascript(js, null);
            }
        });
        webView.setWebChromeClient(new WebChromeClient());

        if (savedInstanceState == null) {
            webView.loadUrl(TARGET_URL);
        } else {
            webView.restoreState(savedInstanceState);
        }
    }

    /** 把普通字符串转成 JS 字符串字面量（含引号与转义） */
    private static String jsString(String v) {
        StringBuilder b = new StringBuilder();
        b.append('\'');
        for (int i = 0; i < v.length(); i++) {
            char c = v.charAt(i);
            switch (c) {
                case '\'': b.append("\\'"); break;
                case '\\': b.append("\\\\"); break;
                case '\n': b.append("\\n"); break;
                case '\r': b.append("\\r"); break;
                default: b.append(c);
            }
        }
        b.append('\'');
        return b.toString();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        webView.saveState(outState);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // 页内回退优先交给 WebView
        if (keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack()) {
            webView.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onPause() {
        super.onPause();
        webView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        webView.onResume();
    }

    @Override
    protected void onDestroy() {
        webView.destroy();
        super.onDestroy();
    }
}
