package it.pagopa.selfcare.onboarding.entity;

import lombok.Data;

@Data
public class AdditionalInformations {
    private boolean belongRegulatedMarket;
    private String regulatedMarketNote;
    private boolean ipa;
    private String ipaCode;
    private boolean establishedByRegulatoryProvision;
    private String establishedByRegulatoryProvisionNote;
    private boolean agentOfPublicService;
    private String agentOfPublicServiceNote;
    private String otherNote;
}
