package com.github.jpeg2000;

import java.io.*;
import jj2000.j2k.io.*;
import java.awt.color.ICC_Profile;

/** This class is defined to represent a Color Specification Box of JPEG JP2
 *  file format.  A Channel Definition Box has a length, and a fixed type
 *  of "colr".  Its content contains the method to define the color space,
 *  the precedence and approximation accuracy (0 for JP2 files), the
 *  enumerated color space, and the ICC color profile if any.
 */
public class ColorSpecificationBox extends Box {

    /** The enumerated color space defined in JP2 file format. */
    public static final int ECS_sRGB = 16;
    public static final int ECS_GRAY = 17;
    public static final int ECS_YCC = 18;

    /** The elements' values. */
    private byte method;
    private byte precedence;
    private byte approximation;
    private int ecs;
    private ICC_Profile profile;

    public ColorSpecificationBox() {
        super(fromString("colr"));
    }

    public ColorSpecificationBox(int ecs) {
        this(1, 0, 1, ecs, null);
    }

    public ColorSpecificationBox(ICC_Profile profile) {
        this(2, 0, 1, 0, profile);
    }

    /** 
     *  Creates a <code>ColorSpecificationBox</code> from the provided data
     *  elements.
     */
    public ColorSpecificationBox(int m, int p, int a, int ecs, ICC_Profile profile) {
        this();
        this.method = (byte)m;
        this.precedence = (byte)p;
        this.approximation = (byte)a;
        this.ecs = ecs;
        this.profile = profile;
    }

    /** Returns the method to define the color space. */
    public byte getMethod() {
        return method;
    }

    public String toString() {
        return "{colr: meth="+method+" prec="+precedence+" approx="+approximation+" ecs="+ecs+"}";

    }
    /** Returns <code>Precedence</code>. */
    public byte getPrecedence() {
        return precedence;
    }

    /** Returns <code>ApproximationAccuracy</code>. */
    public byte getApproximationAccuracy() {
        return approximation;
    }

    /** Returns the enumerated color space. */
    public int getEnumeratedColorSpace() {
        return ecs;
    }

    /** Returns the ICC color profile in this color specification box. */
    public ICC_Profile getICCProfile() {
        return profile;
    }

    @Override public int getLength() {
        return 3 + (method == 1 ? 4 : profile.getData().length);
    }

    @Override public void read(RandomAccessIO in) throws IOException {
        method = in.readByte();
        precedence = in.readByte();
        approximation = in.readByte();
        if (method == 2 || method == 3) {
            byte[] proData = new byte[in.length() - in.getPos()];
            in.readFully(proData, 0, proData.length);
            profile = ICC_Profile.getInstance(proData);
        } else {
            ecs = in.readInt();
        }
    }

    @Override public void write(DataOutputStream out) throws IOException {
        out.write(method);
        out.write(precedence);
        out.write(approximation);
        if (method == 1) {
            out.writeInt(ecs);
        } else if (profile != null) {
            out.write(profile.getData());
        }
    }

}
