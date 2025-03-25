package it.pagopa.selfcare.user.integration_test.steps;

import io.cucumber.java.After;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.And;
import it.pagopa.selfcare.cucumber.utils.SharedStepData;
import it.pagopa.selfcare.onboarding.common.Env;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.user.entity.UserInfo;
import it.pagopa.selfcare.user.entity.UserInstitution;
import it.pagopa.selfcare.user.entity.UserInstitutionRole;
import it.pagopa.selfcare.user.model.OnboardedProduct;
import it.pagopa.selfcare.user.model.constants.OnboardedProductState;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Assertions;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;

@Slf4j
@ApplicationScoped
public class UserSteps {
    private final String mockUserInstitutionId = "65a4b6c7d8e9f01234567890";
    private final String mockUserInstitutionId2 = "78a2342c31599e1812b1819a";
    private final String mockUserId = "35a78332-d038-4bfa-8e85-2cba7f6b7bf7";
    private final String mockInstitutionId = "d0d28367-1695-4c50-a260-6fda526e9aab";
    private final String mockUserId2 = "97a511a7-2acc-47b9-afed-2f3c65753b4a";
    private final String mockUserId3 = "6f8b2d3a-4c1e-44d8-bf92-1a7f8e2c3d5b";
    private final String mockInstitutionId2 = "e3a4c8d2-5b79-4f3e-92d7-184a9b6fcd21";
    private SharedStepData sharedStepData;

    @After("@RemoveUserInstitutionAndUserInfoAfterScenario")
    public void removeInstitutionIdAfterScenario(Scenario scenario) {
        UserInstitution.deleteById(new ObjectId(mockUserInstitutionId))
                .subscribe().with(
                        success -> {
                            log.info("userInstitution with id {} deleted", mockUserInstitutionId);
                            UserInfo user = (UserInfo) UserInfo.findById(mockUserId).await().indefinitely();
                            user.setInstitutions(user.getInstitutions()
                                    .stream()
                                    .filter(institution -> !institution.getInstitutionId().equals(mockInstitutionId))
                                    .toList()
                            );

                            UserInfo.persistOrUpdate(user)
                                    .subscribe()
                                    .with(
                                            updateSuccess -> {
                                                log.info("UserInfo with id {} update", user.getUserId());
                                            },
                                            updateFailure -> {
                                                log.info("Failed to update UserInfo with id {}: {}", user.getUserId(), updateFailure.getMessage());
                                            });
                        },
                        failure -> log.info("Failed to delete userInstitution with id {}: {}", mockUserInstitutionId, failure.getMessage())
                );

    }

