package it.pagopa.selfcare.user.integration_test.steps;

import io.cucumber.java.After;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.And;
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
import org.eclipse.microprofile.config.ConfigProvider;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.CountDownLatch;

@Slf4j
@ApplicationScoped
public class UserSteps {
    private final String mockUserInstitutionId = "65a4b6c7d8e9f01234567890";
    private final String mockUserId = "35a78332-d038-4bfa-8e85-2cba7f6b7bf7";
    private final String mockInstitutionId = "d0d28367-1695-4c50-a260-6fda526e9aab";

    @After("@RemoveUserInstitutionAndUserInfoAfterScenario")
    public void removeInstitutionIdAfterScenario(Scenario scenario) {
        UserInstitution.deleteById(new ObjectId(mockUserInstitutionId))
                .subscribe().with(
                        success -> {
                            log.info("userInstitution with id {} deleted", mockUserInstitutionId);
                            UserInfo user = (UserInfo) UserInfo.findById(mockUserId).await().indefinitely();
                            System.out.println("PRIMA DI ESEGUIRE");
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

    @And("A mock institution with id {string}, institutionName {string}, status {string}, role {string} to userInfo document with id {string}")
    public void createMockInstitutionWithId(String institutionId, String institutionName, String status, String role, String userId) {
        UserInstitutionRole userInstitutionRole = new UserInstitutionRole();
        userInstitutionRole.setInstitutionId(institutionId);
        userInstitutionRole.setRole(PartyRole.valueOf(role));
        userInstitutionRole.setStatus(OnboardedProductState.valueOf(status));
        userInstitutionRole.setInstitutionName(institutionName);

        UserInfo user = (UserInfo) UserInfo.findById(userId).await().indefinitely();
        System.out.println("PRIMA DI ESEGUIRE");
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

    @And("A mock userInstitution with id {string} and onboardedProductState {string}")
    public void createMockInstitutionWithId(String id, String onboardedProductState) {
        System.out.println("quarkus.mongodb.connection-string "+ ConfigProvider.getConfig().getOptionalValue("quarkus.mongodb.connection-string", String.class).orElse("NON TROVATO"));
        System.out.println("quarkus.mongodb.database "+ ConfigProvider.getConfig().getOptionalValue("quarkus.mongodb.database", String.class).orElse("NON TROVATO"));
        final UserInstitution userInstitution = new UserInstitution();
        userInstitution.setId(new ObjectId(id));
        userInstitution.setUserId(mockUserId);
        userInstitution.setInstitutionId(mockInstitutionId);
        userInstitution.setInstitutionDescription("Comune di Milano");
        userInstitution.setUserMailUuid("ID_MAIL#123123-55555-efaz-12312-apclacpela");

        final OnboardedProduct onboardedProduct = new OnboardedProduct();
        onboardedProduct.setCreatedAt(OffsetDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC));
        onboardedProduct.setProductId("prod-pagopa");
        onboardedProduct.setProductRole("SUB_DELEGATE");
        onboardedProduct.setEnv(Env.ROOT);
        onboardedProduct.setStatus(OnboardedProductState.valueOf(onboardedProductState));
        onboardedProduct.setTokenId("asda8312-3311-5642-gsds-gfr2252341");

        userInstitution.setProducts(List.of(onboardedProduct));
        System.out.println("PRIMA DEL PERSIST");
        // Crea un CountDownLatch per bloccare il thread
        CountDownLatch latch = new CountDownLatch(1);
        UserInstitution.persist(userInstitution).subscribe().with(
                success -> {
                    log.info("userInstitution with id {} created", id);
                    latch.countDown();  // Indica che l'operazione è completata
                },
                failure -> {
                    log.info("Failed to create userInstitution with id {}: {}", id, failure.getMessage());
                    latch.countDown();  // Indica che l'operazione è completata, anche in caso di errore
                }
        );

        try {
            latch.await();  // Aspetta che l'operazione asincrona finisca
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("DOPO PERSIST");
    }

}
