package com.alphnology.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Utility class for creating ZIP archives in memory.
 *
 * @author me@fredpena.dev
 * @created 24/10/2025  - 13:54
 */
public class ZipUtils {

    private ZipUtils() {
    }

    /**
     * Creates a ZIP archive from a map of filenames and their byte content.
     *
     * @param files A map where the key is the filename (e.g., "image.png") and the value is the file content as a byte array.
     * @return A byte array representing the ZIP file.
     * @throws IOException if an I/O error occurs.
     */
    public static byte[] createZip(Map<String, byte[]> files) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(baos)) {
            for (Map.Entry<String, byte[]> fileEntry : files.entrySet()) {
                ZipEntry entry = new ZipEntry(fileEntry.getKey());
                zos.putNextEntry(entry);
                zos.write(fileEntry.getValue());
                zos.closeEntry();
            }
            zos.finish();
            return baos.toByteArray();
        }
    }
}
