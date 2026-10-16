FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /workspace
COPY gradle gradle
COPY gradlew build.gradle settings.gradle ./
RUN bash gradlew --version --no-daemon
COPY src src
RUN bash gradlew bootJar --no-daemon

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
RUN groupadd --system budgeting && useradd --system --gid budgeting budgeting \
    && mkdir /app/data && chown budgeting:budgeting /app/data
COPY --from=build /workspace/build/libs/budgeting-0.0.1-SNAPSHOT.jar app.jar
USER budgeting
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
