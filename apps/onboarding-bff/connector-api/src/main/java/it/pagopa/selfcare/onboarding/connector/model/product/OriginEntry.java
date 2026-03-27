package it.pagopa.selfcare.onboarding.connector.model.product;

import it.pagopa.selfcare.onboarding.common.Origin;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OriginEntry {
    private InstitutionType institutionType;
    private Origin origin;
    private String labelKey;

    public enum InstitutionType {
        PA,
        PG,
        GSP,
        SA,
        PT,
        SCP,
        PSP,
        AS,
        REC,
        CON,
        PRV,
        PRV_PF,
        GPU,
        SCEC,
        DEFAULT;
    }
}

