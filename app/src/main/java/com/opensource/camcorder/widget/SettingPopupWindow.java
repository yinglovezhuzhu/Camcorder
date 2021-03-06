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
import android.graphics.drawable.ColorDrawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.PopupWindow;
import android.widget.ToggleButton;

import com.opensource.camcorder.R;
import com.opensource.camcorder.utils.ViewUtil;

/**
 * Use:
 * Created by yinglovezhuzhu@gmail.com on 2014-07-01.
 */
public class SettingPopupWindow extends PopupWindow {

    private ToggleButton mTBtnGrid;
    private ToggleButton mTBtnFlash;

    public SettingPopupWindow(Context context) {
        super(context);
        init(context);
    }

    public SettingPopupWindow(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public SettingPopupWindow(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    public void setGridCheckChangedListener(CompoundButton.OnCheckedChangeListener listener) {
        mTBtnGrid.setOnCheckedChangeListener(listener);
    }

    public void setFlashCheckChangedListener(CompoundButton.OnCheckedChangeListener listener) {
        mTBtnFlash.setOnCheckedChangeListener(listener);
    }

    public boolean isGridChecked() {
        return mTBtnGrid.isChecked();
    }

    public boolean isFlashChecked() {
        return mTBtnFlash.isChecked();
    }

    public void setGridEnabled(boolean enabled) {
        mTBtnGrid.setEnabled(enabled);
    }

    public void setFlashEnabled(boolean enabled) {
        mTBtnFlash.setEnabled(enabled);
    }

    private void init(Context context) {
        setBackgroundDrawable(new ColorDrawable(context.getResources().getColor(R.color.transparent)));
        View contentView = View.inflate(context, R.layout.layout_camcorder_setting, null);
        ViewUtil.measureView(contentView);
        setContentView(contentView);
        setWidth(contentView.getMeasuredWidth());
        setHeight(contentView.getMeasuredHeight());
        setFocusable(true);
        setOutsideTouchable(true);
        mTBtnGrid = (ToggleButton) contentView.findViewById(R.id.tbtn_camcorder_setting_grid);
        mTBtnFlash = (ToggleButton) contentView.findViewById(R.id.tbtn_camcorder_setting_flash);
    }
}
