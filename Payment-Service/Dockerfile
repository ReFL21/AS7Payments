# Use an OpenJDK 21 image (Eclipse Temurin is a good choice)
FROM eclipse-temurin:21-jdk-alpine

# Set working directory inside the container
WORKDIR /app

# Copy the built JAR file into the container
# Adjust the path if your JAR is in a different location
COPY build/libs/*.jar app.jar

# Expose the port the application runs on (default is 8080)
EXPOSE 8080

# Define the entrypoint to run the Spring Boot application
ENTRYPOINT ["java", "-jar", "app.jar"]
