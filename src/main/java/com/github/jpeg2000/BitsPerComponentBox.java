package com.github.jpeg2000;

import jj2000.j2k.io.RandomAccessIO;
import java.io.*;

/** This class is defined to represent a Bits Per Component Box of JPEG
 *  JP2 file format.  A Bits Per Component box has a length, and a fixed
 *  type of "bpcc".  Its content is a byte array containing the bit
 *  depths of the color components.
 *
 *  This box is necessary only when the bit depth are not identical for all
 *  the components.
 */
public class BitsPerComponentBox extends Box {

    private byte[] data;

    public BitsPerComponentBox() {
        super(fromString("bpcc"));
    }

    public BitsPerComponentBox(byte[] bitDepth) {
        this();
        this.data = bitDepth;
    }

    @Override public int getLength() {
        return data.length;
    }

    @Override public void read(RandomAccessIO in) throws IOException {
        data = new byte[in.length()];
        in.readFully(data, 0, data.length);
    }

    @Override public void write(DataOutputStream out) throws IOException {
        out.write(data);
    }

    /**
     * Returns the bit depths for all the image components.
     */
    public byte[] getBitDepth() {
        return data;
    }

}
