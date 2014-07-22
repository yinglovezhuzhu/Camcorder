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

import android.os.Environment;

public class CamcorderConfig {

    public final static String FILE_START_NAME = "VID_";
    public final static String VIDEO_EXTENSION = ".mp4";
    public final static String IMAGE_EXTENSION = ".jpg";
    public final static String DCIM_FOLDER = "/DCIM";
    public final static String CAMERA_FOLDER = "/video";
    public final static String TEMP_FOLDER = "/Temp";
    public final static String CAMERA_FOLDER_PATH = Environment.getExternalStorageDirectory().toString() + CamcorderConfig.DCIM_FOLDER + CamcorderConfig.CAMERA_FOLDER;
    public final static String TEMP_FOLDER_PATH = Environment.getExternalStorageDirectory().toString() + CamcorderConfig.DCIM_FOLDER + CamcorderConfig.CAMERA_FOLDER + CamcorderConfig.TEMP_FOLDER;
    public final static String VIDEO_CONTENT_URI = "content://media/external/video/media";

    public final static String KEY_DELETE_FOLDER_FROM_SDCARD = "deleteFolderFromSDCard";

    public final static String RECEIVER_ACTION_SAVE_FRAME = "com.javacv.recorder.intent.action.SAVE_FRAME";
    public final static String RECEIVER_CATEGORY_SAVE_FRAME = "com.javacv.recorder";
    public final static String TAG_SAVE_FRAME = "saveFrame";

    public final static int RESOLUTION_HIGH = 1300;
    public final static int RESOLUTION_MEDIUM = 500;
    public final static int RESOLUTION_LOW = 180;

    public final static int RESOLUTION_HIGH_VALUE = 2;
    public final static int RESOLUTION_MEDIUM_VALUE = 1;
    public final static int RESOLUTION_LOW_VALUE = 0;
}
