# Staff Booking App — Java/JAX-RS + PostgreSQL + Heroku

A small booking application using one shared PostgreSQL database.

## Architecture

- `staff`: login credentials and display name.
- `schedule_slot`: bookable slots, partitioned by `staff_id`.
- Public users can view slots and book with name + phone.
- Staff authenticate with HTTP Basic Auth.
- Staff can add/remove open slots and cancel bookings.
- Booking uses a PostgreSQL transaction with `SELECT ... FOR UPDATE` so two users cannot book the same slot at the same time.
- Passwords are stored as BCrypt hashes, never plaintext.

## API

Public:
- `GET /api/staff`
- `GET /api/staff/{staffId}/slots?date=YYYY-MM-DD`
- `POST /api/bookings`

Staff Basic Auth:
- `GET /api/staff/me`
- `POST /api/staff/slots`
- `DELETE /api/staff/slots/{slotId}`
- `POST /api/staff/bookings/{bookingId}/cancel`

## Local Windows setup

Requirements:
- JDK 17
- Maven
- PostgreSQL

Create a database, then set:

```bat
set DATABASE_URL=postgres://postgres:YOUR_PASSWORD@localhost:5432/staff_booking
set STAFF_USERNAME=wife
set STAFF_PASSWORD=ChangeThisPassword
set STAFF_DISPLAY_NAME=Your Wife
```

Build/run:

```bat
mvn clean package
java -jar target\staff-booking-app-jar-with-dependencies.jar
```

Open `http://localhost:8080/`.

## Heroku

Heroku's Java buildpack supports Maven/Java apps and supplies `JDBC_DATABASE_URL` when Postgres is attached. The app uses that variable automatically.

```bat
heroku login
heroku create YOUR-APP-NAME
heroku addons:create heroku-postgresql:essential-0
heroku config:set STAFF_USERNAME=wife
heroku config:set STAFF_PASSWORD="USE-A-LONG-RANDOM-PASSWORD"
heroku config:set STAFF_DISPLAY_NAME="Wife"
git init
git add .
git commit -m "Initial staff booking app"
git push heroku main
heroku open
```

The application initializes the tables on startup. On first startup, if `STAFF_USERNAME`, `STAFF_PASSWORD`, and `STAFF_DISPLAY_NAME` are set, it creates the first staff account if it does not already exist.

For a real deployment, change the generated credentials immediately and keep them only in Heroku Config Vars.

## Adding another staff member

Generate a BCrypt password hash or use the application's database migration/admin tooling before adding additional staff. For the simplest controlled setup, add rows directly to Postgres after generating a BCrypt hash.

Example schema row:

```sql
INSERT INTO staff(username, password_hash, display_name)
VALUES ('staff2', '$2a$10$...', 'Staff Two');
```

Do not store a plaintext password in `password_hash`.
