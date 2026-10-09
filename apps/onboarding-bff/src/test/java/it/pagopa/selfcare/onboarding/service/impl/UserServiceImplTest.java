package it.pagopa.selfcare.onboarding.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.UniEmitter;
import it.pagopa.selfcare.onboarding.client.model.Certification;
import it.pagopa.selfcare.onboarding.client.model.CertifiedField;
import it.pagopa.selfcare.onboarding.client.model.InstitutionUpdate;
import it.pagopa.selfcare.onboarding.client.model.ManagerVerification;
import it.pagopa.selfcare.onboarding.client.model.OnboardingData;
import it.pagopa.selfcare.onboarding.client.model.RegistryUser;
import it.pagopa.selfcare.onboarding.client.model.User;
import it.pagopa.selfcare.onboarding.client.model.UserId;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.onboarding.exception.InvalidUserFieldsException;
import it.pagopa.selfcare.onboarding.exception.OnboardingNotAllowedException;
import it.pagopa.selfcare.onboarding.exception.ResourceNotFoundException;
import it.pagopa.selfcare.onboarding.mapper.OnboardingMapper;
import it.pagopa.selfcare.onboarding.service.OnboardingService;
import it.pagopa.selfcare.onboarding.util.PgManagerVerifier;
import jakarta.ws.rs.WebApplicationException;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openapi.quarkus.onboarding_json.model.CheckManagerRequest;
import org.openapi.quarkus.onboarding_json.model.OnboardingGet;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    private static final EnumSet<RegistryUser.Fields> FIELDS = EnumSet.of(RegistryUser.Fields.name, RegistryUser.Fields.familyName);
    private static final String MISMATCH = "the value does not match with the certified data";

    @InjectMocks
    private UserServiceImpl userService;

    @Mock
    private UserRegistryService userRegistryClient;

    @Mock
    private OnboardingService onboardingMsClient;

    @Mock
    private OnboardingMapper onboardingMapper;

    @Mock
    private PgManagerVerifier pgManagerVerifier;

    @Test
    void validate_nullUser() {
        // given / when
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> userService.validate(null));

        // then the precondition fails at once, before any lookup
        assertEquals("An user is required", e.getMessage());
        verifyNoInteractions(userRegistryClient);
    }

    @Test
    void validate_userNotFoundInRegistryIsAccepted() {
        // given
        User user = user("Mario", "Rossi");
        when(userRegistryClient.search("TAX", FIELDS)).thenReturn(Optional.empty());

        // when
        userService.validate(user).await().indefinitely();

        // then
        verify(userRegistryClient).search("TAX", FIELDS);
        verifyNoMoreInteractions(userRegistryClient);
    }

    @Test
    void validate_registryLookupStartsOnlyWhenSubscribedAndRunsOnAWorker() {
        // given
        AtomicReference<Thread> lookupThread = new AtomicReference<>();
        when(userRegistryClient.search("TAX", FIELDS)).thenAnswer(invocation -> {
            lookupThread.set(Thread.currentThread());
            return Optional.empty();
        });

        // when the validation is only assembled
        Uni<Void> validation = userService.validate(user("Mario", "Rossi"));

        // then nothing is looked up
        verifyNoInteractions(userRegistryClient);

        // when it is subscribed
        validation.await().indefinitely();

        // then the blocking registry client was not called on the subscribing thread
        assertNotNull(lookupThread.get());
        assertNotSame(Thread.currentThread(), lookupThread.get());
    }

    @Test
    void validate_registryFailureIsPropagated() {
        // given
        IllegalStateException failure = new IllegalStateException("registry down");
        when(userRegistryClient.search("TAX", FIELDS)).thenThrow(failure);
        Uni<Void> validation = userService.validate(user("Mario", "Rossi"));

        // when / then
        assertSame(failure, assertThrows(IllegalStateException.class, () -> validation.await().indefinitely()));
    }

    @Test
    void validate_notCertifiedFieldsAreNeverCompared() {
        // given
        RegistryUser found = new RegistryUser();
        found.setName(certified(Certification.NONE, "Other"));
        found.setFamilyName(certified(Certification.NONE, "Other"));
        when(userRegistryClient.search("TAX", FIELDS)).thenReturn(Optional.of(found));

        // when
        userService.validate(user("Mario", "Rossi")).await().indefinitely();

        // then
        verify(userRegistryClient).search("TAX", FIELDS);
    }

    @Test
    void validate_missingCertifiedFieldsAreAccepted() {
        // given
        when(userRegistryClient.search("TAX", FIELDS)).thenReturn(Optional.of(new RegistryUser()));

        // when / then
        userService.validate(user("Mario", "Rossi")).await().indefinitely();
    }

    @Test
    void validate_certifiedValuesAreComparedIgnoringCase() {
        // given
        RegistryUser found = new RegistryUser();
        found.setName(certified(Certification.SPID, "Mario"));
        found.setFamilyName(certified(Certification.SPID, "Rossi"));
        when(userRegistryClient.search("TAX", FIELDS)).thenReturn(Optional.of(found));

        // when / then
        userService.validate(user("mario", "ROSSI")).await().indefinitely();
    }

    @Test
    void validate_nameMismatchIsReported() {
        // given
        RegistryUser found = new RegistryUser();
        found.setName(certified(Certification.SPID, "different value"));
        found.setFamilyName(certified(Certification.SPID, "Rossi"));
        when(userRegistryClient.search("TAX", FIELDS)).thenReturn(Optional.of(found));
        Uni<Void> validation = userService.validate(user("Mario", "Rossi"));

        // when
        InvalidUserFieldsException e = assertThrows(InvalidUserFieldsException.class,
                () -> validation.await().indefinitely());

        // then
        assertNotNull(e.getInvalidFields());
        assertEquals(1, e.getInvalidFields().size());
        assertEquals("name", e.getInvalidFields().get(0).getName());
        assertEquals(MISMATCH, e.getInvalidFields().get(0).getReason());
    }

    @Test
    void validate_surnameMismatchIsReported() {
        // given
        RegistryUser found = new RegistryUser();
        found.setName(certified(Certification.SPID, "Mario"));
        found.setFamilyName(certified(Certification.SPID, "different value"));
        when(userRegistryClient.search("TAX", FIELDS)).thenReturn(Optional.of(found));
        Uni<Void> validation = userService.validate(user("Mario", "Rossi"));

        // when
        InvalidUserFieldsException e = assertThrows(InvalidUserFieldsException.class,
                () -> validation.await().indefinitely());

        // then
        assertEquals(1, e.getInvalidFields().size());
        assertEquals("surname", e.getInvalidFields().get(0).getName());
        assertEquals(MISMATCH, e.getInvalidFields().get(0).getReason());
    }

    @Test
    void validate_bothMismatchesAreReportedNameFirst() {
        // given
        RegistryUser found = new RegistryUser();
        found.setName(certified(Certification.SPID, "x"));
        found.setFamilyName(certified(Certification.SPID, "y"));
        when(userRegistryClient.search("TAX", FIELDS)).thenReturn(Optional.of(found));
        Uni<Void> validation = userService.validate(user("Mario", "Rossi"));

        // when
        InvalidUserFieldsException e = assertThrows(InvalidUserFieldsException.class,
                () -> validation.await().indefinitely());

        // then
        assertEquals(List.of("name", "surname"),
                e.getInvalidFields().stream().map(InvalidUserFieldsException.InvalidField::getName).toList());
    }

    @Test
    void onboardingUsers_delegatesAndWaitsForTheDownstreamResult() {
        // given
        OnboardingData onboardingData = new OnboardingData();
        AtomicReference<UniEmitter<? super Void>> emitter = new AtomicReference<>();
        when(onboardingMsClient.onboardingUsers(onboardingData)).thenReturn(Uni.createFrom().<Void>emitter(emitter::set));

        // when
        CompletableFuture<Void> result = userService.onboardingUsers(onboardingData).subscribeAsCompletionStage();

        // then the write is pending until the downstream answers
        verify(onboardingMsClient).onboardingUsers(onboardingData);
        assertFalse(result.isDone());

        // when the downstream completes
        emitter.get().complete(null);

        // then
        assertTrue(result.isDone());
        assertFalse(result.isCompletedExceptionally());
    }

    @Test
    void onboardingUsers_downstreamFailureIsPropagated() {
        // given
        OnboardingData onboardingData = new OnboardingData();
        WebApplicationException failure = new WebApplicationException(502);
        when(onboardingMsClient.onboardingUsers(onboardingData)).thenReturn(Uni.createFrom().failure(failure));
        Uni<Void> onboarding = userService.onboardingUsers(onboardingData);

        // when / then
        assertSame(failure, assertThrows(WebApplicationException.class, () -> onboarding.await().indefinitely()));
    }

    @Test
    void onboardingUsersAggregator_delegatesAndWaitsForTheDownstreamResult() {
        // given
        OnboardingData onboardingData = new OnboardingData();
        AtomicReference<UniEmitter<? super Void>> emitter = new AtomicReference<>();
        when(onboardingMsClient.onboardingUsersAggregator(onboardingData))
                .thenReturn(Uni.createFrom().<Void>emitter(emitter::set));

        // when
        CompletableFuture<Void> result = userService.onboardingUsersAggregator(onboardingData).subscribeAsCompletionStage();

        // then
        verify(onboardingMsClient).onboardingUsersAggregator(onboardingData);
        assertFalse(result.isDone());

        // when the downstream completes
        emitter.get().complete(null);

        // then
        assertTrue(result.isDone());
        assertFalse(result.isCompletedExceptionally());
    }

    @Test
    void onboardingUsersAggregator_downstreamFailureIsPropagated() {
        // given
        OnboardingData onboardingData = new OnboardingData();
        WebApplicationException failure = new WebApplicationException(502);
        when(onboardingMsClient.onboardingUsersAggregator(onboardingData)).thenReturn(Uni.createFrom().failure(failure));
        Uni<Void> onboarding = userService.onboardingUsersAggregator(onboardingData);

        // when / then
        assertSame(failure, assertThrows(WebApplicationException.class, () -> onboarding.await().indefinitely()));
    }

    @Test
    void checkManager_returnsTheDownstreamAnswer() {
        // given
        CheckManagerRequest request = new CheckManagerRequest();
        when(onboardingMsClient.checkManager(request))
                .thenReturn(Uni.createFrom().item(true), Uni.createFrom().item(false));

        // when
        var actualAsync1 = userService.checkManager(request).await().indefinitely();

        // then
        assertTrue(actualAsync1);
        var actualAsync2 = userService.checkManager(request).await().indefinitely();

        assertFalse(actualAsync2);
    }

    @Test
    void getManagerInfo_onboardingNotFoundIsRewrittenToTheBffMessage() {
        // given
        when(onboardingMsClient.getOnboardingWithUserInfo("id"))
                .thenReturn(Uni.createFrom().failure(new ResourceNotFoundException("raw body")));
        Uni<User> manager = userService.getManagerInfo("id", "TAX");

        // when
        ResourceNotFoundException e = assertThrows(ResourceNotFoundException.class, () -> manager.await().indefinitely());

        // then
        assertEquals("Onboarding not found", e.getMessage());
        verifyNoInteractions(pgManagerVerifier);
    }

    @Test
    void getManagerInfo_otherOnboardingFailuresArePropagatedUnchanged() {
        // given
        WebApplicationException failure = new WebApplicationException(503);
        when(onboardingMsClient.getOnboardingWithUserInfo("id")).thenReturn(Uni.createFrom().failure(failure));
        Uni<User> manager = userService.getManagerInfo("id", "TAX");

        // when / then
        assertSame(failure, assertThrows(WebApplicationException.class, () -> manager.await().indefinitely()));
        verifyNoInteractions(pgManagerVerifier);
    }

    @Test
    void getManagerInfo_userAlreadyAdminSkipsTheVerification() {
        // given
        OnboardingData onboardingData = onboardingWith(manager("TAX"));
        stubOnboarding(onboardingData);

        // when
        User result = userService.getManagerInfo("id", "TAX").await().indefinitely();

        // then
        assertEquals(PartyRole.MANAGER, result.getRole());
        verify(pgManagerVerifier, never()).doVerify(any(), any());
    }

    @Test
    void getManagerInfo_verifiedUserGetsTheManager() {
        // given
        User manager = manager("MANAGER-TAX");
        stubOnboarding(onboardingWith(manager));
        ManagerVerification verification = new ManagerVerification();
        verification.setVerified(true);
        when(pgManagerVerifier.doVerify("TAX", "COMPANY")).thenReturn(verification);

        // when
        User result = userService.getManagerInfo("id", "TAX").await().indefinitely();

        // then
        assertSame(manager, result);
    }

    @Test
    void getManagerInfo_registryVerificationRunsOnAWorkerAfterTheOnboardingArrives() throws Exception {
        // given the onboarding is pending
        AtomicReference<UniEmitter<? super OnboardingGet>> emitter = new AtomicReference<>();
        AtomicReference<Thread> verifierThread = new AtomicReference<>();
        OnboardingGet downstream = new OnboardingGet();
        User manager = manager("MANAGER-TAX");
        when(onboardingMsClient.getOnboardingWithUserInfo("id"))
                .thenReturn(Uni.createFrom().<OnboardingGet>emitter(emitter::set));
        when(onboardingMapper.toOnboardingData(downstream)).thenReturn(onboardingWith(manager));
        ManagerVerification verification = new ManagerVerification();
        verification.setVerified(true);
        when(pgManagerVerifier.doVerify("TAX", "COMPANY")).thenAnswer(invocation -> {
            verifierThread.set(Thread.currentThread());
            return verification;
        });

        // when the manager is requested
        CompletableFuture<User> result = userService.getManagerInfo("id", "TAX").subscribeAsCompletionStage();

        // then no registry call starts while the onboarding is pending
        assertFalse(result.isDone());
        verifyNoInteractions(onboardingMapper, pgManagerVerifier);

        // when the onboarding arrives
        emitter.get().complete(downstream);

        // then the blocking verification ran on a worker, not on the completing thread
        assertSame(manager, result.get(10, TimeUnit.SECONDS));
        assertNotNull(verifierThread.get());
        assertNotSame(Thread.currentThread(), verifierThread.get());
    }

    @Test
    void getManagerInfo_unverifiedUserIsNotAllowed() {
        // given
        stubOnboarding(onboardingWith(manager("MANAGER-TAX")));
        ManagerVerification verification = new ManagerVerification();
        verification.setVerified(false);
        when(pgManagerVerifier.doVerify("TAX", "COMPANY")).thenReturn(verification);
        Uni<User> manager = userService.getManagerInfo("id", "TAX");

        // when
        OnboardingNotAllowedException e = assertThrows(OnboardingNotAllowedException.class,
                () -> manager.await().indefinitely());

        // then
        assertEquals("User is not an admin of the institution", e.getMessage());
    }

    @Test
    void getManagerInfo_verifierNotFoundIsNotRewrittenAsMissingOnboarding() {
        // given the 404 recovery covers only the onboarding lookup
        stubOnboarding(onboardingWith(manager("MANAGER-TAX")));
        ResourceNotFoundException failure = new ResourceNotFoundException("verifier raw message");
        when(pgManagerVerifier.doVerify("TAX", "COMPANY")).thenThrow(failure);
        Uni<User> manager = userService.getManagerInfo("id", "TAX");

        // when
        ResourceNotFoundException e = assertThrows(ResourceNotFoundException.class,
                () -> manager.await().indefinitely());

        // then
        assertSame(failure, e);
    }

    @Test
    void getManagerInfo_withoutManagerIsNotFound() {
        // given
        User operator = manager("OP-TAX");
        operator.setRole(PartyRole.OPERATOR);
        stubOnboarding(onboardingWith(operator));
        Uni<User> manager = userService.getManagerInfo("id", "TAX");

        // when
        ResourceNotFoundException e = assertThrows(ResourceNotFoundException.class,
                () -> manager.await().indefinitely());

        // then
        assertEquals("Manager not found", e.getMessage());
        verifyNoInteractions(pgManagerVerifier);
    }

    @Test
    void searchUser_runsOnAWorkerOnlyWhenSubscribed() {
        // given
        UserId expected = new UserId();
        expected.setId(UUID.randomUUID());
        AtomicReference<Thread> lookupThread = new AtomicReference<>();
        when(userRegistryClient.searchUser("TAX")).thenAnswer(invocation -> {
            lookupThread.set(Thread.currentThread());
            return expected;
        });

        // when the search is only assembled
        Uni<UserId> search = userService.searchUser("TAX");

        // then
        verifyNoInteractions(userRegistryClient);

        // when subscribed
        UserId result = search.await().indefinitely();

        // then
        assertSame(expected, result);
        assertNotSame(Thread.currentThread(), lookupThread.get());
    }

    @Test
    void searchUser_registryFailureIsPropagated() {
        // given
        ResourceNotFoundException failure = new ResourceNotFoundException("no such user");
        when(userRegistryClient.searchUser("TAX")).thenThrow(failure);
        Uni<UserId> search = userService.searchUser("TAX");

        // when / then
        assertSame(failure, assertThrows(ResourceNotFoundException.class, () -> search.await().indefinitely()));
    }

    private void stubOnboarding(OnboardingData onboardingData) {
        OnboardingGet downstream = new OnboardingGet();
        when(onboardingMsClient.getOnboardingWithUserInfo("id")).thenReturn(Uni.createFrom().item(downstream));
        when(onboardingMapper.toOnboardingData(downstream)).thenReturn(onboardingData);
    }

    private static OnboardingData onboardingWith(User... users) {
        InstitutionUpdate update = new InstitutionUpdate();
        update.setTaxCode("COMPANY");
        OnboardingData onboardingData = new OnboardingData();
        onboardingData.setInstitutionUpdate(update);
        onboardingData.setUsers(List.of(users));
        return onboardingData;
    }

    private static User manager(String taxCode) {
        User manager = new User();
        manager.setTaxCode(taxCode);
        manager.setRole(PartyRole.MANAGER);
        return manager;
    }

    private static User user(String name, String surname) {
        User user = new User();
        user.setTaxCode("TAX");
        user.setName(name);
        user.setSurname(surname);
        return user;
    }

    private static CertifiedField<String> certified(Certification certification, String value) {
        CertifiedField<String> field = new CertifiedField<>();
        field.setCertification(certification);
        field.setValue(value);
        return field;
    }
}
