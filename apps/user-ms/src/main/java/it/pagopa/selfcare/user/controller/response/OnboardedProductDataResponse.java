package it.pagopa.selfcare.user.controller.response;

import it.pagopa.selfcare.onboarding.common.Env;
import it.pagopa.selfcare.user.model.constants.OnboardedProductState;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.OffsetDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
public class OnboardedProductDataResponse extends OnboardedProductResponse {

    private boolean excludeRoleFromUserGroups;

}
