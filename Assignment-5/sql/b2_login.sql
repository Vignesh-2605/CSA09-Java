-- Assignment 5 (CO5) Task B2 - a LOCAL TEST database only.
-- Name: Vignesh S   Register Number: 192325002
USE csa09_test;
DROP TABLE IF EXISTS app_user;
CREATE TABLE app_user (
    user_id  INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(40)  NOT NULL UNIQUE,
    password VARCHAR(80)  NOT NULL,
    role     VARCHAR(20)  NOT NULL
);
-- Passwords are stored in clear here ONLY because the task is about SQL
-- injection and the unsafe query must be reproduced exactly as described.
-- A real system stores a salted hash; that is noted in the report.
INSERT INTO app_user (username, password, role) VALUES
    ('vignesh',  'Pass@192325002', 'STUDENT'),
    ('transport','Bus@2026',       'OFFICE'),
    ('admin',    'Adm!n#2026',     'ADMIN');
