// $Id: JPXInputStreamBFO.java 28760 2018-06-28 16:23:39Z mike $

package com.github.jpeg2000;

import java.io.*;
import java.awt.*;
import java.awt.color.*;

import jj2000.j2k.decoder.DecoderSpecs;
import jj2000.j2k.quantization.dequantizer.Dequantizer;
import jj2000.j2k.image.invcomptransf.InvCompTransf;
import jj2000.j2k.fileformat.reader.FileFormatReader;
import jj2000.j2k.codestream.reader.BitstreamReaderAgent;
import jj2000.j2k.codestream.reader.HeaderDecoder;
import jj2000.j2k.wavelet.synthesis.InverseWT;
import jj2000.j2k.entropy.decoder.EntropyDecoder;
import jj2000.j2k.codestream.HeaderInfo;
import jj2000.j2k.image.BlkImgDataSrc;
import jj2000.j2k.image.DataBlkInt;
import jj2000.j2k.image.ImgDataConverter;
import jj2000.j2k.util.ISRandomAccessIO;
import jj2000.j2k.util.FacilityManager;
import jj2000.j2k.util.MsgLogger;
import jj2000.j2k.roi.ROIDeScaler;
import jj2000.j2k.io.RandomAccessIO;
import jj2000.j2k.io.EndianType;

public class ImageInputStream extends InputStream implements FileFormatReaderListener, MsgLogger {
    // final
    private final RandomAccessIO in;
    private Thread registerThread;
    private BlkImgDataSrc src; // image data source
    private int ntw, nth, numtx, numty, iw, ih, scanline, numc;
    private int[] channels;

    // variable
    private DataBlkInt db;
    private int pos, ty, length;
    private byte[] buf;
    private boolean baseline = true;
    private boolean seenapprox;
    private int[][] palette;
    private ColorSpace cs;

    public ImageInputStream(RandomAccessIO is) throws IOException {
        this.in = is;
        registerThread = Thread.currentThread();
        FacilityManager.registerMsgLogger(registerThread, this);
        J2KImageReadParamJava j2kreadparam = new J2KImageReadParamJava();
        j2kreadparam.setParsingEnabled(true);
        FileFormatReader ff = new FileFormatReader(in, this);
        ff.readFileFormat();
        in.seek(ff.getFirstCodeStreamPos());

        HeaderInfo hi = new HeaderInfo();
        HeaderDecoder hd = new HeaderDecoder(in, j2kreadparam, hi);
        numc = hd.getNumComps();
        int[] depth = new int[numc];
        for (int i=0;i<numc;i++) {
            depth[i] = hd.getOriginalBitDepth(i);
            // PDF/A3: "The bit-depth of the JPEG2000 data shall have a value in the
            // range 1 to 38. All colour channels in the JPEG2000 data shall have the
            // same bit-depth."
        }
        DecoderSpecs decSpec = hd.getDecoderSpecs();
        BitstreamReaderAgent breader = BitstreamReaderAgent.createInstance(in, hd, j2kreadparam, decSpec, false, hi);
        EntropyDecoder entdec = hd.createEntropyDecoder(breader, j2kreadparam);
        ROIDeScaler roids = hd.createROIDeScaler(entdec, j2kreadparam, decSpec);
        Dequantizer deq = hd.createDequantizer(roids, depth, decSpec);
        InverseWT invWT = InverseWT.createInstance(deq, decSpec);
        invWT.setImgResLevel(breader.getImgRes());
        ImgDataConverter converter = new ImgDataConverter(invWT, 0);
        InvCompTransf ictransf = new InvCompTransf(converter, decSpec, depth);
        this.src = ictransf;
        
        channels = new int[numc];
        for (int i=0;i<numc;i++) {
            channels[i] = i;
        }
        ntw = src.getNomTileWidth();
        nth = src.getNomTileHeight();
        iw = src.getImgWidth();
        ih = src.getImgHeight();
        numtx = (iw + ntw - 1) / ntw;
        numty = (ih + nth - 1) / nth;
        numc = src.getNumComps();
        scanline = iw * numc;
        buf = new byte[scanline * nth];
        pos = length = buf.length;
        db = new DataBlkInt(0, 0, ntw, nth);
    }

