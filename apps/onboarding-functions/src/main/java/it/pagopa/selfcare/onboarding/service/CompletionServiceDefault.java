package it.pagopa.selfcare.onboarding.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.functions.ExecutionContext;
import it.pagopa.selfcare.onboarding.common.*;
import it.pagopa.selfcare.onboarding.dto.OnboardingAggregateOrchestratorInput;
import it.pagopa.selfcare.onboarding.entity.*;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.exception.GenericOnboardingException;
import it.pagopa.selfcare.onboarding.mapper.InstitutionMapper;
import it.pagopa.selfcare.onboarding.mapper.OnboardingMapper;
import it.pagopa.selfcare.onboarding.mapper.ProductMapper;
import it.pagopa.selfcare.onboarding.mapper.UserMapper;
import it.pagopa.selfcare.onboarding.repository.OnboardingRepository;
import it.pagopa.selfcare.product.entity.Product;
import it.pagopa.selfcare.product.service.ProductService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.openapi.quarkus.core_json.api.DelegationApi;
import org.openapi.quarkus.core_json.api.InstitutionApi;
import org.openapi.quarkus.core_json.model.*;
import org.openapi.quarkus.document_json.api.DocumentControllerApi;
import org.openapi.quarkus.document_json.model.DocumentResponse;
import org.openapi.quarkus.party_registry_proxy_json.api.AooApi;
import org.openapi.quarkus.party_registry_proxy_json.api.InfocamereApi;
import org.openapi.quarkus.party_registry_proxy_json.api.NationalRegistriesApi;
import org.openapi.quarkus.party_registry_proxy_json.api.UoApi;
import org.openapi.quarkus.user_json.model.AddUserRoleDto;
import org.openapi.quarkus.user_registry_json.api.UserApi;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

import static it.pagopa.selfcare.onboarding.common.OnboardingStatus.REJECTED;
import static it.pagopa.selfcare.onboarding.common.PartyRole.MANAGER;
import static it.pagopa.selfcare.onboarding.common.ProductId.PROD_PN;
import static it.pagopa.selfcare.onboarding.common.WorkflowType.*;
import static jakarta.ws.rs.core.Response.Status.Family.SUCCESSFUL;
import static org.openapi.quarkus.core_json.model.DelegationResponse.StatusEnum.ACTIVE;

@ApplicationScoped
@SuppressWarnings({"java:S6813", "java:S107"})
public class CompletionServiceDefault implements CompletionService {

    @RestClient
    @Inject
    InstitutionApi institutionApi;
    @RestClient
    @Inject
    org.openapi.quarkus.user_json.api.UserApi userApi;
    @RestClient
    @Inject
    UserApi userRegistryApi;
    @RestClient
    @Inject
    AooApi aooApi;
    @RestClient
    @Inject
    UoApi uoApi;
    @RestClient
    @Inject
    DelegationApi delegationApi;
    @RestClient
    @Inject
    org.openapi.quarkus.party_registry_proxy_json.api.InstitutionApi institutionRegistryProxyApi;
    @RestClient
    @Inject
    org.openapi.quarkus.user_json.api.InstitutionApi userInstitutionApi;
    @RestClient
    @Inject
    InfocamereApi infocamereApi;
    @RestClient
    @Inject
    NationalRegistriesApi nationalRegistriesApi;
    @RestClient
    @Inject
    DocumentControllerApi documentControllerApi;


    private final InstitutionMapper institutionMapper;
    private final OnboardingRepository onboardingRepository;
    private final ObjectMapper objectMapper;
    private final UserMapper userMapper;
    private final ProductMapper productMapper;
    private final NotificationService notificationService;
    private final ProductService productService;
    private final OnboardingMapper onboardingMapper;
    private final boolean hasToSendEmail;
    private final boolean forceInstitutionCreation;
    private static final String USERS_FIELD_LIST = "fiscalCode,familyName,name";

