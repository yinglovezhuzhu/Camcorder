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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

public class ZipUtil {
	
	private ZipUtil() { }

    /**
     * UnZip files from zip file.
     * @param zipFilePath
     * @param folderPath
     * @param selectedEntries
     */
	public static void unZipFile(String zipFilePath, String folderPath, List<String> selectedEntries) {
		File folder = new File(folderPath);
		if(!folder.exists() && !folder.mkdirs()) {
			//无法解压到此目录
			return;
		}
		File srcFile = new File(zipFilePath);
		if(!srcFile.exists() || srcFile.isDirectory()) {
			//文件不存在或者文件是目录
			return;
		}
		ZipFile zipFile = null;
		try {
			zipFile = new ZipFile(srcFile);
			if(null == selectedEntries) {
				Enumeration<? extends ZipEntry> entries = zipFile.entries();
				while(entries.hasMoreElements()) {
					ZipEntry entry = entries.nextElement();
					unZipFileFromZip(zipFile, entry, folder);
				}
			} else {
				if(selectedEntries.isEmpty()){
					return;
				}
				for (String entryName : selectedEntries) {
					ZipEntry entry = zipFile.getEntry(entryName);
					unZipFileFromZip(zipFile, entry, folder);
				}
			}
		} catch (ZipException ze) {
			ze.printStackTrace();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		} catch (IllegalStateException ise) {
			//Like do some work when zip file is closed.
			ise.printStackTrace();
		} finally {
			if(null != zipFile) {
				try {
					zipFile.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	/**
	 * UnZip a file from a Zip file.
	 * @param zipFile
	 * @param entry
	 * @param folder
	 */
	private static void unZipFileFromZip(ZipFile zipFile, ZipEntry entry, File folder) {
		File file = new File(folder, entry.getName());
		if(entry.isDirectory()) {
			if(!file.exists() && !file.mkdirs()) {
				//创建文件夹失败
				System.out.println("Craete folder failed, no write permission");
			}
		} else {
			int readLen = 0;
			byte [] buffer = new byte[1024*1024];
			InputStream is = null;
			FileOutputStream fos = null;
			try {
				is = zipFile.getInputStream(entry);
				fos = new FileOutputStream(file);
				while((readLen = is.read(buffer)) != -1) {
					fos.write(buffer, 0, readLen);
				}
			} catch (IOException e) {
				// TODO 复制文件失败
				e.printStackTrace();
			} finally {
				try {
					if(null != is) {
						is.close();
					}
					if(null != fos) {
						fos.close();
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

}
