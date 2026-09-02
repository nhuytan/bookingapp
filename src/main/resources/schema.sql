CREATE TABLE IF NOT EXISTS staff (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    display_name VARCHAR(150) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS schedule_slot (
    id BIGSERIAL PRIMARY KEY,
    staff_id BIGINT NOT NULL REFERENCES staff(id) ON DELETE CASCADE,
    slot_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    status VARCHAR(10) NOT NULL DEFAULT 'open',
    customer_name VARCHAR(150),
    customer_phone VARCHAR(40),
    booked_at TIMESTAMPTZ,
    CONSTRAINT schedule_slot_status_ck CHECK (status IN ('open', 'booked')),
    CONSTRAINT schedule_slot_time_ck CHECK (end_time > start_time)
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_staff_slot_time
ON schedule_slot(staff_id, slot_date, start_time);

CREATE INDEX IF NOT EXISTS ix_slot_staff_date
ON schedule_slot(staff_id, slot_date);

CREATE INDEX IF NOT EXISTS ix_slot_status
ON schedule_slot(status);
