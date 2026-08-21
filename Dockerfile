# ============================================================
# Build Stage
# ============================================================

FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /app

COPY pom.xml .

RUN mvn dependency:go-offline -B

COPY src ./src

RUN mvn clean package -DskipTests


# ============================================================
# Runtime Stage
# ============================================================

FROM eclipse-temurin:17-jre

WORKDIR /app

#COPY --from=build /app/target/ai-servicenow-1.0.0-SNAPSHOT.jar app.jar
COPY --from=build /app/target/*.jar app.jar

# Application port
EXPOSE 8080

# JVM configuration
ENV JAVA_OPTS=""

#ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
ENTRYPOINT ["sh", "-c", "exec java ${JAVA_OPTS} -jar app.jar"]
