FROM eclipse-temurin:21-jdk-alpine

WORKDIR /app

# Gradle 빌드 결과물 복사 (파일 이름 상관없이 *.jar 한 개만 있다고 가정)
COPY build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]