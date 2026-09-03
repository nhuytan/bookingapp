CREATE TABLE IF NOT EXISTS staff (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    display_name VARCHAR(150) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    is_admin BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS service (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    duration_minutes INT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS schedule_slot (
    id BIGSERIAL PRIMARY KEY,
    staff_id BIGINT NOT NULL REFERENCES staff(id) ON DELETE CASCADE,
    service_id BIGINT REFERENCES service(id),
    slot_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    status VARCHAR(10) NOT NULL DEFAULT 'open',
    customer_name VARCHAR(150),
    customer_phone VARCHAR(40),
    booked_at TIMESTAMPTZ,
    CONSTRAINT schedule_slot_status_ck CHECK (status IN ('open', 'booked', 'blocked')),
    CONSTRAINT schedule_slot_time_ck CHECK (end_time > start_time)
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_staff_slot_time
ON schedule_slot(staff_id, slot_date, start_time);

CREATE INDEX IF NOT EXISTS ix_slot_staff_date
ON schedule_slot(staff_id, slot_date);

CREATE INDEX IF NOT EXISTS ix_slot_status
ON schedule_slot(status);

-- Safe migrations: run every startup, no-op if already applied.
ALTER TABLE schedule_slot ADD COLUMN IF NOT EXISTS service_id BIGINT REFERENCES service(id);

ALTER TABLE schedule_slot DROP CONSTRAINT IF EXISTS schedule_slot_status_ck;
ALTER TABLE schedule_slot ADD CONSTRAINT schedule_slot_status_ck CHECK (status IN ('open', 'booked', 'blocked'));

ALTER TABLE staff ADD COLUMN IF NOT EXISTS is_admin BOOLEAN NOT NULL DEFAULT FALSE;
