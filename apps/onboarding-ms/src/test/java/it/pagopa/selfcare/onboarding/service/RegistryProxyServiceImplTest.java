package it.pagopa.selfcare.onboarding.service;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.service.impl.RegistryProxyServiceImpl;
import org.junit.jupiter.api.Test;
import org.openapi.quarkus.party_registry_proxy_json.api.UoApi;
import org.openapi.quarkus.party_registry_proxy_json.model.UOResource;

class RegistryProxyServiceImplTest {

    @Test
    void findUoByRecipientCode_shouldDelegateToUoApi() {
        //given
        UoApi uoApi = mock(UoApi.class);
        RegistryProxyServiceImpl service = new RegistryProxyServiceImpl(uoApi);
        UOResource expected = new UOResource();
        expected.setCodiceIpa("IPA001");
        Uni<UOResource> expectedUni = Uni.createFrom().item(expected);
        when(uoApi.findByUnicodeUsingGET1(eq("recipient-001"), isNull())).thenReturn(expectedUni);

        //when
        Uni<UOResource> result = service.findUoByRecipientCode("recipient-001", null);

        //then
        assertSame(expectedUni, result);
        verify(uoApi).findByUnicodeUsingGET1("recipient-001", null);
    }

    @Test
    void findUoByRecipientCode_shouldDelegateToUoApiWithCategories() {
        //given
        UoApi uoApi = mock(UoApi.class);
        RegistryProxyServiceImpl service = new RegistryProxyServiceImpl(uoApi);
        UOResource expected = new UOResource();
        expected.setCodiceIpa("IPA002");
        Uni<UOResource> expectedUni = Uni.createFrom().item(expected);
        when(uoApi.findByUnicodeUsingGET1(eq("recipient-002"), eq("PA,PSP"))).thenReturn(expectedUni);

        //when
        Uni<UOResource> result = service.findUoByRecipientCode("recipient-002", "PA,PSP");

        //then
        assertSame(expectedUni, result);
        verify(uoApi).findByUnicodeUsingGET1("recipient-002", "PA,PSP");
    }
}
