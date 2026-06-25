package it.pagopa.selfcare.dashboard.model.delegation;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DelegationWithPagination {

    private List<DelegationWithInfo> delegations;
    private PageInfo pageInfo;

}
