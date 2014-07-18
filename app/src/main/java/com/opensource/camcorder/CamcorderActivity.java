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
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.StatFs;
import android.os.SystemClock;
import android.provider.MediaStore.Video;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
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
import android.widget.Toast;
import android.widget.ToggleButton;

import com.googlecode.javacv.FFmpegFrameRecorder;
import com.googlecode.javacv.cpp.opencv_core.IplImage;
import com.opensource.camcorder.utils.StringUtil;
import com.opensource.camcorder.widget.CamcorderTitlebar;
import com.opensource.camcorder.widget.ProgressView;
import com.opensource.camcorder.widget.SettingPopupWindow;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.nio.Buffer;
import java.nio.ShortBuffer;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

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


    private boolean mStartPreviewFail = false;

    private int mStorageStatus = STORAGE_STATUS_OK;

    /** 是否正在录制 **/
    private boolean mRecorderRecording = false;
    /** 录制的开始时间 **/
    private long mRecordStartTime;
    /** 录制的时间 **/
    private long mRecordedDuration = 0L;

    // The video file that the hardware camera is about to startRecord into
    // (or is recording into.)
    private String mVideoFilename;


    private ParcelFileDescriptor mVideoFileDescriptor;
    // The video file that has already been recorded, and that is being
    // examined by the user.
    private String mCurrentVideoFilename;
    private Uri mCurrentVideoUri;
    private ContentValues mCurrentVideoValues;


    // The video duration limit. 0 menas no limit.
    private int mMaxVideoDurationInMs;

    boolean mPausing = false;
    boolean mPreviewing = false; // True if preview is started.

    private ContentResolver mContentResolver;

    private boolean mRecordingTimeCountsDown = false;

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


