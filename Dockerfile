FROM maven:3-jdk-11
ADD . /pdfmerge
WORKDIR /pdfmerge
RUN mvn clean install
 
FROM openjdk:11-jdk
VOLUME /tmp
COPY --from=0 "/pdfmerge/target/pdfmerge-*-SNAPSHOT.jar" app.jar
CMD [ "sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar /app.jar" ]
