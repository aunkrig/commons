
/*
 * de.unkrig.commons - A general-purpose Java class library
 *
 * Copyright (c) 2013, Arno Unkrig
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice, this list of conditions and the
 *       following disclaimer.
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
 *       following disclaimer in the documentation and/or other materials provided with the distribution.
 *    3. The name of the author may not be used to endorse or promote products derived from this software without
 *       specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL
 * THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package de.unkrig.commons.junit4;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.CompressorOutputStream;

import de.unkrig.commons.file.org.apache.commons.compress.archivers.ArchiveFormat;
import de.unkrig.commons.file.org.apache.commons.compress.archivers.ArchiveFormatFactory;
import de.unkrig.commons.file.org.apache.commons.compress.compressors.CompressionFormat;
import de.unkrig.commons.file.org.apache.commons.compress.compressors.CompressionFormatFactory;
import de.unkrig.commons.lang.protocol.ConsumerWhichThrows;
import de.unkrig.commons.nullanalysis.Nullable;

/**
 * A helper class for creating, reading and comparing directory trees (and contained ZIP files). Extremely useful for
 * testing code that processes or transforms directory trees and/or ZIP files.
 */
public
class Files {

    private final Object desc;

    /** Initializes this object from the given object array. */
    public
    Files(Object[] desc) { this.desc = desc; }

    /** Initializes this object from the given file or directory. */
    public
    Files(File file) throws IOException, ArchiveException, CompressorException { this.desc = Files.load(file); }

    /** @return The contents of the given file */
    private static Object
    load(File file) throws IOException, ArchiveException, CompressorException {

        // Directory?
        if (file.isDirectory()) return Files.loadDir(file);

        // Archive file?
        ArchiveFormat af = ArchiveFormatFactory.forFileName(file.getName());
        if (af != null) return Files.loadArchiveFile(file, af);

        // Compressed file?
        CompressionFormat cf = CompressionFormatFactory.forFileName(file.getName());
        if (cf != null) return Files.loadCompressedFile(file, cf);

        // Plain file.
        return Files.loadPlainFile(file);
    }

    /** @return Pairs of name and contents (iff {@code is} is an archive, or the plain text in the file */
    private static Object
    load(InputStream is) throws IOException, ArchiveException, CompressorException {

        if (!is.markSupported()) is = new BufferedInputStream(is);

        // Archive?
        ArchiveFormat af = ArchiveFormatFactory.forContents(is);
        if (af != null) return Files.loadArchive(af.archiveInputStream(is));

        // Compressed contents?
        CompressionFormat cf = CompressionFormatFactory.forContents(is);
        if (cf != null) return Files.load(cf.compressorInputStream(is));

        // Load plain contents.
        return Files.loadReader(new InputStreamReader(is));
    }

    /** @return The entries of the given archive file */
    private static Object[]
    loadArchiveFile(File archiveFile, ArchiveFormat af) throws IOException, ArchiveException, CompressorException {
        ArchiveInputStream ais = af.open(archiveFile);
        try {
            Object[] entries = Files.loadArchive(ais);
            ais.close();
            return entries;
        } finally {
            try { ais.close(); } catch (Exception e) {}
        }
    }

    /** @return The contents of the compressed file */
    private static Object
    loadCompressedFile(File file, CompressionFormat cf)
    throws IOException, ArchiveException, CompressorException  {
        InputStream is = new FileInputStream(file);
        try {
            CompressorInputStream cis = cf.compressorInputStream(is);

            Object contents = Files.load(cis);

            cis.close();

            return contents;
        } finally {
            try { is.close(); } catch (Exception e) {}
        }
    }

    /** @return Pairs of name an contents */
    private static Object[]
    loadArchive(ArchiveInputStream ais) throws IOException, ArchiveException, CompressorException {

        List<Object> result = new ArrayList<Object>();
        for (ArchiveEntry ae = ais.getNextEntry(); ae != null; ae = ais.getNextEntry()) {
            result.add(ae.getName());
            result.add(Files.load(ais));
        }

        return result.toArray();
    }

