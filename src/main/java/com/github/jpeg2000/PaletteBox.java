/*
 * $RCSfile: PaletteBox.java,v $
 *
 * 
 * Copyright (c) 2005 Sun Microsystems, Inc. All  Rights Reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met: 
 * 
 * - Redistribution of source code must retain the above copyright 
 *   notice, this  list of conditions and the following disclaimer.
 * 
 * - Redistribution in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in 
 *   the documentation and/or other materials provided with the
 *   distribution.
 * 
 * Neither the name of Sun Microsystems, Inc. or the names of 
 * contributors may be used to endorse or promote products derived 
 * from this software without specific prior written permission.
 * 
 * This software is provided "AS IS," without a warranty of any 
 * kind. ALL EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND 
 * WARRANTIES, INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY, 
 * FITNESS FOR A PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY
 * EXCLUDED. SUN MIDROSYSTEMS, INC. ("SUN") AND ITS LICENSORS SHALL 
 * NOT BE LIABLE FOR ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF 
 * USING, MODIFYING OR DISTRIBUTING THIS SOFTWARE OR ITS
 * DERIVATIVES. IN NO EVENT WILL SUN OR ITS LICENSORS BE LIABLE FOR 
 * ANY LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT, SPECIAL,
 * CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER CAUSED AND
 * REGARDLESS OF THE THEORY OF LIABILITY, ARISING OUT OF THE USE OF OR
 * INABILITY TO USE THIS SOFTWARE, EVEN IF SUN HAS BEEN ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGES. 
 * 
 * You acknowledge that this software is not designed or intended for 
 * use in the design, construction, operation or maintenance of any 
 * nuclear facility. 
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 05:01:36 $
 * $State: Exp $
 */
package com.github.jpeg2000;

import java.awt.image.IndexColorModel;

import javax.imageio.metadata.IIOInvalidTreeException;
import javax.imageio.metadata.IIOMetadataNode;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/** This class is designed to represent a palette box for JPEG 2000 JP2 file
 *  format.  A palette box has a length, and a fixed type of "pclr".
 *
 * Its content contains the number of palette entry, the number of color
 * components, the bit depths of the output components, the LUT.
 *
 * Currently, only 8-bit color index is supported.
 */
public class PaletteBox extends Box {
    private int numEntries;
    private int numc;
    private int[] bitDepth;
    private int[] lut;

    /**
     * Constructs a <code>PlatteBox</code> from an
     * <code>IndexColorModel</code>.
     */
    public PaletteBox(IndexColorModel icm) {
        this(computeLength(icm), icm.hasAlpha() ? new int[] { 8, 8, 8, 8 } : new int[] { 8, 8, 8 }, getLUT(icm));
    }

    /**
     * Constructs a <code>PlatteBox</code> from an
     * <code>org.w3c.dom.Node</code>.
     */
    public PaletteBox(Node node) throws IIOInvalidTreeException {
        super(node);
        byte[][] tlut = null;
        int index = 0;

        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            String name = child.getNodeName();

            if ("NumberEntries".equals(name)) {
                numEntries = Box.getIntElementValue(child);
            }

            if ("NumberColors".equals(name)) {
                numc = Box.getIntElementValue(child);
            }

            if ("BitDepth".equals(name)) {
                bitDepth = Box.getIntArrayElementValue(child);
            }

            if ("LUT".equals(name)) {
                tlut = new byte[numEntries][];

                NodeList children1 = child.getChildNodes();

                for (int j = 0; j <children1.getLength(); j++) {
                    Node child1 = children1.item(j);
                    name = child1.getNodeName();
                    if ("LUTRow".equals(name)) {
                        tlut[index++] = Box.getByteArrayElementValue(child1);
                    }
                }
            }
        }

        //XXX: currently only 8-bit LUT is supported so no decode is needed
        // For more refer to read palette box section.
        lut = new int[numc*numEntries];

