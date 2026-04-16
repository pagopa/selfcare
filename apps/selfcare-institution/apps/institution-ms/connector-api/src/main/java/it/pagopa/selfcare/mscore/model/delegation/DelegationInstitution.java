package it.pagopa.selfcare.mscore.model.delegation;

import com.fasterxml.jackson.annotation.JsonInclude;
import it.pagopa.selfcare.mscore.constant.DelegationType;
import it.pagopa.selfcare.mscore.model.institution.Institution;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DelegationInstitution {

    private long id;
    private String delegationId;
    private DelegationType delegationType;
    private String delegationProductId;
    private Institution institution;

}
