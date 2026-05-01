FROM eclipse-temurin:21-jdk AS builder

WORKDIR /app

COPY car-service/gradlew car-service/
COPY car-service/gradle/ car-service/gradle/
COPY car-service/build.gradle.kts car-service/gradle.properties car-service/settings.gradle.kts car-service/

COPY shared-events/build.gradle.kts shared-events/settings.gradle.kts shared-events/
COPY shared-events/src/ shared-events/src/

RUN cd car-service && ./gradlew dependencies --no-daemon 2>/dev/null || true

COPY car-service/src/ car-service/src/

RUN cd car-service && ./gradlew buildFatJar --no-daemon

FROM gcr.io/distroless/java21-debian13 AS runner

WORKDIR /app

COPY --from=builder /app/car-service/build/libs/*-all.jar app.jar

USER nonroot:nonroot

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]