    public CompletionServiceDefault(ProductService productService,
                                    NotificationService notificationService,
                                    OnboardingMapper onboardingMapper,
                                    UserMapper userMapper,
                                    ProductMapper productMapper,
                                    InstitutionMapper institutionMapper,
                                    OnboardingRepository onboardingRepository, ObjectMapper objectMapper,
                                    @ConfigProperty(name = "onboarding-functions.persist-users.send-mail") boolean hasToSendEmail,
                                    @ConfigProperty(name = "onboarding-functions.force-institution-persist") boolean forceInstitutionCreation) {
        this.institutionMapper = institutionMapper;
        this.onboardingRepository = onboardingRepository;
        this.objectMapper = objectMapper;
        this.productService = productService;
        this.notificationService = notificationService;
        this.onboardingMapper = onboardingMapper;
        this.userMapper = userMapper;
        this.productMapper = productMapper;
        this.hasToSendEmail = hasToSendEmail;
        this.forceInstitutionCreation = forceInstitutionCreation;
    }

    @Override
    public String createInstitutionAndPersistInstitutionId(Onboarding onboarding) {

        if (Objects.nonNull(onboarding.getInstitution()) &&
                StringUtils.isNotBlank(onboarding.getInstitution().getId())) {
            return onboarding.getInstitution().getId();
        }

        InstitutionResponse institutionResponse = createOrRetrieveInstitution(onboarding);

        if (Objects.nonNull(institutionResponse)) {
            onboardingRepository
                    .update("institution.id = ?1 and updatedAt = ?2 ", institutionResponse.getId(), LocalDateTime.now())
                    .where("_id", onboarding.getId());
            return institutionResponse.getId();
        }

        throw new GenericOnboardingException("Error when create institutions!");
    }

    public InstitutionResponse createOrRetrieveInstitution(Onboarding onboarding) {
        if (forceInstitutionCreation) {
            //When onboarding a pg institution this condition ensures that the institution's information are persisted correctly
            return createInstitution(onboarding.getInstitution());
        }

        Institution institution = onboarding.getInstitution();
        InstitutionsResponse institutionsResponse = getInstitutions(institution);

        if (Objects.nonNull(institutionsResponse.getInstitutions())
                && institutionsResponse.getInstitutions().size() > 1) {
            throw new GenericOnboardingException("List of institutions is ambiguous, it is empty or has more than one element!!");
        }

        if (Objects.isNull(institutionsResponse.getInstitutions()) || institutionsResponse.getInstitutions().isEmpty()) {
            return createInstitution(institution);
        }

        InstitutionResponse existingInstitution = institutionsResponse.getInstitutions().get(0);

        if (shouldUpdateInstitutionFields(institution)) {
            updateInstitutionFields(existingInstitution.getId(), institution);
        }

        return existingInstitution;
    }

    private boolean shouldUpdateInstitutionFields(Institution institution) {
        return StringUtils.isNotBlank(institution.getRea())
                || StringUtils.isNotBlank(institution.getBusinessRegisterPlace())
                || StringUtils.isNotBlank(institution.getShareCapital())
                || StringUtils.isNotBlank(institution.getSupportEmail());

    }

    private void updateInstitutionFields(String institutionId, Institution institution) {
        InstitutionPut updateRequest = new InstitutionPut();
        updateRequest.setRea(institution.getRea());
        updateRequest.setBusinessRegisterPlace(institution.getBusinessRegisterPlace());
        updateRequest.setShareCapital(institution.getShareCapital());
        updateRequest.setSupportEmail(institution.getSupportEmail());

        institutionApi.updateInstitutionUsingPUT(institutionId, updateRequest);
    }

    @Override
    public void sendCompletedEmail(OnboardingWorkflow onboardingWorkflow) {
        Onboarding onboarding = onboardingWorkflow.getOnboarding();
        List<String> destinationMails = getDestinationMails(onboarding);
        destinationMails.add(onboarding.getInstitution().getDigitalAddress());
        Product product = productService.getProductIsValid(onboarding.getProductId());
        notificationService.sendCompletedEmail(onboarding.getInstitution().getDescription(),
                destinationMails, product, onboarding.getInstitution().getInstitutionType(),
                onboardingWorkflow);
    }

