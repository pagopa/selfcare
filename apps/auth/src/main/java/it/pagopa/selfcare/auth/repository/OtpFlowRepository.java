package it.pagopa.selfcare.auth.repository;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.auth.entity.OtpFlow;
import it.pagopa.selfcare.auth.model.OtpStatus;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.bson.Document;

/**
 * Data-access chokepoint for OTP flows.
 *
 * <p>Auth issues sessions before {@code TenantContext} exists, so callers pass the tenant resolved
 * fail-closed from {@code X-Tenant-Id}. During migration, tenant-scoped reads also match legacy
 * records with no {@code tenantId}; the null branch MUST be dropped after the backfill.
 */
@ApplicationScoped
public class OtpFlowRepository {

  private static final String TENANT_ID_FIELD = OtpFlow.Fields.tenantId.name();
  private static final String UUID_FIELD = OtpFlow.Fields.uuid.name();
  private static final String USER_ID_FIELD = OtpFlow.Fields.userId.name();
  private static final String CREATED_AT_FIELD = OtpFlow.Fields.createdAt.name();

  private static Document tenantScoped(Document filter, String tenantId) {
    if (tenantId == null || tenantId.isBlank()) {
      // Migration-phase concession for non-request callers/tests: preserve pre-multitenant
      // behaviour when no tenant can be resolved. Auth endpoints pass a mandatory tenant.
      return filter;
    }
    return new Document(
        "$and",
        List.of(
            filter,
            new Document(
                "$or",
                List.of(new Document(TENANT_ID_FIELD, tenantId), new Document(TENANT_ID_FIELD, null)))));
  }

  public Uni<OtpFlow> persist(OtpFlow otpFlow) {
    return OtpFlow.persist(otpFlow).replaceWith(otpFlow);
  }

  public Uni<OtpFlow> findLastOtpFlowByUserId(String userId, String tenantId) {
    return OtpFlow.find(
            tenantScoped(new Document(USER_ID_FIELD, userId), tenantId),
            new Document(CREATED_AT_FIELD, -1))
        .firstResult();
  }

  public Uni<Optional<OtpFlow>> findOtpFlowByUuid(String uuid, String tenantId) {
    return OtpFlow.find(tenantScoped(new Document(UUID_FIELD, uuid), tenantId)).firstResultOptional();
  }

  public Uni<Long> updateOtpFlow(
      String uuid, String tenantId, OtpStatus newStatus, Boolean attemptsIncrement) {
    StringBuilder updateBuilder = new StringBuilder();
    updateBuilder.append("{");
    if (Boolean.TRUE.equals(attemptsIncrement)) {
      updateBuilder.append(" '$inc': { 'attempts': 1 },");
    }
    updateBuilder.append(" '$set': { 'status': ?1, 'updatedAt': ?2 } }");
    return OtpFlow.update(
            updateBuilder.toString(), newStatus, Date.from(OffsetDateTime.now().toInstant()))
        .where(tenantScoped(new Document(UUID_FIELD, uuid), tenantId));
  }

  public Uni<Long> countTodayDistinctUsers(String tenantId) {
    OffsetDateTime now = OffsetDateTime.now();
    OffsetDateTime startOfDay = now.toLocalDate().atStartOfDay().atOffset(now.getOffset());
    return OtpFlow.<OtpFlow>find(
            tenantScoped(new Document(CREATED_AT_FIELD, new Document("$gte", startOfDay.toInstant())), tenantId))
        .list()
        .map(list -> list.stream().map(OtpFlow::getUserId).distinct().count());
  }
}
