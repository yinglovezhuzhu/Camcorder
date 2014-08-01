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

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;


/**
 * Use:
 * Created by yinglovezhuzhu@gmail.com on 2014-08-01.
 */
public class FocusView extends View {

    private static final int STATE_IDLE     = 0;
    private static final int STATE_FOCUSING = 1;
    private static final int STATE_SUCCESS  = 2;
    private static final int STATE_FAILED   = 3;

    private static final int MSG_UPDATE_DRAW = 0;

    private Paint mPaint;
    private LocalHandler mHandler = new LocalHandler();
    private int mNormalColor = Color.argb(0xFF, 0xFF, 0xFF, 0xFF);
    private int mSuccessColor = Color.argb(0xFF, 0x00, 0xFF, 0x00);
    private int mFailedColor = Color.argb(0xFF, 0xFF, 0x00, 0x00);
    private float mStrokeWidth = 3.0f;
    private int mMinRadius = 50;
    private int mMaxRadius = 80;
    private int mCurrentRadius = 80 - (int) mStrokeWidth;
    private PointF mCenter = new PointF(mMaxRadius, mMaxRadius);

    private int mState = STATE_IDLE;

    private boolean mEnabled = true;

    private boolean mAutoDismiss = false;

    private OnLongTouchListener mLongTouchListener;

    private OnFocusListener mOnFocusListener;


    public FocusView(Context context) {
        super(context);
        init();
    }

    public FocusView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public FocusView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        switch (mState) {
            case STATE_IDLE:    //STATE_IDLE, draw nothing.
                return;
            case STATE_FOCUSING://STATE_FOCUSING
                mPaint.setColor(mNormalColor);
                break;
            case STATE_SUCCESS://STATE_SUCCESS
                mPaint.setColor(mSuccessColor);
                break;
            case STATE_FAILED://STATE_FAILED
                mPaint.setColor(mFailedColor);
                break;
            default:
                mPaint.setColor(mNormalColor);
                break;
        }
        canvas.drawCircle(mCenter.x, mCenter.y, mCurrentRadius, mPaint);
        super.onDraw(canvas);
    }

    private long mDownTime = 0L;
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
        if(mEnabled && mState == STATE_IDLE) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    mDownTime = System.currentTimeMillis();
                    mCenter.x = event.getX();
                    mCenter.y = event.getY();
                    break;
                case MotionEvent.ACTION_MOVE:
                    break;
                case MotionEvent.ACTION_UP:
                    if(mCenter.x - event.getX() < 10f && mCenter.y - event.getY() < 10f) {
                        if(System.currentTimeMillis() - mDownTime > 500) {
                            if(null != mLongTouchListener) {
                                mLongTouchListener.onLongTouch(FocusView.this);
                            }
                        } else {
                            fixCenterPoint();
                            mCurrentRadius = mMaxRadius;
                            mState = STATE_FOCUSING;
                            if(null != mOnFocusListener) {
                                mOnFocusListener.onFocus(FocusView.this, mCenter.x, mCenter.y);
                            }
                            invalidate();
                            mHandler.sendEmptyMessageDelayed(MSG_UPDATE_DRAW, 20);
                        }
                    }
                    break;
                default:
                    break;
            }
            return true;
        }
        return super.onTouchEvent(event);
    }

    /**
     * Enable or disable this FocusView <br/>
     * <p/>If the FocusView was disabled, it won't show any thing when touch it.<br/>
     * @param enabled
     */
    public void setEnabled(boolean enabled) {
        this.mEnabled = enabled;
        mState = STATE_IDLE;
        invalidate();
    }



    /**
     * Get the state of this FocusView
     * @return
     */
    public boolean isEnabled() {
        return mEnabled;
    }

    public void setAutoDismiss(boolean autoDismiss) {
        this.mAutoDismiss = autoDismiss;
    }

    public boolean isAutoDismiss() {
        return mAutoDismiss;
    }

    /**
     * Focus success <br/>
     * <p/>
     *
     * <br/>
     * @see {@link #focusFailed()}
     */
    public void focusSuccessed() {
        if(!mEnabled) {
            return;
        }
        mState = STATE_SUCCESS;
        if(mCurrentRadius <= mMinRadius) {
            invalidate();
            mHandler.sendEmptyMessage(MSG_UPDATE_DRAW);
        }
    }

    /**
     * Focus failed <br/>
     *
     * <br/>
     * @see {@link #focusSuccessed()} )}
     */
    public void focusFailed() {
        if(!mEnabled) {
            return;
        }
        mState = STATE_FAILED;
        if(mCurrentRadius <= mMinRadius) {
            invalidate();
            mHandler.sendEmptyMessage(MSG_UPDATE_DRAW);
        }
    }

    public void setOnLongTouchListener(OnLongTouchListener l) {
        this.mLongTouchListener = l;
    }

    public void setOnFocusListener(OnFocusListener l) {
        this.mOnFocusListener = l;
    }

    private void init() {
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setStrokeWidth(mStrokeWidth);
        mPaint.setStyle(Paint.Style.STROKE);

    }

    /**
     * 更正圆心点，保证能够绘制整个园
     */
    private void fixCenterPoint() {
        mCenter.x = mCenter.x < mMaxRadius + mStrokeWidth ? mMaxRadius + mStrokeWidth : mCenter.x;
        mCenter.x = mCenter.x > getWidth() - mMaxRadius - mStrokeWidth ? getWidth() - mMaxRadius - mStrokeWidth: mCenter.x;
        mCenter.y = mCenter.y < mMaxRadius + mStrokeWidth ? mMaxRadius + mStrokeWidth : mCenter.y;
        mCenter.y = mCenter.y > getHeight() - mMaxRadius - mStrokeWidth ? getHeight() - mMaxRadius - mStrokeWidth : mCenter.y;
    }

    private class LocalHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_UPDATE_DRAW:
                    if(mCurrentRadius > mMinRadius) {
                        mCurrentRadius -= 2;
                        invalidate();
                        mHandler.sendEmptyMessageDelayed(MSG_UPDATE_DRAW, 20);
                    } else {
                        if(mState == STATE_SUCCESS || mState == STATE_FAILED || mAutoDismiss) {
                            mHandler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    mState = STATE_IDLE;
                                    invalidate();
                                }
                            }, 500);
                        }
                    }
                    break;
                default:
                    break;
            }
            super.handleMessage(msg);
        }
    }

    public static interface OnLongTouchListener {
        public void onLongTouch(View view);
    }

    public static interface  OnFocusListener {
        public void onFocus(View view, float x, float y);
    }
}
