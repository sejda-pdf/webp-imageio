/*
 * Copyright 2013 Luciad (http://www.luciad.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
#include <stdlib.h>
#include <stdio.h>
#include <string.h>

#include <webp/decode.h>
#include <webp/encode.h>

#include "com_luciad_imageio_webp_WebP.h"
#include "com_luciad_imageio_webp_WebPDecoderOptions.h"
#include "com_luciad_imageio_webp_WebPEncoderOptions.h"

#ifdef UNUSED 
#elif defined(__GNUC__) 
# define UNUSED(x) UNUSED_ ## x __attribute__((unused)) 
#elif defined(__LCLINT__) 
# define UNUSED(x) /*@unused@*/ x 
#else 
# define UNUSED(x) x 
#endif

static int WebPRescalerGetScaledDimensions(int src_width, int src_height,
                                           int* const scaled_width,
                                           int* const scaled_height) {
  // TODO: copied from utils/rescaler_utils.c. Not available in the public libwebp API.
  int width = *scaled_width;
  int height = *scaled_height;

  // if width is unspecified, scale original proportionally to height ratio.
  if (width == 0) {
    width = (src_width * height + src_height / 2) / src_height;
  }
  // if height is unspecified, scale original proportionally to width ratio.
  if (height == 0) {
    height = (src_height * width + src_width / 2) / src_width;
  }
  // Check if the overall dimensions still make sense.
  if (width <= 0 || height <= 0) {
    return 0;
  }

  *scaled_width = width;
  *scaled_height = height;
  return 1;
}

static VP8StatusCode setDecBufferSize(WebPDecoderConfig* const out) {
  // TODO: this is a copy of WebPAllocateDecBuffer from dec/buffer.c. Width/height determination should be shared.
  int w, h;
  if (out == NULL) {
    return VP8_STATUS_INVALID_PARAM;
  }

  w = out->input.width;
  h = out->input.height;
  if (w <= 0 || h <= 0) {
    return VP8_STATUS_INVALID_PARAM;
  }

  if (out->options.use_cropping) {
    const int cw = out->options.crop_width;
    const int ch = out->options.crop_height;
    const int x = out->options.crop_left & ~1;
    const int y = out->options.crop_top & ~1;
    if (x < 0 || y < 0 || cw <= 0 || ch <= 0 || x + cw > w || y + ch > h) {
      return VP8_STATUS_INVALID_PARAM;   // out of frame boundary.
    }
    w = cw;
    h = ch;
  }
  if (out->options.use_scaling) {
    int scaled_width = out->options.scaled_width;
    int scaled_height = out->options.scaled_height;
    if (!WebPRescalerGetScaledDimensions(w, h, &scaled_width, &scaled_height)) {
      return VP8_STATUS_INVALID_PARAM;
    }
    w = scaled_width;
    h = scaled_height;
  }

  out->output.width = w;
  out->output.height = h;

  return VP8_STATUS_OK;
}

JNIEXPORT jint JNICALL Java_com_luciad_imageio_webp_WebP_getInfo(
  JNIEnv *env, jclass UNUSED(cls),
  jbyteArray data, jint offset, jint length,
  jintArray outInfo) {

  jint result = VP8_STATUS_OK;
  jint* info_ptr = NULL;
  int width_height[2];
  jint data_size = 0;
  uint8_t* data_ptr = NULL;

  data_ptr = (uint8_t*)((*env)->GetPrimitiveArrayCritical(env, data, NULL)) + offset;
  if (data_ptr == NULL) {
    result = VP8_STATUS_INVALID_PARAM;
    goto exit;
  }
  data_size = length;

  result = WebPGetInfo(data_ptr, data_size, &width_height[0], &width_height[0] + 1);

  (*env)->ReleasePrimitiveArrayCritical(env, data, data_ptr, JNI_ABORT);

  info_ptr = (*env)->GetIntArrayElements(env, outInfo, NULL);
  if (info_ptr != NULL) {
    info_ptr[0] = width_height[0];
    info_ptr[1] = width_height[1];
    (*env)->ReleaseIntArrayElements(env, outInfo, info_ptr, 0);
  }

exit:
  return result;
}

