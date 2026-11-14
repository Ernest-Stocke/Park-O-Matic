-- Park-O-Matic database schema (created automatically by JdbcRepository; kept here for reference / viva).
-- Works on SQLite and MySQL. Money is whole rupees; timestamps are ISO-8601 text; booleans are 0/1.

CREATE TABLE IF NOT EXISTS app_users (
    username      VARCHAR(50)  PRIMARY KEY,
    password_hash VARCHAR(200) NOT NULL,          -- salt:hash (PBKDF2), never the password
    role          VARCHAR(20)  NOT NULL           -- ADMIN or EMPLOYEE
);

CREATE TABLE IF NOT EXISTS vehicles (
    reg_no       VARCHAR(20)  PRIMARY KEY,        -- normalised Indian plate, e.g. MH12AB1234
    owner_name   VARCHAR(100) NOT NULL,
    vehicle_type VARCHAR(20)  NOT NULL            -- CAR, BIKE, VIP, EMERGENCY
);

CREATE TABLE IF NOT EXISTS parking_slots (
    slot_id   VARCHAR(10) PRIMARY KEY,            -- C01, B02, V01, E01 ...
    slot_type VARCHAR(20) NOT NULL,
    occupied  INT         NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS bookings (
    booking_id VARCHAR(20) PRIMARY KEY,           -- BK-001 ...
    reg_no     VARCHAR(20) NOT NULL,
    slot_id    VARCHAR(10) NOT NULL,              -- deliberately NOT a foreign key: admins may later remove the slot
    entry_time VARCHAR(30) NOT NULL,
    exit_time  VARCHAR(30),                       -- NULL while the booking is active
    FOREIGN KEY (reg_no) REFERENCES vehicles(reg_no)
);

CREATE TABLE IF NOT EXISTS bills (
    bill_id          VARCHAR(20) PRIMARY KEY,     -- BILL-001 ...
    booking_id       VARCHAR(20) NOT NULL,
    duration_minutes INT NOT NULL,
    billable_hours   INT NOT NULL,
    hourly_rate      INT NOT NULL,                -- rate at the time of billing
    total_amount     INT NOT NULL,
    paid             INT NOT NULL DEFAULT 0,
    FOREIGN KEY (booking_id) REFERENCES bookings(booking_id)
);

CREATE TABLE IF NOT EXISTS rates (
    vehicle_type VARCHAR(20) PRIMARY KEY,
    hourly_rate  INT NOT NULL
);

CREATE TABLE IF NOT EXISTS counters (             -- source of BK-/BILL- numbers
    name  VARCHAR(30) PRIMARY KEY,
    value INT NOT NULL
);
