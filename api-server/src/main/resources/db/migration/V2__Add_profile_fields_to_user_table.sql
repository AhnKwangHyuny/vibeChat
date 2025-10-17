-- Add columns for user profile enhancement
ALTER TABLE users
ADD COLUMN greeting VARCHAR(255),
ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'OFFLINE',
ADD COLUMN user_type VARCHAR(20) NOT NULL DEFAULT 'GUEST';
