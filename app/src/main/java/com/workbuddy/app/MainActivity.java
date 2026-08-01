package com.workbuddy.app;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.appcompat.app.AppCompatActivity;

/**
 * WorkBuddy 手机适配壳 v3
 *
 * 目标页 https://www.workbuddy.cn/app 是固定 1280px 桌面布局。
 * v3 做法：
 *   1. 用 setInitialScale 按屏幕宽度精确缩放，让 1280px 刚好铺满手机
 *   2. 用 JS 把登录按钮克隆到「缩放后可见区域的右上角」（fixed 定位）
 *   3. 把桌面整体内容向左平移，使原桌面 640 中心对齐手机屏幕中心
 *      —— 这样标题、场景切换、输入框都居中
 *   4. 保留双指缩放能力让用户放大查看细节
 */
public class MainActivity extends AppCompatActivity {

    private static final String TARGET_URL = "https://www.workbuddy.cn/app";

    private WebView webView;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 沉浸式 + 透明状态栏
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);

        // 获取屏幕宽度（dp 单位）用于计算缩放
        DisplayMetrics dm = getResources().getDisplayMetrics();
        final int screenWidthPx = dm.widthPixels;

        webView = new WebView(this);
        setContentView(webView);

        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);

        // 缩放配置：保留桌面 layout 宽度（1280px），只改变视觉缩放
        s.setUseWideViewPort(true);
        s.setLoadWithOverviewMode(false);

        // 允许用户双指缩放
        s.setSupportZoom(true);
        s.setBuiltInZoomControls(true);
        s.setDisplayZoomControls(false);

        // 让 1280px 桌面铺满屏幕
        int targetScale = Math.max(25, Math.min(50,
                (int) Math.round(screenWidthPx * 100.0 / 1280.0)));
        webView.setInitialScale(targetScale);

        // 其他兼容配置
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        s.setAllowFileAccess(true);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        s.setUserAgentString(s.getUserAgentString() + " WorkBuddyApp/3.0");
        s.setMediaPlaybackRequiresUserGesture(false);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                injectMobileFixes(view, screenWidthPx);
            }
        });
        webView.setWebChromeClient(new WebChromeClient());

        if (savedInstanceState == null) {
            webView.loadUrl(TARGET_URL);
        } else {
            webView.restoreState(savedInstanceState);
        }
    }

    /**
     * 注入移动端修复 JS：
     *  1. 克隆登录按钮到右上角（fixed 定位，相对缩放后视口）
     *  2. 把桌面内容整体左移，使原 640 中心对齐手机中心
     */
    private void injectMobileFixes(WebView view, int screenWidthPx) {
        // 桌面 layout 宽度（1280）
        final float desktopWidth = 1280f;
        // 实际视觉宽度
        final float visualWidth = screenWidthPx;
        // 缩放比（0.25 ~ 0.50）
        final float scaleRatio = visualWidth / desktopWidth;
        // 桌面内容要移动的距离（桌面坐标下的 px 偏移）：
        // 原桌面中心在 x=640，手机视觉中心对应桌面坐标 = visualWidth/(2*scaleRatio)
        // shift = visualCenter_in_desktop - 640
        final double shift = (visualWidth / (2 * scaleRatio)) - (desktopWidth / 2.0);

        StringBuilder b = new StringBuilder();
        b.append("(function(){");

        // 1. 克隆登录按钮到缩放后可见的右上角
        b.append("function setupLogin(){");
        b.append("var orig=null;");
        b.append("var cs=document.querySelectorAll('button,a,[role=button]');");
        b.append("for(var i=0;i<cs.length;i++){");
        b.append("  var t=(cs[i].textContent||'').trim();");
        b.append("  if(/登录|登陆|log\\s*in|sign\\s*in/i.test(t)){orig=cs[i];break;}");
        b.append("}");
        b.append("if(!orig)return;");
        // 原按钮半透明，不挡内容
        b.append("try{orig.style.opacity='0';orig.style.pointerEvents='none';}catch(e){}");
        // 找到或创建克隆
        b.append("var c=document.getElementById('__wb_mobile_login');");
        b.append("if(!c){");
        b.append("  c=orig.cloneNode(true);");
        b.append("  c.id='__wb_mobile_login';");
        // 克隆按钮用 fixed 定位到 layout 视口的右上角
        // 因 setInitialScale 不改变 layout 视口（仍 1280），fixed 的 top/right 是 layout 像素
        // 视觉上相当于在缩放后屏幕的右上角
        b.append("  c.style.cssText='position:fixed!important;");
        b.append("top:").append(Math.round(8 / scaleRatio)).append("px!important;");
        b.append("right:").append(Math.round(8 / scaleRatio)).append("px!important;");
        b.append("z-index:2147483647!important;");
        // 按钮内容因 scaleRatio 被缩小，反向放大
        b.append("transform:scale(").append(1.0f / scaleRatio).append(")!important;");
        b.append("transform-origin:top right!important;");
        b.append("width:auto!important;height:auto!important;min-width:0!important;min-height:0!important;");
        b.append("font-size:").append(14.0f / scaleRatio).append("px!important;line-height:normal!important;");
        b.append("padding:").append(8.0f / scaleRatio).append("px ").append(16.0f / scaleRatio).append("px!important;");
        b.append("border-radius:").append(20.0f / scaleRatio).append("px!important;");
        b.append("box-shadow:0 4px 16px rgba(0,0,0,.15)!important;");
        b.append("background:linear-gradient(135deg,#6366f1,#8b5cf6)!important;");
        b.append("color:#fff!important;font-weight:600!important;cursor:pointer!important;");
        b.append("display:inline-flex!important;align-items:center!important;justify-content:center!important;");
        b.append("-webkit-tap-highlight-color:transparent!important;';");
        // 点击转发到原按钮
        b.append("  c.addEventListener('click',function(e){");
        b.append("    e.stopPropagation();e.preventDefault();");
        b.append("    orig.dispatchEvent(new MouseEvent('click',{bubbles:true,cancelable:true}));");
        b.append("    orig.click();");
        b.append("  });");
        b.append("  document.body.appendChild(c);");
        b.append("}");
        b.append("}");
        b.append("setupLogin();");

        // 2. 把整个桌面内容平移，使居中对齐到手机屏幕中心
        // shift > 0 表示内容要向右移动（小屏手机，桌面中心 640 需要右移才能对齐手机视觉中心）
        // shift < 0 表示大屏，内容要向左移动
        b.append("function centerLayout(){");
        // 头部整体平移
        b.append("var h=document.querySelector('header')||document.querySelector('[class*=header]');");
        b.append("if(h){h.style.transform='translateX(").append(shift).append("px)!important';}");
        // 主内容区平移
        b.append("var m=null;var ss=['main.cloud-welcome__main','main','.cloud-welcome','#root','#__next','#root > div'];");
        b.append("for(var i=0;i<ss.length;i++){var x=document.querySelector(ss[i]);if(x){m=x;break;}}");
        b.append("if(m){m.style.transform='translateX(").append(shift).append("px)!important';}");
        // 左侧边栏
        b.append("var s=document.querySelector('[class*=sidebar]')||document.querySelector('[class*=Sidebar]');");
        b.append("if(s){s.style.transform='translateX(").append(shift).append("px)!important';}");
        // 场景切换标签（保持原样）
        // 输入框 / 代码编辑器保持居中
        b.append("var eds=document.querySelectorAll('textarea,[contenteditable=\"true\"],.editor,[class*=editor],[class*=prompt]');");
        b.append("for(var i=0;i<eds.length;i++){eds[i].style.margin='0 auto!important';}");
        b.append("}");
        b.append("centerLayout();");

        // 3. 持续监听 SPA 动态渲染
        b.append("var __mo=new MutationObserver(function(){setupLogin();centerLayout();});");
        b.append("__mo.observe(document.body,{childList:true,subtree:true,attributes:true});");
        b.append("setTimeout(function(){setupLogin();centerLayout();},1500);");
        b.append("setTimeout(function(){setupLogin();centerLayout();},4000);");
        b.append("})();");

        view.evaluateJavascript(b.toString(), null);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        webView.saveState(outState);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
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