JNIEXPORT jintArray JNICALL Java_com_luciad_imageio_webp_WebP_decode(
  JNIEnv *env, jclass UNUSED(cls),
  jlong optionsPtr,
  jbyteArray data, jint offset, jint length, 
  jintArray outFlags, 
  jboolean bigendian) {
  jint* flags_ptr = NULL;
  jint data_size = 0;
  uint8_t* data_ptr = NULL;
  jintArray pixels = NULL;
  uint8_t* pixels_ptr = NULL;
  VP8StatusCode status = 0;
  WebPDecoderConfig config;

  flags_ptr = (*env)->GetIntArrayElements(env, outFlags, NULL);
  if (flags_ptr == NULL) {
    goto exit;
  }

  // Init a configuration object
  if(!WebPInitDecoderConfig(&config)) {
    flags_ptr[0] = VP8_STATUS_INVALID_PARAM;
    goto exit;
  }
  config.options = *((WebPDecoderOptions*)(intptr_t)optionsPtr);

  // Retrieve the bitstream features to determine the image's intrinsic size.
  data_ptr = (uint8_t*)((*env)->GetPrimitiveArrayCritical(env, data, NULL)) + offset;
  if (data_ptr == NULL) {
    flags_ptr[0] = VP8_STATUS_INVALID_PARAM;
    goto exit;
  }
  data_size = length;


  status = WebPGetFeatures(data_ptr, data_size, &config.input);
  if (status != VP8_STATUS_OK) {
    flags_ptr[0] = status;
    goto exit;
  }

  if (data_ptr != NULL) {
    (*env)->ReleasePrimitiveArrayCritical(env, data, data_ptr, JNI_ABORT);
    data_ptr = NULL;
  }

  // Set the output buffer size based on the config options
  setDecBufferSize(&config);

  // Allocate the output pixel array of the appropriate size. We allocate an int
  // array because we want integer packed pixels on the Java side.
  pixels = (*env)->NewIntArray(env, config.output.width * config.output.height);
  if (pixels == NULL) {
    flags_ptr[0] = VP8_STATUS_OUT_OF_MEMORY;
    goto exit;
  }

  data_ptr = (uint8_t*)((*env)->GetPrimitiveArrayCritical(env, data, NULL)) + offset;
  pixels_ptr = (*env)->GetPrimitiveArrayCritical(env, pixels, NULL);
  if (data_ptr == NULL || pixels_ptr == NULL) {
    flags_ptr[0] = VP8_STATUS_INVALID_PARAM;
    goto exit;
  }

  // Set the mode depending on machine endianness. Java expects ARGB with A in the most significant
  // byte.
  config.output.colorspace = bigendian ? MODE_ARGB : MODE_BGRA;

  config.output.is_external_memory = 1;
  config.output.u.RGBA.rgba = pixels_ptr;
  config.output.u.RGBA.stride = config.output.width * sizeof(jint);
  config.output.u.RGBA.size = config.output.width * config.output.height * sizeof(jint);

  // Decode
  status = WebPDecode(data_ptr, data_size, &config);

  flags_ptr[0] = status;
  flags_ptr[1] = config.output.width;
  flags_ptr[2] = config.output.height;
  flags_ptr[3] = config.input.has_alpha;

exit:
  if (data_ptr != NULL) {
    (*env)->ReleasePrimitiveArrayCritical(env, data, data_ptr, JNI_ABORT);
  }

  if (pixels_ptr != NULL) {
    (*env)->ReleasePrimitiveArrayCritical(env, pixels, pixels_ptr, 0);
  }

  if (flags_ptr != NULL) {
    (*env)->ReleaseIntArrayElements(env, outFlags, flags_ptr, 0);
  }

  return pixels;
}

#define PROPERTY(cls, jtype, get, set, property, type, field) \
JNIEXPORT jtype JNICALL Java_com_luciad_imageio_webp_##cls##_##get##property \
  (JNIEnv* UNUSED(env), jclass UNUSED(cls_), jlong optionsPtr) { \
  type* options = (type*) (intptr_t) optionsPtr; \
  return options->field; \
} \
\
JNIEXPORT void JNICALL Java_com_luciad_imageio_webp_##cls##_##set##property \
  (JNIEnv* UNUSED(env), jclass UNUSED(cls_), jlong optionsPtr, jtype value) { \
  type* options = (type*) (intptr_t) optionsPtr; \
  options->field = value; \
}

