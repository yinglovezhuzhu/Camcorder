/*
 * Copyright (C) 2014 The Android Open Source Project.
 *
 *        yinglovezhuzhu@gmail.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.opensource.camcorder.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.View;

import com.opensource.camcorder.R;

import java.util.Stack;

/**
 * Use:
 * Created by yinglovezhuzhu@gmail.com on 2014-05-30.
 */
public class ProgressView extends View {

    private static final int MSG_UPDATE_CURSOR = 0;

    private static final int MIN_SPLIT_WIDTH = 2;
    private static final int INVALID_POSITION = -1;

    private float mMaxProgress = 100f;
    private float mProgress = 0f;
    private int mSplitWidth = MIN_SPLIT_WIDTH;
    private float mMinMask = 10f;

    /** Background color */
    private int mBackgroundColor = Color.TRANSPARENT;
    /** Progress an text color */
    private int mProgressColor = Color.argb(0xff, 0x06, 0xD2, 0x85);
    /** Mark color */
    private int mSplitColor = Color.RED;
    private int mMinMaskColor = Color.argb(0xff, 0x06, 0xD2, 0x85);

    private int mCursorHighColor = Color.argb(0xff, 0x00, 0xEE, 0xFF);
    private int mCursorDarkColor = Color.argb(0x00, 0xFF, 0xFF, 0xFF);
    private int mCursorCurrentColor = mCursorDarkColor;
    private boolean mCursorHigh = false;
    private long mCursorBlinkTime = 500L; //默认闪烁0.5秒
    private int mCusorWidth = 10;

    private int mDeleteComfirmColor = Color.RED;
    private float mLastSplitPosition = INVALID_POSITION;


    private RectF mRectF;
    private Paint mPaint;

    private Stack<Float> mSplits = new Stack<Float>();

    private long mUiThreadId;

    private boolean mConfirming = false;

    private boolean mShowCursor = true;

    private OnDeleteListener mDeleteListener;

    private OnProgressUpdateListener mOnProgressUpdateListener;

