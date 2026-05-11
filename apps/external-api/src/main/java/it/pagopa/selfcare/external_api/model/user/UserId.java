package it.pagopa.selfcare.external_api.model.user;

import lombok.Data;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

@Data
public class UserId {
    @NotNull
    private UUID id;
}
