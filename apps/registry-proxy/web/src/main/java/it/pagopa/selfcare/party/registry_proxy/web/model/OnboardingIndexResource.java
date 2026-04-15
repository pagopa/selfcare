package it.pagopa.selfcare.party.registry_proxy.web.model;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class OnboardingIndexResource {

    @NotNull
    private String onboardingId;

    private String institutionId;
    private String description;
    private String parentDescription;
    private String taxCode;
    private String subunitCode;
    private String subunitType;
    private String productId;
    private String institutionType;
    private String status;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime activatedAt;
    private OffsetDateTime expiringDate;

}
