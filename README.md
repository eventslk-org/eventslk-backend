# Event Registration API

Spring Boot backend for an event registration and booking system. This module provides REST APIs for authentication, event management, user management, and booking operations. It uses JWT-based security, MySQL persistence, and Spring Data JPA.

## Tech Stack

- Java 21
- Spring Boot 3.5
- Spring Security
- JWT authentication
- Spring Data JPA
- MySQL
- Lombok
- Dotenv support for local environment loading

## Features

- User signup and login with JWT token issuance
- Event create, read, update, and delete operations
- Booking creation, lookup by user, and cancellation
- User lookup, update, and deletion
- Dockerfile for containerized builds
- Local run scripts for Linux/macOS and Windows

## Project Layout

- `src/main/java/com/event_registration/lk/controller/`: REST controllers
- `src/main/java/com/event_registration/lk/service/`: service interfaces and implementations
- `src/main/java/com/event_registration/lk/repository/`: Spring Data repositories
- `src/main/java/com/event_registration/lk/dto/`: request and response objects
- `src/main/java/com/event_registration/lk/entity/`: JPA entities
- `src/main/resources/`: application configuration
- `reports/`: analysis and planning documents for the wider project

## Requirements

- JDK 21
- Maven 3.9+
- MySQL 8+

## Configuration

The application starts with the `dev` profile active by default.

Required environment variables:

- `DB_NAME`
- `DB_USERNAME`
- `DB_PASSWORD`
- `JWT_SECRET`

Optional environment variables:

- `DB_HOST` default: `localhost`
- `DB_PORT` default: `3306`

The dev profile config uses a MySQL connection string like:

`jdbc:mysql://${DB_HOST:localhost}:${DB_PORT:3306}/${DB_NAME}`

The application also loads a local `.env` file at startup if one is present.

## Run Locally

### Option 1: Maven

```bash
mvn clean package
mvn spring-boot:run
```

### Option 2: Build JAR and run

```bash
mvn clean package
java -jar target/EventRegistrationAPI-1.0-SNAPSHOT.jar
```

### Option 3: Provided scripts

- Linux/macOS: `./run.sh`
- Windows: `run.cmd`

The scripts export the required database and JWT variables before launching the JAR.

## Docker

Build the image:

```bash
docker build -t event-registration-api .
```

Run the container:

```bash
docker run --rm -p 8080:8080 \
   -e DB_NAME=event_reg_db \
   -e DB_USERNAME=root \
   -e DB_PASSWORD=mysql \
   -e JWT_SECRET=change-me \
   event-registration-api
```

## Base URL

The app runs on `http://localhost:8080` by default.

## API Overview

Security behavior in the current configuration:

- `POST /auth/**` is public
- `GET /event` is public
- `/event/**` other than `GET /event` requires the `ADMIN` role
- `/book/**` requires authentication
- `/user/**` requires authentication

### Authentication

Base path: `/auth`

| Method | Endpoint | Description |
| --- | --- | --- |
| `POST` | `/auth/signup` | Create a new user account |
| `POST` | `/auth/login` | Authenticate a user and return a JWT token |
| `GET` | `/auth` | Simple hello endpoint |

Example login payload:

```json
{
   "email": "user@example.com",
   "password": "secret123"
}
```

### Events

Base path: `/event`

| Method | Endpoint | Description |
| --- | --- | --- |
| `GET` | `/event` | List all events |
| `POST` | `/event` | Create a new event |
| `PUT` | `/event` | Update an existing event |
| `DELETE` | `/event/{eventId}` | Delete an event by ID |

Event payload fields:

- `eventId`
- `name`
- `description`
- `priceRanges` with `label` and `price`
- `dates` as a list of `LocalDateTime` values
- `location`
- `image` as bytes if required by the client

### Bookings

Base path: `/book`

| Method | Endpoint | Description |
| --- | --- | --- |
| `POST` | `/book` | Create a booking |
| `GET` | `/book/user/{userId}` | Get booking details for a user |
| `DELETE` | `/book/{bookingId}` | Cancel a booking |

Example booking payload:

```json
{
   "eventId": "E123456789",
   "userId": 1,
   "localDateTime": "2026-04-13T10:30:00"
}
```

### Users

Base path: `/user`

| Method | Endpoint | Description |
| --- | --- | --- |
| `GET` | `/user` | List all users |
| `GET` | `/user/email/{email}` | Get a user by email |
| `PUT` | `/user/{userId}` | Update a user |
| `DELETE` | `/user/{userId}` | Delete a user |
| `GET` | `/user/hello` | Test endpoint that returns the current date and time |

Example user payload:

```json
{
   "username": "Jane Doe",
   "password": "secret123",
   "email": "jane@example.com",
   "role": "USER"
}
```

## Notes

- Event IDs are generated automatically with an `E` prefix.
- Booking IDs are generated automatically with a `B` prefix.
- Ticket numbers are generated automatically when a booking is created.
- If you extend the API, keep the security rules in `SecurityConfig` aligned with the new routes.

