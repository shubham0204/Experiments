# Simple Driverless Postgres Client (Type 4 Driver) in Java

> Executes the given `SELECT` query on a Postgres DB without using any JDBC/native driver

This project aims to demonstrate how drivers (types 1-4) communicate with a Postgres DB server to execute queries and display their results.

## Setup

### Postgres DB with Docker

Start a container with `postgres:latest` image:

```shell
docker run --name postgres_test \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=password \
  -e POSTGRES_DB=testdb \
  -e POSTGRES_INITDB_ARGS="-c password_encryption=md5" \
  -p 5432:5432 \
  -d postgres:latest
```

Setup MD5 password authentication, replacing the default scram-sha-256 method:

```shell
docker exec postgres_test bash -c 'sed -i "s/scram-sha-256/md5/g" /var/lib/postgresql/data/pg_hba.conf'
docker restart postgres_test
```

Create a table with dummy data:

```shell
docker exec -it postgres_test psql -U postgres -d testdb -c "
CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL,
    age INT
);
"

docker exec -it postgres_test psql -U postgres -d testdb -c "
INSERT INTO users (name, email, age) VALUES
('Alice Johnson', 'alice@example.com', 28),
('Bob Smith', 'bob@example.com', 35),
('Charlie Brown', 'charlie@example.com', 42),
('Diana Prince', 'diana@example.com', 31),
('Eve Davis', 'eve@example.com', 26);
"
```

Verify if the table `users` was populated:

```shell
docker exec -it postgres_test psql -U postgres -d testdb -c "SELECT * FROM users;"
```

Verify if MD5 authentication was enabled:

```shell
docker exec -it postgres_test psql -U postgres -d testdb -c "SHOW password_encryption;"
```

### Execute the Postgres Client

Execute `Main.java` with Maven:

```shell
mvn exec:java -Dexec.mainClass="io.shubham0204.Main"  
```

## Working

Communication with the DB is performed with the `PgClient` class. The following sequence of steps is followed to connect, authenticate and execute a query:

1. Client creates a `Socket` with given `host` and `port`.
2. When `PgClient.authenticate()` is called, a `StartupMessage` is sent to the server with `PgClient.sendStartupMessage()`.
3. The server, as a response, sends the `AuthenticationRequest` message containing the salt.
4. The client sends the `AuthenticationMD5Password` message built in `PgClient.buildPasswordMessage()` to the server containing the user name and the MD5 hashed password.
5. If the credentials are valid, server returns the `AuthenticationOk` message. `ParameterStatus` and `BackendKeyData` messages are also sent by the server, but we do not process them. Finally, the `ReadyForQuery` message indicates that the server is ready for query execution.
6. The client sends a `Query` message containing the query provided by the user.
7. The server responds with the `RowDescription` message containing information like number of rows processed, number of columns etc. The row data is returned in the `DataRow` message.
8. Completion of a single command in the query is signaled by the `CommandComplete` message. Completion of the entire query is signaled by another `ReadyForQuery` message from the server.

## References

- [Java Type 4 Drivers](https://en.wikipedia.org/wiki/JDBC_driver#Type_4_driver_–_Database-Protocol_driver/Thin_Driver_(Pure_Java_driver))
- [Postgres Message Formats](https://www.postgresql.org/docs/current/protocol-message-formats.html)
- [Postgres Message Flow](https://www.postgresql.org/docs/current/protocol-flow.html)