    @SuppressLint("HandlerLeak")
    private class MainHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_UPDATE_CURSOR:
                    mCursorCurrentColor = mCursorHigh ? mCursorDarkColor : mCursorHighColor;
                    mCursorHigh = !mCursorHigh;
                    postInvalidate();
                    mHandler.sendEmptyMessageDelayed(MSG_UPDATE_CURSOR, mCursorBlinkTime);
                    break;
                default:
                    break;
            }
            super.handleMessage(msg);
        }
    }
    private MainHandler mHandler = new MainHandler();

    public ProgressView(Context context) {
        this(context, null);
    }

    public ProgressView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ProgressView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mUiThreadId = Thread.currentThread().getId();
        initProgressBar();

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ProgressView, defStyleAttr, 0);

        setBackgroundColor(a.getColor(R.styleable.ProgressView_backgroundColor, mBackgroundColor));
        setProgressColor(a.getColor(R.styleable.ProgressView_progressColor, mProgressColor));
        setSplitColor(a.getColor(R.styleable.ProgressView_splitColor, mSplitColor));
        setMaxProgress(a.getFloat(R.styleable.ProgressView_max, mMaxProgress));
        setProgress(a.getFloat(R.styleable.ProgressView_progress, mProgress));
        setSplitWidth(a.getDimensionPixelSize(R.styleable.ProgressView_splitWidth, MIN_SPLIT_WIDTH));
        setMinMask(a.getFloat(R.styleable.ProgressView_minMask, 0f));
        setMinMaskColor(a.getColor(R.styleable.ProgressView_minMaskColor, mMinMaskColor));
        showCursor(a.getBoolean(R.styleable.ProgressView_showCursor, true));
        setCursorHighColor(a.getColor(R.styleable.ProgressView_cursorHightlightColor, mCursorHighColor));
        setCursorDarkColor(a.getColor(R.styleable.ProgressView_cursorDarkColor, mCursorDarkColor));
        setCursorWidth(a.getDimensionPixelOffset(R.styleable.ProgressView_cursorWidth, mCusorWidth));
        a.recycle();
        mHandler.sendEmptyMessageDelayed(MSG_UPDATE_CURSOR, mCursorBlinkTime);
    }

    /**
     * Set background color
     */
    public void setBackgroundColor(int color) {
        this.mBackgroundColor = color;
        refreshProgress();
    }

    public int getBackgroundColor(int color) {
        return mBackgroundColor;
    }

    /**
     * Set progress color
     * @param color
     */
    public void setProgressColor(int color) {
        this.mProgressColor = color;
        refreshProgress();
    }

    /**
     * Get progress color
     * @return
     */
    public int getProgressColor() {
        return mProgressColor;
    }

    /**
     * Set split stroke color
     * @param color
     */
    public void setSplitColor(int color) {
        this.mSplitColor = color;
        refreshProgress();
    }

    /**
     * Get split stroke color
     * @return
     */
    public int getSplitColor() {
        return mSplitColor;
    }

    /**
     * Set max progress
     * @param maxProgress
     */
    public void setMaxProgress(float maxProgress) {
        this.mMaxProgress = maxProgress;
    }

    /**
     * Get max progress
     * @return
     */
    public float getMaxProgress() {
        return mMaxProgress;
    }

    /**
     * Set progress
     * @param progress
     */
    public synchronized void setProgress(float progress) {
        if(progress < 0) {
            progress = 0;
        }
        if(progress > mMaxProgress) {
            progress = mMaxProgress;
        }
        if(mProgress != progress) {
            this.mProgress = progress;
            if(progress == 0) {
                clearSplits();
            } else {
                refreshProgress();
            }
            if(null != mOnProgressUpdateListener) {
                mOnProgressUpdateListener.onProgressUpdate(mMaxProgress, mProgress);
            }
        }
    }

    /**
     * Get current progress.
     * @return
     */
    public float getProgress() {
        return mProgress;
    }


    public void setSplitWidth(int width) {
        this.mSplitWidth = width;
        refreshProgress();
    }

    /**
     * Get split stoke width
     * @return
     */
    public int getSplitWidth() {
        return mSplitWidth;
    }

    /**
     * Set min mask position<br/>
     * <p/>The min mask position is a progress value
     * @param progress
     */
    public void setMinMask(float progress) {
        this.mMinMask = progress;
        refreshProgress();
    }

    /**
     * Get min mask position
     */
    public float getmMinMask() {
        return this.mMinMask;
    }

    /**
     * Set the color of min mask
     * @param color
     */
    public void setMinMaskColor(int color) {
        this.mMinMaskColor = color;
        refreshProgress();
    }

    /**
     * Get the color of min mask
     * @return
     */
    public int getMinMaskColor() {
        return mMinMaskColor;
    }

    /**
     * Add a split line<br/>
     * <p/> The split position is a progress value.
     * @param progress
     */
    public void pushSplit(float progress) {
        if(progress == 0 || mSplits.contains(progress)) {
            return;
        }
        mSplits.push(progress);
        refreshProgress();
    }

    /**
     * Delete the last split position.
     * @return the las split position.
     */
    public float popSplit() {
        if(mSplits.empty()) {
            return 0;
        }
        Float split = mSplits.pop();
        refreshProgress();
        return split == null ? 0 : split;
    }

    /**
     * Peek the last split position
     * @return the last split position
     */
    public float peekSplit() {
        if(mSplits.isEmpty()) {
            return 0f;
        }
        Float split = mSplits.peek();
        return split == null ? 0 : split;
    }

    /**
     * Empty split positions
     */
    public void clearSplits() {
        mSplits.clear();
        refreshProgress();
    }

    /**
     * Exit confirm state
     */
    public void clearConfirm() {
        if(mConfirming) {
            mLastSplitPosition = INVALID_POSITION;
            mConfirming = false;
        }
    }

    /**
     * Delete back to last split
     * @param isConfirm
     */
    public void deleteBack(boolean isConfirm) {
        if(mProgress <= 0) {
            return;
        }
        if(isConfirm) {
            if(mConfirming) {
                if(mProgress < mMaxProgress) {
                    popSplit();
                }
                if(mDeleteListener != null) {
                    mDeleteListener.onDelete(mLastSplitPosition, mProgress);
                }
                setProgress(mLastSplitPosition);
                mLastSplitPosition = INVALID_POSITION;
                mConfirming = false;
            } else {
                float latest = INVALID_POSITION;
                if(mProgress < mMaxProgress) {
                    latest = popSplit();
                }
                mLastSplitPosition = peekSplit();
                if(latest != INVALID_POSITION) {
                    mSplits.push(latest);
                }
                if(mDeleteListener != null) {
                    mDeleteListener.onConfirm(mLastSplitPosition, mProgress);
                }
                refreshProgress();
                mConfirming = true;
            }
        } else {
            if(mProgress < mMaxProgress) {
                popSplit();
            }
            float split = peekSplit();
            if(mDeleteListener != null) {
                mDeleteListener.onDelete(split, mProgress);
            }
            setProgress(split);
        }
    }

    /**
     * Delete back to las split
     * @param isConfirm
     * @param l
     */
    public void deleteBack(boolean isConfirm, OnDeleteListener l) {
        setOnDeleteListener(l);
        deleteBack(isConfirm);
    }

    /**
     * Set show cursor or not.
     * @param isShow
     */
    public void showCursor(boolean isShow) {
        this.mShowCursor = isShow;
        postInvalidate();
    }

    /**
     * Set cursor high light color
     * @param color
     */
    public void setCursorHighColor(int color) {
        this.mCursorHighColor = color;
        postInvalidate();
    }

    /**
     * Set cursor dark color
     * @param color
     */
    public void setCursorDarkColor(int color) {
        this.mCursorDarkColor = color;
        postInvalidate();
    }

    public void setCursorWidth(int width) {
        this.mCusorWidth = width;
        postInvalidate();
    }

    /**
     * Set the listener when delete back.
     * @param l
     */
    public void setOnDeleteListener(OnDeleteListener l) {
        this.mDeleteListener = l;
    }

    /**
     * Set the listener to listen progress updating.
     * @param l
     */
    public void setOnProgressUpdateListener(OnProgressUpdateListener l) {
        this.mOnProgressUpdateListener = l;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.drawColor(mBackgroundColor); //Draw background color.

        drawMinMask(canvas);

        drawProgress(canvas);

        drawSplits(canvas);

        if(mShowCursor) {
            drawCursor(canvas);
        }
    }

    private void initProgressBar() {
        mPaint = new Paint();
        mPaint.setAntiAlias(true);

        mRectF = new RectF();
    }

    private void drawProgress(Canvas canvas) {
        int width = getWidth();
        mRectF.top = 0f;
        mRectF.bottom = mRectF.top + getHeight();
        if(mLastSplitPosition == INVALID_POSITION) {
            mPaint.setColor(mProgressColor);
            mRectF.left = getLeft();
            mRectF.right = mRectF.left + (mProgress * width) / mMaxProgress;
            canvas.drawRect(mRectF, mPaint);
        } else {
            if(mLastSplitPosition > 0) {
                mPaint.setColor(mProgressColor);
                mRectF.left = getLeft();
                mRectF.right = mRectF.left + (mLastSplitPosition * width) / mMaxProgress;
                canvas.drawRect(mRectF, mPaint);
            }
            mPaint.setColor(mDeleteComfirmColor);
            mRectF.left = (mLastSplitPosition * width) / mMaxProgress;
            mRectF.right = mRectF.left + ((mProgress - mLastSplitPosition) * width) / mMaxProgress;
            canvas.drawRect(mRectF, mPaint);
        }
    }

    private void drawMinMask(Canvas canvas) {
        mPaint.setColor(mMinMaskColor);
        mRectF.top = 0f;
        mRectF.bottom = mRectF.top + getHeight();
        if(mMinMask < mSplitWidth) {
            mRectF.left = 0;
            mRectF.right = mSplitWidth;
        } else {
            mRectF.right = (mMinMask * getWidth()) / mMaxProgress;
            mRectF.left = mRectF.right - mSplitWidth;
        }
        canvas.drawRect(mRectF, mPaint);
    }

    private void drawSplits(Canvas canvas) {
        int width = getWidth();
        mPaint.setColor(mSplitColor);
        mRectF.top = 0f;
        mRectF.bottom = mRectF.top + getHeight();
        for(Float split : mSplits) {
            if(split < mSplitWidth) {
                mRectF.left = 0;
                mRectF.right = mSplitWidth;
            } else {
                mRectF.right = (split * width) / mMaxProgress;
                mRectF.left = mRectF.right - mSplitWidth;
            }
            canvas.drawRect(mRectF, mPaint);
        }
    }

    private void drawCursor(Canvas canvas) {
        int width = getWidth();
        mPaint.setColor(mCursorCurrentColor);
        mRectF.left = (mProgress * width) / mMaxProgress;
        mRectF.top = 0f;
        mRectF.bottom = mRectF.top + getHeight();
        mRectF.right = mRectF.left + mCusorWidth;
        canvas.drawRect(mRectF, mPaint);
    }

    private synchronized void refreshProgress() {
        if (mUiThreadId == Thread.currentThread().getId()) {
            this.postInvalidate();
        }
    }

    /**
     * The listener to listen delete back<br/>
     * <p/>when sue {@link #deleteBack(boolean)} with value true, it would <br/>
     * callback {@link com.opensource.camcorder.widget.ProgressView.OnDeleteListener#onConfirm(float, float)}<br/>
     * and {@link com.opensource.camcorder.widget.ProgressView.OnDeleteListener#onDelete(float, float)}<br/>
     * But only {@link com.opensource.camcorder.widget.ProgressView.OnDeleteListener#onDelete(float, float)} callback<br/>
     * when delete with not confirm mode.
     */
    public static interface OnDeleteListener {

        /**
         * In confirm step
         * @param lastProgress
         * @param progress
         */
        public void onConfirm(float lastProgress, float progress);

        /**
         * Delete back
         * @param lastProgress
         * @param progress
         */
        public void onDelete(float lastProgress, float progress);
    }

    public static interface OnProgressUpdateListener {

        public void onProgressUpdate(float max, float progress);
    }
}