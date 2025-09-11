#!/bin/bash
# This script provides an example of how to seed the database with sample data while the application is running.

# Docker container name for MySQL, as defined in docker-compose.yml
CONTAINER_NAME="vibechat-mysql-1"

# Check if the container is running
if [ ! "$(docker ps -q -f name=${CONTAINER_NAME})" ]; then
    echo "Error: MySQL container (${CONTAINER_NAME}) is not running."
    echo "Please start the services with 'docker-compose up' first."
    exit 1
fi

echo "Seeding database with sample data..."

# Example: Insert a sample user and a public chat room.
# Note: Use environment variables for credentials in a real-world scenario.
DB_USER="your-db-user"
DB_PASS="your-db-password"
DB_NAME="vibechat"

# SQL commands to execute
SQL_COMMANDS="
INSERT INTO users (provider, nickname, created_at) VALUES ('GUEST', 'Alice', NOW());
INSERT INTO users (provider, nickname, created_at) VALUES ('GUEST', 'Bob', NOW());

INSERT INTO tags (name, popularity) VALUES ('spring-boot', 10);
INSERT INTO tags (name, popularity) VALUES ('react', 20);
INSERT INTO tags (name, popularity) VALUES ('docker', 15);

INSERT INTO chat_rooms (title, description, is_private, created_by, created_at) VALUES ('Spring Boot Discussion', 'A room to discuss all things Spring Boot', false, 1, NOW());

INSERT INTO room_tags (room_id, tag_id) VALUES (1, 1);
"

# Execute the SQL commands inside the container
docker exec -i ${CONTAINER_NAME} mysql -u${DB_USER} -p${DB_PASS} ${DB_NAME} <<EOF
${SQL_COMMANDS}
EOF

echo "Database seeding complete."