    @Override
    public void persistUsers(Onboarding onboarding) {
        Product product = productService.getProduct(onboarding.getProductId());
        if (Boolean.TRUE.equals(onboarding.getIsAggregator()) && !PROD_PN.getValue().equals(onboarding.getProductId()) && verifyAllowedOnboardingByWorkflowType(onboarding.getWorkflowType())) {
            onboarding.setToAddOnAggregates(Boolean.TRUE);
        }
        for (User user : onboarding.getUsers()) {

            if (!product.getRoleMappings(onboarding.getInstitution().getInstitutionType().name())
                    .get(user.getRole()).isSkipUserCreation()) {

                AddUserRoleDto userRoleDto = userMapper.toUserRole(onboarding);
                userRoleDto.hasToSendEmail(hasToSendEmail);
                userRoleDto.setUserMailUuid(user.getUserMailUuid());
                userRoleDto.setProduct(productMapper.toProduct(onboarding, user));
                userRoleDto.getProduct().setTokenId(onboarding.getId());
                userRoleDto.getProduct().setToAddOnAggregates(Optional.ofNullable(onboarding.getToAddOnAggregates()).orElse(false));
            /*
              The second parameter (header param) of the following method is used to build a bearer token with which invoke the API
              {@link it.pagopa.selfcare.onboarding.client.auth.AuthenticationPropagationHeadersFactory}
             */
                try (Response response = userApi.createUserByUserId(user.getId(), userRoleDto)) {
                    if (!SUCCESSFUL.equals(response.getStatusInfo().getFamily())) {
                        throw new GenericOnboardingException("Impossible to create or update role for user with ID: " + user.getId());
                    }
                }
            }
        }
    }

    boolean verifyAllowedOnboardingByWorkflowType(WorkflowType workflowType){
        EnumSet<WorkflowType> allowedWorkflowType = EnumSet.of(
                IMPORT_AGGREGATION,
                INCREMENT_REGISTRATION_AGGREGATOR
        );
        return allowedWorkflowType.contains(workflowType);
    }

    @Override
    public String createDelegation(Onboarding onboarding) {
        if (Objects.nonNull(onboarding.getAggregator())) {
            DelegationRequest delegationRequest = getDelegationRequest(onboarding);
            return delegationApi.createDelegationUsingPOST(delegationRequest).getId();
        } else {
            throw new GenericOnboardingException("Aggregator is null, impossible to create delegation");
        }
    }

    @Override
    public void sendMailRejection(ExecutionContext context, Onboarding onboarding) {
        List<String> destinationMails = Collections.singletonList(onboarding.getInstitution().getDigitalAddress());
        Product product = productService.getProductIsValid(onboarding.getProductId());
        notificationService.sendMailRejection(destinationMails, product, onboarding.getReasonForReject());
    }

    @Override
    public void persistOnboarding(Onboarding onboarding) {
        //Prepare data for request
        InstitutionOnboardingRequest onboardingRequest = new InstitutionOnboardingRequest();
        onboardingRequest.pricingPlan(onboarding.getPricingPlan());
        onboardingRequest.productId(onboarding.getProductId());
        onboardingRequest.setTokenId(onboarding.getId());
        Optional.ofNullable(onboarding.getActivatedAt())
                .ifPresent(date -> onboardingRequest.setActivatedAt(date.atZone(ZoneId.systemDefault()).toOffsetDateTime()));

        if (Objects.nonNull(onboarding.getBilling())) {
            BillingRequest billingRequest = new BillingRequest();
            billingRequest.recipientCode(onboarding.getBilling().getRecipientCode());
            billingRequest.publicServices(onboarding.getBilling().isPublicServices());
            billingRequest.vatNumber(onboarding.getBilling().getVatNumber());
            billingRequest.setTaxCodeInvoicing(onboarding.getBilling().getTaxCodeInvoicing());
            onboardingRequest.billing(billingRequest);
        }

        onboardingRequest.setOrigin(onboarding.getInstitution().getOrigin().name());
        onboardingRequest.setOriginId(onboarding.getInstitution().getOriginId());
        onboardingRequest.setInstitutionType(InstitutionOnboardingRequest.InstitutionTypeEnum.valueOf(onboarding.getInstitution().getInstitutionType().name()));
        onboardingRequest.setIsAggregator(onboarding.getIsAggregator());
        //If contract exists we send the path of the contract
        Optional<DocumentResponse> optDocument = documentControllerApi.getDocumentByOnboardingId(onboarding.getId()).stream().findFirst();
        optDocument.ifPresent(token -> onboardingRequest.setContractPath(token.getContractSigned()));

        institutionApi.onboardingInstitutionUsingPOST(onboarding.getInstitution().getId(), onboardingRequest);
    }

