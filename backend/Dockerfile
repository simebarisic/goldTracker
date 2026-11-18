# ---- stage 1: build ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml ./
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -B

# ---- stage 2: runtime ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/gold-tracker.jar ./app.jar

EXPOSE 4100
# Heap do 75% limita kontejnera (mem_limit u composeu); bez limita bi JVM uzeo
# četvrtinu RAM-a cijelog servera, a više takvih backenda zajedno ga može srušiti.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"
ENTRYPOINT ["java", "-jar", "app.jar"]
