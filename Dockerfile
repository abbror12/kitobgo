# 1-bosqich: build (Gradle + JDK 25)
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app

# Avval faqat build fayllari — dependency'lar alohida layer'da keshlanadi,
# src o'zgarganda qaytadan yuklab o'tirilmaydi.
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon -q || true

COPY src src
RUN ./gradlew bootJar --no-daemon -q

# 2-bosqich: runtime (faqat JRE)
FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
