package com.hellozh.stopwatch;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.os.SystemClock;
import android.view.View;

import java.util.Random;

/**
 * 全屏跑动的小人：头部为 Lenna 人脸（正面、不旋转、不缩放变形），身体为火柴人，
 * 用于在另一台相机前测试人脸检测。start()/stop() 由生命周期驱动，setRunning() 由开关驱动。
 */
public class RunnersView extends View {

    private static final int COUNT = 4;
    private static final int[] SHIRTS = {0xFFE53935, 0xFF1E88E5, 0xFF43A047, 0xFFFB8C00};

    private static class Runner {
        float x, y;      // 头部左上角
        float vx, vy;    // px/s
        float phase;     // 步伐相位
        int shirt;
    }

    private final Bitmap mHead;
    private final Runner[] mRunners = new Runner[COUNT];
    private final Paint mBitmapPaint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG);
    private final Paint mLimb = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mBody = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF mDst = new RectF();
    private final Random mRandom = new Random();

    private boolean mDark;
    private boolean mStarted;
    private boolean mRunning = true;
    private boolean mInit;
    private long mLastTime;
    private float mHeadSize;

    private final Runnable mTick = new Runnable() {
        @Override
        public void run() {
            if (!mStarted || !mRunning) return;
            step();
            invalidate();
            postOnAnimation(this);
        }
    };

    public RunnersView(Context context) {
        this(context, null);
    }

    public RunnersView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mHead = BitmapFactory.decodeResource(getResources(), R.drawable.lenna_head);
        mLimb.setStyle(Paint.Style.STROKE);
        mLimb.setStrokeCap(Paint.Cap.ROUND);
        mBody.setStyle(Paint.Style.STROKE);
        mBody.setStrokeCap(Paint.Cap.ROUND);
        for (int i = 0; i < COUNT; i++) mRunners[i] = new Runner();
    }

    private void initRunners(int w, int h) {
        // 头越大越容易被检测；取宽度的 1/4，且不超过高度的 1/5
        mHeadSize = Math.min(w / 4f, h / 5f);
        float speedBase = w * 0.35f;
        for (int i = 0; i < COUNT; i++) {
            Runner r = mRunners[i];
            r.x = mRandom.nextFloat() * Math.max(1, w - mHeadSize);
            r.y = mRandom.nextFloat() * Math.max(1, h - mHeadSize * 3.2f);
            double a = mRandom.nextDouble() * Math.PI * 2;
            float s = speedBase * (0.7f + mRandom.nextFloat() * 0.6f);
            r.vx = (float) Math.cos(a) * s;
            r.vy = (float) Math.sin(a) * s * 0.6f;
            if (Math.abs(r.vx) < speedBase * 0.4f) r.vx = (r.vx < 0 ? -1 : 1) * speedBase * 0.4f;
            r.phase = mRandom.nextFloat() * 6.28f;
            r.shirt = SHIRTS[i % SHIRTS.length];
        }
        mInit = true;
    }

    private void step() {
        long now = SystemClock.uptimeMillis();
        float dt = Math.min(0.05f, (now - mLastTime) / 1000f);
        mLastTime = now;
        int w = getWidth(), h = getHeight();
        float bodyH = mHeadSize * 2.2f;
        for (Runner r : mRunners) {
            r.x += r.vx * dt;
            r.y += r.vy * dt;
            if (r.x < 0) { r.x = 0; r.vx = Math.abs(r.vx); }
            if (r.x > w - mHeadSize) { r.x = w - mHeadSize; r.vx = -Math.abs(r.vx); }
            if (r.y < 0) { r.y = 0; r.vy = Math.abs(r.vy); }
            if (r.y > h - mHeadSize - bodyH) { r.y = h - mHeadSize - bodyH; r.vy = -Math.abs(r.vy); }
            r.phase += dt * 12f;
        }
    }

    @Override
    protected void onDraw(Canvas c) {
        int w = getWidth(), h = getHeight();
        if (w == 0 || h == 0) return;
        if (!mInit) { initRunners(w, h); mLastTime = SystemClock.uptimeMillis(); }
        float stroke = mHeadSize * 0.09f;
        mLimb.setStrokeWidth(stroke);
        mBody.setStrokeWidth(stroke * 2.2f);
        for (Runner r : mRunners) {
            float cx = r.x + mHeadSize / 2f;
            float neckY = r.y + mHeadSize;
            float hipY = neckY + mHeadSize * 1.0f;
            float swing = (float) Math.sin(r.phase);
            float legLen = mHeadSize * 1.2f;
            float armLen = mHeadSize * 0.85f;
            float dir = r.vx >= 0 ? 1 : -1;
            // 四肢用与衣服同色的彩色（中等明度），避免与黑/白数字同色
            mLimb.setColor(r.shirt);
            // 腿
            c.drawLine(cx, hipY, cx + swing * mHeadSize * 0.6f * dir, hipY + legLen, mLimb);
            c.drawLine(cx, hipY, cx - swing * mHeadSize * 0.6f * dir, hipY + legLen, mLimb);
            // 手臂
            float shoulderY = neckY + mHeadSize * 0.15f;
            c.drawLine(cx, shoulderY, cx - swing * mHeadSize * 0.55f * dir, shoulderY + armLen, mLimb);
            c.drawLine(cx, shoulderY, cx + swing * mHeadSize * 0.55f * dir, shoulderY + armLen, mLimb);
            // 躯干
            mBody.setColor(r.shirt);
            c.drawLine(cx, neckY, cx, hipY, mBody);
            // 头（Lenna），保持正面、不旋转
            mDst.set(r.x, r.y, r.x + mHeadSize, r.y + mHeadSize);
            c.drawBitmap(mHead, null, mDst, mBitmapPaint);
        }
    }

    public void setDark(boolean dark) { mDark = dark; invalidate(); }

    public void setRunning(boolean running) {
        mRunning = running;
        setVisibility(running ? VISIBLE : INVISIBLE);
        removeCallbacks(mTick);
        if (running && mStarted) { mLastTime = SystemClock.uptimeMillis(); postOnAnimation(mTick); }
    }

    public boolean isRunningEnabled() { return mRunning; }

    public void start() {
        mStarted = true;
        removeCallbacks(mTick);
        if (mRunning) { mLastTime = SystemClock.uptimeMillis(); postOnAnimation(mTick); }
    }

    public void stop() {
        mStarted = false;
        removeCallbacks(mTick);
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        mInit = false; // 旋转/尺寸变化后重新布置
    }
}
