package com.opensource.camcorder;

import android.os.Build;

import com.googlecode.javacv.cpp.avcodec;

/**
 * Use:
 * Created by yinglovezhuzhu@gmail.com on 2014-06-30.
 */
public class CamcorderProfile {

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

}
