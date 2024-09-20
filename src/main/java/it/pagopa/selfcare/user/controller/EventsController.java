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

    @Operation(
            summary = "Resend all user events based on institutionId, userId, and fromDate",
            description = "Resends all events for a specific user within a given institution starting from the specified date and time. This endpoint allows administrators to trigger the reprocessing or notification of user-related events that occurred after the provided `fromDate`."
    )
    @POST
    @Path(value = "/sc-users")
    public Uni<Void> sendUsersEvents(@QueryParam(value = "institutionId") String institutionId,
                                  @QueryParam(value = "userId") String userId,
                                  @NotNull @QueryParam(value = "fromDate") LocalDateTime fromDate){
        return userService.sendEventsByDateAndUserIdAndInstitutionId(fromDate, institutionId, userId);
    }
}
