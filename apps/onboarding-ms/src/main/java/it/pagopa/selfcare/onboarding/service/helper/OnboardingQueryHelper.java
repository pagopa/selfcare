package it.pagopa.selfcare.onboarding.service.helper;

import static it.pagopa.selfcare.onboarding.constants.CustomError.*;
import static it.pagopa.selfcare.onboarding.util.ErrorMessage.*;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.tuples.Tuple2;
import it.pagopa.selfcare.onboarding.common.OnboardingStatus;
import it.pagopa.selfcare.onboarding.controller.request.ApproveRequest;
import it.pagopa.selfcare.onboarding.controller.request.CheckManagerRequest;
import it.pagopa.selfcare.onboarding.controller.request.ReasonRequest;
import it.pagopa.selfcare.onboarding.controller.response.OnboardingGet;
import it.pagopa.selfcare.onboarding.controller.response.OnboardingGetResponse;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.exception.InvalidRequestException;
import it.pagopa.selfcare.onboarding.factory.OnboardingResponseFactory;
import it.pagopa.selfcare.onboarding.util.QueryUtils;
import it.pagopa.selfcare.onboarding.util.SortEnum;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.LocalDateTime;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;

/**
 * Helper che raccoglie le utility di query, recupero e aggiornamento
 * degli onboarding nel database MongoDB.
 */
@Slf4j
@ApplicationScoped
public class OnboardingQueryHelper {

    @Inject
    OnboardingResponseFactory onboardingResponseFactory;

    @Inject
    OnboardingPersistenceHelper persistenceHelper;

    @Inject
    QueryUtils queryUtils;

    // -------------------------------------------------------------------------
    // Recupero onboarding
    // -------------------------------------------------------------------------

    public Uni<Onboarding> retrieveOnboarding(String onboardingId) {
        // Migration-phase concession for code outside an active request: keep the pre-tenant
        // unscoped lookup rather than making schedulers/event consumers fail closed.
        if (queryUtils.currentTenantId().isEmpty()) {
            return Onboarding.findByIdOptional(onboardingId)
                    .onItem().transformToUni(opt ->
                            opt.map(Onboarding.class::cast)
                                    .map(o -> Uni.createFrom().item(o))
                                    .orElse(Uni.createFrom().failure(
                                            new InvalidRequestException(
                                                    String.format("Onboarding with id '%s' not found", onboardingId)))));
        }
        return Onboarding.find(queryUtils.tenantScoped(new Document("_id", onboardingId)))
                .firstResult()
                .onItem().transformToUni(opt ->
                        Objects.nonNull(opt)
                                ? Uni.createFrom().item((Onboarding) opt)
                                : Uni.createFrom().failure(
                                        new InvalidRequestException(
                                                String.format("Onboarding with id '%s' not found", onboardingId))));
    }

    public Uni<Onboarding> retrieveOnboardingAndCheckIfExpired(String onboardingId) {
        // Migration-phase concession for code outside an active request; see QueryUtils.
        if (queryUtils.currentTenantId().isEmpty()) {
            return Onboarding.findByIdOptional(onboardingId)
                    .onItem().transformToUni(opt ->
                            opt.map(Onboarding.class::cast)
                                    .filter(o -> OnboardingStatus.TOBEVALIDATED.equals(o.getStatus())
                                            || !isOnboardingExpired(o.getExpiringDate()))
                                    .map(o -> Uni.createFrom().item(o))
                                    .orElse(Uni.createFrom().failure(
                                            new InvalidRequestException(
                                                    String.format(ONBOARDING_EXPIRED.getMessage(), onboardingId),
                                                    ONBOARDING_EXPIRED.getCode()))));
        }
        return Onboarding.find(queryUtils.tenantScoped(new Document("_id", onboardingId)))
                .firstResult()
                .onItem().transformToUni(opt ->
                        Optional.ofNullable(opt)
                                .map(Onboarding.class::cast)
                                .filter(o -> OnboardingStatus.TOBEVALIDATED.equals(o.getStatus())
                                        || !isOnboardingExpired(o.getExpiringDate()))
                                .map(o -> Uni.createFrom().item(o))
                                .orElse(Uni.createFrom().failure(
                                        new InvalidRequestException(
                                                String.format(ONBOARDING_EXPIRED.getMessage(), onboardingId),
                                                ONBOARDING_EXPIRED.getCode()))));
    }

    public Uni<Onboarding> checkIfToBeValidated(Onboarding onboarding) {
        return OnboardingStatus.TOBEVALIDATED.equals(onboarding.getStatus())
                ? Uni.createFrom().item(onboarding)
                : Uni.createFrom().failure(new InvalidRequestException(
                        String.format(ONBOARDING_NOT_TO_BE_VALIDATED.getMessage(), onboarding.getId()),
                        ONBOARDING_NOT_TO_BE_VALIDATED.getCode()));
    }

