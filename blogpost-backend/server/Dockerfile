# syntax=docker/dockerfile:1
FROM python:3.10-slim-buster
WORKDIR /app

# Install libraries for MariaDB
RUN apt-get update -y
RUN apt-get install -y libmariadb-dev
RUN apt-get update && apt-get install -y gcc wget
RUN wget https://dlm.mariadb.com/2678574/Connectors/c/connector-c-3.3.3/mariadb-connector-c-3.3.3-debian-bullseye-amd64.tar.gz -O - | tar -zxf - --strip-components=1 -C /usr

# Install Python dependencies
RUN pip install fastapi uvicorn[standard] mariadb
COPY .. .
CMD [ "python" , "main.py" ]