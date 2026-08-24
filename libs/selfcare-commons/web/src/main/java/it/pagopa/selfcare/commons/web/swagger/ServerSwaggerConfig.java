package it.pagopa.selfcare.commons.web.swagger;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.servers.ServerVariable;
import io.swagger.v3.oas.models.servers.ServerVariables;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.annotation.RequestScope;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Configuration
public class ServerSwaggerConfig {

  private static final Pattern PATTERN = Pattern.compile("^([^:]+:\\/\\/[^(?:\\:|\\/)]+)(?:(?:\\:(\\d+)\\/)|\\/).*$");

  @Bean
  @RequestScope
  public OpenAPI customOpenAPI(HttpServletRequest request) {
    OpenAPI openApi = new OpenAPI();

    // Configurazione del server
    final ServerVariable urlVariable = new ServerVariable();
    final ServerVariable portVariable = new ServerVariable();
    final ServerVariable basePathVariable = new ServerVariable();

    final Matcher matcher = PATTERN.matcher(Optional.ofNullable(request.getRequestURL())
      .map(StringBuffer::toString)
      .orElse("http://localhost:8080"));
    if (matcher.matches()) {
      urlVariable.setDefault(matcher.group(1));
      portVariable.setDefault(Optional.ofNullable(matcher.group(2)).orElse("80"));
    }
    basePathVariable.setDefault("");

    final ServerVariables serverVariables = new ServerVariables();
    serverVariables.addServerVariable("url", urlVariable);
    serverVariables.addServerVariable("port", portVariable);
    serverVariables.addServerVariable("basePath", basePathVariable);

    Server server = new Server();
    server.setUrl("{url}:{port}{basePath}");
    server.setVariables(serverVariables);

    openApi.setServers(List.of(server));
    return openApi;
  }
}
