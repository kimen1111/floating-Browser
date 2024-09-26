package com.example.a20240823_flotwindow1;

import android.app.Service;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.webkit.WebView;

public class FloatingWindowManager {

    private final Context context;
    private final WindowManager windowManager;
    private final View floatingView;
    private final WindowManager.LayoutParams params;
    private final Service service;
    private OnWindowEventListener eventListener;
    private WebView webView;
    private FrameLayout fullscreenContainer;
    private WebChromeClient.CustomViewCallback customViewCallback;
    private View customView;
    private boolean isReloading = false;  // リロード中かどうかを追跡するフラグ
    private Handler handler = new Handler();
    private float youtubePlaybackTime = 0;

    // YouTube動画の再生時間を取得
    private void saveYoutubePlaybackTime() {
        webView.evaluateJavascript(
                "(function() { return document.querySelector('video') ? document.querySelector('video').currentTime : 0; })();",
                time -> youtubePlaybackTime = Float.parseFloat(time));
    }

    // YouTube動画の再生時間を設定
    private void restoreYoutubePlaybackTime() {
        webView.evaluateJavascript(
                "(function() { if (document.querySelector('video')) { document.querySelector('video').currentTime = " + youtubePlaybackTime + "; } })();",
                null);
    }
    private void setFloatingWindowFocusable(boolean focusable) {
        if (focusable) {
            params.flags &= ~WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;  // フォーカス可能にする
            //params.x +=10;
        } else {
            params.flags |= WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;   // フォーカス不可能にする
            //params.x -=10;
        }
        windowManager.updateViewLayout(floatingView, params);
    }


    public FloatingWindowManager(Service service, WindowManager windowManager) {
        this.context = service;
        this.service = service;
        this.windowManager = windowManager;

        floatingView = LayoutInflater.from(context).inflate(R.layout.layout_floating_widget, null);

        // フローティングウィンドウのレイアウトパラメータ設定
        params = new WindowManager.LayoutParams(
                800,
                600,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                //WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                        WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH |
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN|
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 0;
        params.y = 50;















        // WebView の設定
        webView = floatingView.findViewById(R.id.webview);
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);  // JavaScript を有効にする
        // クッキーの有効化
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cookieManager.setAcceptThirdPartyCookies(webView, true);
        }
        // キャッシュの有効化
        // オフライン時にキャッシュを使う設定（オプション）
        webSettings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        webView.setWebViewClient(new WebViewClient());

        webSettings.setDomStorageEnabled(true);  // DOMストレージを有効にする
        // デスクトップモード用のUser-Agent設定
        //String newUserAgent = webSettings.getUserAgentString() + " Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36";
        //webSettings.setUserAgentString(newUserAgent);
        String newUserAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.114 Safari/537.36";
        //webSettings.setUserAgentString(newUserAgent);
        webSettings.setMediaPlaybackRequiresUserGesture(false);  // メディア再生を自動許可//アベマはこれで見れる
        //webSettings.setUseWideViewPort(true);  // デスクトップのように広いビューポートを使用
        //webSettings.setLoadWithOverviewMode(true);  // ページ全体を表示

        webSettings.setSupportZoom(true);  // ズームを有効化
        webSettings.setBuiltInZoomControls(true);  // ズームコントロールを表示
        webSettings.setDisplayZoomControls(false);  // ズームコントロールを非表示
        webSettings.setDefaultZoom(WebSettings.ZoomDensity.FAR);  // デフォルトのズーム倍率を設定



        webView.setFocusable(true);
        webView.setFocusableInTouchMode(true);
// currentUrl をクラス内で保持できるようにする
        String currentUrl = "";
        EditText urlTextbox = floatingView.findViewById(R.id.header_textbox);

        webView.setWebViewClient(new WebViewClient() {

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {

                // 特定のURLのみにUserAgent(PCモード)を適用する
                if (url.contains("https://abema.tv/")) {
                    String newUserAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.114 Safari/537.36";
                    webSettings.setUserAgentString(newUserAgent);
                    webSettings.setUseWideViewPort(true);
                    webSettings.setLoadWithOverviewMode(true);
                    // ページ幅に合わせるために初期スケールを設定しない
                    webView.setInitialScale(0);

                } else {
                    // 他のページではデフォルトのUserAgentを使用
                    webSettings.setUserAgentString(WebSettings.getDefaultUserAgent(context));
                }
                super.onPageStarted(view, url, favicon);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                restoreYoutubePlaybackTime();
                // WebView から現在の URL を取得して EditText に表示

                //String currentUrl = webView.getUrl();  // WebView から現在のURLを取得
                //urlTextbox.setHint(currentUrl);  // ヒントテキストに URL を表示

                webView.evaluateJavascript(
                        "(function() { " +
                                "var video = document.querySelector('video');" +
                                "if (video) { video.muted = false; video.play(); }" +
                                "})();", null);
            }

        });

