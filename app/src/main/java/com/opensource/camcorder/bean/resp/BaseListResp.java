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

package com.opensource.camcorder.bean.resp;

import java.util.ArrayList;
import java.util.List;

/**
 * Use:
 * Created by yinglovezhuzhu@gmail.com on 2014-08-20.
 */
public class BaseListResp<T> extends BaseResp {

    private List<T> pageList = new ArrayList<T>();
    private PageInfo pageTurn;

    public List<T> getPageList() {
        return pageList;
    }

    public void setPageList(List<T> pageList) {
        this.pageList = pageList;
    }

    public PageInfo getPageTurn() {
        return pageTurn;
    }

    public void setPageTurn(PageInfo pageTurn) {
        this.pageTurn = pageTurn;
    }

    @Override
    public String toString() {
        return "BaseListResp{" +
                super.toString() +
                ", pageList=" + pageList +
                ", pageTurn=" + pageTurn +
                '}';
    }

    /**
     * 分页信息
     */
    public static class PageInfo {

        private int currentPage = 0;
        private int start = 0;
        private int end = 0;
        private int firstPage = 0;
        private int prevPage = 0;
        private int nextPage = 0;
        private int page = 0;
        private int pageCount = 0;
        private int pageSize = 0;
        private int rowCount = 0;

        public int getCurrentPage() {
            return currentPage;
        }

        public void setCurrentPage(int currentPage) {
            this.currentPage = currentPage;
        }

        public int getStart() {
            return start;
        }

        public void setStart(int start) {
            this.start = start;
        }

        public int getEnd() {
            return end;
        }

        public void setEnd(int end) {
            this.end = end;
        }

        public int getFirstPage() {
            return firstPage;
        }

        public void setFirstPage(int firstPage) {
            this.firstPage = firstPage;
        }

        public int getPrevPage() {
            return prevPage;
        }

        public void setPrevPage(int prevPage) {
            this.prevPage = prevPage;
        }

        public int getNextPage() {
            return nextPage;
        }

        public void setNextPage(int nextPage) {
            this.nextPage = nextPage;
        }

        public int getPage() {
            return page;
        }

        public void setPage(int page) {
            this.page = page;
        }

        public int getPageCount() {
            return pageCount;
        }

        public void setPageCount(int pageCount) {
            this.pageCount = pageCount;
        }

        public int getPageSize() {
            return pageSize;
        }

        public void setPageSize(int pageSize) {
            this.pageSize = pageSize;
        }

        public int getRowCount() {
            return rowCount;
        }

        public void setRowCount(int rowCount) {
            this.rowCount = rowCount;
        }

        @Override
        public String toString() {
            return "PageInfo{" +
                    "currentPage=" + currentPage +
                    ", start=" + start +
                    ", end=" + end +
                    ", firstPage=" + firstPage +
                    ", prevPage=" + prevPage +
                    ", nextPage=" + nextPage +
                    ", page=" + page +
                    ", pageCount=" + pageCount +
                    ", pageSize=" + pageSize +
                    ", rowCount=" + rowCount +
                    '}';
        }
    }
}
