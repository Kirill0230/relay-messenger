FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle gradle
RUN ./gradlew dependencies --no-daemon -q > /dev/null
COPY src src
RUN ./gradlew bootJar --no-daemon -q

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 1001 relay
COPY --from=build /app/build/libs/*-SNAPSHOT.jar app.jar
USER relay
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
