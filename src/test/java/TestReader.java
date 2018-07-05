import jj2000.j2k.io.*;
import com.github.jpeg2000.*;
import java.io.*;
import java.awt.color.ColorSpace;

public class TestReader {
    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            System.err.println("Usage: java -jar test.jar file.jpx [file.jpx...]");
            System.exit(0);
        }
        for (String s : args) {
            File infile = new File(s);
            if (s.toLowerCase().endsWith(".jp2") || s.toLowerCase().endsWith(".jpx")) {
                s = s.substring(0, s.length() - 4);
            }
            File outfile = new File(s + ".pnm");
            RandomAccessIO in = new BEBufferedRandomAccessFile(infile, "r", 8192);
            final int[] enumcs = new int[1];
            J2KReader iin = new J2KReader(new J2KFile().read(in)) {
                protected ColorSpace createColorSpace(int e) {
                    enumcs[0] = e;
                    return super.createColorSpace(e);
                }
            };

            System.out.print("# Creating "+outfile+": "+iin.getWidth()+"x"+iin.getHeight()+"x"+iin.getNumComponents());
            if (iin.isIndexed()) {
                System.out.print(" palette="+iin.getIndexSize());
            }
            ColorSpace cs = iin.getColorSpace();
            if (cs == null) {
                System.out.println(" cs=unknown("+enumcs[0]+")");
            } else if (enumcs[0] == 16) {
                System.out.println(" cs=sRGB");
            } else if (enumcs[0] == 17) {
                System.out.println(" cs=gray");
            } else {
                System.out.println(" cs=icc("+cs.getNumComponents()+")");
            }

            int type = 0;
            if (iin.getNumComponents() == 1) {
                // Indexed images will also be caught in this block
                type = 5;
            } else if (iin.getNumComponents() == 3) {
                // Lab color images will also be caught in this block
                type = 6;
            }
            if (type != 0) {
                OutputStream out = new BufferedOutputStream(new FileOutputStream(outfile));
                out.write(("P"+type+" "+iin.getWidth()+" "+iin.getHeight()+" 255\n").getBytes("ISO-8859-1"));
                int c;
                while ((c=iin.read()) >= 0) {
                    out.write(c);
                }
                out.close();
            }
        }
    }
}