    /**
     * @return {@code null} if {@code this} and {@code that} are equal, or a human-readable text that describes the
     *         first difference between {@code this} and {@code that}
     */
    @Nullable public String
    diff(Files that) {
        return Files.diff("", this.desc, that.desc);
    }

    /**
     * @param name Is included in the return value
     * @return     {@code null} if {@code desc1} and {@code desc2} are equal, or a human-readable text that describes
     *             the first difference between {@code desc1} and {@code desc2}
     */
    @Nullable public static String
    diff(String name, Object desc1, Object desc2) {
        if (desc1 instanceof String) {
            if (desc2 instanceof String) {
                if (desc1.equals(desc2)) {
                    return null;
                }
                return (
                    name
                    + ": Contents should be '"
                    + ((String) desc1).replace('\n', '|')
                    + "', not '"
                    + ((String) desc2).replace('\n', '|')
                    + "'"
                );
            }
            return name + " should be a plain member / ZIP entry";
        }
        if (desc2 instanceof String) {
            return name + " should be a directory or ZIP file";
        }

        name += name.endsWith(".zip") ? '!' : '/';
        SortedMap<String, Object> entries1 = new TreeMap<String, Object>();
        {
            Object[] oa = (Object[]) desc1;
            for (int i = 0; i < oa.length;) {
                entries1.put((String) oa[i++], oa[i++]);
            }
        }
        SortedMap<String, Object> entries2 = new TreeMap<String, Object>();
        {
            Object[] oa = (Object[]) desc2;
            for (int i = 0; i < oa.length;) {
                entries2.put((String) oa[i++], oa[i++]);
            }
        }
        for (
            Iterator<Entry<String, Object>> it1 = entries1.entrySet().iterator(), it2 = entries2.entrySet().iterator();
            ;
        ) {
            if (it1.hasNext()) {
                Entry<String, Object> entry1 = it1.next();
                if (it2.hasNext()) {
                    Entry<String, Object> entry2 = it2.next();
                    String                name1  = entry1.getKey();
                    String                name2  = entry2.getKey();
                    int                   cmp    = name1.compareTo(name2);
                    if (cmp < 0) return name + name1 + " missing";
                    if (cmp > 0) return "Unexpected " + name + name2;
                    String diff = Files.diff(name + name1, entry1.getValue(), entry2.getValue());
                    if (diff != null) return diff;
                } else {
                    return name + entry1.getKey() + " missing";
                }
            } else {
                if (it2.hasNext()) {
                    return "Unexpected " + name + it2.next().getKey();
                } else {
                    break;
                }
            }
        }
        return null;
    }

    /**
     * Creates the directory tree in the file system.
     */
    public void
    save(File file) throws IOException { Files.saveFile(file, this.desc); }

    private static void
    saveFile(File file, @Nullable Object desc) throws IOException {

        {
            CompressionFormat cf = CompressionFormatFactory.forFileName(file.getName());
            if (cf != null) {

                assert desc != null;

                CompressorOutputStream cos;
                try {
                    cos = cf.create(file);
                } catch (CompressorException ce) {
                    throw new IOException(ce);
                }

                Files.save(desc, cf.getUncompressedFileName(file.getName()), cos);

                cos.close();
                return;
            }
        }

        if (desc instanceof String) {
            String       contents = (String) desc;
            OutputStream os       = new FileOutputStream(file);
            try {
                Files.saveContents(contents, os);
                os.close();
            } finally {
                try { os.close(); } catch (Exception e) {}
            }
            return;
        } else
        if (desc instanceof Object[]) {
            Object[] entries = (Object[]) desc;

            ArchiveFormat af = ArchiveFormatFactory.forFileName(file.getName());
            if (af != null) {
                Files.saveArchive(entries, af, file);
            } else {
                Files.saveDir(entries, file);
            }
        } else
        if (desc == null) {
            file.mkdirs();
        } else
        {
            throw new IllegalArgumentException(String.valueOf(desc));
        }
    }

    /**
     * @param desc Pairs of member name and {@link String} (contents of plain file) or {@code Object[]}
     *             (subdirectory or ZIP file)
     */
    private static void
    saveDir(Object[] desc, File dir) throws IOException {
        dir.mkdirs();
        for (int i = 0; i < desc.length;) {
            Files.saveFile(new File(dir, (String) desc[i++]), desc[i++]);
        }
    }