    public void addNode(Box box) {
        if (box instanceof PaletteBox) {
            PaletteBox b = (PaletteBox) box;
            int indexsize = b.getNumEntries();
            int numc = b.getNumComp();
            palette = new int[indexsize][numc];
            for (int i=0;i<indexsize;i++) {
                for (int c=0;c<numc;c++) {
                    palette[i][c] = b.getComponentValue(i, c);
                }
            }
        } else if (box instanceof ColorSpecificationBox) {
            ColorSpecificationBox b = (ColorSpecificationBox) box;
            switch (b.getMethod()) {
                case 1: // enumerated
                    cs = createColorSpace(b.getEnumeratedColorSpace());
                    break;
                case 2: // icc_profiled
                case 3: // restricted ICC
                    cs = new ICC_ColorSpace(b.getICCProfile());
                    break;
                default:
                    // PDF/A3: "The value of the METH entry in its 'colr' box shall be
                    // 0x01, 0x02 or 0x03."
            }
            if (b.getApproximationAccuracy() == 1) {
                // PDF/A3: "If the number of colour space specifications in the JPEG2000 data
                // is greater than 1, there shall be exactly one colour space specification
                // that has the value 0x01 in the APPROX field."
                if (seenapprox) {
                    baseline = false;
                } else {
                    seenapprox = true;
                }
            }
        } else if (box instanceof ChannelDefinitionBox) {
            ChannelDefinitionBox b = (ChannelDefinitionBox)box;
            short[] c = b.getChannel();
            short[] a = b.getAssociation();
            for (int i=0;i<c.length;i++) {
                channels[c[i]] = a[i] - 1;
            }
        }
    }

    public void flush() {
    }

    public void printmsg(int sev, String msg) {
    }

    public void println(String str, int flind, int ind) {
    }

    //--------------------------------------------------------------
    // InputStream methods

