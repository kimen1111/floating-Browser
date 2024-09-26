package com.example.a20240823_flotwindow1;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class LauncherActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // オーバーレイパーミッションの確認と要求
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, 1234);
        } else {
            // 受け取ったインテントの処理
            handleIntent(getIntent());
        }

        // このアクティビティを閉じる
        finish();
    }

    // インテントの内容を処理して、FloatingWindowService に引き渡す
    private void handleIntent(Intent intent) {
        if (intent != null) {
            // テキスト選択からのインテント処理
            if (Intent.ACTION_PROCESS_TEXT.equals(intent.getAction())) {
                CharSequence selectedText = intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT);
                if (selectedText != null) {
                    // サービスを起動し、選択されたテキストを渡す
                    startFloatingWindowService(selectedText.toString());
                }
            }
            // 共有からのインテント処理
            else if (Intent.ACTION_SEND.equals(intent.getAction()) && "text/plain".equals(intent.getType())) {
                String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
                if (sharedText != null) {
                    // サービスを起動し、共有されたテキストを渡す
                    startFloatingWindowService(sharedText);
                }
            } else {
                // 通常の起動時は何も渡さずにサービスを起動
                startFloatingWindowService(null);
            }
        }
    }

    // フローティングウィンドウサービスを開始し、必要に応じてテキストを渡す
    private void startFloatingWindowService(String query) {
        Intent serviceIntent = new Intent(this, FloatingWindowService.class);
        if (query != null) {
            serviceIntent.putExtra("query", query);  // クエリを渡す
        }
        startService(serviceIntent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // パーミッションの結果を受け取った後に再度サービスを起動
        if (requestCode == 1234 && Settings.canDrawOverlays(this)) {
            handleIntent(getIntent());
        }

        // このアクティビティを閉じる
        finish();
    }
}
