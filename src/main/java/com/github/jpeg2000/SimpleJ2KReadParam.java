package com.github.jpeg2000;

/**
 * Interface which defines the parameters required to read a JP2 image.
 * The default values for each property are used.
 *
 * @author http://bfo.com
 */
public class SimpleJ2KReadParam implements J2KReadParam {
    
    public boolean getNoROIDescaling() {
        return true;
    }

    public double getDecodingRate() {
        return Double.MAX_VALUE;
    }

    public int getResolution() {
        return -1;
    }

}
