package it.pagopa.selfcare.institution.service;

import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoEntityBase;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import it.pagopa.selfcare.institution.entity.MailNotification;
import it.pagopa.selfcare.product.entity.Product;
import it.pagopa.selfcare.product.service.ProductService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.openapi.quarkus.selfcare_user_json.api.InstitutionApi;
import org.openapi.quarkus.selfcare_user_json.model.OnboardedProductResponse;
import org.openapi.quarkus.selfcare_user_json.model.OnboardedProductState;
import org.openapi.quarkus.selfcare_user_json.model.UserInstitutionResponse;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@ApplicationScoped
public class InstitutionSendMailScheduledServiceImpl implements InstitutionSendMailScheduledService {

    private static final Logger log = LoggerFactory.getLogger(InstitutionSendMailScheduledServiceImpl.class);

    private static final String ID_INSTITUTION_PLACEHOLDER = "id_institution";
    private static final String ADDED_USERS_LIST_PLACEHOLDER = "added_users_list";
    private static final String REMOVED_USERS_LIST_PLACEHOLDER = "removed_users_list";

    private final MailServiceImpl mailService;
    private final String templateMail;
    private final String templateMailFirstNotification;
    private final ProductService productService;
    private final String startDate;
    private final Integer querySize;

    private final boolean sendAllNotification;
    private final Integer pecNotificationFrequency;

    @RestClient
    @Inject
    InstitutionApi userInstitutionApi;

    public InstitutionSendMailScheduledServiceImpl(MailServiceImpl mailService,
                                                   @ConfigProperty(name = "institution-send-mail.notification-path") String templateMail,
                                                   @ConfigProperty(name = "institution-send-mail.first-notification-path") String templateMailFirstNotification,
                                                   @ConfigProperty(name = "institution-send-mail.notification-start-date") String startDate,
                                                   ProductService productService,
                                                   @ConfigProperty(name = "institution-send-mail.notification-query-size") Integer querySize,
                                                   @ConfigProperty(name = "institution-send-mail.notification-send-all") boolean sendAllNotification,
                                                   @ConfigProperty(name = "institution-send-mail.scheduler.pec-notification-frequency") Integer pecNotificationFrequency) {
        this.mailService = mailService;
        this.templateMail = templateMail;
        this.templateMailFirstNotification = templateMailFirstNotification;
        this.productService = productService;
        this.startDate = startDate;
        this.querySize = querySize;
        this.sendAllNotification = sendAllNotification;
        this.pecNotificationFrequency = pecNotificationFrequency;
    }

    @Override
    public Uni<Void> retrieveInstitutionFromMailNotificationAndSendMail() {
        final long moduleDayOfTheEpoch = calculateModuleDayOfTheEpoch(pecNotificationFrequency);
        return Multi.createBy().repeating()
                .uni(AtomicInteger::new,
                        currentPage -> runQueryAndSendNotification(moduleDayOfTheEpoch, currentPage.getAndIncrement()))
                .withDelay(Duration.ofSeconds(30))
                .until(Boolean.FALSE::equals)
                .collect()
                .asList()
                .replaceWith(Uni.createFrom().voidItem());
    }

    public Uni<Boolean> runQueryAndSendNotification(Long moduleDayOfTheEpoch, int page) {
        var mailNotificationPage = MailNotification.find(MailNotification.FIELD_MODULE_DAY_OF_THE_EPOCH + "=?1", moduleDayOfTheEpoch)
                .page(page, querySize);

        Uni<List<ReactivePanacheMongoEntityBase>> uniResultEntity = sendAllNotification
                ? mailNotificationPage.list()
                : mailNotificationPage.firstResult()
                    .onItem().ifNotNull().transform(List::of)
                    .onItem().ifNull().continueWith(ArrayList::new);

        return uniResultEntity
                .onItem().invoke(mailNotifications -> log.info(String.format("[moduleDayOfTheEpoch=%d] Page %d processed, iteration has been found %d mail notifications", moduleDayOfTheEpoch, page, mailNotifications.size())))
                .onItem().transformToUni(this::retrieveMailNotificationListAndSendMail)
                .onItem().invoke(mailSize -> log.info(String.format("[moduleDayOfTheEpoch=%d] Page %d processed, iteration has been send %d mails", moduleDayOfTheEpoch, page, mailSize)))
                .replaceWith(mailNotificationPage.hasNextPage())
                .onFailure().invoke(throwable -> log.error(String.format("[moduleDayOfTheEpoch=%d] Error during send mail page %d processed, error=%s", moduleDayOfTheEpoch, page, throwable.getMessage())));
    }

    private Uni<Integer> retrieveMailNotificationListAndSendMail(List<ReactivePanacheMongoEntityBase> query) {
        return Multi.createFrom().iterable(query)
                .onItem().transform(MailNotification.class::cast)
                .onItem().transformToUniAndMerge(this::constructAndSendMail)
                .onFailure().invoke(throwable -> log.error("Error during send scheduled mail", throwable))
                .filter(isMailSent -> isMailSent)
                .collect().asList()
                .map(List::size);
    }

