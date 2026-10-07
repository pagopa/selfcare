package it.pagopa.selfcare.onboarding.controller.request;

import org.eclipse.microprofile.openapi.annotations.media.Schema;
import lombok.Data;

@Data
public class AdditionalInformationsDto {

    @Schema(description = "${openapi.onboarding.institutions.model.assistance.belongRegulatedMarket}")
    private boolean belongRegulatedMarket;

    @Schema(description = "${openapi.onboarding.institutions.model.assistance.regulatedMarketNote}")
    private String regulatedMarketNote;

    @Schema(description = "${openapi.onboarding.institutions.model.assistance.ipa}")
    private boolean ipa;

    @Schema(description = "${openapi.onboarding.institutions.model.assistance.ipaCode}")
    private String ipaCode;

    @Schema(description = "${openapi.onboarding.institutions.model.assistance.establishedByRegulatoryProvision}")
    private boolean establishedByRegulatoryProvision;

    @Schema(description = "${openapi.onboarding.institutions.model.assistance.establishedByRegulatoryProvisionNote}")
    private String establishedByRegulatoryProvisionNote;

    @Schema(description = "${openapi.onboarding.institutions.model.assistance.agentOfPublicService}")
    private boolean agentOfPublicService;

    @Schema(description = "${openapi.onboarding.institutions.model.assistance.agentOfPublicServiceNote}")
    private String agentOfPublicServiceNote;

    @Schema(description = "${openapi.onboarding.institutions.model.assistance.otherNote}")
    private String otherNote;

}
