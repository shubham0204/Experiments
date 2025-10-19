# Blogpost-app Backend with FastAPI

## Setup

### Cloning the repository

```commandline
$ git clone --depth=1 https://github.com/shubham0204/blogpost-backend
```

Deploy the backend with [Docker Compose]() or [locally]()

#### Docker Compose

```commandline
$ cd blogpost-backend
$ docker compose up
```

#### Local Setup

```commandline
$ pip install -r requirements.txt
$ python main.py
```

## Features

### Performance

- [ ] HTTP Header Caching with Redis

### Authentication

- [x] [JWT authentication](https://dev.to/kimmaida/signing-and-validating-json-web-tokens-jwt-for-everyone-25fb) on (most) endpoints

### Database

- [ ] Add indexes for faster retrieval of blogs

### Security

- [x] [Databases use UUIDs as primary keys](https://www.mysqltutorial.org/mysql-uuid/), instead of consecutive integers to
 avoid incremental access of entities if a table is compromised in an attack
- [x] [All random UUIDs are generated with cryptographically safe](https://crypto.stackexchange.com/questions/39186/what-does-it-mean-for-a-random-number-generator-to-be-cryptographically-secure)
RNGs i.e. with `os.urandom`
- [x] Rate limiters are installed to each endpoint to prevent 
brute-force and DDoS attacks

### Deployment

- [x] Docker Compose based deployment
- [ ] Descriptive logs for FastAPI endpoints
- [ ] Host FastAPI docs on GitHub Pages
- [ ] Export Postman collection
- [ ] Simulate backend with Postman Runs
