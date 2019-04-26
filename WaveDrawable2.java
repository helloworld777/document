package com.idodh.donha.customview;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Xfermode;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.Log;
import android.view.Choreographer;
import android.view.animation.DecelerateInterpolator;

/**  波浪从左往右
 * Created by jing on 16-12-6.
 */

public class WaveDrawable2 extends Drawable implements Animatable {

    private static final float WAVE_AMPLITUDE_FACTOR = 0.15f;
    private static final float WAVE_SPEED_FACTOR = 0.02f;
    private Drawable mDrawable;
    private int mWidth, mHeight;
    private int mWaveAmplitude, mWaveLength, mWaveOffset, mWaveStep, mWaveLevel;
    private ValueAnimator mAnimator;
    private float mProgress = 0.3f;
    private Paint mPaint;
    private Bitmap mMask;
    private Matrix mMatrix = new Matrix();
    private boolean mRunning = false;
    private boolean mIndeterminate = false;

    private static final PorterDuffXfermode sXfermode = new PorterDuffXfermode(PorterDuff.Mode.DST_OUT);
    private static ColorFilter sGrayFilter = new ColorMatrixColorFilter(new float[]{
            1.5F, 1.5F, 1.5F, 0, -1,
            1.5F, 1.5F, 1.5F, 0, -1,
            1.5F, 1.5F, 1.5F, 0, -1,
            0,    0,    0,    1, 0

    });

    private ColorFilter mCurFilter = null;

    private Choreographer.FrameCallback mFrameCallback = new Choreographer.FrameCallback() {
        @Override
        public void doFrame(long l) {
            invalidateSelf();
            if (mRunning) {
                Choreographer.getInstance().postFrameCallback(this);
            }
        }
    };

    public WaveDrawable2(Drawable drawable) {
        init(drawable);
    }

    public WaveDrawable2(Context context, int imgRes) {
        Drawable drawable;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            drawable = context.getDrawable(imgRes);
        } else {
            drawable = context.getResources().getDrawable(imgRes);
        }

