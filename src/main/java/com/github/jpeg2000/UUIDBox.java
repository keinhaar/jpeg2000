package com.github.jpeg2000;

import java.io.*;
import jj2000.j2k.io.*;
import javax.xml.stream.*;
import javax.xml.transform.*;
import javax.xml.transform.stream.*;
import javax.xml.transform.stax.*;

/** This class is defined to represent a UUID Box of JPEG JP2
 *  file format.  This type of box has a length, a type of "uuid".  Its
 *  content is a 16-byte UUID followed with a various-length data.
 */
public class UUIDBox extends Box {

    public static final String UUID_XMP = "be7acfcb97a942e89c71999491e3afac";
    /** The data elements in this UUID box. */
    private byte[] uuid;
    private byte[] data;

    /**
     * Constructs a <code>UUIDBox</code> from its content data array.
     */
    public UUIDBox() {
        super(fromString("uuid"));
    }

    public UUIDBox(String key, byte[] data) {
        this();
        if (key.length() != 32) {
            throw new IllegalArgumentException();
        }
        uuid = new byte[16];
        for (int i=0;i<16;i++) {
            uuid[i] = (byte)((Character.digit(key.charAt(i*2), 16) << 4) + Character.digit(key.charAt(i*2+1), 16));
        }
        this.data = data;
    }

    @Override public int getLength() {
        return uuid.length + data.length;
    }

    @Override public void read(RandomAccessIO in) throws IOException {
        uuid = new byte[16];
        in.readFully(uuid, 0, 16);
        data = new byte[in.length() - in.getPos()];
        in.readFully(data, 0, data.length);
    }

    @Override public void write(DataOutputStream out) throws IOException {
        out.write(uuid, 0, uuid.length);
        out.write(data, 0, data.length);
    }

    /** Returns the UUID of this box. */
    public String getUUID() {
        return toString(uuid);
    }

    /** Returns the UUID data of this box. */
    public byte[] getData() {
        return data;
    }

    @Override public void write(XMLStreamWriter out) throws XMLStreamException {
        out.writeStartElement(toString(getType()).trim());
        out.writeAttribute("length", Integer.toString(getLength()));
        out.writeAttribute("uuid", getUUID());
        boolean raw = true;
        if (UUID_XMP.equals(getUUID())) {
            try {
                String s = new String(getData(), "UTF-8");
                TransformerFactory tf = TransformerFactory.newInstance();
                Transformer t = tf.newTransformer();
                t.transform(new StreamSource(new StringReader(s)), new StreamResult(new StringWriter()));
                // Syntax is valid, redo output to actual outputstream
                t.transform(new StreamSource(new StringReader(s)), new StAXResult(out));
                raw = false;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        if (raw) {
            out.writeCharacters(toString(data));
        }
        out.writeEndElement();
    }
}
