package it.pagopa.selfcare.dashboard.model.user;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class CheckUserResponse {

    @Schema(description = "${swagger.dashboard.institutions.model.checkUserResponse}")
    private final Boolean isUserOnboarded;

    @JsonCreator
    public CheckUserResponse(@JsonProperty("isUserOnboarded") Boolean isUserOnboarded) {
        this.isUserOnboarded = isUserOnboarded;
    }
}

