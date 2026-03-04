package it.pagopa.selfcare.onboarding.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import io.quarkus.mongodb.panache.reactive.ReactivePanacheQuery;
import io.quarkus.panache.mock.PanacheMock;
import io.quarkus.test.InjectMock;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.mongodb.MongoTestResource;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.controller.response.InstitutionResponse;
import it.pagopa.selfcare.onboarding.entity.Institution;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.service.impl.InstitutionServiceDefault;
import jakarta.inject.Inject;
import java.util.List;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.openapi.quarkus.core_json.api.InstitutionApi;
import org.openapi.quarkus.core_json.model.InstitutionsResponse;

@QuarkusTest
@QuarkusTestResource(value = MongoTestResource.class, restrictToAnnotatedClass = true)
class InstitutionServiceDefaultTest {

    @Inject
    InstitutionServiceDefault institutionService;

    @InjectMock
    @RestClient
    InstitutionApi institutionApi;


    @Test
    void getInstitutions() {
        Onboarding onboarding = new Onboarding();
        onboarding.setInstitution(dummyInstitution());
        PanacheMock.mock(Onboarding.class);
        ReactivePanacheQuery query = Mockito.mock(ReactivePanacheQuery.class);
        when(query.stream()).thenReturn(Multi.createFrom().item(onboarding));
        when(Onboarding.find(any())).thenReturn(query);
        final List<String> institutionIds = List.of("institutionId");

        AssertSubscriber<InstitutionResponse> subscriber = institutionService
                .getInstitutions(institutionIds)
                .subscribe().withSubscriber(AssertSubscriber.create(20));

        // Attendi il completamento e verifica i risultati
        subscriber.awaitCompletion().assertCompleted();
        List<InstitutionResponse> resultList = subscriber.getItems();

        // Verifica il risultato
        assertNotNull(resultList);
        assertFalse(resultList.isEmpty());
        resultList.forEach(actual -> {
            assertNotNull(actual);
            assertEquals(onboarding.getInstitution().getId(), actual.getId());
        });
    }

    @Test
    @RunOnVertxContext
    void getInstitutions_withEmptyIds(UniAsserter asserter) {
        final List<String> institutionIds = List.of();
        PanacheMock.mock(Onboarding.class);
        asserter.execute(() -> institutionService.getInstitutions(institutionIds));
        PanacheMock.verifyNoInteractions(Onboarding.class);
    }

    @Test
    @RunOnVertxContext
    void getInstitutions_withNullIds(UniAsserter asserter) {
        final List<String> institutionIds = null;
        PanacheMock.mock(Onboarding.class);
        asserter.execute(() -> institutionService.getInstitutions(institutionIds));
        PanacheMock.verifyNoInteractions(Onboarding.class);
    }

    private Institution dummyInstitution() {
        Institution institution = new Institution();
        institution.setId("institutionId");
        institution.setInstitutionType(InstitutionType.GSP);
        return institution;
    }

    @Test
    void getInstitutionsUsingGETTest() {
        // given
        InstitutionsResponse response = mock(InstitutionsResponse.class);
        when(institutionApi.getInstitutionsUsingGET(
                any(), any(), any(), any(), any(), any()))
                .thenReturn(Uni.createFrom().item(response));

        // when
        Uni<InstitutionsResponse> uni = institutionService.getInstitutionsUsingGET(
                "", "", "", "", "", false
        );

        // then
        UniAssertSubscriber<InstitutionsResponse> sub = uni.subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        sub.assertCompleted().assertItem(response);

        verify(institutionApi).getInstitutionsUsingGET(
                eq(""), eq(""), eq(""), eq(""), eq(""), eq(false)
        );

        verifyNoMoreInteractions(institutionApi);
    }

}
