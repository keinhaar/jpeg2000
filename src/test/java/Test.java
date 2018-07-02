import jj2000.j2k.io.*;
import com.github.jpeg2000.*;
import java.io.*;


public class Test {
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
            ImageInputStream iin = new ImageInputStream(in);
            OutputStream out = new BufferedOutputStream(new FileOutputStream(outfile));
            System.out.println("# Creating "+outfile);
            if (iin.getNumComponents() == 1) {
                // Indexed images will also be caught in this block
                out.write(("P4 "+iin.getWidth()+" "+iin.getHeight()+" 255\n").getBytes("ISO-8859-1"));
            } else if (iin.getNumComponents() == 3) {
                // Lab color images will also be caught in this block
                out.write(("P6 "+iin.getWidth()+" "+iin.getHeight()+" 255\n").getBytes("ISO-8859-1"));
            } else {
                // CMYK images here; there is no PNM format for CMYK, make one up.
                out.write(("PX "+iin.getWidth()+" "+iin.getHeight()+" 255\n").getBytes("ISO-8859-1"));
            }
            int c;
            while ((c=iin.read()) >= 0) {
                out.write(c);
            }
            out.close();
        }
    }
}
