
/*
 * Copyright 2009 Cedric Priscal
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 */

package com.android.barcode;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import android.util.Log;


public class SerialPort {
	private static final String TAG = "SerialPortNative";

	/*
	 * Do not remove or rename the field mFd: it is used by native method close();
	 */
	private int fd;
	private int writelen;
	private String str;

	public SerialPort(String device, int baudrate) throws SecurityException, IOException {

		fd = open(device, baudrate);
		if (fd < 0) {
			Log.e(TAG, "native open returns null");
			throw new IOException();
		}
	}
	public int getFd()
	{
		return fd;
	}
	public int WriteSerial(int fd, String str, int len) {
		writelen = write(fd, str, len);
		return writelen;
	}
	public String ReadSerial(int fd, int len) throws UnsupportedEncodingException{
		byte[] tmp;
		tmp = read(fd,len);
		if(tmp == null)
		{
			return null;
		}
		if(isUTF8(tmp))
		{
			str = new String(tmp, "utf8");
			Log.d(TAG, "is a utf8 string");
		}
		else
		{
			str = new String(tmp, "gbk");
			Log.d(TAG, "is a gbk string");
		}
		return str;
	}
	public void SerialClose(int fd){
		close(fd);
	}
	
	private boolean isUTF8(byte[] sx){
		Log.d(TAG, "begian to set codeset");
		for(int i = 0; i < sx.length; )
		{
			if(sx[i] < 0)
			{
				if((sx[i]>>>5) == 0x7FFFFFE)
				{
					if(((i + 1) < sx.length) && ((sx[i + 1]>>>6) == 0x3FFFFFE))
					{
						i = i + 2;
					}
					else
					{
						return false;
					}
				}
			else if((sx[i]>>>4) == 0xFFFFFFE)
				{
					if(((i + 2) < sx.length) && ((sx[i + 1]>>>6) == 0x3FFFFFE) && ((sx[i + 2]>>>6) == 0x3FFFFFE))
					{
						i = i + 3;
					}
					else
					{
						return false;
					}
				}
				else
				{
					return false;
				}
			}
			else
			{
				i++;
			}
		}
		return true;
	}
	// JNI
	public native static int open(String dev, int baudrate);
	public native static int write(int fd, String wb, int len);
	public native static byte[] read(int fd, int len);
	public native void close(int fd);
	//public native static void close(int fd);
	static {
		System.loadLibrary("serial_port");
	}
}
