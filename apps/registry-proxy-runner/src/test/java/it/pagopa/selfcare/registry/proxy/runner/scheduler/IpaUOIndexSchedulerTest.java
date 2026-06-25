package it.pagopa.selfcare.registry.proxy.runner.scheduler;

import static org.mockito.Mockito.*;

import it.pagopa.selfcare.registry.proxy.runner.model.IpaUo;
import it.pagopa.selfcare.registry.proxy.runner.service.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IpaUOIndexSchedulerTest {

  @Mock IpaUoOpenDataService ipaOpenDataService;

  @Mock
  IpaUOIndexWriterService indexWriterService;

  @InjectMocks
  IpaUORegistryProxyScheduler scheduler;

  @Test
  void testFeedAiSearchIndex() {
    List<IpaUo> items = List.of(new IpaUo());

    when(ipaOpenDataService.fetch()).thenReturn(items);

    scheduler.feedAiSearchIndex();

    verify(indexWriterService, times(1)).index(items);
  }
}
