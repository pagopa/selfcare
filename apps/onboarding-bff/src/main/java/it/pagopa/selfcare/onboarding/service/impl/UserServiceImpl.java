package it.pagopa.selfcare.onboarding.service.impl;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import it.pagopa.selfcare.onboarding.service.*;

import it.pagopa.selfcare.onboarding.client.model.*;
import it.pagopa.selfcare.onboarding.exception.InvalidUserFieldsException;
import it.pagopa.selfcare.onboarding.exception.OnboardingNotAllowedException;
import it.pagopa.selfcare.onboarding.exception.ResourceNotFoundException;
import it.pagopa.selfcare.onboarding.mapper.OnboardingMapper;
import it.pagopa.selfcare.onboarding.util.LogUtils;
import it.pagopa.selfcare.onboarding.util.PgManagerVerifier;
import it.pagopa.selfcare.onboarding.util.Preconditions;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openapi.quarkus.onboarding_json.model.CheckManagerRequest;
import org.owasp.encoder.Encode;

import java.util.ArrayList;
import java.util.EnumSet;

import static it.pagopa.selfcare.onboarding.util.Utils.getManager;
import static it.pagopa.selfcare.onboarding.util.Utils.isUserAdmin;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final EnumSet<RegistryUser.Fields> FIELD_LIST = EnumSet.of(RegistryUser.Fields.name, RegistryUser.Fields.familyName);
    private static final String INVALID_FIELD_REASON = "the value does not match with the certified data";
    private final UserRegistryService userRegistryConnector;
    private final OnboardingService onboardingMsConnector;
    private final OnboardingMapper onboardingMapper;
    private final PgManagerVerifier pgManagerVerifier;

    // The user registry client is synchronous: the lookup runs on a worker, the comparison on the resulting item
    @Override
    public Uni<Void> validate(User user) {
        log.trace("validate start");
        log.debug(LogUtils.CONFIDENTIAL_MARKER, "validate user = {}", user);
        Preconditions.notNull(user, "An user is required");
        return Uni.createFrom().item(() -> userRegistryConnector.search(user.getTaxCode(), FIELD_LIST))
                .runSubscriptionOn(Infrastructure.getDefaultWorkerPool())
                .invoke(searchResult -> searchResult.ifPresent(foundUser -> {
                    final ArrayList<InvalidUserFieldsException.InvalidField> invalidFields = new ArrayList<>();
                    if (!isValid(user.getName(), foundUser.getName())) {
                        invalidFields.add(new InvalidUserFieldsException.InvalidField("name", INVALID_FIELD_REASON));
                    }
                    if (!isValid(user.getSurname(), foundUser.getFamilyName())) {
                        invalidFields.add(new InvalidUserFieldsException.InvalidField("surname", INVALID_FIELD_REASON));
                    }
                    if (!invalidFields.isEmpty()) {
                        throw new InvalidUserFieldsException(invalidFields);
                    }
                }))
                .invoke(() -> log.trace("validate end"))
                .replaceWithVoid();
    }

    @Override
    public Uni<Void> onboardingUsers(OnboardingData onboardingData) {
        log.trace("onboardingUsers start");
        log.debug("onboardingUsers onboardingData = {}", onboardingData);
        return onboardingMsConnector.onboardingUsers(onboardingData)
                .invoke(() -> log.trace("onboardingUsers end"));
    }

    @Override
    public Uni<Void> onboardingUsersAggregator(OnboardingData onboardingData) {
        log.trace("onboardingUsersAggregator start");
        log.debug("onboardingUsersAggregator onboardingData = {}", onboardingData);
        return onboardingMsConnector.onboardingUsersAggregator(onboardingData)
                .invoke(() -> log.trace("onboardingUsersAggregator end"));
    }

    @Override
    public Uni<Boolean> checkManager(CheckManagerRequest checkManagerRequest) {
        log.trace("checkManager start");
        log.debug("checkManager checkManagerRequest = {}", checkManagerRequest);
        return onboardingMsConnector.checkManager(checkManagerRequest)
                .invoke(result -> log.trace("checkManager end"));
    }

    private <T> boolean isValid(T field, CertifiedField<T> certifiedField) {
        return certifiedField == null
                || !Certification.isCertified(certifiedField.getCertification())
                || (String.class.isAssignableFrom(certifiedField.getValue().getClass())
                ? ((String) certifiedField.getValue()).equalsIgnoreCase((String) field)
                : certifiedField.getValue().equals(field));
    }

    @Override
    public Uni<User> getManagerInfo(String onboardingId, String userTaxCode) {
        log.trace("getManagerInfo start");
        log.debug("getManagerInfo onboardingId = {}", Encode.forJava(onboardingId));

        return onboardingMsConnector.getOnboardingWithUserInfo(onboardingId)
                .map(onboardingMapper::toOnboardingData)
                .onFailure(ResourceNotFoundException.class).transform(e -> {
                    log.error("Onboarding not found", e);
                    return new ResourceNotFoundException("Onboarding not found");
                })
                .chain(onboardingData -> verifyManager(onboardingData, userTaxCode));
    }

    private Uni<User> verifyManager(OnboardingData onboardingData, String userTaxCode) {
        String institutionTaxCode = onboardingData.getInstitutionUpdate().getTaxCode();
        log.debug("getManagerInfo institutionTaxCode = {}", institutionTaxCode);

        User managerInfo = getManager(onboardingData.getUsers())
                .orElseThrow(() -> new ResourceNotFoundException("Manager not found"));

        boolean isAlreadyAdmin = isUserAdmin(userTaxCode, onboardingData.getUsers());
        if (isAlreadyAdmin) {
            return Uni.createFrom().item(managerInfo);
        }

        // The registry lookups behind the verifier are synchronous HTTP calls
        return Uni.createFrom().item(() -> pgManagerVerifier.doVerify(userTaxCode, institutionTaxCode))
                .runSubscriptionOn(Infrastructure.getDefaultWorkerPool())
                .invoke(managerVerification -> {
                    if (!managerVerification.isVerified()) {
                        log.error("User is not an admin of the institution");
                        throw new OnboardingNotAllowedException("User is not an admin of the institution");
                    }
                })
                .map(managerVerification -> {
                    log.trace("getManagerInfo end");
                    return managerInfo;
                });
    }

    @Override
    public Uni<UserId> searchUser(String taxCode) {
        log.trace("searchUser start");
        log.debug("searchUser taxCode = {}", taxCode);
        return Uni.createFrom().item(() -> userRegistryConnector.searchUser(taxCode))
                .runSubscriptionOn(Infrastructure.getDefaultWorkerPool())
                .invoke(userId -> log.trace("searchUser end"));
    }
}
