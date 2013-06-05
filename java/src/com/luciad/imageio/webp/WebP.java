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
package com.luciad.imageio.webp;

import java.awt.image.*;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.Hashtable;

final class WebP {
  private static boolean NATIVE_LIBRARY_LOADED = false;

  static synchronized void loadNativeLibrary() {
    if ( !NATIVE_LIBRARY_LOADED ) {
      NATIVE_LIBRARY_LOADED = true;
      System.loadLibrary( "webp-imageio" );
    }
  }

  static {
    loadNativeLibrary();
  }

  private WebP() {
  }

  public static BufferedImage decode( WebPReadParam aReadParam, byte[] aData, int aOffset, int aLength ) throws IOException {
    if ( aReadParam == null ) {
      throw new NullPointerException( "Decoder options may not be null" );
    }

    if ( aData == null ) {
      throw new NullPointerException( "Input data may not be null" );
    }

    if ( aOffset + aLength > aData.length ) {
      throw new IllegalArgumentException( "Offset/length exceeds array size" );
    }

    int[] out = new int[4];
    int[] pixels = decode( aReadParam.fPointer, aData, aOffset, aLength, out, ByteOrder.nativeOrder().equals( ByteOrder.BIG_ENDIAN ) );
    VP8StatusCode status = VP8StatusCode.getStatusCode( out[0] );
    switch ( status ) {
      case VP8_STATUS_OK:
        break;
      case VP8_STATUS_OUT_OF_MEMORY:
        throw new OutOfMemoryError();
      default:
        throw new IOException( "Decode returned code " + status );
    }

    int width = out[1];
    int height = out[2];
    boolean alpha = out[3] != 0;

    ColorModel colorModel;
    if ( alpha ) {
      colorModel = new DirectColorModel( 32, 0x00ff0000, 0x0000ff00, 0x000000ff, 0xff000000 );
    } else {
      colorModel = new DirectColorModel( 24, 0x00ff0000, 0x0000ff00, 0x000000ff, 0x00000000 );
    }

    SampleModel sampleModel = colorModel.createCompatibleSampleModel( width, height );
    DataBufferInt db = new DataBufferInt( pixels, width * height );
    WritableRaster raster = WritableRaster.createWritableRaster( sampleModel, db, null );

    return new BufferedImage( colorModel, raster, false, new Hashtable<Object, Object>() );
  }

  private static native int[] decode( long aDecoderOptionsPointer, byte[] aData, int aOffset, int aLength, int[] aFlags, boolean aBigEndian );

  public static int[] getInfo( byte[] aData, int aOffset, int aLength ) throws IOException {
    int[] out = new int[2];
    int result = getInfo( aData, aOffset, aLength, out );
    if (result == 0) {
      throw new IOException( "Invalid WebP data" );
    }

    return out;
  }

  private static native int getInfo( byte[] aData, int aOffset, int aLength, int[] aOut );

  public static byte[] encode( WebPWriteParam aWriteParam, RenderedImage aImage ) throws IOException {
    if ( aWriteParam == null ) {
      throw new NullPointerException( "Encoder options may not be null" );
    }

    if ( aImage == null ) {
      throw new NullPointerException( "Image may not be null" );
    }

    boolean encodeAlpha = hasTranslucency( aImage );
    if ( encodeAlpha ) {
      byte[] rgbaData = getRGBA( aImage );
      return encodeRGBA( aWriteParam.getPointer(), rgbaData, aImage.getWidth(), aImage.getHeight(), aImage.getWidth() * 4 );
    }
    else {
      byte[] rgbData = getRGB( aImage );
      return encodeRGB( aWriteParam.getPointer(), rgbData, aImage.getWidth(), aImage.getHeight(), aImage.getWidth() * 3 );
    }
  }

  private static native byte[] encodeRGBA( long aConfig, byte[] aRgbaData, int aWidth, int aHeight, int aStride );

  private static native byte[] encodeRGB( long aConfig, byte[] aRgbaData, int aWidth, int aHeight, int aStride );

  private static boolean hasTranslucency( RenderedImage aRi ) {
    return aRi.getColorModel().hasAlpha();
  }

  private static int getShift( int aMask ) {
    int shift = 0;
    while ( ( ( aMask >> shift ) & 0x1 ) == 0 ) {
      shift++;
    }
    return shift;
  }

