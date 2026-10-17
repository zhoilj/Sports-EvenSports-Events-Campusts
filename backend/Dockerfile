FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
ENV PORT=8080
COPY --from=build /workspace/target/sports-events-1.0.0.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java -XX:MaxRAMPercentage=75.0 -Dserver.port=${PORT:-8080} -jar /app/app.jar"]
