import jj2000.j2k.codestream.writer.*;
import jj2000.j2k.entropy.encoder.*;
import jj2000.j2k.image.*;
import jj2000.j2k.image.forwcomptransf.*;
import jj2000.j2k.quantization.quantizer.*;
import jj2000.j2k.roi.encoder.*;
import jj2000.j2k.util.*;
import jj2000.j2k.wavelet.analysis.*;
import jj2000.j2k.*;
import com.github.jpeg2000.*;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import javax.imageio.*;
import java.awt.Point;
import java.awt.Rectangle;
import java.io.*;
import javax.xml.stream.*;

class TestWriter {
    public static void main(String[] args) throws Exception {
        final boolean lossless = true;

        OutputStream out = new BufferedOutputStream(new FileOutputStream("out.jp2"));

//        BlkImgDataSrc src = new MySrc(482, 680, 3, 8, 256, System.in);
        BlkImgDataSrc src = new MySrc(ImageIO.read(new File(args[0])), 256);
        SimpleJ2KWriteParam param = new SimpleJ2KWriteParam(src.getNumComps(), src.getNumTiles(), lossless);
        System.out.println(param);
        param.setProgressionName("pos-comp");
//        ("res".equals(name) || "layer".equals(name) || "res-pos".equals(name) || "pos-comp".equals(name) || "comp-pos".equals(name)) 


        Tiler imgtiler = new Tiler(src, 0, 0, 0, 0, src.getImgWidth(), src.getImgHeight());
        System.out.println(imgtiler.getNumTiles()+" "+param.getNumTiles());

        ForwCompTransf fctransf = new ForwCompTransf(src, param);
        ImgDataConverter converter = new ImgDataConverter(fctransf);
        ForwardWT dwt = ForwardWT.createInstance(converter, param);
        Quantizer quant = Quantizer.createInstance(dwt, param);
        ROIScaler rois = ROIScaler.createInstance(quant, param);
        EntropyCoder ecoder = EntropyCoder.createInstance(rois, param, param.getCodeBlockSize(), param.getPrecinctPartition(), param.getBypass(), param.getResetMQ(), param.getTerminateOnByte(), param.getCausalCXInfo(), param.getCodeSegSymbol(), param.getMethodForMQLengthCalc(), param.getMethodForMQTermination());

        J2KFile file = new J2KFile();
        file.add(new FileTypeBox())
            .add(new HeaderBox()
                .add(new ImageHeaderBox(src.getImgWidth(), src.getImgHeight(), src.getNumComps(), 8, false, false))
                .add(new ColorSpecificationBox(16)))
            .add(new CodeStreamBox());
        file.write(out);

//        ByteArrayOutputStream tempout = new ByteArrayOutputStream();
        FileCodestreamWriter bwriter = new FileCodestreamWriter(out, Integer.MAX_VALUE);
        float rate = Float.POSITIVE_INFINITY;
        PostCompRateAllocator ralloc = PostCompRateAllocator.createInstance(ecoder, rate, bwriter, param);
        HeaderEncoder headenc = new HeaderEncoder(src, new boolean[src.getNumComps()], dwt, src, param, rois,ralloc);
        ralloc.setHeaderEncoder(headenc);
        headenc.encodeMainHeader();
        ralloc.initialize();
        headenc.reset();
        headenc.encodeMainHeader();
        bwriter.commitBitstreamHeader(headenc);
        ralloc.runAndWrite();
        bwriter.close();

        
        file.write(XMLOutputFactory.newFactory().createXMLStreamWriter(System.out)).close();
//        file.add(new CodeStreamBox(tempout.toByteArray()));
//        file.write(out);
    }

    public static class MySrc implements BlkImgDataSrc {

        private final int w, h, numc, bpc, nomtw, nomth, scanline, numx, numy;
        private InputStream in;
        private int tx, ty, tw, th;
        private final byte[] buf;

        MySrc(BufferedImage img) {
            this(img, Integer.MAX_VALUE);
        }

        MySrc(BufferedImage img, int tilesize) {
            this.w = img.getWidth();
            this.h = img.getHeight();
            this.numc = img.getColorModel().getNumColorComponents();
            this.bpc = img.getColorModel().getComponentSize()[0];
            this.nomtw = Math.min(tilesize, w);
            this.nomth = Math.min(tilesize, h);
            this.scanline = (w * numc * bpc + 7) >> 3;
            this.numx = (w + nomtw - 1)  / nomtw;
            this.numy = (h + nomth - 1)  / nomth;
            this.buf = new byte[scanline * h];
            tw = nomtw;
            th = nomth;
            Raster raster = img.getData();
            for (int y=0;y<h;y++) {
                for (int x=0;x<w;x++) {
                    for (int z=0;z<numc;z++) {
                        buf[y*scanline + x*numc + z] = (byte)raster.getSample(x, y, z);
                    }
                }
            }
        }

