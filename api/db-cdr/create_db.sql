CREATE DATABASE IF NOT EXISTS ${CDR_DB_NAME} CHARACTER SET utf8 COLLATE utf8_general_ci;

GRANT SELECT, INSERT, UPDATE, DELETE, CREATE TEMPORARY TABLES ON ${CDR_DB_NAME}.* TO '${WORKBENCH_DB_USER}'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE, DROP, ALTER, CREATE, INDEX, REFERENCES, CREATE TEMPORARY TABLES, CREATE VIEW ON ${CDR_DB_NAME}.* TO '${LIQUIBASE_DB_USER}'@'%';
