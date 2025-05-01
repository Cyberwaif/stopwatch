package com.hellozh.stopwatch;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import java.util.Locale;

public class StopwatchActivity extends Activity {

    private TextView mTextViewStopwatch;
    private Button mButtonStart;
    private Button mButtonStop;
    private Button mButtonReset;
    private Button mButtonInvert;

    private Handler mHandler = new Handler();
    private long mStartTime = 0L;
    private long mTimeInMilliseconds = 0L;
    private long mTimeSwapBuff = 0L;
    private long mUpdateTime = 0L;

    private boolean mIsRunning = false;
    private boolean mIsDarkTheme = true;

    private Runnable mRunnable = new Runnable() {

        public void run() {

            mTimeInMilliseconds = SystemClock.uptimeMillis() - mStartTime;

            mUpdateTime = mTimeSwapBuff + mTimeInMilliseconds;

            int secs = (int) (mUpdateTime / 1000);
            int mins = secs / 60 % 100;
            secs = secs % 60;
            int milliseconds = (int) (mUpdateTime % 1000);

            mTextViewStopwatch.setText(String.format(Locale.getDefault(), "%02d:%02d.%03d", mins, secs, milliseconds));

            mHandler.postDelayed(this, 10);
        }

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_stop_watch);

        mTextViewStopwatch = findViewById(R.id.text_view_stopwatch);
        mButtonStart = findViewById(R.id.button_start);
        mButtonStop = findViewById(R.id.button_stop);
        mButtonReset = findViewById(R.id.button_reset);
        mButtonInvert = findViewById(R.id.button_invert);

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
                mTextViewStopwatch.setText("00:00:000");
                mHandler.removeCallbacks(mRunnable);
            }
        });

        invertColors();

        mButtonInvert.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                invertColors();
            }
        });

    }

    private void invertColors() {
        if (mIsDarkTheme) {
            // Light theme
            getWindow().getDecorView().setBackgroundColor(0xFFFFFFFF);
            mTextViewStopwatch.setTextColor(0xFF000000);
            mButtonStart.setBackgroundColor(0xFFEEEEEE);
            mButtonStop.setBackgroundColor(0xFFEEEEEE);
            mButtonReset.setBackgroundColor(0xFFEEEEEE);
            mButtonInvert.setBackgroundColor(0xFFEEEEEE);
            mButtonStart.setTextColor(0xFF000000);
            mButtonStop.setTextColor(0xFF000000);
            mButtonReset.setTextColor(0xFF000000);
            mButtonInvert.setTextColor(0xFF000000);
        } else {
            // Dark theme
            getWindow().getDecorView().setBackgroundColor(0xFF000000);
            mTextViewStopwatch.setTextColor(0xFFFFFFFF);
            mButtonStart.setBackgroundColor(0xFF222222);
            mButtonStop.setBackgroundColor(0xFF222222);
            mButtonReset.setBackgroundColor(0xFF222222);
            mButtonInvert.setBackgroundColor(0xFF222222);
            mButtonStart.setTextColor(0xFFFFFFFF);
            mButtonStop.setTextColor(0xFFFFFFFF);
            mButtonReset.setTextColor(0xFFFFFFFF);
            mButtonInvert.setTextColor(0xFFFFFFFF);
        }
        mIsDarkTheme = !mIsDarkTheme;
    }

    // Consider overriding onPause and onResume to handle activity lifecycle

}
