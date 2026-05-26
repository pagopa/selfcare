package it.pagopa.selfcare.onboarding.service;

import it.pagopa.selfcare.onboarding.entity.Onboarding;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.bson.Document;

public interface OnboardingRepositoryService {
  Optional<Onboarding> findById(String onboardingId);

  void update(Onboarding onboarding);

  void updateStatus(String onboardingId, String status, LocalDateTime updatedAt);

  void updateStatusAndInstanceId(
    String onboardingId,
    String status,
    String instanceId,
    LocalDateTime updatedAt);

  long countByQuery(Document query);

  List<Onboarding> findByQueryPaged(Document query, int page, int pageSize);

  List<Onboarding> findByFilters(
    String taxCode,
    String subunitCode,
    String origin,
    String originId,
    String productId);

  List<Onboarding> findByOnboardingUsers(String institutionId, String productId);
}
