package com.hellozh.stopwatch;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;

import java.util.Random;

/**
 * 色块跳变背景：整屏切成硬边方格（横向 3 块），每 100ms 成批换色，
 * 没有渐变、没有过渡，看起来就是持续快速闪现的色块。
 *
 * 色调约束（保证 80sp 粗体数字始终可读）：
 * 深色主题的方块全部取自"鲜艳但压得很暗"的颜色，最亮的一档对白字仍有 8.7:1 对比度；
 * 浅色主题的方块是高饱和的明快色，最暗的一档对黑字仍有 10.2:1。
 * 所以相邻色块之间色相差异极大、跳得很猛，计时数字却始终清晰。
 */
public class ToneBackgroundDrawable extends Drawable {

    /** 深色主题色块：色相鲜明、亮度统一压到很低。 */
    private static final int[] DARK_COLORS = {
            0xFF000000, // 纯黑
            0xFF1E3A8A, // 深蓝
            0xFF312E81, // 靛紫
            0xFF064E3B, // 墨绿
            0xFF7F1D1D, // 暗红
            0xFF78350F, // 褐金
            0xFF164E63, // 深青
            0xFF3B0764, // 深紫
            0xFF4C1D95, // 紫罗兰
            0xFF0F172A, // 石板
    };

    /**
     * 浅色主题色块：高饱和度的明快色。
     * 黑字压在它们上面最差也有 10:1 对比度（最深的 #FF9AA8 / #C3A6FF 约 10.2:1），
     * 因此可以跳得很花，数字依然清楚。
     */
    private static final int[] LIGHT_COLORS = {
            0xFFFFD84D, // 明黄
            0xFF7FD8FF, // 天蓝
            0xFFFF9AA8, // 珊瑚粉
            0xFFA8E6A1, // 嫩绿
            0xFFC3A6FF, // 薰衣草
            0xFFFFC08A, // 杏橙
            0xFF8FE3D0, // 薄荷
            0xFFFFB3E6, // 浅洋红
            0xFF9EC5FF, // 矢车菊
            0xFFD9F5C8, // 黄绿
            0xFFFFFFFF, // 纯白
            0xFFF0F0F0, // 中性浅灰
    };

    /** 横向切几块；纵向按屏幕比例自动算，保持方块接近正方形。 */
    private static final int COLUMNS = 3;
    /** 每次换色的间隔：100ms，一眼就能看到连续的硬跳。 */
    private static final long STEP_MS = 100L;
    /** 每隔多少步做一次整屏全换，制造更明显的"闪一下"。 */
    private static final int FULL_SHUFFLE_EVERY = 5;

    private final Paint mPaint = new Paint();
    private final Random mRandom = new Random();
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final Runnable mStep = new Runnable() {
        @Override
        public void run() {
            advance();
            if (mStarted && mMotionEnabled) mHandler.postDelayed(this, STEP_MS);
        }
    };

    private int[] mColors = DARK_COLORS;
    private boolean mDark = true;
    private boolean mMotionEnabled = true;
    private boolean mStarted = false;

    private int mWidth;
    private int mHeight;
    private int mRows = 1;
    private int[] mTiles = new int[0];
    private int mStepCount;

    public ToneBackgroundDrawable() {
        mPaint.setAntiAlias(false);
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setDither(false);
        shuffleAll();
    }

    /** 切换色调：true = 深色（鲜艳但压暗），false = 浅色（接近纯白的各种色偏）。 */
    public void setDark(boolean dark) {
        if (mDark == dark) return;
        mDark = dark;
        mColors = dark ? DARK_COLORS : LIGHT_COLORS;
        shuffleAll();
        invalidateSelf();
    }

    /** 动态开关。关闭后恢复原始的黑/白纯色背景，不再换色。 */
    public void setMotionEnabled(boolean enabled) {
        if (mMotionEnabled == enabled) return;
        mMotionEnabled = enabled;
        if (enabled) {
            shuffleAll();               // 立刻生成一帧新色块，避免先闪回旧画面
            if (mStarted) scheduleNext();
        } else {
            mHandler.removeCallbacks(mStep);
        }
        // 无论开关都要立即重绘：关闭时切回纯色，开启时立刻出现第一帧色块
        invalidateSelf();
    }

    public boolean isMotionEnabled() {
        return mMotionEnabled;
    }

    public void start() {
        mStarted = true;
        if (mMotionEnabled) scheduleNext();
    }

    public void stop() {
        mStarted = false;
        mHandler.removeCallbacks(mStep);
    }

    private void scheduleNext() {
        mHandler.removeCallbacks(mStep);
        mHandler.postDelayed(mStep, STEP_MS);
    }

    /** 推进一步：多数情况只换一部分格子，每隔几步整屏全换。 */
    private void advance() {
        mStepCount++;
        if (mStepCount % FULL_SHUFFLE_EVERY == 0 || mTiles.length == 0) {
            shuffleAll();
        } else {
            // 随机换掉 45% 左右的格子
            for (int i = 0; i < mTiles.length; i++) {
                if (mRandom.nextInt(100) < 45) mTiles[i] = pickColor();
            }
        }
        invalidateSelf();
    }

    private void shuffleAll() {
        ensureTiles();
        for (int i = 0; i < mTiles.length; i++) {
            mTiles[i] = pickColor();
        }
    }

    private int pickColor() {
        return mColors[mRandom.nextInt(mColors.length)];
    }

    /** 屏幕尺寸变化时重建格子数，保持方块接近正方形。 */
    private void ensureTiles() {
        if (mWidth <= 0 || mHeight <= 0) return;
        int rows = Math.max(1, Math.round((float) mHeight / ((float) mWidth / COLUMNS)));
        if (rows != mRows || mTiles.length != COLUMNS * rows) {
            mRows = rows;
            mTiles = new int[COLUMNS * mRows];
            for (int i = 0; i < mTiles.length; i++) mTiles[i] = pickColor();
        }
    }

    @Override
    public void draw(Canvas canvas) {
        Rect bounds = getBounds();
        if (bounds.isEmpty()) return;
        if (bounds.width() != mWidth || bounds.height() != mHeight) {
            mWidth = bounds.width();
            mHeight = bounds.height();
            ensureTiles();
        }

        canvas.save();
        canvas.translate(bounds.left, bounds.top);

        if (!mMotionEnabled) {
            // 关闭动态时恢复原始纯色背景：深色主题纯黑，浅色主题纯白
            mPaint.setColor(mDark ? 0xFF000000 : 0xFFFFFFFF);
            canvas.drawRect(0, 0, mWidth, mHeight, mPaint);
            canvas.restore();
            return;
        }

        float tileW = mWidth / (float) COLUMNS;
        float tileH = mHeight / (float) mRows;
        for (int r = 0; r < mRows; r++) {
            float top = Math.round(r * tileH);
            float bottom = (r == mRows - 1) ? mHeight : Math.round((r + 1) * tileH);
            for (int c = 0; c < COLUMNS; c++) {
                float left = Math.round(c * tileW);
                float right = (c == COLUMNS - 1) ? mWidth : Math.round((c + 1) * tileW);
                mPaint.setColor(mTiles[r * COLUMNS + c]);
                canvas.drawRect(left, top, right, bottom, mPaint);
            }
        }
        canvas.restore();
    }

    @Override
    public void setAlpha(int alpha) {
        // 色块明暗由调色板决定，忽略外部 alpha，避免破坏对比度
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
    }

    @Override
    public int getOpacity() {
        return PixelFormat.OPAQUE;
    }
}
