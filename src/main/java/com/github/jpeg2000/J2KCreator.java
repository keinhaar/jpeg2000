// $Id: JPXInputStreamBFO.java 28760 2018-06-28 16:23:39Z mike $

package com.github.jpeg2000;

import java.io.*;
import java.awt.*;
import java.awt.color.*;
import java.awt.image.*;
import java.awt.Point;

import jj2000.j2k.codestream.writer.*;
import jj2000.j2k.entropy.encoder.*;
import jj2000.j2k.image.*;
import jj2000.j2k.image.forwcomptransf.*;
import jj2000.j2k.quantization.quantizer.*;
import jj2000.j2k.roi.encoder.*;
import jj2000.j2k.util.*;
import jj2000.j2k.wavelet.analysis.*;
import jj2000.j2k.*;
import jj2000.j2k.io.*;

public class J2KCreator implements MsgLogger {

    private ColorSpecificationBox colr;
    private PaletteBox pclr;
    private J2KWriteParam param;
    private boolean lossless;
    private BlkImgDataSrc src;

    public J2KCreator() {
    }

    public void flush() {
    }

    public void printmsg(int sev, String msg) {
    }

    public void println(String str, int flind, int ind) {
    }

    public void setParams(J2KWriteParam param) {
        this.param = param;
    }

    public void setLossless(boolean lossless) {
        this.lossless = lossless;
    }

    public void setColorSpace(ColorSpace space) {
        colr = new ColorSpecificationBox(space);
        pclr = null;
    }

    public void setColorSpace(ColorSpace space, int numc, int size, byte[] palette) {
        colr = new ColorSpecificationBox(space);
        pclr = new PaletteBox(size, numc, palette);
    }

    public void setSource(BlkImgDataSrc src) {
        this.src = src;
    }

    public void setSource(BufferedImage img, int tilesize) {
        setSource(AbstractDataSource.newInstance(img, tilesize));
        ColorModel cm = img.getColorModel();
        if (cm instanceof IndexColorModel) {
        } else {
            setColorSpace(cm.getColorSpace());
        }
    }

    private J2KFile doCreate(OutputStream out) throws IOException {
        if (src == null) {
            throw new IllegalStateException("No source");
        }
        if (param == null) {
            param = new SimpleJ2KWriteParam(src.getNumComps(), src.getNumTiles(), lossless);
//            param.setProgressionName("res");
        }
        if (param.getNumComponents() != src.getNumComps() || param.getNumTiles() != src.getNumTiles()) {
            throw new IllegalStateException("Param and source do not match");
        }

        Thread registerThread = Thread.currentThread();

        J2KFile file = new J2KFile();
        HeaderBox jp2h = new HeaderBox();
        jp2h.add(new ImageHeaderBox(src.getImgWidth(), src.getImgHeight(), src.getNumComps(), 8, false, false));
        if (colr != null) {
            jp2h.add(colr);
        }
        if (pclr != null) {
            jp2h.add(pclr);
        }
        file.add(new FileTypeBox());
        file.add(jp2h);

        OutputStream bout;
        if (out == null) {
            bout = new ByteArrayOutputStream();
        } else {
            file.add(new CodeStreamBox());
            file.write(out);
            bout = out;
        }

        try {
            FacilityManager.registerMsgLogger(registerThread, this);
            ForwCompTransf fctransf = new ForwCompTransf(src, param);
            ImgDataConverter converter = new ImgDataConverter(fctransf);
            ForwardWT dwt = ForwardWT.createInstance(converter, param);
            Quantizer quant = Quantizer.createInstance(dwt, param);
            ROIScaler rois = ROIScaler.createInstance(quant, param);
            EntropyCoder ecoder = EntropyCoder.createInstance(rois, param, param.getCodeBlockSize(), param.getPrecinctPartition(), param.getBypass(), param.getResetMQ(), param.getTerminateOnByte(), param.getCausalCXInfo(), param.getCodeSegSymbol(), param.getMethodForMQLengthCalc(), param.getMethodForMQTermination());

            FileCodestreamWriter bwriter = new FileCodestreamWriter(bout, Integer.MAX_VALUE);
            float rate = Float.POSITIVE_INFINITY;
            PostCompRateAllocator ralloc = PostCompRateAllocator.createInstance(ecoder, rate, bwriter, param);
            HeaderEncoder headenc = new HeaderEncoder(src, new boolean[src.getNumComps()], dwt, src, param, rois, ralloc);
            ralloc.setHeaderEncoder(headenc);
            headenc.encodeMainHeader();
            ralloc.initialize();
            headenc.reset();
            headenc.encodeMainHeader();
            bwriter.commitBitstreamHeader(headenc);
            ralloc.runAndWrite();
            bwriter.close();

            if (out == null) {
                file.add(new CodeStreamBox(((ByteArrayOutputStream)bout).toByteArray()));
            }
            return file;
        } finally {
            FacilityManager.unregisterMsgLogger(registerThread);
        }
    }

    public J2KFile create() throws IOException {
        return doCreate(null);
    }

    public void write(OutputStream out) throws IOException {
        doCreate(out);
    }
}
