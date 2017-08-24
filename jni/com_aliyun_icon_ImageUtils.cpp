#include "com_aliyun_icon_ImageUtils.h"
#include "IconGenerator.h"
#include <android/bitmap.h>
#include <pthread.h>

#include <stdio.h>
#include <android/log.h>
#include <sys/time.h>

static pthread_mutex_t lock = PTHREAD_MUTEX_INITIALIZER;
#define LOG_TAG "ImageUtils"

#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)
#define  LOGW(...)  __android_log_print(ANDROID_LOG_WARN,LOG_TAG,__VA_ARGS__)
#define  LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG,LOG_TAG,__VA_ARGS__)
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define SWAP16(A)   (( ((uint16_t)(A) & 0xff00) >> 8)    | \
                                       (( (uint16_t)(A) & 0x00ff) << 8))
#define SWAP32(A)   ((( (uint32_t)(A) & 0xff000000) >> 24) | \
                                       (( (uint32_t)(A) & 0x00ff0000) >> 8)   | \
                                       (( (uint32_t)(A) & 0x0000ff00) << 8)   | \
                                       (( (uint32_t)(A) & 0x000000ff) << 24))
#define UNUSED(x) (void)(x)

typedef struct {
    uint8_t a;
    uint8_t r;
    uint8_t g;
    uint8_t b;
} argb_t;

bool IsBigEndian() {
    uint16_t a = 0x1234;
    char b = *(char *) &a;
    if (b == 0x12) {
        return true;
    } else {
        return false;
    }
}

JNIEXPORT jlong JNICALL Java_com_aliyun_icon_IconGenerator_getBitmapAverageColor(
        JNIEnv *env, jclass cls, jobject srcBitmap, jint startX, jint startY,
        jint w, jint h, jint stepX, jint stepY) {

    AndroidBitmapInfo srcBmpInfo;
    char* srcPixels;
    char* lineStart;
    uint32_t x;
    uint32_t y;
    uint32_t a = 0;
    uint32_t r = 0;
    uint32_t g = 0;
    uint32_t b = 0;
    long as = 0;
    long rs = 0;
    long gs = 0;
    long bs = 0;
    int pixelCount = 0;
    int colorSize;
    int ret;
    uint32_t endX;
    uint32_t endY;
    bool isBigEndian;
    bool paramError = false;

    UNUSED(cls);
    isBigEndian = IsBigEndian();

    if ((ret = AndroidBitmap_getInfo(env, srcBitmap, &srcBmpInfo)) < 0) {
        LOGE("AndroidBitmap_getInfo() failed ! error=%d", ret);
        return 0x100000000;
    }

    if (startX < 0 || (uint32_t)startX >= srcBmpInfo.width) {
        startX = 0;
        paramError = true;
    }

    if (startY < 0 || (uint32_t)startY >= srcBmpInfo.height) {
        startY = 0;
        paramError = true;
    }
    endX = startX + w;
    if (endX > srcBmpInfo.width) {
        endX = srcBmpInfo.width;
        paramError = true;
    }
    endY = startY + h;
    if (endY > srcBmpInfo.height) {
        endY = srcBmpInfo.height;
        paramError = true;
    }

    if (paramError) {
        LOGE(
                "ERROR! param invalid, just make it work. startX=%d, startY=%d, w=%d, h=%d",
                startX, startY, (endX - startX), (endY - startY));
    }

    if (stepX <= 0) {
        LOGE("stepX must > 0, so just set stepX=1.");
        stepX = 1;
    }

    if (stepY <= 0) {
        LOGE("stepX must > 0, so just set stepX=1.");
        stepY = 1;
    }

    LOGI(
            "getBitmapAverageColor() image :: width is %d; height is %d; stride is %d; format is %d;flags is  %d",
            srcBmpInfo.width, srcBmpInfo.height, srcBmpInfo.stride,
            srcBmpInfo.format, srcBmpInfo.flags);

    if (srcBmpInfo.format == ANDROID_BITMAP_FORMAT_RGBA_8888) {
        colorSize = 4;
    } else if (srcBmpInfo.format == ANDROID_BITMAP_FORMAT_RGB_565
            || srcBmpInfo.format == ANDROID_BITMAP_FORMAT_RGBA_4444) {
        colorSize = 2;
    } else if (srcBmpInfo.format == ANDROID_BITMAP_FORMAT_A_8) {
        colorSize = 1;
    } else {
        LOGE("Bitmap format is not support !");
        return 0x200000000;
    }

    if ((ret = AndroidBitmap_lockPixels(env, srcBitmap, (void **) (&srcPixels)))
            < 0) {
        LOGE("AndroidBitmap_lockPixels() failed ! error=%d", ret);
        return 0x300000000;
    }
    lineStart = srcPixels + srcBmpInfo.stride * startY;
    for (y = startY; y < endY; y += stepY) {

        for (x = startX; x < endX; x += stepX) {
            if (srcBmpInfo.format == ANDROID_BITMAP_FORMAT_RGBA_8888) {

                uint32_t color = *((uint32_t *) (lineStart + x * colorSize));
                if (!isBigEndian) {
                    color = SWAP32(color);
                }

                r = (color & 0xFF000000) >> 24;
                g = (color & 0x00FF0000) >> 16;
                b = (color & 0x0000FF00) >> 8;
                a = (color & 0x000000FF);

            } else if (srcBmpInfo.format == ANDROID_BITMAP_FORMAT_RGB_565) {
                uint16_t color = *((uint16_t *) (lineStart + x * colorSize));
                a = 0xFF;
                r = (color & 0xF800) >> 8; // >> 11 - 3
                g = (color & 0x07E0) >> 3; // >> 5 -2
                b = (color & 0x001F) << 3; // >> -3
            } else if (srcBmpInfo.format == ANDROID_BITMAP_FORMAT_RGBA_4444) {
                uint16_t color = *((uint16_t *) (lineStart + x * colorSize));
                if (!isBigEndian) {
                    color = SWAP16(color);
                }
                r = (color & 0xF000) >> 8; // >> 12 - 4
                g = (color & 0x0F00) >> 4; // >> 8 - 4
                b = (color & 0x00F0); // >> 4 - 4
                a = (color & 0x000F) << 4; //>> 0 - 4
            } else if (srcBmpInfo.format == ANDROID_BITMAP_FORMAT_A_8) {
                uint8_t color = *((uint8_t *) (lineStart + x * colorSize));
                a = color;
                r = 0;
                g = 0;
                b = 0;
            }
            as += a;
            rs += r;
            gs += g;
            bs += b;
            pixelCount++;
            //LOGI("x:%d-y:%d :: a=%d r=%d g=%d b=%d", x, y, a, r, g, b);
        }
        lineStart = lineStart + srcBmpInfo.stride * stepY;
    }
    if (pixelCount != 0) {
        a = (uint32_t)(as / pixelCount);
        r = (uint32_t)(rs / pixelCount);
        g = (uint32_t)(gs / pixelCount);
        b = (uint32_t)(bs / pixelCount);
    }

    AndroidBitmap_unlockPixels(env, srcBitmap);

    return ((a << 24) | (r << 16) | (g << 8) | b);
}
