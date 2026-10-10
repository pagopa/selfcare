package it.pagopa.selfcare.onboarding.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import it.pagopa.selfcare.onboarding.client.PartyProcessRestClient;
import it.pagopa.selfcare.onboarding.client.model.OnboardingData;
import it.pagopa.selfcare.onboarding.client.model.OnboardingInstitutionRequest;
import it.pagopa.selfcare.onboarding.client.model.Product;
import it.pagopa.selfcare.onboarding.client.model.InstitutionInfo;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import io.smallrye.mutiny.subscription.UniEmitter;
import org.openapi.quarkus.user_json.model.UserInstitutionResponse;
import org.openapi.quarkus.onboarding_json.model.GetInstitutionRequest;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import it.pagopa.selfcare.onboarding.mapper.InstitutionMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openapi.quarkus.onboarding_json.api.InstitutionControllerApi;
import org.openapi.quarkus.user_json.api.UserControllerApi;

@ExtendWith(MockitoExtension.class)
class PartyServiceTest {

    @Mock PartyProcessRestClient restClient;
    @Mock InstitutionMapper mapper;
    @Mock UserControllerApi userApi;
    @Mock InstitutionControllerApi institutionApi;
    @InjectMocks PartyService service;

    @Test
    void onboardingOrganization_mapsOnceAndForwardsTheExactRequestOnce() {
        OnboardingData data = new OnboardingData();
        OnboardingInstitutionRequest request = new OnboardingInstitutionRequest();
        when(mapper.toOnboardingInstitutionRequest(data)).thenReturn(request);

        service.onboardingOrganization(data);

        verify(mapper).toOnboardingInstitutionRequest(data);
        verify(restClient).onboardingOrganization(request);
        verifyNoMoreInteractions(mapper, restClient);
        verifyNoInteractions(userApi, institutionApi);
    }

    @Test
    void onboardingOrganization_checksRequiredDataBeforeMappingOrCallingDownstream() {
        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> service.onboardingOrganization(null));

        assertEquals("Onboarding data is required", failure.getMessage());
        verifyNoInteractions(mapper, restClient, userApi, institutionApi);
    }

    @Test
    @Timeout(value = 2, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void childLookupPrecedesParentAndInstitutionLookupEvenWithoutAllowedTypes() {
        // given
        Product product = new Product();
        product.setId("child");
        product.setParentId("parent");
        AtomicReference<UniEmitter<? super List<UserInstitutionResponse>>> child = new AtomicReference<>();
        AtomicReference<UniEmitter<? super List<UserInstitutionResponse>>> parent = new AtomicReference<>();
        UserInstitutionResponse onboarded = new UserInstitutionResponse();
        onboarded.setInstitutionId("already-onboarded");
        UserInstitutionResponse available = new UserInstitutionResponse();
        available.setInstitutionId("available");
        InstitutionInfo mapped = new InstitutionInfo();
        mapped.setId("available");
        GetInstitutionRequest request = new GetInstitutionRequest();
        request.setInstitutionIds(List.of("available"));
        when(userApi.usersGet(null, null, null, List.of("child"), null, 500, List.of("ACTIVE"), "uid"))
                .thenReturn(Uni.createFrom().<List<UserInstitutionResponse>>emitter(child::set));
        when(userApi.usersGet(null, null, null, List.of("parent"), null, 500, List.of("ACTIVE"), "uid"))
                .thenReturn(Uni.createFrom().<List<UserInstitutionResponse>>emitter(parent::set));
        when(mapper.toInstitutionInfo(available)).thenReturn(mapped);
        when(mapper.toGetInstitutionRequest(List.of(mapped))).thenReturn(request);
        when(institutionApi.getInstitutions(request)).thenReturn(Uni.createFrom().item(List.of()));

        // when
        UniAssertSubscriber<List<InstitutionInfo>> result = service.getInstitutionsByUser(product, "uid")
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // then
        result.assertNotTerminated();
        verify(userApi, never()).usersGet(null, null, null, List.of("parent"), null, 500, List.of("ACTIVE"), "uid");
        verifyNoInteractions(mapper, institutionApi);
        child.get().complete(List.of(onboarded));
        result.assertNotTerminated();
        verifyNoInteractions(mapper, institutionApi);
        parent.get().complete(List.of(onboarded, available));
        result.assertCompleted().assertItem(List.of(mapped));
        verify(institutionApi).getInstitutions(request);
        verify(mapper, never()).toInstitutionInfo(onboarded);
        verifyNoInteractions(restClient);
    }

    @Test
    void emptyUserInstitutionsDoNotTriggerAnInstitutionLookup() {
        // given
        Product product = new Product();
        product.setId("prod-test");
        when(userApi.usersGet(null, null, null, List.of("prod-test"), null, 500, List.of("ACTIVE"), "uid"))
                .thenReturn(Uni.createFrom().item(List.of()));

        // when
        List<InstitutionInfo> result = service.getInstitutionsByUser(product, "uid").await().indefinitely();

        // then
        assertEquals(List.of(), result);
        verifyNoInteractions(mapper, institutionApi, restClient);
    }
}
