package com.github.jpeg2000;

import java.io.*;
import java.util.*;
import jj2000.j2k.io.*;
import javax.xml.stream.*;

public class J2KFile {
    
    private static final long SIGMARKER = 0x6a5020200d0a870al;
    private HeaderBox jp2h;
    private FileTypeBox ftyp;
    private CodeStreamBox jp2c;
    private List<Box> boxes;

    public J2KFile() {
        boxes = new ArrayList<Box>();
    }

    public void read(RandomAccessIO in) throws IOException {
        if (in.readInt() != 12 || in.readInt() != SIGMARKER>>32 || in.readInt() != (int)SIGMARKER) {
            throw new IOException("No JP2 Signature Box");
        }
        while (in.length() - in.getPos() > 0) {
            add(ContainerBox.readBox(in));
        }
    }

    public J2KFile add(Box box) throws IOException {
        if (box instanceof FileTypeBox) {
            if (ftyp == null) {
                ftyp = (FileTypeBox)box;
            } else {
                throw new IOException("More than one ftyp box");
            }
        } else if (ftyp == null) {
            throw new IOException("No ftyp Box");
        } else if (box instanceof HeaderBox) {
            if (jp2h == null) {
                jp2h = (HeaderBox)box;
            } else {
                throw new IOException("More than one jp2h box");
            }
        } else if (box instanceof CodeStreamBox) {
            if (jp2h == null) {
                throw new IOException("jp2c must follow jp2h");
            } else if (jp2c == null) {
                jp2c = (CodeStreamBox)box;
            } else {
                // Others are allowed.
            }
        }
        boxes.add(box);
        return this;
    }

    public HeaderBox getHeaderBox() {
        return jp2h;
    }

    public CodeStreamBox getCodeStreamBox() {
        return jp2c;
    }

    public List<Box> getBoxes() {
        return Collections.<Box>unmodifiableList(boxes);
    }

    public void write(OutputStream out) throws IOException {
        if (jp2h == null || jp2c == null) {
            throw new IOException("Missing jp2h or jp2c");
        }
        DataOutputStream o = new DataOutputStream(out);
        o.writeInt(12);
        o.writeLong(SIGMARKER);
        for (int i=0;i<boxes.size();i++) {
            ContainerBox.writeBox(boxes.get(i), o);
        }
    }

    public XMLStreamWriter write(XMLStreamWriter out) throws XMLStreamException {
        out.writeStartElement("j2k");
        for (Box box : boxes) {
            box.write(out);
        }
        out.writeEndElement();
        return out;
    }

}
