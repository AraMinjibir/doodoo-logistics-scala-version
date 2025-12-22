-- !Ups
CREATE TABLE users (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email_address VARCHAR(254) NOT NULL UNIQUE,
    hash_password TEXT NOT NULL,
    phone_number VARCHAR(20),
    role VARCHAR(50) NOT NULL
);

-- !Downs
DROP TABLE users;