package com.github.jpeg2000.imageio;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;

import com.github.jpeg2000.J2KFile;
import com.github.jpeg2000.J2KReader;

import jj2000.j2k.io.AbstractRandomAccessIO;
import jj2000.j2k.io.RandomAccessIO;

/**
 * ImageIO compatible reader for JPEG2000 images.
 */
public class JPEG2000Reader extends ImageReader
{
    
    public JPEG2000Reader(ImageReaderSpi imageReaderImpl)
    {
        super(imageReaderImpl);
    }

    @Override
    public int getNumImages(boolean allowSearch) throws IOException
    {
        return 1;
    }

    @Override
    public int getWidth(int imageIndex) throws IOException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getHeight(int imageIndex) throws IOException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterator<ImageTypeSpecifier> getImageTypes(int imageIndex) throws IOException
    {
        throw new UnsupportedOperationException();
    }

    /**
     * No Metadata supported
     */
    @Override
    public IIOMetadata getStreamMetadata() throws IOException
    {
        throw new UnsupportedOperationException();
    }

    /**
     * No Metadata supported
     */
    @Override
    public IIOMetadata getImageMetadata(int imageIndex) throws IOException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public BufferedImage read(int imageIndex, ImageReadParam param) throws IOException
    {
        ImageInputStream in = (ImageInputStream) getInput();
        J2KFile jfile = new J2KFile();
        RandomAccessIO io = new J2KRandomAccessIO(in);
        jfile.read(io);
        J2KReader reader = new J2KReader(jfile);
        BufferedImage image = reader.getBufferedImage();
        reader.close();
        return image;
    }

    /**
     * Custom RndomAccessIO class to read the raw data from ImageInputStream
     */
    class J2KRandomAccessIO extends AbstractRandomAccessIO
    {
        byte[] buffer;
        int pos;
        
        public J2KRandomAccessIO(ImageInputStream in) throws IOException
        {
            ByteArrayOutputStream bout = new ByteArrayOutputStream(10000);
            buffer = new byte[20000];
            int len = 0;
            while(len >= 0)
            {
                len = in.read(buffer);
                if(len > 0)
                {
                    bout.write(buffer, 0, len);
                }
            }
            buffer = bout.toByteArray();
            bout = null;
        }

        @Override
        public void close() throws IOException
        {
            buffer = null;
        }

        @Override
        public int getPos() throws IOException
        {
            return pos;
        }

        @Override
        public int length() throws IOException
        {
            return buffer.length;
        }

        @Override
        public void seek(int off) throws IOException
        {
            pos = off;
        }

        @Override
        public int read() throws EOFException, IOException
        {
            if(pos < buffer.length)
            {
                return (buffer[pos++] & 0xFF);
            }
            return 0;
        }

        @Override
        public void readFully(byte[] b, int off, int len) throws IOException
        {
            if(len > buffer.length - pos)
            {
                len = buffer.length - pos;
            }
            System.arraycopy(buffer, pos, b, off, len);
            pos += len;
        }

        @Override
        public void write(int b) throws IOException
        {
        }

        @Override
        public void flush() throws IOException
        {
        }
        
    }
    
    public static void main(String[] args) throws IOException
    {
        BufferedImage img = ImageIO.read(new File("/tmp/relax.jp2"));
        System.out.println(Arrays.asList(ImageIO.getReaderFormatNames()));
        System.out.println(Arrays.asList(ImageIO.getReaderFileSuffixes()));
        System.out.println(img);
    }
}
