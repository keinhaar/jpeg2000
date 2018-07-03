package com.github.jpeg2000;

/**
 * Interface which defines the parameters required to write a JP2 image.
 * Abstracted away from J2KImageReadParamJava
 */
public interface J2KReadParam {
    
    public boolean getNoROIDescaling();

    public double getDecodingRate();

    public int getResolution();

}
