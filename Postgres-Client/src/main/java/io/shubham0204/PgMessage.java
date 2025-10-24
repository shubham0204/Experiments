package io.shubham0204;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public record PgMessage(PgMessageType type, byte[] data) {

    public static PgMessage read(DataInputStream inputStream) {
        try {
            byte type = inputStream.readByte();
            int length = inputStream.readInt() - 4;
            byte[] data = new byte[length];
            inputStream.readFully(data);
            return new PgMessage(mapRawMessageType(type), data);
        } catch (IOException e) {
            throw new RuntimeException("I/O failed in read(): %s".formatted(e.getMessage()));
        }
    }

    public void write(DataOutputStream outputStream) {
        try {
            outputStream.writeByte((byte) mapPgMessageType(type));
            outputStream.writeInt(4 + data.length);
            outputStream.write(data);
            outputStream.flush();
        } catch (IOException e) {
            throw new RuntimeException("I/O failed in write(): %s".formatted(e.getMessage()));
        }
    }

    private static char mapPgMessageType(PgMessageType type) {
        return switch (type) {
            case AUTHENTICATION_REQUEST -> 'R';
            case ERROR -> 'E';
            case BACKEND_KEY_DATA -> 'K';
            case READY_FOR_QUERY -> 'Z';
            case NOTICE -> 'N';
            case PARAMETER_STATUS -> 'S';
            case ROW_DESC -> 'T';
            case ROW_DATA -> 'D';
            case PASSWORD_RESPONSE -> 'p';
            case QUERY -> 'Q';
            case CLOSE -> 'C';
            default -> throw new RuntimeException(
                    "Could not parse raw message type from PgMessageType received: %s".formatted(type));
        };
    }

    private static PgMessageType mapRawMessageType(byte rawMessageType) {
        return switch ((char) rawMessageType) {
            case 'R' -> PgMessageType.AUTHENTICATION_REQUEST;
            case 'E' -> PgMessageType.ERROR;
            case 'K' -> PgMessageType.BACKEND_KEY_DATA;
            case 'Z' -> PgMessageType.READY_FOR_QUERY;
            case 'N' -> PgMessageType.NOTICE;
            case 'S' -> PgMessageType.PARAMETER_STATUS;
            case 'T' -> PgMessageType.ROW_DESC;
            case 'D' -> PgMessageType.ROW_DATA;
            case 'C' -> PgMessageType.CLOSE;
            default -> throw new RuntimeException(
                    "Could not parse PgMessageType from raw message type received: %d"
                            .formatted(rawMessageType));
        };
    }
}
