-- Fresh installation, default Hibernate Envers revision mapping (Boot 4.1 / Hibernate 7.4).
-- Apply once in the same schema used by Hibernate. Do not apply over an existing REVINFO.
CREATE TABLE revinfo (
    rev integer PRIMARY KEY,
    revtstmp bigint
);
CREATE SEQUENCE revinfo_seq START WITH 1 INCREMENT BY 50;
