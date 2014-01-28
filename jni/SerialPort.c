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

#include <termios.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <string.h>
#include <jni.h>

#include "android/log.h"
static const char *TAG="serial_port";
#define LOGI(fmt, args...) __android_log_print(ANDROID_LOG_INFO,  TAG, fmt, ##args)
#define LOGD(fmt, args...) __android_log_print(ANDROID_LOG_DEBUG, TAG, fmt, ##args)
#define LOGE(fmt, args...) __android_log_print(ANDROID_LOG_ERROR, TAG, fmt, ##args)


#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     com_android_barcode_SerialPort
 * Method:    open
 * Signature: (Ljava/lang/String;I)I
 */
JNIEXPORT jint JNICALL Java_com_android_barcode_SerialPort_open
  (JNIEnv *, jclass, jstring, jint);

/*
 * Class:     com_android_barcode_SerialPort
 * Method:    write
 * Signature: (ILjava/lang/String;I)I
 */
JNIEXPORT jint JNICALL Java_com_android_barcode_SerialPort_write
  (JNIEnv *, jobject, jint, jstring, jint);

/*
 * Class:     com_android_barcode_SerialPort
 * Method:    read
 * Signature: (II)[B
 */
JNIEXPORT jbyteArray JNICALL Java_com_android_barcode_SerialPort_read
  (JNIEnv *, jobject, jint, jint);

/*
 * Class:     com_android_barcode_SerialPort
 * Method:    close
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_com_android_barcode_SerialPort_close
  (JNIEnv *, jobject, jint);

#ifdef __cplusplus
}
#endif

static speed_t getBaudrate(jint baudrate)
{
	switch(baudrate) {
	case 0: return B0;
	case 50: return B50;
	case 75: return B75;
	case 110: return B110;
	case 134: return B134;
	case 150: return B150;
	case 200: return B200;
	case 300: return B300;
	case 600: return B600;
	case 1200: return B1200;
	case 1800: return B1800;
	case 2400: return B2400;
	case 4800: return B4800;
	case 9600: return B9600;
	case 19200: return B19200;
	case 38400: return B38400;
	case 57600: return B57600;
	case 115200: return B115200;
	case 230400: return B230400;
	case 460800: return B460800;
	case 500000: return B500000;
	case 576000: return B576000;
	case 921600: return B921600;
	case 1000000: return B1000000;
	case 1152000: return B1152000;
	case 1500000: return B1500000;
	case 2000000: return B2000000;
	case 2500000: return B2500000;
	case 3000000: return B3000000;
	case 3500000: return B3500000;
	case 4000000: return B4000000;
	default: return -1;
	}
}

/*
 * Class:     cedric_serial_SerialPort
 * Method:    open
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT jint JNICALL Java_com_android_barcode_SerialPort_open
  (JNIEnv *env, jobject thiz, jstring path, jint baudrate)
{
	int fd;
	speed_t speed;

	/* Check arguments */
	{
		speed = getBaudrate(baudrate);
		if (speed == -1) {
			/* TODO: throw an exception */
			LOGE("Invalid baudrate");
		}
	}

	/* Opening device */
	{
		jboolean iscopy;
		const char *path_utf = (*env)->GetStringUTFChars(env, path, &iscopy);
		LOGD("Opening serial port %s", path_utf);
		fd = open(path_utf, O_RDWR | O_SYNC);
		LOGD("open() fd = %d", fd);
		(*env)->ReleaseStringUTFChars(env, path, path_utf);
		if (fd == -1)
		{
			/* Throw an exception */
			LOGE("Cannot open port");
			/* TODO: throw an exception */
		}
	}

	/* Configure device */
	{
		struct termios cfg;
		LOGD("Configuring serial port");
		if (tcgetattr(fd, &cfg))
		{
			LOGE("tcgetattr() failed");
			close(fd);
			/* TODO: throw an exception */
		}

		cfmakeraw(&cfg);
		cfsetispeed(&cfg, speed);
		cfsetospeed(&cfg, speed);
		/*******************************/
  		cfg.c_cflag &= ~CSIZE;
    		cfg.c_lflag &= ~(ICANON|ECHO|ECHOE|ISIG);
    		cfg.c_oflag &= ~OPOST;
		//'8' bit
        	cfg.c_cflag |= CS8; 
        	//'N' PARENB
        	cfg.c_cflag &= ~PARENB;
		cfg.c_iflag &= ~INPCK;
  		//'1' STOP
        	cfg.c_cflag &= ~CSTOPB; 

    		cfg.c_cc[VTIME] = 15; 
    		cfg.c_cc[VMIN] = 0; 
    		tcflush(fd, TCIFLUSH);
		/*********************************/

		if (tcsetattr(fd, TCSANOW, &cfg))
		{
			LOGE("tcsetattr() failed");
			close(fd);
			/* TODO: throw an exception */
		}
	}


	return fd;
}
/*
 * Class:     cedric_serial_SerialPort
 * Method:    write
 * Signature: ()V
 */

JNIEXPORT jint JNICALL Java_com_android_barcode_SerialPort_write
  (JNIEnv *env, jobject obj, jint fd, jstring str, jint len)
{
	jboolean iscopy;
	const char *buff_utf = (*env)->GetStringUTFChars(env, str, &iscopy);
	int wlen = 0;
	wlen = write(fd, buff_utf, len);
	if(wlen <= 0)
	{
		LOGE("Write failed\n");
	}
	(*env)->ReleaseStringUTFChars(env, str, buff_utf);
	return wlen;
}
/*
 * Class:     cedric_serial_SerialPort
 * Method:    read
 * Signature: ()V
 */

JNIEXPORT jbyteArray JNICALL Java_com_android_barcode_SerialPort_read
  (JNIEnv *env, jobject obj, jint fd, jint len)
{
	int reval = 0, nread = 0;
	struct timeval tv;
	jbyteArray jba;
	fd_set rfds;
	char *buff = malloc(len);

	do {
		FD_ZERO(&rfds);
		FD_SET(fd, &rfds);
		tv.tv_sec = 0;
		tv.tv_usec = 100000;
		if ((reval = select(1 + fd, &rfds, NULL, NULL, &tv)) > 0)
		{
			if(FD_ISSET(fd, &rfds))
			{
				int temp = read(fd, buff + nread, len - nread);
				if(temp < 0)
				{
					LOGE("read serial port error\n");
					free(buff);
					return NULL;
				}
				nread += temp;
			}
		}
		else if(reval == 0 && nread != 0)			//timeout means 
		{
			break;
/*			int i, j;
			for(i = 0, j = 0; i < nread; i++)
			{
				if(buff[i] != 0)
				{
					result[j] = buff[i];
					j++;
				}
			//	LOGD("0x%2x ", buff[i]);
			}*/
//			jba = (*env)->NewByteArray(env, j);
//			(*env)->SetByteArrayRegion(env, jba, 0, j, result);
//			LOGD("readlength=%d\n, reallength=%d\n, all data received!\n", nread, j);
		}
		else
		{
			free(buff);
			return NULL;
		}
	} 
	while(nread < len);
	jba = (*env)->NewByteArray(env, nread);
	(*env)->SetByteArrayRegion(env, jba, 0, nread, buff);
	free(buff);
	return jba;
}
/*
 * Class:     cedric_serial_SerialPort
 * Method:    close
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_android_barcode_SerialPort_close
  (JNIEnv *env, jobject obj, jint fd)
{
	close(fd);
}
