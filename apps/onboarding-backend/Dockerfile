FROM maven@sha256:f9bde890aa2cea7e6c1d6480bead88ed07f5bf29ab0a76289553e5c9895d682b
ADD https://github.com/microsoft/ApplicationInsights-Java/releases/download/3.2.11/applicationinsights-agent-3.2.11.jar /applicationinsights-agent.jar
VOLUME /tmp
COPY target/*.jar app.jar
ENTRYPOINT ["java","-jar","app.jar"]

