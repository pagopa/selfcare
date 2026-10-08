package it.pagopa.selfcare.onboarding.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import it.pagopa.selfcare.onboarding.client.UserRegistryRestClient;
import it.pagopa.selfcare.onboarding.client.model.EmbeddedExternalId;
import it.pagopa.selfcare.onboarding.client.model.RegistryUser;
import it.pagopa.selfcare.onboarding.mapper.UserMapper;
import it.pagopa.selfcare.onboarding.mapper.UserMapperImpl;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserRegistryServiceTest {

    private static final List<String> FIELD_LIST = List.of(UserRegistryService.USERS_FIELD_LIST);

    @Mock UserRegistryRestClient restClient;
    @Spy UserMapper mapper = new UserMapperImpl();
    @InjectMocks UserRegistryService service;

    @Test
    void searchUser_keepsTheQueryFieldsAndMapsTheUuid() {
        RegistryUser found = new RegistryUser();
        UUID id = UUID.randomUUID();
        found.setId(id.toString());
        when(restClient.search(any(), eq(FIELD_LIST))).thenReturn(found);

        var result = service.searchUser("tax-code");

        assertEquals(id, result.getId());
        ArgumentCaptor<EmbeddedExternalId> request = ArgumentCaptor.forClass(EmbeddedExternalId.class);
        verify(restClient).search(request.capture(), eq(FIELD_LIST));
        assertEquals("tax-code", request.getValue().getFiscalCode());
        verify(mapper).toUserId(found);
        verifyNoMoreInteractions(restClient);
    }

    @Test
    void searchUser_keepsANonNullWrapperWhenTheSearchReturnsNull() {
        var result = service.searchUser("tax-code");

        assertNotNull(result);
        assertNull(result.getId());
        verify(restClient).search(new EmbeddedExternalId("tax-code"), FIELD_LIST);
        verify(mapper).toUserId(null);
        verifyNoMoreInteractions(restClient);
    }

    @Test
    void searchUser_doesNotHideInvalidUuidValues() {
        RegistryUser found = new RegistryUser();
        found.setId("invalid-uuid");
        when(restClient.search(any(), eq(FIELD_LIST))).thenReturn(found);

        assertThrows(IllegalArgumentException.class, () -> service.searchUser("tax-code"));

        verify(restClient).search(new EmbeddedExternalId("tax-code"), FIELD_LIST);
        verify(mapper).toUserId(found);
        verifyNoMoreInteractions(restClient);
    }
}