        init(drawable);
    }

    private void init(Drawable drawable) {
        mDrawable = drawable;
        mWidth = mDrawable.getIntrinsicWidth();
        mHeight = mDrawable.getIntrinsicHeight();
        mWaveAmplitude = Math.max(8, (int) (mWidth * WAVE_AMPLITUDE_FACTOR));
        mWaveLength = mHeight;
        mWaveStep = Math.max(1, (int) (mHeight* WAVE_SPEED_FACTOR));

        mMatrix.reset();
        mMask = createMask(mHeight, mWaveLength, mWaveAmplitude);
        mPaint = new Paint();
        mPaint.setFilterBitmap(false);
        mPaint.setColor(Color.BLACK);
        mPaint.setXfermode(sXfermode);

        mAnimator = ValueAnimator.ofFloat(0, 1);
        mAnimator.setInterpolator(new DecelerateInterpolator());
        mAnimator.setRepeatMode(ValueAnimator.RESTART);
        mAnimator.setRepeatCount(ValueAnimator.INFINITE);
        mAnimator.setDuration(5000);
        mAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                setProgress((float) valueAnimator.getAnimatedValue());
                if (!mRunning) {
                    invalidateSelf();
                }
            }
        });

        setProgress(0);
        start();
    }

    /**
     * Set wave move distance (in pixels) in very animation frame
     * @param step distance in pixels
     */
    public void setWaveSpeed(int step) {
        mWaveStep = Math.min(step, mWidth / 2);
    }

    /**
     * Set wave amplitude (in pixels)
     * @param amplitude
     */
    public void setWaveAmplitude(int amplitude) {
        amplitude = Math.max(0, Math.min(amplitude, mWidth / 2));
        if (mWaveAmplitude != amplitude) {
            mWaveAmplitude = amplitude;
            mMask = createMask(mHeight, mWaveLength, mWaveAmplitude);
            invalidateSelf();
        }
    }

    /**
     * Set wave length (in pixels)
     * @param length
     */
    public void setWaveLength(int length) {
        length = Math.max(8, Math.min(mWidth * 2, length));
        if (length != mWaveLength) {
            mWaveLength = length;
            mMask = createMask(mWidth, mWaveLength, mWaveAmplitude);
            invalidateSelf();
        }
    }

    @Override
    public void setBounds(int left, int top, int right, int bottom) {
        super.setBounds(left, top, right, bottom);
        mDrawable.setBounds(left, top, right, bottom);
    }

    @Override
    public int getIntrinsicHeight() {
        return mHeight;
    }

    @Override
    public int getIntrinsicWidth() {
        return mWidth;
    }

    @Override
    public void draw(Canvas canvas) {
        mDrawable.setColorFilter(sGrayFilter);
        mDrawable.draw(canvas);
        mDrawable.setColorFilter(mCurFilter);

        if (mProgress <= 0.001f) {
            return;
        }

        @SuppressLint("WrongConstant")
        int sc = canvas.saveLayer(0, 0, mWidth, mHeight, null,
                Canvas.MATRIX_SAVE_FLAG |
                        Canvas.CLIP_SAVE_FLAG |
                        Canvas.HAS_ALPHA_LAYER_SAVE_FLAG |
                        Canvas.FULL_COLOR_LAYER_SAVE_FLAG |
                        Canvas.CLIP_TO_LAYER_SAVE_FLAG);

        mDrawable.draw(canvas);

        if (mProgress >= 0.999f) {
            return;
        }

        mWaveOffset += mWaveStep;
        if (mWaveOffset > mWaveLength) {
            mWaveOffset -= mWaveLength;
        }
        if (mWaveLevel > 0) {
            mPaint.setColor(Color.TRANSPARENT);
            canvas.drawRect(mWaveLevel,0,mWidth,mHeight,mPaint);
            mPaint.setColor(Color.WHITE);

        }
        mMatrix.setTranslate(mWaveLevel-2*mWaveAmplitude,-mWaveOffset);
        canvas.drawBitmap(mMask, mMatrix, mPaint);
        mPaint.setColor(Color.WHITE);
        canvas.drawRect(mWaveLevel,0,mWidth,mHeight,mPaint);
        canvas.restoreToCount(sc);
    }

    @Override
    protected boolean onLevelChange(int level) {
        setProgress(level/100f);
        return true;
    }

    @Override
    public void setAlpha(int i) {
        mDrawable.setAlpha(i);
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        mCurFilter = colorFilter;
        invalidateSelf();
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    @Override
    public void start() {
        mRunning = true;
        Choreographer.getInstance().postFrameCallback(mFrameCallback);
    }

    @Override
    public void stop() {
        mRunning = false;
        Choreographer.getInstance().removeFrameCallback(mFrameCallback);
    }

    @Override
    public boolean isRunning() {
        return mRunning;
    }

    public boolean isIndeterminate() {
        return mIndeterminate;
    }

    public void setIndeterminate(boolean indeterminate) {
        mIndeterminate = indeterminate;
        if (mIndeterminate) {
            mAnimator.start();
        } else {
            mAnimator.cancel();
        }
    }

    private void setProgress(float progress) {
        mProgress = progress;
//        mWaveLevel = mWidth - (int)((mWidth + mWaveAmplitude * 2) * mProgress);
        mWaveLevel = (int)((mWidth + mWaveAmplitude * 2) * mProgress);
        invalidateSelf();
    }

    private Bitmap createMask(int width, int length, int amplitude) {

        final int count = (int) Math.ceil((width + length) / (float)length);

        Bitmap bm = Bitmap.createBitmap(amplitude * 2,length * count,  Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bm);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

        Path path = new Path();
        path.moveTo(amplitude, 0);
//        Log.i("bbbbb","开始点："+amplitude+",0   bm.getWidth()="+bm.getWidth());
        final float stepX = length / 4f;
        float x = 0;
        float y = 0;
        for (int i = 0; i < count * 2; i++) {
            y += stepX;
            path.quadTo(x, y, amplitude, y+stepX);
//            Log.i("bbbbb","控制点："+x+"，"+y+","+amplitude+","+(y+stepX)+"  bm.getHeight()="+bm.getHeight());
//            path.quadTo(y, x, amplitude, x+stepX);
            y += stepX;
            x = bm.getWidth() - x;
        }
        path.lineTo(bm.getWidth(), bm.getHeight());
        path.lineTo(bm.getWidth(), 0);
        path.close();

        c.drawPath(path, p);
        return bm;
    }
}