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

# Copy resources including cookies file
COPY backend/src/main/resources ./resources

# Install ffmpeg, yt-dlp runtime dependencies, and browsers for cookie extraction
RUN apt-get update && \
    curl -fsSL https://deb.nodesource.com/setup_20.x | bash - && \
    apt-get install -y \
    ffmpeg \
    python3-pip \
    ca-certificates \
    curl \
    firefox \
    chromium-browser \
    nodejs \
    --no-install-recommends && \
    pip3 install --no-cache-dir yt-dlp && \
    rm -rf /var/lib/apt/lists/*

# Create .wgetrc for cookie support
RUN echo "hsts = off" > ~/.wgetrc

EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
