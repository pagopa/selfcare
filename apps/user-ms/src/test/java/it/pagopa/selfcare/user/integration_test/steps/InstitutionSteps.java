package it.pagopa.selfcare.user.integration_test.steps;

import io.cucumber.java.After;
import io.cucumber.java.Scenario;
import it.pagopa.selfcare.user.entity.UserInstitution;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.List;

@Slf4j
@ApplicationScoped
public class InstitutionSteps {
    private final String mockUserId = "35a78332-d038-4bfa-8e85-2cba7f6b7bf7";
    private final String mockUserId2 = "97a511a7-2acc-47b9-afed-2f3c65753b4a";
    private final String mockInstitutionId2 = "e3a4c8d2-5b79-4f3e-92d7-184a9b6fcd21";

    @After("@RemoveUserInstitutions")
    public void removeUserInstitutionAfterCreateFromAPI(Scenario scenario) {
        long deleted = UserInstitution.delete("institutionId = ?1 and userId in ?2",
                mockInstitutionId2, List.of(mockUserId, mockUserId2))
                .await().atMost(Duration.ofSeconds(5));
        log.info("Deleted {} scenario userInstitutions for institution {}", deleted, mockInstitutionId2);
    }
}