        //game中にフォーカスを外す
        floatingView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_OUTSIDE) {
                    urlTextbox.setVisibility(View.GONE);
                    floatingView.findViewById(R.id.textbox_button).setVisibility(View.GONE);
                    setFloatingWindowFocusable(false);  // ウィンドウ外がタッチされたらフォーカスを外す
                }
                return false;
            }
        });






        //スクロールイベントによるページの更新
        webView.setOnTouchListener(new View.OnTouchListener() {
            private float initialY;
            private final float SWIPE_THRESHOLD = 100;  // スワイプの閾値
            private boolean isReloading = false;
            private Handler handler = new Handler();
            private Runnable longPressCheck;
            private boolean isLongPress = false;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                setFloatingWindowFocusable(true);

                urlTextbox.setText("");

                String currentUrl = webView.getUrl();  // WebView から現在のURLを取得
                urlTextbox.setHint(currentUrl);  // ヒントテキストに URL を表示



                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialY = event.getY();
                        isLongPress = false;

                        longPressCheck = new Runnable() {
                            @Override
                            public void run() {
                                isLongPress = true;
                            }
                        };
                        handler.postDelayed(longPressCheck, 220);  // 0.22秒後にチェック
                        break;

                    case MotionEvent.ACTION_MOVE:
                        float currentY = event.getY();
                        if (currentY - initialY > SWIPE_THRESHOLD && webView.getScrollY() == 0 && !isReloading && isLongPress) {
                            saveYoutubePlaybackTime();  // 再生時間を保存
                            isReloading = true;
                            webView.reload();  // ページをリロード

                            handler.postDelayed(() -> {
                                isReloading = false;
                                //restoreYoutubePlaybackTime();  // ページリロード後に再生時間を復元
                            }, 500);
                        }
                        break;

                    case MotionEvent.ACTION_UP:
                        handler.removeCallbacks(longPressCheck);  // タッチが離れた場合はチェックをリセット
                        break;
                }
                return false;
            }
        });
        Button textboxButton = floatingView.findViewById(R.id.textbox_button);

        textboxButton.setOnClickListener(v -> {
            urlTextbox.setHint("");
            urlTextbox.setText("");


            //urlTextbox.setVisibility(View.GONE); // VISIBLEならGONEにする
        });

        // テキストボックスにタッチした時の動作を設定
        urlTextbox.setOnTouchListener((v, event) -> {


            //searchTextbox.setVisibility(View.VISIBLE);


            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                String hint = urlTextbox.getHint().toString();
                // テキストボックスが空の場合、ヒントをテキストに設定

                urlTextbox.setText(hint);
                urlTextbox.setSelection(hint.length());  // カーソルを末尾に移動

            }
            return false;
        });


        urlTextbox.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_UP) {
                String enteredText = urlTextbox.getText().toString();

                // URLかどうか確認
                if (!enteredText.startsWith("http://") && !enteredText.startsWith("https://")) {
                    // URLでない場合はGoogle検索
                    enteredText = "https://www.google.co.jp/search?q=" + enteredText;
                }

                // WebViewでURLを読み込む
                webView.loadUrl(enteredText);
                return true;
            }
            return false;
        });









        //デバイスの戻るボタンを使う
        webView.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN) {
                if (webView.canGoBack()) {
                    webView.goBack();  // WebViewの履歴があれば戻る
                    return true;
                }
            }
            return false;
        });



        // WebChromeClientでフルスクリーン表示を管理
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                // フルスクリーンモードにする際の処理
                if (customView != null) {
                    callback.onCustomViewHidden();
                    return;
                }

                // フルスクリーン用のビューを作成
                fullscreenContainer = new FrameLayout(context);
                fullscreenContainer.addView(view, new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT));

                // 元のフローティングウィンドウのコンテンツを非表示にしてフルスクリーンビューを追加
                floatingView.findViewById(R.id.webview).setVisibility(View.GONE);  // 元のWebViewを隠す
                floatingView.findViewById(R.id.fullscreen_area).setVisibility(View.VISIBLE);  // フルスクリーンエリアを表示
                ((FrameLayout) floatingView.findViewById(R.id.fullscreen_area)).addView(fullscreenContainer);
                customView = view;
                customViewCallback = callback;

            }


            @Override
            public void onHideCustomView() {
                // フルスクリーン解除時の処理
                if (customView == null) {
                    return;
                }

                // フルスクリーンのビューを削除
                ((FrameLayout) floatingView.findViewById(R.id.fullscreen_area)).removeView(fullscreenContainer);
                fullscreenContainer = null;
                customView = null;
                customViewCallback.onCustomViewHidden();

                // 元のWebViewを再表示
                floatingView.findViewById(R.id.webview).setVisibility(View.VISIBLE);  // 元のWebViewを再表示
                floatingView.findViewById(R.id.fullscreen_area).setVisibility(View.GONE);  // フルスクリーンエリアを隠す
            }
        });



        // 任意のURLをロード
        //webView.loadUrl("https://www.google.co.jp/");
