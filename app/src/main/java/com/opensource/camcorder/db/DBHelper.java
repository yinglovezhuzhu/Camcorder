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

package com.opensource.camcorder.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper extends SQLiteOpenHelper {
	
	private static final String DB_NAME = "shixin_android.db";
	
	private static final int DB_VERSION = 1;
	
	public static DBHelper mDBHelper = null;
	
	public DBHelper(Context context) {
		super(context, DB_NAME, null, DB_VERSION);
	}
	
	public static synchronized DBHelper getInstance(Context context) {
		if(mDBHelper == null) {
			mDBHelper = new DBHelper(context);
		}
		return mDBHelper;
	}
	
	public static synchronized SQLiteDatabase getReadableDatabase(Context context) {
		return getInstance(context).getReadableDatabase();
	}
	
	public static synchronized SQLiteDatabase getWriteableDatabase(Context context) {
		return getInstance(context).getWritableDatabase();
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		//草稿表 video_draft
		//_id(记录id); apply_vid(); uid(所有者用户id); raw_video(原始视频); worked_video(加工视频); cover(封面);
		//pic_name(封面文件名); video_name(视频文件名); description(视频描述); fee_mode(收费方式); fee_price(收费价格); 
		//share_mode(共享方式); share_key(共享key); visible_friends(可见好友/公开为提醒好友); tag(标签);
		//category(分类); type(类型); video_type(视频类型); timestamp(时间戳); clip_num(编辑次数); singnature(签名);
		//share_weixin_friend(分享微信朋友圈); share_sina_weibo(分享新浪微博); longitude(地理经度); latitude(地理维度);
		//location(地理位置); effet_tag(效果标签); filter(滤镜); filter_data(滤镜数据); music(配乐); music_groud_id(配乐id);
		//watermark(水印); watermark_group_id(水印组id); theme(主题); video_cover_timestamp(视频封面的时间戳);
		//process_theme(处理视频的theme); process_video(处理完毕的视频); is_processed(是否已经处理完成/视频的所有处理);

        db.execSQL("create table if not exists video_draft(_id INTEGER PRIMARY KEY AUTOINCREMENT, apply_vid TEXT, " +
                "uid TEXT, raw_video TEXT, worked_video TEXT, cover TEXT, pic_name TEXT, video_name TEXT, " +
                "description TEXT, fee_mode INTEGER, fee_price INTEGER, share_mode INTEGER, share_key TEXT, visible_friends TEXT, " +
                "tag TEXT, category TEXT, type INTEGER, video_type INTEGER, timestamp INTEGER, clip_num INTEGER, singnature TEXT, " +
                "share_weixin_freind)");

        //水印表 watermark
        //_id(记录id); uid(用户id); download_state(下载状态 0 未下载; 1 下载中; 2 已下载); used_state(使用状态 0 未使用; 1 已使用);
        // watermark_id(水印id); data(水印数据); type(水印类型); timestamp(时间戳);
        db.execSQL("create table if not exists watermark(_id INTEGER PRIMARY KEY AUTOINCREMENT, uid TEXT, download_state INTEGER DEFAULT 0, " +
                "used_state INTEGER DEFAULT 0, watermark_id TEXT, data TEXT, type INTEGER, timestamp INTEGER)");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// TODO Auto-generated method stub

	}

}