        MySrc(int w, int h, int numc, int bpc, int tilesize, InputStream in) {
            this.w = w;
            this.h = h;
            this.numc = numc;
            this.bpc = bpc;
            this.nomtw = Math.min(tilesize, w);
            this.nomth = Math.min(tilesize, h);
            this.scanline = (w * numc * bpc + 7) >> 3;
            this.numx = (w + nomtw - 1)  / nomtw;
            this.numy = (h + nomth - 1)  / nomth;
            this.buf = new byte[scanline * nomth];
            tw = nomtw;
            th = nomth;
            this.in = in;
            nextRow();
        }

        private void nextRow() {
//            System.out.println("nextrow: tx="+tx+" ty="+ty);
            if (in != null) {
                try {
                    int l = scanline * th;
                    int o = 0;
                    int c;
                    while (l > 0 && (c=in.read(buf, o, l)) >= 0) {
                        o += c;
                        l -= c;
                    }
                    if (ty + 1 == numy) {
                        System.out.println("Closing: read "+o+"/"+l+" of "+(scanline*th)+": tx="+tx+"/"+numx+" ty="+ty+"/"+numy+" sl="+scanline+" th="+th);
                        in.close();
                        in = null;
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        public int getFixedPoint(int c) {
            return 0;
        }

        public DataBlk getInternCompData(DataBlk blk, int c) {
            if (blk.getDataType() != DataBlk.TYPE_INT) {
                blk = new DataBlkInt(blk.ulx, blk.uly, blk.w, blk.h);
            }
//            System.out.println("Here: tx="+tx+"x"+ty+" c="+c+" blkxy="+blk.ulx+"x"+blk.uly+" blkwh="+blk.w+"x"+blk.h);
            DataBlkInt blki = (DataBlkInt)blk;
            if (blki.data == null || blki.data.length != blk.w * blk.h) {
                blki.data = new int[blk.w * blk.h];
            }
            for (int y=0;y<blk.h;y++) {
                int i = (blk.uly + y)*scanline + blk.ulx*numc + c;
                int o = blki.offset + y*tw;
                for (int x=0;x<tw;x++) {
                    blki.data[o] = (buf[i] & 0xFF) - 128;
                    i += numc;
                    o++;
                }
            }
            blk.progressive = false;
            return blk;
        }

        public DataBlk getCompData(DataBlk blk, int c) {
            throw new Error();
        }

        public int getTileWidth() {
            return tw;
        }

        public int getTileHeight() {
            return th;
        }

        public int getTileCompWidth(int t, int c) {
            return tw;
        }

        public int getTileCompHeight(int t, int c) {
            return th;
        }

        public void setTile(int x, int y) {
            System.out.println("setTile "+x+"x"+y);
            if (x < 0 || y < 0 || x >= numx || y >= numy) {
                throw new IllegalArgumentException("Tile "+x+"x"+y+" out of bounds");
            }
            tx = x;
            ty = y;
            tw = Math.min(nomtw, w - (tx * nomtw));
            th = Math.min(nomth, h - (ty * nomth));
        }

        public void nextTile() {
            System.out.println("nextTile: tx="+tx+"x"+ty);
            if (++tx == numx) {
                tx = 0;
                if (++ty == numy) {
                    throw new NoNextElementException();
                }
                nextRow();
            }
            tw = Math.min(nomtw, w - (tx * nomtw));
            th = Math.min(nomth, h - (ty * nomth));
        }

        public Point getTile(Point co) {
            if (co == null) {
                return new Point(tx, ty);
            } else {
                co.x = tx;
                co.y = ty;
                return co;
            }
        }

        public Point getNumTiles(Point co) {
            if (co == null) {
                return new Point(numx, numy);
            } else {
                co.x = numx;
                co.y = numy;
                return co;
            }
        }

        public int getNumComps() {
            return numc;
        }

        public int getNumTiles() {
            return numx * numy;
        }

        public int getTileGridXOffset() {
            return 0;
        }

        public int getTileGridYOffset() {
            return 0;
        }

        public int getTileIdx() {
            return ty * numx + tx;
        }

        public int getCompULX(int c) {
            return tx * nomtw;
        }

        public int getCompULY(int c) {
            return ty * nomth;
        }

        public int getImgWidth() {
            return w;
        }

        public int getImgHeight() {
            return h;
        }

        public int getCompImgWidth(int c) {
            return w;
        }

        public int getCompImgHeight(int c) {
            return h;
        }

        public int getTilePartULX() {
            return 0;
        }

        public int getTilePartULY() {
            return 0;
        }

        public int getCompSubsX(int c) {
            return 1;
        }

        public int getCompSubsY(int c) {
            return 1;
        }

        public int getNomRangeBits(int c) {
            return 8;
        }

        public int getImgULX() {
            return 0;
        }

        public int getImgULY() {
            return 0;
        }

        public int getNomTileWidth() {
            return nomtw;
        }

        public int getNomTileHeight() {
            return nomth;
        }
    }

}
