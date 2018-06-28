/*
 * $RCSfile: ImageUtil.java,v $
 *
 * 
 * Copyright (c) 2005 Sun Microsystems, Inc. All  Rights Reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met: 
 * 
 * - Redistribution of source code must retain the above copyright 
 *   notice, this  list of conditions and the following disclaimer.
 * 
 * - Redistribution in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in 
 *   the documentation and/or other materials provided with the
 *   distribution.
 * 
 * Neither the name of Sun Microsystems, Inc. or the names of 
 * contributors may be used to endorse or promote products derived 
 * from this software without specific prior written permission.
 * 
 * This software is provided "AS IS," without a warranty of any 
 * kind. ALL EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND 
 * WARRANTIES, INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY, 
 * FITNESS FOR A PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY
 * EXCLUDED. SUN MIDROSYSTEMS, INC. ("SUN") AND ITS LICENSORS SHALL 
 * NOT BE LIABLE FOR ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF 
 * USING, MODIFYING OR DISTRIBUTING THIS SOFTWARE OR ITS
 * DERIVATIVES. IN NO EVENT WILL SUN OR ITS LICENSORS BE LIABLE FOR 
 * ANY LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT, SPECIAL,
 * CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER CAUSED AND
 * REGARDLESS OF THE THEORY OF LIABILITY, ARISING OUT OF THE USE OF OR
 * INABILITY TO USE THIS SOFTWARE, EVEN IF SUN HAS BEEN ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGES. 
 * 
 * You acknowledge that this software is not designed or intended for 
 * use in the design, construction, operation or maintenance of any 
 * nuclear facility. 
 *
 *
 * NOTE NOTE: Trimmed version of com.github.jaiimageio.impl.common,
 * with only methods used by jpeg2000 package remaining.
 */
package com.github.jpeg2000;

import java.awt.Rectangle;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.color.ICC_ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.ComponentSampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.awt.image.DataBufferShort;
import java.awt.image.DataBufferUShort;
import java.awt.image.DirectColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.MultiPixelPackedSampleModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.SinglePixelPackedSampleModel;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.ServiceLoader;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.spi.ImageReaderWriterSpi;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.spi.IIORegistry;
import javax.imageio.spi.ServiceRegistry;
import javax.imageio.ImageReadParam;
import javax.imageio.spi.ImageWriterSpi;
import javax.imageio.IIOException;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriter;


class ImageUtil {