  private static byte[] getRGB( RenderedImage aRi ) throws IOException {
    int width = aRi.getWidth();
    int height = aRi.getHeight();

    ColorModel colorModel = aRi.getColorModel();
    if ( colorModel instanceof ComponentColorModel ) {
      ComponentSampleModel sampleModel = ( ComponentSampleModel ) aRi.getSampleModel();
      int type = sampleModel.getTransferType();
      if ( type == DataBuffer.TYPE_BYTE ) {
        return extractComponentRGBByte( width, height, sampleModel, ( ( DataBufferByte ) aRi.getData().getDataBuffer() ) );
      }
      else if ( type == DataBuffer.TYPE_INT ) {
        return extractComponentRGBInt( width, height, sampleModel, ( ( DataBufferInt ) aRi.getData().getDataBuffer() ) );
      }
      else {
        throw new IOException( "Incompatible image: " + aRi );
      }
    }
    else if ( colorModel instanceof DirectColorModel ) {
      SinglePixelPackedSampleModel sampleModel = ( SinglePixelPackedSampleModel ) aRi.getSampleModel();
      int type = sampleModel.getTransferType();
      if ( type == DataBuffer.TYPE_INT ) {
        return extractDirectRGBInt( width, height, ( DirectColorModel ) colorModel, sampleModel, ( ( DataBufferInt ) aRi.getData().getDataBuffer() ) );
      }
      else {
        throw new IOException( "Incompatible image: " + aRi );
      }
    }

    return extractGenericRGB( aRi, width, height, colorModel );
  }

  private static byte[] extractGenericRGB( RenderedImage aRi, int aWidth, int aHeight, ColorModel aColorModel ) {
    Object dataElements = null;
    byte[] rgbData = new byte[ aWidth * aHeight * 3 ];
    for ( int b = 0, y = 0; y < aHeight; y++ ) {
      for ( int x = 0; x < aWidth; x++, b += 3 ) {
        dataElements = aRi.getData().getDataElements( x, y, dataElements );
        rgbData[ b ] = ( byte ) aColorModel.getRed( dataElements );
        rgbData[ b + 1 ] = ( byte ) aColorModel.getGreen( dataElements );
        rgbData[ b + 2 ] = ( byte ) aColorModel.getBlue( dataElements );
      }
    }
    return rgbData;
  }

  private static byte[] extractDirectRGBInt( int aWidth, int aHeight, DirectColorModel aColorModel, SinglePixelPackedSampleModel aSampleModel, DataBufferInt aDataBuffer ) {
    byte[] out = new byte[ aWidth * aHeight * 3 ];

    int rMask = aColorModel.getRedMask();
    int gMask = aColorModel.getGreenMask();
    int bMask = aColorModel.getBlueMask();
    int rShift = getShift( rMask );
    int gShift = getShift( gMask );
    int bShift = getShift( bMask );
    int[] bank = aDataBuffer.getBankData()[ 0 ];
    int scanlineStride = aSampleModel.getScanlineStride();
    int scanIx = 0;
    for ( int b = 0, y = 0; y < aHeight; y++ ) {
      int pixIx = scanIx;
      for ( int x = 0; x < aWidth; x++, b += 3 ) {
        int pixel = bank[ pixIx++ ];
        out[ b ] = ( byte ) ( ( pixel & rMask ) >>> rShift );
        out[ b + 1 ] = ( byte ) ( ( pixel & gMask ) >>> gShift );
        out[ b + 2 ] = ( byte ) ( ( pixel & bMask ) >>> bShift );
      }
      scanIx += scanlineStride;
    }
    return out;
  }

  private static byte[] extractComponentRGBInt( int aWidth, int aHeight, ComponentSampleModel aSampleModel, DataBufferInt aDataBuffer ) {
    byte[] out = new byte[ aWidth * aHeight * 3 ];

    int[] bankIndices = aSampleModel.getBankIndices();
    int[] rBank = aDataBuffer.getBankData()[ bankIndices[ 0 ] ];
    int[] gBank = aDataBuffer.getBankData()[ bankIndices[ 1 ] ];
    int[] bBank = aDataBuffer.getBankData()[ bankIndices[ 2 ] ];

    int[] bankOffsets = aSampleModel.getBandOffsets();
    int rScanIx = bankOffsets[ 0 ];
    int gScanIx = bankOffsets[ 1 ];
    int bScanIx = bankOffsets[ 2 ];

    int pixelStride = aSampleModel.getPixelStride();
    int scanlineStride = aSampleModel.getScanlineStride();
    for ( int b = 0, y = 0; y < aHeight; y++ ) {
      int rPixIx = rScanIx;
      int gPixIx = gScanIx;
      int bPixIx = bScanIx;
      for ( int x = 0; x < aWidth; x++, b += 3 ) {
        out[ b ] = ( byte ) rBank[ rPixIx ];
        rPixIx += pixelStride;
        out[ b + 1 ] = ( byte ) gBank[ gPixIx ];
        gPixIx += pixelStride;
        out[ b + 2 ] = ( byte ) bBank[ bPixIx ];
        bPixIx += pixelStride;
      }
      rScanIx += scanlineStride;
      gScanIx += scanlineStride;
      bScanIx += scanlineStride;
    }
    return out;
  }

