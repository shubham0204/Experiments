package io.shubham0204;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.ByteBuffer;
import org.junit.jupiter.api.Test;

public class CStringUtilsTest {

    @Test
    public void getCString_works() {
        var message = "HelloCString";
        var data = "%s\0".formatted(message).getBytes();
        var buffer = ByteBuffer.wrap(data);
        var parsedMessage = CStringUtils.getCString(buffer);
        assertEquals(message, parsedMessage);
    }

    @Test
    public void putCString_works() {
        var message = "Hello";
        var buffer = ByteBuffer.allocate(10);
        CStringUtils.putCString(buffer, message);
        var data = buffer.array();
        assertEquals((byte) 'H', data[0]);
        assertEquals((byte) 'e', data[1]);
        assertEquals((byte) 'l', data[2]);
        assertEquals((byte) 'l', data[3]);
        assertEquals((byte) 'o', data[4]);
        assertEquals((byte) '\0', data[5]);
    }
}
