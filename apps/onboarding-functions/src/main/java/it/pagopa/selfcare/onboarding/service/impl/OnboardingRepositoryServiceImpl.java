package it.pagopa.selfcare.onboarding.service.impl;

import com.mongodb.MongoException;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.exception.DatabaseThrottledException;
import it.pagopa.selfcare.onboarding.repository.OnboardingRepository;
import it.pagopa.selfcare.onboarding.service.OnboardingRepositoryService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ApplicationScoped
@Slf4j
public class OnboardingRepositoryServiceImpl implements OnboardingRepositoryService {

    private static final int COSMOS_TOO_MANY_REQUESTS_CODE = 16500;
    private static final long DEFAULT_RETRY_AFTER_MS = 1000L;
    private static final Pattern RETRY_AFTER_MS = Pattern.compile("RetryAfterMs\\s*=\\s*(\\d+)");

    private final OnboardingRepository onboardingRepository;

    @Inject
    public OnboardingRepositoryServiceImpl(OnboardingRepository onboardingRepository) {
        this.onboardingRepository = onboardingRepository;
    }

    @Override
    public Optional<Onboarding> findById(String onboardingId) {
        log.debug("Finding onboarding by id: onboardingId={}", onboardingId);
        return onboardingRepository.findByIdOptional(onboardingId);
    }

    @Override
    public void update(Onboarding onboarding) {
        log.debug("Updating onboarding entity: onboardingId={}", onboarding != null ? onboarding.getId() : null);
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
        log.debug("Counting onboardings by query");
        return onboardingRepository.find(query).count();
    }

    @Override
    public Map<String, Long> countByProduct(Document query) {
        log.debug("Counting onboardings by product");
        List<Document> pipeline =
                List.of(
                        new Document("$match", query),
                        new Document("$group", new Document("_id", "$productId").append("count", new Document("$sum", 1))));
        Map<String, Long> counts = new HashMap<>();
        try {
            List<Document> groups =
                    onboardingRepository.mongoCollection().aggregate(pipeline, Document.class).into(new ArrayList<>());
            for (Document group : groups) {
                if (group.getString("_id") != null) {
                    counts.put(group.getString("_id"), ((Number) group.get("count")).longValue());
                }
            }
        } catch (MongoException e) {
            if (isThrottled(e)) {
                throw new DatabaseThrottledException("Database is throttling requests", retryAfterMs(e), e);
            }
            throw e;
        }
        return counts;
    }

    private static boolean isThrottled(MongoException e) {
        return e.getCode() == COSMOS_TOO_MANY_REQUESTS_CODE
                || (e.getMessage() != null && e.getMessage().contains("TooManyRequests"));
    }

    private static long retryAfterMs(MongoException e) {
        Matcher matcher = RETRY_AFTER_MS.matcher(String.valueOf(e.getMessage()));
        return matcher.find() ? Long.parseLong(matcher.group(1)) : DEFAULT_RETRY_AFTER_MS;
    }

    @Override
    public List<Onboarding> findByQueryPaged(Document query, int page, int pageSize) {
        log.debug("Finding onboardings by query paged: page={}, pageSize={}", page, pageSize);
        return onboardingRepository.find(query).page(page, pageSize).list();
    }

    @Override
    public List<Onboarding> findByFilters(
            String taxCode,
            String subunitCode,
            String origin,
            String originId,
            String productId) {
        log.debug("Finding onboardings by filters: taxCode={}, subunitCode={}, origin={}, originId={}, productId={}", taxCode, subunitCode, origin, originId, productId);
        return onboardingRepository.findByFilters(taxCode, subunitCode, origin, originId, productId);
    }

    @Override
    public List<Onboarding> findByOnboardingUsers(String institutionId, String productId) {
        log.debug("Finding onboarding users: institutionId={}, productId={}", institutionId, productId);
        return onboardingRepository.findByOnboardingUsers(institutionId, productId);
    }
}
