package it.pagopa.selfcare.dashboard.service;

import com.fasterxml.jackson.core.type.TypeReference;
import it.pagopa.selfcare.backoffice.generated.openapi.v1.dto.BrokerPsp;
import it.pagopa.selfcare.backoffice.generated.openapi.v1.dto.Brokers;
import it.pagopa.selfcare.backoffice.generated.openapi.v1.dto.BrokersPsp;
import it.pagopa.selfcare.core.generated.openapi.v1.dto.BrokerResponse;
import it.pagopa.selfcare.dashboard.client.CoreInstitutionApiRestClient;
import it.pagopa.selfcare.dashboard.client.MsBackOfficeChannelApiClient;
import it.pagopa.selfcare.dashboard.client.MsBackOfficeStationApiClient;
import it.pagopa.selfcare.dashboard.model.backoffice.BrokerInfo;
import it.pagopa.selfcare.dashboard.model.mapper.BrokerMapperImpl;
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
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BrokerServiceImplTest extends BaseServiceTest {

    @InjectMocks
    private BrokerServiceImpl brokerService;
    @Mock
    private MsBackOfficeStationApiClient backofficeStationApiClient;
    @Mock
    private MsBackOfficeChannelApiClient backofficeChannelApiClient;
    @Mock
    private CoreInstitutionApiRestClient coreInstitutionApiRestClient;
    @Spy
    private BrokerMapperImpl brokerMapper;

    @BeforeEach
    public void setUp() {
        super.setUp();
    }

    @Test
    void getBrokersPSP() throws IOException {
        String institutionType = "PSP";

        byte[] resourceStream = Files.readAllBytes(Paths.get("src/test/resources/expectations/checkThisExpectations/BrokerInfo.json"));
        List<BrokerInfo> brokerInfo = objectMapper.readValue(resourceStream, new TypeReference<>() {
        });

        byte[] resourceBrokerPsp = Files.readAllBytes(Paths.get("src/test/resources/stubs/brokerPsp.json"));
        List<BrokerPsp> brokerPspInfo = objectMapper.readValue(resourceBrokerPsp, new TypeReference<>() {
        });

        BrokersPsp brokersPsp = new BrokersPsp();
        brokersPsp.setBrokersPsp(brokerPspInfo);

        when(backofficeChannelApiClient._getBrokersPsp(0, null, 1000, null, null, null, "ASC"))
                .thenReturn(ResponseEntity.ok(brokersPsp));

        List<BrokerInfo> result = brokerService.findAllByInstitutionType(institutionType);
        Assertions.assertEquals(brokerInfo, result);
        Mockito.verify(backofficeChannelApiClient, Mockito.times(1))
                ._getBrokersPsp(0, null, 1000, null, null, null, "ASC");
    }

    @Test
    void getBrokersEC() throws IOException {
        String institutionType = "EC";

        ClassPathResource pathResource = new ClassPathResource("expectations/checkThisExpectations/Brokers.json");
        byte[] resourceStream = Files.readAllBytes(pathResource.getFile().toPath());
        Brokers brokers= objectMapper.readValue(resourceStream, new TypeReference<>() {
        });

        ResponseEntity<Brokers> brokersResponseEntity = ResponseEntity.ok(brokers);
       when(backofficeStationApiClient._getBrokers(0, null, 1000, null, null, null, "ASC"))
        .thenReturn(brokersResponseEntity);

        List<BrokerInfo> result = brokerService.findAllByInstitutionType(institutionType);
        Assertions.assertEquals(brokers.getBrokers().get(0).getBrokerCode(), result.get(0).getCode());
        Assertions.assertEquals(brokers.getBrokers().get(0).getDescription(), result.get(0).getDescription());
        Assertions.assertEquals(brokers.getBrokers().get(0).getEnabled(), result.get(0).getEnabled());
        Mockito.verify(backofficeStationApiClient, Mockito.times(1))
                ._getBrokers(0, null, 1000, null, null, null, "ASC");
    }

    @Test
    void getBrokerEmptyList() {
        String institutionType = "EC";
        ResponseEntity<Brokers> responseEntity = ResponseEntity.ok(new Brokers());
        when(backofficeStationApiClient._getBrokers(0, null, 1000, null, null, null, "ASC"))
                .thenReturn(responseEntity);

        List<BrokerInfo> result = brokerService.findAllByInstitutionType(institutionType);
        Assertions.assertEquals(0, result.size());
        Mockito.verify(backofficeStationApiClient, Mockito.times(1))
                ._getBrokers(0, null, 1000, null, null, null, "ASC");
    }

    @Test
    void getFindInstitutionsByProductAndType() throws IOException {
        String institutionType = "EC";
        String productId = "productId";

        ClassPathResource pathResource = new ClassPathResource("stubs/BrokerResponse.json");
        byte[] resourceStream = Files.readAllBytes(pathResource.getFile().toPath());
        List<BrokerResponse> brokerResponse = objectMapper.readValue(resourceStream, new TypeReference<>() {
        });

        ResponseEntity<List<BrokerResponse>> responseEntity = ResponseEntity.ok(brokerResponse);

        when(coreInstitutionApiRestClient._getInstitutionBrokersUsingGET(productId, institutionType))
                .thenReturn(responseEntity);

        List<BrokerInfo> brokers = brokerMapper.fromInstitutions(brokerResponse);

        List<BrokerInfo> result = brokerService.findInstitutionsByProductAndType(productId, institutionType);
        Assertions.assertEquals(brokers, result);
        Mockito.verify(coreInstitutionApiRestClient, Mockito.times(1))
                ._getInstitutionBrokersUsingGET(productId, institutionType);
    }

    @Test
    void getFindInstitutionsByProductAndTypeEmptyList() {
        when(coreInstitutionApiRestClient._getInstitutionBrokersUsingGET("productId", "EC"))
                .thenReturn(ResponseEntity.ok(Collections.emptyList()));
        List<BrokerInfo> brokers = brokerService.findInstitutionsByProductAndType("productId", "EC");
        Assertions.assertEquals(0, brokers.size());
        Mockito.verify(coreInstitutionApiRestClient, Mockito.times(1))
                ._getInstitutionBrokersUsingGET("productId", "EC");
    }
}