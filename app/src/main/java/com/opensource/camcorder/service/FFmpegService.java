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

package com.opensource.camcorder.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.opensource.camcorder.IFFmpegService;
import com.opensource.camcorder.utils.FFmpegTool;

/**
 * Usage 
 * 
 * @author yinglovezhuzhu@gmail.com
 */
public class FFmpegService extends Service {
	
	private static final String TAG = "FFmpegService";
	
	private final LocalBinder mBinder = new LocalBinder();
	
	@Override
	public void onCreate() {
		super.onCreate();
		System.out.println("FFmpegService OnCreate()+++++++++++++++++++++++++>>>>>>>>>>>>>>>>>");
	}

	@Override
	public IBinder onBind(Intent intent) {
		if(intent.hasExtra("AAA")) {
			System.out.println(intent.getStringExtra("AAA"));
		}
		return mBinder;
	}

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		return super.onStartCommand(intent, flags, startId);
	}
	
	@Override
	public void onRebind(Intent intent) {
		// TODO Auto-generated method stub
		super.onRebind(intent);
	}
	
	@Override
	public void onDestroy() {
		Log.w(TAG, "FFmpegService +++++++++++++++++++++++++>>>>>>>>>>>>>>>>> onDestroy()");
		FFmpegTool.exit(0);
//		Process.killProcess(Process.myPid());
		super.onDestroy();
	}
	
	
	public final class LocalBinder extends IFFmpegService.Stub {

		@Override
		public int cutVideo(String input, String output, String start,
				String duration) throws RemoteException {
			int ret = Integer.MIN_VALUE;
			ret = FFmpegTool.cutVideo(input, output, start, duration);
			stopSelf();
			return ret;
		}

		@Override
		public int mergeVideo(String cache, String output, String[] inputs)
				throws RemoteException {
			int ret = Integer.MIN_VALUE;
			ret = FFmpegTool.mergeVideo(cache, output, inputs);
			stopSelf();
			return ret;
		}

		@Override
		public int addWaterMark(String videoPath, String imagePath,
				int position, int verticalMarging, int horizontalMarging,
				String outputPath, String format) throws RemoteException {
			int ret = Integer.MIN_VALUE;
			ret = FFmpegTool.addWaterMark(videoPath, imagePath, position, verticalMarging, horizontalMarging, outputPath, format);
			stopSelf();
			return ret;
		}

		@Override
		public int removeAudio(String input, String output)
				throws RemoteException {
			int ret = Integer.MIN_VALUE;
			ret = FFmpegTool.removeAudio(input, output);
			stopSelf();
			return ret;
		}

		@Override
		public int fetchAudio(String input, String output, String format)
				throws RemoteException {
			int ret = Integer.MIN_VALUE;
			ret = FFmpegTool.fetchAudio(input, output, format);
			stopSelf();
			return ret;
		}

		@Override
		public int addAudio(String video, String audio, String output, String format)
				throws RemoteException {
			int ret = Integer.MIN_VALUE;
			ret = FFmpegTool.addAudio(video, audio, output, format);
			stopSelf();
			return ret;
		}

        @Override
        public int ffmpeg(String[] args) throws RemoteException {
            int ret = Integer.MAX_VALUE;
            ret = FFmpegTool.ffmpeg(args);
            stopSelf();
            return ret;
        }

    }
}