    /**
     * Sets the supplied <code>Raster</code>'s data from an array
     * of packed binary data of the form returned by
     * <code>getPackedBinaryData()</code>.
     *
     * @throws IllegalArgumentException if <code>isBinary()</code> returns
     * <code>false</code> with the <code>SampleModel</code> of the
     * supplied <code>Raster</code> as argument.
     */
    public static void setPackedBinaryData(byte[] binaryDataArray, WritableRaster raster, Rectangle rect) {
        SampleModel sm = raster.getSampleModel();
        if(!isBinary(sm)) {
            throw new IllegalArgumentException(I18N.getString("ImageUtil0"));
        }

        int rectX = rect.x;
        int rectY = rect.y;
        int rectWidth = rect.width;
        int rectHeight = rect.height;

        DataBuffer dataBuffer = raster.getDataBuffer();

        int dx = rectX - raster.getSampleModelTranslateX();
        int dy = rectY - raster.getSampleModelTranslateY();

        MultiPixelPackedSampleModel mpp = (MultiPixelPackedSampleModel)sm;
        int lineStride = mpp.getScanlineStride();
        int eltOffset = dataBuffer.getOffset() + mpp.getOffset(dx, dy);
        int bitOffset = mpp.getBitOffset(dx);

        int b = 0;

        if(bitOffset == 0) {
            if(dataBuffer instanceof DataBufferByte) {
                byte[] data = ((DataBufferByte)dataBuffer).getData();
                if(data == binaryDataArray) {
                    // Optimal case: simply return.
                    return;
                }
                int stride = (rectWidth + 7)/8;
                int offset = 0;
                for(int y = 0; y < rectHeight; y++) {
                    System.arraycopy(binaryDataArray, offset,
                                     data, eltOffset,
                                     stride);
                    offset += stride;
                    eltOffset += lineStride;
                }
            } else if(dataBuffer instanceof DataBufferShort ||
                      dataBuffer instanceof DataBufferUShort) {
                short[] data = dataBuffer instanceof DataBufferShort ?
                    ((DataBufferShort)dataBuffer).getData() :
                    ((DataBufferUShort)dataBuffer).getData();

                for(int y = 0; y < rectHeight; y++) {
                    int xRemaining = rectWidth;
                    int i = eltOffset;
                    while(xRemaining > 8) {
                        data[i++] =
                            (short)(((binaryDataArray[b++] & 0xFF) << 8) |
                                    (binaryDataArray[b++] & 0xFF));
                        xRemaining -= 16;
                    }
                    if(xRemaining > 0) {
                        data[i++] =
                            (short)((binaryDataArray[b++] & 0xFF) << 8);
                    }
                    eltOffset += lineStride;
                }
            } else if(dataBuffer instanceof DataBufferInt) {
                int[] data = ((DataBufferInt)dataBuffer).getData();

                for(int y = 0; y < rectHeight; y++) {
                    int xRemaining = rectWidth;
                    int i = eltOffset;
                    while(xRemaining > 24) {
                        data[i++] =
                            (int)(((binaryDataArray[b++] & 0xFF) << 24) |
                                  ((binaryDataArray[b++] & 0xFF) << 16) |
                                  ((binaryDataArray[b++] & 0xFF) << 8) |
                                  (binaryDataArray[b++] & 0xFF));
                        xRemaining -= 32;
                    }
                    int shift = 24;
                    while(xRemaining > 0) {
                        data[i] |=
                            (int)((binaryDataArray[b++] & 0xFF) << shift);
                        shift -= 8;
                        xRemaining -= 8;
                    }
                    eltOffset += lineStride;
                }
            }
        } else { // bitOffset != 0
            int stride = (rectWidth + 7)/8;
            int offset = 0;
            if(dataBuffer instanceof DataBufferByte) {
                byte[] data = ((DataBufferByte)dataBuffer).getData();

                if((bitOffset & 7) == 0) {
                    for(int y = 0; y < rectHeight; y++) {
                        System.arraycopy(binaryDataArray, offset,
                                         data, eltOffset,
                                         stride);
                        offset += stride;
                        eltOffset += lineStride;
                    }
                } else { // bitOffset % 8 != 0
                    int rightShift = bitOffset & 7;
                    int leftShift = 8 - rightShift;
                    int leftShift8 = 8 + leftShift;
		    int mask = (byte)(255<<leftShift);
		    int mask1 = (byte)~mask;

                    for(int y = 0; y < rectHeight; y++) {
                        int i = eltOffset;
                        int xRemaining = rectWidth;
                        while(xRemaining > 0) {
                            byte datum = binaryDataArray[b++];

                            if (xRemaining > leftShift8) {
				// when all the bits in this BYTE will be set
				// into the data buffer.
                                data[i] = (byte)((data[i] & mask ) |
                                    ((datum&0xFF) >>> rightShift));
                                data[++i] = (byte)((datum & 0xFF) << leftShift);
                            } else if (xRemaining > leftShift) {
				// All the "leftShift" high bits will be set
				// into the data buffer.  But not all the
				// "rightShift" low bits will be set.
				data[i] = (byte)((data[i] & mask ) |
				    ((datum&0xFF) >>> rightShift));
				i++;
				data[i] =
				    (byte)((data[i] & mask1) | ((datum & 0xFF) << leftShift));
			    }
			    else {
				// Less than "leftShift" high bits will be set.
				int remainMask = (1 << leftShift - xRemaining) - 1;
                                data[i] =
                                    (byte)((data[i] & (mask | remainMask)) |
				    (datum&0xFF) >>> rightShift & ~remainMask);
                            }
                            xRemaining -= 8;
                        }
                        eltOffset += lineStride;
                    }
                }
            } else if(dataBuffer instanceof DataBufferShort ||
                      dataBuffer instanceof DataBufferUShort) {
                short[] data = dataBuffer instanceof DataBufferShort ?
                    ((DataBufferShort)dataBuffer).getData() :
                    ((DataBufferUShort)dataBuffer).getData();

		int rightShift = bitOffset & 7;
		int leftShift = 8 - rightShift;
                int leftShift16 = 16 + leftShift;
		int mask = (short)(~(255 << leftShift));
		int mask1 = (short)(65535 << leftShift);
		int mask2 = (short)~mask1;

                for(int y = 0; y < rectHeight; y++) {
                    int bOffset = bitOffset;
		    int xRemaining = rectWidth;
                    for(int x = 0; x < rectWidth;
			x += 8, bOffset += 8, xRemaining -= 8) {
                        int i = eltOffset + (bOffset >> 4);
                        int mod = bOffset & 15;
                        int datum = binaryDataArray[b++] & 0xFF;
                        if(mod <= 8) {
			    // This BYTE is set into one SHORT
			    if (xRemaining < 8) {
				// Mask the bits to be set.
				datum &= 255 << 8 - xRemaining;
			    }
                            data[i] = (short)((data[i] & mask) | (datum << leftShift));
                        } else if (xRemaining > leftShift16) {
			    // This BYTE will be set into two SHORTs
                            data[i] = (short)((data[i] & mask1) | ((datum >>> rightShift)&0xFFFF));
                            data[++i] =
                                (short)((datum << leftShift)&0xFFFF);
                        } else if (xRemaining > leftShift) {
			    // This BYTE will be set into two SHORTs;
			    // But not all the low bits will be set into SHORT
			    data[i] = (short)((data[i] & mask1) | ((datum >>> rightShift)&0xFFFF));
			    i++;
			    data[i] =
			        (short)((data[i] & mask2) | ((datum << leftShift)&0xFFFF));
			} else {
			    // Only some of the high bits will be set into
			    // SHORTs
			    int remainMask = (1 << leftShift - xRemaining) - 1;
			    data[i] = (short)((data[i] & (mask1 | remainMask)) |
				      ((datum >>> rightShift)&0xFFFF & ~remainMask));
			}
                    }
                    eltOffset += lineStride;
                }
            } else if(dataBuffer instanceof DataBufferInt) {
                int[] data = ((DataBufferInt)dataBuffer).getData();
                int rightShift = bitOffset & 7;
		int leftShift = 8 - rightShift;
		int leftShift32 = 32 + leftShift;
		int mask = 0xFFFFFFFF << leftShift;
		int mask1 = ~mask;

                for(int y = 0; y < rectHeight; y++) {
                    int bOffset = bitOffset;
		    int xRemaining = rectWidth;
                    for(int x = 0; x < rectWidth;
			x += 8, bOffset += 8, xRemaining -= 8) {
                        int i = eltOffset + (bOffset >> 5);
                        int mod = bOffset & 31;
                        int datum = binaryDataArray[b++] & 0xFF;
                        if(mod <= 24) {
			    // This BYTE is set into one INT
			    int shift = 24 - mod;
			    if (xRemaining < 8) {
				// Mask the bits to be set.
				datum &= 255 << 8 - xRemaining;
			    }
                            data[i] = (data[i] & (~(255 << shift))) | (datum << shift);
                        } else if (xRemaining > leftShift32) {
			    // All the bits of this BYTE will be set into two INTs
                            data[i] = (data[i] & mask) | (datum >>> rightShift);
                            data[++i] = datum << leftShift;
                        } else if (xRemaining > leftShift) {
			    // This BYTE will be set into two INTs;
			    // But not all the low bits will be set into INT
                            data[i] = (data[i] & mask) | (datum >>> rightShift);
			    i++;
                            data[i] = (data[i] & mask1) | (datum << leftShift);
                        } else {
			    // Only some of the high bits will be set into INT
			    int remainMask = (1 << leftShift - xRemaining) - 1;
			    data[i] = (data[i] & (mask | remainMask)) |
				      (datum >>> rightShift & ~remainMask);
			}
                    }
                    eltOffset += lineStride;
                }
            }
        }
    }

