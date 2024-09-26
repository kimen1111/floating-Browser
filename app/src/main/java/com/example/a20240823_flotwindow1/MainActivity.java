package com.example.a20240823_flotwindow1;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private ActivityResultLauncher<Intent> overlayPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button startFloatingWindowButton = findViewById(R.id.startFloatingWindowButton);

        // フローティングウィンドウを開始するためのボタンのクリックリスナー
        startFloatingWindowButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // オーバーレイ権限があるか確認し、権限がない場合はリクエスト
                if (Settings.canDrawOverlays(MainActivity.this)) {
                    startFloatingWindowService();
                } else {
                    askPermission();
                }
            }
        });

        // ActivityResultLauncher の設定
        overlayPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (Settings.canDrawOverlays(MainActivity.this)) {
                        startFloatingWindowService();
                    } else {
                        // 権限が拒否された場合のフィードバック
                        Toast.makeText(this, "フローティングウィンドウを表示するためにオーバーレイ権限が必要です", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    // 権限をリクエストするメソッド
    private void askPermission() {
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName()));
        overlayPermissionLauncher.launch(intent);
    }

    // フローティングウィンドウを開始するメソッド
    private void startFloatingWindowService() {
        startService(new Intent(MainActivity.this, FloatingWindowService.class));
        finish();
    }
}
