package it.pagopa.selfcare.registry.proxy.runner.scheduler;

import static org.mockito.Mockito.*;

import it.pagopa.selfcare.registry.proxy.runner.model.AnacStation;
import it.pagopa.selfcare.registry.proxy.runner.service.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AnacIndexSchedulerTest {

  @Mock AnacDataService openDataService;

  @Mock StationIndexWriterService indexWriterService;

  @InjectMocks AnacRegistryProxyScheduler scheduler;

  @Test
  void testFeedAiSearchIndex() {
    List<AnacStation> items = List.of(new AnacStation());

    when(openDataService.fetch()).thenReturn(items);

    scheduler.feedAiSearchIndex();

    verify(indexWriterService, times(1)).index(items);
  }
}
