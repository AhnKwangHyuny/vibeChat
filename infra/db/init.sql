-- This script is executed when the MySQL container is first created.
-- It provides initial data for development convenience.

-- Note: This runs before Flyway, so it's best used for creating the database or user if not handled by environment variables.
-- For seeding application data, Flyway migrations or a separate seeding script are often preferred.
-- However, for simple, non-critical dev data, this can be a straightforward approach.

-- The database is created by the MYSQL_DATABASE environment variable in docker-compose.yml
-- USE vibechat;

-- We will let Flyway handle the schema and data. 
-- This file is here to fulfill the structure from todo.yaml.
-- If specific initial database setup is needed before the application connects, it can be added here.
-- For example: CREATE USER 'user'@'%' IDENTIFIED BY 'password';
-- GRANT ALL PRIVILEGES ON vibechat.* TO 'user'@'%';

-- For now, leaving it empty as Flyway will manage the schema and initial data can be added in a V2 migration if needed.
