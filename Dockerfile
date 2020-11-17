FROM openjdk:8-alpine

COPY target/uberjar/golf.jar /golf/app.jar

EXPOSE 3000

CMD ["java", "-jar", "/golf/app.jar"]