    @After("@RemoveUserInstitutionAfterCreateFromAPI")
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
    }

    @After("@RemoveUserInstitutionAndUserInfoAfterScenarioWithUnusedUser")
    public void removeInstitutionIdAfterScenarioWithUnusedUser(Scenario scenario) {
        UserInstitution.deleteById(new ObjectId(mockUserInstitutionId))
                .subscribe().with(
                        success -> {
                            log.info("userInstitution with id {} deleted", mockUserInstitutionId);
                        },
                        failure -> log.info("Failed to delete userInstitution with id {}: {}", mockUserInstitutionId, failure.getMessage())
                );
        UserInstitution.deleteById(new ObjectId(mockUserInstitutionId2))
                .subscribe().with(
                        success -> {
                            log.info("userInstitution with id {} deleted", mockUserInstitutionId2);
                        },
                        failure -> log.info("Failed to delete userInstitution with id {}: {}", mockUserInstitutionId2, failure.getMessage())
                );
    }


    @And("A mock userInfo with id {string}, institutionName {string}, status {string}, role {string} to userInfo document with id {string}")
    public void createMockUserInfo(String institutionId, String institutionName, String status, String role, String userId) {
        UserInstitutionRole userInstitutionRole = new UserInstitutionRole();
        userInstitutionRole.setInstitutionId(institutionId);
        userInstitutionRole.setRole(PartyRole.valueOf(role));
        userInstitutionRole.setStatus(OnboardedProductState.valueOf(status));
        userInstitutionRole.setInstitutionName(institutionName);

        UserInfo user = (UserInfo) UserInfo.findById(userId).await().indefinitely();
        user.getInstitutions().add(userInstitutionRole);

        UserInfo.persistOrUpdate(user)
                .subscribe()
                .with(
                        success -> {
                            log.info("UserInfo with id {} updated", user.getUserId());
                        },
                        failure -> {
                            log.info("Failed to update UserInfo with id {}: {}", user.getUserId(), failure.getMessage());
                        });

    }

    @And("A mock userInstitution with id {string} and onboardedProductState {string} and role {string} and productId {string}")
    public void createMockInstitution(String id, String onboardedProductState, String role, String productId) {
        final UserInstitution userInstitution = new UserInstitution();
        userInstitution.setId(new ObjectId(id));
        userInstitution.setUserId(mockUserId);
        userInstitution.setInstitutionId(mockInstitutionId);
        userInstitution.setInstitutionDescription("Comune di Milano");
        userInstitution.setUserMailUuid("ID_MAIL#123123-55555-efaz-12312-apclacpela");

        final OnboardedProduct onboardedProduct = new OnboardedProduct();
        onboardedProduct.setCreatedAt(OffsetDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC));
        onboardedProduct.setProductId(productId);
        onboardedProduct.setProductRole("admin");
        onboardedProduct.setRole(PartyRole.valueOf(role));
        onboardedProduct.setEnv(Env.ROOT);
        onboardedProduct.setStatus(OnboardedProductState.valueOf(onboardedProductState));
        onboardedProduct.setTokenId("asda8312-3311-5642-gsds-gfr2252341");

        userInstitution.setProducts(List.of(onboardedProduct));
        CountDownLatch latch = new CountDownLatch(1);
        UserInstitution.persist(userInstitution).subscribe().with(
                success -> {
                    log.info("userInstitution with id {} created", id);
                    latch.countDown();
                },
                failure -> {
                    log.info("Failed to create userInstitution with id {}: {}", id, failure.getMessage());
                    latch.countDown();
                }
        );

        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @And("A mock userInstitution with id {string} and onboardedProductState {string} and role {string} and productId {string} and institutionId {string} and institutionDescription {string} and unused user")
    public void createMockInstitutionWithUnusedUser(String id, String onboardedProductState, String role, String productId, String institutionId, String institutionDescription) {
        final UserInstitution userInstitution = new UserInstitution();
        userInstitution.setId(new ObjectId(id));
        userInstitution.setUserId(mockUserId3);
        userInstitution.setInstitutionId(institutionId);
        userInstitution.setInstitutionDescription(institutionDescription);
        userInstitution.setUserMailUuid("ID_MAIL#123123-55555-efaz-12312-apclacpela");

        final OnboardedProduct onboardedProduct = new OnboardedProduct();
        onboardedProduct.setCreatedAt(OffsetDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC));
        onboardedProduct.setProductId(productId);
        onboardedProduct.setProductRole(role.equals("SUB_DELEGATE") ? "admin" : "referente amministrativo");
        onboardedProduct.setRole(PartyRole.valueOf(role));
        onboardedProduct.setEnv(Env.ROOT);
        onboardedProduct.setStatus(OnboardedProductState.valueOf(onboardedProductState));
        onboardedProduct.setTokenId("asda8312-3311-5642-gsds-gfr2252341");

        userInstitution.setProducts(List.of(onboardedProduct));
        CountDownLatch latch = new CountDownLatch(1);
        UserInstitution.persist(userInstitution).subscribe().with(
                success -> {
                    log.info("userInstitution with id {} created", id);
                    latch.countDown();
                },
                failure -> {
                    log.info("Failed to create userInstitution with id {}: {}", id, failure.getMessage());
                    latch.countDown();
                }
        );

        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @And("The response body does not contain:")
    public void checkResponseBodyNotContains(Map<String, String> unexpectedKeyValues) {
        unexpectedKeyValues.forEach((key, unexpectedValue) -> {
            final String actualValue = sharedStepData.getResponse().body().jsonPath().getString(key);
            Assertions.assertNotEquals(unexpectedValue, actualValue,
                    String.format("The field %s unexpectedly contains the value %s", key, unexpectedValue));
        });
    }
}
