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

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.ThumbnailUtils;
import android.net.Uri;
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
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.googlecode.javacv.FFmpegFrameRecorder;
import com.googlecode.javacv.cpp.opencv_core.IplImage;

import java.io.File;
import java.nio.Buffer;
import java.nio.ShortBuffer;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static com.googlecode.javacv.cpp.opencv_core.IPL_DEPTH_8U;

/**
 * The Camcorder activity.
 */
public class CamcorderActivity extends NoSearchActivity implements
		View.OnClickListener, View.OnTouchListener,
        SurfaceHolder.Callback, Camera.PreviewCallback {

    private static final String TAG = "CamcorderActivity";

    private static final int CLEAR_SCREEN_DELAY = 4;
    private static final int UPDATE_RECORD_TIME = 5;
    private static final int ENABLE_SHUTTER_BUTTON = 6;

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


    private SurfaceView mVideoPreview;
    private SurfaceHolder mSurfaceHolder = null;
    private ImageView mVideoFrame;

    private boolean mStartPreviewFail = false;

    private int mStorageStatus = STORAGE_STATUS_OK;

    /** 是否正在录制 **/
    private boolean mRecorderRecording = false;
    /** 录制的开始时间 **/
    private long mRecordStartTime;
    /** 录制的时间 **/
    private long mRecordedDuration = 0L;

    // The video file that the hardware camera is about to record into
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

    
    private MainOrientationEventListener mOrientationListener;
    // The device orientation in degrees. Default is unknown.
    private int mOrientation = OrientationEventListener.ORIENTATION_UNKNOWN;
    // The orientation compensation for icons and thumbnails. Degrees are in
    // counter-clockwise
    private int mOrientationCompensation = 0;
    private int mOrientationHint; // the orientation hint for video playback
    
    

	//录制视频和保存音频的类
	private volatile FFmpegFrameRecorder mFFmpegFrameRecorder;
	//IplImage对象,用于存储摄像头返回的byte[]，以及图片的宽高，depth，channel等
	private IplImage mYUVIplImage = null;
	//视频帧率
	private int mVideoFrameRange = 30;
	
	//音频录制
	//录制音频的线程
//	private AudioRecordRunnable audioRecordRunnable;
	private Thread mAudioRecordThread;
	/** 音频采样平率Hz **/
	private int mAudioSampleRate = 44100;

	private boolean isFinished = false;

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                break;
            case MotionEvent.ACTION_UP:
                break;
            default:
                break;
        }
        return false;
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

        mVideoPreview = (SurfaceView) findViewById(R.id.sv_recorder_preview);
        
        
        findViewById(R.id.btn_recorder_video).setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
//				prepare();
//				initiateRecording(true);
                record();
			}
		});
        
        findViewById(R.id.btn_recorder_image).setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
