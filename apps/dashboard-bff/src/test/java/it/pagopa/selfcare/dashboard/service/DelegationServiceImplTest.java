package it.pagopa.selfcare.dashboard.service;

import com.fasterxml.jackson.core.type.TypeReference;
import it.pagopa.selfcare.core.generated.openapi.v1.dto.*;
import it.pagopa.selfcare.dashboard.client.CoreDelegationApiRestClient;
import it.pagopa.selfcare.dashboard.client.CoreInstitutionApiRestClient;
import it.pagopa.selfcare.dashboard.exception.ResourceNotFoundException;
import it.pagopa.selfcare.dashboard.model.delegation.DelegationRequest;
import it.pagopa.selfcare.dashboard.model.delegation.*;
import it.pagopa.selfcare.dashboard.model.mapper.DelegationRestClientMapperImpl;
import it.pagopa.selfcare.dashboard.model.mapper.InstitutionMapperImpl;
import it.pagopa.selfcare.onboarding.generated.openapi.v1.dto.InstitutionType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;

import static it.pagopa.selfcare.onboarding.common.ProductId.PROD_PAGOPA;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DelegationServiceImplTest extends BaseServiceTest {

    @InjectMocks
    private DelegationServiceImpl delegationServiceImpl;
    @Mock
    private CoreInstitutionApiRestClient coreInstitutionApiRestClient;
    @Mock
    private CoreDelegationApiRestClient coreDelegationApiRestClient;
    @Spy
    private DelegationRestClientMapperImpl delegationMapper;
    @Spy
    private InstitutionMapperImpl institutionMapper;


    @BeforeEach
    public void setUp() {
        super.setUp();
    }

    @Test
    void testCreateDelegation() throws IOException {

        ClassPathResource pathResourceResponse = new ClassPathResource("expectations/DelegationResponse.json");
        byte[] resourceStreamResponse = Files.readAllBytes(pathResourceResponse.getFile().toPath());
        DelegationResponse delegationResponse = objectMapper.readValue(resourceStreamResponse, new TypeReference<>() {
        });

        ClassPathResource pathResource = new ClassPathResource("expectations/DelegationRequest.json");
        byte[] resourceStream = Files.readAllBytes(pathResource.getFile().toPath());
        DelegationRequest delegation = objectMapper.readValue(resourceStream, new TypeReference<>() {
        });

        ResponseEntity<DelegationResponse> delegationResponseResponseEntity = ResponseEntity.ok(delegationResponse);

        DelegationId delegationId = new DelegationId();
        delegationId.setId("id");
        DelegationResponse result = new DelegationResponse();
        result.setId("id");
        when(coreDelegationApiRestClient._createDelegationUsingPOST(any())).thenReturn(delegationResponseResponseEntity);

        DelegationId response = delegationServiceImpl.createDelegation(delegation);
        Assertions.assertEquals(delegationId.getId(), response.getId());
        Mockito.verify(coreDelegationApiRestClient, Mockito.times(1))
                ._createDelegationUsingPOST(any());
    }

    @Test
    void testCreateDelegationPagoPa() throws IOException {

        ClassPathResource pathResource = new ClassPathResource("expectations/DelegationRequestPagoPa.json");
        byte[] resourceStream = Files.readAllBytes(pathResource.getFile().toPath());
        DelegationRequest delegation = objectMapper.readValue(resourceStream, new TypeReference<>() {
        });

        DelegationRequest delegationTaxCode = objectMapper.readValue(resourceStream, new TypeReference<>() {
        });

        ClassPathResource pathResourceInstitution = new ClassPathResource("stubs/InstitutionsResponse.json");
        byte[] resourceStreamInstitution = Files.readAllBytes(pathResourceInstitution.getFile().toPath());
        InstitutionsResponse institutionsResponse = objectMapper.readValue(resourceStreamInstitution, new TypeReference<>() {
        });
        ResponseEntity<InstitutionsResponse> responseEntityInstitution = ResponseEntity.ok(institutionsResponse);

        ClassPathResource pathResourceDelegation = new ClassPathResource("stubs/DelegationResponse.json");
        byte[] resourceStreamDelegation = Files.readAllBytes(pathResourceDelegation.getFile().toPath());
        DelegationResponse delegationResponse = objectMapper.readValue(resourceStreamDelegation, new TypeReference<>() {
        });
        ResponseEntity<DelegationResponse> responseEntityDelegation = ResponseEntity.ok(delegationResponse);

        DelegationId delegationId = new DelegationId();
        delegationId.setId("id");
        DelegationResponse result = new DelegationResponse();
        result.setId("id");

        when(coreInstitutionApiRestClient._getInstitutionsUsingGET(delegationTaxCode.getTo(), null, null, null, null, null)).thenReturn(responseEntityInstitution);
        when(coreDelegationApiRestClient._createDelegationUsingPOST(any())).thenReturn(responseEntityDelegation);

        DelegationId response = delegationServiceImpl.createDelegation(delegation);
        Assertions.assertEquals(delegationId.getId(), response.getId());
        Mockito.verify(coreInstitutionApiRestClient, Mockito.times(1))
                ._getInstitutionsUsingGET(delegationTaxCode.getTo(), null, null, null, null, null);
        Mockito.verify(coreDelegationApiRestClient, Mockito.times(1))
                ._createDelegationUsingPOST(any());
    }

    @Test
    void testCreateDelegationPagoPaWithNoActiveOnboarding() {
        DelegationRequest delegation = new DelegationRequest();
        delegation.setProductId(PROD_PAGOPA.getValue());
        delegation.setTo("taxCode");
        delegation.setFrom("from");
        delegation.setType(DelegationType.PT);

        InstitutionsResponse institutionsResponse = new InstitutionsResponse();
        InstitutionResponse institutionResponse = new InstitutionResponse();
        institutionResponse.setId("institutionId");

        institutionResponse.setOnboarding(List.of(
                new OnboardedProductResponse()
                        .productId(PROD_PAGOPA.getValue())
                        .status(OnboardedProductResponse.StatusEnum.DELETED)
        ));
        institutionsResponse.setInstitutions(List.of(institutionResponse));

        when(coreInstitutionApiRestClient._getInstitutionsUsingGET(any(), any(), any(), any(), any(), any()))
                .thenReturn(ResponseEntity.ok(institutionsResponse));

        assertThrows(ResourceNotFoundException.class, () ->
                delegationServiceImpl.createDelegation(delegation)
        );

        Mockito.verify(coreInstitutionApiRestClient, Mockito.times(1))
                ._getInstitutionsUsingGET(any(), any(), any(), any(), any(), any());
        Mockito.verify(coreDelegationApiRestClient, Mockito.never())
                ._createDelegationUsingPOST(any());
    }



    @Test
    void testCreateDelegationWithResourceNotFoundException() {
        DelegationRequest delegationPagoPa = new DelegationRequest();
        delegationPagoPa.setProductId(PROD_PAGOPA.getValue());
        delegationPagoPa.setId("id");
        delegationPagoPa.setFrom("from");
        delegationPagoPa.setTo("to");
        delegationPagoPa.setType(DelegationType.PT);
        InstitutionsResponse institutionsResponse = new InstitutionsResponse();
        InstitutionResponse inst = new InstitutionResponse();
        inst.setId("id");
        inst.setInstitutionType(InstitutionType.PA.getValue());
        institutionsResponse.setInstitutions(Collections.emptyList());
        ResponseEntity<InstitutionsResponse> responseEntity = ResponseEntity.ok(institutionsResponse);
        when(coreInstitutionApiRestClient._getInstitutionsUsingGET(any(), any(), any(), any(), any(), any())).thenReturn(responseEntity);
        assertThrows(ResourceNotFoundException.class, () -> delegationServiceImpl.createDelegation(delegationPagoPa));
    }

    @Test
    void getDelegations() throws IOException {

        ClassPathResource pathExpectation = new ClassPathResource("expectations/Delegation.json");
        byte[] ExpectationStream = Files.readAllBytes(pathExpectation.getFile().toPath());
        List<Delegation> delegations = objectMapper.readValue(ExpectationStream, new TypeReference<>() {
        });

        GetDelegationParameters delegationParameters = GetDelegationParameters.builder()
                .productId("setProductId")
                .taxCode("taxCode")
                .search("name")
                .order("ASC")
                .page(0)
                .size(1000)
                .build();

        List<DelegationResponse> delegationsResponse = delegations.stream()
                .map(delegation -> {
                    DelegationResponse response = new DelegationResponse();
                    response.setId(delegation.getId());
                    response.setInstitutionId(delegation.getInstitutionId());
                    response.setBrokerId(delegation.getBrokerId());
                    response.setProductId(delegation.getProductId());
                    response.setInstitutionName(delegation.getInstitutionName());
                    response.setInstitutionRootName(delegation.getInstitutionRootName());
                    response.setBrokerName(delegation.getBrokerName());
                    response.setType(DelegationResponse.TypeEnum.AOO);
                    return response;
                }).toList();

        when(coreDelegationApiRestClient._getDelegationsUsingGET(any(), any(), any(), any(), any(), any(), anyInt(), anyInt())).thenReturn(ResponseEntity.ok(delegationsResponse));

        List<Delegation> response = delegationServiceImpl.getDelegations(delegationParameters);

        Assertions.assertEquals(delegations, response);
        Mockito.verify(coreDelegationApiRestClient, Mockito.times(1))
                ._getDelegationsUsingGET(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void getDelegationV2() throws IOException {

        ClassPathResource pathExpectation = new ClassPathResource("expectations/DelegationWithPagination.json");
        byte[] ExpectationStream = Files.readAllBytes(pathExpectation.getFile().toPath());
        DelegationWithPagination delegationWithPagination = objectMapper.readValue(ExpectationStream, new TypeReference<>() {
        });

        GetDelegationParameters delegationParameters = GetDelegationParameters.builder()
                .productId("setProductId")
                .taxCode("taxCode")
                .search("name")
                .order("ASC")
                .page(0)
                .size(1000)
                .build();

        DelegationWithPaginationResponse delegationsResponse = new DelegationWithPaginationResponse();
        delegationsResponse.setDelegations(delegationWithPagination.getDelegations().stream()
                .map(delegation -> {
                    DelegationResponse response = new DelegationResponse();
                    response.setId(delegation.getId());
                    return response;
                }).toList());
        it.pagopa.selfcare.core.generated.openapi.v1.dto.PageInfo pageInfo = new it.pagopa.selfcare.core.generated.openapi.v1.dto.PageInfo();
        pageInfo.setPageSize(delegationWithPagination.getPageInfo().getPageSize());
        pageInfo.setTotalPages(delegationWithPagination.getPageInfo().getTotalPages());
        pageInfo.setTotalElements(delegationWithPagination.getPageInfo().getTotalElements());
        delegationsResponse.setPageInfo(pageInfo);

        when(coreDelegationApiRestClient._getDelegationsUsingGET1(any(), any(), any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(ResponseEntity.ok(delegationsResponse));

        DelegationWithPagination response = delegationServiceImpl.getDelegationsV2(delegationParameters);

        Assertions.assertEquals(delegationWithPagination.getDelegations().get(0).getId(), response.getDelegations().get(0).getId());
        Mockito.verify(coreDelegationApiRestClient, Mockito.times(1))
                ._getDelegationsUsingGET1(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void testSetToInstitutionIdWithSingleResult() {
        final DelegationRequest delegationRequest = new DelegationRequest();
        delegationRequest.setTo("to");
        delegationRequest.setProductId("prod-1");

        final InstitutionsResponse institutionsResponse = new InstitutionsResponse();
        institutionsResponse.setInstitutions(List.of(
                getInstitutionResponse("institutionId", List.of(
                        getOnboardedProductResponse("prod-1", OnboardedProductResponse.StatusEnum.ACTIVE, OnboardedProductResponse.InstitutionTypeEnum.GSP),
                        getOnboardedProductResponse("prod-1", OnboardedProductResponse.StatusEnum.DELETED, OnboardedProductResponse.InstitutionTypeEnum.GSP),
                        getOnboardedProductResponse("prod-2", OnboardedProductResponse.StatusEnum.ACTIVE, OnboardedProductResponse.InstitutionTypeEnum.GSP)
                ))
        ));
        when(coreInstitutionApiRestClient._getInstitutionsUsingGET(any(), any(), any(), any(), any(), any()))
                .thenReturn(ResponseEntity.ok(institutionsResponse));

        delegationServiceImpl.setToInstitutionId(delegationRequest);
        assertEquals("institutionId", delegationRequest.getTo());
    }

    @Test
    void testSetToInstitutionIdWithMultipleResultsWithPT() {
        final DelegationRequest delegationRequest = new DelegationRequest();
        delegationRequest.setTo("to");
        delegationRequest.setProductId("prod-1");

        final InstitutionsResponse institutionsResponse = new InstitutionsResponse();
        institutionsResponse.setInstitutions(List.of(
                getInstitutionResponse("institutionId1", List.of(
                        getOnboardedProductResponse("prod-1", OnboardedProductResponse.StatusEnum.ACTIVE, OnboardedProductResponse.InstitutionTypeEnum.GSP),
                        getOnboardedProductResponse("prod-1", OnboardedProductResponse.StatusEnum.DELETED, OnboardedProductResponse.InstitutionTypeEnum.PT),
                        getOnboardedProductResponse("prod-2", OnboardedProductResponse.StatusEnum.ACTIVE, OnboardedProductResponse.InstitutionTypeEnum.GSP)
                )),
                getInstitutionResponse("institutionId2", List.of(
                        getOnboardedProductResponse("prod-2", OnboardedProductResponse.StatusEnum.ACTIVE, OnboardedProductResponse.InstitutionTypeEnum.PSP),
                        getOnboardedProductResponse("prod-1", OnboardedProductResponse.StatusEnum.ACTIVE, OnboardedProductResponse.InstitutionTypeEnum.PT)
                )),
                getInstitutionResponse("institutionId3", List.of(
                        getOnboardedProductResponse("prod-1", OnboardedProductResponse.StatusEnum.ACTIVE, OnboardedProductResponse.InstitutionTypeEnum.PSP)
                ))
        ));
        when(coreInstitutionApiRestClient._getInstitutionsUsingGET(any(), any(), any(), any(), any(), any()))
                .thenReturn(ResponseEntity.ok(institutionsResponse));

        delegationServiceImpl.setToInstitutionId(delegationRequest);
        assertEquals("institutionId2", delegationRequest.getTo());
    }

    @Test
    void testSetToInstitutionIdWithMultipleResultsWithoutPT() {
        final DelegationRequest delegationRequest = new DelegationRequest();
        delegationRequest.setTo("to");
        delegationRequest.setProductId("prod-1");

        final InstitutionsResponse institutionsResponse = new InstitutionsResponse();
        institutionsResponse.setInstitutions(List.of(
                getInstitutionResponse("institutionId1", List.of(
                        getOnboardedProductResponse("prod-1", OnboardedProductResponse.StatusEnum.ACTIVE, OnboardedProductResponse.InstitutionTypeEnum.GSP),
                        getOnboardedProductResponse("prod-1", OnboardedProductResponse.StatusEnum.DELETED, OnboardedProductResponse.InstitutionTypeEnum.PT),
                        getOnboardedProductResponse("prod-2", OnboardedProductResponse.StatusEnum.ACTIVE, OnboardedProductResponse.InstitutionTypeEnum.GSP)
                )),
                getInstitutionResponse("institutionId2", List.of(
                        getOnboardedProductResponse("prod-2", OnboardedProductResponse.StatusEnum.ACTIVE, OnboardedProductResponse.InstitutionTypeEnum.PSP),
                        getOnboardedProductResponse("prod-1", OnboardedProductResponse.StatusEnum.DELETED, OnboardedProductResponse.InstitutionTypeEnum.PT)
                )),
                getInstitutionResponse("institutionId3", List.of(
                        getOnboardedProductResponse("prod-1", OnboardedProductResponse.StatusEnum.ACTIVE, OnboardedProductResponse.InstitutionTypeEnum.PSP)
                ))
        ));
        when(coreInstitutionApiRestClient._getInstitutionsUsingGET(any(), any(), any(), any(), any(), any()))
                .thenReturn(ResponseEntity.ok(institutionsResponse));

        delegationServiceImpl.setToInstitutionId(delegationRequest);
        assertEquals("institutionId1", delegationRequest.getTo());
    }

    @Test
    void testSetToInstitutionIdWithoutResults() {
        final DelegationRequest delegationRequest = new DelegationRequest();
        delegationRequest.setTo("to");
        delegationRequest.setProductId("prod-1");

        final InstitutionsResponse institutionsResponse = new InstitutionsResponse();
        institutionsResponse.setInstitutions(List.of());
        when(coreInstitutionApiRestClient._getInstitutionsUsingGET(any(), any(), any(), any(), any(), any()))
                .thenReturn(ResponseEntity.ok(institutionsResponse));

        assertThrows(ResourceNotFoundException.class, () -> delegationServiceImpl.setToInstitutionId(delegationRequest));
    }

    private InstitutionResponse getInstitutionResponse(String institutionId, List<OnboardedProductResponse> onboardings) {
        final InstitutionResponse institutionResponse = new InstitutionResponse();
        institutionResponse.setId(institutionId);
        institutionResponse.setOnboarding(onboardings);
        return institutionResponse;
    }

    private OnboardedProductResponse getOnboardedProductResponse(String productId, OnboardedProductResponse.StatusEnum status,
                                                                 OnboardedProductResponse.InstitutionTypeEnum institutionType) {
        final OnboardedProductResponse onb = new OnboardedProductResponse();
        onb.setProductId(productId);
        onb.setStatus(status);
        onb.setInstitutionType(institutionType);
        return onb;
    }

}