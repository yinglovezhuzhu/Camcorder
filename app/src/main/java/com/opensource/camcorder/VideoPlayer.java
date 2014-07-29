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

import android.content.Context;
import android.media.MediaPlayer;
import android.view.View;
import android.widget.VideoView;


public class VideoPlayer implements View.OnClickListener,
        MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener {

    private final VideoView mVideoView;
    private final View mIconView;
    private final View mThumbView;
    private final View mProgressView;
    private final String mVideoPath;

    public VideoPlayer(View contentView, Context context, String videoPath) {

        mVideoView = (VideoView) contentView.findViewById(R.id.vv_video_edit_preview);
        mIconView = contentView.findViewById(R.id.iv_video_edit_icon);
        mThumbView = contentView.findViewById(R.id.iv_video_edit_thumb);
        mProgressView = contentView.findViewById(R.id.pb_video_edit_progress);

        mVideoPath = videoPath;

        mVideoView.setOnErrorListener(this);
        mVideoView.setOnCompletionListener(this);
        mVideoView.setVideoPath(mVideoPath);

        // make the video view handle keys for seeking and pausing
        mVideoView.requestFocus();

        mIconView.setOnClickListener(this);

    }

    public void start() {
        mVideoView.start();
        mProgressView.setVisibility(View.GONE);
        mThumbView.setVisibility(View.GONE);
        mIconView.setVisibility(View.GONE);
    }

    public void stop() {
        mVideoView.stopPlayback();
        mThumbView.setVisibility(View.VISIBLE);
        mIconView.setVisibility(View.VISIBLE);
    }

    public void onPause() {
        mVideoView.suspend();
    }

    public void onResume() {
        mVideoView.resume();
    }

    public void onDestroy() {
        mVideoView.stopPlayback();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.iv_video_edit_icon:
                start();
                break;
            default:
                break;
        }
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        return false;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        mIconView.setVisibility(View.VISIBLE);
    }
}
