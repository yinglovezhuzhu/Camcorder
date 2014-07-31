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

    public static final String EXTRA_VIDEO = "extra_video";
    public static final String EXTRA_THUMB = "extra_thumb";
    public static final String EXTRA_SOURCE_VIDEO = "extra_source_video";
    public static final String EXTRA_SOURCE_THUMB = "extra_source_thumb";

    public final static String DCIM_FOLDER = "/DCIM";
    public final static String TEMP_FOLDER_PATH = Environment.getExternalStorageDirectory().toString() + CamcorderConfig.DCIM_FOLDER + CamcorderConfig.VIDEO_FOLDER + CamcorderConfig.TEMP_FOLDER;

    public final static int RESOLUTION_HIGH = 1300;
    public final static int RESOLUTION_MEDIUM = 500;
    public final static int RESOLUTION_LOW = 180;

    public final static int RESOLUTION_HIGH_VALUE = 2;
    public final static int RESOLUTION_MEDIUM_VALUE = 1;
    public final static int RESOLUTION_LOW_VALUE = 0;

    public static final int THUMB_QUALITY = 60;

    public static final String DATE_FORMAT_MILLISECOND = "yyyyMMdd_HHmmssmmm";

    public final static String VIDEO_FOLDER = "video";
    public final static String TEMP_FOLDER = "temp";
    public final static String THUMB_FOLDER = "thumb";

    public final static String VIDEO_PREFIX = "VID_";
    public final static String IMAGE_PREFIX = "IMG_";
    public final static String VIDEO_SUFFIX = ".mp4";
    public final static String IMAGE_SUFFIX = ".jpg";
}
