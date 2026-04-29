package it.pagopa.selfcare.mscore.model.aggregation;

import it.pagopa.selfcare.mscore.model.institution.Institution;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;

import java.util.List;

@Data
@FieldNameConstants(asEnum = true)
@AllArgsConstructor
@NoArgsConstructor
public class UserInstitutionAggregation {
    private String id;
    private UserInstitutionBinding bindings;
    private List<Institution> institutions;
}