//				saveRecording();
                pause();
                releaseResources();
			}
		});
        
        /*findViewById(R.id.btn_recorder_title_button1).setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if(mCameraId == Camera.CameraInfo.CAMERA_FACING_BACK) {
					switchCameraId(Camera.CameraInfo.CAMERA_FACING_FRONT);
				} else if(mCameraId == Camera.CameraInfo.CAMERA_FACING_FRONT) {
					switchCameraId(Camera.CameraInfo.CAMERA_FACING_BACK);
				} 
			}
		});*/
        ToggleButton tb = (ToggleButton) findViewById(R.id.tbtn_recorder_delay);
        tb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                setVideoFlash(isChecked);
            }
        });

        findViewById(R.id.btn_recorder_delete).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pause();
            }
        });

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

        mOrientationListener = new MainOrientationEventListener(CamcorderActivity.this);

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
			} catch (FFmpegFrameRecorder.Exception e) {
				Log.v(TAG, e.getMessage());
				e.printStackTrace();
			}
		}
	}

    public static int roundOrientation(int orientation) {
        return ((orientation + 45) / 90 * 90) % 360;
    }

    private class MainOrientationEventListener
            extends OrientationEventListener {
        public MainOrientationEventListener(Context context) {
            super(context);
        }

        @Override
        public void onOrientationChanged(int orientation) {
            if (mRecorderRecording) return;
            // We keep the last known orientation. So if the user first orient
            // the camera then point the camera to floor or sky, we still have
            // the correct orientation.
            if (orientation == ORIENTATION_UNKNOWN) return;
            mOrientation = roundOrientation(orientation);
            // When the screen is unlocked, display rotation may change. Always
            // calculate the up-to-date orientationCompensation.
            int orientationCompensation = mOrientation
                    + Util.getDisplayRotation(CamcorderActivity.this);
            if (mOrientationCompensation != orientationCompensation) {
                mOrientationCompensation = orientationCompensation;
            }
        }
    }


    public void onClick(View v) {
        switch (v.getId()) {
        }
    }


    private void updateAndShowStorageHint(boolean mayHaveSd) {
        mStorageStatus = getStorageStatus(mayHaveSd);
        showStorageHint();
    }

    private void showStorageHint() {
        String errorMessage = null;
        switch (mStorageStatus) {
            case STORAGE_STATUS_NONE:
                errorMessage = getString(R.string.no_storage);
                break;
            case STORAGE_STATUS_LOW:
                errorMessage = getString(R.string.spaceIsLow_content);
                break;
            case STORAGE_STATUS_FAIL:
                errorMessage = getString(R.string.access_sd_fail);
                break;
        }
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

    private void record() {
        mRecordStartTime = System.currentTimeMillis();
        mRecorderRecording = true;
    }

    private void pause() {
        mRecorderRecording = false;
//        isFinished = true;
        mRecordedDuration += System.currentTimeMillis() - mRecordStartTime;
    }

    private void reset() {
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


    @Override
    protected void onResume() {
        super.onResume();
        mPausing = false;

        // Start orientation listener as soon as possible because it takes
        // some time to get first orientation.
        mOrientationListener.enable();
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
        
        initRecorder();
        
        if (mPreviewing) {
            mCameraDevice.stopPreview();
            mPreviewing = false;
        }
        setPreviewDisplay(mSurfaceHolder);
        Util.setCameraDisplayOrientation(this, mCameraId, mCameraDevice);

        setCameraParameters();
        
        if(null != mYUVIplImage) {
        	mYUVIplImage.release();
            mYUVIplImage = null;
        }
        mYUVIplImage = IplImage.create(mPreviewHeight, mPreviewWidth,IPL_DEPTH_8U, 2);

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
        closeCamera();

        if (mReceiver != null) {
            unregisterReceiver(mReceiver);
            mReceiver = null;
        }
        resetScreenOn();*/

        mOrientationListener.disable();
    }

    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
        if (!mRecorderRecording) keepScreenOnAwhile();
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
            case KeyEvent.KEYCODE_CAMERA:
                if (event.getRepeatCount() == 0) {
//                    mShutterButton.performClick();
                    return true;
                }
                break;
            case KeyEvent.KEYCODE_DPAD_CENTER:
                if (event.getRepeatCount() == 0) {
//                    mShutterButton.performClick();
                    return true;
                }
                break;
            case KeyEvent.KEYCODE_MENU:
                if (mRecorderRecording) {
//                    onStopVideoRecording(true);
                    return true;
                }
                break;
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
     * 切换摄像头
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


    private void startVideoRecording() {
        Log.v(TAG, "startVideoRecording");
        if (mStorageStatus != STORAGE_STATUS_OK) {
            Log.v(TAG, "Storage issue, ignore the start request");
            return;
        }

        keepScreenOn();
    }


    /*private void stopVideoRecordingAndGetThumbnail() {
        stopVideoRecording();
        acquireVideoThumb();
    }*/

    /*private void stopVideoRecordingAndReturn(boolean valid) {
        stopVideoRecording();
        doReturnToCaller(valid);
    }*/

    /*private void stopVideoRecordingAndShowAlert() {
        stopVideoRecording();
        showAlert();
    }*/

    private void showAlert() {
//        fadeOut(findViewById(R.id.shutter_button));
        if (mCurrentVideoFilename != null) {
            Bitmap src = ThumbnailUtils.createVideoThumbnail(
                    mCurrentVideoFilename, Video.Thumbnails.MINI_KIND);
            // MetadataRetriever already rotates the thumbnail. We should rotate
            // it back (and mirror if it is front-facing camera).
            CameraInfo[] info = CameraHolder.instance().getCameraInfo();
            if (info[mCameraId].facing == CameraInfo.CAMERA_FACING_BACK) {
                src = Util.rotateAndMirror(src, -mOrientationHint, false);
            } else {
                src = Util.rotateAndMirror(src, -mOrientationHint, true);
            }
            mVideoFrame.setImageBitmap(src);
            mVideoFrame.setVisibility(View.VISIBLE);
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

    private boolean isAlertVisible() {
        return this.mVideoFrame.getVisibility() == View.VISIBLE;
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

      //获取摄像头的所有支持的分辨率
  		List<Camera.Size> supportedPreviewSizes = mCameraDevice.getParameters().getSupportedPreviewSizes();
  		if(supportedPreviewSizes != null && supportedPreviewSizes.size() > 0){
  			Collections.sort(supportedPreviewSizes, new Util.ResolutionComparator());
  			Camera.Size previewSize =  null;	
  			if(defaultScreenResolution == -1){
  				boolean hasSize = false;
  				//如果摄像头支持640*480，那么强制设为640*480
  				for(int i = 0;i<supportedPreviewSizes.size();i++){
  					Size size = supportedPreviewSizes.get(i);
  					if(size != null && size.width==640 && size.height==480){
  						previewSize = size;
  						hasSize = true;
  						break;
  					}
  				}
  				//如果不支持设为中间的那个
  				if(!hasSize){
  					int mediumResolution = supportedPreviewSizes.size()/2;
  					if(mediumResolution >= supportedPreviewSizes.size())
  						mediumResolution = supportedPreviewSizes.size() - 1;
  					previewSize = supportedPreviewSizes.get(mediumResolution);
  				}
  			}else{
  				if(defaultScreenResolution >= supportedPreviewSizes.size())
  					defaultScreenResolution = supportedPreviewSizes.size() - 1;
  				previewSize = supportedPreviewSizes.get(defaultScreenResolution);
  			}
  			//获取计算过的摄像头分辨率
  			if(previewSize != null ){
  				mPreviewWidth = previewSize.width;
  				mPreviewHeight = previewSize.height;
  				mParameters.setPreviewSize(mPreviewWidth, mPreviewHeight);
  				mVideoWidth = mPreviewWidth > mPreviewHeight ? mPreviewHeight : mPreviewWidth;
  				mVideoHeight = mPreviewWidth > mPreviewHeight ? mPreviewHeight : mPreviewWidth;
  				if(mFFmpegFrameRecorder != null) {
  					mFFmpegFrameRecorder.setImageWidth(mVideoWidth);
  					mFFmpegFrameRecorder.setImageHeight(mVideoHeight);
  				}

  			}
  		}
        System.out.println("AAAAAAAAAAA " + mPreviewWidth + "<>" + mPreviewHeight);
        mParameters.setPreviewFrameRate(mPreviewFrameRate);
//        DisplayMetrics dm = getResources().getDisplayMetrics();
//        int size = dm.widthPixels > dm.heightPixels ? dm.heightPixels : dm.widthPixels;
//        ViewGroup.LayoutParams lp = mVideoPreview.getLayoutParams();
//        lp.width = size;
//        lp.height = size;
        SurfaceHolder holder = mVideoPreview.getHolder();
        holder.setFixedSize(mPreviewHeight, mPreviewWidth);
//        holder.setSizeFromLayout();

        // 设置闪光灯，默认关闭
        setVideoFlash(false);
    }

    /******* 闪光灯相关 ****************************************************************************/

    /**
     * 设置录像模式闪光灯
     */
    private boolean setVideoFlash(boolean isOn) {
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

    
	//视频文件的存放地址
	private String strVideoPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "rec_video.mp4";
	//视频文件对象
	private File fileVideoPath = null;

	//当前录制的质量，会影响视频清晰度和文件大小
	private int currentResolution = CONSTANTS.RESOLUTION_MEDIUM_VALUE;

	//默认调用摄像头的分辨率
	int defaultScreenResolution = -1;

	/**
	 * 初始化Recorder
	 */
	private void initRecorder() {
		strVideoPath = Util.createFinalPath(this);//Util.createTempPath(tempFolderPath);
		
		RecorderParameters recorderParameters = Util.getRecorderParameter(currentResolution);
		mAudioSampleRate = recorderParameters.getAudioSamplingRate();
		mVideoFrameRange = recorderParameters.getVideoFrameRate();


		fileVideoPath = new File(strVideoPath); 
		Log.e(TAG, "File Path:" + fileVideoPath);
		mFFmpegFrameRecorder = new FFmpegFrameRecorder(strVideoPath, mPreviewWidth, mPreviewHeight, 1);
		mFFmpegFrameRecorder.setFormat(recorderParameters.getVideoOutputFormat());
		mFFmpegFrameRecorder.setSampleRate(recorderParameters.getAudioSamplingRate());
		mFFmpegFrameRecorder.setFrameRate(recorderParameters.getVideoFrameRate());
		mFFmpegFrameRecorder.setVideoCodec(recorderParameters.getVideoCodec());
		mFFmpegFrameRecorder.setVideoQuality(recorderParameters.getVideoQuality()); 
		mFFmpegFrameRecorder.setAudioQuality(recorderParameters.getVideoQuality());
		mFFmpegFrameRecorder.setAudioCodec(recorderParameters.getAudioCodec());
		mFFmpegFrameRecorder.setVideoBitrate(recorderParameters.getVideoBitrate());
		mFFmpegFrameRecorder.setAudioBitrate(recorderParameters.getAudioBitrate());
		
		mAudioRecordThread = new Thread(new AudioRecordRunnable());

//        prepare();
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

		//progressView.putProgressList((int) totalTime);
		//停止刷新进度
//		progressView.setCurrentState(State.PAUSE);
	}
	
	/**
	 * 停止录制
	 * @author QD
	 *
	 */
	/*public class AsyncStopRecording extends AsyncTask<Void,Integer,Void>{
		
		private ProgressBar bar;
		private TextView progress;
		@Override
		protected void onPreExecute() {
			isFinalizing = true;
			recordFinish = true;
			runAudioThread = false;
			
			//创建处理进度条
//			creatingProgress= new Dialog(CamcorderActivity.this,R.style.Dialog_loading_noDim);
//			Window dialogWindow = creatingProgress.getWindow();
//			WindowManager.LayoutParams lp = dialogWindow.getAttributes();
//			lp.width = (int) (getResources().getDisplayMetrics().density*240);
//			lp.height = (int) (getResources().getDisplayMetrics().density*80);
//			lp.gravity = Gravity.CENTER;
//			dialogWindow.setAttributes(lp);
//			creatingProgress.setCanceledOnTouchOutside(false);
//			creatingProgress.setContentView(R.layout.activity_recorder_progress);
			
//			progress = (TextView) creatingProgress.findViewById(R.id.recorder_progress_progresstext);
//			bar = (ProgressBar) creatingProgress.findViewById(R.id.recorder_progress_progressbar);
//			creatingProgress.show();
			
			//txtTimer.setVisibility(View.INVISIBLE);
			//handler.removeCallbacks(mUpdateTimeTask);
			super.onPreExecute();
		}
		
		@Override
		protected void onProgressUpdate(Integer... values) {
//			progress.setText(values[0]+"%");
//			bar.setProgress(values[0]);
		}
		
		*//**
		 * 依据byte[]里的数据合成一张bitmap，
		 * 截成480*480，并且旋转90度后，保存到文件
		 * @param data
		 *//*
		private void getFirstCapture(byte[] data){
			
			publishProgress(10);
			
			String captureBitmapPath = CONSTANTS.CAMERA_FOLDER_PATH;
			
			captureBitmapPath = Util.createImagePath(CamcorderActivity.this);
//			YuvImage localYuvImage = new YuvImage(data, 17, previewWidth,previewHeight, null);
			YuvImage localYuvImage = new YuvImage(data, 17, mPreviewWidth,mPreviewHeight, null);
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			FileOutputStream outStream = null;
			
			publishProgress(50);
			
			try {
				File file = new File(captureBitmapPath);
				if(!file.exists())
					file.createNewFile();
//				localYuvImage.compressToJpeg(new Rect(0, 0, previewWidth, previewHeight),100, bos);
				localYuvImage.compressToJpeg(new Rect(0, 0, mPreviewWidth, mPreviewHeight),100, bos);
				Bitmap localBitmap1 = BitmapFactory.decodeByteArray(bos.toByteArray(),
						0,bos.toByteArray().length);
				
				bos.close();
				
				Matrix localMatrix = new Matrix();
				if (cameraSelection == 0)
					localMatrix.setRotate(90.0F);
				else
					localMatrix.setRotate(270.0F);
				
				Bitmap	localBitmap2 = Bitmap.createBitmap(localBitmap1, 0, 0,
									localBitmap1.getHeight(),
									localBitmap1.getHeight(),
									localMatrix, true);
				
				publishProgress(70);
				
				ByteArrayOutputStream bos2 = new ByteArrayOutputStream();
				localBitmap2.compress(Bitmap.CompressFormat.JPEG, 100, bos2);
					 
				outStream = new FileOutputStream(captureBitmapPath);
				outStream.write(bos2.toByteArray());
				outStream.close();
				
				localBitmap1.recycle();
				localBitmap2.recycle();
				
				publishProgress(90);
				
				isFirstFrame = false;
				imagePath = captureBitmapPath;
			} catch (FileNotFoundException e) {
				isFirstFrame = true;
				e.printStackTrace();
			} catch (IOException e) {
				isFirstFrame = true;
				e.printStackTrace();
			}        
		}
			
		
		@Override
		protected Void doInBackground(Void... params) {
			if(firstData != null)
				getFirstCapture(firstData);
			isFinalizing = false;
			if (mFFmpegFrameRecorder != null && mRecorderRecording) {
				mRecorderRecording = false;
				releaseResources();
			}
			publishProgress(100);
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
//			creatingProgress.dismiss();
//			registerVideo();
//			returnToCaller(true);
			mFFmpegFrameRecorder = null;

			isFinished = true;
		}
		
	}*/
	
	/**
	 * 录制音频的线程
	 *
	 */
	class AudioRecordRunnable implements Runnable {

		@Override
		public void run() {
			android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);

            /* audio data getting thread */
            AudioRecord audioRecord;
			// Audio
			int bufferSize;
			short [] audioData;
			int bufferReadResult;

			bufferSize = AudioRecord.getMinBufferSize(mAudioSampleRate, AudioFormat.CHANNEL_IN_MONO,
					AudioFormat.ENCODING_PCM_16BIT);
			audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, mAudioSampleRate, 
					AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize);
			audioData = new short[bufferSize];

			Log.d(TAG, "audioRecord.prepare()");
			audioRecord.startRecording();

			/* ffmpeg_audio encoding loop */
			while (!isFinished) {
				// Log.v(LOG_TAG,"recording? " + recording);
				bufferReadResult = audioRecord.read(audioData, 0, audioData.length);
				if (bufferReadResult > 0) {
					// Log.v(LOG_TAG, "bufferReadResult: " + bufferReadResult);
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
