package it.pagopa.selfcare.registry.proxy.runner.model;

import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OnboardingSearchDocument {

    private String onboardingId;
    private String status;
    private OffsetDateTime expiringDate;
}