FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/moneymanager-0.0.1-SNAPSHOT.jar money-manager.jar
ENTRYPOINT ["java", "-jar", "money-manager.jar"]