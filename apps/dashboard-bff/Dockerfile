FROM maven@sha256:dcfe7934d3780beda6e1f0f641bc7db3fa9b8818899981f6eddae052c78394c7
ADD https://github.com/microsoft/ApplicationInsights-Java/releases/download/3.2.11/applicationinsights-agent-3.2.11.jar /applicationinsights-agent.jar
VOLUME /tmp
COPY target/*.jar app.jar
ENTRYPOINT ["java","-jar","app.jar"]

