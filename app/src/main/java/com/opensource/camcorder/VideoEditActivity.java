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

package com.opensource.camcorder;

import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.opensource.camcorder.widget.CamcorderTitlebar;
import com.opensource.camcorder.widget.HorizontalGridView;
import com.opensource.camcorder.widget.OnChildSelectedListener;

/**
 * Use:
 * Created by yinglovezhuzhu@gmail.com on 2014-07-22.
 */
public class VideoEditActivity extends NoSearchActivity {

    private CamcorderTitlebar mTitlebar;
    private SurfaceView mSurfaceView;
    private ImageView mIvThumb;
    private ImageView mIvFlow;
    private ImageView mIvIcon;
    private HorizontalGridView mHGridView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_video_edit);


        initView();

    }

    private void initView() {

        initTitlebar();

        mSurfaceView = (SurfaceView) findViewById(R.id.sv_video_preview);
        mIvThumb = (ImageView) findViewById(R.id.iv_video_edit_thumb);
        mIvFlow = (ImageView) findViewById(R.id.iv_video_edit_flow);
        mIvIcon = (ImageView) findViewById(R.id.iv_video_edit_icon);

        ViewGroup.LayoutParams lp = mSurfaceView.getLayoutParams();
        lp.width = getResources().getDisplayMetrics().widthPixels;
        lp.height = lp.width;

        mHGridView = (HorizontalGridView) findViewById(R.id.hgv_video_edit_boxes);

        mHGridView.setAdapter(new RecyclerView.Adapter() {
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
                ImageView iv = new ImageView(VideoEditActivity.this);
                iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                System.out.println("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA<<<<<<<<<<<<>>>>>>>>" + i);
                View view = View.inflate(VideoEditActivity.this, R.layout.item_box, null);
                return new ViewHolder(view);
            }

            @Override
            public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, final int i) {
                viewHolder.itemView.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        mHGridView.setSelectedPosition(i);
                    }
                });
                System.out.println("BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB<<<<<<<<<<>>>>>>>>>>>>" + i);
//                if(i == 0) {
//                    ((ViewHolder)viewHolder).setImageResource(R.drawable.ic_drawer);
//                } else if(i == 1) {
//                    ((ViewHolder)viewHolder).setImageResource(R.drawable.ic_none);
//                } else {
//                    ((ViewHolder)viewHolder).setImageResource(R.drawable.ic_launcher);
//                }
            }

            @Override
            public int getItemCount() {
                return 20;
            }
        });

        mHGridView.setOnChildSelectedListener(new OnChildSelectedListener() {
            @Override
            public void onChildSelected(ViewGroup parent, View view, int position, long id) {
                System.out.println("CCCCCCCCCCCCCCCCCCCCCCCC<<<<<<<<>>>>>>>>" + id);
            }
        });
    }

    private void initTitlebar() {
        mTitlebar = (CamcorderTitlebar) findViewById(R.id.tb_video_edit);
        mTitlebar.setLeftButton(R.string.cancel, new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
        mTitlebar.setRightButton(R.string.next_step, new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
        mTitlebar.setButton1(R.drawable.selector_ic_save, 0, new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
        mTitlebar.setButton2(0,0,null);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        ImageView iv;
        public ViewHolder(View itemView) {
            super(itemView);
//            this.iv = (ImageView) itemView;

        }

//        public void setImageResource(int resId) {
//            iv.setImageResource(resId);
//        }
    }
}
