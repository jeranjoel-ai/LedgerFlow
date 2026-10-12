-- Creates one database per service (spec: "one database per service, separate
-- schemas are acceptable locally"). Runs once on first container start.
CREATE DATABASE account_db;
CREATE DATABASE payment_db;
CREATE DATABASE ledger_db;
CREATE DATABASE notification_db;
