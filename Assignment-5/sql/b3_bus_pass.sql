-- Assignment 5 (CO5) Task B3 - the bus_pass table.
-- Name: Vignesh S   Register Number: 192325002
USE csa09_test;
DROP TABLE IF EXISTS bus_pass;
CREATE TABLE bus_pass (
    pass_id       VARCHAR(16)  NOT NULL PRIMARY KEY,
    register_no   VARCHAR(16)  NOT NULL,
    name          VARCHAR(80)  NOT NULL,
    route_no      VARCHAR(8)   NOT NULL,
    boarding_stop VARCHAR(60)  NOT NULL,
    valid_until   DATE         NOT NULL,
    amount_paise  BIGINT       NOT NULL,
    KEY idx_route (route_no),
    KEY idx_register (register_no),
    CONSTRAINT chk_amount CHECK (amount_paise > 0)
);
