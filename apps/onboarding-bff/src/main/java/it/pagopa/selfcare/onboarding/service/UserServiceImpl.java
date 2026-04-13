package it.pagopa.selfcare.onboarding.service;

import static it.pagopa.selfcare.onboarding.client.model.user.User.Fields.familyName;
import static it.pagopa.selfcare.onboarding.client.model.user.User.Fields.name;
import static it.pagopa.selfcare.onboarding.util.Utils.getManager;
import static it.pagopa.selfcare.onboarding.util.Utils.isUserAdmin;

import it.pagopa.selfcare.commons.base.logging.LogUtils;
import it.pagopa.selfcare.onboarding.client.OnboardingMsClient;
import it.pagopa.selfcare.onboarding.client.UserRegistryClient;
import it.pagopa.selfcare.onboarding.exception.ResourceNotFoundException;
import it.pagopa.selfcare.onboarding.client.model.ManagerVerification;
import it.pagopa.selfcare.onboarding.client.model.CheckManagerData;
import it.pagopa.selfcare.onboarding.client.model.OnboardingData;
import it.pagopa.selfcare.onboarding.client.model.User;
import it.pagopa.selfcare.onboarding.client.model.Certification;
import it.pagopa.selfcare.onboarding.client.model.CertifiedField;
import it.pagopa.selfcare.onboarding.client.model.UserId;
import it.pagopa.selfcare.onboarding.exception.InvalidUserFieldsException;
import it.pagopa.selfcare.onboarding.exception.OnboardingNotAllowedException;
import it.pagopa.selfcare.onboarding.service.strategy.UserAllowedValidationStrategy;
import it.pagopa.selfcare.onboarding.util.PgManagerVerifier;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.owasp.encoder.Encode;
import java.util.Objects;

@Slf4j
@ApplicationScoped
public class UserServiceImpl implements UserService {

    private static final EnumSet<it.pagopa.selfcare.onboarding.client.model.user.User.Fields> FIELD_LIST = EnumSet.of(name, familyName);
    private static final String INVALID_FIELD_REASON = "the value does not match with the certified data";
    private final UserRegistryClient userRegistryConnector;
    private final OnboardingMsClient onboardingMsConnector;
    private final PgManagerVerifier pgManagerVerifier;
    private final UserAllowedValidationStrategy userAllowedValidationStrategy;

    public UserServiceImpl(UserRegistryClient userRegistryConnector,
                           OnboardingMsClient onboardingMsConnector,
                           PgManagerVerifier pgManagerVerifier, UserAllowedValidationStrategy userAllowedValidationStrategy) {
        this.userRegistryConnector = userRegistryConnector;
        this.onboardingMsConnector = onboardingMsConnector;
        this.pgManagerVerifier = pgManagerVerifier;
        this.userAllowedValidationStrategy = userAllowedValidationStrategy;
    }

    @Override
    public void validate(User user) {
        log.trace("validate start");
        log.debug(LogUtils.CONFIDENTIAL_MARKER, "validate user = {}", user);
        Objects.requireNonNull(user, "An user is required");
        final Optional<it.pagopa.selfcare.onboarding.client.model.user.User> searchResult =
                userRegistryConnector.search(user.getTaxCode(), FIELD_LIST);
        searchResult.ifPresent(foundUser -> {
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
        });
        log.trace("validate end");
    }

    @Override
    public void onboardingUsers(OnboardingData onboardingData) {
        log.trace("onboardingUsers start");
        log.debug("onboardingUsers onboardingData = {}", onboardingData);
        onboardingMsConnector.onboardingUsers(onboardingData);
        log.trace("onboardingUsers end");
    }

    @Override
    public void onboardingUsersAggregator(OnboardingData onboardingData) {
        log.trace("onboardingUsersAggregator start");
        log.debug("onboardingUsersAggregator onboardingData = {}", onboardingData);
        onboardingMsConnector.onboardingUsersAggregator(onboardingData);
        log.trace("onboardingUsersAggregator end");
    }

    @Override
    public boolean  checkManager(CheckManagerData checkManagerData) {
        log.trace("checkManager start");
        log.debug("checkManager checkManagerData = {}", checkManagerData);
        boolean checkManager =  onboardingMsConnector.checkManager(checkManagerData);
        log.trace("checkManager end");
        return checkManager;
    }

    private <T> boolean isValid(T field, CertifiedField<T> certifiedField) {
        return certifiedField == null
                || !Certification.isCertified(certifiedField.getCertification())
                || (String.class.isAssignableFrom(certifiedField.getValue().getClass())
                ? ((String) certifiedField.getValue()).equalsIgnoreCase((String) field)
                : certifiedField.getValue().equals(field));
    }

    @Override
    public User getManagerInfo(String onboardingId, String userTaxCode) {
        log.trace("getManagerInfo start");
        log.debug("getManagerInfo onboardingId = {}", Encode.forJava(onboardingId));

        OnboardingData onboardingData;
        try {
            onboardingData = onboardingMsConnector.getOnboardingWithUserInfo(onboardingId);
        } catch (ResourceNotFoundException e) {
            log.error("Onboarding not found", e);
            throw new ResourceNotFoundException("Onboarding not found");
        }

        String institutionTaxCode = onboardingData.getInstitutionUpdate().getTaxCode();
        log.debug("getManagerInfo institutionTaxCode = {}", institutionTaxCode);

        User managerInfo = getManager(onboardingData.getUsers())
                .orElseThrow(() -> new ResourceNotFoundException("Manager not found"));

        boolean isAlreadyAdmin = isUserAdmin(userTaxCode, onboardingData.getUsers());
        if (isAlreadyAdmin) {
            return managerInfo;
        }

        ManagerVerification managerVerification = pgManagerVerifier.doVerify(userTaxCode, institutionTaxCode);
        if (!managerVerification.isVerified()) {
            log.error("User is not an admin of the institution");
            throw new OnboardingNotAllowedException("User is not an admin of the institution");
        }

        log.trace("getManagerInfo end");
        return managerInfo;
    }

  @Override
  public boolean isAllowedUserByUid(String uid) {
      log.trace("isAllowedUser for {}", uid);
      return userAllowedValidationStrategy.isAuthorizedUser(uid);
  }

    @Override
    public UserId searchUser(String taxCode) {
        log.trace("searchUser start");
        log.debug("searchUser taxCode = {}", taxCode);
        UserId userId = userRegistryConnector.searchUser(taxCode);
        log.trace("searchUser end");
        return userId;
    }
}
