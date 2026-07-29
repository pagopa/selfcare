package it.pagopa.selfcare.onboarding.web.model;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class AdditionalInformationsDto {

    @Schema(description = "${swagger.onboarding.institutions.model.assistance.belongRegulatedMarket}")
    private boolean belongRegulatedMarket;

    @Schema(description = "${swagger.onboarding.institutions.model.assistance.regulatedMarketNote}")
    private String regulatedMarketNote;

    @Schema(description = "${swagger.onboarding.institutions.model.assistance.ipa}")
    private boolean ipa;

    @Schema(description = "${swagger.onboarding.institutions.model.assistance.ipaCode}")
    private String ipaCode;

    @Schema(description = "${swagger.onboarding.institutions.model.assistance.establishedByRegulatoryProvision}")
    private boolean establishedByRegulatoryProvision;

    @Schema(description = "${swagger.onboarding.institutions.model.assistance.establishedByRegulatoryProvisionNote}")
    private String establishedByRegulatoryProvisionNote;

    @Schema(description = "${swagger.onboarding.institutions.model.assistance.agentOfPublicService}")
    private boolean agentOfPublicService;

    @Schema(description = "${swagger.onboarding.institutions.model.assistance.agentOfPublicServiceNote}")
    private String agentOfPublicServiceNote;

    @Schema(description = "${swagger.onboarding.institutions.model.assistance.otherNote}")
    private String otherNote;

}
