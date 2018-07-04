package com.github.jpeg2000;

import java.io.*;
import java.util.*;
import jj2000.j2k.io.*;

public class ResolutionSuperBox extends ContainerBox {
    
    private ResolutionBox res;

    public ResolutionSuperBox() {
        super(fromString("res "));
    }

    public ResolutionSuperBox(ResolutionBox box) {
        this();
        add(box);
    }

    public ResolutionBox getResolutionBox() {
        return res;
    }

    @Override public ContainerBox add(Box box) {
        if (box instanceof ResolutionBox) {
            if (res == null) {
                res = (ResolutionBox)box;
            } else {
                throw new IllegalStateException("More than one resc/resd box");
            }
        }
        return super.add(box);
    }
}
