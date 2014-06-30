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


import android.os.Parcel;
import android.os.Parcelable;


public class SavedFrames implements Parcelable {

    private byte [] frameBytesData = null;
    private long timeStamp = 0L;
    private String cachePath = null;
    private int frameSize = 0;

    public byte[] getFrameBytesData() {
        return frameBytesData;
    }

    public void setFrameBytesData(byte[] frameBytesData) {
        this.frameBytesData = frameBytesData;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }

    public SavedFrames(byte[] frameBytesData, long timeStamp) {
        this.frameBytesData = frameBytesData;
        this.timeStamp = timeStamp;
    }


    public String getCachePath() {
        return cachePath;
    }

    public void setCachePath(String cachePath) {
        this.cachePath = cachePath;
    }

    public int getframeSize() {
        return frameSize;
    }

    public void setframeSize(int frameSize) {
        this.frameSize = frameSize;
    }

    public SavedFrames(Parcel in) {
        readFromParcel(in);
    }

    public SavedFrames() {
        frameSize = 0;
        frameBytesData = new byte[0];
        timeStamp = 0L;
        cachePath = null;
    }

    public static final Creator<SavedFrames> CREATOR = new Creator<SavedFrames>() {
        @Override
        public SavedFrames createFromParcel(Parcel paramParcel) {
            SavedFrames savedFrame = new SavedFrames();
            savedFrame.readFromParcel(paramParcel);
            return savedFrame;
        }

        @Override
        public SavedFrames[] newArray(int paramInt) {
            return new SavedFrames[paramInt];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int arg1) {
        out.writeLong(timeStamp);
        out.writeInt(frameSize);
        out.writeByteArray(frameBytesData);
        out.writeString(cachePath);
    }

    private void readFromParcel(Parcel in) {
        timeStamp = in.readLong();
        frameSize = in.readInt();
        frameBytesData = new byte[frameSize];
        in.readByteArray(frameBytesData);
        cachePath = in.readString();
    }

}
