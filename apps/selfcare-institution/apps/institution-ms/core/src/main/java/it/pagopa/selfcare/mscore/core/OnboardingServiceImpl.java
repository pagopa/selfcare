package it.pagopa.selfcare.mscore.core;

import it.pagopa.selfcare.mscore.api.InstitutionConnector;
import it.pagopa.selfcare.mscore.api.MailNotificationConnector;
import it.pagopa.selfcare.mscore.config.InstitutionSendMailConfig;
import it.pagopa.selfcare.mscore.constant.CustomError;
import it.pagopa.selfcare.mscore.constant.RelationshipState;
import it.pagopa.selfcare.mscore.core.util.UtilEnumList;
import it.pagopa.selfcare.mscore.exception.InvalidRequestException;
import it.pagopa.selfcare.mscore.exception.ResourceNotFoundException;
import it.pagopa.selfcare.mscore.model.institution.Institution;
import it.pagopa.selfcare.mscore.model.institution.Onboarding;
import it.pagopa.selfcare.mscore.model.onboarding.VerifyOnboardingFilters;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static it.pagopa.selfcare.mscore.constant.GenericError.ONBOARDING_OPERATION_ERROR;

@Slf4j
@Service
public class OnboardingServiceImpl implements OnboardingService {
    private final OnboardingDao onboardingDao;
    private final InstitutionService institutionService;
    private final InstitutionConnector institutionConnector;
    private final MailNotificationConnector mailNotificationConnector;

    private final InstitutionSendMailConfig institutionSendMailConfig;

    public OnboardingServiceImpl(OnboardingDao onboardingDao,
                                 InstitutionService institutionService,
                                 InstitutionConnector institutionConnector,
                                 MailNotificationConnector mailNotificationConnector,
                                 InstitutionSendMailConfig institutionSendMailConfig) {

        this.onboardingDao = onboardingDao;
        this.institutionService = institutionService;
        this.institutionConnector = institutionConnector;
        this.mailNotificationConnector = mailNotificationConnector;
        this.institutionSendMailConfig = institutionSendMailConfig;
    }

    @Override
    public void verifyOnboardingInfo(String externalId, String productId) {
        institutionService.retrieveInstitutionsWithFilter(externalId, productId, UtilEnumList.VALID_RELATIONSHIP_STATES);
    }

    @Override
    public void verifyOnboardingInfoSubunit(String taxCode, String subunitCode, String productId) {
        Boolean existsOnboardingValid = institutionConnector.existsByTaxCodeAndSubunitCodeAndProductAndStatusList(taxCode,
                subunitCode, Optional.ofNullable(productId), UtilEnumList.VALID_RELATIONSHIP_STATES);
        if (Boolean.FALSE.equals(existsOnboardingValid)) {
            throw new ResourceNotFoundException(String.format(CustomError.INSTITUTION_NOT_ONBOARDED.getMessage(), taxCode, productId),
                    CustomError.INSTITUTION_NOT_ONBOARDED.getCode());
        }
    }

    @Override
    public void verifyOnboardingInfoByFilters(VerifyOnboardingFilters filters) {
        filters.setValidRelationshipStates(UtilEnumList.VALID_RELATIONSHIP_STATES);

        Boolean existsOnboardingValid = institutionConnector.existsOnboardingByFilters(filters);
        if (Boolean.FALSE.equals(existsOnboardingValid)) {
            throw new ResourceNotFoundException(CustomError.INSTITUTION_NOT_ONBOARDED_BY_FILTERS.getMessage(),
                    CustomError.INSTITUTION_NOT_ONBOARDED_BY_FILTERS.getCode());
        }
    }

    public int calculateModuleDayOfTheEpoch(String epochDatePecNotification, OffsetDateTime createdAtOnboarding, Integer sendingFrequencyPecNotification) {
        LocalDate epochStart = LocalDate.parse(epochDatePecNotification);
        long daysDiff = ChronoUnit.DAYS.between(epochStart, createdAtOnboarding);
        return (int) (daysDiff % sendingFrequencyPecNotification);
    }

