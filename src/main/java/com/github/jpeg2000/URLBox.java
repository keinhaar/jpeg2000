package com.github.jpeg2000;

import java.io.*;
import jj2000.j2k.io.*;

/** This class is defined to represent a Data Entry URL Box of JPEG JP2
 *  file format.  A Data Entry URL Box has a length, and a fixed type
 *  of "url ".  Its content are a one-byte version, a three-byte flags and
 *  a URL pertains to the UUID List box within its UUID Info superbox.
 */
public class URLBox extends Box {

    /** The element values. */
    private byte version;
    private byte[] flags;
    private String url;

    public URLBox() {
        super(fromString("url "));
    }

    /** Constructs a <code>DataEntryURLBox</code> from its data elements. */
    public URLBox(byte version, byte[] flags, String url) {
        this();
        this.version = version;
        this.flags = flags;
        this.url = url;
    }

    @Override public int getLength() {
        return 4 + url.length();
    }

    @Override public void read(RandomAccessIO in) throws IOException {
        version = in.readByte();
        flags = new byte[3];
        flags[0] = in.readByte();
        flags[1] = in.readByte();
        flags[2] = in.readByte();

        byte[] b = new byte[in.length() - in.getPos()];
        in.readFully(b, 0, b.length);
        url = new String(b, 0, b.length, "ISO-8859-1");
    }

    @Override public void write(DataOutputStream out) throws IOException {
        out.write(version);
        out.write(flags[0]);
        out.write(flags[1]);
        out.write(flags[2]);
        out.write(url.getBytes("ISO-8859-1"));
    }

    /** Returns the <code>Version</code> data element. */
    public byte getVersion() {
        return version;
    }

    /** Returns the <code>Flags</code> data element. */
    public byte[] getFlags() {
        return flags;
    }

    /** Returns the <code>URL</code> data element. */
    public String getURL() {
        return url;
    }

}
