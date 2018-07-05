package com.github.jpeg2000;

import java.io.*;
import jj2000.j2k.io.*;
import javax.xml.stream.*;

/** 
 * This class represnts the "xml " box.
 *
 * @author http://bfo.com
 */
public class XMLBox extends Box {

    public XMLBox() {
        super(fromString("xml "));
    }

    // TODO
}
