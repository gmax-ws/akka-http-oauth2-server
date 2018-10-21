FROM frolvlad/alpine-scala

EXPOSE 8086

COPY target/scala-2.12/akka-http-oauth2-server-assembly-1.0.0.jar ./akka-http-oauth2-server-assembly-1.0.0.jar

CMD ["java", "-jar", "./akka-http-oauth2-server-assembly-1.0.0.jar"]
