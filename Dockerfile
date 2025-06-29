FROM eclipse-temurin:21-jre
COPY target/open-schedule.jar open-schedule.jar
EXPOSE 51675
ENTRYPOINT ["java", "-jar", "/open-schedule.jar"]
