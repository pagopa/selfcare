package it.pagopa.selfcare.institution.service;

import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoEntityBase;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheQuery;
import io.quarkus.panache.mock.PanacheMock;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import it.pagopa.selfcare.institution.entity.MailNotification;
import it.pagopa.selfcare.product.entity.Product;
import it.pagopa.selfcare.product.service.ProductService;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.openapi.quarkus.selfcare_user_json.api.InstitutionApi;
import org.openapi.quarkus.selfcare_user_json.model.OnboardedProductResponse;
import org.openapi.quarkus.selfcare_user_json.model.OnboardedProductState;
import org.openapi.quarkus.selfcare_user_json.model.UserInstitutionResponse;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@QuarkusTest
class InstitutionSendMailScheduledServiceImplTest {
    @InjectMock
    MailServiceImpl mailService;

    @InjectMock
    ProductService productService;

    @Inject
    InstitutionSendMailScheduledServiceImpl service;

    @RestClient
    @InjectMock
    InstitutionApi institutionApi;

    @ConfigProperty(name = "institution-send-mail.notification-query-size")
    Integer querySize;

    @ConfigProperty(name = "institution-send-mail.first-notification-path")
    String templateMailFirstNotification;

    @ConfigProperty(name = "institution-send-mail.notification-path")
    String templateMail;

    @Test
    void retrieveInstitutionFromMailNotificationAndSendMail() {
        final String institutionId = "institutionId";
        final List<String> productIds = List.of("prod-1", "prod-2");
        final String digitalAddress = "test@test.com";

        PanacheMock.mock(MailNotification.class);
        ReactivePanacheQuery<ReactivePanacheMongoEntityBase> query = Mockito.mock(ReactivePanacheQuery.class);
        when(MailNotification.find(any(), any(Object.class))).thenReturn(query);
        when(query.page(0, querySize)).thenReturn(query);
        when(query.hasNextPage()).thenReturn(Uni.createFrom().item(false));
        when(query.firstResult()).thenReturn(Uni.createFrom().item(getMailNotification(institutionId, digitalAddress,
                productIds, 1, Instant.now().minus(60, ChronoUnit.DAYS), Instant.now().minus(60, ChronoUnit.DAYS))));

        final Product product1 = new Product();
        product1.setTitle("product-title-1");
        when(productService.getProduct("prod-1")).thenReturn(product1);

        final Product product2 = new Product();
        product2.setTitle("product-title-2");
        when(productService.getProduct("prod-2")).thenReturn(product2);

        final OnboardedProductResponse userInst1Prod1 = getProduct("prod-1", OnboardedProductState.ACTIVE, LocalDateTime.now(), LocalDateTime.now());
        final OnboardedProductResponse userInst1Prod2 = getProduct("prod-2", OnboardedProductState.ACTIVE, LocalDateTime.now().minusDays(31), LocalDateTime.now());
        final OnboardedProductResponse userInst1Prod3 = getProduct("prod-2", OnboardedProductState.PENDING, LocalDateTime.now(), LocalDateTime.now());
        final UserInstitutionResponse userInst1 = getUserInstitution(institutionId, List.of(userInst1Prod1, userInst1Prod2, userInst1Prod3));

        final OnboardedProductResponse userInst2Prod1 = getProduct("prod-1", OnboardedProductState.ACTIVE, LocalDateTime.now().minusDays(10), LocalDateTime.now());
        final OnboardedProductResponse userInst2Prod2 = getProduct("prod-1", OnboardedProductState.DELETED, LocalDateTime.now(), LocalDateTime.now());
        final OnboardedProductResponse userInst2Prod3 = getProduct("prod-2", OnboardedProductState.SUSPENDED, LocalDateTime.now(), LocalDateTime.now());
        final OnboardedProductResponse userInst2Prod4 = getProduct("prod-2", OnboardedProductState.DELETED, LocalDateTime.now(), LocalDateTime.now().minusDays(31));
        final UserInstitutionResponse userInst2 = getUserInstitution(institutionId, List.of(userInst2Prod1, userInst2Prod2, userInst2Prod3, userInst2Prod4));

        when(institutionApi.institutionsInstitutionIdUserInstitutionsGet(institutionId, null, productIds, null, null, null))
                .thenReturn(Uni.createFrom().item(List.of(userInst1, userInst2)));

        final Map<String, String> expectedMailParameters = getExpectedMailParameters(
                institutionId,
                "Utenti aggiunti questo mese: <ul><li>2 utenti per il prodotto product-title-1</li><li>1 utenti per il prodotto product-title-2</li></ul>",
                "Utenti rimossi questo mese: <ul><li>1 utenti per il prodotto product-title-1</li></ul>"
        );

        Uni<Void> result = service.retrieveInstitutionFromMailNotificationAndSendMail();
        UniAssertSubscriber<Void> subscriber = result.subscribe().withSubscriber(UniAssertSubscriber.create());
        subscriber.assertCompleted();
        Mockito.verify(mailService, Mockito.times(1))
                .sendMail(Mockito.eq(List.of(digitalAddress)), Mockito.eq(templateMail), Mockito.eq(expectedMailParameters), Mockito.isNull());
    }

