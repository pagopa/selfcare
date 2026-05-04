package it.pagopa.selfcare.mscore.web.model.institution;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;

@Data
public class OnboardingsResponse {

    private List<OnboardingResponse> onboardings;
}