    /**
     * Copies data into the packed array of the <code>Raster</code>
     * from an array of unpacked data of the form returned by
     * <code>getUnpackedBinaryData()</code>.
     *
     * <p> If the data are binary, then the target bit will be set if
     * and only if the corresponding byte is non-zero.
     *
     * @throws IllegalArgumentException if <code>isBinary()</code> returns
     * <code>false</code> with the <code>SampleModel</code> of the
     * supplied <code>Raster</code> as argument.
     */
    public static void setUnpackedBinaryData(byte[] bdata, WritableRaster raster, Rectangle rect) {
        SampleModel sm = raster.getSampleModel();
        if(!isBinary(sm)) {
            throw new IllegalArgumentException(I18N.getString("ImageUtil0"));
        }

        int rectX = rect.x;
        int rectY = rect.y;
        int rectWidth = rect.width;
        int rectHeight = rect.height;

        DataBuffer dataBuffer = raster.getDataBuffer();

        int dx = rectX - raster.getSampleModelTranslateX();
        int dy = rectY - raster.getSampleModelTranslateY();

        MultiPixelPackedSampleModel mpp = (MultiPixelPackedSampleModel)sm;
        int lineStride = mpp.getScanlineStride();
        int eltOffset = dataBuffer.getOffset() + mpp.getOffset(dx, dy);
        int bitOffset = mpp.getBitOffset(dx);

        int k = 0;

        if(dataBuffer instanceof DataBufferByte) {
            byte[] data = ((DataBufferByte)dataBuffer).getData();
            for(int y = 0; y < rectHeight; y++) {
                int bOffset = eltOffset*8 + bitOffset;
                for(int x = 0; x < rectWidth; x++) {
                    if(bdata[k++] != (byte)0) {
                        data[bOffset/8] |=
                            (byte)(0x00000001 << (7 - bOffset & 7));
                    }
                    bOffset++;
                }
                eltOffset += lineStride;
            }
        } else if(dataBuffer instanceof DataBufferShort ||
                  dataBuffer instanceof DataBufferUShort) {
            short[] data = dataBuffer instanceof DataBufferShort ?
                ((DataBufferShort)dataBuffer).getData() :
                ((DataBufferUShort)dataBuffer).getData();
            for(int y = 0; y < rectHeight; y++) {
                int bOffset = eltOffset*16 + bitOffset;
                for(int x = 0; x < rectWidth; x++) {
                    if(bdata[k++] != (byte)0) {
                        data[bOffset/16] |=
                            (short)(0x00000001 <<
                                    (15 - bOffset % 16));
                    }
                    bOffset++;
                }
                eltOffset += lineStride;
            }
        } else if(dataBuffer instanceof DataBufferInt) {
            int[] data = ((DataBufferInt)dataBuffer).getData();
            for(int y = 0; y < rectHeight; y++) {
                int bOffset = eltOffset*32 + bitOffset;
                for(int x = 0; x < rectWidth; x++) {
                    if(bdata[k++] != (byte)0) {
                        data[bOffset/32] |=
                            (int)(0x00000001 <<
                                  (31 - bOffset % 32));
                    }
                    bOffset++;
                }
                eltOffset += lineStride;
            }
        }
    }

