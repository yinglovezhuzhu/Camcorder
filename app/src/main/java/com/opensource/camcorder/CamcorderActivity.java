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
import android.app.Dialog;
import android.app.Service;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.ThumbnailUtils;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.os.StatFs;
import android.provider.MediaStore.Video;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.googlecode.javacv.FFmpegFrameRecorder;
import com.googlecode.javacv.cpp.opencv_core.IplImage;
import com.opensource.camcorder.service.FFmpegService;
import com.opensource.camcorder.utils.CamcorderUtil;
import com.opensource.camcorder.utils.LogUtil;
import com.opensource.camcorder.utils.StringUtil;
import com.opensource.camcorder.widget.CamcorderTitlebar;
import com.opensource.camcorder.widget.FocusView;
import com.opensource.camcorder.widget.GridView;
import com.opensource.camcorder.widget.ProgressView;
import com.opensource.camcorder.widget.SettingPopupWindow;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.ShortBuffer;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import static com.googlecode.javacv.cpp.opencv_core.IPL_DEPTH_8U;

/**
 * The Camcorder activity.
 */
public class CamcorderActivity extends NoSearchActivity implements
		View.OnClickListener, CompoundButton.OnCheckedChangeListener, View.OnTouchListener,
        SurfaceHolder.Callback, Camera.PreviewCallback {

    private static final String TAG = "CamcorderActivity";

    private static final int SCREEN_DELAY = 2 * 60 * 1000;

    // The brightness settings used when it is set to automatic in the system.
    // The reason why it is set to 0.7 is just because 1.0 is too bright.
    private static final float DEFAULT_CAMERA_BRIGHTNESS = 0.7f;

    private static final long NO_STORAGE_ERROR = -1L;
    private static final long CANNOT_STAT_ERROR = -2L;
    private static final long LOW_STORAGE_THRESHOLD = 512L * 1024L;

    private static final int STORAGE_STATUS_OK = 0;
    private static final int STORAGE_STATUS_LOW = 1;
    private static final int STORAGE_STATUS_NONE = 2;
    private static final int STORAGE_STATUS_FAIL = 3;

    private static final int CLEAR_SCREEN_DELAY = 4;
    private static final int UPDATE_RECORD_TIME = 5;
    private static final int ENABLE_SHUTTER_BUTTON = 6;
    private static final int UPDATE_PROGRESS = 7;

    private static final long VIDEO_MIN_DURATION = 2 * 1000;
    private static final long VIDEO_MAX_DURATION = 8 * 1000;


    private SurfaceView mVideoPreview;
    private SurfaceHolder mSurfaceHolder = null;

    private CamcorderTitlebar mTitlebar;
    private Button mBtnVideo;
    private Button mBtnImage;
    private ToggleButton mTBtnDelay;
    private ToggleButton mTBtnFocus;
    private Button mBtnDelete;

    private SettingPopupWindow mSettingWindow;

    private GridView mGridView;

    private FocusView mFocusView;

    private boolean mStartPreviewFail = false;

    /** 是否正在录制 **/
    private boolean mRecorderRecording = false;
    /** 录制的开始时间 **/
    private long mRecordStartTime;
    /** 录制的时间 **/
    private long mRecordedDuration = 0L;

    private long mCurrentRecordedDuration = 0L;

    /** 当前录制的视频文件名 **/
    private String mCurrentVideoTempFilename;
    /** 已经录制的文件名队列 **/
    private Stack<String> mVideoTmepFilenames = new Stack<String>();
    /** 最终合成的视频文件名 **/
    private String mVideoFilename;
    /** 合成的视频缩略图 **/
    private String mVideoThumbFilename;

    boolean mPausing = false;
    boolean mPreviewing = false; // True if preview is started.

    private final Handler mHandler = new MainHandler();


    private android.hardware.Camera mCameraDevice;
    private Parameters mParameters;
    // multiple cameras support
    private int mNumberOfCameras;
    private int mCameraId = 0;

    private int mPreviewWidth = 480;
    private int mPreviewHeight = 480;
    private int mVideoWidth = 480;
    private int mVideoHeight = 480;
    private int mPreviewFrameRate = 30;

	//录制视频和保存音频的类
	private volatile NewFFmpegFrameRecorder mFFmpegFrameRecorder;
	//IplImage对象,用于存储摄像头返回的byte[]，以及图片的宽高，depth，channel等
	private IplImage mYUVIplImage = null;
	//视频帧率
	private int mVideoFrameRange = 30;

	//音频录制
	//录制音频的线程
	private Thread mAudioRecordThread;
	/** 音频采样平率Hz **/
	private int mAudioSampleRate = 44100;
    /**  **/
	private boolean mRecordFinished = false;

    private ProgressView mProgressView;
    private LinearLayout mToolbar;

    private DisplayMetrics mDisplayMetrics;

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                Log.w(TAG, "Touch ++ ACTION_DOWN");
                if(mTBtnFocus.isChecked()) {
                    return false; //如果是对焦状态，把触摸事件交给下一级处理
                }
                startRecord();
                break;
            case MotionEvent.ACTION_UP:
                Log.w(TAG, "Touch ++ ACTION_UP");
                if(mTBtnFocus.isChecked()) {
                    return false;//如果是对焦状态，把触摸事件交给下一级处理
                }
                stopRecord();
                break;
            default:
                break;
        }
        return true;
    }


    // This Handler is used to post message back onto the main thread of the
    // application
    private class MainHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {

                case ENABLE_SHUTTER_BUTTON: //Enable shutter button
                    break;

                case CLEAR_SCREEN_DELAY: {
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    break;
                }
                case UPDATE_PROGRESS:
                    if(mRecorderRecording) {
                        mProgressView.setProgress(mProgressView.peekSplit() + (Long) msg.obj);
                    } else {
                        if(!mRecordFinished) {
                            mProgressView.pushSplit(mProgressView.getProgress());
                        }
                    }
                    break;
                default:
                    Log.v(TAG, "Unhandled message: " + msg.what);
                    break;
            }
        }
    }

    private void showCameraErrorAndFinish() {
        Resources ress = getResources();
        CamcorderUtil.showFatalErrorAndFinish(CamcorderActivity.this,
                ress.getString(R.string.camera_error_title),
                ress.getString(R.string.cannot_connect_camera));
    }

    /**
     * 重新启动预览（用于切换摄像头等）
     * @return
     */
    private boolean restartPreview() {
        try {
            startPreview();
        } catch (CameraHardwareException e) {
            showCameraErrorAndFinish();
            return false;
        }
        return true;
    }

    @Override
    public void onCreate(Bundle savedStateInstance) {
        super.onCreate(savedStateInstance);

        Window win = getWindow();

        // Overright the brightness settings if it is automatic
        int mode = Settings.System.getInt(
                getContentResolver(),
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
        if (mode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
            WindowManager.LayoutParams winParams = win.getAttributes();
            winParams.screenBrightness = DEFAULT_CAMERA_BRIGHTNESS;
            win.setAttributes(winParams);
        }

        mDisplayMetrics = getResources().getDisplayMetrics();

        mNumberOfCameras = CameraHolder.instance().getNumberOfCameras();

        /*
         * To reduce startup time, we start the preview in another thread.
         * We make sure the preview is started at the end of onCreate.
         */
        Thread startPreviewThread = new Thread(new Runnable() {
            public void run() {
                try {
                    mStartPreviewFail = false;
                    startPreview();
                } catch (CameraHardwareException e) {
                    // In eng build, we throw the exception so that test tool
                    // can detect it and report it
                    if ("eng".equals(Build.TYPE)) {
                        throw new RuntimeException(e);
                    }
                    mStartPreviewFail = true;
                }
            }
        });
        startPreviewThread.start();

        requestWindowFeature(Window.FEATURE_PROGRESS);
        setContentView(R.layout.activity_camcorder);

        initView();


        // don't set mSurfaceHolder here. We have it set ONLY within
        // surfaceCreated / surfaceDestroyed, other parts of the code
        // assume that when it is set, the surface is also set.
        SurfaceHolder holder = mVideoPreview.getHolder();
        holder.addCallback(this);
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            //This constant was deprecated in API level 11.
            //this is ignored, this value is set automatically when needed.
            //so only when API level bellow 11 need to be set.
            holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        }

//        mOrientationListener = new MainOrientationEventListener(CamcorderActivity.this);

        // Make sure preview is started.
        try {
            startPreviewThread.join();
            if (mStartPreviewFail) {
                showCameraErrorAndFinish();
                return;
            }
        } catch (InterruptedException ex) {
            // ignore
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mPausing = false;

        // Start orientation listener as soon as possible because it takes
        // some time to get first orientation.
        mVideoPreview.setVisibility(View.VISIBLE);
        if (!mPreviewing && !mStartPreviewFail) {
            if (!restartPreview()) return;
        }
        keepScreenOnAwhile();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mPausing = true;
        if(mRecorderRecording) {
            stopRecord();
        }
        closeCamera();

        resetScreenOn();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        resetRecorder();
        closeCamera();
    }

    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
        if (!mRecorderRecording) {
            keepScreenOnAwhile();
        }
    }

	@Override
	public void onPreviewFrame(byte[] data, Camera camera) {
		// 一帧帧保存
		/* get video data */
		if (mRecorderRecording && null != mYUVIplImage && null != data) {
			final long frameTime = System.currentTimeMillis() - mRecordStartTime;
            Log.v(TAG, "Record FrameTime:" + frameTime);
			byte[] tempData;
			if(mCameraId == Camera.CameraInfo.CAMERA_FACING_BACK) {
				//FIXME 这里需要判断横竖屏
				tempData = CamcorderUtil.rotateYUV420Degree90(data, mPreviewWidth, mPreviewHeight);
			} else if(mCameraId == Camera.CameraInfo.CAMERA_FACING_FRONT) {
				//FIXME 这里需要判断横竖屏
				tempData = CamcorderUtil.rotateYUV420Degree270(data, mPreviewWidth, mPreviewHeight);
			} else {
				tempData = data;
			}
			if(null == mYUVIplImage) {
				return;
			}
			mYUVIplImage.getByteBuffer().put(tempData);
			Log.v(TAG, "Writing Frame");
			try {
				if(null == mFFmpegFrameRecorder) {
                    mHandler.sendMessage(mHandler.obtainMessage(UPDATE_PROGRESS, mCurrentRecordedDuration));
					return;
				}
				mFFmpegFrameRecorder.setTimestamp(1000 * frameTime);
				mFFmpegFrameRecorder.record(mYUVIplImage);
                mCurrentRecordedDuration = frameTime;
                mHandler.sendMessage(mHandler.obtainMessage(UPDATE_PROGRESS, mCurrentRecordedDuration));
			} catch (FFmpegFrameRecorder.Exception e) {
				Log.v(TAG, e.getMessage());
				e.printStackTrace();
			}
		}
	}


    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        // Make sure we have a surface in the holder before proceeding.
        if (holder.getSurface() == null) {
            Log.d(TAG, "holder.getSurface() == null");
            return;
        }

        mSurfaceHolder = holder;

        if (mPausing) {
            // We're pausing, the screen is off and we already stopped
            // video recording. We don't want to start the camera again
            // in this case in order to conserve power.
            // The fact that surfaceChanged is called _after_ an onPause appears
            // to be legitimate since in that case the lockscreen always returns
            // to portrait orientation possibly triggering the notification.
            return;
        }

        // The mCameraDevice will be null if it is fail to connect to the
        // camera hardware. In this case we will show a dialog and then
        // finish the activity, so it's OK to ignore it.
        if (mCameraDevice == null) {
            return;
        }

        // Set preview display if the surface is being created. Preview was
        // already started.
        if (holder.isCreating()) {
            setPreviewDisplay(holder);
        } else {
            restartPreview();
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mSurfaceHolder = null;
    }

    @Override
    public void onBackPressed() {
        if (mPausing) {
            return;
        }
        if (mRecorderRecording) {
            stopRecord();
            resetRecorder();
        }
        exit();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // Do not handle any key if the activity is paused.
        if (mPausing) {
            return true;
        }
        switch (keyCode) {
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_camcorder_title_button1: //摄像头切换
                switchCamera();
                break;
            case R.id.btn_camcorder_title_button2: //设置
                mSettingWindow.showAsDropDown(mTitlebar.getButton2());
                break;
            case R.id.btn_camcorder_title_left: //取消
                //TODO 取消拍摄
                exit();
                break;
            case R.id.btn_camcorder_title_right: //下一步
                resetRecorder();
                new DealFinishWorkTask().execute();
                break;
            case R.id.btn_camcorder_video: //视频
                break;
            case R.id.btn_camcorder_image: //图片
                break;
            case R.id.btn_camcorder_delete: //删除
                mProgressView.deleteBack(true);
                break;
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()) {
            case R.id.tbtn_camcorder_setting_grid: //网格
                showGridView(isChecked);
                break;
            case R.id.tbtn_camcorder_setting_flash: //闪光
                setVideoFlash(isChecked);
                break;
            case R.id.tbtn_camcorder_delay: //延时
                mTBtnFocus.setChecked(false);
                break;
            case R.id.tbtn_camcorder_focus: //对焦
//                mTBtnFocus.setChecked(false);
                mFocusView.setEnabled(isChecked);
                break;
            default:
                break;
        }
    }

    /**
     * 初始化完成，启动画面录制线程和音频录制线程
     */
    public void prepare() {
        try {
            mFFmpegFrameRecorder.start();
            mAudioRecordThread.start();
        } catch (FFmpegFrameRecorder.Exception e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 开始录制
     */
    private void startRecord() {
        if(mRecordFinished) {
            return;
        }
        if(null == mFFmpegFrameRecorder) {
            initRecorder();
        }
        mRecordStartTime = System.currentTimeMillis();
        mCurrentRecordedDuration = 0L;
        mRecorderRecording = true;
        keepScreenOn();
    }

    /**
     * 停止录制
     */
    private void stopRecord() {
        if(mRecorderRecording) {
            mRecorderRecording = false;
            mHandler.removeMessages(UPDATE_PROGRESS);
            mHandler.sendMessage(mHandler.obtainMessage(UPDATE_PROGRESS, mCurrentRecordedDuration));
            if(mCurrentRecordedDuration > 0) {
                mVideoTmepFilenames.push(mCurrentVideoTempFilename);
                mRecordedDuration += mCurrentRecordedDuration;
                mCurrentRecordedDuration = 0L;
            }
            if(null != mFFmpegFrameRecorder) {
                resetRecorder();
            }
        }
    }

    /**
     * 重置，会释放资源
     */
    private void resetRecorder() {
        if(mRecorderRecording) {
            stopRecord();
        }
        releaseResources();
    }

    /**
     * 释放资源，停止录制视频和音频
     */
    private void releaseResources(){
        mRecorderRecording = false;
        try {
            if(mFFmpegFrameRecorder != null) {
                mFFmpegFrameRecorder.stop();
                mFFmpegFrameRecorder.release();
            }
        } catch (com.googlecode.javacv.FrameRecorder.Exception e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        if(mYUVIplImage != null) {
            mYUVIplImage.release();
        }

        mFFmpegFrameRecorder = null;
        mYUVIplImage = null;

        if(mAudioRecordThread != null) {
            if(mAudioRecordThread.isAlive() && !mAudioRecordThread.isInterrupted()) {
                mAudioRecordThread.interrupt();
            }
            mAudioRecordThread = null;
        }

    }



    /**
     * 获取存储卡的状态
     * @param mayHaveSd
     * @return
     */
    private int getStorageStatus(boolean mayHaveSd) {
        long remaining = mayHaveSd ? getAvailableStorage() : NO_STORAGE_ERROR;
        if (remaining == NO_STORAGE_ERROR) {
            return STORAGE_STATUS_NONE;
        } else if (remaining == CANNOT_STAT_ERROR) {
            return STORAGE_STATUS_FAIL;
        }
        return remaining < LOW_STORAGE_THRESHOLD
                ? STORAGE_STATUS_LOW
                : STORAGE_STATUS_OK;
    }

    private void setPreviewDisplay(SurfaceHolder holder) {
        try {
            mCameraDevice.setPreviewDisplay(holder);
        } catch (Throwable ex) {
            closeCamera();
            throw new RuntimeException("setPreviewDisplay failed", ex);
        }
    }

    /**
     * 开启摄像头预览
     * @throws CameraHardwareException
     */
    private void startPreview() throws CameraHardwareException {
        Log.v(TAG, "startPreview");
        if (mCameraDevice == null) {
            // If the activity is paused and resumed, camera device has been
            // released and we need to open the camera.
            mCameraDevice = CameraHolder.instance().open(mCameraId);
        }

        if (mPreviewing) {
            mCameraDevice.stopPreview();
            mPreviewing = false;
        }
        setPreviewDisplay(mSurfaceHolder);
        CamcorderUtil.setCameraDisplayOrientation(this, mCameraId, mCameraDevice);

        setCameraParameters();

        try {
            mCameraDevice.startPreview();
            mPreviewing = true;
        } catch (Throwable ex) {
            closeCamera();
            throw new RuntimeException("startPreview failed", ex);
        }

        //Add preview clallback
        mCameraDevice.setPreviewCallback(this);

    }

    /**
     * 关闭相机
     */
    private void closeCamera() {
        Log.v(TAG, "closeCamera");
        if (mCameraDevice == null) {
            Log.d(TAG, "already stopped.");
            return;
        }
        mCameraDevice.setPreviewCallback(null);
        // If we don't lock the camera, release() will fail.
        mCameraDevice.lock();
        CameraHolder.instance().release();
        mCameraDevice = null;
        mPreviewing = false;
    }

    /**
     * Returns
     *
     * @return number of bytes available, or an ERROR code.
     */
    private static long getAvailableStorage() {
        try {
            if (!ImageManager.hasStorage()) {
                return NO_STORAGE_ERROR;
            } else {
                String storageDirectory =
                        Environment.getExternalStorageDirectory().toString();
                StatFs stat = new StatFs(storageDirectory);
                return (long) stat.getAvailableBlocks()
                        * (long) stat.getBlockSize();
            }
        } catch (Exception ex) {
            // if we can't stat the filesystem then we don't know how many
            // free bytes exist. It might be zero but just leave it
            // blank since we really don't know.
            Log.e(TAG, "Fail to access sdcard", ex);
            return CANNOT_STAT_ERROR;
        }
    }

    /**
     * 切换摄像头，并且重新开启预览
     * @param cameraId 摄像头id
     * @see {@link android.hardware.Camera.CameraInfo#CAMERA_FACING_BACK}
     * @see {@link android.hardware.Camera.CameraInfo#CAMERA_FACING_FRONT}
     */
    private void switchCameraId(int cameraId) {
        if (mPausing) {
        	return;
        }
        mCameraId = cameraId;

        closeCamera();

        restartPreview();
    }

    /**
     * 切换摄像头</br>
     * 如果当前是后置，则切换为前置；当前是前置，则切换为后置
     */
    private void switchCamera() {
        if(mNumberOfCameras < 2) {
            return; //如果只有一个摄像头，什么也不做
        }
        if(mCameraId == Camera.CameraInfo.CAMERA_FACING_BACK) {
            switchCameraId(Camera.CameraInfo.CAMERA_FACING_FRONT);
        } else if(mCameraId == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            switchCameraId(Camera.CameraInfo.CAMERA_FACING_BACK);
        }
    }

    private static void fadeIn(View view) {
        view.setVisibility(View.VISIBLE);
        Animation animation = new AlphaAnimation(0F, 1F);
        animation.setDuration(500);
        view.startAnimation(animation);
    }

    private static void fadeOut(View view) {
        view.setVisibility(View.INVISIBLE);
        Animation animation = new AlphaAnimation(1F, 0F);
        animation.setDuration(500);
        view.startAnimation(animation);
    }

    private void resetScreenOn() {
        mHandler.removeMessages(CLEAR_SCREEN_DELAY);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private void keepScreenOnAwhile() {
        mHandler.removeMessages(CLEAR_SCREEN_DELAY);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mHandler.sendEmptyMessageDelayed(CLEAR_SCREEN_DELAY, SCREEN_DELAY);
    }

    private void keepScreenOn() {
        mHandler.removeMessages(CLEAR_SCREEN_DELAY);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    /**
     * Whether the setting value is supported.
     * @param value setting value.
     * @param supported the camera supported values.
     * @return
     */
    private static boolean isSupported(String value, List<String> supported) {
        return supported == null ? false : supported.indexOf(value) >= 0;
    }

    /**
     * Set camera setting parameters.
     */
    private void setCameraParameters() {
        mParameters = mCameraDevice.getParameters();

        Camera.Size previewSize = getDefaultPreviewSize(mParameters);

        //获取计算过的摄像头分辨率
        if(previewSize != null ){
            mPreviewWidth = previewSize.width;
            mPreviewHeight = previewSize.height;
        } else {
            mPreviewWidth = 480;
            mPreviewHeight = 480;
        }
        mParameters.setPreviewSize(mPreviewWidth, mPreviewHeight);
        //将获得的Preview Size中的最小边作为视频的大小
        mVideoWidth = mPreviewWidth > mPreviewHeight ? mPreviewHeight : mPreviewWidth;
        mVideoHeight = mPreviewWidth > mPreviewHeight ? mPreviewHeight : mPreviewWidth;
        if(mFFmpegFrameRecorder != null) {
            mFFmpegFrameRecorder.setImageWidth(mVideoWidth);
            mFFmpegFrameRecorder.setImageHeight(mVideoHeight);
        }

        mParameters.setPreviewFrameRate(mPreviewFrameRate);

        List<String> supportedFocusMode = mParameters.getSupportedFocusModes();
        if(isSupported(Camera.Parameters.FOCUS_MODE_AUTO, supportedFocusMode)) {
            mParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        }


        mCameraDevice.setParameters(mParameters);

        // 设置闪光灯，默认关闭
        setVideoFlash(false);

        //是否支持打开/关闭闪光灯
        mSettingWindow.setFlashEnabled(isSupportedVideoFlash());

        mTBtnFocus.setEnabled(isSupportedFocus());

        layoutPreView();
    }

    /**
     * 获取默认的Preview Size，640x480或者是所支持的Preview Size中最接近640x480的一个
     * @param parameters
     * @return
     */
    private Camera.Size getDefaultPreviewSize(Camera.Parameters parameters) {
        if(null == parameters) {
            return null;
        }
        Camera.Size previewSize = null;
        //获取摄像头的所有支持的分辨率
        List<Camera.Size> supportedPreviewSizes = parameters.getSupportedPreviewSizes();
        if(null != supportedPreviewSizes && !supportedPreviewSizes.isEmpty()) {
            for (Camera.Size size : supportedPreviewSizes) {
                Log.w(TAG, "PreviewSize: width=" + size.width + "<>height=" + size.height);
            }
            Collections.sort(supportedPreviewSizes, new SizeComparator());
            //如果摄像头支持640*480，那么强制设为640*480
            for (Camera.Size size : supportedPreviewSizes) {
                if (size.width == 640 && size.height == 480) {
                    previewSize = size;
                    break;
                }
            }
            //如果摄像头不支持640x480，那么设置为最接近640x480的那个
            if (null == previewSize) {
                previewSize = supportedPreviewSizes.get(0);
                int widthDiffer = Math.abs(previewSize.width - 640);
                for (int i = 1; i < supportedPreviewSizes.size(); i++) {
                    Camera.Size size = supportedPreviewSizes.get(i);
                    int widthDiffer2 = Math.abs(size.width - 640);
                    if (widthDiffer > widthDiffer2) {
                        previewSize = size;
                    }
                }
            }
        }
        return previewSize;
    }

    /******* 闪光灯相关 ****************************************************************************/

    /**
     * 设置录像模式闪光灯
     */
    private boolean setVideoFlash(boolean isOn) {
        if(null == mCameraDevice) {
            return false;
        }
        List<String> supportedFlash = mParameters.getSupportedFlashModes();
        String flashMode = isOn ? Parameters.FLASH_MODE_TORCH : Parameters.FLASH_MODE_OFF;
        if (isSupported(flashMode, supportedFlash)) {
            mParameters.setFlashMode(flashMode);
            mCameraDevice.setParameters(mParameters);
            // Keep preview size up to date.
            mParameters = mCameraDevice.getParameters();
            return true;
        }
        return false;
    }

    /**
     * 是否支持视频模式的闪光灯（视频模式闪光灯是指在preview的过程中都亮的模式）
     * @see {@link android.hardware.Camera.Parameters#FLASH_MODE_TORCH}
     * @return true说明支持
     */
    private boolean isSupportedVideoFlash() {
        List<String> supportedFlash = mParameters.getSupportedFlashModes();
        String flashMode = Parameters.FLASH_MODE_TORCH;
        return isSupported(flashMode, supportedFlash);
    }

    /**********************************************************************************************/


    /******* 对焦相关 ××××**************************************************************************/

    /**
     * 手动对焦
     */
    private void startFocus() {
        if(null == mCameraDevice) {
            return;
        }
        if(isSupportedFocus()) {
            List<String> supportedFocusMode = mParameters.getSupportedFocusModes();
            if(isSupported(Parameters.FOCUS_MODE_AUTO, supportedFocusMode)) {
                mParameters.setFocusMode(Parameters.FOCUS_MODE_AUTO);
            } else if(isSupported(Parameters.FOCUS_MODE_MACRO, supportedFocusMode)) {
                mParameters.setFocusMode(Parameters.FOCUS_MODE_MACRO);
            }
            mCameraDevice.setParameters(mParameters);
            mCameraDevice.autoFocus(new Camera.AutoFocusCallback() {
                @Override
                public void onAutoFocus(boolean success, Camera camera) {
                    //TODO 成功对焦后要做的事情
                    System.out.println("Focus success...................................................." + success);
                }
            });
        }
    }

    /**
     * 是否支持手动对焦
     * @return
     */
    private boolean isSupportedFocus() {
        List<String> supportedFocusMode = mParameters.getSupportedFocusModes();
        return isSupported(Parameters.FOCUS_MODE_AUTO, supportedFocusMode) || isSupported(Parameters.FOCUS_MODE_MACRO, supportedFocusMode);
    }

    /**********************************************************************************************/

    /** 初始化View、 View相关 ***********************************************************************/
    private void initView() {

        initTitleBar();

        mProgressView = (ProgressView) findViewById(R.id.pv_recorder_progress);

        mVideoPreview = (SurfaceView) findViewById(R.id.sv_recorder_preview);

        mGridView = (GridView) findViewById(R.id.gv_recorder_grid);

        mFocusView = (FocusView) findViewById(R.id.fv_recorder_focus);

        mToolbar = (LinearLayout) findViewById(R.id.ll_recorder_toolbar);
        mBtnVideo = (Button) findViewById(R.id.btn_camcorder_video);
        mBtnImage = (Button) findViewById(R.id.btn_camcorder_image);
        mTBtnDelay = (ToggleButton) findViewById(R.id.tbtn_camcorder_delay);
        mTBtnFocus = (ToggleButton) findViewById(R.id.tbtn_camcorder_focus);
        mBtnDelete = (Button) findViewById(R.id.btn_camcorder_delete);

        mVideoPreview.setOnTouchListener(this);
        mBtnVideo.setOnClickListener(this);
        mBtnImage.setOnClickListener(this);
        mTBtnDelay.setOnCheckedChangeListener(this);
        mTBtnFocus.setOnCheckedChangeListener(this);
        mBtnDelete.setOnClickListener(this);

        mBtnVideo.setEnabled(false);
        mBtnImage.setEnabled(false);
        mTBtnDelay.setEnabled(false);
        mTBtnFocus.setEnabled(true);
        mBtnDelete.setEnabled(false);

        mProgressView.setMaxProgress(VIDEO_MAX_DURATION); //八秒毫秒值8000
        mProgressView.setMinMask(VIDEO_MIN_DURATION); //最小两秒毫秒值2000
        mProgressView.setOnDeleteListener(new ProgressView.OnDeleteListener() {

            @Override
            public void onConfirm(float lastProgress, float progress) {

            }

            @Override
            public void onDelete(float lastProgress, float progress) {
                mRecordedDuration = (long)lastProgress;
                if(mVideoTmepFilenames.isEmpty()) {
                    return;
                }
                String tempFilename = mVideoTmepFilenames.pop();
                File tempFile = new File(tempFilename);
                if(tempFile.exists()) {
                    tempFile.delete();
                }
            }
        });
        mProgressView.setOnProgressUpdateListener(new ProgressView.OnProgressUpdateListener() {

            @Override
            public void onProgressUpdate(float max, float progress) {
                mTitlebar.setRightButtonEnabled(progress >= VIDEO_MIN_DURATION);
                mBtnDelete.setEnabled(progress > 0);
                mRecordFinished = progress >= max;
                if(mRecordFinished) {
                    stopRecord();
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {

                            new DealFinishWorkTask().execute();
                        }
                    }, 250);
                }
            }
        });

        initSettingPopWindow(); //初始化设置弹出框

        showGridView(mSettingWindow.isGridChecked());


        ViewGroup.LayoutParams focusViewLayoutParams = mFocusView.getLayoutParams();
        focusViewLayoutParams.width = mDisplayMetrics.widthPixels;
        focusViewLayoutParams.height = mPreviewWidth * mDisplayMetrics.widthPixels / mPreviewHeight;
        mFocusView.setEnabled(mTBtnFocus.isChecked());
        mFocusView.setOnFocusListener(new FocusView.OnFocusListener() {
            @Override
            public void onFocus(View view, float x, float y) {
                //TODO 对焦
                if(null == mCameraDevice) {
                    return;
                }
                mCameraDevice.autoFocus(new Camera.AutoFocusCallback() {

                    @Override
                    public void onAutoFocus(boolean success, Camera camera) {
                        LogUtil.i(TAG, "Focus success.............................. " + success);
                        if(success) {
                            mFocusView.focusSuccessed();
                        } else {
                            mFocusView.focusFailed();
                        }
                    }
                });
            }
        });

    }

    /**
     * 初始化标题栏
     */
    private void initTitleBar() {
        mTitlebar = (CamcorderTitlebar) findViewById(R.id.tb_camcorder);
        mTitlebar.setLeftButton(R.string.cancel, this);
        mTitlebar.setRightButton(R.string.next_step, this);
        mTitlebar.setButton1(R.drawable.selector_ic_change_camera, 0, this);
        mTitlebar.setButton2(R.drawable.selector_ic_camera_setting, R.drawable.ic_arrow_down_right, this);
        mTitlebar.setRightButtonEnabled(false);
        mTitlebar.setButton1Enabled(mNumberOfCameras > 1);
    }

    /**
     * 初始化设置弹出框
     */
    private void initSettingPopWindow() {
        mSettingWindow = new SettingPopupWindow(this);
        mSettingWindow.setGridCheckChangedListener(this);
        mSettingWindow.setFlashCheckChangedListener(this);
    }

    /**
     * 计算布局预览View
     */
    private void layoutPreView() {
        ViewGroup.LayoutParams videoPreviewLayoutParams = mVideoPreview.getLayoutParams();
        videoPreviewLayoutParams.width = mDisplayMetrics.widthPixels;
        videoPreviewLayoutParams.height = mPreviewWidth * mDisplayMetrics.widthPixels / mPreviewHeight;

        FrameLayout.LayoutParams toolbarLayoutParams = (FrameLayout.LayoutParams) mToolbar.getLayoutParams();
        toolbarLayoutParams.topMargin = mVideoHeight * mDisplayMetrics.widthPixels / mPreviewHeight;
    }

    /**
     * 显示网格
     */
    private void showGridView(boolean isShow) {
        if(isShow) {
            mGridView.setVisibility(View.VISIBLE);
            DisplayMetrics dm = getResources().getDisplayMetrics();
            ViewGroup.LayoutParams gridViewLayoutParams = mGridView.getLayoutParams();
            if(gridViewLayoutParams == null) {
                gridViewLayoutParams = new ViewGroup.LayoutParams(dm.widthPixels, mVideoHeight * dm.widthPixels / mPreviewHeight);
            } else {
                gridViewLayoutParams.width = dm.widthPixels;
                gridViewLayoutParams.height = mVideoHeight * dm.widthPixels / mPreviewHeight;
            }
            mGridView.setLayoutParams(gridViewLayoutParams);
            mGridView.invalidate();
        } else {
            mGridView.setVisibility(View.INVISIBLE);
        }
    }

    /**********************************************************************************************/


	//当前录制的质量，会影响视频清晰度和文件大小
	private int currentResolution = CamcorderConfig.RESOLUTION_MEDIUM_VALUE;


	/**
	 * 初始化Recorder
	 */
	private void initRecorder() {
        mCurrentVideoTempFilename = CamcorderUtil.createVideoFilename(new File(CamcorderApp.APP_FOLDER, CamcorderConfig.VIDEO_FOLDER).getAbsolutePath());

		CamcorderParameters recorderParameters = CamcorderUtil.getRecorderParameter(currentResolution);
		mAudioSampleRate = recorderParameters.getAudioSamplingRate();
		mVideoFrameRange = recorderParameters.getVideoFrameRate();

		mFFmpegFrameRecorder = new NewFFmpegFrameRecorder(mCurrentVideoTempFilename, mVideoWidth, mVideoHeight, 1);
		mFFmpegFrameRecorder.setFormat(recorderParameters.getVideoOutputFormat());
		mFFmpegFrameRecorder.setSampleRate(recorderParameters.getAudioSamplingRate());
		mFFmpegFrameRecorder.setFrameRate(recorderParameters.getVideoFrameRate());
		mFFmpegFrameRecorder.setVideoCodec(recorderParameters.getVideoCodec());
		mFFmpegFrameRecorder.setVideoQuality(recorderParameters.getVideoQuality());
		mFFmpegFrameRecorder.setAudioQuality(recorderParameters.getVideoQuality());
		mFFmpegFrameRecorder.setAudioCodec(recorderParameters.getAudioCodec());
		mFFmpegFrameRecorder.setVideoBitrate(recorderParameters.getVideoBitrate());
		mFFmpegFrameRecorder.setAudioBitrate(recorderParameters.getAudioBitrate());


        //如果YUVIplImage已经存在，释放它
        if(null != mYUVIplImage) {
            mYUVIplImage.release();
            mYUVIplImage = null;
        }
        mYUVIplImage = IplImage.create(mPreviewHeight, mPreviewWidth,IPL_DEPTH_8U, 2);

        //如果音频录制线程正在运行，则中断它
        if(null != mAudioRecordThread && mAudioRecordThread.isAlive() && !mAudioRecordThread.isInterrupted()) {
            mAudioRecordThread.interrupt();
            mAudioRecordThread = null;
        }
		mAudioRecordThread = new AudioRecordThread();

        prepare();
	}

    /**
     * 删除本次拍摄中的所有视频文件和缩略图文件
     */
    private void clearFiles() {
        if(null != mVideoFilename) {
            File file = new File(mVideoFilename);
            if(file.exists()) {
                if(!file.delete()) {
                    LogUtil.w(TAG, "Could not delete file:" + file.getAbsolutePath());
                }
            }
        }
        if(!StringUtil.isEmpty(mVideoThumbFilename)) {
            File file = new File(mVideoThumbFilename);
            if(file.exists()) {
                if(!file.delete()) {
                    LogUtil.w(TAG, "Could not delete file:" + file.getAbsolutePath());
                }
            }
        }
        while(!mVideoTmepFilenames.isEmpty()) {
            String videoFile = mVideoTmepFilenames.pop();
            File file = new File(videoFile);
            if(file.exists()) {
                if(!file.delete()) {
                    LogUtil.w(TAG, "Could not delete file:" + file.getAbsolutePath());
                }
            }
        }
    }

    /**
     * 退出拍摄
     */
    private void exit() {
        if(mRecordedDuration > 0L) {
            new AlertDialog.Builder(this)
                    .setTitle("提示")
                    .setMessage("确定放弃本视频？")
                    .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                   clearFiles();
                                }
                            });
                            finish();
                        }
                    })
                    .setNegativeButton("取消", new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    }).show();
        } else {
            finish();
        }
    }


    public class DealFinishWorkTask extends AsyncTask<String, Integer, Map<String, String>> {
        private static final String KEY_VIDEO = "video";
        private static final String KEY_THUMB = "thumb";
        private static final String KEY_RET = "ret";
        private Dialog mmDialog;
        private ProgressBar mmProgressBar;
        private TextView mmTvProgress;
        private IFFmpegService mmService;
        private boolean mmServiceConnected = false;

        @Override
        protected void onPreExecute() {
            //创建处理进度条
            mmDialog= new Dialog(CamcorderActivity.this,R.style.DialogLoadingNoDim);
			Window dialogWindow = mmDialog.getWindow();
			WindowManager.LayoutParams lp = dialogWindow.getAttributes();
			lp.width = (int) (getResources().getDisplayMetrics().density*240);
			lp.height = (int) (getResources().getDisplayMetrics().density*80);
			lp.gravity = Gravity.CENTER;
			dialogWindow.setAttributes(lp);
            mmDialog.setCanceledOnTouchOutside(false);
            mmDialog.setContentView(R.layout.layout_deal_progress);

            mmTvProgress = (TextView) mmDialog.findViewById(R.id.recorder_progress_progresstext);
            mmProgressBar = (ProgressBar) mmDialog.findViewById(R.id.recorder_progress_progressbar);
            mmDialog.show();
            super.onPreExecute();

        }

        @Override
        protected Map<String, String> doInBackground(String... params) {

            Map<String, String> result = new HashMap<String, String>(3);
            publishProgress(5);

            if(mVideoTmepFilenames.isEmpty()) {
                result.put(KEY_VIDEO, null);
                result.put(KEY_THUMB, null);
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
            bindService(new Intent(CamcorderActivity.this, FFmpegService.class), conn,
                    Service.BIND_AUTO_CREATE);


            do {
                publishProgress(10);
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } while(!mmServiceConnected && !isCancelled());

            if(isCancelled()) {
                //取消操作
                publishProgress(100);
                return null;
            }

            publishProgress(20);

            mVideoFilename = CamcorderUtil.createVideoFilename(new File(CamcorderApp.APP_FOLDER, CamcorderConfig.TEMP_FOLDER).getAbsolutePath());
            if(mVideoTmepFilenames.size() == 1) { //如果只有一个视频文件，直接拷贝一个副本
                try {
                    File sourceFile = new File(mVideoTmepFilenames.peek());
                    FileInputStream fis = new FileInputStream(sourceFile);
                    FileOutputStream fos = new FileOutputStream(new File(mVideoFilename));
                    publishProgress(30);
                    byte [] buffer = new byte[2048];
                    int length = 0;
                    while ((length = fis.read(buffer)) != -1) {
                        fos.write(buffer, 0, length);
                    }
                    publishProgress(50);
                    fos.flush();
                    fos.close();
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    File videoFile = new File(mVideoFilename);
                    String [] files = new String[mVideoTmepFilenames.size()];
                    mVideoTmepFilenames.toArray(files);
                    publishProgress(25);
                    int ret = mmService.mergeVideo(videoFile.getParent(),
                            mVideoFilename,
                            files);
                    LogUtil.i(TAG, "Merge video result:++>>> " + ret);
                    result.put(KEY_RET, String.valueOf(ret));
                    if(ret != 0) { //合并视频失败
                        publishProgress(100);
                        return result;
                    }

                    if(mmServiceConnected) {
                        unbindService(conn);
                    }
                } catch (RemoteException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }

            publishProgress(60);

            if(StringUtil.isEmpty(mVideoFilename)) {
                result.put(KEY_VIDEO, null);
                result.put(KEY_THUMB, null);
            }
            result.put(KEY_VIDEO, mVideoFilename);
            publishProgress(65);
            Bitmap bm = ThumbnailUtils.createVideoThumbnail(mVideoFilename, Video.Thumbnails.FULL_SCREEN_KIND);
            if(null == bm) {
                publishProgress(85);
                result.put(KEY_THUMB, null);
            } else {
                publishProgress(75);
                mVideoThumbFilename = CamcorderUtil.createImageFilename(new File(CamcorderApp.APP_FOLDER, CamcorderConfig.THUMB_FOLDER).getAbsolutePath());
                File thumbFile = new File(mVideoThumbFilename);
                try {
                    boolean state = bm.compress(Bitmap.CompressFormat.JPEG, CamcorderConfig.THUMB_QUALITY, new FileOutputStream(thumbFile));
                    publishProgress(90);
                    if(state && thumbFile.exists()) {
                    result.put(KEY_THUMB, thumbFile.getAbsolutePath());
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
            return result;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            mmProgressBar.setProgress(values[0]);
            mmTvProgress.setText(values[0] + "%");
        }

        @Override
        protected void onPostExecute(Map<String, String> results) {

            publishProgress(100);
            mmDialog.cancel();

            //TODO 处理完成,跳转至编辑界面
            if(null != results) {
                Intent intent = new Intent(CamcorderActivity.this, VideoEditActivity.class);
                intent.putExtra(CamcorderConfig.EXTRA_VIDEO, results.get(KEY_VIDEO));
                intent.putExtra(CamcorderConfig.EXTRA_THUMB, results.get(KEY_THUMB));
                startActivity(intent);
            }
            super.onPostExecute(results);
        }
    }

    public static class SizeComparator implements Comparator<Size> {
        @Override
        public int compare(Camera.Size size1, Camera.Size size2) {
            if (size1.height != size2.height)
                return size1.height - size2.height;
            else
                return size1.width - size2.width;
        }
    }


    /**
     * 录制音频的线程
     */
    private class AudioRecordThread extends Thread {

        @Override
        public void run() {
            super.run();
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);

            int bufferSize = AudioRecord.getMinBufferSize(mAudioSampleRate, AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT);

             /* audio data getting thread */
            AudioRecord audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, mAudioSampleRate,
                    AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize);
            short [] audioData = new short[bufferSize];
            int bufferReadResult;

            if(AudioRecord.STATE_UNINITIALIZED == audioRecord.getState()) {
                audioRecord.release();
                return;
            }

            Log.d(TAG, "audioRecord.prepare()");
            audioRecord.startRecording();

			/* ffmpeg_audio encoding loop */
            while (!mRecordFinished && !interrupted()) {
                // Log.v(LOG_TAG,"recording? " + recording);
                bufferReadResult = audioRecord.read(audioData, 0, audioData.length);
                if (bufferReadResult > 0) {
                    // Log.v(LOG_TAG, "mmBufferReadResult: " + mmBufferReadResult);
                    // If "recording" isn't true when start this thread, it
                    // never get's set according to this if statement...!!!
                    // Why? Good question...
                    if (mRecorderRecording) {
                        try {
                            Buffer[] barray = new Buffer[1];
                            barray[0] = ShortBuffer.wrap(audioData, 0, bufferReadResult);
                            mFFmpegFrameRecorder.record(barray);
                            // Log.v(LOG_TAG,"recording " + 1024*i + " to " +
                            // 1024*i+1024);
                        } catch (FFmpegFrameRecorder.Exception e) {
                            Log.v(TAG, e.getMessage());
                            e.printStackTrace();
                        }
                    }
                }
            }
            Log.v(TAG, "AudioThread Finished, release audioRecord");

			/* encoding finish, release audio recorder */
            if (audioRecord != null) {
                audioRecord.stop();
                audioRecord.release();
                Log.v(TAG, "audioRecord released");
            }
        }

    }

}
