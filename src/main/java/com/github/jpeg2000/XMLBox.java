package com.github.jpeg2000;

import java.io.*;
import jj2000.j2k.io.*;

/** This class is defined to represent a XML box of JPEG JP2
 *  file format.  This type of box has a length, a type of "xml ".  Its
 *  content is a text string of a XML instance.
 */
public class XMLBox extends Box {

    public XMLBox() {
        super(fromString("xml "));
    }

}
