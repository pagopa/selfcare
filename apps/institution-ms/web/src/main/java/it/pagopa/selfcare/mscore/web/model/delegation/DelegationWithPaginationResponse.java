package it.pagopa.selfcare.mscore.web.model.delegation;

import com.fasterxml.jackson.annotation.JsonInclude;
import it.pagopa.selfcare.mscore.model.delegation.PageInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DelegationWithPaginationResponse {

    private List<DelegationResponse> delegations;
    private PageInfo pageInfo;

}
