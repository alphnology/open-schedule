FROM eclipse-temurin:25-jre-jammy

WORKDIR /app

RUN groupadd --system appgroup && useradd --system --gid appgroup appuser

COPY target/open-schedule.jar app.jar

RUN chown appuser:appgroup app.jar

USER appuser

EXPOSE 51675

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-jar", "app.jar"]
