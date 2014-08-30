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
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Checkable;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
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
import com.opensource.camcorder.service.FFmpegService;
import com.opensource.camcorder.utils.CamcorderUtil;
import com.opensource.camcorder.utils.FileUtil;
import com.opensource.camcorder.utils.StringUtil;
import com.opensource.camcorder.widget.CamcorderTitlebar;
import com.opensource.camcorder.widget.HorizontalGridView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Use:
 * Created by yinglovezhuzhu@gmail.com on 2014-07-22.
 */
public class VideoEditActivity extends NoSearchActivity {

    private static final String TAG = "VideoEditActivity";

    private CamcorderTitlebar mTitlebar;
//    private SurfaceView mSurfaceView;
    private VideoView mVideoView;
    private ImageView mIvThumb;
    private ImageView mIvFlow;
    private ImageView mIvIcon;
    private ProgressBar mPbEditing;
    private HorizontalGridView mHGridView;
    private RadioGroup mToolbar;

    private WatermarkAdapter mAdapter;

    private ImageFetcher mImageFetcher;
    private ImageResizer mImageResizer;

    private VideoPlayer mVideoPlayer;

    private String mVideoPath;
    private String mThumbPath;

    private String mResultVideoPath;
    private String mResultThumbPath;

    private boolean mEdited = false; //是否进行过编辑

    private int mScreenWidth;

