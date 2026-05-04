package it.pagopa.selfcare.mscore.core;

import it.pagopa.selfcare.mscore.api.DelegationConnector;
import it.pagopa.selfcare.mscore.api.EventHubConnector;
import it.pagopa.selfcare.mscore.core.mapper.DelegationNotificationMapper;
import it.pagopa.selfcare.mscore.model.delegation.DelegationWithPagination;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventsServiceImpl implements EventsService {

    private static final long DELEGATION_PAGE_SIZE = 100L;

    private final DelegationConnector delegationConnector;
    private final EventHubConnector eventHubConnector;
    private final DelegationNotificationMapper delegationNotificationMapper;

    @Override
    public void sendDelegationEvents(OffsetDateTime fromDate) {
        long successCount = 0L;
        long errorCount = 0L;
        long page = 0L;
        log.info("Starting to send delegation events from date: {}", fromDate);
        DelegationWithPagination delegationsPage;
        do {
            delegationsPage = delegationConnector.findFromDate(fromDate, page, DELEGATION_PAGE_SIZE);
            final Map<Boolean, Long> results = delegationsPage.getDelegations().stream().collect(Collectors.partitioningBy(
                    d -> eventHubConnector.sendEvent(delegationNotificationMapper.toDelegationNotificationToSend(d)),
                    Collectors.counting()
            ));
            successCount += results.get(true);
            errorCount += results.get(false);
            page++;
            log.info("Number of delegation events sent: {} (success: {}, error: {})", successCount + errorCount, successCount, errorCount);
        } while (page < delegationsPage.getPageInfo().getTotalPages());
        log.info("Finished sending delegation events from date: {}", fromDate);
    }

}
