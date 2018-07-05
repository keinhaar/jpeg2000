package com.github.jpeg2000;

import java.io.*;
import jj2000.j2k.io.*;
import javax.xml.stream.*;

/** This class is defined to represent a UUID Box of JPEG JP2
 *  file format.  This type of box has a length, a type of "uuid".  Its
 *  content is a 16-byte UUID followed with a various-length data.
 */
public class UUIDBox extends Box {

    /** The data elements in this UUID box. */
    private byte[] uuid;
    private byte[] udata;

    /**
     * Constructs a <code>UUIDBox</code> from its content data array.
     */
    public UUIDBox() {
        super(fromString("uuid"));
    }

    @Override public int getLength() {
        return uuid.length + udata.length;
    }

    @Override public void read(RandomAccessIO in) throws IOException {
        uuid = new byte[16];
        in.readFully(uuid, 0, 16);
        udata = new byte[in.length() - in.getPos()];
        in.readFully(udata, 0, udata.length);
    }

    @Override public void write(DataOutputStream out) throws IOException {
        out.write(uuid, 0, uuid.length);
        out.write(udata, 0, udata.length);
    }

    /** Returns the UUID of this box. */
    public byte[] getUUID() {
        return uuid;
    }

    /** Returns the UUID data of this box. */
    public byte[] getData() {
        return udata;
    }

    @Override public void write(XMLStreamWriter out) throws XMLStreamException {
        out.writeStartElement(toString(getType()).trim());
        out.writeAttribute("length", Integer.toString(getLength()));
        out.writeAttribute("uuid", toString(uuid));
        out.writeCharacters(toString(udata));
        out.writeEndElement();
    }
}
