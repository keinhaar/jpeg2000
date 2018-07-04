package com.github.jpeg2000;

import java.io.*;
import jj2000.j2k.io.*;
import java.awt.image.ColorModel;


/**
 *  This class is designed to represent a Channel Definition Box of
 *  JPEG JP2 file format.  A Channel Definition Box has a length, and
 *  a fixed type of "cdef".  Its content defines the type of the image
 *  channels: color channel, alpha channel or premultiplied alpha channel.
 */
public class ChannelDefinitionBox extends Box {

    /** The cached data elements. */
    private short num;
    private short[] channels;
    private short[] types;
    private short[] associations;

    /** Fills the channel definitions into the arrays based on the number
     *  of components and isPremultiplied.
     */
    public static void fillBasedOnBands(int numComps,
                                        boolean isPremultiplied,
                                        short[] c,
                                        short[] t,
                                        short[] a) {
        int num = numComps * (isPremultiplied ? 3 : 2);
        if (isPremultiplied) {
            for (int i = numComps * 2; i < num; i++) {
                c[i] = (short)(i - numComps * 2);
                t[i] = 2;       // 2 -- premultiplied
                a[i] = (short)(i + 1 - numComps * 2);
            }
        }

        for (int i = 0; i < numComps; i++) {
            int j = i + numComps;
            c[i] = (short)i;
            t[i] = 0;       // The original channel
            a[j] = a[i] = (short)(i + 1);

            c[j] = (short)numComps;
            t[j] = 1;           // 1 -- transparency
        }
    }

    public ChannelDefinitionBox() {
        super(fromString("cdef"));
    }

    /** Constructs a <code>ChannelDefinitionBox</code> based on the provided
     *  <code>ColorModel</code>.
     */
    public ChannelDefinitionBox(ColorModel colorModel) {
        this();

        // creates the buffers for the channel definitions.
        short length = (short)(colorModel.getComponentSize().length - 1);
        num = (short)(length * (colorModel.isAlphaPremultiplied() ? 3 : 2));
        channels = new short[num];
        types = new short[num];
        associations = new short[num];

        // fills the arrays.
        fillBasedOnBands(length, colorModel.isAlphaPremultiplied(), channels, types, associations);
    }

    /** 
     * Constructs a <code>ChannelDefinitionBox</code> based on the provided
     * channel definitions.
     */
    public ChannelDefinitionBox(short[] channel, short[] types, short[] associations) {
        this();
        this.num = (short)channel.length;
        this.channels = channel;
        this.types = types;
        this.associations = associations;
    }

    @Override public void read(RandomAccessIO in) throws IOException {
        num = in.readShort();
        channels = new short[num];
        types = new short[num];
        associations = new short[num];

        for (int i=0;i<num;i++) {
            channels[i] = in.readShort();
            channels[i] = in.readShort();
            associations[i] = in.readShort();
        }
    }

    @Override public int getLength() {
        return 2 + num*6;
    }

    @Override public void write(DataOutputStream out) throws IOException {
        out.writeShort(num);
        for (int i=0;i<num;i++) {
            out.writeShort(channels[i]);
            out.writeShort(types[i]);
            out.writeShort(associations[i]);
        }
    }

    /** Returns the defined channels. */
    public short[] getChannel() {
        return channels;
    }

    /** Returns the channel types. */
    public short[] getTypes() {
        return types;
    }

    /** Returns the association which associates a color channel to a color
     *  component in the color space of the image.
     */
    public short[] getAssociation() {
        return associations;
    }

}
