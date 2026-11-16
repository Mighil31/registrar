# Build: docker build -t registrar-backend backend/
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q package -DskipTests

FROM eclipse-temurin:21-jre
RUN useradd --system --uid 10001 app
WORKDIR /app
COPY --from=build /src/target/registrar-backend-*.jar app.jar
USER app
EXPOSE 8082 9092
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
