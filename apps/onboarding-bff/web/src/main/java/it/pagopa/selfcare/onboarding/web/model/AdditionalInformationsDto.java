package it.pagopa.selfcare.onboarding.web.model;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class AdditionalInformationsDto {

    @ApiModelProperty(value = "${swagger.onboarding.institutions.model.assistance.belongRegulatedMarket}")
    private boolean belongRegulatedMarket;

    @ApiModelProperty(value = "${swagger.onboarding.institutions.model.assistance.regulatedMarketNote}")
    private String regulatedMarketNote;

    @ApiModelProperty(value = "${swagger.onboarding.institutions.model.assistance.ipa}")
    private boolean ipa;

    @ApiModelProperty(value = "${swagger.onboarding.institutions.model.assistance.ipaCode}")
    private String ipaCode;

    @ApiModelProperty(value = "${swagger.onboarding.institutions.model.assistance.establishedByRegulatoryProvision}")
    private boolean establishedByRegulatoryProvision;

    @ApiModelProperty(value = "${swagger.onboarding.institutions.model.assistance.establishedByRegulatoryProvisionNote}")
    private String establishedByRegulatoryProvisionNote;

    @ApiModelProperty(value = "${swagger.onboarding.institutions.model.assistance.agentOfPublicService}")
    private boolean agentOfPublicService;

    @ApiModelProperty(value = "${swagger.onboarding.institutions.model.assistance.agentOfPublicServiceNote}")
    private String agentOfPublicServiceNote;

    @ApiModelProperty(value = "${swagger.onboarding.institutions.model.assistance.otherNote}")
    private String otherNote;

}
