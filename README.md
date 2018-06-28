jpeg2000
====================

This is another version of the "JJ2000" package, which provides an API for reading and writing JPEG-2000 encoded image data in Java. It is based on the "JAI" branch of the code, but with the JAI dependencies removed so it can be compiled as a standalone package again.

How to build
------------
Download and run "ant". A single Jar is built in "target/jj2000.jar". There are no external dependencies

License
--------------------
The JJ2000 portion of the code is covered under the  [JJ2000](LICENSE-JJ2000.txt) license. The JAI portions of the code are covered under a modified [BSD](LICENSE-Sun.txt) license.

History (which may be wrong)
----------------------------
The JJ2000 package was originally written by a team from Swiss Federal Institute of Technology-EPFL, Ericsson Radio Systems AB and Canon Research Centre France S.A during 1999-2000 as part of the development of the original JPEG2000 specification. The source code was made available at http://jpeg2000.epfl.ch/ and the final release there was version 5.1 (this site disappeared around 2010; an archive version is available at http://web.archive.org/web/20100818165144/http://jpeg2000.epfl.ch/)

The code was then adopted by Sun as part of their JAI project (Java Advanced Imaging). It was hosted at https://jai-imageio.dev.java.net/, with  changes made by Sun to fit into their JAI architecture. This eventually shut down too (disappearing completely in 2016 after a long period of bitrot) and the code was migrated to Github and https://github.com/jai-imageio/jai-imageio-jpeg2000 in April 2010, where this fork came from. The [JAI project](https://github.com/jai-imageio/) is still active.

The original pre-JAI code from JJ2000 also moved, to https://code.google.com/archive/p/jj2000/. From there it was copied to Github at https://github.com/Unidata/jj2000, and probably elsewhere too.

The two codebases diverged slightly; the "ucar" build (derived from the original JJ2000 codebase) had issues with failing to read the last tile from JP2 streams where the number of tiles was listed as one less than required; common in many of our test files. This has been [Patched](https://github.com/Unidata/jj2000/pull/8). The "jai" build had issues with an integer overflow, usually causing black blobs on the image. This has also been [Patched](https://github.com/jai-imageio/jai-imageio-jpeg2000/pull/24), although at the time of writing the pull request has not been merged, because maven.

Although the two packages are largely identical in terms of API and functionality, we have found the "JAI" branch to use less memory overall for the kind of files where this is an issue (large, high resolution, single tile images - the differences seems to stem from the changes in jj2000.j2k.codestream.reader.FileBitstreamReaderAgent). So we've chosen to base this branch on that version.
