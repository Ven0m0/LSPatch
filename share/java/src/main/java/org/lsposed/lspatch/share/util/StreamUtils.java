package org.lsposed.lspatch.share.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class StreamUtils {
    
    private static final int DEFAULT_BUFFER_SIZE = 8192;
    
    /**
     * Transfers all bytes from input stream to output stream using a buffer
     * @param is Input stream to read from
     * @param os Output stream to write to
     * @throws IOException if an I/O error occurs
     */
    public static void transfer(InputStream is, OutputStream os) throws IOException {
        transfer(is, os, DEFAULT_BUFFER_SIZE);
    }
    
    /**
     * Transfers all bytes from input stream to output stream using a specified buffer size
     * @param is Input stream to read from
     * @param os Output stream to write to
     * @param bufferSize Size of the buffer to use for transfer
     * @throws IOException if an I/O error occurs
     */
    public static void transfer(InputStream is, OutputStream os, int bufferSize) throws IOException {
        byte[] buffer = new byte[bufferSize];
        int n;
        while (-1 != (n = is.read(buffer))) {
            os.write(buffer, 0, n);
        }
    }
}