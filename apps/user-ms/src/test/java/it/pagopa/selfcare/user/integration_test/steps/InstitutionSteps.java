package it.pagopa.selfcare.user.integration_test.steps;

import io.cucumber.java.After;
import io.cucumber.java.Scenario;
import it.pagopa.selfcare.user.entity.UserInstitution;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

@Slf4j
@ApplicationScoped
public class InstitutionSteps {
    private final String mockUserId = "35a78332-d038-4bfa-8e85-2cba7f6b7bf7";
    private final String mockUserId2 = "97a511a7-2acc-47b9-afed-2f3c65753b4a";
    private final String mockInstitutionId2 = "e3a4c8d2-5b79-4f3e-92d7-184a9b6fcd21";

    @After("@RemoveUserInstitutions")
    public void removeUserInstitutionAfterCreateFromAPI(Scenario scenario) {
        UserInstitution.find("institutionId = ?1 and userId = ?2", mockInstitutionId2, mockUserId2)
                .firstResult()
                .subscribe()
                .with(
                        success -> {
                            UserInstitution userInstitution = (UserInstitution) success;
                            if(!Objects.isNull(userInstitution)) {
                                UserInstitution.deleteById(userInstitution.getId())
                                        .subscribe()
                                        .with(
                                                deleteSuccess -> log.info("Deleted userInstitution with userId {} and institutionId {}", mockUserId2, mockInstitutionId2),
                                                deleteFailure -> log.info("Failed to delete userInstitution with  userId {} and institutionId {}: {}", mockUserId2, mockInstitutionId2, deleteFailure.getMessage())
                                        );
                            } else {
                                log.info("No userInstitution with userId {} and institutionId {}", mockUserId2, mockInstitutionId2);
                            }
                        },
                        failure -> log.info("Failed to find userInstitution with userId {} and institutionId {}", mockUserId2, mockInstitutionId2)
                );

        UserInstitution.find("institutionId = ?1 and userId = ?2", mockInstitutionId2, mockUserId)
                .firstResult()
                .subscribe()
                .with(
                        success -> {
                            UserInstitution userInstitution = (UserInstitution) success;
                            if(!Objects.isNull(userInstitution)) {
                                UserInstitution.deleteById(userInstitution.getId())
                                        .subscribe()
                                        .with(
                                                deleteSuccess -> log.info("Deleted userInstitution with userId {} and institutionId {}", mockUserId, mockInstitutionId2),
                                                deleteFailure -> log.info("Failed to delete userInstitution with  userId {} and institutionId {}: {}", mockUserId, mockInstitutionId2, deleteFailure.getMessage())
                                        );
                            } else {
                                log.info("No userInstitution with userId {} and institutionId {}", mockUserId, mockInstitutionId2);
                            }
                        },
                        failure -> log.info("Failed to find userInstitution with userId {} and institutionId {}", mockUserId, mockInstitutionId2)
                );
    }
}
