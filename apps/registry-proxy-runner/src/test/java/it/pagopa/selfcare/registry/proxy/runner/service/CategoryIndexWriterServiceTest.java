package it.pagopa.selfcare.registry.proxy.runner.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import it.pagopa.selfcare.registry.proxy.runner.client.AzureSearchRestClient;
import it.pagopa.selfcare.registry.proxy.runner.model.IpaCategory;
import it.pagopa.selfcare.registry.proxy.runner.model.SearchServiceIndexRequest;
import it.pagopa.selfcare.registry.proxy.runner.model.SearchServiceIndexResponse;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CategoryIndexWriterServiceTest {

  @Mock AzureSearchRestClient azureSearchRestClient;

  @InjectMocks CategoryIndexWriterService service;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(service, "indexName", "cat-idx");
    ReflectionTestUtils.setField(service, "apiVersion", "1.0");
  }

  @Test
  void testIndex_AlwaysUpdates() {
    IpaCategory cat = new IpaCategory();
    cat.setCode("CAT1");

    when(azureSearchRestClient.search(
            eq("cat-idx"),
            eq("1.0"),
            any(),
            any(),
            eq(false),
            eq(1),
            eq(0),
            eq("id eq 'IPA_CAT1'")))
        .thenReturn(new SearchServiceIndexResponse());

    service.index(List.of(cat));

    verify(azureSearchRestClient, times(1))
        .index(eq("cat-idx"), eq("1.0"), any(SearchServiceIndexRequest.class));
  }
}
