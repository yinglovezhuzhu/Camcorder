/*******************************************************************************
 * Copyright (C) ${year}.year The Android Open Source Project.
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
 *******************************************************************************/

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

    private int mMaxProgress = 100;
    private int mProgress = 0;
    private int mSplitWidth = MIN_SPLIT_WIDTH;
    private int mMinMask = 10;

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
    private int mLastSplitPosition = INVALID_POSITION;


    private RectF mRectF;
    private Paint mPaint;

    private Stack<Integer> mSplits = new Stack<Integer>();

    private long mUiThreadId;

    private boolean mConfirming = false;

    private boolean mShowCursor = true;

    private OnDeleteListener mDeleteListener;

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
        setMaxProgress(a.getInt(R.styleable.ProgressView_max, mMaxProgress));
        setProgress(a.getInt(R.styleable.ProgressView_progress, mProgress));
        setSplitWidth(a.getDimensionPixelSize(R.styleable.ProgressView_splitWidth, MIN_SPLIT_WIDTH));
        setMinMask(a.getInt(R.styleable.ProgressView_minMask, 0));
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
    public void setMaxProgress(int maxProgress) {
        this.mMaxProgress = maxProgress;
    }

    /**
     * Get max progress
     * @return
     */
    public int getMaxProgress() {
        return mMaxProgress;
    }

    /**
     * Set progress
     * @param progress
     */
    public synchronized void setProgress(int progress) {
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
        }
    }

    /**
     * Get current progress.
     * @return
     */
    public int getProgress() {
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
    public void setMinMask(int progress) {
        this.mMinMask = progress;
        refreshProgress();
    }

    /**
     * Get min mask position
     */
    public int getmMinMask() {
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
    public void pushSplit(int progress) {
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
    public int popSplit() {
        if(mSplits.empty()) {
            return 0;
        }
        Integer split = mSplits.pop();
        refreshProgress();
        return split == null ? 0 : split;
    }

    /**
     * Peek the last split position
     * @return the last split position
     */
    public int peekSplit() {
        if(mSplits.empty()) {
            return 0;
        }
        Integer split = mSplits.peek();
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
                int latest = INVALID_POSITION;
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
            int split = peekSplit();
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
        mRectF.top = getTop();
        mRectF.bottom = getBottom();
        if(mLastSplitPosition == INVALID_POSITION) {
            mPaint.setColor(mProgressColor);
            mRectF.left = getLeft();
            mRectF.right = mRectF.left + ((float)mProgress * width) / mMaxProgress;
            canvas.drawRect(mRectF, mPaint);
        } else {
            if(mLastSplitPosition > 0) {
                mPaint.setColor(mProgressColor);
                mRectF.left = getLeft();
                mRectF.right = mRectF.left + ((float)mLastSplitPosition * width) / mMaxProgress;
                canvas.drawRect(mRectF, mPaint);
            }
            mPaint.setColor(mDeleteComfirmColor);
            mRectF.left = ((float)mLastSplitPosition * width) / mMaxProgress;
            mRectF.right = mRectF.left + ((float)(mProgress - mLastSplitPosition) * width) / mMaxProgress;
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
            mRectF.right = ((float)mMinMask * getWidth()) / mMaxProgress;
            mRectF.left = mRectF.right - mSplitWidth;
        }
        canvas.drawRect(mRectF, mPaint);
    }

    private void drawSplits(Canvas canvas) {
        int width = getWidth();
        mPaint.setColor(mSplitColor);
        mRectF.top = 0f;
        mRectF.bottom = mRectF.top + getHeight();
        for(Integer split : mSplits) {
            if(split < mSplitWidth) {
                mRectF.left = 0;
                mRectF.right = mSplitWidth;
            } else {
                mRectF.right = ((float)split * width) / mMaxProgress;
                mRectF.left = mRectF.right - mSplitWidth;
            }
            canvas.drawRect(mRectF, mPaint);
        }
    }

    private void drawCursor(Canvas canvas) {
        int width = getWidth();
        mPaint.setColor(mCursorCurrentColor);
        mRectF.left = ((float)mProgress * width) / mMaxProgress;
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
     * callback {@link com.opensource.camcorder.widget.ProgressView.OnDeleteListener#onConfirm(int, int)}<br/>
     * and {@link com.opensource.camcorder.widget.ProgressView.OnDeleteListener#onDelete(int, int)}<br/>
     * But only {@link com.opensource.camcorder.widget.ProgressView.OnDeleteListener#onDelete(int, int)} callback<br/>
     * when delete with not confirm mode.
     */
    public static interface OnDeleteListener {

        /**
         * In confirm step
         * @param lastProgress
         * @param progress
         */
        public void onConfirm(int lastProgress, int progress);

        /**
         * Delete back
         * @param lastProgress
         * @param progress
         */
        public void onDelete(int lastProgress, int progress);
    }
//public ProgressView(Context context) {
//    super(context);
//    init(context);
//}
//
//    public ProgressView(Context paramContext, AttributeSet paramAttributeSet) {
//        super(paramContext, paramAttributeSet);
//        init(paramContext);
//
//    }
//
//    public ProgressView(Context paramContext, AttributeSet paramAttributeSet,
//                        int paramInt) {
//        super(paramContext, paramAttributeSet, paramInt);
//        init(paramContext);
//    }
//
//    private Paint progressPaint, firstPaint, threePaint,breakPaint;//三个颜色的画笔
//    private float firstWidth = 4f, threeWidth = 1f;//断点的宽度
//    private LinkedList<Integer> linkedList = new LinkedList<Integer>();
//    private float perPixel = 0l;
//    private float countRecorderTime = 8000;//总的录制时间
//
//    public void setTotalTime(float time){
//        countRecorderTime = time;
//    }
//
//    private void init(Context paramContext) {
//
//        progressPaint = new Paint();
//        firstPaint = new Paint();
//        threePaint = new Paint();
//        breakPaint = new Paint();
//
//        // 背景
//        setBackgroundColor(Color.parseColor("#19000000"));
//
//        // 主要进度的颜色
//        progressPaint.setStyle(Paint.Style.FILL);
//        progressPaint.setColor(Color.parseColor("#19e3cf"));
//
//        // 一闪一闪的黄色进度
//        firstPaint.setStyle(Paint.Style.FILL);
//        firstPaint.setColor(Color.parseColor("#ffcc42"));
//
//        // 3秒处的进度
//        threePaint.setStyle(Paint.Style.FILL);
//        threePaint.setColor(Color.parseColor("#12a899"));
//
//        breakPaint.setStyle(Paint.Style.FILL);
//        breakPaint.setColor(Color.parseColor("#000000"));
//
//        DisplayMetrics dm = new DisplayMetrics();
//        ((Activity)paramContext).getWindowManager().getDefaultDisplay().getMetrics(dm);
//        perPixel = dm.widthPixels/countRecorderTime;
//
//        perSecProgress = perPixel;
//
//    }
//
//    /**
//     * 绘制状态
//     * @author QD
//     *
//     */
//    public static enum State {
//        START(0x1),PAUSE(0x2);
//
//        static State mapIntToValue(final int stateInt) {
//            for (State value : State.values()) {
//                if (stateInt == value.getIntValue()) {
//                    return value;
//                }
//            }
//            return PAUSE;
//        }
//
//        private int mIntValue;
//
//        State(int intValue) {
//            mIntValue = intValue;
//        }
//
//        int getIntValue() {
//            return mIntValue;
//        }
//    }
//
//
//    private volatile State currentState = State.PAUSE;//当前状态
//    private boolean isVisible = true;//一闪一闪的黄色区域是否可见
//    private float countWidth = 0;//每次绘制完成，进度条的长度
//    private float perProgress = 0;//手指按下时，进度条每次增长的长度
//    private float perSecProgress = 0;//每毫秒对应的像素点
//    private long initTime;//绘制完成时的时间戳
//    private long drawFlashTime = 0;//闪动的黄色区域时间戳
//
//    protected void onDraw(Canvas canvas) {
//        super.onDraw(canvas);
//        long curTime = System.currentTimeMillis();
//        //Log.i("recorder", curTime  - initTime + "");
//        countWidth = 0;
//        //每次绘制都将队列中的断点的时间顺序，绘制出来
//        if(!linkedList.isEmpty()){
//            float frontTime = 0;
//            Iterator<Integer> iterator = linkedList.iterator();
//            while(iterator.hasNext()){
//                int time = iterator.next();
//                //求出本次绘制矩形的起点位置
//                float left = countWidth;
//                //求出本次绘制矩形的终点位置
//                countWidth += (time-frontTime)*perPixel;
//                //绘制进度条
//                canvas.drawRect(left, 0,countWidth,getMeasuredHeight(),progressPaint);
//                //绘制断点
//                canvas.drawRect(countWidth, 0,countWidth + threeWidth,getMeasuredHeight(),breakPaint);
//                countWidth += threeWidth;
//
//                frontTime = time;
//            }
//            //绘制三秒处的断点
//            if(linkedList.getLast() <= 3000)
//                canvas.drawRect(perPixel*3000, 0,perPixel*3000+threeWidth,getMeasuredHeight(),threePaint);
//        }else//绘制三秒处的断点
//            canvas.drawRect(perPixel*3000, 0,perPixel*3000+threeWidth,getMeasuredHeight(),threePaint);//绘制三秒处的矩形
//
//        //当手指按住屏幕时，进度条会增长
//        if(currentState == State.START){
//            perProgress += perSecProgress*(curTime - initTime );
//            if(countWidth + perProgress <= getMeasuredWidth())
//                canvas.drawRect(countWidth, 0,countWidth + perProgress,getMeasuredHeight(),progressPaint);
//            else
//                canvas.drawRect(countWidth, 0,getMeasuredWidth(),getMeasuredHeight(),progressPaint);
//        }
//        //绘制一闪一闪的黄色区域，每500ms闪动一次
//        if(drawFlashTime==0 || curTime - drawFlashTime >= 500){
//            isVisible = !isVisible;
//            drawFlashTime = System.currentTimeMillis();
//        }
//        if(isVisible){
//            if(currentState == State.START)
//                canvas.drawRect(countWidth + perProgress, 0,countWidth + firstWidth + perProgress,getMeasuredHeight(),firstPaint);
//            else
//                canvas.drawRect(countWidth, 0,countWidth + firstWidth,getMeasuredHeight(),firstPaint);
//        }
//        //结束绘制一闪一闪的黄色区域
//        initTime = System.currentTimeMillis();
//        invalidate();
//    }
//
//    /**
//     * 设置进度条的状态
//     * @param state
//     */
//    public void setCurrentState(State state){
//        currentState = state;
//        if(state == State.PAUSE)
//            perProgress = perSecProgress;
//    }
//
//    /**
//     * 手指抬起时，将时间点保存到队列中
//     * @param time:ms为单位
//     */
//    public void putProgressList(int time) {
//        linkedList.add(time);
//    }
}