package it.pagopa.selfcare.mscore.model.institution;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class InstitutionAdditionalInfoForIpa {
    private String rea;
    private String shareCapital;
    private String businessRegisterPlace;
}
