# syntax=docker/dockerfile:1
FROM python:3.10-slim-buster
WORKDIR /proxy

# Install Python dependencies
RUN pip install fastapi uvicorn[standard] httpx redis
COPY . .
CMD [ "python3" , "main.py" ]