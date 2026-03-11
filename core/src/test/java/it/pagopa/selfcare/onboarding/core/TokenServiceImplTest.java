package it.pagopa.selfcare.onboarding.core;


import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.onboarding.connector.api.OnboardingMsConnector;
import it.pagopa.selfcare.onboarding.connector.api.PartyConnector;
import it.pagopa.selfcare.onboarding.connector.model.onboarding.OnboardingData;
import it.pagopa.selfcare.onboarding.connector.model.onboarding.User;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatusCode;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TokenServiceImplTest {

    @InjectMocks
    private TokenServiceImpl tokenService;

    @Mock
    private PartyConnector partyConnector;

    @Mock
    private OnboardingMsConnector onboardingMsConnector;


    @Test
    void shouldNotCompleteTokenV2WhenIdIsNull() {
        Executable executable = () -> tokenService.completeTokenV2(null, null);
        //then
        Exception e = Assertions.assertThrows(IllegalArgumentException.class, executable);
        Assertions.assertEquals("TokenId is required", e.getMessage());
        Mockito.verifyNoInteractions(partyConnector);
    }

    @Test
    void shouldCompleteTokenV2() throws IOException {
        //given
        String tokenId = "example";
        MockMultipartFile mockMultipartFile = new MockMultipartFile("example", new ByteArrayInputStream("example".getBytes(StandardCharsets.UTF_8)));
        doNothing().when(onboardingMsConnector).onboardingTokenComplete(anyString(), any());
        // when
        tokenService.completeTokenV2(tokenId, mockMultipartFile);
        //then
        Mockito.verify(onboardingMsConnector, Mockito.times(1))
                .onboardingTokenComplete(tokenId, mockMultipartFile);
    }

    @Test
    void shouldNotCompleteOnboardingUsersWhenIdIsNull() {
        Executable executable = () -> tokenService.completeOnboardingUsers(null, null);
        //then
        Exception e = Assertions.assertThrows(IllegalArgumentException.class, executable);
        Assertions.assertEquals("OnboardingId is required", e.getMessage());
        Mockito.verifyNoInteractions(partyConnector);
    }

    @Test
    void shouldCompleteOnboardingUsers() throws IOException {
        //given
        String tokenId = "example";
        MockMultipartFile mockMultipartFile = new MockMultipartFile("example", new ByteArrayInputStream("example".getBytes(StandardCharsets.UTF_8)));
        doNothing().when(onboardingMsConnector).onboardingUsersComplete(anyString(), any());
        // when
        tokenService.completeOnboardingUsers(tokenId, mockMultipartFile);
        //then
        Mockito.verify(onboardingMsConnector, Mockito.times(1))
                .onboardingUsersComplete(tokenId, mockMultipartFile);
    }

    @Test
    void verifyOnboarding() {
        //given
        final String onboardingId = "onboardingId";
        // when
        tokenService.verifyOnboarding(onboardingId);
        //then
        Mockito.verify(onboardingMsConnector, Mockito.times(1))
                .getOnboarding(onboardingId);
    }

    @Test
    void onboardingWithUserInfo() {
        //given
        final String onboardingId = "onboardingId";
        // when
        tokenService.getOnboardingWithUserInfo(onboardingId);
        //then
        Mockito.verify(onboardingMsConnector, Mockito.times(1))
                .getOnboardingWithUserInfo(onboardingId);
    }

    @Test
    void approveOnboarding() {
        //given
        String onboardingId = "example";
        doNothing().when(onboardingMsConnector).approveOnboarding(onboardingId);
        // when
        tokenService.approveOnboarding(onboardingId);
        //then
        Mockito.verify(onboardingMsConnector, Mockito.times(1))
                .approveOnboarding(onboardingId);
    }

    @Test
    void rejectOnboarding() {
        //given
        final String onboardingId = "example";
        final String reason = "reason";
        doNothing().when(onboardingMsConnector).rejectOnboarding(onboardingId, reason);
        // when
        tokenService.rejectOnboarding(onboardingId, reason);
        //then
        Mockito.verify(onboardingMsConnector, Mockito.times(1))
                .rejectOnboarding(onboardingId, reason);
    }

    @Test
    void getContract() {
        //given
        final String onboardingId = "onboardingId";
        // when
        tokenService.getContract(onboardingId);
        //then
        Mockito.verify(onboardingMsConnector, Mockito.times(1))
                .getContract(onboardingId);
    }

    @Test
    void getTemplateAttachment() {
        //given
        final String onboardingId = "onboardingId";
        final String filename = "filename";
        // when
        tokenService.getTemplateAttachment(onboardingId, filename);
        //then
        Mockito.verify(onboardingMsConnector, Mockito.times(1))
                .getTemplateAttachment(onboardingId, filename);
    }

    @Test
    void headAttachmentTest() {
        //given
        final String onboardingId = "onboardingId";
        final String filename = "filename";

        when(onboardingMsConnector.headAttachment(onboardingId, filename)).thenReturn(HttpStatusCode.valueOf(204));

        // when
        HttpStatusCode result = tokenService.headAttachment(onboardingId, filename);

        //then
        Mockito.verify(onboardingMsConnector, Mockito.times(1))
                .headAttachment(eq("onboardingId"), eq("filename"));
        assertTrue(result.is2xxSuccessful());
        assertFalse(result.is4xxClientError());
        assertFalse(result.is5xxServerError());
    }

    @Test
    void headAttachmentTest_shouldReturnFalse_whenFileNotExist() {
        //given
        final String onboardingId = "onboardingId";
        final String filename = "filename";

        when(onboardingMsConnector.headAttachment(onboardingId, filename)).thenReturn(HttpStatusCode.valueOf(404));

        // when
        HttpStatusCode result = tokenService.headAttachment(onboardingId, filename);

        //then
        Mockito.verify(onboardingMsConnector, Mockito.times(1))
                .headAttachment(eq("onboardingId"), eq("filename"));
        assertFalse(result.is2xxSuccessful());
        assertTrue(result.is4xxClientError());
        assertFalse(result.is5xxServerError());
    }

    @Test
    void headAttachmentTest_shouldReturnFalse_whenException() {
        //given
        final String onboardingId = "onboardingId";
        final String filename = "filename";

        when(onboardingMsConnector.headAttachment(onboardingId, filename)).thenReturn(HttpStatusCode.valueOf(500));

        // when
        HttpStatusCode result = tokenService.headAttachment(onboardingId, filename);

        //then
        Mockito.verify(onboardingMsConnector, Mockito.times(1))
                .headAttachment(eq("onboardingId"), eq("filename"));
        assertFalse(result.is2xxSuccessful());
        assertFalse(result.is4xxClientError());
        assertTrue(result.is5xxServerError());
    }

    @Test
    void uploadAttachment() throws IOException {
        //given
        final String onboardingId = "onboardingId";
        final String filename = "filename";
        MockMultipartFile mockMultipartFile = new MockMultipartFile("example", new ByteArrayInputStream("example".getBytes(StandardCharsets.UTF_8)));
        // when
        tokenService.uploadAttachment(onboardingId, mockMultipartFile, filename);
        //then
        Mockito.verify(onboardingMsConnector, Mockito.times(1))
                .uploadAttachment(onboardingId, mockMultipartFile, filename);
    }


    @Test
    void getAggregatesCsv() {
        //given
        final String onboardingId = "onboardingId";
        final String productId = "productId";
        // when
        tokenService.getAggregatesCsv(onboardingId, productId);
        //then
        Mockito.verify(onboardingMsConnector, Mockito.times(1))
                .getAggregatesCsv(onboardingId, productId);
    }

    @Test
    void verifyAllowedUserByRoleTest() {
        //given
        final String onboardingId = "onboardingId";
        final String uid = "uid1";
        OnboardingData onboardingData = new OnboardingData();

        User userManager = new User();
        userManager.setRole(PartyRole.MANAGER);
        userManager.setId(uid);

        User userDelegate = new User();
        userDelegate.setRole(PartyRole.DELEGATE);
        userDelegate.setId("uid2");

        onboardingData.setUsers(List.of(userManager, userDelegate));

        when(onboardingMsConnector.getOnboardingWithUserInfo(anyString())).thenReturn(onboardingData);

        // when
        boolean result = tokenService.verifyAllowedUserByRole(onboardingId, uid);

        //then
        assertTrue(result);
        Mockito.verify(onboardingMsConnector, Mockito.times(1))
            .getOnboardingWithUserInfo(anyString());
    }

    @Test
    void verifyAllowedUserByRoleTest_CaseKO() {
        //given
        final String onboardingId = "onboardingId";
        final String uid = "uid1";

        OnboardingData onboardingData = new OnboardingData();

        User user = new User();
        user.setRole(PartyRole.DELEGATE);
        user.setId("uid2");

        onboardingData.setUsers(List.of(user));

        when(onboardingMsConnector.getOnboardingWithUserInfo(anyString())).thenReturn(onboardingData);

        // when
        boolean result = tokenService.verifyAllowedUserByRole(onboardingId, uid);

        //then
        assertFalse(result);
        Mockito.verify(onboardingMsConnector, Mockito.times(1))
            .getOnboardingWithUserInfo(anyString());
    }
}
