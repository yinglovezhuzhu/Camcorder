package com.opensource.camcorder.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import com.opensource.camcorder.R;


/**
 * Use:
 * Created by yinglovezhuzhu@gmail.com on 2014-06-23.
 */
public class CamcorderTitlebar extends LinearLayout {

    private Button mBtnLeft;
    private Button mBtnRight;
    private Button mButton1;
    private Button mButton2;


    public CamcorderTitlebar(Context context) {
        super(context);
        init();
    }

    public CamcorderTitlebar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CamcorderTitlebar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs);
        init();
    }


    /**
     * 设置左边的按钮
     * @param resid 文字资源id
     * @param listener
     */
    public void setLeftButton(int resid, OnClickListener listener) {
        if(resid <= 0) {
            mBtnLeft.setVisibility(View.INVISIBLE);
            return;
        }
        mBtnLeft.setVisibility(View.VISIBLE);
        mBtnLeft.setText(resid);;
        mBtnLeft.setOnClickListener(listener);
    }

    /**
     * 设置左边的按钮
     * @param text 文字
     * @param listener
     */
    public void setLeftButton(CharSequence text, OnClickListener listener) {
        if(null == text) {
        	mBtnLeft.setVisibility(View.INVISIBLE);
            return;
        }
        mBtnLeft.setVisibility(View.VISIBLE);
        mBtnLeft.setText(text);
        mBtnLeft.setOnClickListener(listener);
    }

    /**
     * 设置右边的按钮
     * @param resid 文字资源id
     * @param listener
     */
    public void setRightButton(int resid, OnClickListener listener) {
        if(resid <= 0) {
            mBtnRight.setVisibility(View.INVISIBLE);
            return;
        }
        mBtnRight.setVisibility(View.VISIBLE);
        mBtnRight.setText(resid);
        mBtnRight.setOnClickListener(listener);
    }

    /**
     * 设置右边的按钮
     * @param text 文字
     * @param listener
     */
    public void setRightButton(CharSequence text, OnClickListener listener) {
        if(null == text) {
            mBtnRight.setVisibility(View.INVISIBLE);
            return;
        }
        mBtnRight.setVisibility(View.VISIBLE);
        mBtnRight.setText(text);
        mBtnRight.setOnClickListener(listener);
    }


    /**
     * 设置按钮1</br>
     * @param drawableLeft 左边图标
     * @param drawableRight 右边图标
     * @param listener
     */
    public void setButton1(int drawableLeft, int drawableRight, OnClickListener listener) {
        if(drawableLeft <= 0) {
            mButton1.setVisibility(View.INVISIBLE);
            return;
        }
        mButton1.setVisibility(View.VISIBLE);
        mButton1.setCompoundDrawablesWithIntrinsicBounds(drawableLeft, 0, 0, 0);
        mButton1.setOnClickListener(listener);
    }

    /**
     * 设置按钮2</br>
     * @param drawableLeft 左边图标
     * @param drawableRight 右边图标
     * @param listener
     */
    public void setButton2(int drawableLeft, int drawableRight, OnClickListener listener) {
        if(drawableLeft <= 0 && drawableRight <= 0) {
            mButton2.setVisibility(View.INVISIBLE);
            return;
        }
        mButton2.setVisibility(View.VISIBLE);
        mButton2.setCompoundDrawablesWithIntrinsicBounds(drawableLeft, 0, drawableRight , 0);
        mButton2.setOnClickListener(listener);
    }

    /**
     * 启用/禁用左边按钮
     * @param enabled
     */
    public void setLeftButtonEnabled(boolean enabled) {
        mBtnLeft.setEnabled(enabled);
    }

    /**
     * 启用/禁用按钮1
     * @param enabled
     */
    public void setButton1Enabled(boolean enabled) {
        mButton1.setEnabled(enabled);
    }

    /**
     * 启用/禁用按钮2
     * @param enabled
     */
    public void setButton2Enabled(boolean enabled) {
        mButton2.setEnabled(enabled);
    }

    /**
     * 启用/禁用右边按钮
     * @param enabled
     */
    public void setRightButtonEnabled(boolean enabled) {
        mBtnRight.setEnabled(enabled);
    }

    public Button getButton1() {
        return mButton1;
    }

    public Button getButton2() {
        return mButton2;
    }



    private void init() {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        inflater.inflate(R.layout.layout_camcorder_title, this);

        mBtnLeft = (Button) findViewById(R.id.btn_camcorder_title_left);
        mBtnRight = (Button) findViewById(R.id.btn_camcorder_title_right);
        mButton1 = (Button) findViewById(R.id.btn_camcorder_title_button1);
        mButton2 = (Button) findViewById(R.id.btn_camcorder_title_button2);

    }
}
