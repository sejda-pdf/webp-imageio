package com.luciad.imageio.webp;

import org.junit.Test;

import javax.imageio.*;
import javax.imageio.stream.MemoryCacheImageInputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Random;

import static org.junit.Assert.*;

public class WebPTest {
    @Test
    public void testFindReaderByMimeType() {
        assertNotNull(
                "Image reader was not registered",
                findReader(ImageIO.getImageReadersByMIMEType("image/webp"))
        );
    }

    @Test
    public void testFindReaderByFormatName() {
        assertNotNull(
                "Image reader was not registered",
                findReader(ImageIO.getImageReadersByFormatName("webp"))
        );
    }

    @Test
    public void testFindReaderBySuffix() {
        assertNotNull(
                "Image reader was not registered",
                findReader(ImageIO.getImageReadersBySuffix("webp"))
        );
    }

    @Test
    public void testFindWriterByMimeType() {
        assertNotNull(
                "Image writer was not registered",
                findWriter(ImageIO.getImageWritersByMIMEType("image/webp"))
        );
    }

    @Test
    public void testFindWriterByFormatName() {
        assertNotNull(
                "Image writer was not registered",
                findWriter(ImageIO.getImageWritersByFormatName("webp"))
        );
    }

    @Test
    public void testFindWriterBySuffix() {
        assertNotNull(
                "Image writer was not registered",
                findWriter(ImageIO.getImageWritersBySuffix("webp"))
        );
    }

    @Test
    public void testCompress() throws IOException {
        BufferedImage image = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        MemoryCacheImageOutputStream imageOut = new MemoryCacheImageOutputStream(out);

        ImageWriter writer = getImageWriter();
        writer.setOutput(imageOut);
        writer.write(image);

        imageOut.close();
        out.close();

        byte[] data = out.toByteArray();
        assertNotEquals(0, data.length);
        assertEquals('R', data[0] & 0xFF);
        assertEquals('I', data[1] & 0xFF);
        assertEquals('F', data[2] & 0xFF);
        assertEquals('F', data[3] & 0xFF);
    }

    @Test
    public void testRoundtrip() throws IOException {
        BufferedImage image = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
        Random rng = new Random(42);
        int[] buffer = ((DataBufferInt) image.getData().getDataBuffer()).getData();
        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = rng.nextInt();
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        MemoryCacheImageOutputStream imageOut = new MemoryCacheImageOutputStream(out);

        ImageWriter writer = getImageWriter();
        writer.setOutput(imageOut);
        WebPWriteParam writeParam = (WebPWriteParam) writer.getDefaultWriteParam();
        writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        writeParam.setCompressionType("Lossless");
        writer.write(null, new IIOImage(image, null, null), writeParam);

        imageOut.close();
        out.close();

        byte[] webpData = out.toByteArray();

        ImageReader reader = getImageReader();
        reader.setInput(new MemoryCacheImageInputStream(new ByteArrayInputStream(webpData)));
        BufferedImage decodedImage = reader.read(0);

        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                assertEquals(image.getRGB(x, y), decodedImage.getRGB(x, y));
            }
        }
    }

    private ImageWriter getImageWriter() {
        return findWriter(ImageIO.getImageWritersByMIMEType("image/webp"));
    }

    private ImageWriter findWriter(Iterator<ImageWriter> writers) {
        ImageWriter writer = null;
        while (writers.hasNext()) {
            ImageWriter writerCandidate = writers.next();
            if (writerCandidate.getOriginatingProvider() instanceof WebPImageWriterSpi) {
                writer = writerCandidate;
                break;
            }
        }
        return writer;
    }

    private ImageReader getImageReader() {
        return findReader(ImageIO.getImageReadersByMIMEType("image/webp"));
    }

    private ImageReader findReader(Iterator<ImageReader> readers) {
        ImageReader reader = null;
        while (readers.hasNext()) {
            ImageReader readerCandidate = readers.next();
            if (readerCandidate.getOriginatingProvider() instanceof WebPImageReaderSpi) {
                reader = readerCandidate;
                break;
            }
        }
        return reader;
    }
}