    @Override
    public Institution persistOnboarding(String institutionId, String
            productId, Onboarding onboarding, StringBuilder httpStatus) {

        Institution institution = persistAndGetInstitution(institutionId, productId, onboarding, httpStatus);

        if (isMailNotificationEnabled(institution, productId)) {
            mailNotificationConnector.addMailNotification(institutionId, productId, institution.getDigitalAddress(),
                        calculateModuleDayOfTheEpoch(institutionSendMailConfig.getEpochDatePecNotification(), onboarding.getCreatedAt(),
                                institutionSendMailConfig.getPecNotificationFrequency()));
        }

        return institution;
    }

    private Institution persistAndGetInstitution(String institutionId, String productId, Onboarding onboarding, StringBuilder httpStatus) {
        log.trace("persistForUpdate start");
        log.debug("persistForUpdate institutionId = {}, productId = {}", institutionId, productId);
        onboarding.setStatus(RelationshipState.ACTIVE);
        onboarding.setProductId(productId);

        if (Objects.isNull(onboarding.getCreatedAt())) {
            onboarding.setCreatedAt(OffsetDateTime.now());
        }

        //Verify if onboarding exists, in case onboarding must fail
        final Institution institution = institutionConnector.findById(institutionId);

        Onboarding existingOnboarding = Optional.ofNullable(institution.getOnboarding())
                .orElse(List.of())
                .stream()
                .filter(item -> productId.equals(item.getProductId())
                        && UtilEnumList.VALID_RELATIONSHIP_STATES.contains(item.getStatus()))
                .findFirst()
                .orElse(null);

        if (existingOnboarding != null) {
            if (existingOnboarding.getInstitutionType().equals(onboarding.getInstitutionType())) {
                log.info("Found existing onboarding for product {} with matching institutionType {}",
                        productId, existingOnboarding.getInstitutionType());

                httpStatus.append(HttpStatus.OK.value());
                return institution;
            } else {
                throw new IllegalStateException(
                        String.format("Conflicting institutionType for product %s: existing=%s, new=%s",
                                productId,
                                existingOnboarding.getInstitutionType(),
                                onboarding.getInstitutionType())
                );
            }
        }


        try {
            //If not exists, persist a new onboarding for product
            setOnboardingFields(onboarding, institution);
            final Institution institutionUpdated = institutionConnector.findAndAddOnboarding(institutionId, onboarding);

            log.trace("persistForUpdate end");
            httpStatus.append(HttpStatus.CREATED.value());
            return institutionUpdated;
        } catch (Exception e) {
            onboardingDao.rollbackPersistOnboarding(institutionId, onboarding);
            log.info("rollbackPersistOnboarding completed for institution {} and product {}", institutionId, productId);
            throw new InvalidRequestException(ONBOARDING_OPERATION_ERROR.getMessage() + " " + e.getMessage(),
                    ONBOARDING_OPERATION_ERROR.getCode());
        }
    }

    private static void setOnboardingFields(Onboarding onboarding, Institution institution) {
        Optional.ofNullable(onboarding.getInstitutionType())
                .ifPresent(onboarding::setInstitutionType);

        Optional.ofNullable(onboarding.getOrigin())
                .ifPresentOrElse(
                        value -> {}, // Do nothing if already present
                        () -> onboarding.setOrigin(institution.getOrigin())
                );

        Optional.ofNullable(onboarding.getOriginId())
                .ifPresentOrElse(
                        value -> {}, // Do nothing if already present
                        () -> onboarding.setOriginId(institution.getOriginId())
                );
    }

    private boolean isMailNotificationEnabled(Institution institution, String productId) {
        // Mail notifications are disabled if the global property is enabled
        if (Boolean.TRUE.equals(institutionSendMailConfig.getPecNotificationDisabled())) {
            return false;
        }

        // Return false if there is any active onboarding for the product with institution type PT, true otherwise
        final Onboarding onboarding = institution.getOnboarding().stream()
                .filter(o -> productId.equals(o.getProductId()) && RelationshipState.ACTIVE.equals(o.getStatus()))
                .findFirst()
                .orElseThrow();
        return !InstitutionType.PT.equals(onboarding.getInstitutionType());

    }

    @Override
    public void deleteOnboardedInstitution(String institutionId, String productId) {
        institutionConnector.findAndDeleteOnboarding(institutionId, productId);
        mailNotificationConnector.removeMailNotification(institutionId, productId);
    }

}
