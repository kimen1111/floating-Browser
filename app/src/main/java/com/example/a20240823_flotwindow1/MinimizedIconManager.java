package com.example.a20240823_flotwindow1;

import android.content.Context;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.ImageView;

public class MinimizedIconManager {

    private final Context context;
    private final WindowManager windowManager;
    private ImageView iconView;
    private final WindowManager.LayoutParams iconParams;
    private Handler handler = new Handler(Looper.getMainLooper());
    private float velocityX = 0;
    private float velocityY = 0;
    private boolean isFlinging = false;
    private OnIconClickListener clickListener;

    public MinimizedIconManager(Context context, WindowManager windowManager) {
        this.context = context;
        this.windowManager = windowManager;

        iconParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);

        iconParams.gravity = Gravity.TOP | Gravity.START;
        iconParams.x = 50;
        iconParams.y = 50;
    }

    public void showMinimizedIcon() {
        if (iconView == null) {
            iconView = new ImageView(context);
            iconView.setImageDrawable(context.getApplicationInfo().loadIcon(context.getPackageManager()));

            iconView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    keepWithinScreenBounds();
                    iconView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                }
            });
        }

        windowManager.addView(iconView, iconParams);

        iconView.setOnTouchListener(new View.OnTouchListener() {
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;
            private long touchStartTime;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = iconParams.x;
                        initialY = iconParams.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        touchStartTime = System.currentTimeMillis();
                        velocityX = 0;
                        velocityY = 0;
                        isFlinging = false;
                        handler.removeCallbacksAndMessages(null);
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        iconParams.x = initialX + (int) (event.getRawX() - initialTouchX);
                        iconParams.y = initialY + (int) (event.getRawY() - initialTouchY);
                        keepWithinScreenBounds();
                        windowManager.updateViewLayout(iconView, iconParams);
                        return true;
                    case MotionEvent.ACTION_UP:
                        long touchEndTime = System.currentTimeMillis();
                        long deltaTime = touchEndTime - touchStartTime;

                        if (deltaTime < 200 && Math.abs(event.getRawX() - initialTouchX) < 10 && Math.abs(event.getRawY() - initialTouchY) < 10) {
                            iconView.performClick();
                        } else {
                            velocityX = (event.getRawX() - initialTouchX) / deltaTime * 1000;
                            velocityY = (event.getRawY() - initialTouchY) / deltaTime * 1000;
                            startFlingAnimation();
                        }
                        return true;
                }
                return false;
            }
        });

        iconView.setOnClickListener(v -> {
            if (!isFlinging) {
                windowManager.removeView(iconView);
                if (clickListener != null) {
                    clickListener.onIconClick();
                }
            }
        });
    }

    private void startFlingAnimation() {
        isFlinging = true;
        final long duration = 500;
        final long startTime = System.currentTimeMillis();
        handler.post(new Runnable() {
            @Override
            public void run() {
                long currentTime = System.currentTimeMillis();
                float elapsedTime = currentTime - startTime;
                float progress = elapsedTime / duration;

                if (progress < 1.0) {
                    float deceleration = 1 - progress;

                    // 画面幅の取得方法を修正
                    Point screenSize = getScreenSize();
                    int screenWidth = screenSize.x;

                    if (iconParams.x <= 0 || iconParams.x >= screenWidth - iconView.getMeasuredWidth()) {
                        deceleration /= 10;
                    }

                    iconParams.x += velocityX * deceleration * 0.05;
                    iconParams.y += velocityY * deceleration * 0.05;
                    keepWithinScreenBounds();
                    windowManager.updateViewLayout(iconView, iconParams);

                    handler.postDelayed(this, 16);
                } else {
                    isFlinging = false;
                    velocityX = 0;
                    velocityY = 0;
                }
            }
        });
    }

    private void keepWithinScreenBounds() {
        Point screenSize = getScreenSize();

        int screenWidth = screenSize.x;
        int screenHeight = screenSize.y;
        int iconWidth = iconView.getMeasuredWidth();
        int iconHeight = iconView.getMeasuredHeight();

        if (iconParams.x < 0) iconParams.x = 0;
        if (iconParams.y < 0) iconParams.y = 0;
        if (iconParams.x > screenWidth - iconWidth) {
            iconParams.x = screenWidth - iconWidth;
        }
        if (iconParams.y > screenHeight - iconHeight) {
            iconParams.y = screenHeight - iconHeight;
        }
    }

    private Point getScreenSize() {
        Point screenSize = new Point();
        windowManager.getDefaultDisplay().getSize(screenSize);
        return screenSize;
    }

    public void hideMinimizedIcon() {
        if (iconView != null && iconView.getParent() != null) {
            windowManager.removeView(iconView);
            handler.removeCallbacksAndMessages(null);
        }
    }

    public void setOnIconClickListener(OnIconClickListener listener) {
        this.clickListener = listener;
    }

    public interface OnIconClickListener {
        void onIconClick();
    }

    public void destroy() {
        hideMinimizedIcon();
        clickListener = null;
        handler = null;
    }
}