//"https://www.youtube.com/
// https://www.google.co.jp/
// https://www.bing.com/
        // フローティングウィンドウを表示
        windowManager.addView(floatingView, params);
        //ウィンドウを動かす
        View headerBackground = floatingView.findViewById(R.id.header_background);

        headerBackground.setOnTouchListener(new View.OnTouchListener() {
            private float initialX, initialY;
            private int initialTouchX, initialTouchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        // 初期位置を取得
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = (int) event.getRawX();
                        initialTouchY = (int) event.getRawY();
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        // ドラッグした位置に応じてウィンドウの位置を更新
                        params.x = (int) (initialX + (event.getRawX() - initialTouchX));
                        params.y = (int) (initialY + (event.getRawY() - initialTouchY));
                        windowManager.updateViewLayout(floatingView, params);
                        return true;
                }
                return false;
            }
        });





        setupMinimizeButton();
        setupResizeHandles();
        setupCloseButton();
        setupcircleButton();

    }

    // WebView の参照を取得するメソッド
    public WebView getWebView() {
        return webView;
    }
    //最小化前の位置を保存する変数を追加

    // 最小化ボタンの設定
    private void setupMinimizeButton() {
        ImageView minimizeButton = floatingView.findViewById(R.id.minimize_btn);
        minimizeButton.setOnClickListener(v -> {
            //floatingView.setVisibility(View.GONE);
            // フラグに FLAG_NOT_FOCUSABLE を追加して、フォーカスを外す
            params.flags |= WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
            params.x += 2500;
            // ウィンドウの位置を更新
            windowManager.updateViewLayout(floatingView, params);



            if (eventListener != null) {
                eventListener.onMinimize();
            }
        });
    }



    // ウィンドウのリサイズ用の設定
    private void setupResizeHandles() {
        View resizeHandleRight = floatingView.findViewById(R.id.resize_handle_right);
        View resizeHandleBottom = floatingView.findViewById(R.id.resize_handle_bottom);

        resizeHandleRight.setOnTouchListener(new View.OnTouchListener() {
            private int initialWidth;
            private float initialTouchX;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialWidth = params.width;
                        initialTouchX = event.getRawX();
                        floatingView.setBackgroundResource(R.drawable.floating_widget_background_resizing);
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        params.width = initialWidth + (int) (event.getRawX() - initialTouchX);
                        windowManager.updateViewLayout(floatingView, params);
                        return true;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        floatingView.setBackgroundResource(R.drawable.floating_widget_background);
                        return true;
                }
                return false;
            }
        });

        resizeHandleBottom.setOnTouchListener(new View.OnTouchListener() {
            private int initialHeight;
            private float initialTouchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialHeight = params.height;
                        initialTouchY = event.getRawY();
                        floatingView.setBackgroundResource(R.drawable.floating_widget_background_resizing);
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        params.height = initialHeight + (int) (event.getRawY() - initialTouchY);
                        windowManager.updateViewLayout(floatingView, params);
                        return true;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        floatingView.setBackgroundResource(R.drawable.floating_widget_background);
                        return true;
                }
                return false;
            }
        });
    }

    // クローズボタンの設定
    private void setupCloseButton() {
        ImageView closeButton = floatingView.findViewById(R.id.close_btn);
        closeButton.setOnClickListener(v -> {
            windowManager.removeView(floatingView);
            service.stopSelf();
        });
    }

    // 円ボタン 入力回復ボタン
    private void setupcircleButton() {
        ImageView circleButton = floatingView.findViewById(R.id.circle1);
        circleButton.setOnClickListener(v -> {
                    View headerTextbox = floatingView.findViewById(R.id.header_textbox);
                    if (headerTextbox.getVisibility() == View.VISIBLE) {
                        headerTextbox.setVisibility(View.GONE); // VISIBLEならGONEにする
                        floatingView.findViewById(R.id.textbox_button).setVisibility(View.GONE);

                    } else {
                        headerTextbox.setVisibility(View.VISIBLE); // GONEならVISIBLEにする
                        floatingView.findViewById(R.id.textbox_button).setVisibility(View.VISIBLE);
                    }});
    }



    // ウィンドウを再表示するメソッド
    public void showFloatingWindow() {
        if (floatingView != null && floatingView.getVisibility() == View.VISIBLE) {//== View.VISIBLE::floatingView が表示されていない
            //floatingView.setVisibility(View.VISIBLE);
            // フラグから FLAG_NOT_FOCUSABLE を取り除く
            params.flags &= ~WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;

            params.x -= 2500;
            // ウィンドウの位置を更新
            windowManager.updateViewLayout(floatingView, params);
        }
    }

    // ウィンドウを破棄するメソッド
    public void destroy() {
        if (floatingView != null) {
            windowManager.removeView(floatingView);
        }
        eventListener = null;
    }

    // イベントリスナーの設定
    public void setOnWindowEventListener(OnWindowEventListener listener) {
        this.eventListener = listener;
    }

    public interface OnWindowEventListener {
        void onMinimize();
    }

}
