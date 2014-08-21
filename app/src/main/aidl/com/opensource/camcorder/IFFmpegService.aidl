// IFFmpegService.aidl.aidl
package com.opensource.camcorder;

// Declare any non-default types here with import statements

interface IFFmpegService {

    int mergeVideo(String cache, String output, in String [] inputs);

    int cutVideo(String input, String output, String start, String duration);

    int addWaterMark(String videoPath, String imagePath, int position, int verticalMarging, int horizontalMarging, String outputPath, String format);

    int removeAudio(String input, String output);

    int fetchAudio(String input, String output, String format);

    int addAudio(String video, String audio, String output, String format);

    /** Use this to use ffmpeg command line  **/
    int ffmpeg(in String [] args);
}
