package it.pagopa.selfcare.auth.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.mongodb.MongoTestResource;
import it.pagopa.selfcare.auth.entity.OtpFlow;
import it.pagopa.selfcare.auth.model.OtpStatus;
import jakarta.inject.Inject;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
@QuarkusTestResource(MongoTestResource.class)
class OtpFlowRepositoryTest {

  @Inject OtpFlowRepository repository;

  @BeforeEach
  @AfterEach
  void clean() {
    OtpFlow.deleteAll().await().indefinitely();
  }

  @Test
  void findByUuidReturnsSameTenantOtp() {
    persist(otpFlow("same-tenant", "user", "AR", OffsetDateTime.now()));

    Optional<OtpFlow> result =
        repository.findOtpFlowByUuid("same-tenant", "AR").await().indefinitely();

    assertTrue(result.isPresent());
    assertEquals("AR", result.get().getTenantId());
  }

  @Test
  void findByUuidDoesNotReturnOtherTenantOtp() {
    persist(otpFlow("pnpg-tenant", "user", "PNPG", OffsetDateTime.now()));

    Optional<OtpFlow> result =
        repository.findOtpFlowByUuid("pnpg-tenant", "AR").await().indefinitely();

    assertTrue(result.isEmpty());
  }

  @Test
  void findByUuidStillReturnsLegacyOtpWithoutTenant() {
    persist(otpFlow("legacy", "user", null, OffsetDateTime.now()));

    Optional<OtpFlow> result = repository.findOtpFlowByUuid("legacy", "AR").await().indefinitely();

    assertTrue(result.isPresent());
  }

  @Test
  void findLastByUserIsUnscopedWhenNoTenantIsProvided() {
    OffsetDateTime now = OffsetDateTime.now();
    persist(otpFlow("older-ar", "user", "AR", now.minusMinutes(1)));
    persist(otpFlow("newer-pnpg", "user", "PNPG", now));

    OtpFlow result = repository.findLastOtpFlowByUserId("user", null).await().indefinitely();

    assertEquals("newer-pnpg", result.getUuid());
  }

  private void persist(OtpFlow otpFlow) {
    OtpFlow.persist(otpFlow).await().indefinitely();
  }

  private OtpFlow otpFlow(String uuid, String userId, String tenantId, OffsetDateTime createdAt) {
    return OtpFlow.builder()
        .uuid(uuid)
        .tenantId(tenantId)
        .userId(userId)
        .otp("otp")
        .status(OtpStatus.PENDING)
        .attempts(0)
        .createdAt(createdAt)
        .expiresAt(createdAt.plusMinutes(5))
        .build();
  }
}
