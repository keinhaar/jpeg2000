package com.github.jpeg2000;

import java.io.*;
import jj2000.j2k.io.*;

/** This class is defined to represent a XML box of JPEG JP2
 *  file format.  This type of box has a length, a type of "xml ".  Its
 *  content is a text string of a XML instance.
 */
public class CodeStreamBox extends Box {

    private byte[] data;
    private int length;
    private RandomAccessIO io;

    public CodeStreamBox() {
        super(fromString("jp2c"));
    }

    public CodeStreamBox(byte[] data) {
        this();
        this.data = data;
        this.length = data.length;
    }

    @Override public int getLength() {
        return length;
    }

    @Override public void read(RandomAccessIO io) throws IOException {
        this.io = io;
        this.length = io.length();
    }

    @Override public void write(DataOutputStream out) throws IOException {
        if (data != null) {
            out.write(data);
        } else if (io != null) {
            byte[] b = new byte[8192];
            io.seek(0);
            int remaining = io.length();
            while (remaining > 0) {
                int c = Math.min(b.length, remaining);
                io.readFully(b, 0, c);
                out.write(b, 0, c);
                remaining -= c;
            }
        }
    }

    public RandomAccessIO getRandomAccessIO() throws IOException {
        if (io != null) {
            io.seek(0);
            return io;
        }
        throw new IllegalStateException("Not created from a RandomAccessIO");
    }

}
