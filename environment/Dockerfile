FROM eclipse-temurin:21

# Copy the jar
RUN mkdir /opt/app
COPY build/libs/app.jar /opt/app

# The default http port
EXPOSE 8080

ENTRYPOINT java -jar /opt/app/app.jar