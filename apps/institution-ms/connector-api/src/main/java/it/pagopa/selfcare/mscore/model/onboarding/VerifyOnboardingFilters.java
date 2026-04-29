package it.pagopa.selfcare.mscore.model.onboarding;

import it.pagopa.selfcare.mscore.constant.CustomError;
import it.pagopa.selfcare.mscore.constant.RelationshipState;
import it.pagopa.selfcare.mscore.exception.InvalidRequestException;
import lombok.Data;
import org.springframework.util.StringUtils;

import java.util.List;

@Data
public class VerifyOnboardingFilters {
    private String productId;
    private String externalId; 
    private String taxCode; 
    private String origin; 
    private String originId; 
    private String subunitCode;
    private List<RelationshipState> validRelationshipStates;

    public VerifyOnboardingFilters(String productId, String externalId, String taxCode, String origin, String originId, String subunitCode) {
        this.productId = productId;

        /*
            We don't set the values of all the attributes of the class, but try to value only a portion of them based on different
            search patterns priority that we intend to convey.
            And if the provided parameters do not fall within the identified cases for a search then return exception.
         */
        if (StringUtils.hasText(externalId)) {
            this.externalId = externalId;
        } else if (StringUtils.hasText(taxCode)) {
            this.taxCode = taxCode;
            this.subunitCode = subunitCode;
            this.origin = origin;
        } else if(StringUtils.hasText(subunitCode)) {
            this.subunitCode = subunitCode;
            this.origin = origin;
        } else if (StringUtils.hasText(origin) && StringUtils.hasText(originId)) {
            this.origin = origin;
            this.originId = originId;
        } else {
            throw new InvalidRequestException(CustomError.ONBOARDING_INFO_FILTERS_ERROR.getMessage(), CustomError.ONBOARDING_INFO_FILTERS_ERROR.getCode());
        }
    }
}
