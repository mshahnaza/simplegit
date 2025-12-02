FROM amazoncorretto:21-alpine
WORKDIR /app
COPY target/simplegit-1.0-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
