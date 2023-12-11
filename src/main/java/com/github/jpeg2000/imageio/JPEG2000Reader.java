package com.github.jpeg2000.imageio;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.*;

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
public class JPEG2000Reader extends ImageReader {

    private BufferedImage image;

    public JPEG2000Reader(ImageReaderSpi imageReaderImpl) {
        super(imageReaderImpl);
    }

    @Override public int getNumImages(boolean allowSearch) throws IOException {
        return 1;
    }

    @Override public int getWidth(int imageIndex) throws IOException {
        return getImage().getWidth();
    }

    @Override public int getHeight(int imageIndex) throws IOException {
        return getImage().getHeight();
    }

    @Override public Iterator<ImageTypeSpecifier> getImageTypes(int imageIndex) throws IOException {
        BufferedImage image = getImage();
        return Collections.<ImageTypeSpecifier>singleton(new ImageTypeSpecifier(image.getColorModel(), image.getSampleModel())).iterator();
    }

    @Override public IIOMetadata getStreamMetadata() throws IOException {
        throw new UnsupportedOperationException("Metadata not supported");
    }

    @Override
    public IIOMetadata getImageMetadata(int imageIndex) throws IOException {
        throw new UnsupportedOperationException("Metadata not supported");
    }

    @Override public BufferedImage read(int imageIndex, ImageReadParam param) throws IOException {
        return getImage();
    }

    @Override public void setInput(Object input, boolean seekForwardOnly, boolean ignoreMetadata) {
        super.setInput(input, seekForwardOnly, ignoreMetadata);
        image = null;
    }

    private BufferedImage getImage() throws IOException {
        if (image == null) {
            ImageInputStream in = (ImageInputStream) getInput();
            J2KFile jfile = new J2KFile();
            RandomAccessIO io;
            if (in.length() > 0) {
                io = new FixedJ2KRandomAccessIO(in);
            } else {
                io = new VariableJ2KRandomAccessIO(in);
            }
            jfile.read(io);
            J2KReader reader = new J2KReader(jfile);
            image = reader.getBufferedImage();
            reader.close();
        }
        return image;
    }

    /**
     * AbstractRandomAccessIO that proxies to an unknown-length ImageInputStream,
     * which reads the source stream into memory
     */
    private static class VariableJ2KRandomAccessIO extends AbstractRandomAccessIO {
        private byte[] buffer;
        private int pos;
        
        VariableJ2KRandomAccessIO(ImageInputStream in) throws IOException {
            ByteArrayOutputStream bout = new ByteArrayOutputStream(32768);
            byte[] buffer = new byte[32768];
            int len = 0;
            while (len >= 0) {
                len = in.read(buffer);
                if (len > 0) {
                    bout.write(buffer, 0, len);
                }
            }
            this.buffer = bout.toByteArray();
        }

        @Override public void close() throws IOException {
            buffer = null;
        }

        @Override public int getPos() throws IOException {
            if (buffer == null) {
                throw new IOException("Closed");
            }
            return pos;
        }

        @Override public int length() throws IOException {
            if (buffer == null) {
                throw new IOException("Closed");
            }
            return buffer.length;
        }

        @Override public void seek(int off) throws IOException {
            // RandomAccessIO requires EOF if we seek beyond stream bounds; ImageInputStream does not
            if (buffer == null) {
                throw new IOException("Closed");
            }
            if (pos > buffer.length || pos < 0) {
                throw new EOFException();
            }
            pos = off;
        }

        @Override public int read() throws IOException {
            // RandomAccessIO requires an EOFException rather than a -1 return value
            if (buffer == null) {
                throw new IOException("Closed");
            }
            if (pos >= buffer.length || pos < 0) {
                throw new EOFException();
            }
            return buffer[pos++] & 0xFF;
        }

        @Override public void readFully(byte[] b, int off, int len) throws IOException {
            if (buffer == null) {
                throw new IOException("Closed");
            } else if (len > buffer.length - pos) {
                throw new EOFException("Requested " + len + " but " + (buffer.length - pos) + " remaining");
            }
            System.arraycopy(buffer, pos, b, off, len);
            pos += len;
        }

        @Override public void write(int b) throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override public void flush() throws IOException {
        }
        
    }

    /**
     * AbstractRandomAccessIO that proxies to a fixed-length ImageInputStream,
     * so doesn't have to read anything into memory
     */
    private static class FixedJ2KRandomAccessIO extends AbstractRandomAccessIO {
        private final ImageInputStream in;
        private final long start;
        private boolean closed;
        
        FixedJ2KRandomAccessIO(ImageInputStream in) throws IOException {
            this.in = in;
            this.start = in.getStreamPosition();
            if (in.length() - this.start > Integer.MAX_VALUE) {
                 throw new IllegalArgumentException("Can't support stream of " + in.length() + " bytes");
            }
        }

        @Override public void close() throws IOException {
            closed = true;
            // in.close();      No, don't proxy this.
        }

        @Override public int getPos() throws IOException {
            if (closed) {
                throw new IOException("Closed");
            }
            return (int)(in.getStreamPosition() - start);
        }

        @Override public int length() throws IOException {
            if (closed) {
                throw new IOException("Closed");
            }
            return (int)(in.length() - start);
        }

        @Override public void seek(int off) throws IOException {
            // RandomAccessIO requires EOF if we seek beyond stream bounds; ImageInputStream does not
            if (closed) {
                throw new IOException("Closed");
            }
            if (off < 0 || off > length()) {
                throw new EOFException("off="+off+" len="+length());
            }
            in.seek(start + off);
        }

        @Override public int read() throws IOException {
            if (closed) {
                throw new IOException("Closed");
            }
            int v = in.read();
            if (v < 0) {
                throw new EOFException();
            }
            return v;
        }

        @Override public void readFully(byte[] b, int off, int len) throws IOException {
            if (closed) {
                throw new IOException("Closed");
            }
            in.readFully(b, off, len);
        }

        @Override public void write(int b) throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override public void flush() throws IOException {
        }
    }
    
    /*
    public static void main(String[] args) throws IOException {
        BufferedImage img = javax.imageio.ImageIO.read(new java.io.File(args[0]));
        System.out.println(Arrays.asList(javax.imageio.ImageIO.getReaderFormatNames()));
        System.out.println(Arrays.asList(javax.imageio.ImageIO.getReaderFileSuffixes()));
        System.out.println(img);
    }
    */
}