    @Override
    public void persistActivatedAt(Onboarding onboarding) {
        if (Objects.isNull(onboarding.getActivatedAt())) {
            LocalDateTime now = LocalDateTime.now();
            onboardingRepository
                    .update("activatedAt = ?1 and updatedAt = ?2 ", now, now)
                    .where("_id", onboarding.getId());
        }
    }

    @Override
    public void rejectOutdatedOnboardings(Onboarding onboarding) {
        LocalDateTime now = LocalDateTime.now();
        onboardingRepository
                .update("status = ?1 and updatedAt = ?2 ", REJECTED, now)
                .where("productId = ?1 and institution.origin = ?2 and institution.originId = ?3 and _id != ?4 and status = PENDING or status = TOBEVALIDATED",
                        onboarding.getProductId(), onboarding.getInstitution().getOrigin(), onboarding.getInstitution().getOriginId(), onboarding.getId());
    }

    @Override
    public void sendCompletedEmailAggregate(Onboarding onboarding) {
        List<String> destinationMails = getDestinationMails(onboarding);
        destinationMails.add(onboarding.getInstitution().getDigitalAddress());
        notificationService.sendCompletedEmailAggregate(onboarding.getAggregator().getDescription(), destinationMails);
    }

    @Override
    public String createAggregateOnboardingRequest(OnboardingAggregateOrchestratorInput onboardingAggregateOrchestratorInput) {
        setAggregatorInstitutionId(onboardingAggregateOrchestratorInput);
        Onboarding onboardingToUpdate = onboardingMapper.mapToOnboarding(onboardingAggregateOrchestratorInput);
        onboardingToUpdate.setWorkflowType(CONFIRMATION_AGGREGATE);
        onboardingToUpdate.setStatus(OnboardingStatus.PENDING);
        onboardingRepository.persistOrUpdate(onboardingToUpdate);
        return onboardingToUpdate.getId();
    }

    @Override
    public void sendTestEmail(ExecutionContext context) {
        notificationService.sendTestEmail(context);
    }

    @Override
    public String existsDelegation(OnboardingAggregateOrchestratorInput input) {
        boolean existsDelegation = false;

        if (Objects.nonNull(input) && Objects.nonNull(input.getInstitution()) && Objects.nonNull(input.getAggregate())) {
            try {
                setAggregatorInstitutionId(input);

                DelegationWithPaginationResponse delegationWithPaginationResponse = delegationApi.getDelegationsUsingGET1(null, input.getInstitution().getId(), null, null,
                        input.getAggregate().getTaxCode(), null, null, null);
                if (Objects.nonNull(delegationWithPaginationResponse) && !CollectionUtils.isEmpty(delegationWithPaginationResponse.getDelegations())) {
                    existsDelegation = delegationWithPaginationResponse.getDelegations().stream().anyMatch(delegation -> ACTIVE.equals(delegation.getStatus()));
                }
            } catch (WebApplicationException e) {
                throw new GenericOnboardingException(String.format("Error during retrieve delegation %s", e.getMessage()));
            }
        }
        return existsDelegation ? "true" : "false";
    }