  private static byte[] extractComponentRGBByte( int aWidth, int aHeight, ComponentSampleModel aSampleModel, DataBufferByte aDataBuffer ) {
    byte[] out = new byte[ aWidth * aHeight * 3 ];

    int[] bankIndices = aSampleModel.getBankIndices();
    byte[] rBank = aDataBuffer.getBankData()[ bankIndices[ 0 ] ];
    byte[] gBank = aDataBuffer.getBankData()[ bankIndices[ 1 ] ];
    byte[] bBank = aDataBuffer.getBankData()[ bankIndices[ 2 ] ];

    int[] bankOffsets = aSampleModel.getBandOffsets();
    int rScanIx = bankOffsets[ 0 ];
    int gScanIx = bankOffsets[ 1 ];
    int bScanIx = bankOffsets[ 2 ];

    int pixelStride = aSampleModel.getPixelStride();
    int scanlineStride = aSampleModel.getScanlineStride();
    for ( int b = 0, y = 0; y < aHeight; y++ ) {
      int rPixIx = rScanIx;
      int gPixIx = gScanIx;
      int bPixIx = bScanIx;
      for ( int x = 0; x < aWidth; x++, b += 3 ) {
        out[ b ] = rBank[ rPixIx ];
        rPixIx += pixelStride;
        out[ b + 1 ] = gBank[ gPixIx ];
        gPixIx += pixelStride;
        out[ b + 2 ] = bBank[ bPixIx ];
        bPixIx += pixelStride;
      }
      rScanIx += scanlineStride;
      gScanIx += scanlineStride;
      bScanIx += scanlineStride;
    }
    return out;
  }

  private static byte[] getRGBA( RenderedImage aRi ) throws IOException {
    int width = aRi.getWidth();
    int height = aRi.getHeight();

    ColorModel colorModel = aRi.getColorModel();
    if ( colorModel instanceof ComponentColorModel ) {
      ComponentSampleModel sampleModel = ( ComponentSampleModel ) aRi.getSampleModel();
      int type = sampleModel.getTransferType();
      if ( type == DataBuffer.TYPE_BYTE ) {
        return extractComponentRGBAByte( width, height, sampleModel, ( ( DataBufferByte ) aRi.getData().getDataBuffer() ) );
      }
      else if ( type == DataBuffer.TYPE_INT ) {
        return extractComponentRGBAInt( width, height, sampleModel, ( ( DataBufferInt ) aRi.getData().getDataBuffer() ) );
      }
      else {
        throw new IOException( "Incompatible image: " + aRi );
      }
    }
    else if ( colorModel instanceof DirectColorModel ) {
      SinglePixelPackedSampleModel sampleModel = ( SinglePixelPackedSampleModel ) aRi.getSampleModel();
      int type = sampleModel.getTransferType();
      if ( type == DataBuffer.TYPE_INT ) {
        return extractDirectRGBAInt( width, height, ( DirectColorModel ) colorModel, sampleModel, ( ( DataBufferInt ) aRi.getData().getDataBuffer() ) );
      }
      else {
        throw new IOException( "Incompatible image: " + aRi );
      }
    }

    return extractGenericRGBA( aRi, width, height, colorModel );
  }

  private static byte[] extractGenericRGBA( RenderedImage aRi, int aWidth, int aHeight, ColorModel aColorModel ) {
    Object dataElements = null;
    byte[] rgbData = new byte[ aWidth * aHeight * 4 ];
    for ( int b = 0, y = 0; y < aHeight; y++ ) {
      for ( int x = 0; x < aWidth; x++, b += 4 ) {
        dataElements = aRi.getData().getDataElements( x, y, dataElements );
        rgbData[ b ] = ( byte ) aColorModel.getRed( dataElements );
        rgbData[ b + 1 ] = ( byte ) aColorModel.getGreen( dataElements );
        rgbData[ b + 2 ] = ( byte ) aColorModel.getBlue( dataElements );
        rgbData[ b + 3 ] = ( byte ) aColorModel.getAlpha( dataElements );
      }
    }
    return rgbData;
  }

