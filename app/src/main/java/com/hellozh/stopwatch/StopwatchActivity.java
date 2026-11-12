package com.hellozh.stopwatch;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;

import android.os.Handler;
import android.os.SystemClock;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import java.util.Locale;

public class StopwatchActivity extends Activity {

    private TextView mTextViewStopwatch;
    private Button mButtonStart;
    private Button mButtonStop;
    private Button mButtonReset;
    private Button mButtonInvert;
    private Switch mSwitchRunners;
    private RunnersView mRunners;

    private Handler mHandler = new Handler();
    private long mStartTime = 0L;
    private long mTimeInMilliseconds = 0L;
    private long mTimeSwapBuff = 0L;
    private long mUpdateTime = 0L;

    private boolean mIsRunning = false;
    private boolean mIsDarkTheme = false;


    private String formatElapsed(long millis) {
        long totalSecs = millis / 1000;
        long mins = totalSecs / 60;
        long secs = totalSecs % 60;
        long ms = millis % 1000;
        return String.format(Locale.getDefault(), "%02d:%02d.%03d", mins, secs, ms);
    }

    private Runnable mRunnable = new Runnable() {

        public void run() {

            mTimeInMilliseconds = SystemClock.uptimeMillis() - mStartTime;

            mUpdateTime = mTimeSwapBuff + mTimeInMilliseconds;

            mTextViewStopwatch.setText(formatElapsed(mUpdateTime));

            mHandler.postDelayed(this, 10);
        }

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_stop_watch);

        // 必须在 setContentView 之后：API 30+ 的 getInsetsController() 依赖已创建的 DecorView，
        // 提前调用会抛 NullPointerException
        enterFullscreen();

        mTextViewStopwatch = findViewById(R.id.text_view_stopwatch);
        mButtonStart = findViewById(R.id.button_start);
        mButtonStop = findViewById(R.id.button_stop);
        mButtonReset = findViewById(R.id.button_reset);
        mButtonInvert = findViewById(R.id.button_invert);
        mSwitchRunners = findViewById(R.id.switch_runners);
        mRunners = findViewById(R.id.runners_view);


        mButtonStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!mIsRunning) {
                    mStartTime = SystemClock.uptimeMillis();
                    mHandler.postDelayed(mRunnable, 10);
                    mIsRunning = true;
                }
            }
        });

        mButtonStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mIsRunning) {
                    mTimeSwapBuff += mTimeInMilliseconds;
                    mHandler.removeCallbacks(mRunnable);
                    mIsRunning = false;
                }
            }
        });

        mButtonReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mStartTime = 0L;
                mTimeInMilliseconds = 0L;
                mTimeSwapBuff = 0L;
                mUpdateTime = 0L;
                mIsRunning = false;
                mTextViewStopwatch.setText(formatElapsed(0));
                mHandler.removeCallbacks(mRunnable);
            }
        });

        applyTheme();

        mButtonInvert.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mIsDarkTheme = !mIsDarkTheme;
                applyTheme();
            }
        });

        mSwitchRunners.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton b, boolean checked) {
                mRunners.setRunning(checked);
            }
        });
    }

    private void enterFullscreen() {
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false);
            WindowInsetsController controller = window.getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            window.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        }
        // 兜底：部分 ROM 在 DecorView 尚未 attach 时会让 getInsetsController() 返回 null，
        // 靠 window flags 至少隐藏状态栏
        window.getDecorView().setSystemUiVisibility(
                window.getDecorView().getSystemUiVisibility()
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        // 从通知栏/手势下滑后重新回到全屏
        if (hasFocus) enterFullscreen();
    }

    private void applyTheme() {
        findViewById(android.R.id.content).setBackgroundColor(mIsDarkTheme ? 0xFF000000 : 0xFFFFFFFF);
        mRunners.setDark(mIsDarkTheme);

        int textColor = mIsDarkTheme ? 0xFFFFFFFF : 0xFF000000;
        int buttonBackground = mIsDarkTheme ? 0xFF222222 : 0xFFEEEEEE;

        mTextViewStopwatch.setTextColor(textColor);
        // 数字外加反色光晕（与背景同色），避免被小人遮挡/干扰；多次叠加使光晕更实
        int haloColor = mIsDarkTheme ? 0xFF000000 : 0xFFFFFFFF;
        mTextViewStopwatch.setShadowLayer(24f, 0f, 0f, haloColor);
        mButtonStart.setBackgroundColor(buttonBackground);
        mButtonStop.setBackgroundColor(buttonBackground);
        mButtonReset.setBackgroundColor(buttonBackground);
        mButtonInvert.setBackgroundColor(buttonBackground);
        mButtonStart.setTextColor(textColor);
        mButtonStop.setTextColor(textColor);
        mButtonReset.setTextColor(textColor);
        mButtonInvert.setTextColor(textColor);
        mSwitchRunners.setTextColor(textColor);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mRunners != null) mRunners.start();
        // 计时基于 uptimeMillis，后台期间的时间不会丢失；恢复时重建刷新循环即可
        if (mIsRunning) {
            mHandler.removeCallbacks(mRunnable);
            mHandler.post(mRunnable);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // 后台时停止 10ms 刷新，避免无谓的 CPU 占用
        mHandler.removeCallbacks(mRunnable);
        if (mRunners != null) mRunners.stop();
    }
}