    @Override
    public String verifyOnboardingAggregate(OnboardingAggregateOrchestratorInput input) {
        if (Objects.isNull(input.getInstitution().getId())) {
            input.getInstitution().setId(
                    onboardingRepository.findByFilters(
                                    input.getInstitution().getTaxCode(),
                                    null,
                                    input.getInstitution().getOrigin().getValue(),
                                    input.getInstitution().getOriginId(),
                                    input.getProductId())
                            .stream()
                            .findFirst()
                            .map(onboardingAggregator -> onboardingAggregator.getInstitution().getId())
                            .orElseThrow(() -> new GenericOnboardingException("Onboarding not found"))
            );
        }

        if (Objects.isNull(input.getAggregate().getId())) {
            onboardingRepository.findByFilters(
                            input.getAggregate().getTaxCode(),
                            input.getAggregate().getSubunitCode(),
                            input.getAggregate().getOrigin().getValue(),
                            input.getAggregate().getOriginId(),
                            input.getProductId())
                    .stream()
                    .findFirst()
                    .ifPresent(onboardingAggregate ->
                            input.getAggregate().setId(onboardingAggregate.getInstitution().getId())
                    );
        }
        Onboarding onboardingAggregate = onboardingMapper.mapToOnboarding(input);
        try {
            return objectMapper.writeValueAsString(onboardingAggregate);
        } catch (JsonProcessingException e) {
            throw new GenericOnboardingException(e.getMessage());
        }
    }

    private void setAggregatorInstitutionId(OnboardingAggregateOrchestratorInput input) {
        if (Objects.isNull(input.getInstitution().getId())) {
            Onboarding onboardingAggregator = onboardingRepository.findByFilters(input.getInstitution().getTaxCode(),
                    null, input.getInstitution().getOrigin().getValue(),
                    input.getInstitution().getOriginId(), input.getProductId()).stream()
                    .findFirst()
                    .orElseThrow(() -> new GenericOnboardingException("Onboarding not found"));
            input.getInstitution().setId(onboardingAggregator.getInstitution().getId());
            Onboarding onboarding = onboardingRepository.findById(input.getId());
            onboarding.getInstitution().setId(onboardingAggregator.getInstitution().getId());
            onboardingRepository.persistOrUpdate(onboarding);
        }
    }

    private static DelegationRequest getDelegationRequest(Onboarding onboarding) {
        DelegationRequest delegationRequest = new DelegationRequest();
        delegationRequest.setProductId(onboarding.getProductId());
        delegationRequest.setType(DelegationRequest.TypeEnum.EA);
        delegationRequest.setInstitutionFromName(onboarding.getInstitution().getDescription());
        delegationRequest.setFrom(onboarding.getInstitution().getId());
        delegationRequest.setTo(onboarding.getAggregator().getId());
        delegationRequest.setInstitutionToName(onboarding.getAggregator().getDescription());
        delegationRequest.setInstitutionFromRootName(onboarding.getInstitution().getParentDescription());
        return delegationRequest;
    }

    private void setGSPCategory(InstitutionRequest institutionRequest) {
        AttributesRequest category = new AttributesRequest();
        category.setCode("L37");
        category.setDescription("Gestori di Pubblici Servizi");
        institutionRequest.setAttributes(List.of(category));
    }

    private boolean isInstitutionPresentOnIpa(Institution institution) {
        try {
            if (InstitutionPaSubunitType.AOO.equals(institution.getSubunitType())) {
                aooApi.findByUnicodeUsingGET(institution.getSubunitCode(), null);
            } else if (InstitutionPaSubunitType.UO.equals(institution.getSubunitType())) {
                uoApi.findByUnicodeUsingGET1(institution.getSubunitCode(), null);
            } else {
                institutionRegistryProxyApi.findInstitutionUsingGET(institution.getTaxCode(), null, null);
            }
            return true;
        } catch (WebApplicationException e) {
            if (e.getResponse().getStatus() == 404) {
                return false;
            }
            throw new GenericOnboardingException(e.getMessage());
        }
    }

    private InstitutionsResponse getInstitutions(Institution institution) {
        InstitutionsResponse institutionsResponse;

        if (StringUtils.isNotBlank(institution.getTaxCode())) {
            institutionsResponse = institutionApi.getInstitutionsUsingGET(institution.getTaxCode(), institution.getSubunitCode(), null, null, null, null);
        } else {
            String origin = Objects.nonNull(institution.getOrigin()) ? institution.getOrigin().getValue() : null;
            institutionsResponse = institutionApi.getInstitutionsUsingGET(null, null, origin, institution.getOriginId(), null, null);
        }
        return institutionsResponse;
    }

