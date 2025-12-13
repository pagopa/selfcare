package it.pagopa.selfcare.webhook.controller;

import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.Map;

@Path("/info")
@Tag(name = "System", description = "System information")
public class InfoController {

    @ConfigProperty(name = "quarkus.application.version")
    String version;

    @GET
    @Path("/version")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get application version", description = "Retrieve the current version of the application")
    public Uni<Map<String, String>> getVersion() {
        return Uni.createFrom().item(Map.of("version", version));
    }
}
