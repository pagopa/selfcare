package it.pagopa.selfcare.onboarding.connector.model.onboarding;

import it.pagopa.selfcare.onboarding.common.InstitutionType;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;


@Data
@NoArgsConstructor
public class CheckManagerData {

    private UUID userId;
    private InstitutionType institutionType;
    private String origin;
    private String originId;
    private String productId;
    private String taxCode;
    private String subunitCode;
}
