package it.pagopa.selfcare.auth.controller;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.auth.controller.response.*;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import it.pagopa.selfcare.auth.service.FooService;

@Tag(name = "Login")
@Path("/login")
@RequiredArgsConstructor
@Slf4j
public class LoginController {

    private final FooService fooService;

    @Operation(description = "Foo API", summary = "Foo API")
    @GET
    @Path(value = "/foo")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<String> foo() {
        return fooService.foo();
    }

}