    @Test
    void shouldNotSendMail_whenOnboardingHappenToday() {
        final String institutionId = "institutionId";
        final List<String> productIds = List.of("prod-1", "prod-2");
        final String digitalAddress = "test@test.com";

        PanacheMock.mock(MailNotification.class);
        ReactivePanacheQuery<ReactivePanacheMongoEntityBase> query = Mockito.mock(ReactivePanacheQuery.class);
        when(MailNotification.find(any(), any(Object.class))).thenReturn(query);
        when(query.page(0, querySize)).thenReturn(query);
        when(query.hasNextPage()).thenReturn(Uni.createFrom().item(false));
        when(query.firstResult()).thenReturn(Uni.createFrom().item(getMailNotification(institutionId, digitalAddress,
                productIds, 1, Instant.now(), Instant.now().minus(60, ChronoUnit.DAYS))));

        Uni<Void> result = service.retrieveInstitutionFromMailNotificationAndSendMail();
        UniAssertSubscriber<Void> subscriber = result.subscribe().withSubscriber(UniAssertSubscriber.create());
        subscriber.assertCompleted();
        Mockito.verify(mailService, Mockito.times(0))
                .sendMail(Mockito.anyList(), Mockito.anyString(), Mockito.anyMap(), Mockito.anyString());
    }

    @Test
    void shouldNotSendMail_whenDigitalAddressIsMissing() {
        final String institutionId = "institutionId";
        final List<String> productIds = List.of("prod-1", "prod-2");
        final String digitalAddress = null;

        PanacheMock.mock(MailNotification.class);
        ReactivePanacheQuery<ReactivePanacheMongoEntityBase> query = Mockito.mock(ReactivePanacheQuery.class);
        when(MailNotification.find(any(), any(Object.class))).thenReturn(query);
        when(query.page(0, querySize)).thenReturn(query);
        when(query.hasNextPage()).thenReturn(Uni.createFrom().item(false));
        when(query.firstResult()).thenReturn(Uni.createFrom().item(getMailNotification(institutionId, digitalAddress,
                productIds, 1, Instant.now().minus(60, ChronoUnit.DAYS), Instant.now().minus(60, ChronoUnit.DAYS))));

        Uni<Void> result = service.retrieveInstitutionFromMailNotificationAndSendMail();
        UniAssertSubscriber<Void> subscriber = result.subscribe().withSubscriber(UniAssertSubscriber.create());
        subscriber.assertCompleted();
        Mockito.verify(mailService, Mockito.times(0))
                .sendMail(Mockito.anyList(), Mockito.anyString(), Mockito.anyMap(), Mockito.anyString());
    }

    @Test
    void shouldLogErrorAndContinueOnMailSendFailure() {
        final String institutionId = "institutionId";
        final List<String> productIds = List.of("prod-1", "prod-2");
        final String digitalAddress = "test@test.com";

        PanacheMock.mock(MailNotification.class);
        ReactivePanacheQuery<ReactivePanacheMongoEntityBase> query = Mockito.mock(ReactivePanacheQuery.class);
        when(MailNotification.find(any(), any(Object.class))).thenReturn(query);
        when(query.page(0, querySize)).thenReturn(query);
        when(query.hasNextPage()).thenReturn(Uni.createFrom().item(false));
        when(query.firstResult()).thenReturn(Uni.createFrom().item(getMailNotification(institutionId, digitalAddress,
                productIds, 1, Instant.now().minus(60, ChronoUnit.DAYS), Instant.now().minus(60, ChronoUnit.DAYS))));

        Mockito.doThrow(new RuntimeException("Mail send failed")).when(mailService).sendMail(Mockito.anyList(), Mockito.anyString(), Mockito.anyMap(), Mockito.anyString());

        // Execute
        Uni<Boolean> result = service.runQueryAndSendNotification(1L, 0);
        UniAssertSubscriber<Boolean> subscriber = result.subscribe().withSubscriber(UniAssertSubscriber.create());

        // Verify
        subscriber.assertItem(false);
    }

