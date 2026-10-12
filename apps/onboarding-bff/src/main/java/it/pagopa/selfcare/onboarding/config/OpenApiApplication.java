package it.pagopa.selfcare.onboarding.config;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;
import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
import org.eclipse.microprofile.openapi.annotations.info.Info;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;
import org.eclipse.microprofile.openapi.annotations.servers.Server;

@ApplicationPath("/")
@OpenAPIDefinition(
        info = @Info(title = "${quarkus.smallrye-openapi.info-title}",
                version = "${quarkus.smallrye-openapi.info-version}"),
        servers = @Server(url = "http://localhost", description = "Generated server url"))
@SecurityScheme(securitySchemeName = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer",
        bearerFormat = "JWT", description = "${openapi.security.schema.bearer.description}")
public class OpenApiApplication extends Application {
}
