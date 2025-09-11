CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    provider VARCHAR(20) NOT NULL,
    provider_id VARCHAR(128),
    nickname VARCHAR(32) NOT NULL,
    avatar_url VARCHAR(255),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE chat_rooms (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(80) NOT NULL,
    description VARCHAR(255),
    is_private BOOLEAN NOT NULL DEFAULT FALSE,
    invite_code CHAR(36) UNIQUE,
    created_by BIGINT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (created_by) REFERENCES users(id)
);

CREATE TABLE tags (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(32) NOT NULL UNIQUE,
    popularity INT DEFAULT 0
);

CREATE TABLE room_tags (
    room_id BIGINT,
    tag_id BIGINT,
    PRIMARY KEY (room_id, tag_id),
    FOREIGN KEY (room_id) REFERENCES chat_rooms(id) ON DELETE CASCADE,
    FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE
);

CREATE TABLE messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    type ENUM('TEXT', 'IMAGE', 'GIF', 'VIDEO') NOT NULL,
    content_text TEXT,
    media_url VARCHAR(255),
    media_thumb_url VARCHAR(255),
    media_duration_sec SMALLINT,
    client_temp_id VARCHAR(36),
    is_deleted BOOLEAN DEFAULT FALSE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (room_id) REFERENCES chat_rooms(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id),
    INDEX idx_messages_room_id_created_at (room_id, created_at DESC)
);

CREATE TABLE message_reports (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    message_id BIGINT NOT NULL,
    reporter_user_id BIGINT,
    reason VARCHAR(64) NOT NULL,
    details VARCHAR(255),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (message_id) REFERENCES messages(id) ON DELETE CASCADE,
    FOREIGN KEY (reporter_user_id) REFERENCES users(id) ON DELETE SET NULL
);

-- Additional indexes from TRD
CREATE INDEX idx_chat_rooms_title ON chat_rooms(title);
CREATE INDEX idx_chat_rooms_is_private_created_at ON chat_rooms(is_private, created_at DESC);
CREATE INDEX idx_tags_name ON tags(name(8)); -- Prefix index
CREATE INDEX idx_tags_popularity ON tags(popularity DESC);
CREATE INDEX idx_room_tags_tag_id ON room_tags(tag_id);
