package com.example.a20240823_flotwindow1;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.core.app.NotificationCompat;

public class BackgroundWebViewService extends Service {

    private WebView webView;
    private static final String CHANNEL_ID = "WebViewServiceChannel";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // フォアグラウンドサービスの開始
        createNotificationChannel();
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("WebView Background Service")
                .setContentText("WebView is running in the background")
                .setSmallIcon(R.drawable.ic_close2)  // 任意のアイコン
                .build();

        // フォアグラウンドサービスとしてサービスを開始
        startForeground(1, notification);

        // WebViewの初期化
        webView = new WebView(this);
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);  // JavaScript を有効化
        webView.setWebViewClient(new WebViewClient());

        // 任意のURLをバックグラウンドでロード
        webView.loadUrl("https://www.youtube.com");

        return START_STICKY;  // サービスを継続的に実行
    }

    @Override
    public void onDestroy() {
        // WebViewの停止処理
        if (webView != null) {
            webView.stopLoading();
            webView.destroy();  // メモリリークを防ぐためにWebViewを破棄
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;  // バインドは不要
    }

    // 通知チャネルを作成
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "WebView Background Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }
}
