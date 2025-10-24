package io.shubham0204;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        var dbHost = "localhost";
        var dbPort = 5432;
        var dbName = "testdb";
        var user = "postgres";
        var password = "password";

        var client = new PgClient();
        client.connect(dbHost, dbPort);
        client.authenticate(dbName, user, password);
        client.executeQuery("SELECT * FROM users;");
    }
}
