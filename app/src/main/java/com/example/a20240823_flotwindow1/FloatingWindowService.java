package com.example.a20240823_flotwindow1;

import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.WindowManager;
import android.webkit.WebView;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FloatingWindowService extends Service {

    private FloatingWindowManager floatingWindowManager;
    private MinimizedIconManager minimizedIconManager;
    private WebView webView;
    private WindowManager windowManager;

    @Override
    public void onCreate() {
        super.onCreate();

        // オーバーレイパーミッションがあるか確認し、なければ要求
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            // パーミッションがない場合はサービスを停止
            stopSelf();
            return;
        }

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        // フローティングウィンドウとWebViewの初期化
        floatingWindowManager = new FloatingWindowManager(this, windowManager);
        minimizedIconManager = new MinimizedIconManager(this, windowManager);

        // WebView の取得
        webView = floatingWindowManager.getWebView();

        // イベントリスナーの設定（最小化イベントの処理）
        floatingWindowManager.setOnWindowEventListener(new FloatingWindowManager.OnWindowEventListener() {
            @Override
            public void onMinimize() {
                // ウィンドウが最小化されたら、アイコンを表示
                minimizedIconManager.showMinimizedIcon();
            }
        });

        // 最小化されたアイコンをクリックしたときの処理
        minimizedIconManager.setOnIconClickListener(() -> {
            // フローティングウィンドウを再表示する
            floatingWindowManager.showFloatingWindow();
        });



        // 初期状態では google.co.jp を表示
        webView.loadUrl("https://www.google.co.jp/");
        //https://www.youtube.com/
        //https://www.google.co.jp/

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // インテントの内容に応じてWebViewに表示する内容を処理
        if (intent != null) {
            String query = intent.getStringExtra("query");
            if (query != null) {
                if (isValidUrl(query)) {
                    webView.loadUrl(query);  // URLの場合はそのまま表示
                } else {

                    if(extractUrl(query)!=null){
                        webView.loadUrl(extractUrl(query));
                    }
                    else {
                        searchOnWeb(query);  // 検索クエリとしてGoogleで検索
                    }
                }
            }
        }
        return START_STICKY; // サービスが終了しても再起動するように設定
    }

    private void searchOnWeb(String query) {
        String searchUrl = "https://www.google.co.jp/search?q=" + Uri.encode(query);
        webView.loadUrl(searchUrl);
    }
    // URLを抽出するメソッド
    private String extractUrl(String text) {
        // 正規表現を使ってURL部分のみを抽出
        Pattern urlPattern = Pattern.compile("(https?://[\\w\\-._~:/?#@!$&'()*+,;=%]+)");
        Matcher matcher = urlPattern.matcher(text);
        if (matcher.find()) {
            String foundUrl = matcher.group(0);
            // `#:~:text` を含むURLは除外
            if (!foundUrl.contains("#:~:text")) {
                return foundUrl;  // 最初に見つかったURLを返す
                 }
        }
        return null;
    }

    private boolean isValidUrl(String text) {

        return !TextUtils.isEmpty(text) && (text.startsWith("http://") || text.startsWith("https://"));
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (floatingWindowManager != null) {
            floatingWindowManager.destroy();
        }
        if (minimizedIconManager != null) {
            minimizedIconManager.hideMinimizedIcon();
        }
    }

}
