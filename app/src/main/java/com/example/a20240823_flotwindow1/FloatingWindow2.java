package com.example.a20240823_flotwindow1;

import android.content.Context;
import android.graphics.PixelFormat;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class FloatingWindow2 {
    private Context context;
    private WindowManager windowManager;
    private WebView webView;
    private View floatingView;

    public FloatingWindow2(Context context, WindowManager windowManager) {
        this.context = context;
        this.windowManager = windowManager;

        // フローティングウィンドウの初期化
        floatingView = LayoutInflater.from(context).inflate(R.layout.layout_floating_widget, null);
        webView = floatingView.findViewById(R.id.webview);

        // WebView設定
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webView.setWebViewClient(new WebViewClient());

        // フローティングウィンドウを表示
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                600,
                700,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                        WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.LEFT;
        params.x = 0;
        params.y = 800;
        windowManager.addView(floatingView, params);
    }

    public void loadUrl(String url) {
        webView.loadUrl(url);
    }
}
