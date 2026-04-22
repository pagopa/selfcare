package it.pagopa.selfcare.onboarding.model;

import it.pagopa.selfcare.onboarding.common.OnboardingStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Builder
@Getter
@Setter
public class OnboardingGetFilters {
    private String productId;
    private List<String> productIds;
    private String institutionId;
    private String onboardingId;
    private String subunitCode;
    private String taxCode;
    private OnboardingStatus status;
    private String from;
    private String to;
    private String userId;
    private Integer page;
    private Integer size;
    private boolean skipPagination;
}
