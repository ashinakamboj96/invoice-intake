FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN apt-get update && apt-get install -y maven
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Install Tesseract and English trained data
RUN apt-get update && apt-get install -y \
    tesseract-ocr \
    tesseract-ocr-eng \
    && rm -rf /var/lib/apt/lists/*

# Copy built jar
COPY --from=build /app/target/*.jar app.jar

# apt's tesseract-ocr on Ubuntu 22.04 (jammy) installs Tesseract 4.1.1, whose data lives under
# .../4.00/tessdata -- not .../5/tessdata. Verified by inspecting the built image directly.
ENV OCR_TESSDATA_PATH=/usr/share/tesseract-ocr/4.00/tessdata
ENV JAVA_OPTS=""

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
