package com.openwebui;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private EditText urlEditText;
    private Button saveButton;
    private SharedPreferences sharedPreferences;

    private static final String PREFS_NAME = "OpenWebUIPrefs";
    private static final String URL_KEY = "instance_url";
    private static final int PERMISSION_REQUEST_CODE = 123;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 禁用内容绘制到系统窗口，确保 WebView 不会延伸到状态栏下方
        Window window = getWindow();
        WindowCompat.setDecorFitsSystemWindows(window, true);
        
        // 设置初始状态栏颜色为OWUI对应的颜色
        updateStatusBarColor();
        
        // 根据系统暗色模式设置状态栏图标颜色
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            WindowInsetsControllerCompat wic = new WindowInsetsControllerCompat(window, window.getDecorView());
            // 根据暗色模式设置图标明亮度：暗色模式下图标为浅色，亮色模式下图标为深色
            wic.setAppearanceLightStatusBars(!isDarkThemeEnabled()); 
        }
        
        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webview);
        urlEditText = findViewById(R.id.url_edit_text);
        saveButton = findViewById(R.id.save_button);

        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // 请求通知权限
        requestNotificationPermission();

        setupWebView();

        // Load saved URL if exists
        String savedUrl = sharedPreferences.getString(URL_KEY, "");
        if (!savedUrl.isEmpty()) {
            loadWebsite(savedUrl);
            urlEditText.setText(savedUrl);
            webView.setVisibility(View.VISIBLE);
            urlEditText.setVisibility(View.GONE);
            saveButton.setVisibility(View.GONE);
        } else {
            // 如果没有保存的URL，使用OWUI对应的颜色
            updateStatusBarColor();
        }

        saveButton.setOnClickListener(v -> {
            String inputUrl = urlEditText.getText().toString().trim();
            if (!inputUrl.isEmpty()) {
                // Ensure URL has proper protocol
                if (!inputUrl.startsWith("http://") && !inputUrl.startsWith("https://")) {
                    inputUrl = "https://" + inputUrl;
                }
                
                // Save the URL
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString(URL_KEY, inputUrl);
                editor.apply();

                loadWebsite(inputUrl);
                urlEditText.setVisibility(View.GONE);
                saveButton.setVisibility(View.GONE);
                webView.setVisibility(View.VISIBLE);
            }
        });
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) 
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, 
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 
                        PERMISSION_REQUEST_CODE);
            }
        }
    }

    private void setupWebView() {
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);
        webSettings.setSupportZoom(true);
        webSettings.setDefaultTextEncodingName("utf-8");
        
        // 设置允许通知和音频播放
        webSettings.setMediaPlaybackRequiresUserGesture(false); // 允许自动播放音频
        
        // 设置页面可见性策略
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);
        
        // 支持文件下载和访问
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setAllowUniversalAccessFromFileURLs(true); // 允许从文件URL访问
        
        // 启用暗色模式支持 - 让网页可以感知系统暗色模式
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            webSettings.setForceDark(isDarkThemeEnabled() ? WebSettings.FORCE_DARK_ON : WebSettings.FORCE_DARK_OFF);
        }
        
        // WebChromeClient用于处理权限请求
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onPermissionRequest(PermissionRequest request) {
                // 接受音频和视频捕获权限
                for (String permission : request.getResources()) {
                    if (permission.equals(PermissionRequest.RESOURCE_AUDIO_CAPTURE)
                            || permission.equals(PermissionRequest.RESOURCE_VIDEO_CAPTURE)) {
                        request.grant(new String[]{permission});
                        return;
                    }
                }
                // 对于其他权限，接受所有
                request.grant(request.getResources());
            }
        });
        
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                
                // 在页面开始加载前，注入CSS和JavaScript来通知系统暗色模式
                injectDarkModeScript();
                
                // 页面开始加载时，使用OWUI对应的颜色（因为网页还在加载中）
                updateStatusBarColor();
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                
                // 在页面加载完成后，再次注入暗色模式脚本
                injectDarkModeScript();
                
                // 确保页面被标记为可见，这样音频会播放而不是发送通知
                webView.evaluateJavascript(
                    "(function() {" +
                    "  // 确保页面始终被视为可见以触发前台行为" +
                    "  Object.defineProperty(document, 'visibilityState', {" +
                    "    get: function() { return 'visible'; }" +
                    "  });" +
                    "  Object.defineProperty(document, 'hidden', {" +
                    "    get: function() { return false; }" +
                    "  });" +
                    "  var event = new Event('visibilitychange');" +
                    "  document.dispatchEvent(event);" +
                    "  console.log('Page visibility set to visible');" +
                    "})()", null);
                
                // 页面加载完成后，继续使用OWUI对应的颜色（保持一致性）
                updateStatusBarColor();
            }
        });
    }

    private void injectDarkModeScript() {
        // 注入JavaScript来模拟prefers-color-scheme媒体查询并确保页面始终处于活动状态
        String jsScript = "javascript:(function() {" +
                "var mediaQueryList = {" +
                "  matches: " + (isDarkThemeEnabled() ? "true" : "false") + "," +
                "  media: '(prefers-color-scheme: " + (isDarkThemeEnabled() ? "dark" : "light") + ")'," +
                "  onchange: null," +
                "  addListener: function(listener) { /* no-op */ }," +
                "  removeListener: function(listener) { /* no-op */ }," +
                "  addEventListener: function(type, listener) { /* no-op */ }," +
                "  removeEventListener: function(type, listener) { /* no-op */ }," +
                "  dispatchEvent: function(event) { return true; }" +
                "};" +
                
                "if (!window.matchMedia) {" +
                "  window.matchMedia = function(query) {" +
                "    if (query.includes('prefers-color-scheme')) {" +
                "      return mediaQueryList;" +
                "    } else {" +
                "      return {" +
                "        matches: false," +
                "        media: query," +
                "        onchange: null," +
                "        addListener: function() {}," +
                "        removeListener: function() {}," +
                "        addEventListener: function() {}," +
                "        removeEventListener: function() {}," +
                "        dispatchEvent: function() { return true; }" +
                "      };" +
                "    }" +
                "  };" +
                "}" +
                
                // 设置documentElement类以匹配系统主题
                "document.documentElement.classList.remove('dark', 'light');" +
                "document.documentElement.classList.add('" + (isDarkThemeEnabled() ? "dark" : "light") + "');" +
                
                "var meta = document.querySelector('meta[name=\"theme-color\"]');" +
                "if (meta) {" +
                "  meta.setAttribute('content', '" + (isDarkThemeEnabled() ? "#171717" : "#ffffff") + "');" +
                "}" +
                
                // 确保页面始终被视为可见以触发前台行为（播放音频而不是发送通知）
                "Object.defineProperty(document, 'visibilityState', {" +
                "  get: function() { return 'visible'; }" +
                "});" +
                
                "Object.defineProperty(document, 'hidden', {" +
                "  get: function() { return false; }" +
                "});" +
                
                "var event = new Event('visibilitychange');" +
                "document.dispatchEvent(event);" +
                
                "console.log('Dark mode script injected: " + (isDarkThemeEnabled() ? "dark" : "light") + "');})()";
        
        webView.evaluateJavascript(jsScript, null);
    }

    private void updateStatusBarColor() {
        // 根据系统暗色模式设置状态栏颜色为OWUI对应的背景色
        Window window = getWindow();
        if (isDarkThemeEnabled()) {
            // OWUI暗色模式背景色 - 通常是 #000000 或 #171717
            window.setStatusBarColor(Color.parseColor("#171717"));
        } else {
            // OWUI亮色模式背景色 - 通常是 #ffffff
            window.setStatusBarColor(Color.parseColor("#ffffff"));
        }
    }

    private boolean isDarkThemeEnabled() {
        int nightModeFlags = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return nightModeFlags == Configuration.UI_MODE_NIGHT_YES;
    }

    private void loadWebsite(String url) {
        // 在加载URL之前注入暗色模式脚本
        injectDarkModeScript();
        // 更新状态栏颜色为OWUI对应的颜色
        updateStatusBarColor();
        webView.loadUrl(url);
    }
}