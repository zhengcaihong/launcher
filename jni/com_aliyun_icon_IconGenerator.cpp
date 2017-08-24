#include "com_aliyun_icon_IconGenerator.h"
#include "IconGenerator.h"
#include <android/bitmap.h>
#include <pthread.h>

#include <stdio.h>
#include <android/log.h>
#include <sys/time.h>

static timeval start;
static timeval end;

static char showBuffer[256];
static float timeuse;

static pthread_mutex_t lock=PTHREAD_MUTEX_INITIALIZER;

JNIEXPORT jint JNICALL Java_com_aliyun_icon_IconGenerator_generator(JNIEnv *env,
		jobject thiz, jintArray input, jintArray parameters, jintArray output) {
	pthread_mutex_lock(&lock);
	sprintf(showBuffer, "icon!!!!");
	__android_log_write(ANDROID_LOG_DEBUG, "icongenerator", showBuffer);

	int *smallIconInt = (int*) (env->GetIntArrayElements(input, 0));
	int *paras = (int*) (env->GetIntArrayElements(parameters, 0));
	int *bigIconInt = (int*) (env->GetIntArrayElements(output, 0));

	unsigned char *smallIconUC = (unsigned char *) smallIconInt;
	unsigned char *bigIconUC = (unsigned char *) bigIconInt;

	gettimeofday(&start, NULL);
	Generator(smallIconUC, paras, bigIconUC);
	gettimeofday(&end, NULL);
	timeuse = 1000000 * (end.tv_sec - start.tv_sec) + end.tv_usec
			- start.tv_usec;
	timeuse /= 1000000;
	sprintf(showBuffer, "icontime ------------------  %f ", timeuse * 1000);
	__android_log_write(ANDROID_LOG_DEBUG, "icongenerator", showBuffer);

	sprintf(showBuffer, "pro finish!");
	__android_log_write(ANDROID_LOG_DEBUG, "icongenerator", showBuffer);

	env->ReleaseIntArrayElements(input, smallIconInt, 0);
	sprintf(showBuffer, "release 1 ok!");
	__android_log_write(ANDROID_LOG_DEBUG, "icongenerator", showBuffer);

	env->ReleaseIntArrayElements(output, bigIconInt, 0);

	sprintf(showBuffer, "release 2 ok!");
	__android_log_write(ANDROID_LOG_DEBUG, "icongenerator", showBuffer);

	env->ReleaseIntArrayElements(parameters, paras, 0);

	sprintf(showBuffer, "release ok!");
	__android_log_write(ANDROID_LOG_DEBUG, "icongenerator", showBuffer);

	pthread_mutex_unlock(&lock);

	return 1;
}
