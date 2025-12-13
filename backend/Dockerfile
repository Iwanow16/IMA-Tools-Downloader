FROM eclipse-temurin:17-jdk-jammy AS builder
WORKDIR /app
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./
COPY src ./src
RUN chmod +x gradlew && ./gradlew clean bootJar -x test --no-daemon

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar ./app.jar
COPY scripts /app/scripts
RUN chmod +x /app/scripts/*.sh || true

# Install ffmpeg and yt-dlp runtime dependencies
RUN apt-get update && apt-get install -y ffmpeg python3-pip ca-certificates --no-install-recommends \
    && pip3 install --no-cache-dir yt-dlp \
    && rm -rf /var/lib/apt/lists/*
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
