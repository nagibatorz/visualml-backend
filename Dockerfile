FROM eclipse-temurin:21-jdk
RUN apt-get update && apt-get install -y maven
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests
EXPOSE 8080
CMD ["java", "-Dserver.port=8080", "-jar", "target/classifierapi-0.0.1-SNAPSHOT.jar"]