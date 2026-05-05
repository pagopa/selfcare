package it.pagopa.selfcare.registry.proxy.runner.scheduler;

import static org.mockito.Mockito.*;

import it.pagopa.selfcare.registry.proxy.runner.model.IpaInstitution;
import it.pagopa.selfcare.registry.proxy.runner.service.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IpaInstitutionIndexSchedulerTest {

  @Mock IpaInstitutionOpenDataService ipaOpenDataService;

  @Mock InstitutionIndexWriterService institutionIndexWriterService;

  @InjectMocks
  IpaInstitutionRegistryProxyScheduler scheduler;

  @Test
  void testFeedAiSearchIndex() {
    List<IpaInstitution> insts = List.of(new IpaInstitution());

    when(ipaOpenDataService.fetch()).thenReturn(insts);

    scheduler.feedAiSearchIndex();

    verify(institutionIndexWriterService, times(1)).index(insts);
  }
}
