# Multi-stage build (RECOMMENDED)
FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

# Copy Maven wrapper and project files
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY src src

# Make mvnw executable and build the application
RUN chmod +x mvnw && ./mvnw clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre
WORKDIR /app

# Copy from build stage
COPY --from=build /app/target/moneymanager-0.0.1-SNAPSHOT.jar money-manager.jar

EXPOSE 8080

# Run with the production profile and prefer IPv4 stack
CMD ["sh", "-c", "java -Dserver.port=${PORT:-8080} -Dspring.profiles.active=prod -Djava.net.preferIPv4Stack=true -jar money-manager.jar"]