package it.pagopa.selfcare.onboarding.utils;

import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.common.OnboardingStatus;
import it.pagopa.selfcare.onboarding.common.Origin;
import it.pagopa.selfcare.onboarding.config.NotificationConfig;
import it.pagopa.selfcare.onboarding.dto.*;
import it.pagopa.selfcare.onboarding.entity.Billing;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.entity.PaymentServiceProvider;
import it.pagopa.selfcare.onboarding.entity.Token;
import org.openapi.quarkus.core_json.model.InstitutionResponse;
import org.openapi.quarkus.core_json.model.PaymentServiceProviderResponse;
import org.openapi.quarkus.party_registry_proxy_json.api.GeographicTaxonomiesApi;
import org.openapi.quarkus.party_registry_proxy_json.api.InstitutionApi;
import org.openapi.quarkus.party_registry_proxy_json.model.GeographicTaxonomyResource;
import org.openapi.quarkus.party_registry_proxy_json.model.InstitutionResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class BaseNotificationBuilder implements NotificationBuilder {
    public static final String CLOSED = "CLOSED";
    public static final String ACTIVE = "ACTIVE";
    private final String alternativeEmail;
    protected NotificationConfig.Consumer consumer;
    protected final InstitutionApi proxyRegistryInstitutionApi;
    protected final GeographicTaxonomiesApi geographicTaxonomiesApi;
    protected final org.openapi.quarkus.core_json.api.InstitutionApi coreInstitutionApi;
    protected static final String DESCRIPTION_TO_REPLACE_REGEX = " - COMUNE";
    protected static final Logger log = LoggerFactory.getLogger(BaseNotificationBuilder.class);

    public BaseNotificationBuilder(
            String alternativeEmail,
            NotificationConfig.Consumer consumer,
            InstitutionApi proxyRegistryInstitutionApi,
            GeographicTaxonomiesApi geographicTaxonomiesApi,
            org.openapi.quarkus.core_json.api.InstitutionApi coreInstitutionApi) {
        this.alternativeEmail = alternativeEmail;
        this.consumer = consumer;
        this.proxyRegistryInstitutionApi = proxyRegistryInstitutionApi;
        this.geographicTaxonomiesApi = geographicTaxonomiesApi;
        this.coreInstitutionApi = coreInstitutionApi;
    }

    public NotificationToSend buildNotificationToSend(
            Onboarding onboarding, Token token, InstitutionResponse institution, QueueEvent queueEvent) {
        NotificationToSend notificationToSend = new NotificationToSend();
        if (queueEvent.equals(QueueEvent.ADD)) {
            notificationToSend.setId(onboarding.getId());
        } else {
            notificationToSend.setId(UUID.randomUUID().toString());
        }
        notificationToSend.setState(
                convertOnboardingStatusToNotificationStatus(onboarding.getStatus()));
        mapDataFromOnboarding(onboarding, notificationToSend, queueEvent);
        notificationToSend.setInstitution(retrieveInstitution(institution, onboarding));
        if (Objects.nonNull(token)) {
            setTokenData(notificationToSend, token);
        }

        return notificationToSend;
    }

    private void mapDataFromOnboarding(
            Onboarding onboarding, NotificationToSend notificationToSend, QueueEvent queueEvent) {
        notificationToSend.setOnboardingTokenId(onboarding.getId());
        notificationToSend.setBilling(retrieveBilling(onboarding));
        notificationToSend.setPricingPlan(onboarding.getPricingPlan());
        notificationToSend.setCreatedAt(
                OffsetDateTime.of(
                        Optional.ofNullable(onboarding.getActivatedAt()).orElse(onboarding.getCreatedAt()),
                        ZoneOffset.UTC));
        notificationToSend.setProduct(onboarding.getProductId());
        if (Objects.nonNull(onboarding.getReferenceOnboardingId())) {
            notificationToSend.setReferenceOnboardingId(onboarding.getReferenceOnboardingId());
        }
        if (queueEvent.equals(QueueEvent.ADD)) {
            // when onboarding complete last update is activated date
            notificationToSend.setUpdatedAt(
                    OffsetDateTime.of(
                            Optional.ofNullable(onboarding.getActivatedAt()).orElse(onboarding.getCreatedAt()),
                            ZoneOffset.UTC));
        } else {
            if (onboarding.getStatus().equals(OnboardingStatus.DELETED)) {
                // Queue.ClosedAt: if token.deleted show closedAt
                notificationToSend.setClosedAt(
                        OffsetDateTime.of(
                                Optional.ofNullable(onboarding.getDeletedAt()).orElse(onboarding.getUpdatedAt()),
                                ZoneOffset.UTC));
                notificationToSend.setUpdatedAt(
                        OffsetDateTime.of(
                                Optional.ofNullable(onboarding.getDeletedAt()).orElse(onboarding.getUpdatedAt()),
                                ZoneOffset.UTC));
            } else {
                // when update last update is updated date
                notificationToSend.setUpdatedAt(
                        OffsetDateTime.of(
                                Optional.ofNullable(onboarding.getUpdatedAt()).orElse(onboarding.getCreatedAt()),
                                ZoneOffset.UTC));
            }
        }
    }

    private String convertOnboardingStatusToNotificationStatus(OnboardingStatus status) {
        if (status.equals(OnboardingStatus.DELETED)) {
            return CLOSED;
        } else if (status.equals(OnboardingStatus.COMPLETED)) {
            return ACTIVE;
        } else {
            return status.name();
        }
    }

    @Override
    public InstitutionToNotify retrieveInstitution(
            InstitutionResponse institution, Onboarding onboarding) {
        InstitutionToNotify toNotify = new InstitutionToNotify();
        toNotify.setInstitutionType(onboarding.getInstitution().getInstitutionType());
        toNotify.setDescription(institution.getDescription());
        toNotify.setDigitalAddress(
                institution.getDigitalAddress() == null
                        ? alternativeEmail
                        : institution.getDigitalAddress());
        toNotify.setAddress(institution.getAddress());
        toNotify.setTaxCode(onboarding.getInstitution().getTaxCode());
        toNotify.setOrigin(onboarding.getInstitution().getOrigin().getValue());
        toNotify.setOriginId(onboarding.getInstitution().getOriginId());
        toNotify.setZipCode(institution.getZipCode());
        toNotify.setPaymentServiceProvider(
                toSetPaymentServiceProvider(institution.getPaymentServiceProvider(), onboarding));
        if (institution.getSubunitType() != null && !"EC".equals(institution.getSubunitType())) {
            toNotify.setSubUnitType(institution.getSubunitType());
            toNotify.setSubUnitCode(institution.getSubunitCode());
        }
        RootParent rootParent = new RootParent();
        if (Objects.nonNull(institution.getRootParent())) {
            rootParent.setId(institution.getRootParent().getId());
            rootParent.setDescription(institution.getRootParent().getDescription());
            InstitutionResponse parentInstitution =
                    coreInstitutionApi.retrieveInstitutionByIdUsingGET(rootParent.getId(), onboarding.getProductId());
            rootParent.setOriginId(
                    Objects.nonNull(parentInstitution) ? parentInstitution.getOriginId() : null);
            toNotify.setRootParent(rootParent);
        }
        if (Objects.nonNull(institution.getAttributes()) && !institution.getAttributes().isEmpty()) {
            toNotify.setCategory(institution.getAttributes().get(0).getCode());
        } else if (InstitutionType.GSP == onboarding.getInstitution().getInstitutionType()
                && Origin.SELC.name().equals(onboarding.getInstitution().getOrigin().name())) {
            toNotify.setCategory("L37");
        }
        if (Objects.isNull(institution.getCity())
                && InstitutionType.PA == onboarding.getInstitution().getInstitutionType()) {
            retrieveAndSetGeographicData(toNotify);
        } else {
            toNotify.setCounty(institution.getCounty());
            toNotify.setCountry(institution.getCountry());
            toNotify.setIstatCode(institution.getIstatCode());
            toNotify.setCity(institution.getCity().replace(DESCRIPTION_TO_REPLACE_REGEX, ""));
        }
        return toNotify;
    }

    private PaymentServiceProvider toSetPaymentServiceProvider(
            PaymentServiceProviderResponse psp, Onboarding onboarding) {
        PaymentServiceProvider paymentServiceProviderToNotify = new PaymentServiceProvider();
        if (Objects.isNull(psp)) {
            return null;
        }

        paymentServiceProviderToNotify.setAbiCode(psp.getAbiCode());
        paymentServiceProviderToNotify.setBusinessRegisterNumber(psp.getBusinessRegisterNumber());
        paymentServiceProviderToNotify.setLegalRegisterName(psp.getLegalRegisterName());
        paymentServiceProviderToNotify.setLegalRegisterNumber(psp.getLegalRegisterNumber());
        paymentServiceProviderToNotify.setVatNumberGroup(psp.getVatNumberGroup());
        if (InstitutionType.PSP.equals(onboarding.getInstitution().getInstitutionType())) {
            if (Objects.nonNull(onboarding.getInstitution().getPaymentServiceProvider().getProviderNames())) {
                paymentServiceProviderToNotify.setProviderNames(
                        onboarding.getInstitution().getPaymentServiceProvider().getProviderNames());
            }
            if (Objects.nonNull(onboarding.getInstitution().getPaymentServiceProvider().getContractId())) {
                paymentServiceProviderToNotify.setContractId(
                        onboarding.getInstitution().getPaymentServiceProvider().getContractId());
            }
            if (Objects.nonNull(onboarding.getInstitution().getPaymentServiceProvider().getContractType())) {
                paymentServiceProviderToNotify.setContractType(
                        onboarding.getInstitution().getPaymentServiceProvider().getContractType());
            }
        }
        return paymentServiceProviderToNotify;
    }

    @Override
    public void retrieveAndSetGeographicData(InstitutionToNotify institution) {
        InstitutionResource proxyInfo =
                proxyRegistryInstitutionApi.findInstitutionUsingGET(institution.getTaxCode(), null, null);
        institution.setIstatCode(proxyInfo.getIstatCode());
        institution.setCategory(proxyInfo.getCategory());
        GeographicTaxonomyResource geographicTaxonomies =
                geographicTaxonomiesApi.retrieveGeoTaxonomiesByCodeUsingGET(proxyInfo.getIstatCode());
        institution.setCounty(geographicTaxonomies.getProvinceAbbreviation());
        institution.setCountry(geographicTaxonomies.getCountryAbbreviation());
        institution.setCity(geographicTaxonomies.getDesc().replace(DESCRIPTION_TO_REPLACE_REGEX, ""));
    }

    @Override
    public BillingToSend retrieveBilling(Onboarding onboarding) {
        return convertBilling(onboarding.getBilling());
    }

    private BillingToSend convertBilling(Billing billing) {
        BillingToSend billingToSend = new BillingToSend();
        if (Objects.isNull(billing)) {
            return null;
        }
        billingToSend.setVatNumber(billing.getVatNumber());
        billingToSend.setRecipientCode(billing.getRecipientCode());
        return billingToSend;
    }

    @Override
    public void setTokenData(NotificationToSend notificationToSend, Token token) {
        if (Objects.nonNull(token) && Objects.nonNull(token.getContractSigned())) {
            File file = new File(token.getContractSigned());
            notificationToSend.setFileName(file.getName());
            notificationToSend.setContentType(token.getContractSigned());
        }
    }
}
