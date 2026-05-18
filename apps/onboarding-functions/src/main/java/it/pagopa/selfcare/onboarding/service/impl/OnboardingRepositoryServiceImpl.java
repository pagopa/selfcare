package it.pagopa.selfcare.onboarding.service.impl;

import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.repository.OnboardingRepository;
import it.pagopa.selfcare.onboarding.service.OnboardingRepositoryService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
@Slf4j
public class OnboardingRepositoryServiceImpl implements OnboardingRepositoryService {

    private final OnboardingRepository onboardingRepository;

    @Inject
    public OnboardingRepositoryServiceImpl(OnboardingRepository onboardingRepository) {
        this.onboardingRepository = onboardingRepository;
    }

    @Override
    public Optional<Onboarding> findById(String onboardingId) {
        return onboardingRepository.findByIdOptional(onboardingId);
    }

    @Override
    public void update(Onboarding onboarding) {
        onboardingRepository.update(onboarding);
    }

    @Override
    public void updateStatus(String onboardingId, String status, LocalDateTime updatedAt) {
        log.debug(
                "Updating onboarding status: onboardingId={}, status={}, updatedAt={}",
                onboardingId,
                status,
                updatedAt);
        onboardingRepository
                .update("status = ?1 and updatedAt = ?2", status, updatedAt)
                .where("_id", onboardingId);
    }

    @Override
    public void updateStatusAndInstanceId(
            String onboardingId,
            String status,
            String instanceId,
            LocalDateTime updatedAt) {
        log.debug(
                "Updating onboarding status with instance: onboardingId={}, status={}, instanceId={}, updatedAt={}",
                onboardingId,
                status,
                instanceId,
                updatedAt);
        onboardingRepository
                .update(
                        "status = ?1 and workflowInstanceId = ?2 and updatedAt = ?3",
                        status,
                        instanceId,
                        updatedAt)
                .where("_id", onboardingId);
    }

    @Override
    public long countByQuery(Document query) {
        return onboardingRepository.find(query).count();
    }

    @Override
    public List<Onboarding> findByQueryPaged(Document query, int page, int pageSize) {
        return onboardingRepository.find(query).page(page, pageSize).list();
    }

    @Override
    public List<Onboarding> findByFilters(
            String taxCode,
            String subunitCode,
            String origin,
            String originId,
            String productId) {
        return onboardingRepository.findByFilters(taxCode, subunitCode, origin, originId, productId);
    }

    @Override
    public List<Onboarding> findByOnboardingUsers(String institutionId, String productId) {
        return onboardingRepository.findByOnboardingUsers(institutionId, productId);
    }
}
