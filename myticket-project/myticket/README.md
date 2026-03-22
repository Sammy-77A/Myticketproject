# MyTicket — Campus Event Management & Ticketing System

## Requirements
- Java 21
- Maven 3.9+
- No other installation needed (SQLite is embedded and configured automatically)

## Running the backend

**Online mode** (requires Gmail SMTP credentials, hCaptcha keys, and Daraja M-Pesa keys in `application.properties`):
```bash
mvn -pl myticket-backend spring-boot:run
```

**Offline mode** (no internet required — all external services mocked):
```bash
mvn -pl myticket-backend spring-boot:run -Dspring-boot.run.profiles=offline
```

- The backend starts on `http://localhost:8080`.
- The database file `myticket.db` is created automatically in the working directory.
- QR code images generated for tickets are saved to the `qr-codes/` directory.
- Mock emails (in offline mode) are saved as `.txt` files to the `emails/outbox/` directory.

## Running the web frontend
Open any `.html` file contained within `myticket-backend/src/main/resources/static/` in a modern browser or simply visit `http://localhost:8080/index.html` after starting the backend.

## Running the JavaFX desktop app
```bash
mvn -pl myticket-desktop javafx:run
```

## First-time setup
1. Start the backend (offline mode is recommended for demo purposes).
2. Register an account at `http://localhost:8080/register.html`.
3. Check `emails/outbox/` for the verification email, copy the token, and call the verification endpoint:
   `GET http://localhost:8080/api/auth/verify-email?token={token}`
4. Log in at `http://localhost:8080/login.html`.
5. **To create events**, promote your account to ORGANIZER:
   - The first account registered can be manually set to ADMIN by editing the `myticket.db` SQLite database using any generic SQLite client:
     `UPDATE users SET role = 'ADMIN' WHERE email = 'your@email.com';`
   - Then use the Admin panel in the JavaFX Desktop App to seamlessly promote other users to ORGANIZER status.

## Seeding demo data
Demo data is seeded automatically on first startup when the database is empty. The `DataSeeder` component runs on every launch and inserts sample categories, events, and a default Admin/Organizer/Student user set *only if no users presently exist*.

To reset and re-seed your data entirely:
1. Delete the `myticket.db` file.
2. Restart the backend.
