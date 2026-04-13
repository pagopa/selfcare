package it.pagopa.selfcare.onboarding.client.model;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class UserId {

    @NotNull
    private UUID id;

}
