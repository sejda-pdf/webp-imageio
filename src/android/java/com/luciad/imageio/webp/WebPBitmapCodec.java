package com.luciad.imageio.webp;

import android.graphics.Bitmap;

import java.io.IOException;

public class WebPBitmapCodec {

  public static Bitmap decodeByteArray(byte[] aData) throws IOException {
    return decodeByteArray(aData, 0, aData.length);
  }

  public static Bitmap decodeByteArray(byte[] aData, WebPDecoderOptions aOptions) throws IOException {
    return decodeByteArray(aData, 0, aData.length, aOptions);
  }

  public static Bitmap decodeByteArray(byte[] aData, int aOffset, int aLength) throws IOException {
    return decodeByteArray(aData, aOffset, aLength, new WebPDecoderOptions());
  }

  public static Bitmap decodeByteArray(byte[] aData, int aOffset, int aLength, WebPDecoderOptions aOptions) throws IOException {
    int[] outParams = new int[4];
    int[] pixels = WebP.decode(aOptions, aData, aOffset, aLength, outParams);

    int width = outParams[1];
    int height = outParams[2];

    return Bitmap.createBitmap(
            pixels, width, height, Bitmap.Config.ARGB_8888
    );
  }

  public static byte[] compress(Bitmap aBitmap) throws IOException {
    return compress(aBitmap, new WebPEncoderOptions());
  }

  public static byte[] compress(Bitmap aBitmap, WebPEncoderOptions aOptions) throws IOException {
    int width = aBitmap.getWidth();
    int height = aBitmap.getHeight();

    if (aBitmap.hasAlpha()) {
      byte[] rgba = extractRGBA(aBitmap);
      return WebP.encodeRGBA(aOptions, rgba, width, height, width * 4);
    } else {
      byte[] rgb = extractRGB(aBitmap);
      return WebP.encodeRGB(aOptions, rgb, width, height, width * 3);
    }
  }

  private static byte[] extractRGBA(Bitmap aBitmap) {
    int width = aBitmap.getWidth();
    int height = aBitmap.getHeight();

    int[] argb = new int[width * height];
    aBitmap.getPixels(argb, 0, width, 0, 0, width, height);

    byte[] rgba = new byte[width * height * 4];

    for (int in = 0, out = 0; in < rgba.length; in++, out += 4) {
      rgba[out] = (byte)(argb[in] >> 16);
      rgba[out + 1] = (byte)(argb[in] >> 8);
      rgba[out + 2] = (byte)(argb[in]);
      rgba[out + 3] = (byte)(argb[in] >> 24);
    }

    return rgba;
  }

  private static byte[] extractRGB(Bitmap aBitmap) {
    int width = aBitmap.getWidth();
    int height = aBitmap.getHeight();

    int[] argb = new int[width * height];
    aBitmap.getPixels(argb, 0, width, 0, 0, width, height);

    byte[] rgb = new byte[width * height * 3];

    for (int in = 0, out = 0; in < argb.length; in++, out += 3) {
      rgb[out] = (byte)(argb[in] >> 16);
      rgb[out + 1] = (byte)(argb[in] >> 8);
      rgb[out + 2] = (byte)(argb[in]);
    }

    return rgb;
  }
}
