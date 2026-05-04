package it.pagopa.selfcare.registry.proxy.runner.scheduler;

import static org.mockito.Mockito.*;

import it.pagopa.selfcare.registry.proxy.runner.model.IpaCategory;
import it.pagopa.selfcare.registry.proxy.runner.service.CategoryIndexWriterService;
import it.pagopa.selfcare.registry.proxy.runner.service.IpaCategoryOpenDataService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IpaCategoriesIndexSchedulerTest {

  @Mock IpaCategoryOpenDataService ipaOpenDataService;

  @Mock CategoryIndexWriterService categoryIndexWriterService;

  @InjectMocks IpaCategoriesRegistryProxyScheduler scheduler;

  @Test
  void testFeedAiSearchIndex() {
    List<IpaCategory> cats = List.of(new IpaCategory());

    when(ipaOpenDataService.fetch()).thenReturn(cats);

    scheduler.feedAiSearchIndex();

    verify(categoryIndexWriterService, times(1)).index(cats);
  }
}