        int k = 0;
        for (int i = 0; i < numc; i++) {
            for (int j = 0; j < numEntries; j++) {
                lut[k++] = tlut[j][i];
            }
        }

    }

    /**
     * Constructs a <code>PlatteBox</code> from the provided length, bit
     *  depths of the color components and the LUT.
     */
    public PaletteBox(int length, int[] depths, int[] lut) {
        super(length, 0x70636C72, null);
        this.bitDepth = depths;
        this.lut = lut;
        this.numc = depths.length;
        this.numEntries = lut.length / numc;
    }

    /** Constructs a <code>PlatteBox</code> from the provided byte array.
     */
    public PaletteBox(byte[] data) {
        super(8 + data.length, 0x70636C72, data);
    }

    /** Compute the length of this box. */
    private static int computeLength(IndexColorModel icm) {
        int size = icm.getMapSize();
        int numc = icm.hasAlpha() ? 4 : 3;
        return 11 + numc + size * numc;
    }

    /** Gets the LUT from the <code>IndexColorModel</code> as an two-dimensional
     *  byte array.
     */
    private static int[] getLUT(IndexColorModel icm) {
        int size = icm.getMapSize();
        int numc = icm.hasAlpha() ? 3 : 4;   // IndexColorModel always RGB or RGBA
        int[] lut = new int[size * numc];
        int k = 0;
        for (int i=0;i<size;i++) {
            lut[k++] = icm.getRed(i);
            lut[k++] = icm.getGreen(i);
            lut[k++] = icm.getBlue(i);
            if (numc == 4) {
                lut[k++] = icm.getAlpha(i);
            }
        }
        return lut;
    }

    /** Return the number of palette entries. */
    public int getNumEntries() {
        return numEntries;
    }

    /** Return the number of color components. */
    public int getNumComp() {
        return numc;
    }

    public int getComponentDepth(int component) {
        return (bitDepth[component] & 0x7F) + 1;
    }

    public boolean isComponentSigned(int component) {
        return (bitDepth[component] & 0x80) != 0;
    }

    public int getComponentValue(int entry, int component) {
        return lut[entry * numc + component];
    }

    /** creates an <code>IIOMetadataNode</code> from this palette box.
     *  The format of this node is defined in the XML dtd and xsd
     *  for the JP2 image file.
     */
    public IIOMetadataNode getNativeNode() {
        IIOMetadataNode node = new IIOMetadataNode(Box.getName(getType()));
        setDefaultAttributes(node);

        IIOMetadataNode child = new IIOMetadataNode("NumberEntries");
        child.setUserObject(new Integer(numEntries));
	child.setNodeValue("" + numEntries);
        node.appendChild(child);

        child = new IIOMetadataNode("NumberColors");
        child.setUserObject(new Integer(numc));
	child.setNodeValue("" + numc);
        node.appendChild(child);

        child = new IIOMetadataNode("BitDepth");
        child.setUserObject(bitDepth);
	child.setNodeValue(ImageUtil.convertObjectToString(bitDepth));
        node.appendChild(child);

        child = new IIOMetadataNode("LUT");
        for (int i = 0; i < numEntries; i++) {
            IIOMetadataNode child1 = new IIOMetadataNode("LUTRow");
            byte[] row = new byte[numc];
            for (int j = 0; j < numc; j++) {
                row[j] = (byte)getComponentValue(i, j);
            }

            child1.setUserObject(row);
	    child1.setNodeValue(ImageUtil.convertObjectToString(row));
            child.appendChild(child1);
        }
        node.appendChild(child);

        return node;
    }

    @Override
    protected void parse(byte[] data) {
        System.out.println("PLT: "+new java.math.BigInteger(1, data).toString(16));
        int k = 0;
        numEntries = ((data[k++]&0xFF)<<8) | (data[k++]&0xFF);
        numc = data[k++]&0xFF;
        bitDepth = new int[numc];
        for (int i=0;i<numc;i++) {
            bitDepth[i] = data[k++]&0xFF;
        }
        lut = new int[data.length - k];
        for (int i=0;i<lut.length;i++) {
            int c = i % numc;
            int d = getComponentDepth(c);
            boolean signed = isComponentSigned(c);
            if (d <= 8) {
                lut[i] = signed ? (int)data[k++] : data[k++]&0xFF;
            } else {
                lut[i] = ((signed ? (int)data[k++] : data[k++]&0xFF)<<8) | (data[k++]&0xFF);
            }
        }
    }

    protected void compose() {
        if (data != null) {
            return;
        }
        data = new byte[3 + numc + numEntries * numc];
        int k = 0;
        data[k++] = (byte)(numEntries >> 8);
        data[k++] = (byte)(numEntries & 0xFF);
        data[k++] = (byte)numc;

        for (int i=0;i<numc;i++) {
            data[k++] = (byte)bitDepth[i];
        }

        for (int i = 0; i < numEntries; i++) {
            for (int j = 0; j < numc; j++) {
                data[k++] = (byte)getComponentValue(i, j);
            }
        }
    }
}
