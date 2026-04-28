FROM eclipse-temurin:21-jdk AS builder

WORKDIR /app

COPY booking-service/gradlew booking-service/
COPY booking-service/gradle/ booking-service/gradle/
COPY booking-service/build.gradle.kts booking-service/gradle.properties booking-service/settings.gradle.kts booking-service/

COPY shared-events/build.gradle.kts shared-events/settings.gradle.kts shared-events/
COPY shared-events/src/ shared-events/src/

RUN cd booking-service && ./gradlew dependencies --no-daemon 2>/dev/null || true

COPY booking-service/src/ booking-service/src/

RUN cd booking-service && ./gradlew buildFatJar --no-daemon

FROM gcr.io/distroless/java21-debian13 AS runner

WORKDIR /app

COPY --from=builder /app/booking-service/build/libs/*-all.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