JNIEXPORT jlong JNICALL Java_com_luciad_imageio_webp_WebPDecoderOptions_createDecoderOptions
  (JNIEnv* UNUSED(env), jclass UNUSED(cls)) {
  WebPDecoderOptions* options = calloc(1, sizeof(WebPDecoderOptions));
  return (jlong)(intptr_t)options;
}

JNIEXPORT void JNICALL Java_com_luciad_imageio_webp_WebPDecoderOptions_deleteDecoderOptions
  (JNIEnv* UNUSED(env), jclass UNUSED(cls_), jlong optionsPtr) {
  WebPDecoderOptions* options = (WebPDecoderOptions*) (intptr_t) optionsPtr;
  free(options);
}

PROPERTY(WebPDecoderOptions, jint, get, set, CropHeight, WebPDecoderOptions, crop_height)
PROPERTY(WebPDecoderOptions, jint, get, set, CropLeft, WebPDecoderOptions, crop_left)
PROPERTY(WebPDecoderOptions, jint, get, set, CropTop, WebPDecoderOptions, crop_top)
PROPERTY(WebPDecoderOptions, jint, get, set, CropWidth, WebPDecoderOptions, crop_width)
PROPERTY(WebPDecoderOptions, jboolean, is, set, NoFancyUpsampling, WebPDecoderOptions, no_fancy_upsampling)
PROPERTY(WebPDecoderOptions, jint, get, set, ScaledWidth, WebPDecoderOptions, scaled_width)
PROPERTY(WebPDecoderOptions, jint, get, set, ScaledHeight, WebPDecoderOptions, scaled_height)
PROPERTY(WebPDecoderOptions, jboolean, is, set, UseCropping, WebPDecoderOptions, use_cropping)
PROPERTY(WebPDecoderOptions, jboolean, is, set, UseScaling, WebPDecoderOptions, use_scaling)
PROPERTY(WebPDecoderOptions, jboolean, is, set, UseThreads, WebPDecoderOptions, use_threads)
PROPERTY(WebPDecoderOptions, jboolean, is, set, BypassFiltering, WebPDecoderOptions, bypass_filtering)

typedef int (*Importer)(WebPPicture* const, const uint8_t* const, int);

static jbyteArray encode
  (JNIEnv *env, jlong configPtr, Importer import, jbyteArray data, jint width, jint height, jint stride) {
  WebPPicture pic;
  WebPConfig* config = (WebPConfig*) (intptr_t) configPtr;
  WebPMemoryWriter wrt;
  int ok;
  uint8_t* data_ptr = NULL;
  jbyteArray result = NULL;
  uint8_t* result_ptr = NULL;

  if (!WebPPictureInit(&pic)) {
    return NULL;
  }

  pic.width = width;
  pic.height = height;
  pic.writer = WebPMemoryWrite;
  pic.custom_ptr = &wrt;

  if (config->lossless) {
    pic.use_argb = 1;
  } else {
    pic.use_argb = 0;
  }

  WebPMemoryWriterInit(&wrt);

  data_ptr = (*env)->GetPrimitiveArrayCritical(env, data, NULL);
  if (data_ptr == NULL) {
    goto exit;
  }
  ok = import(&pic, data_ptr, stride);
  if (data_ptr != NULL) {
    (*env)->ReleasePrimitiveArrayCritical(env, data, data_ptr, JNI_ABORT);
  }
  if (!ok) goto exit;

  if (!WebPEncode(config, &pic)) {
    goto exit;
  }

  result = (*env)->NewByteArray(env, wrt.size);
  if (!result) {
    goto exit;
  }

  result_ptr = (*env)->GetPrimitiveArrayCritical(env, result, NULL);
  if (!result_ptr) {
    goto exit;
  }

  memmove(result_ptr, wrt.mem, wrt.size);
  (*env)->ReleasePrimitiveArrayCritical(env, result, result_ptr, 0);

exit:
  WebPPictureFree(&pic);
  if (wrt.mem) free(wrt.mem);
  return result;
}

