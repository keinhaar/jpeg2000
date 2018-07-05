package com.github.jpeg2000;

import java.io.*;
import jj2000.j2k.io.*;
import javax.xml.stream.*;

/** This class is defined to represent a UUID list Box of JPEG JP2
 *  file format.  This type of box has a length, a type of "ulst".  Its
 *  contents include the number of UUID entry and a list of 16-byte UUIDs.
 */
public class UUIDListBox extends Box {

    private byte[][] uuids;

    public UUIDListBox() {
        super(fromString("ulst"));
    }

    /** Constructs a <code>UUIDListBox</code> from the provided uuid number
     *  and uuids.  The provided uuids should have a size of 16; otherwise,
     *  <code>Exception</code> may thrown in later the process.  The provided
     *  number should consistent with the size of the uuid array.
     */
    public UUIDListBox(byte[][] uuids) {
        this();
        this.uuids = uuids;
    }

    @Override public int getLength() {
         return 2 + uuids.length * 16;
    }

    @Override public void read(RandomAccessIO in) throws IOException {
        int num = in.readShort();
        uuids = new byte[num][16];
        for (int i=0;i<num;i++) {
            in.readFully(uuids[i], 0, 16);
        }
    }

    @Override public void write(DataOutputStream out) throws IOException {
        out.writeShort(uuids.length);
        for (int i=0;i<uuids.length;i++) {
            out.write(uuids[i]);
        }
    }

    @Override public void write(XMLStreamWriter out) throws XMLStreamException {
        out.writeStartElement(toString(getType()).trim());
        out.writeAttribute("length", Integer.toString(getLength()));
        for (int i=0;i<uuids.length;i++) {
            out.writeStartElement("uuid");
            out.writeCharacters(toString(uuids[i]));
            out.writeEndElement();
        }
        out.writeEndElement();
    }
}
