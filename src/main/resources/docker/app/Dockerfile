FROM openjdk:8-jdk-alpine
VOLUME /tmp
ADD oanda-2.0.jar oanda-2.0.jar
ENTRYPOINT exec java -Djava.security.egd=file:/dev/./urandom -jar /oanda-2.0.jar