    public static boolean isBinary(SampleModel sm) {
        return sm instanceof MultiPixelPackedSampleModel &&
            ((MultiPixelPackedSampleModel)sm).getPixelBitStride() == 1 &&
            sm.getNumBands() == 1;
    }

    public static ColorModel createColorModel(ColorSpace colorSpace, SampleModel sampleModel) {
        ColorModel colorModel = null;

        if(sampleModel == null) {
            throw new IllegalArgumentException(I18N.getString("ImageUtil1"));
        }

        int numBands = sampleModel.getNumBands();
        if (numBands < 1 || numBands > 4) {
            return null;
        }

        int dataType = sampleModel.getDataType();
        if (sampleModel instanceof ComponentSampleModel) {
            if (dataType < DataBuffer.TYPE_BYTE ||
                //dataType == DataBuffer.TYPE_SHORT ||
                dataType > DataBuffer.TYPE_DOUBLE) {
                return null;
            }

            if (colorSpace == null)
                colorSpace =
                    numBands <= 2 ?
                    ColorSpace.getInstance(ColorSpace.CS_GRAY) :
                    ColorSpace.getInstance(ColorSpace.CS_sRGB);

            boolean useAlpha = (numBands == 2) || (numBands == 4);
            int transparency = useAlpha ?
                               Transparency.TRANSLUCENT : Transparency.OPAQUE;

            boolean premultiplied = false;

            int dataTypeSize = DataBuffer.getDataTypeSize(dataType);
            int[] bits = new int[numBands];
            for (int i = 0; i < numBands; i++) {
                bits[i] = dataTypeSize;
            }

            colorModel = new ComponentColorModel(colorSpace,
                                                 bits,
                                                 useAlpha,
                                                 premultiplied,
                                                 transparency,
                                                 dataType);
        } else if (sampleModel instanceof SinglePixelPackedSampleModel) {
            SinglePixelPackedSampleModel sppsm =
                (SinglePixelPackedSampleModel)sampleModel;

            int[] bitMasks = sppsm.getBitMasks();
            int rmask = 0;
            int gmask = 0;
            int bmask = 0;
            int amask = 0;

            numBands = bitMasks.length;
            if (numBands <= 2) {
                rmask = gmask = bmask = bitMasks[0];
                if (numBands == 2) {
                    amask = bitMasks[1];
                }
            } else {
                rmask = bitMasks[0];
                gmask = bitMasks[1];
                bmask = bitMasks[2];
                if (numBands == 4) {
                    amask = bitMasks[3];
                }
            }

            int[] sampleSize = sppsm.getSampleSize();
            int bits = 0;
            for (int i = 0; i < sampleSize.length; i++) {
                bits += sampleSize[i];
            }

            if (colorSpace == null)
                colorSpace = ColorSpace.getInstance(ColorSpace.CS_sRGB);

            colorModel =
                new DirectColorModel(colorSpace,
                                     bits, rmask, gmask, bmask, amask,
                                     false,
                                     sampleModel.getDataType());
        } else if (sampleModel instanceof MultiPixelPackedSampleModel) {
            int bits =
                ((MultiPixelPackedSampleModel)sampleModel).getPixelBitStride();
            int size = 1 << bits;
            byte[] comp = new byte[size];

            for (int i = 0; i < size; i++)
                comp[i] = (byte)(255 * i / (size - 1));

            colorModel = new IndexColorModel(bits, size, comp, comp, comp);
        }

        return colorModel;
    }

    /** Converts the provided object to <code>String</code> */
    public static String convertObjectToString(Object obj) {
        if (obj == null)
            return "";

        String s = "";
        if (obj instanceof byte[]) {
            byte[] bArray = (byte[])obj;
            for (int i = 0; i < bArray.length; i++)
                s += bArray[i] + " ";
            return s;
        }

        if (obj instanceof int[]) {
            int[] iArray = (int[])obj;
            for (int i = 0; i < iArray.length; i++)
                s += iArray[i] + " " ;
            return s;
        }

        if (obj instanceof short[]) {
            short[] sArray = (short[])obj;
            for (int i = 0; i < sArray.length; i++)
                s += sArray[i] + " " ;
            return s;
        }

        return obj.toString();

    }
}
