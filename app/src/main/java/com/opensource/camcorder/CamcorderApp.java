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

import android.app.Application;
import android.os.Environment;
import android.os.Process;
import android.widget.Toast;

import com.opensource.camcorder.entity.Decoration;
import com.opensource.camcorder.utils.CamcorderUtil;
import com.opensource.camcorder.utils.StringUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Use:
 * Created by yinglovezhuzhu@gmail.com on 2014-07-24.
 */
public class CamcorderApp extends Application {

    public static String APP_FOLDER = Environment.getExternalStorageDirectory().getAbsolutePath();

    private static Map<String, List<? extends Decoration>> mDecorations = new HashMap<String, List<? extends Decoration>>();

    @Override
    public void onCreate() {

        APP_FOLDER = CamcorderUtil.getApplicationFolder(getResources().getString(R.string.app_name));
        if(StringUtil.isEmpty(APP_FOLDER)) {
            Toast.makeText(this, "没有可用的存储设备!", Toast.LENGTH_SHORT).show();
            Process.killProcess(Process.myPid());
        }
        super.onCreate();
    }

    public static void putDecorations(String key, List<? extends Decoration> decorations) {
        mDecorations.put(key, decorations);
    }

    public static List<? extends Decoration> getDecorations(String key) {
        return mDecorations.get(key);
    }
}
