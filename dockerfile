FROM openjdk:17-jdk-slim

WORKDIR /app

COPY target/the-nails-1.0.jar /app/the-nails-1.0.jar

COPY src/main/resources/application.yml /app/application.yml

COPY src/main/resources/logback.xml /app/logback.xml

ENV SPRING_CONFIG_LOCATION=/app/application.yml
ENV LOGGING_CONFIG=/app/logback.xml

EXPOSE 8080

CMD ["java", "-jar", "/app/the-nails-1.0.jar", "--spring.config.location=/app/application.yml", "--logging.config=/app/logback.xml"]