    public Uni<Onboarding> checkIfCompleted(Onboarding onboarding) {
        return OnboardingStatus.COMPLETED.equals(onboarding.getStatus())
                ? Uni.createFrom().item(onboarding)
                : Uni.createFrom().failure(new InvalidRequestException(
                        String.format(ONBOARDING_NOT_COMPLETED.getMessage(), onboarding.getId(), onboarding.getStatus()),
                        ONBOARDING_NOT_COMPLETED.getCode()));
    }

    public static boolean isOnboardingExpired(LocalDateTime dateTime) {
        LocalDateTime now = LocalDateTime.now();
        return Objects.nonNull(dateTime) && (now.isEqual(dateTime) || now.isAfter(dateTime));
    }

    // -------------------------------------------------------------------------
    // Costruzione response paginata
    // -------------------------------------------------------------------------

    public Uni<OnboardingGetResponse> constructOnboardingGetResponse(Tuple2<List<Onboarding>, Long> tuple) {
        return convertOnboardingListToResponse(tuple.getItem1())
                .onItem().transform(items -> {
                    OnboardingGetResponse response = new OnboardingGetResponse();
                    response.setCount(tuple.getItem2());
                    response.setItems(items);
                    return response;
                });
    }

    public Uni<List<OnboardingGet>> convertOnboardingListToResponse(List<Onboarding> onboardings) {
        return Multi.createFrom().iterable(onboardings)
                .onItem().transformToUniAndConcatenate(onboardingResponseFactory::toGetResponse)
                .collect().asList();
    }

    public io.quarkus.mongodb.panache.reactive.ReactivePanacheQuery<Onboarding> runQuery(Document query, Document sort) {
        return Onboarding.find(query, sort);
    }

    // -------------------------------------------------------------------------
    // Aggiornamenti stato onboarding
    // -------------------------------------------------------------------------

    public Uni<Long> updateReasonForRejectAndUpdateStatus(String onboardingId, ReasonRequest reasonForReject) {
        Map<String, Object> params = QueryUtils.createMapForOnboardingReject(
                reasonForReject, OnboardingStatus.REJECTED.name());
        Document query = QueryUtils.buildUpdateDocument(params);
        return performUpdate(onboardingId, query);
    }

    public Uni<Long> updateApproverUserUuid(String onboardingId, ApproveRequest approveRequest) {
        Map<String, Object> params = QueryUtils.createMapForOnboardingApprove(approveRequest);
        Document query = QueryUtils.buildUpdateDocument(params);
        return performUpdate(onboardingId, query);
    }

    public Uni<Long> updateOnboardingStatus(String id, Map<String, Object> queryParameter) {
        Document query = QueryUtils.buildUpdateDocument(queryParameter);
        return performUpdate(id, query);
    }

    public Uni<Long> updateOnboardingValues(String onboardingId, Onboarding onboarding) {
        Map<String, Object> params = QueryUtils.createMapForOnboardingUpdate(onboarding);
        Document query = QueryUtils.buildUpdateDocument(params);
        return performUpdate(onboardingId, query);
    }

    private Uni<Long> performUpdate(String id, Document query) {
        // Migration-phase concession for code outside an active request; see QueryUtils.
        if (queryUtils.currentTenantId().isEmpty()) {
            return Onboarding.update(query).where("_id", id)
                    .onItem().transformToUni(count -> {
                        if (count == 0) {
                            return Uni.createFrom().failure(new InvalidRequestException(
                                    String.format(ONBOARDING_NOT_FOUND_OR_ALREADY_DELETED.getMessage(), id)));
                        }
                        return Uni.createFrom().item(count);
                    });
        }
        return Onboarding.update(query).where(queryUtils.tenantScoped(new Document("_id", id)))
                .onItem().transformToUni(count -> {
                    if (count == 0) {
                        return Uni.createFrom().failure(new InvalidRequestException(
                                String.format(ONBOARDING_NOT_FOUND_OR_ALREADY_DELETED.getMessage(), id)));
                    }
                    return Uni.createFrom().item(count);
                });
    }

    // -------------------------------------------------------------------------
    // Utility
    // -------------------------------------------------------------------------

    public Uni<List<Onboarding>> findOnboardingsByFilters(CheckManagerRequest request) {
        return persistenceHelper.getOnboardingByFilters(
                        request.getTaxCode(), request.getSubunitCode(),
                        request.getOrigin(), request.getOriginId(), request.getProductId())
                .collect().asList();
    }

    public Uni<List<Onboarding>> getOnboardingListOrNull(List<Onboarding> onboardings) {
        return onboardings.isEmpty() ? Uni.createFrom().nullItem() : Uni.createFrom().item(onboardings);
    }

    public Document buildSortByCreatedAtDesc() {
        return QueryUtils.buildSortDocument(Onboarding.Fields.createdAt.name(), SortEnum.DESC);
    }
}

