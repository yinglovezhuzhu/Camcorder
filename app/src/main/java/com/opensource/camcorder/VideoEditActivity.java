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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Checkable;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.opensource.bitmaploader.ImageCache;
import com.opensource.bitmaploader.ImageFetcher;
import com.opensource.bitmaploader.ImageResizer;
import com.opensource.bitmaploader.ImageWorker;
import com.opensource.bitmaploader.Utils;
import com.opensource.camcorder.entity.Watermark;
import com.opensource.camcorder.utils.CamcorderUtil;
import com.opensource.camcorder.utils.FileUtil;
import com.opensource.camcorder.utils.LogUtil;
import com.opensource.camcorder.utils.StringUtil;
import com.opensource.camcorder.widget.CamcorderTitlebar;
import com.opensource.camcorder.widget.HorizontalGridView;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Use:
 * Created by yinglovezhuzhu@gmail.com on 2014-07-22.
 */
public class VideoEditActivity extends NoSearchActivity {

    public static final String EXTRA_VIDEO = "extra_video";

    public static final String EXTRA_THUMB = "extra_thumb";

    private static final String TAG = "VideoEditActivity";

    private CamcorderTitlebar mTitlebar;
//    private SurfaceView mSurfaceView;
    private VideoView mVideoView;
    private ImageView mIvThumb;
    private ImageView mIvFlow;
    private ImageView mIvIcon;
    private HorizontalGridView mHGridView;
    private RadioGroup mToolbar;

    private WatermarkAdapter mAdapter;

    private ImageFetcher mImageFetcher;
    private ImageResizer mImageResizer;

    private VideoPlayer mVideoPlayer;

    private String mVideoPath;
    private String mThumbPath;

