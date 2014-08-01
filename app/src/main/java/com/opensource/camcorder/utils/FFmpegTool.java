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

/**
 * Usage 
 * 
 * @author yinglovezhuzhu@gmail.com
 */
public class FFmpegTool {
	
	private FFmpegTool() {}
	
	static {
		System.loadLibrary("avutil-52");
		System.loadLibrary("swresample-0");
		System.loadLibrary("avcodec-55");
		
		System.loadLibrary("swscale-2");
		System.loadLibrary("avformat-55");
		System.loadLibrary("avfilter-4");
		
		System.loadLibrary("ffmpeg-jni");
	}
	
	public static native void testLog();
	
	public static native void exit(int code);
	
	/**
	 * 合并视频,输入的视频格式必须都一致，否则会出错
	 * @param cacheFolder 缓存目录，用来保存合并视频时生成的视频列表清单文件，这个目录必须可读写，否则会报错退出
	 * @param outputPath 输出视频文件，合并后的输出文件路径
	 * @param paths 输入视频文件文件，要合并的输入视频文件，可以多个，但至少两个
	 * @return
	 */
	public static native int mergeVideo(String cacheFolder, String outputPath, String ... paths);
	
	
	
	/**
	 * 裁剪视频
	 * @param inputPath 输入视频文，
	 * @param outputPath 输出视频文件
	 * @param start 指定开始时间
	 * @param duration 时长，从开始时间计算，如果超出最大时长，则会截取指定开始时间到视频结束时间的视频
	 * @return
	 */
	public static native int cutVideo(String inputPath, String start, String duration, String outputPath);

	/**
	 * 为视频添加水印
	 * @param videoPath
	 * @param imagePath
	 * @param position
	 * @param verticalMarging
	 * @param horizontalMarging
	 * @return
	 */
	public static int addWaterMark(String videoPath, String imagePath, int position, int verticalMarging, int horizontalMarging, String outputPath, String format) {
		return addWaterMark(videoPath, imagePath, position, String.valueOf(verticalMarging), String.valueOf(horizontalMarging), outputPath, format);
	}
	
	/**
	 * 添加水印的本地方法
	 * @param videoPath
	 * @param imagePath
	 * @param position 位置
	 * @param verticalMarging
	 * @param horizontalMarging
	 * @param outputPath
	 * @return
	 */
	private static native int addWaterMark(String videoPath, String imagePath, int position, String verticalMarging, String horizontalMarging, String outputPath, String format);
	
	public static native int removeAudio(String input, String output);
	
	public static native int fetchAudio(String input, String output, String format);
	
	public static native int addAudio(String video, String audio, String output, String format);
	
	
	public static native int ffmpeg(String... args);
}
