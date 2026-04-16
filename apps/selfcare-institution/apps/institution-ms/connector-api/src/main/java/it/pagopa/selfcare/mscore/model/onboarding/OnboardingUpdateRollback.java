package it.pagopa.selfcare.mscore.model.onboarding;

import it.pagopa.selfcare.mscore.model.institution.Institution;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OnboardingUpdateRollback {
    private Token token;
    private List<String> userList;
    private Institution updatedInstitution;

}
