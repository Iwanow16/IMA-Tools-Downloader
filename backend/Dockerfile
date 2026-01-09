FROM eclipse-temurin:17-jdk-jammy AS builder
WORKDIR /app
COPY backend/gradlew .
COPY backend/gradle gradle
COPY backend/build.gradle backend/settings.gradle ./
COPY backend/src ./src
RUN chmod +x gradlew && ./gradlew clean bootJar -x test --no-daemon

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar ./app.jar
COPY backend/src/main/resources ./resources

# Install minimal dependencies
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
    ffmpeg \
    curl \
    ca-certificates \
    python3-pip && \
    rm -rf /var/lib/apt/lists/*

# Install yt-dlp
RUN pip3 install --no-cache-dir yt-dlp

EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
