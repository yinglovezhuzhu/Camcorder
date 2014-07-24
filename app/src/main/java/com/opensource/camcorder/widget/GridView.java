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
import android.util.AttributeSet;
import android.view.View;

/**
 * Use:
 * Created by yinglovezhuzhu@gmail.com on 2014-07-24.
 */
public class GridView extends View {

    private Paint mPaint;

    private int mGridColor = Color.argb(0xAA, 0xFF, 0xFF, 0xFF);

    private float mBorderWidth = 2.0f;

    public GridView(Context context) {
        super(context);
        init();
    }

    public GridView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public GridView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }


    @Override
    protected void onDraw(Canvas canvas) {

        canvas.drawARGB(0, 0, 0, 0); //透明背景色


        drawGrid(canvas);


        super.onDraw(canvas);
    }

    private void drawGrid(Canvas canvas) {
        int width = getWidth();
        int height = getHeight();

        canvas.drawLine(0f, height / 3.0f, width, height / 3.0f, mPaint);
        canvas.drawLine(0f, height / 3.0f * 2, width, height / 3.0f * 2, mPaint);
        canvas.drawLine(width / 3.0f, 0f, width / 3.0f, height, mPaint);
        canvas.drawLine(width / 3.0f * 2, 0f, width / 3.0f * 2, height, mPaint);
    }

    private void init() {
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setStrokeWidth(mBorderWidth);
        mPaint.setColor(mGridColor);
    }
}
