package it.pagopa.selfcare.onboarding.utils;

import it.pagopa.selfcare.onboarding.config.NotificationConfig;
import it.pagopa.selfcare.onboarding.dto.*;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.entity.Token;
import org.openapi.quarkus.core_json.model.InstitutionResponse;
import org.openapi.quarkus.party_registry_proxy_json.api.GeographicTaxonomiesApi;
import org.openapi.quarkus.party_registry_proxy_json.api.InstitutionApi;

import java.util.Objects;

public class StandardNotificationBuilder extends BaseNotificationBuilder
        implements NotificationUserBuilder {
    public StandardNotificationBuilder(
            String alternativeEmail,
            NotificationConfig.Consumer consumer,
            InstitutionApi proxyRegistryInstitutionApi,
            GeographicTaxonomiesApi geographicTaxonomiesApi,
            org.openapi.quarkus.core_json.api.InstitutionApi coreInstitutionApi) {
        super(
                alternativeEmail,
                consumer,
                proxyRegistryInstitutionApi,
                geographicTaxonomiesApi,
                coreInstitutionApi);
    }

    @Override
    public NotificationToSend buildNotificationToSend(
            Onboarding onboarding, Token token, InstitutionResponse institution, QueueEvent queueEvent) {
        NotificationToSend notificationToSend =
                super.buildNotificationToSend(onboarding, token, institution, queueEvent);
        this.retrieveAndSetAggregatorInfo(onboarding, notificationToSend);
        notificationToSend.setInternalIstitutionID(institution.getId());
        notificationToSend.setInstitutionId(institution.getId());
        notificationToSend.setTestInstitution(Boolean.TRUE.equals(institution.getIsTest()));
        notificationToSend.setNotificationType(queueEvent);
        return notificationToSend;
    }

    @Override
    public BillingToSend retrieveBilling(Onboarding onboarding) {
        if (Objects.isNull(onboarding.getBilling())) {
            return null;
        }

        BillingToSend billing = super.retrieveBilling(onboarding);
        billing.setPublicServices(onboarding.getBilling().isPublicServices());
        billing.setTaxCodeInvoicing(onboarding.getBilling().getTaxCodeInvoicing());
        return billing;
    }

    @Override
    public void setTokenData(NotificationToSend notificationToSend, Token token) {
        if (Objects.nonNull(token)) {
            super.setTokenData(notificationToSend, token);
            notificationToSend.setFilePath(token.getContractSigned());
        }
    }

    private void retrieveAndSetAggregatorInfo(
            Onboarding onboarding, NotificationToSend notificationToSend) {
        boolean isAggregator = Boolean.TRUE.equals(onboarding.getIsAggregator());
        notificationToSend.setIsAggregator(isAggregator);
        if (Objects.nonNull(onboarding.getAggregator())) {
            RootAggregator rootAggregator = new RootAggregator();
            rootAggregator.setInstitutionId(onboarding.getAggregator().getId());
            rootAggregator.setOriginId(onboarding.getAggregator().getOriginId());
            rootAggregator.setDescription(onboarding.getAggregator().getDescription());
            notificationToSend.setRootAggregator(rootAggregator);
        }
    }

    @Override
    public NotificationUserToSend buildUserNotificationToSend(
            Onboarding onboarding,
            Token token,
            InstitutionResponse institution,
            String createdAt,
            String updatedAt,
            String status,
            String userId,
            String partyRole,
            String productRole) {
        return null;
    }
}