    private boolean mEdited = false; //是否进行过编辑


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(!iniData()) {
            Toast.makeText(this, "视频数据不能为空", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setContentView(R.layout.activity_video_edit);

        initIconFetcher();

        initImageWorker();

        initView();

    }

    @Override
    public void onBackPressed() {
        exit();
    }

    private boolean iniData() {
        Intent intent = getIntent();
        if(intent.hasExtra(EXTRA_VIDEO)) {
            mVideoPath = intent.getStringExtra(EXTRA_VIDEO);
            if(StringUtil.isEmpty(mVideoPath)) {
                return false;
            }
        } else {
            return false;
        }
        if(intent.hasExtra(EXTRA_THUMB)) {
            mThumbPath = intent.getStringExtra(EXTRA_THUMB);
        }
        return true;
    }

    private void initView() {

        initTitlebar();

        mVideoPlayer = new VideoPlayer(findViewById(R.id.fl_video_edit_content), this, mVideoPath);


//        mSurfaceView = (SurfaceView) findViewById(R.id.sv_video_preview);
        mVideoView = (VideoView) findViewById(R.id.vv_video_edit_preview);
        mIvThumb = (ImageView) findViewById(R.id.iv_video_edit_thumb);
        mIvFlow = (ImageView) findViewById(R.id.iv_video_edit_flow);
        mIvIcon = (ImageView) findViewById(R.id.iv_video_edit_icon);

//        ViewGroup.LayoutParams lp = mSurfaceView.getLayoutParams();
        ViewGroup.LayoutParams lp = mVideoView.getLayoutParams();
        lp.width = getResources().getDisplayMetrics().widthPixels;
        lp.height = lp.width;

        mHGridView = (HorizontalGridView) findViewById(R.id.hgv_video_edit_boxes);

        mAdapter = new WatermarkAdapter(this);

        mHGridView.setAdapter(mAdapter);

        mToolbar = (RadioGroup) findViewById(R.id.rg_recorder_toolbar);
        mToolbar.setOnCheckedChangeListener(mOnCheckChangeListener);

        mToolbar.check(R.id.rbtn_video_edit_watermark); //默认选中水印

        if(StringUtil.isEmpty(mThumbPath)) {

        } else {
            mImageResizer.loadImage(mThumbPath, mIvThumb);
        }

        mVideoPlayer.start();

    }

    private void initTitlebar() {
        mTitlebar = (CamcorderTitlebar) findViewById(R.id.tb_video_edit);
        mTitlebar.setLeftButton(R.string.cancel, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exit();
            }
        });
        mTitlebar.setRightButton(R.string.next_step, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //TODO 合成最终效果，跳转至上传页面
            }
        });
        mTitlebar.setButton1(R.drawable.selector_ic_save, 0, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //TODO 保存草稿

            }
        });
        mTitlebar.setButton2(0,0,null);
    }

    private RadioGroup.OnCheckedChangeListener mOnCheckChangeListener = new RadioGroup.OnCheckedChangeListener() {

        @Override
        public void onCheckedChanged(RadioGroup group, int checkedId) {
            switch (checkedId) {
                case R.id.rbtn_video_edit_watermark:
                    mAdapter.clear();
                    new InitWatermarkDataTask(VideoEditActivity.this).execute();
                    mAdapter.setCheckedPosition(mAdapter.getCheckedPosition());
                    break;
                case R.id.rbtn_video_edit_music:
                    mAdapter.clear();
                    Toast.makeText(VideoEditActivity.this, "功能建设中", Toast.LENGTH_SHORT).show();
                    break;
                case R.id.rbtn_video_edit_filter:
                    mAdapter.clear();
                    Toast.makeText(VideoEditActivity.this, "功能建设中", Toast.LENGTH_SHORT).show();
                    break;
                case R.id.rbtn_video_edit_theme:
                    mAdapter.clear();
                    Toast.makeText(VideoEditActivity.this, "功能建设中", Toast.LENGTH_SHORT).show();
                    break;
                default:
                    break;
            }
        }
    };

    private void initIconFetcher() {
        ImageCache.ImageCacheParams cacheParams = new ImageCache.ImageCacheParams("icon_thumb");
        cacheParams.memCacheSize = 1024 * 1024 * Utils.getMemoryClass(this) / 3;
        cacheParams.compressFormat = Bitmap.CompressFormat.PNG;

        mImageFetcher = new ImageFetcher(this, getResources().getDimensionPixelSize(R.dimen.video_edit_box_item_icon_size));
        mImageFetcher.setImageCache(new ImageCache(this, cacheParams));

    }

    private void initImageWorker() {
        ImageCache.ImageCacheParams cacheParams= new ImageCache.ImageCacheParams("video_thumb");
        cacheParams.memCacheSize = 1024 * 1024 * Utils.getMemoryClass(this) / 3;
        cacheParams.compressFormat = Bitmap.CompressFormat.PNG;

        mImageResizer = new ImageResizer(this, getResources().getDimensionPixelSize(R.dimen.video_edit_box_item_icon_size));
        mImageResizer.setImageCache(new ImageCache(this, cacheParams));

    }

    /**
     * 删除产生的临时文件
     * 包括合成的视频和视频的缩略图
     * 下一次进入的时候会重新合并视频和生成缩略图。
     */
    private void deleteFiles() {
        if(!StringUtil.isEmpty(mVideoPath)) {
            File videoFile = new File(mVideoPath);
            if(videoFile.exists()) {
                if(!videoFile.delete()) {
                    LogUtil.w(TAG, "Delete file failed: path -> " + mVideoPath);
                }
            }
        }
        if(!StringUtil.isEmpty(mThumbPath)) {
            File thumbFile = new File(mThumbPath);
            if(thumbFile.exists()) {
                if(!thumbFile.delete()) {
                    LogUtil.w(TAG, "Delete file failed: path -> " + mThumbPath);
                }
            }
        }
    }

    /**
     * 退出编辑页面
     */
    private void exit() {
        if(mEdited) {
            new AlertDialog.Builder(VideoEditActivity.this)
                    .setMessage("确定返回码？编辑效果将丢失")
                    .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            deleteFiles();
                            finish();
                        }
                    })
                    .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    }).show();
        } else {
            deleteFiles();
            finish();
        }
    }


    /**
     * 水印列表适配器
     */
    private class WatermarkAdapter extends RecyclerView.Adapter {

        public static final int INVALID_POSITION = -1;

        private Context mmContext;
        private List<Watermark> mmWatermarkDatas = new ArrayList<Watermark>();

        private int mmCheckedPosition = INVALID_POSITION;

        public WatermarkAdapter(Context context) {
            this.mmContext = context;
        }

        public void add(Watermark data) {
            this.mmWatermarkDatas.add(data);
            notifyDataSetChanged();
        }

        public void addAll(Collection<Watermark> datas) {
            this.mmWatermarkDatas.addAll(datas);
            notifyDataSetChanged();
        }

        public void clear() {
            this.mmWatermarkDatas.clear();
            notifyDataSetChanged();
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, final int i) {
            View view = View.inflate(mmContext, R.layout.item_box, null);
            final ViewHolder viewHolder = new ViewHolder(view);
            viewHolder.itemView.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    int position = viewHolder.getPosition();
                    switch (position) {
                        case 0:
                            //TODO 打开水印库
                            break;
                        default:
                            int lastPosition = mmCheckedPosition;
                            mmCheckedPosition = position;
                            notifyItemChanged(lastPosition);
                            notifyItemChanged(mmCheckedPosition);
                            Watermark mark = mmWatermarkDatas.get(mmCheckedPosition);
                            if(StringUtil.isEmpty(mark.getPath())) {
                                mIvFlow.setImageBitmap(null);
                                mIvFlow.setVisibility(View.GONE);
                            } else {
                                mImageResizer.loadImage(mark.getPath(), mIvFlow);
                                mIvFlow.setVisibility(View.VISIBLE);
                            }
                            break;
                    }
                }
            });
            return viewHolder;
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
            final ViewHolder holder = ((ViewHolder)viewHolder);
            holder.setChecked(mmCheckedPosition == position);
            Watermark mark = mmWatermarkDatas.get(position);
            holder.setLabel(mark.getName());
            switch (position) {
                case 0:
                    holder.setThumb(R.drawable.ic_drawer, ImageView.ScaleType.CENTER);
                    holder.showTip(R.string.tip_new, ViewHolder.TIP_TYPE_NEW);
                    break;
                case 1:
                    holder.setThumb(R.drawable.ic_none, ImageView.ScaleType.CENTER);
                    holder.showTip(null, ViewHolder.TIP_TYPE_NONE);
                    break;
                default:
                    holder.setThumb(mark.getPath(), mImageResizer, ImageView.ScaleType.CENTER_CROP);
                    if(position % 3 == 0) {
                        holder.showTip("HOT", ViewHolder.TIP_TYPE_HOT);
                    } else {
                        holder.showTip(null, ViewHolder.TIP_TYPE_NONE);
                    }
                    break;
            }
        }

        @Override
        public int getItemCount() {
            return this.mmWatermarkDatas.size();
        }

        /**
         * 获取选中的item的位置
         * @return
         */
        public int getCheckedPosition() {
            return mmCheckedPosition;
        }

        /**
         * 设置选中的item位置
         * @param position
         */
        public void setCheckedPosition(int position) {
            int lastPosition = mmCheckedPosition;
            mmCheckedPosition = position;
            notifyItemChanged(lastPosition);
            notifyItemChanged(mmCheckedPosition);
        }
    }

    private class ViewHolder extends RecyclerView.ViewHolder implements Checkable {

        /** 无Tip **/
        public static final int TIP_TYPE_NONE = 0;
        /** 显示NEW的tip **/
        public static final int TIP_TYPE_NEW = 1;
        /** 显示HOT的tip **/
        public static final int TIP_TYPE_HOT = 2;


        private LinearLayout mmLlThumbContent;
        private ImageView mmIvThumb;
        private ImageView mmIvDownloadIcon;
        private TextView mmTvTip;
        private TextView mmTvLabel;

        private boolean mmIsChecked = false;


        public ViewHolder(View itemView) {
            super(itemView);
            initView();
        }

        @Override
        public void setChecked(boolean checked) {
            if(checked) {
                mmLlThumbContent.setBackgroundResource(R.drawable.bg_item_box_selected);
                mmTvLabel.setTextColor(getResources().getColor(R.color.white));
            } else {
                mmLlThumbContent.setBackgroundResource(R.drawable.bg_item_box_normal);
                mmTvLabel.setTextColor(getResources().getColor(R.color.text_color_gray));
            }
            this.mmIsChecked = checked;
        }

        @Override
        public boolean isChecked() {
            return mmIsChecked;
        }

        @Override
        public void toggle() {
            setChecked(!mmIsChecked);
        }

        /**
         * 显示Tip
         * @param text 文字
         * @param type 类型
         * @see {@link com.opensource.camcorder.VideoEditActivity.ViewHolder#TIP_TYPE_NONE}
         * @see {@link com.opensource.camcorder.VideoEditActivity.ViewHolder#TIP_TYPE_NEW}
         * @see {@link com.opensource.camcorder.VideoEditActivity.ViewHolder#TIP_TYPE_HOT}
         */
        public void showTip(CharSequence text, int type) {
            if(showTipByType(type)) {
                mmTvTip.setText(text);
            }
        }

        /**
         * 显示Tip
         * @param resid 文字
         * @param type 类型
         * @see {@link com.opensource.camcorder.VideoEditActivity.ViewHolder#TIP_TYPE_NONE}
         * @see {@link com.opensource.camcorder.VideoEditActivity.ViewHolder#TIP_TYPE_NEW}
         * @see {@link com.opensource.camcorder.VideoEditActivity.ViewHolder#TIP_TYPE_HOT}
         */
        public void showTip(int resid, int type) {
            if(showTipByType(type)) {
                mmTvTip.setText(resid);
            }
        }

        /**
         * 设置缩略图（资源图片）
         * @param resid
         * @param scaleType
         */
        public void setThumb(int resid, ImageView.ScaleType scaleType) {
            mmIvThumb.setScaleType(scaleType);
            mmIvThumb.setImageResource(resid);
        }

        /**
         * 设置缩略图
         * @param url
         * @param imageWorker
         * @param scaleType
         */
        public void setThumb(String url, ImageWorker imageWorker, ImageView.ScaleType scaleType) {
            mmIvThumb.setScaleType(scaleType);
            imageWorker.loadImage(url, mmIvThumb);
        }

//        /**
//         * 设置缩略图(本地图片)
//         * @param path
//         * @param imageResizer
//         * @param scaleType
//         */
//        public void setThumb(String path, ImageResizer imageResizer, ImageView.ScaleType scaleType) {
//            mmIvThumb.setScaleType(scaleType);
//            imageResizer.loadImage(path, mmIvThumb);
//        }

        /**
         * 设置标签文字
         * @param text
         */
        public void setLabel(CharSequence text) {
            mmTvLabel.setText(text);
        }

        /**
         * 设置是否已经下载，如果没下载显示未下载的图标
         * @param isDownloaded
         */
        public void setDownloaded(boolean isDownloaded) {
            mmIvDownloadIcon.setVisibility(isDownloaded ? View.VISIBLE : View.GONE);
        }

        private void initView() {
            mmLlThumbContent = (LinearLayout) itemView.findViewById(R.id.ll_item_box_thumb_content);
            mmIvThumb = (ImageView) itemView.findViewById(R.id.iv_item_box_thumb);
            mmIvDownloadIcon = (ImageView) itemView.findViewById(R.id.iv_item_box_download_icon);
            mmTvTip = (TextView) itemView.findViewById(R.id.tv_item_box_tip);
            mmTvLabel = (TextView) itemView.findViewById(R.id.tv_item_box_label);
        }

        /**
         * 根据类型来显示或者隐藏Tip
         * @param type
         * @return 是否显示tip
         * @see {@link com.opensource.camcorder.VideoEditActivity.ViewHolder#TIP_TYPE_NONE}
         * @see {@link com.opensource.camcorder.VideoEditActivity.ViewHolder#TIP_TYPE_NEW}
         * @see {@link com.opensource.camcorder.VideoEditActivity.ViewHolder#TIP_TYPE_HOT}
         */
        private boolean showTipByType(int type) {
            if(TIP_TYPE_NONE == type) {
                mmTvTip.setVisibility(View.GONE);
            } else if(TIP_TYPE_HOT == type) {
                mmTvTip.setVisibility(View.VISIBLE);
                mmTvTip.setBackgroundResource(R.drawable.bg_hot_tip);
                return true;
            } else if(TIP_TYPE_NEW == type) {
                mmTvTip.setVisibility(View.VISIBLE);
                mmTvTip.setBackgroundResource(R.drawable.bg_new_tip);
                return true;
            } else {
                mmTvTip.setVisibility(View.GONE);
            }
            return false;
        }
    }

    private class InitWatermarkDataTask extends AsyncTask<Void, Integer, List<Watermark>> {

        private Context mmContext;

        public InitWatermarkDataTask(Context context) {
            this.mmContext = context;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected List<Watermark> doInBackground(Void... params) {
            List<Watermark> results = new ArrayList<Watermark>();
            results.add(new Watermark("水印库"));
            results.add(new Watermark("无水印"));
            File cacheFolder = CamcorderUtil.getExternalLocalCacheDir(mmContext);
            if(null != cacheFolder) {
                if(!cacheFolder.exists()) {
                    cacheFolder.mkdirs();
                }
                File file1 = new File(cacheFolder, "watermark_black.png");
                File file2 = new File(cacheFolder, "watermark_kiss.png");
                Watermark mark1 = new Watermark("加黑");
                Watermark mark2 = new Watermark("KISS");
                if(file1.exists()) {
                    mark1.setPath(file1.getAbsolutePath());
                    results.add(mark1);
                } else {
                    if(FileUtil.copyRaw2Dir(mmContext, R.raw.watermark_black, file1)) {
                        mark1.setPath(file1.getAbsolutePath());
                        results.add(mark1);
                    }
                }
                if(file2.exists()) {
                    mark2.setPath(file2.getAbsolutePath());
                    results.add(mark2);
                } else {
                    if(FileUtil.copyRaw2Dir(mmContext, R.raw.watermark_kiss, file2)) {
                        mark2.setPath(file2.getAbsolutePath());
                        results.add(mark2);
                    }
                }
            }
            return results;
        }

        @Override
        protected void onPostExecute(List<Watermark> results) {
            if(isCancelled()) {
                return;
            }
            mAdapter.clear();
            mAdapter.addAll(results);
            super.onPostExecute(results);
        }
    }
}
