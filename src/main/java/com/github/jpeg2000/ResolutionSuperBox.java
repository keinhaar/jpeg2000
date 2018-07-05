package com.github.jpeg2000;

import java.io.*;
import java.util.*;
import jj2000.j2k.io.*;
import javax.xml.stream.*;

/**
 * Represents the "res" resolution superbox
 */
public class ResolutionSuperBox extends ContainerBox {
    
    public ResolutionSuperBox() {
        super(fromString("res "));
    }

    public ResolutionSuperBox(ResolutionBox box) {
        this();
        add(box);
    }

}
