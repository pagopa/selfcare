package it.pagopa.selfcare.onboarding.controller.request;

import lombok.Data;

@Data
public class AdditionalInformationsDto {
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
