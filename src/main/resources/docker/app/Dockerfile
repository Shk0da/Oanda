FROM openjdk:8-jdk-alpine
VOLUME /tmp
ADD oandabot.war oandabot.war
ENTRYPOINT exec java -Djava.security.egd=file:/dev/./urandom -jar /oandabot.war