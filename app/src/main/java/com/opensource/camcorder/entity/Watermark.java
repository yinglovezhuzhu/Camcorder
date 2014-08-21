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

import java.util.ArrayList;
import java.util.List;

/**
 * Use:
 * Created by yinglovezhuzhu@gmail.com on 2014-07-29.
 */
public class Watermark extends Decoration {

    private WatermarkData userData;

    public Watermark() {
    }

    public Watermark(String name) {
        super(name);
    }

    public WatermarkData getUserData() {
        return userData;
    }

    public void setUserData(WatermarkData userData) {
        this.userData = userData;
    }

    @Override
    public String toString() {
        return "Watermark{" +
                super.toString() +
                ", userData=" + userData +
                '}';
    }

    public static class WatermarkData {
        private int timestamp = 0;
        private List<ElementData> elements = new ArrayList();

        public int getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(int timestamp) {
            this.timestamp = timestamp;
        }

        public List<ElementData> getElements() {
            return elements;
        }

        public void setElements(List<ElementData> elements) {
            this.elements = elements;
        }

        public void addElement(ElementData element) {
            elements.add(element);
        }

        @Override
        public String toString() {
            return "WatermarkData{" +
                    "timestamp=" + timestamp +
                    ", elements=" + elements +
                    '}';
        }
    }

    public static class ElementData {
        private int idx;
        private String type;
        private String valueSource;
        private String defaultValue;
        private int editable;
        private Rect rect;
        private Font font;
        private Shadow shadow;
        private List<ResourceData> resources = new ArrayList<ResourceData>();

        public int getIdx() {
            return idx;
        }

        public void setIdx(int idx) {
            this.idx = idx;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getValueSource() {
            return valueSource;
        }

        public void setValueSource(String valueSource) {
            this.valueSource = valueSource;
        }

        public String getDefaultValue() {
            return defaultValue;
        }

        public void setDefaultValue(String defaultValue) {
            this.defaultValue = defaultValue;
        }

        public int getEditable() {
            return editable;
        }

        public void setEditable(int editable) {
            this.editable = editable;
        }

        public Rect getRect() {
            return rect;
        }

        public void setRect(Rect rect) {
            this.rect = rect;
        }

        public Font getFont() {
            return font;
        }

        public void setFont(Font font) {
            this.font = font;
        }

        public Shadow getShadow() {
            return shadow;
        }

        public void setShadow(Shadow shadow) {
            this.shadow = shadow;
        }

        public List<ResourceData> getResources() {
            return resources;
        }

        public void setResources(List<ResourceData> resources) {
            this.resources = resources;
        }

        @Override
        public String toString() {
            return "ElementData{" +
                    "idx=" + idx +
                    ", type='" + type + '\'' +
                    ", valueSource='" + valueSource + '\'' +
                    ", defaultValue='" + defaultValue + '\'' +
                    ", editable=" + editable +
                    ", rect=" + rect +
                    ", font=" + font +
                    ", shadow=" + shadow +
                    ", resources=" + resources +
                    '}';
        }
    }

    /**
     * 水印位置的实体类
     */
    public static class Rect {
        private int x;
        private int y;
        private int width;
        private int height;

        public int getX() {
            return x;
        }

        public void setX(int x) {
            this.x = x;
        }

        public int getY() {
            return y;
        }

        public void setY(int y) {
            this.y = y;
        }

        public int getWidth() {
            return width;
        }

        public void setWidth(int width) {
            this.width = width;
        }

        public int getHeight() {
            return height;
        }

        public void setHeight(int height) {
            this.height = height;
        }

        @Override
        public String toString() {
            return "Rect{" +
                    "x=" + x +
                    ", y=" + y +
                    ", width=" + width +
                    ", height=" + height +
                    '}';
        }
    }

    /**
     * 文字信息
     */
    public static class Font {
        private int size;
        private String type;
        private String color;
        private String bold;
        private int max;
        private int alpha;

        public int getSize() {
            return size;
        }

        public void setSize(int size) {
            this.size = size;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getColor() {
            return color;
        }

        public void setColor(String color) {
            this.color = color;
        }

        public String getBold() {
            return bold;
        }

        public void setBold(String bold) {
            this.bold = bold;
        }

        public int getMax() {
            return max;
        }

        public void setMax(int max) {
            this.max = max;
        }

        public int getAlpha() {
            return alpha;
        }

        public void setAlpha(int alpha) {
            this.alpha = alpha;
        }

        @Override
        public String toString() {
            return "Font{" +
                    "size=" + size +
                    ", type='" + type + '\'' +
                    ", color='" + color + '\'' +
                    ", bold='" + bold + '\'' +
                    ", max=" + max +
                    ", alpha=" + alpha +
                    '}';
        }
    }

    /**
     * 影子属性
     */
    public static class Shadow {
        private int x;
        private int y;
        private int size;
        private String color;
        private int alpha;

        public int getX() {
            return x;
        }

        public void setX(int x) {
            this.x = x;
        }

        public int getY() {
            return y;
        }

        public void setY(int y) {
            this.y = y;
        }

        public int getSize() {
            return size;
        }

        public void setSize(int size) {
            this.size = size;
        }

        public String getColor() {
            return color;
        }

        public void setColor(String color) {
            this.color = color;
        }

        public int getAlpha() {
            return alpha;
        }

        public void setAlpha(int alpha) {
            this.alpha = alpha;
        }

        @Override
        public String toString() {
            return "Shadow{" +
                    "x=" + x +
                    ", y=" + y +
                    ", size=" + size +
                    ", color='" + color + '\'' +
                    ", alpha=" + alpha +
                    '}';
        }
    }

    /**
     * 其他资源属性
     */
    public static class ResourceData {
        private String key;
        private String value;
        private String name;

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return "ResourceData{" +
                    "key='" + key + '\'' +
                    ", value='" + value + '\'' +
                    ", name='" + name + '\'' +
                    '}';
        }
    }
}
