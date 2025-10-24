package io.shubham0204;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

public class CStringUtils {

    public static String getCString(ByteBuffer buf) {
        var baos = new ByteArrayOutputStream();
        byte b;
        while ((b = buf.get()) != 0) {
            baos.write(b);
        }
        return baos.toString();
    }

    public static void putCString(ByteBuffer buf, String str) {
        buf.put(str.getBytes());
        buf.put((byte) 0);
    }
}