    @Test
    void shouldHandleNoPecNotificationsForCurrentModuleDay() {
        final int page = 0;
        PanacheMock.mock(MailNotification.class);
        ReactivePanacheQuery<ReactivePanacheMongoEntityBase> query = Mockito.mock(ReactivePanacheQuery.class);
        when(MailNotification.find(any(), any(Object.class)))
                .thenReturn(query);
        when(query.page(page, querySize)).thenReturn(query);
        when(query.hasNextPage()).thenReturn(Uni.createFrom().item(false));
        when(query.firstResult()).thenReturn(Uni.createFrom().nullItem());

        // Execute
        Uni<Boolean> result = service.runQueryAndSendNotification(0L, page);
        UniAssertSubscriber<Boolean> subscriber = result.subscribe().withSubscriber(UniAssertSubscriber.create());

        // Verify
        Mockito.verify(mailService, Mockito.never()).sendMail(Mockito.anyList(), Mockito.anyString(), Mockito.anyMap(), Mockito.anyString());
        subscriber.assertCompleted();
    }

    @Test
    void sendFirstMailTemplate() {
        final String institutionId = "institutionId";
        final List<String> productIds = List.of("prod-1");
        final String digitalAddress = "test@test.com";

        PanacheMock.mock(MailNotification.class);
        ReactivePanacheQuery<ReactivePanacheMongoEntityBase> query = Mockito.mock(ReactivePanacheQuery.class);
        when(MailNotification.find(any(), any(Object.class))).thenReturn(query);
        when(query.page(0, querySize)).thenReturn(query);
        when(query.hasNextPage()).thenReturn(Uni.createFrom().item(false));
        when(query.firstResult()).thenReturn(Uni.createFrom().item(getMailNotification(institutionId, digitalAddress,
                productIds, 1, Instant.now().minus(20, ChronoUnit.DAYS), Instant.now().minus(60, ChronoUnit.DAYS))));

        final Product product1 = new Product();
        product1.setTitle("product-title-1");
        when(productService.getProduct("prod-1")).thenReturn(product1);

        final OnboardedProductResponse userInst1Prod1 = getProduct("prod-1", OnboardedProductState.ACTIVE, LocalDateTime.now(), LocalDateTime.now());
        final UserInstitutionResponse userInst1 = getUserInstitution(institutionId, List.of(userInst1Prod1));

        when(institutionApi.institutionsInstitutionIdUserInstitutionsGet(institutionId, null, productIds, null, null, null))
                .thenReturn(Uni.createFrom().item(List.of(userInst1)));

        Uni<Void> result = service.retrieveInstitutionFromMailNotificationAndSendMail();
        UniAssertSubscriber<Void> subscriber = result.subscribe().withSubscriber(UniAssertSubscriber.create());
        subscriber.assertCompleted();

        Mockito.verify(mailService, Mockito.times(1))
                .sendMail(Mockito.anyList(), Mockito.eq(templateMailFirstNotification), Mockito.anyMap(), Mockito.any());
        Mockito.verify(mailService, Mockito.never())
                .sendMail(Mockito.anyList(), Mockito.eq(templateMail), Mockito.anyMap(), Mockito.any());
    }

    private MailNotification getMailNotification(String institutionId, String digitalAddress, List<String> productIds,
                                                 Integer moduleDayOfTheEpoch, Instant createdAt, Instant updatedAt) {
        final MailNotification mailNotification = new MailNotification();
        mailNotification.setInstitutionId(institutionId);
        mailNotification.setDigitalAddress(digitalAddress);
        mailNotification.setProductIds(productIds);
        mailNotification.setModuleDayOfTheEpoch(moduleDayOfTheEpoch);
        mailNotification.setCreatedAt(createdAt);
        mailNotification.setUpdatedAt(updatedAt);
        return mailNotification;
    }

    private UserInstitutionResponse getUserInstitution(String institutionId, List<OnboardedProductResponse> products) {
        final UserInstitutionResponse response = new UserInstitutionResponse();
        response.setInstitutionId(institutionId);
        response.setProducts(products);
        return response;
    }

    private OnboardedProductResponse getProduct(String productId, OnboardedProductState status,
                                                LocalDateTime createdAt, LocalDateTime updatedAt) {
        final OnboardedProductResponse response = new OnboardedProductResponse();
        response.setProductId(productId);
        response.setStatus(status);
        response.setCreatedAt(createdAt);
        response.setUpdatedAt(updatedAt);
        return response;
    }

    private Map<String, String> getExpectedMailParameters(String institutionId, String addedUsersList, String removedUsersList) {
        final Map<String, String> expectedMailParameters = new HashMap<>();
        expectedMailParameters.put("id_institution", institutionId);
        expectedMailParameters.put("added_users_list", addedUsersList);
        expectedMailParameters.put("removed_users_list", removedUsersList);
        return expectedMailParameters;
    }

}