package it.pagopa.selfcare.dashboard.service;

import it.pagopa.selfcare.backoffice.generated.openapi.v1.dto.Brokers;
import it.pagopa.selfcare.backoffice.generated.openapi.v1.dto.BrokersPsp;
import it.pagopa.selfcare.core.generated.openapi.v1.dto.BrokerResponse;
import it.pagopa.selfcare.dashboard.client.CoreInstitutionApiRestClient;
import it.pagopa.selfcare.dashboard.client.MsBackOfficeChannelApiClient;
import it.pagopa.selfcare.dashboard.client.MsBackOfficeStationApiClient;
import it.pagopa.selfcare.dashboard.model.backoffice.BrokerInfo;
import it.pagopa.selfcare.dashboard.model.mapper.BrokerMapper;
import lombok.extern.slf4j.Slf4j;
import org.owasp.encoder.Encode;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static it.pagopa.selfcare.onboarding.common.InstitutionType.PSP;


@Slf4j
@Service
public class BrokerServiceImpl implements BrokerService {

    private final MsBackOfficeStationApiClient backofficeStationApiClient;
    private final MsBackOfficeChannelApiClient backofficeChannelApiClient;
    private final CoreInstitutionApiRestClient coreInstitutionApiRestClient;

    private final BrokerMapper brokerMapper;

    private static final String DEFAULT_ORDER_DIRECTION = "ASC";

    public BrokerServiceImpl(MsBackOfficeStationApiClient backofficeStationApiClient,
                             MsBackOfficeChannelApiClient backofficeChannelApiClient,
                             CoreInstitutionApiRestClient coreInstitutionApiRestClient,
                             BrokerMapper brokerMapper) {
        this.backofficeStationApiClient = backofficeStationApiClient;
        this.backofficeChannelApiClient = backofficeChannelApiClient;
        this.coreInstitutionApiRestClient = coreInstitutionApiRestClient;
        this.brokerMapper = brokerMapper;
    }

    private static final int DEFAULT_LIMIT = 1000;
    private static final int START_PAGE = 0;

    @Override
    public List<BrokerInfo> findAllByInstitutionType(String institutionType) {
        log.trace("findAllByInstitutionType start");
        List<BrokerInfo> brokers;
        if (PSP.name().equals(institutionType)) {
            brokers = parseBrokersPSP(backofficeChannelApiClient._getBrokersPsp(START_PAGE, null, DEFAULT_LIMIT, null, null, null, DEFAULT_ORDER_DIRECTION).getBody());
        } else {
            brokers = parseBrokersEC(backofficeStationApiClient._getBrokers(START_PAGE, null, DEFAULT_LIMIT, null, null, null, DEFAULT_ORDER_DIRECTION).getBody());
        }
        log.debug("findAllByInstitutionType result = {}", brokers);
        log.trace("findAllByInstitutionType end");
        return brokers;
    }

    private List<BrokerInfo> parseBrokersPSP(BrokersPsp brokersPspResource) {
        List<BrokerInfo> brokers = Collections.emptyList();
        if (brokersPspResource != null && brokersPspResource.getBrokersPsp() != null) {
            brokers = brokersPspResource.getBrokersPsp().stream()
                    .map(brokerMapper::fromBrokerPSPResource)
                    .collect(Collectors.toList());
        }
        return brokers;
    }

    private List<BrokerInfo> parseBrokersEC(Brokers brokersResource) {
        List<BrokerInfo> brokers = Collections.emptyList();
        if (brokersResource != null && brokersResource.getBrokers() != null) {
            brokers = brokersResource.getBrokers().stream()
                    .map(brokerMapper::fromBrokerResource)
                    .collect(Collectors.toList());
        }
        return brokers;
    }

    @Override
    public List<BrokerInfo> findInstitutionsByProductAndType(String productId, String institutionType) {
        log.trace("findInstitutionsByProductAndType start");
        log.debug("findInstitutionsByProductAndType productId = {}, type = {}", Encode.forJava(productId), Encode.forJava(institutionType));
        List<BrokerResponse> brokerResponses = coreInstitutionApiRestClient._getInstitutionBrokersUsingGET(productId, institutionType).getBody();
        List<BrokerInfo> brokers = brokerMapper.fromInstitutions(brokerResponses);
        log.debug("findInstitutionsByProductAndType result = {}", brokers);
        log.trace("findInstitutionsByProductAndType end");
        return brokers;
    }
}
