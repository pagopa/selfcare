package it.pagopa.selfcare.party.registry_proxy.connector.model.institution;

import lombok.Data;

import java.util.List;

@Data
public class OnboardingPage {
    private Integer total;
    private List<Onboarding> data;
}
