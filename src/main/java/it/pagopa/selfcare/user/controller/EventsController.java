package it.pagopa.selfcare.user.controller;

import io.quarkus.security.Authenticated;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.user.service.UserService;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Authenticated
@Tag(name = "Events")
@Path("/events")
@RequiredArgsConstructor
@Slf4j
public class EventsController {
    private final UserService userService;

    @Operation(summary = "The API resend all the users's given an institutionId and a userId after the given fromDate")
    @POST
    @Path(value = "/sc-users")
    public Uni<Void> sendUsersEvents(@QueryParam(value = "institutionId") String institutionId,
                                  @QueryParam(value = "userId") String userId,
                                  @NotNull @QueryParam(value = "fromDate") LocalDateTime fromDate){
        return userService.sendEventsByDateAndUserIdAndInstitutionId(fromDate, institutionId, userId);
    }
}