    /**
     * Function that creates institution based on institution type and Origin,
     * Origin indicates which is the indexes where data come from, for ex. IPA comes from index of Pubbliche Amministrazioni
     * Look at <a href="https://pagopa.atlassian.net/wiki/spaces/SCP/pages/708804909/Glossario">...</a> for more information about institution type and indexes
     */
    private InstitutionResponse createInstitution(Institution institution) {

        if (InstitutionType.SA.equals(institution.getInstitutionType())
                && Origin.ANAC.equals(institution.getOrigin())) {

            return institutionApi.createInstitutionFromAnacUsingPOST(institutionMapper.toInstitutionRequest(institution));
        }

        if (InstitutionType.AS.equals(institution.getInstitutionType())
                && Origin.IVASS.equals(institution.getOrigin())) {

            return institutionApi.createInstitutionFromIvassUsingPOST(institutionMapper.toInstitutionRequest(institution));
        }

        if (InstitutionType.PG.equals(institution.getInstitutionType()) &&
                (Origin.INFOCAMERE.equals(institution.getOrigin()) || Origin.ADE.equals(institution.getOrigin()))) {

            return institutionApi.createInstitutionFromInfocamereUsingPOST(institutionMapper.toInstitutionRequest(institution));
        }

        if (Origin.IPA.name().equalsIgnoreCase(institution.getOrigin().name()) && isInstitutionPresentOnIpa(institution)) {

            InstitutionFromIpaPost fromIpaPost = new InstitutionFromIpaPost();
            fromIpaPost.setTaxCode(institution.getTaxCode());
            fromIpaPost.setRea(institution.getRea());
            fromIpaPost.setShareCapital(institution.getShareCapital());
            fromIpaPost.setBusinessRegisterPlace(institution.getBusinessRegisterPlace());
            fromIpaPost.setGeographicTaxonomies(Optional.ofNullable(institution.getGeographicTaxonomies())
                    .map(geographicTaxonomies -> geographicTaxonomies.stream().map(institutionMapper::toGeographicTaxonomy).toList())
                    .orElse(List.of()));
            fromIpaPost.setInstitutionType(InstitutionFromIpaPost.InstitutionTypeEnum.valueOf(institution.getInstitutionType().name()));
            if (Objects.nonNull(institution.getSubunitType())) {
                fromIpaPost.setSubunitCode(institution.getSubunitCode());
                fromIpaPost.setSubunitType(InstitutionFromIpaPost.SubunitTypeEnum.valueOf(institution.getSubunitType().name()));
            }
            return institutionApi.createInstitutionFromIpaUsingPOST(fromIpaPost);
        }

        InstitutionRequest institutionRequest = institutionMapper.toInstitutionRequest(institution);
        // Override category in case of GSP not present in IPA
        if (InstitutionType.GSP.equals(institution.getInstitutionType()) && !Origin.IPA.equals(institution.getOrigin())) {
            setGSPCategory(institutionRequest);
        }
        return institutionApi.createInstitutionUsingPOST(institutionMapper.toInstitutionRequest(institution));
    }

    private List<String> getDestinationMails(Onboarding onboarding) {
        return onboarding.getUsers().stream()
                .filter(userToOnboard -> MANAGER.equals(userToOnboard.getRole()))
                .map(userToOnboard -> Optional.ofNullable(userRegistryApi.findByIdUsingGET(USERS_FIELD_LIST, userToOnboard.getId()))
                        .filter(userResource -> Objects.nonNull(userResource.getWorkContacts())
                                && userResource.getWorkContacts().containsKey(userToOnboard.getUserMailUuid()))
                        .map(user -> user.getWorkContacts().get(userToOnboard.getUserMailUuid()))
                )
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(workContract -> StringUtils.isNotBlank(workContract.getEmail().getValue()))
                .map(workContract -> workContract.getEmail().getValue())
                .collect(Collectors.toList());
    }

    @Override
    public List<DelegationResponse> retrieveAggregates(Onboarding onboarding) {
        String institutionId = onboarding.getInstitution().getId();
        String productId = onboarding.getProductId();
        DelegationWithPaginationResponse delegations = delegationApi.getDelegationsUsingGET1(null, institutionId, productId, null, null, null, null, null);
        return delegations.getDelegations();
    }

}
