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
        J2KWriter c = new J2KWriter();
        c.setCompressionRatio(4, true);
        c.setSource(ImageIO.read(new File(args[0])), 256);
        OutputStream out = new BufferedOutputStream(new FileOutputStream("out.jp2"));
        c.write(out);
    }

}