  private static byte[] extractDirectRGBAInt( int aWidth, int aHeight, DirectColorModel aColorModel, SinglePixelPackedSampleModel aSampleModel, DataBufferInt aDataBuffer ) {
    byte[] out = new byte[ aWidth * aHeight * 4 ];

    int rMask = aColorModel.getRedMask();
    int gMask = aColorModel.getGreenMask();
    int bMask = aColorModel.getBlueMask();
    int aMask = aColorModel.getAlphaMask();
    int rShift = getShift( rMask );
    int gShift = getShift( gMask );
    int bShift = getShift( bMask );
    int aShift = getShift( aMask );
    int[] bank = aDataBuffer.getBankData()[ 0 ];
    int scanlineStride = aSampleModel.getScanlineStride();
    int scanIx = 0;
    for ( int b = 0, y = 0; y < aHeight; y++ ) {
      int pixIx = scanIx;
      for ( int x = 0; x < aWidth; x++, b += 4 ) {
        int pixel = bank[ pixIx++ ];
        out[ b ] = ( byte ) ( ( pixel & rMask ) >>> rShift );
        out[ b + 1 ] = ( byte ) ( ( pixel & gMask ) >>> gShift );
        out[ b + 2 ] = ( byte ) ( ( pixel & bMask ) >>> bShift );
        out[ b + 3 ] = ( byte ) ( ( pixel & aMask ) >>> aShift );
      }
      scanIx += scanlineStride;
    }
    return out;
  }

  private static byte[] extractComponentRGBAInt( int aWidth, int aHeight, ComponentSampleModel aSampleModel, DataBufferInt aDataBuffer ) {
    byte[] out = new byte[ aWidth * aHeight * 4 ];

    int[] bankIndices = aSampleModel.getBankIndices();
    int[] rBank = aDataBuffer.getBankData()[ bankIndices[ 0 ] ];
    int[] gBank = aDataBuffer.getBankData()[ bankIndices[ 1 ] ];
    int[] bBank = aDataBuffer.getBankData()[ bankIndices[ 2 ] ];
    int[] aBank = aDataBuffer.getBankData()[ bankIndices[ 3 ] ];

    int[] bankOffsets = aSampleModel.getBandOffsets();
    int rScanIx = bankOffsets[ 0 ];
    int gScanIx = bankOffsets[ 1 ];
    int bScanIx = bankOffsets[ 2 ];
    int aScanIx = bankOffsets[ 3 ];

    int pixelStride = aSampleModel.getPixelStride();
    int scanlineStride = aSampleModel.getScanlineStride();
    for ( int b = 0, y = 0; y < aHeight; y++ ) {
      int rPixIx = rScanIx;
      int gPixIx = gScanIx;
      int bPixIx = bScanIx;
      int aPixIx = aScanIx;
      for ( int x = 0; x < aWidth; x++, b += 4 ) {
        out[ b ] = ( byte ) rBank[ rPixIx ];
        rPixIx += pixelStride;
        out[ b + 1 ] = ( byte ) gBank[ gPixIx ];
        gPixIx += pixelStride;
        out[ b + 2 ] = ( byte ) bBank[ bPixIx ];
        bPixIx += pixelStride;
        out[ b + 3 ] = ( byte ) aBank[ aPixIx ];
        aPixIx += pixelStride;
      }
      rScanIx += scanlineStride;
      gScanIx += scanlineStride;
      bScanIx += scanlineStride;
      aScanIx += scanlineStride;
    }
    return out;
  }

  private static byte[] extractComponentRGBAByte( int aWidth, int aHeight, ComponentSampleModel aSampleModel, DataBufferByte aDataBuffer ) {
    byte[] out = new byte[ aWidth * aHeight * 4 ];

    int[] bankIndices = aSampleModel.getBankIndices();
    byte[] rBank = aDataBuffer.getBankData()[ bankIndices[ 0 ] ];
    byte[] gBank = aDataBuffer.getBankData()[ bankIndices[ 1 ] ];
    byte[] bBank = aDataBuffer.getBankData()[ bankIndices[ 2 ] ];
    byte[] aBank = aDataBuffer.getBankData()[ bankIndices[ 3 ] ];

    int[] bankOffsets = aSampleModel.getBandOffsets();
    int rScanIx = bankOffsets[ 0 ];
    int gScanIx = bankOffsets[ 1 ];
    int bScanIx = bankOffsets[ 2 ];
    int aScanIx = bankOffsets[ 3 ];

    int pixelStride = aSampleModel.getPixelStride();
    int scanlineStride = aSampleModel.getScanlineStride();
    for ( int b = 0, y = 0; y < aHeight; y++ ) {
      int rPixIx = rScanIx;
      int gPixIx = gScanIx;
      int bPixIx = bScanIx;
      int aPixIx = aScanIx;
      for ( int x = 0; x < aWidth; x++, b += 4 ) {
        out[ b ] = rBank[ rPixIx ];
        rPixIx += pixelStride;
        out[ b + 1 ] = gBank[ gPixIx ];
        gPixIx += pixelStride;
        out[ b + 2 ] = bBank[ bPixIx ];
        bPixIx += pixelStride;
        out[ b + 3 ] = aBank[ aPixIx ];
        aPixIx += pixelStride;
      }
      rScanIx += scanlineStride;
      gScanIx += scanlineStride;
      bScanIx += scanlineStride;
      aScanIx += scanlineStride;
    }
    return out;
  }
}
