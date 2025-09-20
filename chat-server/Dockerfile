FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copy the Maven project files
COPY pom.xml .

# Copy the source code
COPY src ./src

# Build the application, skipping tests for faster image builds
RUN mvn clean package -DskipTests

# Stage 2: Create the final production image
FROM eclipse-temurin:21-jre

WORKDIR /app

# Install FFmpeg for video processing
RUN apt-get update && apt-get install -y ffmpeg

# Copy the built JAR file from the build stage
COPY --from=build /app/target/*.jar app.jar

# Expose the port the application runs on
EXPOSE 8081

# Command to run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
