package it.pagopa.selfcare.mscore.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.pagopa.selfcare.mscore.core.EventsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;

@RestController
@RequestMapping(value = "/events", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Events")
@Slf4j
@RequiredArgsConstructor
public class EventsController {

    private final EventsService eventsService;

    @Operation(summary = "${swagger.mscore.events.delegations}", description = "${swagger.mscore.events.delegations}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping(value = "/delegations")
    public ResponseEntity<Void> sendDelegationEvents(@Parameter(description = "${swagger.mscore.events.fromDate}")
                                                     @RequestParam(name = "fromDate") OffsetDateTime fromDate) {
        eventsService.sendDelegationEvents(fromDate);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

}