    private boolean nextRow(boolean skip) throws IOException {
//        System.out.println("IN: ty="+ty+"/"+numty+" numtx="+numtx+" numc="+numc+" skip="+skip+" pos="+pos+" length="+length);
        if (ty == numty) {
            return false;
        }
        if (!skip) {
            for (int tx=0;tx<numtx;tx++) {
                src.setTile(tx, ty);
                final int tileix = src.getTileIdx();
                length = scanline * src.getTileHeight();
                final int itx = tx * ntw;
                final int ity = 0;
                for (int iz=0;iz<numc;iz++) {
                    int riz = channels[iz];     // output channel, could differ from input channel
                    final int tw = src.getTileCompWidth(tileix, iz);
                    final int th = src.getTileCompHeight(tileix, iz);
                    db.w = tw;
                    db.h = th;
                    final int depth = src.getNomRangeBits(iz);
                    final int mid = 1 << (depth - 1);
                    final int csx = src.getCompSubsX(iz);
                    final int csy = src.getCompSubsY(iz);
                    final int fb = src.getFixedPoint(iz);
//                    System.out.println("iwh="+iw+","+ih+" txy="+tx+","+ty+" of "+numtx+","+numty+" twh="+tw+","+th+" ntwh="+src.getNomTileWidth()+","+src.getNomTileHeight()+" iz="+iz+"="+riz+" ss="+csx+","+csy+" d="+depth+" mid="+mid+" fb="+fb);
                    int[] shift = null;
                    if (depth < 8) {
                        shift = new int[1<<depth];
                        for (int i=0;i<shift.length;i++) {
                            shift[i] = (int)Math.round(i * 255f / ((1<<depth)-1));
                        }
                    }
                    do {
                        db = (DataBlkInt)src.getInternCompData(db, iz);
                    } while (db.progressive);
                    // Main loop: retrieve value, scaled to 8 bits and adjust midpoint
                    for (int iy=0;iy<th;iy++) {
                        for (int ix=0;ix<tw;ix++) {
                            int val = (db.data[db.offset + iy*tw + ix] >> fb) + mid;
                            if (depth == 8) {
                                val = Math.max(0, Math.min(255, val));
                            } else if (depth > 8) {
                                val = Math.max(0, Math.min(255, val >> (depth-8)));
                            } else {
                                val = shift[val < 0 ? 0 : val >= shift.length ? shift.length-1 : val];
                            }
                            buf[((ity + (iy * csy)) * scanline) + ((itx + (ix * csx)) * numc) + riz] = (byte)val;
                        }
                    }
                    if (csx != 1 || csy != 1) {
                        // Component is subsampled; use bilinear interpolation to fill the gaps. Quick and dirty,
                        // tested with limited test data
                        for (int iy=0;iy<th;iy++) {
                            for (int ix=0;ix<tw;ix++) {
                                // Values on each of the four corners of our space
                                int v00 = buf[((ity + (iy * csy)) * scanline) + ((itx + (ix * csx)) * numc) + riz] & 0xFF;
                                int v01 = ix + 1 == tw ? v00 : buf[((ity + (iy * csy)) * scanline) + ((itx + ((ix+1) * csx)) * numc) + riz] & 0xFF;
                                int v10 = iy + 1 == th ? v00 : buf[((ity + ((iy+1) * csy)) * scanline) + ((itx + (ix * csx)) * numc) + riz] & 0xFF;
                                int v11 = iy + 1 == th ? (ix + 1 == tw ? v00 : v10) : (ix + 1 == tw ? v10 : buf[((ity + ((iy+1) * csy)) * scanline) + ((itx + ((ix+1) * csx)) * numc) + riz] & 0xFF);
                                for (int jy=0;jy<csy;jy++) {
                                    for (int jx=0;jx<csx;jx++) {
                                        if (jx+jy != 0 && ix + jx < ntw && iy + jy < nth) {
                                            // q = interpolated(v00, v01, v10, v11)
                                            int q0 = v00 + ((v10 - v00) * jx / (csx-1));
                                            int q1 = v01 + ((v11 - v01) * jx / (csx-1));
                                            int q = q0 + ((q1-q0) * jy / (csy-1));
                                            buf[((ity + (iy * csy) + jy) * scanline) + ((itx + (ix * csx) + jx) * numc) + riz] = (byte)q;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        ty++;
        pos = 0;
        return true;
    }

    public int read() throws IOException {
        if (pos == length) {
            if (!nextRow(false)) {
                return -1;
            }
        }
        return buf[pos++] & 0xFF;
    }

    public int read(byte[] out) throws IOException {
        return read(out, 0, out.length);
    }

    public int read(byte[] out, int off, int len) throws IOException {
        if (len == 0) {
            return 0;
        }
        int origlen = len;
        while (len > 0) {
            if (pos == length) {
                if (!nextRow(false)) {
                    break;
                }
            }
            int avail = Math.min(len, length - pos);
            System.arraycopy(buf, pos, out, off, avail);
            len -= avail;
            off += avail;
            pos += avail;
        }
        return len == origlen ? -1 : origlen - len;
    }

    public long skip(long len) throws IOException {
        long origlen = len;
        while (len > 0) {
            int avail = Math.min((int)len, length - pos);
            len -= avail;
            pos += avail;
            if (pos == length) {
                if (!nextRow(len > buf.length)) {
                    break;
                }
            }
        }
        return origlen - len;
    }

    public int available() {
        return buf.length - pos;
    }

    public void close() throws IOException {
        FacilityManager.unregisterMsgLogger(registerThread);
        in.close();
    }

    //--------------------------------------------------------------
    // ImageInputStream methods

    public int getWidth() {
        return iw;
    }

    public int getHeight() {
        return ih;
    }

    public int getNumComponents() {
        return numc;
    }

    public int getRowSpan() {
        return getNumComponents() * getWidth();
    }

    public int getBitsPerComponent() {
        return 8;
    }

    public ColorSpace getColorSpace() {
        return cs;
    }

    public boolean isIndexed() {
        return palette != null;
    }

    public int getIndexSize() {
        return palette != null ? palette.length : -1;
    }

    public int getIndexComponent(int color, int component) {
        return palette[color][component];
    }

    public String toString() {
        return "{JPX: w="+getWidth()+" h="+getHeight()+" numc="+getNumComponents()+" bpc="+getBitsPerComponent()+(isIndexed()?" ix"+getIndexSize():"")+" rs="+getRowSpan()+" hash=0x"+Integer.toHexString(System.identityHashCode(this))+"}";
    }

    protected  ColorSpace createColorSpace(int e) {
        switch(e) {
            case 16: return ColorSpace.getInstance(ColorSpace.CS_sRGB);
            case 17: return ColorSpace.getInstance(ColorSpace.CS_GRAY);
        }
        return null;
    }


}
