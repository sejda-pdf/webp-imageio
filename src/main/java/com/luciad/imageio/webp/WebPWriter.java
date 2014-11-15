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

import javax.imageio.IIOImage;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.spi.ImageWriterSpi;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.RenderedImage;
import java.io.IOException;

class WebPWriter extends ImageWriter {
  WebPWriter( ImageWriterSpi originatingProvider ) {
    super( originatingProvider );
  }

  @Override
  public ImageWriteParam getDefaultWriteParam() {
    return new WebPWriteParam( getLocale() );
  }

  @Override
  public IIOMetadata convertImageMetadata( IIOMetadata inData, ImageTypeSpecifier imageType, ImageWriteParam param ) {
    return null;
  }

  @Override
  public IIOMetadata convertStreamMetadata( IIOMetadata inData, ImageWriteParam param ) {
    return null;
  }

  @Override
  public IIOMetadata getDefaultImageMetadata( ImageTypeSpecifier imageType, ImageWriteParam param ) {
    return null;
  }

  @Override
  public IIOMetadata getDefaultStreamMetadata( ImageWriteParam param ) {
    return null;
  }

  @Override
  public void write( IIOMetadata streamMetadata, IIOImage image, ImageWriteParam param ) throws IOException {
    if ( param == null ) {
      param = getDefaultWriteParam();
    }

    WebPWriteParam writeParam = (WebPWriteParam) param;

    ImageOutputStream output = ( ImageOutputStream ) getOutput();
    RenderedImage ri = image.getRenderedImage();

    byte[] encodedData = WebP.encode(writeParam, ri);
    output.write( encodedData );
  }
}
