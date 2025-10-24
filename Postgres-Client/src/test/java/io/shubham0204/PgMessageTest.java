package io.shubham0204;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import org.junit.jupiter.api.Test;

class PgMessageTest {

    @Test
    void read_validMessage_works() throws IOException {
        var buffer = ByteBuffer.allocate(33);
        buffer.put((byte) 'R');
        buffer.putInt(32);
        buffer.put(new byte[28]);
        var byteInputStream = new ByteArrayInputStream(buffer.array());
        var inputStream = new DataInputStream(byteInputStream);

        var message = PgMessage.read(inputStream);
        assertEquals(PgMessageType.AUTHENTICATION_REQUEST, message.type());
        assertEquals(28, message.data().length);

        inputStream.close();
    }

    @Test
    void read_invalidMessageType_throws() throws IOException {
        var buffer = ByteBuffer.allocate(33);
        buffer.put((byte) '\0'); // invalid message type
        buffer.putInt(32);
        buffer.put(new byte[28]);
        var byteInputStream = new ByteArrayInputStream(buffer.array());
        var inputStream = new DataInputStream(byteInputStream);

        assertThrowsExactly(
                RuntimeException.class,
                () -> {
                    PgMessage.read(inputStream);
                },
                "Could not parse PgMessageType from raw message type received");

        inputStream.close();
    }
}
