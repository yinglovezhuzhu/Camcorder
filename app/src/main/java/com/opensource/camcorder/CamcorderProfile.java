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

import android.os.Build;

import com.googlecode.javacv.cpp.avcodec;

/**
 * Use:
 * Created by yinglovezhuzhu@gmail.com on 2014-06-30.
 */
public class CamcorderProfile {

    public static final int QUALITY_HIGHT = 0;
    public static final int QUALITY_LOW = 0;
//    public static final int QUALITY_HIGHT = 0;

    public static final String OUTPUT_FORMAT_MP4 = "mp4";
    public static final String OUTPUT_FORMAT_3GP = "3gp";

    private static boolean AAC_SUPPORTED = Build.VERSION.SDK_INT >= 10;
    private int videoCodec = avcodec.AV_CODEC_ID_MPEG4;
    private int videoFrameRate = 15;
    private int videoQuality = 12;
    private int videoBitrate = 1000000;
    private String videoOutputFormat = AAC_SUPPORTED ? OUTPUT_FORMAT_MP4 : OUTPUT_FORMAT_3GP;

    private int audioCodec = AAC_SUPPORTED ? avcodec.AV_CODEC_ID_AAC : avcodec.AV_CODEC_ID_AMR_NB;
    private int audioChannel = 1;
    private int audioBitrate = 96000;//192000;//AAC_SUPPORTED ? 96000 : 12200;
    private int audioSamplingRate = AAC_SUPPORTED ? 44100 : 8000;
    private int audioQuality = 12;

    public static CamcorderProfile getRecorderParameter(int currentResolution) {
        CamcorderProfile profile = new CamcorderProfile();
        RecorderParameters parameters = new RecorderParameters();
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
        return profile;
    }

}
