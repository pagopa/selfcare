package it.pagopa.selfcare.onboarding.client.model;

import it.pagopa.selfcare.onboarding.common.Origin;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "${openapi.product.model.id}")
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

