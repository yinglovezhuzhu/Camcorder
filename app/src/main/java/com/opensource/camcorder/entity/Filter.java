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
 * Use: 滤镜实体类
 * Created by yinglovezhuzhu@gmail.com on 2014-08-14.
 */
public class Filter extends Decoration {

    private FilterData userData;

    public FilterData getUserData() {
        return userData;
    }

    public void setUserData(FilterData userData) {
        this.userData = userData;
    }

    @Override
    public String toString() {
        return "Filter{" +
                super.toString() +
                ", userData=" + userData +
                '}';
    }

    /**
     * 滤镜数据类
     */
    public static class FilterData {
        private int filterType;
        private String filterData;

        public int getFilterType() {
            return filterType;
        }

        public void setFilterType(int filterType) {
            this.filterType = filterType;
        }

        public String getFilterData() {
            return filterData;
        }

        public void setFilterData(String filterData) {
            this.filterData = filterData;
        }

        @Override
        public String toString() {
            return "FilterData{" +
                    "filterType=" + filterType +
                    ", filterData='" + filterData + '\'' +
                    '}';
        }
    }
}
