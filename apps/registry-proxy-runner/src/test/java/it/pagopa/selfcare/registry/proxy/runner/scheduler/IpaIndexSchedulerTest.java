package it.pagopa.selfcare.registry.proxy.runner.scheduler;

import static org.mockito.Mockito.*;

import it.pagopa.selfcare.registry.proxy.runner.model.AnacStation;
import it.pagopa.selfcare.registry.proxy.runner.model.IpaAoo;
import it.pagopa.selfcare.registry.proxy.runner.model.IpaCategory;
import it.pagopa.selfcare.registry.proxy.runner.model.IpaInstitution;
import it.pagopa.selfcare.registry.proxy.runner.model.IpaUo;
import it.pagopa.selfcare.registry.proxy.runner.model.IvassInsuranceCompany;
import it.pagopa.selfcare.registry.proxy.runner.service.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IpaIndexSchedulerTest {

  @Mock IpaOpenDataService ipaOpenDataService;

  @Mock AnacDataService anacDataService;

  @Mock IvassDataService ivassDataService;

  @Mock InstitutionIndexWriterService institutionIndexWriterService;

  @Mock CategoryIndexWriterService categoryIndexWriterService;

  @Mock AooIndexWriterService aooIndexWriterService;

  @Mock UoIndexWriterService uoIndexWriterService;

  @Mock StationIndexWriterService stationIndexWriterService;

  @Mock InsuranceCompanyIndexWriterService insuranceCompanyIndexWriterService;

  @InjectMocks IpaIndexScheduler scheduler;

  @Test
  void testFeedAiSearchIndex() {
    List<IpaInstitution> insts = List.of(new IpaInstitution());
    List<IpaCategory> cats = List.of(new IpaCategory());
    List<IpaAoo> aoos = List.of(new IpaAoo());
    List<IpaUo> uos = List.of(new IpaUo());
    List<AnacStation> stations = List.of(new AnacStation());
    List<IvassInsuranceCompany> comps = List.of(new IvassInsuranceCompany());

    when(ipaOpenDataService.fetchInstitutions()).thenReturn(insts);
    when(ipaOpenDataService.fetchCategories()).thenReturn(cats);
    when(ipaOpenDataService.fetchAOOs()).thenReturn(aoos);
    when(ipaOpenDataService.fetchUOs()).thenReturn(uos);
    when(anacDataService.fetch()).thenReturn(stations);
    when(ivassDataService.fetch()).thenReturn(comps);

    scheduler.feedAiSearchIndex();

    verify(institutionIndexWriterService, times(1)).index(insts);
    verify(categoryIndexWriterService, times(1)).index(cats);
    verify(aooIndexWriterService, times(1)).index(aoos);
    verify(uoIndexWriterService, times(1)).index(uos);
    verify(stationIndexWriterService, times(1)).index(stations);
    verify(insuranceCompanyIndexWriterService, times(1)).index(comps);
  }
}
