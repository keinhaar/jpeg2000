package com.github.jpeg2000;

import java.io.*;
import java.util.*;
import jj2000.j2k.io.*;
import javax.xml.stream.*;

/**
 * Represents the "uinf" box
 *
 * @author http://bfo.com
 */
public class UUIDInfoBox extends ContainerBox {
    
    private UUIDListBox ulst;
    private URLBox url;

    public UUIDInfoBox() {
        super(fromString("uinf"));
    }

    public UUIDInfoBox(UUIDListBox ulst, URLBox url) {
        this();
        add(ulst);
        add(url);
    }

    public UUIDListBox getUUIDListBox() {
        return ulst;
    }

    public URLBox getURLBox() {
        return url;
    }

    @Override public ContainerBox add(Box box) {
        if (box instanceof UUIDListBox) {
            if (ulst == null) {
                ulst = (UUIDListBox)box;
            } else {
                throw new IllegalStateException("More than one ulst box");
            }
        } else if (box instanceof URLBox) {
            if (ulst == null) {
                throw new IllegalStateException("ulst box must come before url box");
            } else if (url == null) {
                url = (URLBox)box;
            } else {
                throw new IllegalStateException("More than one url box");
            }
        }
        return super.add(box);
    }
}