JNIEXPORT jbyteArray JNICALL Java_com_luciad_imageio_webp_WebP_encodeRGBA
  (JNIEnv *env, jclass UNUSED(cls_), jlong configPtr, jbyteArray data, jint width, jint height, jint stride) {
 return encode(env, configPtr, WebPPictureImportRGBA, data, width, height, stride);
}

JNIEXPORT jbyteArray JNICALL Java_com_luciad_imageio_webp_WebP_encodeRGB
  (JNIEnv *env, jclass UNUSED(cls_), jlong configPtr, jbyteArray data, jint width, jint height, jint stride) {
  return encode(env, configPtr, WebPPictureImportRGB, data, width, height, stride);
}

JNIEXPORT jlong JNICALL Java_com_luciad_imageio_webp_WebPEncoderOptions_createConfig
  (JNIEnv* UNUSED(env), jclass UNUSED(cls_)) {
  WebPConfig* config = calloc(1, sizeof(WebPConfig));
  if (config) {
    WebPConfigInit(config);
  }
  return (jlong)(intptr_t)config;
}

JNIEXPORT void JNICALL Java_com_luciad_imageio_webp_WebPEncoderOptions_deleteConfig
  (JNIEnv* UNUSED(env), jclass UNUSED(cls_), jlong configPtr) {
  WebPConfig* config = (WebPConfig*) (intptr_t) configPtr;
  free(config);
}

PROPERTY(WebPEncoderOptions, jfloat, get, set, Quality, WebPConfig, quality)
PROPERTY(WebPEncoderOptions, jint, get, set, TargetSize, WebPConfig, target_size)
PROPERTY(WebPEncoderOptions, jfloat, get, set, TargetPSNR, WebPConfig, target_PSNR)
PROPERTY(WebPEncoderOptions, jint, get, set, Method, WebPConfig, method)
PROPERTY(WebPEncoderOptions, jint, get, set, Segments, WebPConfig, segments)
PROPERTY(WebPEncoderOptions, jint, get, set, SnsStrength, WebPConfig, sns_strength)
PROPERTY(WebPEncoderOptions, jint, get, set, FilterStrength, WebPConfig, filter_strength)
PROPERTY(WebPEncoderOptions, jint, get, set, FilterSharpness, WebPConfig, filter_sharpness)
PROPERTY(WebPEncoderOptions, jint, get, set, FilterType, WebPConfig, filter_type)
PROPERTY(WebPEncoderOptions, jint, get, set, Autofilter, WebPConfig, autofilter)
PROPERTY(WebPEncoderOptions, jint, get, set, Pass, WebPConfig, pass)
PROPERTY(WebPEncoderOptions, jint, get, set, ShowCompressed, WebPConfig, show_compressed)
PROPERTY(WebPEncoderOptions, jint, get, set, Preprocessing, WebPConfig, preprocessing)
PROPERTY(WebPEncoderOptions, jint, get, set, Partitions, WebPConfig, partitions)
PROPERTY(WebPEncoderOptions, jint, get, set, PartitionLimit, WebPConfig, partition_limit)
PROPERTY(WebPEncoderOptions, jint, get, set, AlphaCompression, WebPConfig, alpha_compression)
PROPERTY(WebPEncoderOptions, jint, get, set, AlphaFiltering, WebPConfig, alpha_filtering)
PROPERTY(WebPEncoderOptions, jint, get, set, AlphaQuality, WebPConfig, alpha_quality)
PROPERTY(WebPEncoderOptions, jint, get, set, Lossless, WebPConfig, lossless)
PROPERTY(WebPEncoderOptions, jint, get, set, EmulateJpegSize, WebPConfig, emulate_jpeg_size)
PROPERTY(WebPEncoderOptions, jint, get, set, ThreadLevel, WebPConfig, thread_level)
PROPERTY(WebPEncoderOptions, jint, get, set, LowMemory, WebPConfig, low_memory)
