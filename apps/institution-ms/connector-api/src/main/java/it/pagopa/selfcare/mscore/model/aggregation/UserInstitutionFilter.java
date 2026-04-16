package it.pagopa.selfcare.mscore.model.aggregation;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class UserInstitutionFilter {
    private String fromCollection = "User";
    private String toCollection = "Institution";
    private String userId;
    private String institutionId;
    private String externalId;
    private List<String> states;

    public UserInstitutionFilter(String userId, String institutionId, String externalId, List<String> states) {
        this.userId = userId;
        this.institutionId = institutionId;
        this.externalId = externalId;
        this.states = states;
    }
}
