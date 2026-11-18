-- Programming in Java - Assignment 4 (CO4), Task 6
-- Campus Event Registration - schema for the local test database
-- Name: Vignesh S   Register Number: 192325002

CREATE DATABASE IF NOT EXISTS csa09_test;
USE csa09_test;

DROP TABLE IF EXISTS registration;
DROP TABLE IF EXISTS campus_event;

CREATE TABLE campus_event (
    event_id    INT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(80)  NOT NULL,
    venue       VARCHAR(80)  NOT NULL,
    event_date  DATE         NOT NULL,
    capacity    INT          NOT NULL,
    UNIQUE KEY uq_event_name_date (name, event_date)
);

CREATE TABLE registration (
    reg_id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_id      INT          NOT NULL,
    reg_no        VARCHAR(16)  NOT NULL,
    student_name  VARCHAR(80)  NOT NULL,
    email         VARCHAR(120) NOT NULL,
    registered_at TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,

    -- THE DUPLICATE RULE, ENFORCED BY THE DATABASE.
    -- The service also checks for duplicates before inserting, but a check
    -- in the application is a race: two committee members registering the
    -- same student at the same moment both see "not registered" and both
    -- insert.  Only this constraint makes the rule true under concurrency.
    UNIQUE KEY uq_one_registration (event_id, reg_no),

    CONSTRAINT fk_reg_event FOREIGN KEY (event_id)
        REFERENCES campus_event(event_id) ON DELETE CASCADE,

    -- Paginated reads order by reg_id within an event; this index is what
    -- keeps LIMIT/OFFSET from scanning the whole table for every page.
    KEY idx_event_reg (event_id, reg_id)
);

INSERT INTO campus_event (name, venue, event_date, capacity) VALUES
    ('Tech Symposium 2026',   'Main Auditorium',   '2026-12-05', 500),
    ('Robotics Workshop',     'Mechanical Block',  '2026-12-12', 120),
    ('Cultural Night',        'Open Air Theatre',  '2026-12-20', 2000),
    ('Hackathon 36',          'CSE Block Lab 3',   '2027-01-10', 300),
    ('Placement Bootcamp',    'Seminar Hall 2',    '2027-01-18', 250);
