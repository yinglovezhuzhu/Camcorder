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
import android.content.DialogInterface;
import android.hardware.Camera;
import android.os.Environment;
import android.view.Surface;

import com.opensource.camcorder.CamcorderConfig;
import com.opensource.camcorder.R;
import com.opensource.camcorder.CamcorderParameters;

import java.io.File;

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
        String filename = prefix + System.currentTimeMillis() + suffix;
        return new File(folder, filename).getAbsolutePath();
    }
}
