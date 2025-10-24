package io.shubham0204;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PgClient {

    private DataInputStream socketInputStream;
    private DataOutputStream socketOutputStream;
    private Socket connectionSocket;
    private final Logger logger = Logger.getLogger(PgClient.class.getName());

    public PgClient() {
    }

    /**
     *
     * Connects to the given Postgres DB at host:port
     *
     * @param host host-name of the Postgres DB server
     * @param port port of the Postgres DB server
     */
    public void connect(String host, int port) {
        try {
            this.connectionSocket = new Socket(host, port);
            this.socketInputStream = new DataInputStream(connectionSocket.getInputStream());
            this.socketOutputStream = new DataOutputStream(connectionSocket.getOutputStream());
            logger.log(
                    Level.INFO,
                    "connected to Postgres DB server on host %s and port %d".formatted(host, port));
        } catch (IOException e) {
            throw new RuntimeException("I/O failed when running connect(): %s".formatted(e.getMessage()));
        }
    }

    /**
     *
     * Authenticate against the DB with given credentials
     * using MD5 user/password method.
     *
     * @param database Name of the database to connect
     * @param user User name
     * @param password Password (plain-text)
     */
    public void authenticate(String database, String user, String password) {
        sendStartupMessage(database, user);
        PgMessage authRequestMessage = PgMessage.read(this.socketInputStream);
        sendMD5AuthMessage(authRequestMessage, user, password);
        PgMessage authOkMessage = PgMessage.read(this.socketInputStream);
        logger.log(Level.INFO, "AuthenticationOk message received");

        while (true) {
            PgMessage msg = PgMessage.read(this.socketInputStream);
            if (msg.type() == PgMessageType.READY_FOR_QUERY) {
                logger.log(Level.INFO, "READY_FOR_QUERY received, authentication complete");
                break;
            }
            logger.log(Level.FINE, "Discarding message type: " + msg.type());
        }
    }

    /**
     *
     * Executes the given query with the database.
     *
     * @param query Postgres SQL query to be executed
     */
    public void executeQuery(String query) {
        buildQueryMessage(query).write(this.socketOutputStream);
        while (true) {
            PgMessage msg = PgMessage.read(this.socketInputStream);
            if (msg.type() == PgMessageType.ROW_DESC) {
                handleRowDescMessage(msg);
            } else if (msg.type() == PgMessageType.ROW_DATA) {
                handleRowDataMessage(msg);
            } else if (msg.type() == PgMessageType.COMMAND_COMPLETE) {
                System.out.println("\n" + new String(msg.data()));
            } else if (msg.type() == PgMessageType.READY_FOR_QUERY) {
                break;
            } else if (msg.type() == PgMessageType.ERROR) {
                System.out.println("Query error: " + new String(msg.data()));
                break;
            }
        }
    }

    public void close() {
        try {
            this.connectionSocket.close();
        } catch (IOException e) {
            throw new RuntimeException("I/O error in close(): %s".formatted(e.getMessage()));
        }
    }

    /**
     * See <a href="https://www.postgresql.org/docs/current/protocol-message-formats.html#PROTOCOL-MESSAGE-FORMATS-STARTUPMESSAGE">StartupMessage</a> message format.
     */
    private void sendStartupMessage(String database, String user) {
        try {
            ByteBuffer messageBuffer = ByteBuffer.allocate(256);
            messageBuffer.putInt(0);
            messageBuffer.putInt(0x00030000);
            CStringUtils.putCString(messageBuffer, "user");
            CStringUtils.putCString(messageBuffer, user);
            CStringUtils.putCString(messageBuffer, "database");
            CStringUtils.putCString(messageBuffer, database);
            messageBuffer.put((byte) 0);
            int messageLength = messageBuffer.position();
            messageBuffer.putInt(0, messageLength);
            this.socketOutputStream.write(messageBuffer.array(), 0, messageLength);
            this.socketOutputStream.flush();
            logger.log(Level.INFO, "startup message sent");
        } catch (IOException e) {
            throw new RuntimeException(
                    "I/O failed in sendStartupMessage(): %s".formatted(e.getMessage()));
        }
    }

    /**
     * See <a href="https://www.postgresql.org/docs/current/protocol-message-formats.html#PROTOCOL-MESSAGE-FORMATS-AUTHENTICATIONMD5PASSWORD">AuthenticationMD5Password</a> message format.
     */
    private PgMessage buildPasswordMessage(byte[] salt, String user, String password) {
        byte[] md5Response = pgMd5Auth(user, password, salt);
        ByteBuffer pwMsg = ByteBuffer.allocate(64);
        pwMsg.put("md5".getBytes());
        pwMsg.put(bytesToHex(md5Response).getBytes());
        pwMsg.put((byte) 0);
        byte[] passwordMessage = new byte[pwMsg.position()];
        pwMsg.position(0);
        pwMsg.get(passwordMessage);
        return new PgMessage(PgMessageType.PASSWORD_RESPONSE, passwordMessage);
    }

    /**
     * See <a href="https://www.postgresql.org/docs/current/protocol-message-formats.html#PROTOCOL-MESSAGE-FORMATS-QUERY">Query</a> message format.
     */
    private PgMessage buildQueryMessage(String query) {
        byte[] queryBytes = query.getBytes();
        byte[] withNull = new byte[queryBytes.length + 1];
        System.arraycopy(queryBytes, 0, withNull, 0, queryBytes.length);
        withNull[queryBytes.length] = 0;
        return new PgMessage(PgMessageType.QUERY, withNull);
    }

    /**
     * See <a href="https://www.postgresql.org/docs/current/protocol-message-formats.html#PROTOCOL-MESSAGE-FORMATS-ROWDESCRIPTION">RowDescription</a> message format.
     */
    private void handleRowDescMessage(PgMessage message) {
        var buf = ByteBuffer.wrap(message.data());
        int columnCount = buf.getShort();

        System.out.println("Columns: " + columnCount);

        for (int i = 0; i < columnCount; i++) {
            String colName = CStringUtils.getCString(buf);
            System.out.printf("%-20s", colName);
            buf.position(buf.position() + 18); // Skip field info
        }
        System.out.println();
        for (int i = 0; i < columnCount * 20; i++) System.out.print("-");
        System.out.println();
    }

    /**
     * See <a href="https://www.postgresql.org/docs/current/protocol-message-formats.html#PROTOCOL-MESSAGE-FORMATS-DATAROW">DataRow</a> message format.
     */
    private void handleRowDataMessage(PgMessage message) {
        var buf = ByteBuffer.wrap(message.data());
        int fieldCount = buf.getShort();

        for (int i = 0; i < fieldCount; i++) {
            int fieldLen = buf.getInt();
            if (fieldLen == -1) {
                System.out.printf("%-20s", "NULL");
            } else {
                byte[] fieldValue = new byte[fieldLen];
                buf.get(fieldValue);
                System.out.printf("%-20s", new String(fieldValue));
            }
        }
        System.out.println();
    }

    private void sendMD5AuthMessage(PgMessage authRequestMessage, String user, String password) {
        logger.log(Level.INFO, "AuthenticationMD5Password message received");
        byte[] salt = new byte[4];
        System.arraycopy(authRequestMessage.data(), 4, salt, 0, 4);
        var passwordMessage = buildPasswordMessage(salt, user, password);
        passwordMessage.write(this.socketOutputStream);
    }

    private byte[] pgMd5Auth(String user, String password, byte[] salt) {
        MessageDigest md5;
        try {
            md5 = MessageDigest.getInstance("MD5");
            // MD5(password + user)
            md5.update(password.getBytes());
            md5.update(user.getBytes());
            byte[] md5Hash = md5.digest();
            // Convert to hex
            String hexMd5 = bytesToHex(md5Hash);
            // MD5(hex_md5 + salt)
            md5.reset();
            md5.update(hexMd5.getBytes());
            md5.update(salt);
            return md5.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm not found by MessageDigest.getInstance()");
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
