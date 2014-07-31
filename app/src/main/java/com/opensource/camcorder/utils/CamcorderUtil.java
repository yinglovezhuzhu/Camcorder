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

package com.opensource.camcorder.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.hardware.Camera;
import android.os.Build;
import android.os.Environment;
import android.view.Surface;

import com.opensource.camcorder.CamcorderConfig;
import com.opensource.camcorder.CamcorderParameters;
import com.opensource.camcorder.R;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Use:
 * Created by yinglovezhuzhu@gmail.com on 2014-07-24.
 */
public class CamcorderUtil {

    private CamcorderUtil() {}


    public static CamcorderParameters getRecorderParameter(int currentResolution) {
        CamcorderParameters parameters = new CamcorderParameters();
        if (currentResolution == CamcorderConfig.RESOLUTION_HIGH_VALUE) {
            parameters.setAudioBitrate(128000);
            parameters.setVideoQuality(0);
        } else if (currentResolution == CamcorderConfig.RESOLUTION_MEDIUM_VALUE) {
            parameters.setAudioBitrate(128000);
            parameters.setVideoQuality(5);
        } else if (currentResolution == CamcorderConfig.RESOLUTION_LOW_VALUE) {
            parameters.setAudioBitrate(96000);
            parameters.setVideoQuality(20);
        }
        return parameters;
    }


    public static void showFatalErrorAndFinish(
            final Activity activity, String title, String message) {
        DialogInterface.OnClickListener buttonListener =
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        activity.finish();
                    }
                };
        new AlertDialog.Builder(activity)
                .setCancelable(false)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(title)
                .setMessage(message)
                .setNeutralButton(R.string.details_ok, buttonListener)
                .show();
    }

    public static int getDisplayRotation(Activity activity) {
        int rotation = activity.getWindowManager().getDefaultDisplay()
                .getRotation();
        switch (rotation) {
            case Surface.ROTATION_0: return 0;
            case Surface.ROTATION_90: return 90;
            case Surface.ROTATION_180: return 180;
            case Surface.ROTATION_270: return 270;
        }
        return 0;
    }

    public static void setCameraDisplayOrientation(Activity activity,
                                                   int cameraId, Camera camera) {
        // See android.hardware.Camera.setCameraDisplayOrientation for
        // documentation.
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        int degrees = getDisplayRotation(activity);
        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(result);
    }

    /**
     * 存储是否可用
     * @return
     */
    public static boolean hasStorage() {
        return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
    }

    /**
     * 根据文件夹名称生成一个应用数据目录
     * @param folderName
     * @return
     */
    public static String getApplicationFolder(String folderName) {
        if(StringUtil.isEmpty(folderName)) {
            return null;
        }
        if(hasStorage()) {
            File storage = Environment.getExternalStorageDirectory();
            File file =  new File(storage, folderName);
            if(!file.exists()) {
                file.mkdirs();
            }
            return file.getAbsolutePath();
        }
        return null;
    }

    /**
     * 根据系统时间产生一个在指定目录下的图片文件名
     * @param folder
     * @return
     */
    public static String createImageFilename(String folder) {
        return createFilename(folder, CamcorderConfig.IMAGE_PREFIX, CamcorderConfig.IMAGE_SUFFIX);
    }

    /**
     * 根据系统时间产生一个在指定目录下的视频文件名
     * @param folder
     * @return
     */
    public static String createVideoFilename(String folder) {
        return createFilename(folder, CamcorderConfig.VIDEO_PREFIX, CamcorderConfig.VIDEO_SUFFIX);
    }

    /**
     * 根据系统时间、前缀、后缀产生一个文件名
     * @param folder
     * @param prefix
     * @param suffix
     * @return
     */
    private static String createFilename(String folder, String prefix, String suffix) {
        File file = new File(folder);
        if (!file.exists() || !file.isDirectory()) {
            file.mkdirs();
        }
//        String filename = prefix + System.currentTimeMillis() + suffix;
        SimpleDateFormat dateFormat = new SimpleDateFormat(CamcorderConfig.DATE_FORMAT_MILLISECOND);
        String filename = prefix + dateFormat.format(new Date(System.currentTimeMillis())) + suffix;
        return new File(folder, filename).getAbsolutePath();
    }

    /**
     * Check if external storage is built-in or removable.
     *
     * @return True if external storage is removable (like an SD card), false
     * otherwise.
     */
    public static boolean isExternalStorageRemovable() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            return Environment.isExternalStorageRemovable();
        }
        return true;
    }

    /**
     * Get the external app cache directory.
     *
     * @param context The context to use
     * @return The external cache dir
     */
    public static File getExternalCacheDir(Context context) {
        if (hasExternalCacheDir()) {
            File cacheDir = context.getExternalCacheDir();
            if(null != cacheDir) {
                return cacheDir;
            }
        }

        // Before Froyo we need to construct the external cache dir ourselves
        final String cacheDir = "Android/data/" + context.getPackageName() + "/cache/";
        return new File(Environment.getExternalStorageDirectory(), cacheDir);
    }

    /**
     * Get the external app data directory
     * @param context
     * @return
     */
    public static File getExternalDataDir(Context context) {
        if(hasExternalCacheDir()) {
            File cacheDir = context.getExternalCacheDir();
            if(null != cacheDir) {
                return cacheDir.getParentFile();
            }
        }
        final String dateDir = "Android/data" + context.getPackageName();
        return new File(Environment.getExternalStorageDirectory(), dateDir);
    }

    /**
     * Get the external app files directory
     * @param context
     * @return
     */
    public static File getExternalFilesDir(Context context) {
        if (hasExternalCacheDir()) {
            File filesDir = context.getExternalFilesDir(null);
            if(filesDir != null) {
                return filesDir;
            }
        }

        // Before Froyo we need to construct the external cache dir ourselves
        final String cacheDir = "Android/data/" + context.getPackageName() + "/files/";
        return new File(Environment.getExternalStorageDirectory(), cacheDir);
    }

    /**
     * Get the external app local cache directory
     * @param context
     * @return
     */
    public static File getExternalLocalCacheDir(Context context) {
        File dataDir = getExternalDataDir(context);
        if(null != dataDir) {
            return new File(dataDir, "local_cache");
        }

        // Before Froyo we need to construct the external cache dir ourselves
        final String cacheDir = "Android/data/" + context.getPackageName() + "/local_cache/";
        return new File(Environment.getExternalStorageDirectory(), cacheDir);
    }

    /**
     * Check if OS version has built-in external cache dir method.
     *
     * @return
     */
    public static boolean hasExternalCacheDir() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO;
    }


    public static byte[] rotateYUV420Degree90(byte[] data, int imageWidth, int imageHeight) {
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

    public static byte[] rotateYUV420Degree180(byte[] data, int imageWidth, int imageHeight) {
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

    public static byte[] rotateYUV420Degree270(byte[] data, int imageWidth, int imageHeight) {

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
