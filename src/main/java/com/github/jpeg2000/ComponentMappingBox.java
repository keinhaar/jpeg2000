package com.github.jpeg2000;

import java.io.*;
import jj2000.j2k.io.*;
import javax.xml.stream.*;

/** 
 *  This class is defined to represent a Color Specification Box of JPEG JP2
 *  file format.  A Channel Definition Box has a length, and a fixed type
 *  of "cmap".  This box exists if and only is a PaletteBox exists.  Its
 *  content defines the type LUT output components and their mapping to the
 *  color component.
 */
public class ComponentMappingBox extends Box {
    /** The data elements. */
    private short[] components;
    private byte[] type;
    private byte[] map;

    public ComponentMappingBox() {
        super(fromString("cmap"));
    }

    /** Constructs a <code>ComponentMappingBox</code> from the provided
     *  component mapping.
     */
    public ComponentMappingBox(short[] comp, byte[] t, byte[] m) {
        this();
        this.components = comp;
        this.type = t;
        this.map = m;
    }

    @Override public int getLength() {
        return components.length * 4;
    }

    /** Parse the component mapping from the provided content data array. */
    @Override public void read(RandomAccessIO in) throws IOException {
        int len = in.length() / 4;
        components = new short[len];
        type = new byte[len];
        map = new byte[len];

        for (int i=0;i<len;i++) {
            components[i] = in.readShort();
            type[i] = in.readByte();
            map[i] = in.readByte();
        }
    }

    @Override public void write(DataOutputStream out) throws IOException {
        for (int i=0;i<type.length;i++) {
            out.writeShort(components[i]);
            out.write(type[i]);
            out.write(map[i]);
        }
    }

    public short[] getComponent() {
        return components;
    }

    public byte[] getComponentType() {
        return type;
    }

    public byte[] getComponentAssociation() {
        return map;
    }

    @Override public void write(XMLStreamWriter out) throws XMLStreamException {
        out.writeStartElement(toString(getType()).trim());
        out.writeAttribute("length", Integer.toString(getLength()));
        for (int i=0;i<components.length;i++) {
            out.writeEmptyElement("cmap");
            out.writeAttribute("component", Integer.toString(components[i]));
            out.writeAttribute("type", Integer.toString(type[i]));
            out.writeAttribute("assoc", Integer.toString(map[i]));
        }
        out.writeEndElement();
    }
}