//    private MainOrientationEventListener mOrientationListener;
    // The device orientation in degrees. Default is unknown.
    private int mOrientation = OrientationEventListener.ORIENTATION_UNKNOWN;
    // The orientation compensation for icons and thumbnails. Degrees are in
    // counter-clockwise
    private int mOrientationCompensation = 0;
    private int mOrientationHint; // the orientation hint for video playback



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

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                Log.w(TAG, "Touch ++ ACTION_DOWN");
                startRecord();
                break;
            case MotionEvent.ACTION_UP:
                Log.w(TAG, "Touch ++ ACTION_UP");
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

                case UPDATE_RECORD_TIME: {
                    updateRecordingTime();
                    break;
                }
                case UPDATE_PROGRESS:
                    if(mRecorderRecording) {
                        long time = (Long) msg.obj;
                        if(time < VIDEO_MAX_DURATION) {
                            mProgressView.setProgress((Long) msg.obj);
                        } else {
                            mProgressView.setProgress(VIDEO_MAX_DURATION);
                            stopRecord();
                        }
                    } else {
                        if(mProgressView.getProgress() < mProgressView.getMaxProgress()) {
                            mProgressView.pushSplit(mProgressView.getProgress());
                        }
                    }
                    if(mProgressView.getProgress() >= VIDEO_MIN_DURATION) {
                        mTitlebar.setRightButtonEnabled(true);
                    } else {
                        mTitlebar.setRightButtonEnabled(false);
                    }
                    break;
                default:
                    Log.v(TAG, "Unhandled message: " + msg.what);
                    break;
            }
        }
    }

    private BroadcastReceiver mReceiver = null;

    private class MyBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_MEDIA_EJECT)) {
                updateAndShowStorageHint(false);
//                stopVideoRecording();
            } else if (action.equals(Intent.ACTION_MEDIA_MOUNTED)) {
                updateAndShowStorageHint(true);
            } else if (action.equals(Intent.ACTION_MEDIA_UNMOUNTED)) {
                // SD card unavailable
                // handled in ACTION_MEDIA_EJECT
            } else if (action.equals(Intent.ACTION_MEDIA_SCANNER_STARTED)) {
                Toast.makeText(CamcorderActivity.this,
                        getResources().getString(R.string.wait), Toast.LENGTH_LONG).show();
            } else if (action.equals(Intent.ACTION_MEDIA_SCANNER_FINISHED)) {
                updateAndShowStorageHint(true);
            }
        }
    }

    /**
     * 生成一个文件名称
     * @param dateTaken
     * @return
     */
    private String createName(long dateTaken) {
        Date date = new Date(dateTaken);
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                getString(R.string.video_file_name_format));

        return dateFormat.format(date);
    }

    private void showCameraErrorAndFinish() {
        Resources ress = getResources();
        Util.showFatalErrorAndFinish(CamcorderActivity.this,
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

        mNumberOfCameras = CameraHolder.instance().getNumberOfCameras();
//        if(mNumberOfCameras > 1) {
//        	mCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
//        }

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

        mContentResolver = getContentResolver();

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
	public void onPreviewFrame(byte[] data, Camera camera) {
		// TODO 一帧帧保存
		/* get video data */
		if (mRecorderRecording && null != mYUVIplImage && null != data) {
			long frameTime = mRecordedDuration + (System.currentTimeMillis() - mRecordStartTime);
            Log.v(TAG, "Record FrameTime:" + frameTime);
			byte[] tempData;
			if(mCameraId == Camera.CameraInfo.CAMERA_FACING_BACK) {
				//FIXME 这里需要判断横竖屏
				tempData = rotateYUV420Degree90(data, mPreviewWidth, mPreviewHeight);
			} else if(mCameraId == Camera.CameraInfo.CAMERA_FACING_FRONT) {
				//FIXME 这里需要判断横竖屏
				tempData = rotateYUV420Degree270(data, mPreviewWidth, mPreviewHeight);
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
					return;
				}
				mFFmpegFrameRecorder.setTimestamp(1000 * frameTime);
				mFFmpegFrameRecorder.record(mYUVIplImage);
                mHandler.sendMessage(mHandler.obtainMessage(UPDATE_PROGRESS, frameTime));
			} catch (FFmpegFrameRecorder.Exception e) {
				Log.v(TAG, e.getMessage());
				e.printStackTrace();
			}
		}
	}


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
                new DealFinishWorkTask().execute(mVideoFilename);
                break;
            case R.id.btn_camcorder_video: //视频
                break;
            case R.id.btn_camcorder_image: //图片
                break;
            case R.id.btn_camcorder_delete: //删除
                break;
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()) {
            case R.id.tbtn_camcorder_setting_grid: //网格
                break;
            case R.id.tbtn_camcorder_setting_flash: //闪光
                setVideoFlash(isChecked);
                break;
            case R.id.tbtn_camcorder_delay: //延时
                break;
            case R.id.tbtn_camcorder_focus: //对焦
                break;
            default:
                break;
        }
    }


    private void updateAndShowStorageHint(boolean mayHaveSd) {
        mStorageStatus = getStorageStatus(mayHaveSd);
        showStorageHint();
    }

    private void showStorageHint() {
//        String errorMessage = null;
//        switch (mStorageStatus) {
//            case STORAGE_STATUS_NONE:
//                errorMessage = getString(R.string.no_storage);
//                break;
//            case STORAGE_STATUS_LOW:
//                errorMessage = getString(R.string.spaceIsLow_content);
//                break;
//            case STORAGE_STATUS_FAIL:
//                errorMessage = getString(R.string.access_sd_fail);
//                break;
//        }
//        if (errorMessage != null) {
//            if (mStorageHint == null) {
//                mStorageHint = OnScreenHint.makeText(this, errorMessage);
//            } else {
//                mStorageHint.setText(errorMessage);
//            }
//            mStorageHint.show();
//        } else if (mStorageHint != null) {
//            mStorageHint.cancel();
//            mStorageHint = null;
//        }
    }

    /**
     * 初始化完成，启动画面录制线程和音频录制线程
     */
    public void prepare() {
        mRecordFinished = true;
        mRecorderRecording = false;
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
        if(null == mFFmpegFrameRecorder) {
            initRecorder();
        }
        mRecordStartTime = System.currentTimeMillis();
        mRecorderRecording = true;
        mRecordFinished = false;
    }

    /**
     * 停止录制
     */
    private void stopRecord() {
        if(mRecorderRecording) {
            mRecorderRecording = false;
            mRecordedDuration += System.currentTimeMillis() - mRecordStartTime;
            mHandler.sendEmptyMessage(UPDATE_PROGRESS);
        }
    }

    /**
     * 重置，会释放资源
     */
    private void resetRecorder() {
        if(mRecorderRecording) {
            stopRecord();
        }
        mRecordFinished = true;
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
        Util.setCameraDisplayOrientation(this, mCameraId, mCameraDevice);

        setCameraParameters();

//        initRecorder();

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



    @Override
    protected void onResume() {
        super.onResume();
        mPausing = false;

        // Start orientation listener as soon as possible because it takes
        // some time to get first orientation.
//        mOrientationListener.enable();
        mVideoPreview.setVisibility(View.VISIBLE);
        if (!mPreviewing && !mStartPreviewFail) {
            if (!restartPreview()) return;
        }
        keepScreenOnAwhile();

        // install an intent filter to receive SD card related events.
        IntentFilter intentFilter =
                new IntentFilter(Intent.ACTION_MEDIA_MOUNTED);
        intentFilter.addAction(Intent.ACTION_MEDIA_EJECT);
        intentFilter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        intentFilter.addAction(Intent.ACTION_MEDIA_SCANNER_STARTED);
        intentFilter.addAction(Intent.ACTION_MEDIA_SCANNER_FINISHED);
        intentFilter.addDataScheme("file");
        mReceiver = new MyBroadcastReceiver();
        registerReceiver(mReceiver, intentFilter);
        mStorageStatus = getStorageStatus(true);

        mHandler.postDelayed(new Runnable() {
            public void run() {
                showStorageHint();
            }
        }, 200);

    }

    @Override
    protected void onPause() {
        super.onPause();
        /*mPausing = true;

        // Hide the preview now. Otherwise, the preview may be rotated during
        // onPause and it is annoying to users.
        mVideoPreview.setVisibility(View.INVISIBLE);

        // This is similar to what mShutterButton.performClick() does,
        // but not quite the same.
        if (mRecorderRecording) {
            stopVideoRecordingAndGetThumbnail();
        } else {
            stopVideoRecording();
        }
        closeCamera();*/

        if(mRecorderRecording) {
            stopRecord();
        }
        closeCamera();


        if (mReceiver != null) {
            unregisterReceiver(mReceiver);
            mReceiver = null;
        }
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
    public void onBackPressed() {
    	this.finish();
//        if (mPausing) {
//            return;
//        }
//        if (mRecorderRecording) {
//            onStopVideoRecording(false);
//        } else {
//            super.onBackPressed();
//        }
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
//            stopVideoRecording();
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

    private void doReturnToCaller(boolean valid) {
        Intent resultIntent = new Intent();
        int resultCode;
        if (valid) {
            resultCode = RESULT_OK;
            resultIntent.setData(mCurrentVideoUri);
        } else {
            resultCode = RESULT_CANCELED;
        }
        setResult(resultCode, resultIntent);
        finish();
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

    private void cleanupEmptyFile() {
        if (mVideoFilename != null) {
            File f = new File(mVideoFilename);
            if (f.length() == 0 && f.delete()) {
                Log.v(TAG, "Empty video file deleted: " + mVideoFilename);
                mVideoFilename = null;
            }
        }
    }


    private void createVideoPath() {
        long dateTaken = System.currentTimeMillis();
        String title = createName(dateTaken);
        String filename = title + ".3gp"; // Used when emailing.
        String cameraDirPath = ImageManager.CAMERA_IMAGE_BUCKET_NAME;
        String filePath = cameraDirPath + "/" + filename;
        File cameraDir = new File(cameraDirPath);
        cameraDir.mkdirs();
        ContentValues values = new ContentValues(7);
        values.put(Video.Media.TITLE, title);
        values.put(Video.Media.DISPLAY_NAME, filename);
        values.put(Video.Media.DATE_TAKEN, dateTaken);
        values.put(Video.Media.MIME_TYPE, "video/3gpp");
        values.put(Video.Media.DATA, filePath);
        mVideoFilename = filePath;
        Log.v(TAG, "Current camera video filename: " + mVideoFilename);
        mCurrentVideoValues = values;
    }

    /**
     * 把视频注册到系统的媒体库
     */
    private void registerVideo() {
        if (mVideoFileDescriptor == null) {
            Uri videoTable = Uri.parse("content://media/external/video/media");
            mCurrentVideoValues.put(Video.Media.SIZE,
                    new File(mCurrentVideoFilename).length());
            try {
                mCurrentVideoUri = mContentResolver.insert(videoTable,
                        mCurrentVideoValues);
            } catch (Exception e) {
                // We failed to insert into the database. This can happen if
                // the SD card is unmounted.
                mCurrentVideoUri = null;
                mCurrentVideoFilename = null;
            } finally {
                Log.v(TAG, "Current video URI: " + mCurrentVideoUri);
            }
        }
        mCurrentVideoValues = null;
    }

    /**
     * 删除当前的视频
     */
    private void deleteCurrentVideo() {
        if (mCurrentVideoFilename != null) {
            deleteVideoFile(mCurrentVideoFilename);
            mCurrentVideoFilename = null;
        }
        if (mCurrentVideoUri != null) {
            mContentResolver.delete(mCurrentVideoUri, null, null);
            mCurrentVideoUri = null;
        }
        updateAndShowStorageHint(true);
    }

    private void deleteVideoFile(String fileName) {
        Log.v(TAG, "Deleting video " + fileName);
        File f = new File(fileName);
        if (!f.delete()) {
            Log.v(TAG, "Could not delete " + fileName);
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
        if(mCameraId == Camera.CameraInfo.CAMERA_FACING_BACK) {
            switchCameraId(Camera.CameraInfo.CAMERA_FACING_FRONT);
        } else if(mCameraId == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            switchCameraId(Camera.CameraInfo.CAMERA_FACING_BACK);
        }
    }


    private void startVideoRecording() {
        Log.v(TAG, "startVideoRecording");
        if (mStorageStatus != STORAGE_STATUS_OK) {
            Log.v(TAG, "Storage issue, ignore the start request");
            return;
        }

        keepScreenOn();
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

//    private void stopVideoRecording() {
//        Log.v(TAG, "stopVideoRecording");
//        if (mMediaRecorderRecording) {
//            boolean needToRegisterRecording = false;
//            mMediaRecorder.setOnErrorListener(null);
//            mMediaRecorder.setOnInfoListener(null);
//            try {
//                mMediaRecorder.stop();
//                mCurrentVideoFilename = mVideoFilename;
//                Log.v(TAG, "Setting current video filename: "
//                        + mCurrentVideoFilename);
//                needToRegisterRecording = true;
//            } catch (RuntimeException e) {
//                Log.e(TAG, "stop fail: " + e.getMessage());
//                deleteVideoFile(mVideoFilename);
//            }
//            mMediaRecorderRecording = false;
////            mHeadUpDisplay.setEnabled(true);
////            updateRecordingIndicator(true);
////            mRecordingTimeView.setVisibility(View.GONE);
//            keepScreenOnAwhile();
//            if (needToRegisterRecording && mStorageStatus == STORAGE_STATUS_OK) {
//                registerVideo();
//            }
//            mVideoFilename = null;
//            mVideoFileDescriptor = null;
//        }
//        releaseMediaRecorder();  // always release media recorder
//    }

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

    /*private void acquireVideoThumb() {
        Bitmap videoFrame = ThumbnailUtils.createVideoThumbnail(
                mCurrentVideoFilename, Video.Thumbnails.MINI_KIND);
//        mThumbController.setData(mCurrentVideoUri, videoFrame);
//        mThumbController.updateDisplayIfNeeded();
    }*/

    /**
     * Update the time show in when recording.
     */
    private void updateRecordingTime() {
        if (!mRecorderRecording) {
            return;
        }
        long now = SystemClock.uptimeMillis();
        long delta = now - mRecordStartTime;

        // Starting a minute before reaching the max duration
        // limit, we'll countdown the remaining time instead.
        boolean countdownRemainingTime = (mMaxVideoDurationInMs != 0
                && delta >= mMaxVideoDurationInMs - 60000);

        long next_update_delay = 1000 - (delta % 1000);
        long seconds;
        if (countdownRemainingTime) {
            delta = Math.max(0, mMaxVideoDurationInMs - delta);
            seconds = (delta + 999) / 1000;
        } else {
            seconds = delta / 1000; // round to nearest
        }

        long minutes = seconds / 60;
        long hours = minutes / 60;
        long remainderMinutes = minutes - (hours * 60);
        long remainderSeconds = seconds - (minutes * 60);

        String secondsString = Long.toString(remainderSeconds);
        if (secondsString.length() < 2) {
            secondsString = "0" + secondsString;
        }
        String minutesString = Long.toString(remainderMinutes);
        if (minutesString.length() < 2) {
            minutesString = "0" + minutesString;
        }
        String text = minutesString + ":" + secondsString;
        if (hours > 0) {
            String hoursString = Long.toString(hours);
            if (hoursString.length() < 2) {
                hoursString = "0" + hoursString;
            }
            text = hoursString + ":" + text;
        }
//        mRecordingTimeView.setText(text);

        if (mRecordingTimeCountsDown != countdownRemainingTime) {
            // Avoid setting the color on every update, do it only
            // when it needs changing.
            mRecordingTimeCountsDown = countdownRemainingTime;

//            int color = getResources().getColor(countdownRemainingTime
//                    ? R.color.recording_time_remaining_text
//                    : R.color.recording_time_elapsed_text);
//
//            mRecordingTimeView.setTextColor(color);
        }

        mHandler.sendEmptyMessageDelayed(
                UPDATE_RECORD_TIME, next_update_delay);
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


    /** 初始化View、 View相关 ***********************************************************************/
    private void initView() {

        initTitleBar();

        mProgressView = (ProgressView) findViewById(R.id.pv_recorder_progress);

        mVideoPreview = (SurfaceView) findViewById(R.id.sv_recorder_preview);

        mToolbar = (LinearLayout) findViewById(R.id.ll_recorder_toolbar);
        mBtnVideo = (Button) findViewById(R.id.btn_camcorder_video);
        mBtnImage = (Button) findViewById(R.id.btn_camcorder_image);
        mTBtnDelay = (ToggleButton) findViewById(R.id.tbtn_camcorder_delay);
        mTBtnFocus = (ToggleButton) findViewById(R.id.tbtn_camcorder_focus);
        mBtnDelete = (Button) findViewById(R.id.btn_camcorder_delete);

        mVideoPreview.setOnTouchListener(this);
        mBtnVideo.setOnClickListener(this);
        mBtnImage.setOnClickListener(this);
        mTBtnDelay.setOnClickListener(this);
        mTBtnFocus.setOnClickListener(this);
        mBtnDelete.setOnClickListener(this);

        mBtnVideo.setEnabled(false);
        mBtnImage.setEnabled(false);
        mTBtnDelay.setEnabled(false);
        mTBtnFocus.setEnabled(false);
        mBtnDelete.setEnabled(false);

        mProgressView.setMaxProgress(VIDEO_MAX_DURATION); //八秒毫秒值8000
        mProgressView.setMinMask(VIDEO_MIN_DURATION); //最小两秒毫秒值2000
        mProgressView.setOnProgressUpdateListener(new ProgressView.OnProgressUpdateListener() {
            @Override
            public void onProgressUpdate(float max, float progress) {
                if(progress == max) {
                    new DealFinishWorkTask().execute(mVideoFilename);
                }
            }
        });

        initSettingPopWindow(); //初始化设置弹出框

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
        DisplayMetrics dm = getResources().getDisplayMetrics();
        ViewGroup.LayoutParams videoPreviewLayoutParams = mVideoPreview.getLayoutParams();
        videoPreviewLayoutParams.width = dm.widthPixels;
        videoPreviewLayoutParams.height = mPreviewWidth * dm.widthPixels / mPreviewHeight;

        FrameLayout.LayoutParams toolbarLayoutParams = (FrameLayout.LayoutParams) mToolbar.getLayoutParams();
        toolbarLayoutParams.topMargin = mVideoHeight * dm.widthPixels / mPreviewHeight;
    }

    /**********************************************************************************************/


	//当前录制的质量，会影响视频清晰度和文件大小
	private int currentResolution = CONSTANTS.RESOLUTION_MEDIUM_VALUE;


	/**
	 * 初始化Recorder
	 */
	private void initRecorder() {

        // 初始化状态
        mRecorderRecording = false; // 非录制中状态
        mRecordFinished = true; //录制完成状态

        mVideoFilename = Util.createFinalPath(this);

		RecorderParameters recorderParameters = Util.getRecorderParameter(currentResolution);
		mAudioSampleRate = recorderParameters.getAudioSamplingRate();
		mVideoFrameRange = recorderParameters.getVideoFrameRate();

		mFFmpegFrameRecorder = new NewFFmpegFrameRecorder(mVideoFilename, mVideoWidth, mVideoHeight, 1);
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
                                    if(null != mVideoFilename) {
                                        File file = new File(mVideoFilename);
                                        if(file.exists()) {
                                            file.delete();
                                        }
                                    }
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


    public class DealFinishWorkTask extends AsyncTask<String, Integer, String> {

        private Dialog mmDialog;
        private ProgressBar mmProgressBar;
        private TextView mmTvProgress;

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
            mmDialog.setContentView(R.layout.activity_recorder_progress);

            mmTvProgress = (TextView) mmDialog.findViewById(R.id.recorder_progress_progresstext);
            mmProgressBar = (ProgressBar) mmDialog.findViewById(R.id.recorder_progress_progressbar);
            mmDialog.show();
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(String... params) {
            publishProgress(10);
            if(null == params || params.length < 0) {
                return null;
            }
            String videoPath = params[0];
            if(StringUtil.isEmpty(videoPath)) {
                return null;
            }
            publishProgress(30);
            Bitmap bm = ThumbnailUtils.createVideoThumbnail(videoPath, Video.Thumbnails.FULL_SCREEN_KIND);
            publishProgress(50);
            File file = new File(Util.createImagePath(CamcorderActivity.this));
            try {
                boolean state = bm.compress(Bitmap.CompressFormat.JPEG, 100, new FileOutputStream(file));
                publishProgress(80);
                if(state && file.exists()) {
                    return file.getAbsolutePath();
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            mmProgressBar.setProgress(values[0]);
        }

        @Override
        protected void onPostExecute(String thumbnail) {

            publishProgress(100);
            mmDialog.cancel();

            //TODO 处理完成,跳转至编辑界面
            if(null != mVideoFilename) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.fromFile(new File(mVideoFilename)), "video/*");
                try {
                    startActivity(intent);
                    finish();
                } catch (android.content.ActivityNotFoundException ex) {
                    Log.e(TAG, "Couldn't view video " + mCurrentVideoUri, ex);
                }
            }
            super.onPostExecute(thumbnail);
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
    class AudioRecordThread extends Thread {

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

	private byte[] rotateYUV420Degree90(byte[] data, int imageWidth, int imageHeight) {
        byte[] yuv = new byte[imageWidth * imageHeight * 3 / 2];
        // Rotate the Y luma
        int i = 0;
        for (int x = 0; x < imageWidth; x++) {
            for (int y = imageHeight - 1; y >= 0; y--) {
                yuv[i] = data[y * imageWidth + x];
                i++;
            }

        }
        // Rotate the U and V color components
        i = imageWidth * imageHeight * 3 / 2 - 1;
        for (int x = imageWidth - 1; x > 0; x = x - 2) {
            for (int y = 0; y < imageHeight / 2; y++) {
                yuv[i] = data[(imageWidth * imageHeight) + (y * imageWidth) + x];
                i--;
                yuv[i] = data[(imageWidth * imageHeight) + (y * imageWidth) + (x - 1)];
                i--;
            }
        }
        return yuv;
    }

	private byte[] rotateYUV420Degree180(byte[] data, int imageWidth, int imageHeight) {
        byte[] yuv = new byte[imageWidth * imageHeight * 3 / 2];
        int i = 0;
        int count = 0;

        for (i = imageWidth * imageHeight - 1; i >= 0; i--) {
            yuv[count] = data[i];
            count++;
        }

        i = imageWidth * imageHeight * 3 / 2 - 1;
        for (i = imageWidth * imageHeight * 3 / 2 - 1; i >= imageWidth
                * imageHeight; i -= 2) {
            yuv[count++] = data[i - 1];
            yuv[count++] = data[i];
        }
        return yuv;
    }

	private byte[] rotateYUV420Degree270(byte[] data, int imageWidth, int imageHeight) {

        /*byte [] yuv = new byte[imageWidth*imageHeight*3/2];
        int nWidth = 0, nHeight = 0;
        int wh = 0;
        int uvHeight = 0;
        if(imageWidth != nWidth || imageHeight != nHeight)
        {
            nWidth = imageWidth;
            nHeight = imageHeight;
            wh = imageWidth * imageHeight;
            uvHeight = imageHeight >> 1;//uvHeight = height / 2
        }

        //旋转Y
        int k = 0;
        for(int i = 0; i < imageWidth; i++) {
            int nPos = 0;
            for(int j = 0; j < imageHeight; j++) {
                yuv[k] = data[nPos + i];
                k++;
                nPos += imageWidth;
            }
        }

        for(int i = 0; i < imageWidth; i+=2){
            int nPos = wh;
            for(int j = 0; j < uvHeight; j++) {
                yuv[k] = data[nPos + i];
                yuv[k + 1] = data[nPos + i + 1];
                k += 2;
                nPos += imageWidth;
            }
        }
        return rotateYUV420Degree180(yuv, imageWidth, imageHeight);*/

        byte[] yuv = new byte[imageWidth * imageHeight * 3 / 2];
        // Rotate the Y luma
        int i = 0;
        for (int x = imageWidth - 1; x >= 0; x--) {
            for (int y = 0; y < imageHeight; y++) {
                yuv[i] = data[y * imageWidth + x];
                i++;
            }

        }
        // Rotate the U and V color components
        i = imageWidth * imageHeight;
        for (int x = imageWidth - 1; x > 0; x = x - 2) {
            for (int y = 0; y < imageHeight / 2; y++) {
                yuv[i] = data[(imageWidth * imageHeight) + (y * imageWidth) + (x - 1)];
                i++;
                yuv[i] = data[(imageWidth * imageHeight) + (y * imageWidth) + x];
                i++;
            }
        }
        return yuv;
    }
}