    private Uni<Boolean> constructAndSendMail(MailNotification mailNotification) {
        long dayDifference = ChronoUnit.DAYS.between(mailNotification.getCreatedAt(), Instant.now());

        //Onboarding happened today, we must not send mail
        if(dayDifference <= 0) {
            log.warn(String.format("Mail did not send for institution %s because onboarding happened today!", mailNotification.getInstitutionId()));
            return Uni.createFrom().item(false);
        }

        if(Objects.isNull(mailNotification.getDigitalAddress())) {
            log.warn(String.format("Mail did not send for institution %s because digitalAddress is empty!", mailNotification.getInstitutionId()));
            return Uni.createFrom().item(false);
        }

        return getInstitutionUsers(mailNotification.getInstitutionId(), mailNotification.getProductIds()).onItem().transformToUni(institutionUsers -> {
            final Map<String, Integer> addedUsersCount = getAddedUsersCount(institutionUsers);
            final Map<String, Integer> removedUsersCount = getRemovedUsersCount(institutionUsers);
            final Map<String, String> mailParameters = getMailParameters(mailNotification.getInstitutionId(), addedUsersCount, removedUsersCount);
            final String template = sendFirstMail(dayDifference) ? templateMailFirstNotification : templateMail;
            return mailService.sendMail(List.of(mailNotification.getDigitalAddress()), template, mailParameters, null)
                    .onItem().invoke(() -> log.info(String.format("Mail sent for institution %s", mailNotification.getInstitutionId())))
                    .map(v -> true)
                    .onFailure().recoverWithItem(false);
        });
    }

    private Map<String, String> getMailParameters(String institutionId, Map<String, Integer> addedUsersCount, Map<String, Integer> removedUsersCount) {
        Map<String, String> mailParameters = new HashMap<>();
        mailParameters.put(ID_INSTITUTION_PLACEHOLDER, institutionId);
        final String addedUsersListPlaceholder = addedUsersCount.isEmpty() ? "" : getUsersListPlaceholder("Utenti aggiunti questo mese: ", addedUsersCount);
        mailParameters.put(ADDED_USERS_LIST_PLACEHOLDER, addedUsersListPlaceholder);
        final String removedUsersListPlaceholder = removedUsersCount.isEmpty() ? "" : getUsersListPlaceholder("Utenti rimossi questo mese: ", removedUsersCount);
        mailParameters.put(REMOVED_USERS_LIST_PLACEHOLDER, removedUsersListPlaceholder);
        return mailParameters;
    }

    private String getUsersListPlaceholder(String prefix, Map<String, Integer> usersCount) {
        return String.format("%s<ul>%s</ul>", prefix, String.join("", usersCount.entrySet().stream()
                .map(e -> String.format("<li>%d utenti per il prodotto %s</li>",
                        e.getValue(), Optional.ofNullable(productService.getProduct(e.getKey())).map(Product::getTitle).orElse("")))
                .toList()));
    }

    private Map<String, Integer> getAddedUsersCount(List<UserInstitutionResponse> userInstitutionResponses) {
        final LocalDateTime xDaysAgo = LocalDateTime.now().minusDays(pecNotificationFrequency);
        return userInstitutionResponses.stream()
                .flatMap(u -> u.getProducts().stream())
                .filter(p -> (OnboardedProductState.ACTIVE.equals(p.getStatus()) || OnboardedProductState.SUSPENDED.equals(p.getStatus())) && p.getCreatedAt() != null && p.getCreatedAt().isAfter(xDaysAgo))
                .collect(Collectors.groupingBy(OnboardedProductResponse::getProductId, Collectors.summingInt(p -> 1)));
    }

    private Map<String, Integer> getRemovedUsersCount(List<UserInstitutionResponse> userInstitutionResponses) {
        final LocalDateTime xDaysAgo = LocalDateTime.now().minusDays(pecNotificationFrequency);
        return userInstitutionResponses.stream()
                .flatMap(u -> u.getProducts().stream())
                .filter(p -> OnboardedProductState.DELETED.equals(p.getStatus()) && p.getUpdatedAt() != null && p.getUpdatedAt().isAfter(xDaysAgo))
                .collect(Collectors.groupingBy(OnboardedProductResponse::getProductId, Collectors.summingInt(p -> 1)));
    }

    private Uni<List<UserInstitutionResponse>> getInstitutionUsers(String institutionId, List<String> productIds) {
        return Optional.ofNullable(productIds)
                .filter(products -> !products.isEmpty())
                .map(products -> userInstitutionApi.institutionsInstitutionIdUserInstitutionsGet(
                        institutionId,
                        null,
                        products,
                        null,
                        null,
                        null
                ))
                .orElse(Uni.createFrom().item(Collections.emptyList()));
    }

    private Long calculateModuleDayOfTheEpoch(Integer sendingFrequency) {
        LocalDate date = LocalDate.parse(startDate);
        LocalDate now = LocalDate.now();
        long dayDifference = ChronoUnit.DAYS.between(date, now);
        return dayDifference % sendingFrequency;
    }

    private boolean sendFirstMail(long dayDifference) {
        return dayDifference <= pecNotificationFrequency;
    }
}
