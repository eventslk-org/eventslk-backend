# event-registration-api

Spring Boot backend for an event registration and booking system with REST APIs for managing events, attendees, bookings, and admin operations. The repository also includes frontend sub-projects for both clients and administrators.

## 🚀 Technologies Used

- **Java & Spring Boot**
- **Security:** Spring Security & JWT (JSON Web Tokens)
- **Database:** MySQL (via Spring Data JPA)
- **Messaging & Notifications:** Apache Kafka, Spring Boot Mail
- **Utilities:** Lombok, DotEnv

## 📁 Project Structure

- `src/main/java/com/event_registration/lk/`: Main backend source code containing Controllers, Services, Repositories, Entities, and DTOs.
- `admin-frontend/`: HTML/CSS/JS files for the administrator dashboard (managing events, users, etc.).
- `client-frontend/`: HTML/CSS/JS files for the client-facing event registration platform.
- `reports/`: Contains analysis and improvement plans for the system.

## 🛠️ Setup & Running Instructions

1. **Prerequisites**: Ensure you have Java 17+ and Maven installed. A running MySQL server and Kafka broker is required according to the dependencies.
2. **Environment Variables**: Create a `.env` file or configure your `application.yml` and `application-dev.yml` with your database credentials, Kafka details, and JWT secrets.
3. **Run the Backend**:
   Use the provided run scripts depending on your OS:
   - Linux/Mac: `./run.sh`
   - Windows: `./run.cmd`
   
   Alternatively, use Maven:
   ```bash
   mvn spring-boot:run
   ```
4. **Access the Application**:
   - The backend runs on `http://localhost:8080`.
   - To use the frontends, open `admin-frontend/index.html` or `client-frontend/index.html` in your browser.

---

## 🔌 API Documentation

The backend exposes several RESTful endpoints to manage authentication, users, events, and bookings.

### 1. Authentication (`/auth`)
Handles user registration and login endpoints utilizing JWT for security.

| Method | Endpoint      | Description                                                          |
|--------|---------------|----------------------------------------------------------------------|
| `POST` | `/auth/signup`| Registers a new user. Expects a `User` object.                       |
| `POST` | `/auth/login` | Authenticates a user. Expects `email` and `password`. Returns JWT.   |
| `GET`  | `/auth`       | Health check endpoint for auth context.                              |

### 2. Events (`/event`)
Handles CRUD operations for events in the system.

| Method   | Endpoint           | Description                                                        |
|----------|--------------------|--------------------------------------------------------------------|
| `GET`    | `/event`           | Retrieves a list of all scheduled events.                          |
| `POST`   | `/event`           | Creates a new event. Expects an `Event` object.                    |
| `PUT`    | `/event`           | Updates an existing event. Expects an updated `Event` object.      |
| `DELETE` | `/event/{eventId}` | Deletes a specific event from the system by ID.                    |

### 3. Bookings (`/book`)
Manages event ticket bookings and cancellations.

| Method   | Endpoint             | Description                                                            |
|----------|----------------------|------------------------------------------------------------------------|
| `POST`   | `/book`              | Books an event for a user. Expects a `BookingRequest` object.          |
| `GET`    | `/book/user/{userId}`| Retrieves complete booking details and history for a specific user ID. |
| `DELETE` | `/book/{bookingId}`  | Cancels an existing booking by its ID.                                 |

### 4. Users (`/user`)
Handles user-specific account operations and administration retrieval tasks.

| Method   | Endpoint              | Description                                                          |
|----------|-----------------------|----------------------------------------------------------------------|
| `GET`    | `/user`               | Retrieves a list of all registered users (Admin typically).          |
| `GET`    | `/user/email/{email}` | Retrieves full user details associated with a specific email address.|
| `PUT`    | `/user/{userId}`      | Updates a user's details. Expects an updated `User` object.          |
| `DELETE` | `/user/{userId}`      | Deletes a user account by their user ID.                             |
| `GET`    | `/user/hello`         | Simple testing endpoint returning a greeting with current date/time. |