    private static void
    saveArchive(Object[] entries, ArchiveFormat af, File archiveFile)
    throws FileNotFoundException, IOException {

        ArchiveOutputStream aos;
        try {
            aos = af.create(archiveFile);
        } catch (ArchiveException ae) {
            throw new IOException(ae);
        }
        try {
            Files.saveArchive(entries, af, aos);
            aos.close();
        } finally {
            try { aos.close(); } catch (Exception e) {}
        }
    }

    private static void
    save(Object desc, String fileName, OutputStream os) throws IOException {

        {
            CompressionFormat cf = CompressionFormatFactory.forFileName(fileName);
            if (cf != null) {

                CompressorOutputStream cos;
                try {
                    cos = cf.compressorOutputStream(os);
                } catch (CompressorException ce) {
                    throw new IOException(ce);
                }

                Files.save(desc, cf.getUncompressedFileName(fileName), cos);

                // Why the ... does CompressorOutputStream not declare 'finish()'?!
                try {
                    cos.getClass().getMethod("finish").invoke(cos);
                } catch (Exception e) {
                    ;
                    cos.flush();
                }

                return;
            }
        }

        if (desc instanceof String) {
            String contents = (String) desc;

            Files.saveContents(contents, os);
        } else
        if (desc instanceof Object[]) {
            final Object[] entries = (Object[]) desc;

            ArchiveFormat af = ArchiveFormatFactory.forFileName(fileName);
            if (af == null) {
                throw new IllegalArgumentException("Could not deduce archive format from file name '" + fileName + "'");
            }

            ArchiveOutputStream aos;
            try {
                aos = af.archiveOutputStream(os);
            } catch (ArchiveException ae) {
                throw new IllegalArgumentException(ae);
            }

            Files.saveArchive(entries, af, aos);
            aos.finish();
        } else
        {
            throw new IllegalArgumentException(String.valueOf(desc));
        }
    }

    private static void
    saveContents(String contents, OutputStream os) throws IOException {
        Writer w = new OutputStreamWriter(os);
        w.write(contents);
        w.flush();
    }

    /**
     * @param entries Pairs of name and {@link String} (plain ZIP entry contents) or {@code Object[]} (nested ZIP
     *                file entries)
     */
    private static void
    saveArchive(Object[] entries, ArchiveFormat archiveFormat, ArchiveOutputStream aos) throws IOException {

        for (int i = 0; i < entries.length;) {
            final String entryName = (String) entries[i++];
            final Object desc      = entries[i++];

            if (desc == null) {
                archiveFormat.writeDirectoryEntry(aos, entryName);
            } else {
                archiveFormat.writeEntry(
                    aos,
                    entryName,
                    new ConsumerWhichThrows<OutputStream, IOException>() {

                        @Override public void
                        consume(OutputStream os) throws IOException { Files.save(desc, entryName, os); }
                    }
                );
            }
        }
    }

    /**
     * @return Pairs of member name and {@link String} (contents of plain file) or {@code Object[]} (subdirectory
     *         or ZIP file)
     */
    private static Object[]
    loadDir(File dir) throws IOException, ArchiveException, CompressorException {
        File[]   members = dir.listFiles();
        Object[] oa      = new Object[2 * members.length];
        for (int i = 0; i < members.length; i++) {
            File member = members[i];
            oa[2 * i]     = member.getName();
            oa[2 * i + 1] = Files.load(member);
        }
        return oa;
    }

    /** @return The {@code file}'s contents; lines separated with '\n' */
    public static Object
    loadPlainFile(File file) throws IOException {
        Reader r = new FileReader(file);
        try {
            String text = Files.loadReader(r);
            r.close();
            return text;
        } finally {
            try { r.close(); } catch (Exception e) {}
        }
    }

    /**
     * @return The {@link Reader}'s contents; lines separated with '\n'
     */
    private static String
    loadReader(Reader r) throws IOException {
        StringBuilder sb = new StringBuilder();
        char[]        ca = new char[1024];
        for (;;) {
            int n = r.read(ca);
            if (n == -1) break;
            sb.append(ca, 0, n);
        }
        return sb.toString();
    }
}
