package it.pagopa.selfcare.registry.proxy.runner.scheduler;

import static org.mockito.Mockito.*;

import it.pagopa.selfcare.registry.proxy.runner.model.IpaAoo;
import it.pagopa.selfcare.registry.proxy.runner.service.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IpaAOOIndexSchedulerTest {

  @Mock IpaAOOOpenDataService openDataService;

  @Mock IpaAOOIndexWriterService indexWriterService;

  @InjectMocks IpaAOORegistryProxyScheduler scheduler;

  @Test
  void testFeedAiSearchIndex() {
    List<IpaAoo> items = List.of(new IpaAoo());

    when(openDataService.fetch()).thenReturn(items);

    scheduler.feedAiSearchIndex();

    verify(indexWriterService, times(1)).index(items);
  }
}
