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

package com.opensource.camcorder.entity;

/**
 * Use:配乐实体类
 * Created by yinglovezhuzhu@gmail.com on 2014-08-14.
 */
public class Music extends Decoration {

    private MusicData musicData;

    public Music() {
    }

    public Music(String name) {
        super(name);
    }

    public MusicData getMusicData() {
        return musicData;
    }

    public void setMusicData(MusicData musicData) {
        this.musicData = musicData;
    }

    @Override
    public String toString() {
        return "Music{" +
                super.toString() +
                ", musicData=" + musicData +
                '}';
    }

    public static class MusicData {
        private int time;
        private String name;
        private String url;

        public int getTime() {
            return time;
        }

        public void setTime(int time) {
            this.time = time;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        @Override
        public String toString() {
            return "MusicData{" +
                    "time=" + time +
                    ", name='" + name + '\'' +
                    ", url='" + url + '\'' +
                    '}';
        }
    }
}
