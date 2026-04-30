FROM eclipse-temurin:21-jdk AS builder

WORKDIR /app

COPY user-service/gradlew user-service/
COPY user-service/gradle/ user-service/gradle/
COPY user-service/build.gradle.kts user-service/gradle.properties user-service/settings.gradle.kts user-service/

COPY shared-events/build.gradle.kts shared-events/settings.gradle.kts shared-events/
COPY shared-events/src/ shared-events/src/

RUN cd user-service && ./gradlew dependencies --no-daemon 2>/dev/null || true

COPY user-service/src/ user-service/src/

RUN cd user-service && ./gradlew buildFatJar --no-daemon

FROM gcr.io/distroless/java21-debian13 AS runner

WORKDIR /app

COPY --from=builder /app/user-service/build/libs/*-all.jar app.jar

USER nonroot:nonroot

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]