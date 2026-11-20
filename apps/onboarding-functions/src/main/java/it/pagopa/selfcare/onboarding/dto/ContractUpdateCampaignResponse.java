package it.pagopa.selfcare.onboarding.dto;

public record ContractUpdateCampaignResponse(Outcome outcome, String digitalAddress) {

    public enum Outcome {
        SENT,
        ALREADY_NOTIFIED,
        MISSING_PEC,
        INVALID
    }
}
