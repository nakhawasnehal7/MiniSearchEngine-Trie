FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY . .
RUN mvn -q clean package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/searchengine-1.0.0.jar app.jar
COPY site ./site
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]