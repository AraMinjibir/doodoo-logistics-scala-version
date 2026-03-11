-- !Ups

CREATE TABLE users(
    id UUID PRIMARY KEY,
    name TEXT NOT NULL,
    username TEXT NOT NULL,
    hash_password TEXT NOT NULL,
    phone_number TEXT,
    role TEXT NOT NULL,
    status TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE
);

CREATE UNIQUE INDEX idx_users_username_unique
    ON users(username);

CREATE INDEX idx_users_status
    ON users(status);

CREATE INDEX idx_users_role
    ON users(role);


-- !Downs

DROP INDEX IF EXISTS idx_users_username_unique;
DROP INDEX IF EXISTS idx_users_status;
DROP INDEX IF EXISTS idx_users_role;

DROP TABLE IF EXISTS users;