    private AddWatermarkTask mAddWatermarkTask;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(!iniData()) {
            Toast.makeText(this, "视频数据不能为空", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setContentView(R.layout.activity_video_edit);

        mScreenWidth = getResources().getDisplayMetrics().widthPixels;

        initIconFetcher();

        initImageResizer();

        initView();

    }

    @Override
    public void onBackPressed() {
        exit();
    }

    private boolean iniData() {
        Intent intent = getIntent();
        if(intent.hasExtra(CamcorderConfig.EXTRA_VIDEO)) {
            mVideoPath = intent.getStringExtra(CamcorderConfig.EXTRA_VIDEO);
            if(StringUtil.isEmpty(mVideoPath)) {
                return false;
            }
        } else {
            return false;
        }
        if(intent.hasExtra(CamcorderConfig.EXTRA_THUMB)) {
            mThumbPath = intent.getStringExtra(CamcorderConfig.EXTRA_THUMB);
        }
        return true;
    }

    private void initView() {
        initTitlebar();

        FrameLayout contentLayout = (FrameLayout) findViewById(R.id.fl_video_edit_content);
        ViewGroup.LayoutParams contentLp = contentLayout.getLayoutParams();
        contentLp.width = getResources().getDisplayMetrics().widthPixels;
        contentLp.height = contentLp.width;
        contentLayout.setLayoutParams(contentLp);

        mVideoPlayer = new VideoPlayer(contentLayout, this, mVideoPath);

        mVideoView = (VideoView) findViewById(R.id.vv_video_edit_preview);
        mIvThumb = (ImageView) findViewById(R.id.iv_video_edit_thumb);
        mIvFlow = (ImageView) findViewById(R.id.iv_video_edit_flow);
        mIvIcon = (ImageView) findViewById(R.id.iv_video_edit_icon);

        ViewGroup.LayoutParams videoViewLp = mVideoView.getLayoutParams();
        videoViewLp.width = getResources().getDisplayMetrics().widthPixels;
        videoViewLp.height = videoViewLp.width;
        mVideoView.setLayoutParams(videoViewLp);

        ViewGroup.LayoutParams flowLp = mIvFlow.getLayoutParams();
        flowLp.width = getResources().getDisplayMetrics().widthPixels;
        flowLp.height = flowLp.width;
        mIvFlow.setLayoutParams(flowLp);

        mPbEditing = (ProgressBar) findViewById(R.id.pb_video_edit_progress);

        mHGridView = (HorizontalGridView) findViewById(R.id.hgv_video_edit_boxes);

        mAdapter = new WatermarkAdapter(this);

        mHGridView.setAdapter(mAdapter);

        mAdapter.setCheckedPosition(1); //开始默认无水印
        mResultVideoPath = mVideoPath;
        mResultThumbPath = mThumbPath;

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
                if(StringUtil.isEmpty(mResultVideoPath)) {
                    Toast.makeText(VideoEditActivity.this, "视频文件为空", Toast.LENGTH_SHORT).show();
                    return;
                }
                File videoFile = new File(mResultVideoPath);
                if(videoFile.exists()) {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(Uri.fromFile(videoFile), "video/*");
                    startActivity(intent);
                } else {
                    Toast.makeText(VideoEditActivity.this, "视频文件不存在", Toast.LENGTH_SHORT).show();
                }
            }
        });
        mTitlebar.setButton1(R.drawable.selector_ic_save, 0, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //TODO 保存草稿

            }
        });
        mTitlebar.setButton2(0, 0, null);
    }

    private RadioGroup.OnCheckedChangeListener mOnCheckChangeListener = new RadioGroup.OnCheckedChangeListener() {

        @Override
        public void onCheckedChanged(RadioGroup group, int checkedId) {
            switch (checkedId) {
                case R.id.rbtn_video_edit_watermark:
                    mAdapter.clear();
//                    new InitWatermarkDataTask(VideoEditActivity.this).execute();
                    mAdapter.clear();
                    mAdapter.addAll((Collection<Watermark>) CamcorderApp.getDecorations("watermark"));
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

        mImageFetcher = new ImageFetcher(this, 0);
        mImageFetcher.setImageCache(new ImageCache(this, cacheParams));

    }

    private void initImageResizer() {
        ImageCache.ImageCacheParams cacheParams= new ImageCache.ImageCacheParams("video_thumb");
        cacheParams.memCacheSize = 1024 * 1024 * Utils.getMemoryClass(this) / 3;
        cacheParams.compressFormat = Bitmap.CompressFormat.PNG;

        mImageResizer = new ImageResizer(this, 0);
        mImageResizer.setImageCache(new ImageCache(this, cacheParams));

    }

    /**
     * 删除产生的临时文件
     * 包括合成的视频和视频的缩略图
     * 下一次进入的时候会重新合并视频和生成缩略图。
     */
    private void deleteFiles() {
        FileUtil.deleteFile(mVideoPath);
        FileUtil.deleteFile(mThumbPath);
        FileUtil.deleteFile(mResultVideoPath);
        FileUtil.deleteFile(mResultThumbPath);
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
                            exitSiltently();
                        }
                    })
                    .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    }).show();
        } else {
            exitSiltently();
        }
    }

    /**
     * 静默退出
     */
    private void exitSiltently() {
        deleteFiles();
        finish();
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
                            if(position == mmCheckedPosition) {
                                return;
                            }
                            mEdited = true;
                            int lastPosition = mmCheckedPosition;
                            mmCheckedPosition = position;
                            notifyItemChanged(lastPosition);
                            notifyItemChanged(mmCheckedPosition);
                            Watermark mark = mmWatermarkDatas.get(mmCheckedPosition);
                            if(StringUtil.isEmpty(mark.getIconUrl())) {
                                mIvFlow.setImageBitmap(null);
                                mIvFlow.setVisibility(View.INVISIBLE);
                                if(mResultVideoPath != null && !mResultVideoPath.equals(mVideoPath)) {
                                    FileUtil.deleteFile(mResultVideoPath);
                                    FileUtil.deleteFile(mResultThumbPath);
                                }
                                mResultVideoPath = mVideoPath;
                                mResultThumbPath = mThumbPath;
                            } else {
                                mIvFlow.setImageBitmap(null);
                                Watermark.WatermarkData data = mark.getUserData();
                                List<Watermark.ElementData> elements = data.getElements();
                                if(elements.isEmpty()) {
                                    return;
                                }
                                Watermark.ElementData element = elements.get(0);
                                Watermark.Rect rect = element.getRect();
                                if(rect.getWidth() > 480 || rect.getHeight() > 480) {
                                    mIvFlow.setScaleType(ImageView.ScaleType.FIT_XY);
                                    mImageResizer.loadImage(element.getDefaultValue(), mIvFlow);
                                } else {
                                    if(rect.getWidth() + rect.getX() > 480) {
                                        rect.setX(480 - rect.getWidth());
                                    }
                                    if(rect.getHeight() + rect.getY() > 480) {
                                        rect.setY(480 - rect.getHeight());
                                    }
                                    mIvFlow.setScaleType(ImageView.ScaleType.MATRIX);
                                    Matrix m = mIvFlow.getImageMatrix();
                                    m.setTranslate(mScreenWidth * rect.getX() / 480, mScreenWidth * rect.getY() / 480);
                                    mIvFlow.setImageMatrix(m);
                                    mImageResizer.loadImage(element.getDefaultValue(), mIvFlow);
                                }
                                mIvFlow.setVisibility(View.VISIBLE);
                                mVideoPlayer.start();
                                if(null != mAddWatermarkTask) {
                                    mAddWatermarkTask.cancel(true);
                                    mAddWatermarkTask = null;
                                }
                                mAddWatermarkTask = new AddWatermarkTask();
                                mAddWatermarkTask.execute(mark);
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
                    holder.setThumb(mark.getIconUrl(), mImageResizer, ImageView.ScaleType.CENTER_CROP);
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

//    private class InitWatermarkDataTask extends AsyncTask<Void, Integer, List<Watermark>> {
//
//        private Context mmContext;
//
//        public InitWatermarkDataTask(Context context) {
//            this.mmContext = context;
//        }
//
//        @Override
//        protected void onPreExecute() {
//            super.onPreExecute();
//        }
//
//        @Override
//        protected List<Watermark> doInBackground(Void... params) {
//            List<Watermark> results = new ArrayList<Watermark>();
//            Watermark wm = new Watermark();
//            wm.setName("水印库");
//            results.add(wm);
//            wm = new Watermark();
//            wm.setName("无水印");
//            results.add(wm);
//            File cacheFolder = CamcorderUtil.getExternalLocalCacheDir(mmContext);
//            if(null != cacheFolder) {
//                if(!cacheFolder.exists()) {
//                    cacheFolder.mkdirs();
//                }
//                File file1 = new File(cacheFolder, "watermark_black.png");
//                File file2 = new File(cacheFolder, "pic_watermark_kisskiss.png");
//                Watermark mark1 = new Watermark();
//                mark1.setName("加黑");
//                Watermark mark2 = new Watermark();
//                mark2.setName("KISS");
//                if(file1.exists()) {
//                    mark1.setIconUrl(file1.getPath());
//                    results.add(mark1);
//                } else {
//                    if(FileUtil.copyRaw2Dir(mmContext, R.raw.watermark_black, file1)) {
//                        mark1.setIconUrl(file1.getAbsolutePath());
//                        results.add(mark1);
//                    }
//                }
//                if(file2.exists()) {
//                    mark2.setIconUrl(file2.getAbsolutePath());
//                    results.add(mark2);
//                } else {
//                    if(FileUtil.copyRaw2Dir(mmContext, R.raw.pic_watermark_kiss, file2)) {
//                        mark2.setIconUrl(file2.getAbsolutePath());
//                        results.add(mark2);
//                    }
//                }
//            }
//            return results;
//        }
//
//        @Override
//        protected void onPostExecute(List<Watermark> results) {
//            if(isCancelled()) {
//                return;
//            }
//            mAdapter.clear();
//            mAdapter.addAll(results);
//            super.onPostExecute(results);
//        }
//    }

    /**
     * 加水印异步线程
     */
    private class AddWatermarkTask extends AsyncTask<Watermark, Integer, Map<String, String>> {

        private static final String KEY_VIDEO = "video";
        private static final String KEY_THUMB = "thumb";
        private static final String KEY_RET = "ret";

        private IFFmpegService mmService;
        private boolean mmServiceConnected = false;

        @Override
        protected void onPreExecute() {
            mPbEditing.setVisibility(View.VISIBLE);
            mTitlebar.setRightButtonEnabled(false);
            super.onPreExecute();
        }

        @Override
        protected Map<String, String> doInBackground(Watermark... params) {
            if(null == params || params.length < 1) {
                return null;
            }

            Map<String, String> result = new HashMap<String, String>(3);
            Watermark watermark = params[0];

            Watermark.WatermarkData data = watermark.getUserData();
            List<Watermark.ElementData> elements = data.getElements();
            if(elements.isEmpty()) {
                return null;
            }
            Watermark.ElementData element = elements.get(0);
            if(StringUtil.isEmpty(element.getDefaultValue())) {
                return null;
            }
            Watermark.Rect rect = element.getRect();
            String filterStr = null;
            if(rect.getWidth() > 480 || rect.getHeight() > 480) {
                filterStr = "[1]scale=w=480:h=480[wm];[0][wm]overlay=x=0:y=0, crop=x=0:y=0:w=480:h=480";
            } else {
                filterStr = "overlay=x=" + rect.getX() + ":y=" + rect.getY();
            }
            if(StringUtil.isEmpty(filterStr)) {
                return null;
            }

            ServiceConnection conn = new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    mmService = IFFmpegService.Stub.asInterface(service);
                    mmServiceConnected = true;
                }

                @Override
                public void onServiceDisconnected(ComponentName name) {
                    mmService = null;
                    mmServiceConnected = false;
                }
            };
            bindService(new Intent(VideoEditActivity.this, FFmpegService.class), conn,
                    Service.BIND_AUTO_CREATE);

            publishProgress(10);
            do {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } while(!mmServiceConnected && !isCancelled());


            File tempFileDir = new File(CamcorderUtil.getExternalFilesDir(VideoEditActivity.this), CamcorderConfig.TEMP_FOLDER);
            String outputVideoPath = CamcorderUtil.createVideoFilename(tempFileDir.getPath());


            File outputFile = new File(outputVideoPath);
            if(outputFile.exists()) {
                outputFile.delete();
            }

            int ret = Integer.MIN_VALUE;
            try {
                String [] args = new String [] {"ffmpeg", "-y", "-i", mVideoPath, "-i", element.getDefaultValue(),
                        "-filter_complex", filterStr,
                        "-c:v", "mpeg4", "-c:a", "copy", "-b:v", "1000k", "-s", "480x480",
                        "-f", "mp4", outputVideoPath, };
                ret = mmService.ffmpeg(args);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            result.put(KEY_RET, String.valueOf(ret));
            if(ret != 0) {
                return result;
            }
            if(mmServiceConnected) {
                unbindService(conn);
            }

            result.put(KEY_VIDEO, outputVideoPath);

            Bitmap bm = ThumbnailUtils.createVideoThumbnail(outputVideoPath, MediaStore.Video.Thumbnails.FULL_SCREEN_KIND);
            if(null == bm) {
                result.put(KEY_THUMB, null);
            } else {
                String outputThumbPath = CamcorderUtil.createImageFilename(tempFileDir.getPath());
                File thumbFile = new File(outputThumbPath);
                if(thumbFile.exists()) {
                    thumbFile.delete();
                }
                try {
                    boolean state = bm.compress(Bitmap.CompressFormat.JPEG, CamcorderConfig.THUMB_QUALITY, new FileOutputStream(thumbFile));
                    if(state && thumbFile.exists()) {
                        result.put(KEY_THUMB, outputThumbPath);
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }

            return result;
        }

        @Override
        protected void onCancelled() {
            mPbEditing.setVisibility(View.GONE);
            super.onCancelled();
        }

        @Override
        protected void onPostExecute(Map<String, String> result) {
            mPbEditing.setVisibility(View.GONE);
            mTitlebar.setRightButtonEnabled(true);
            if(result == null) {
                //添加水印操作失败
            } else {
                String retStr = result.get(KEY_RET);
                int ret = Integer.parseInt(retStr);
                if(ret == 0) {
                    //添加水印操作成功
                    mResultVideoPath = result.get(KEY_VIDEO);
                    mResultThumbPath = result.get(KEY_THUMB);
                }
            }
            super.onPostExecute(result);
        }
    }
}
