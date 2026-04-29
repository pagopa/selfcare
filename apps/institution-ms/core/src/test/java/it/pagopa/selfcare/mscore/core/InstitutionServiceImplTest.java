package it.pagopa.selfcare.mscore.core;

import it.pagopa.selfcare.commons.base.security.PartyRole;
import it.pagopa.selfcare.commons.base.security.SelfCareUser;
import it.pagopa.selfcare.mscore.api.DelegationConnector;
import it.pagopa.selfcare.mscore.api.InstitutionConnector;
import it.pagopa.selfcare.mscore.api.PartyRegistryProxyConnector;
import it.pagopa.selfcare.mscore.api.UserApiConnector;
import it.pagopa.selfcare.mscore.config.CoreConfig;
import it.pagopa.selfcare.mscore.constant.RelationshipState;
import it.pagopa.selfcare.mscore.constant.SearchMode;
import it.pagopa.selfcare.mscore.core.mapper.InstitutionMapper;
import it.pagopa.selfcare.mscore.core.mapper.InstitutionMapperImpl;
import it.pagopa.selfcare.mscore.core.mapper.TokenMapper;
import it.pagopa.selfcare.mscore.core.mapper.TokenMapperImpl;
import it.pagopa.selfcare.mscore.core.strategy.CreateInstitutionStrategy;
import it.pagopa.selfcare.mscore.core.strategy.factory.CreateInstitutionStrategyFactory;
import it.pagopa.selfcare.mscore.core.util.InstitutionPaSubunitType;
import it.pagopa.selfcare.mscore.exception.InvalidRequestException;
import it.pagopa.selfcare.mscore.exception.MsCoreException;
import it.pagopa.selfcare.mscore.exception.ResourceConflictException;
import it.pagopa.selfcare.mscore.exception.ResourceNotFoundException;
import it.pagopa.selfcare.mscore.model.institution.*;
import it.pagopa.selfcare.mscore.model.onboarding.TokenUser;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static it.pagopa.selfcare.commons.utils.TestUtils.mockInstance;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InstitutionServiceImplTest {
    @Mock
    private InstitutionConnector institutionConnector;

    @Mock
    private DelegationConnector delegationConnector;

    @Mock
    private UserApiConnector userApiConnector;

    @InjectMocks
    private InstitutionServiceImpl institutionServiceImpl;

    @Mock
    private PartyRegistryProxyConnector partyRegistryProxyConnector;
    
    @Mock
    private CoreConfig coreConfig;

    @Mock
    private CreateInstitutionStrategyFactory createInstitutionStrategyFactory;

    @Mock
    private CreateInstitutionStrategy createInstitutionStrategy;

    @Spy
    private InstitutionMapper institutionMapper = new InstitutionMapperImpl();

    @Spy
    private TokenMapper tokenMapper = new TokenMapperImpl();

    private CategoryProxyInfo createCategoryProxyInfo() {
        CategoryProxyInfo dummyCategoryProxyInfo = new CategoryProxyInfo();
        dummyCategoryProxyInfo.setCode("Code");
        dummyCategoryProxyInfo.setKind("Kind");
        dummyCategoryProxyInfo.setName("Name");
        dummyCategoryProxyInfo.setOrigin("Origin");

        return dummyCategoryProxyInfo;
    }

    private InstitutionProxyInfo createInstitutionProxyInfo() {
        InstitutionProxyInfo dummyInstitutionProxyInfo = new InstitutionProxyInfo();
        dummyInstitutionProxyInfo.setAddress("42 Main St");
        dummyInstitutionProxyInfo.setAoo("Aoo");
        dummyInstitutionProxyInfo.setCategory("Category");
        dummyInstitutionProxyInfo.setDescription("The characteristics of someone or something");
        dummyInstitutionProxyInfo.setDigitalAddress("42 Main St");
        dummyInstitutionProxyInfo.setId("42");
        dummyInstitutionProxyInfo.setO("foo");
        dummyInstitutionProxyInfo.setOrigin("Origin");
        dummyInstitutionProxyInfo.setOriginId("42");
        dummyInstitutionProxyInfo.setOu("Ou");
        dummyInstitutionProxyInfo.setTaxCode("Tax Code");
        dummyInstitutionProxyInfo.setZipCode("21654");

        return dummyInstitutionProxyInfo;
    }


    /**
     * Method under test: {@link InstitutionServiceImpl#retrieveInstitutionById(String)}
     */
    @Test
    void testRetrieveInstitutionById() {
        Institution institution = new Institution();
        when(institutionConnector.findById(any())).thenReturn(institution);
        assertSame(institution, institutionServiceImpl.retrieveInstitutionById("42"));
        verify(institutionConnector).findById(any());
    }

    /**
     * Method under test: {@link InstitutionServiceImpl#retrieveInstitutionById(String)}
     */
    @Test
    void testRetrieveInstitutionById2() {
        when(institutionConnector.findById(any()))
                .thenThrow(new ResourceNotFoundException("An error occurred", "Code"));
        assertThrows(ResourceNotFoundException.class, () -> institutionServiceImpl.retrieveInstitutionById("42"));
        verify(institutionConnector).findById(any());
    }

    /**
     * Method under test: {@link InstitutionServiceImpl#retrieveInstitutionByIdAndProduct(String, String)}
     */
    @Test
    void testRetrieveInstitutionByIdAndProduct() {
        Institution institution = new Institution();
        when(institutionConnector.findByIdAndProduct(any(), anyString())).thenReturn(institution);
        assertSame(institution, institutionServiceImpl.retrieveInstitutionByIdAndProduct("42", "productId"));
        verify(institutionConnector).findByIdAndProduct(any(), anyString());
    }

    /**
     * Method under test: {@link InstitutionServiceImpl#retrieveInstitutionByIdAndProduct(String, String)}
     */
    @Test
    void testRetrieveInstitutionByIdAndProduct_withException() {
        when(institutionConnector.findByIdAndProduct(any(), anyString()))
                .thenThrow(new ResourceNotFoundException("An error occurred", "Code"));
        assertThrows(ResourceNotFoundException.class, () -> institutionServiceImpl.retrieveInstitutionByIdAndProduct("42", "productId"));
        verify(institutionConnector).findByIdAndProduct(any(), anyString());
    }

    /**
     * Method under test: {@link InstitutionServiceImpl#retrieveInstitutionByExternalId(String)}
     */
    @Test
    void testRetrieveInstitutionByExternalId() {
        Institution institution = new Institution();
        when(institutionConnector.findByExternalId(any())).thenReturn(Optional.of(institution));
        assertSame(institution, institutionServiceImpl.retrieveInstitutionByExternalId("42"));
        verify(institutionConnector).findByExternalId(any());
    }

    /**
     * Method under test: {@link InstitutionServiceImpl#retrieveInstitutionByExternalId(String)}
     */
    @Test
    void testRetrieveInstitutionByExternalId2() {
        when(institutionConnector.findByExternalId(any())).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> institutionServiceImpl.retrieveInstitutionByExternalId("42"));
        verify(institutionConnector).findByExternalId(any());
    }

    /**
     * Method under test: {@link InstitutionServiceImpl#retrieveInstitutionByExternalId(String)}
     */
    @Test
    void testRetrieveInstitutionByExternalId3() {
        when(institutionConnector.findByExternalId(any()))
                .thenThrow(new ResourceNotFoundException("An error occurred", "Code"));
        assertThrows(ResourceNotFoundException.class, () -> institutionServiceImpl.retrieveInstitutionByExternalId("42"));
        verify(institutionConnector).findByExternalId(any());
    }

    /**
     * Method under test: {@link InstitutionServiceImpl#createInstitutionByExternalId(String)}
     */
    @Test
    void shouldThrowExceptionOnCreateInstitutionByExternalIdWhenInstitutionFounded() {
        when(institutionConnector.findByExternalId(any())).thenReturn(Optional.of(new Institution()));
        assertThrows(ResourceConflictException.class, () -> institutionServiceImpl.createInstitutionByExternalId("42"));
        verify(institutionConnector).findByExternalId(any());
    }

    /**
     * Method under test: {@link InstitutionServiceImpl#createInstitutionByExternalId(String)}
     */
    @Test
    void testCreateInstitutionByExternalId2() {
        Institution institution = new Institution();
        when(institutionConnector.save(any())).thenReturn(institution);
        when(institutionConnector.findByExternalId(any())).thenReturn(Optional.empty());

        when(partyRegistryProxyConnector.getCategory(any(), any())).thenReturn(createCategoryProxyInfo());
        when(partyRegistryProxyConnector.getInstitutionById(any())).thenReturn(createInstitutionProxyInfo());
        assertSame(institution, institutionServiceImpl.createInstitutionByExternalId("42"));
        verify(institutionConnector).save(any());
        verify(institutionConnector).findByExternalId(any());
        verify(partyRegistryProxyConnector).getCategory(any(), any());
        verify(partyRegistryProxyConnector).getInstitutionById(any());
    }

    /**
     * Method under test: {@link InstitutionServiceImpl#createInstitutionByExternalId(String)}
     */
    @Test
    void shouldThrowExceptionOnCreationInstitutionByExternalIdWhenSaveAlreadyExists() {
        when(institutionConnector.save(any()))
                .thenThrow(new ResourceConflictException("An error occurred", "START - check institution {} already exists"));
        when(institutionConnector.findByExternalId(any())).thenReturn(Optional.empty());

        when(partyRegistryProxyConnector.getCategory(any(), any())).thenReturn(createCategoryProxyInfo());
        when(partyRegistryProxyConnector.getInstitutionById(any())).thenReturn(createInstitutionProxyInfo());
        assertThrows(MsCoreException.class, () -> institutionServiceImpl.createInstitutionByExternalId("42"));
        verify(institutionConnector).save(any());
        verify(institutionConnector).findByExternalId(any());
        verify(partyRegistryProxyConnector).getCategory(any(), any());
        verify(partyRegistryProxyConnector).getInstitutionById(any());
    }


    /**
     * Method under test: {@link InstitutionServiceImpl#createInstitutionByExternalId(String)}
     */
    @Test
    void shouldThrowExceptionOnCreationInstitutionByExternalIdIfAlreadyExists() {
        when(institutionConnector.findByExternalId(any()))
                .thenThrow(new InvalidRequestException("An error occurred", "START - check institution {} already exists"));
        assertThrows(InvalidRequestException.class, () -> institutionServiceImpl.createInstitutionByExternalId("42"));
        verify(institutionConnector).findByExternalId(any());
    }

    /**
     * Method under test: {@link InstitutionServiceImpl#createInstitutionByExternalId(String)}
     */
    @Test
    void testCreateInstitutionByExternalId6() {

        when(partyRegistryProxyConnector.getCategory(any(), any())).thenReturn(createCategoryProxyInfo());
        when(partyRegistryProxyConnector.getInstitutionById(any())).thenReturn(createInstitutionProxyInfo());
        Institution institution = new Institution();
        when(institutionConnector.save(any())).thenReturn(institution);
        when(institutionConnector.findByExternalId(any())).thenReturn(Optional.empty());
        assertSame(institution, institutionServiceImpl.createInstitutionByExternalId("42"));
        verify(partyRegistryProxyConnector).getCategory(any(), any());
        verify(partyRegistryProxyConnector).getInstitutionById(any());
        verify(institutionConnector).save(any());
        verify(institutionConnector).findByExternalId(any());
    }

    /**
     * Method under test: {@link InstitutionServiceImpl#createPgInstitution(String, String, String, boolean, SelfCareUser)}
     */
    @Test
    void testCreatePgInstitutionNotFoundInstitution() {
        String taxId = "taxId";
        Institution institution = new Institution();
        institution.setTaxCode(taxId);
        when(institutionConnector.findByExternalId(any())).thenReturn(Optional.empty());
        InstitutionByLegal institutionByLegal = new InstitutionByLegal();
        institutionByLegal.setBusinessName("test name");
        institutionByLegal.setBusinessTaxId(taxId);
        when(partyRegistryProxyConnector.getInstitutionsByLegal(any())).thenReturn(List.of(institutionByLegal));
        NationalRegistriesProfessionalAddress professionalAddress = new NationalRegistriesProfessionalAddress();
        professionalAddress.setAddress("via di prova");
        professionalAddress.setZipCode("00100");
        when(partyRegistryProxyConnector.getLegalAddress(taxId)).thenReturn(professionalAddress);
        when(institutionConnector.save(any())).thenReturn(institution);
        when(coreConfig.isInfoCamereEnable()).thenReturn(true);
        Institution response = institutionServiceImpl.createPgInstitution(taxId, "42", "42",true, mock(SelfCareUser.class));
        assertEquals(response.getTaxCode(), taxId);
        verify(institutionConnector).findByExternalId(any());
        verify(institutionConnector).save(any());
    }

    /**
     * Method under test: {@link InstitutionServiceImpl#createPgInstitution(String, String, String, boolean, SelfCareUser)}
     */
    @Test
    void testCreatePgInstitutionFoundedInstitution() {
        String taxId = "taxId";
        Institution institution = new Institution();
        institution.setId("institutionId");
        institution.setTaxCode(taxId);
        institution.setExternalId(taxId);
        when(institutionConnector.findByExternalId(taxId)).thenReturn(Optional.of(institution));
        Institution response = institutionServiceImpl.createPgInstitution(taxId, "42", "42",true, mock(SelfCareUser.class));
        assertEquals(response.getId(), institution.getId());
        assertEquals(response.getTaxCode(), taxId);
        assertEquals(response.getExternalId(), taxId);
        verify(institutionConnector).findByExternalId(any());
        verify(institutionConnector, never()).save(any());
    }

    @Test
    void testGetOnboardingInstitutionByProductId() {
        List<Onboarding> onboardings = new ArrayList<>();
        when(institutionConnector.findOnboardingByIdAndProductId(any(), any())).thenReturn(onboardings);
        List<Onboarding> onboardingList = institutionServiceImpl.getOnboardingInstitutionByProductId("id", "id");
        assertTrue(onboardingList.isEmpty());
    }

    @Test
    void testGetInstitutionsWithTaxCodeSubunitCode() {
        List<Institution> institutionList = new ArrayList<>();
        when(institutionConnector.findByTaxCodeAndSubunitCode(any(), any(), any())).thenReturn(institutionList);
        List<Institution> institutions = institutionServiceImpl.getInstitutions("id", "id", null, null, null, null);
        assertTrue(institutions.isEmpty());
        Mockito.verify(institutionConnector).findByTaxCodeAndSubunitCode(any(), any(), any());
    }

    @Test
    void testGetInstitutionsWithOriginOriginId() {
        List<Institution> institutionList = new ArrayList<>();
        when(institutionConnector.findByOriginAndOriginId(any(), any(), any())).thenReturn(institutionList);
        List<Institution> institutions = institutionServiceImpl.getInstitutions(null, null, "id", "id", null, null);
        assertTrue(institutions.isEmpty());
        Mockito.verify(institutionConnector).findByOriginAndOriginId(any(), any(), any());
    }

    @Test
    void testGetInstitutionsWithTaxCodeAndEnableSubunitsTrue() {
        List<Institution> institutionList = new ArrayList<>();
        when(institutionConnector.findByTaxCodeWithSubunits(any(), any())).thenReturn(institutionList);
        List<Institution> institutions = institutionServiceImpl.getInstitutions("taxcode", null, null, null, null, true);
        assertTrue(institutions.isEmpty());
        Mockito.verify(institutionConnector).findByTaxCodeWithSubunits(any(), any());
    }

    @Test
    void testGetInstitutionsWithTaxCodeAndEnableSubunitsFalse() {
        List<Institution> institutionList = new ArrayList<>();
        when(institutionConnector.findByTaxCodeAndSubunitCode(any(), any(), any())).thenReturn(institutionList);
        List<Institution> institutions = institutionServiceImpl.getInstitutions("taxcode", null, null, null, null, false);
        assertTrue(institutions.isEmpty());
        Mockito.verify(institutionConnector).findByTaxCodeAndSubunitCode(any(), any(), any());
    }

    @Test
    void testGetInstitutionsWithTaxCodeAndEnableSubunitsNull() {
        List<Institution> institutionList = new ArrayList<>();
        when(institutionConnector.findByTaxCodeAndSubunitCode(any(), any(), any())).thenReturn(institutionList);
        List<Institution> institutions = institutionServiceImpl.getInstitutions("taxcode", null, null, null, null, null);
        assertTrue(institutions.isEmpty());
        Mockito.verify(institutionConnector).findByTaxCodeAndSubunitCode(any(), any(), any());
    }

    @Test
    void testGetInstitutionsFails() {
        assertThrows(InvalidRequestException.class, () -> institutionServiceImpl.getInstitutions("id", "id", "id", "id", "id", true));
    }

    /**
     * Method under test: {@link InstitutionServiceImpl#createInstitutionFromIpa(String, InstitutionPaSubunitType, String, List, String, String, InstitutionAdditionalInfoForIpa)}
     */
    @Test
    void testCreateInstitutionFromIpa() {
        when(createInstitutionStrategyFactory.createInstitutionStrategyIpa()).thenReturn(createInstitutionStrategy);
        when(createInstitutionStrategy.createInstitution(any())).thenReturn(new Institution());
        Institution institution = institutionServiceImpl.createInstitutionFromIpa("id", InstitutionPaSubunitType.AOO,"id", List.of(), "email", "phone", new InstitutionAdditionalInfoForIpa());
        assertNotNull(institution);
    }

    @Test
    void testCreateInstitution() {
        when(createInstitutionStrategyFactory.createInstitutionStrategy(any())).thenReturn(createInstitutionStrategy);
        when(createInstitutionStrategy.createInstitution(any())).thenReturn(new Institution());
        Institution institution = institutionServiceImpl.createInstitution(new Institution());
        assertNotNull(institution);
    }

    @Test
    void testCreateInstitutionFromAnac() {
        when(createInstitutionStrategyFactory.createInstitutionStrategyAnac(any())).thenReturn(createInstitutionStrategy);
        when(createInstitutionStrategy.createInstitution(any())).thenReturn(new Institution());
        Institution institution = institutionServiceImpl.createInstitutionFromAnac(new Institution());
        assertNotNull(institution);
    }

    @Test
    void testCreateInstitutionFromIvass() {
        when(createInstitutionStrategyFactory.createInstitutionStrategyIvass(any())).thenReturn(createInstitutionStrategy);
        when(createInstitutionStrategy.createInstitution(any())).thenReturn(new Institution());
        Institution institution = institutionServiceImpl.createInstitutionFromIvass(new Institution());
        assertNotNull(institution);
    }


    @Test
    void testCreateInstitutionFromInfocamere() {
        when(createInstitutionStrategyFactory.createInstitutionStrategyInfocamere(any())).thenReturn(createInstitutionStrategy);
        when(createInstitutionStrategy.createInstitution(any())).thenReturn(new Institution());
        Institution institution = institutionServiceImpl.createInstitutionFromInfocamere(new Institution());
        assertNotNull(institution);
    }

    /**
     * Method under test: {@link InstitutionServiceImpl#getInstitutionsByProductId(String, Integer, Integer)}
     */
    @Test
    void testInstitutionsInstitutionsByProductId() {
        List<Institution> institutions = new ArrayList<>();
        when(institutionConnector.findInstitutionsByProductId(any(), any(), any())).thenReturn(institutions);
        List<Institution> institutionsResult = institutionServiceImpl.getInstitutionsByProductId("id", 0, 1);
        assertTrue(institutionsResult.isEmpty());
    }
    /**
     * Method under test: {@link InstitutionServiceImpl#createPgInstitution(String, String, String, boolean, SelfCareUser)}
     */
    @Test
    void testCreatePgInstitution4() {
        Institution institution = new Institution();
        when(institutionConnector.save(any())).thenReturn(institution);
        when(institutionConnector.findByExternalId(any())).thenReturn(Optional.empty());

        InstitutionByLegal institutionByLegal = new InstitutionByLegal();
        institutionByLegal.setBusinessName("START - check institution {} already exists");
        institutionByLegal.setBusinessTaxId("42");

        SelfCareUser selfCareUser = mock(SelfCareUser.class);
        Institution institutionResult = institutionServiceImpl.createPgInstitution("42", "42", "42",false, selfCareUser);
        assertSame(institution, institutionResult);
    }

    /**
     * Method under test: {@link InstitutionServiceImpl#createPgInstitution(String, String, String, boolean, SelfCareUser)}
     */
    @Test
    void testCreatePgInstitution2() {
        Institution institution = new Institution();
        when(coreConfig.isInfoCamereEnable()).thenReturn(true);
        List<InstitutionByLegal> list = new ArrayList<>();
        InstitutionByLegal institutionByLegal = new InstitutionByLegal();
        institutionByLegal.setBusinessName("START - check institution {} already exists");
        institutionByLegal.setBusinessTaxId("42");
        list.add(institutionByLegal);
        when(partyRegistryProxyConnector.getInstitutionsByLegal(any())).thenReturn(list);
        when(institutionConnector.save(any())).thenReturn(institution);
        when(institutionConnector.findByExternalId(any())).thenReturn(Optional.empty());

        SelfCareUser selfCareUser = mock(SelfCareUser.class);
        assertSame(institution, institutionServiceImpl.createPgInstitution("42", "42", "42",true, selfCareUser));
    }

    /**
     * Method under test: {@link InstitutionServiceImpl#createPgInstitution(String, String, String, boolean, SelfCareUser)}
     */
    @Test
    void testCreatePgInstitution5() {
        when(institutionConnector.findByExternalId(any()))
                .thenThrow(new ResourceNotFoundException("An error occurred", "START - check institution {} already exists"));
        assertThrows(ResourceNotFoundException.class,
                () -> institutionServiceImpl.createPgInstitution("42", "42", "42",true, mock(SelfCareUser.class)));
        verify(institutionConnector).findByExternalId(any());
    }

    /**
     * Method under test: {@link InstitutionServiceImpl#createPgInstitution(String, String, String, boolean, SelfCareUser)}
     */
    @Test
    void testCreatePgInstitution6() {
        Institution institution = new Institution();
        when(institutionConnector.save(any())).thenReturn(institution);
        when(institutionConnector.findByExternalId(any())).thenReturn(Optional.empty());

        InstitutionByLegal institutionByLegal = new InstitutionByLegal();
        institutionByLegal.setBusinessName("START - check institution {} already exists");
        institutionByLegal.setBusinessTaxId("42");

        SelfCareUser selfCareUser = mock(SelfCareUser.class);
        assertSame(institution, institutionServiceImpl.createPgInstitution("42", "42","42", false, selfCareUser));
        verify(institutionConnector).save(any());
        verify(institutionConnector).findByExternalId(any());
    }

    /**
     * Method under test: {@link InstitutionServiceImpl#createPgInstitution(String, String, String, boolean, SelfCareUser)}
     */
    @Test
    void testCreatePgInstitution7() {
        when(institutionConnector.save(any()))
                .thenThrow(new ResourceConflictException("An error occurred", "START - check institution {} already exists"));
        when(institutionConnector.findByExternalId(any())).thenReturn(Optional.empty());

        InstitutionByLegal institutionByLegal = new InstitutionByLegal();
        institutionByLegal.setBusinessName("START - check institution {} already exists");
        institutionByLegal.setBusinessTaxId("42");

        SelfCareUser selfCareUser = mock(SelfCareUser.class);
        assertThrows(ResourceConflictException.class,
                () -> institutionServiceImpl.createPgInstitution("42", "42", "42",true, selfCareUser));
    }

    /**
     * Method under test: {@link InstitutionServiceImpl#createPgInstitution(String, String, String, boolean, SelfCareUser)}
     */
    @Test
    void testCreatePgInstitution9() {
        InstitutionByLegal institutionByLegal = new InstitutionByLegal();
        institutionByLegal.setBusinessName("START - check institution {} already exists");
        institutionByLegal.setBusinessTaxId("42");

        NationalRegistriesProfessionalAddress nationalRegistriesProfessionalAddress = new NationalRegistriesProfessionalAddress();
        nationalRegistriesProfessionalAddress.setAddress("42 Main St");
        nationalRegistriesProfessionalAddress.setZipCode("21654");
        Institution institution = new Institution();
        when(institutionConnector.save(any())).thenReturn(institution);
        when(institutionConnector.findByExternalId(any())).thenReturn(Optional.empty());
        SelfCareUser selfCareUser = mock(SelfCareUser.class);
        assertSame(institution, institutionServiceImpl.createPgInstitution("42", "42", "42",true, selfCareUser));
    }

    /**
     * Method under test: {@link InstitutionServiceImpl#createPgInstitution(String, String, String, boolean, SelfCareUser)}
     */
    @Test
    void testCreatePgInstitution14() {
        InstitutionByLegal institutionByLegal = new InstitutionByLegal();
        institutionByLegal.setBusinessName("START - check institution {} already exists");
        institutionByLegal.setBusinessTaxId("42");

        NationalRegistriesProfessionalAddress nationalRegistriesProfessionalAddress = new NationalRegistriesProfessionalAddress();
        nationalRegistriesProfessionalAddress.setAddress("42 Main St");
        nationalRegistriesProfessionalAddress.setZipCode("21654");
        Institution institution = new Institution();
        when(institutionConnector.save(any())).thenReturn(institution);
        when(institutionConnector.findByExternalId(any())).thenReturn(Optional.empty());
        SelfCareUser selfCareUser = mock(SelfCareUser.class);
        assertSame(institution, institutionServiceImpl.createPgInstitution("42", "42", "42", false, selfCareUser));
        verify(institutionConnector).save(any());
        verify(institutionConnector).findByExternalId(any());
    }

    /**
     * Method under test: {@link InstitutionServiceImpl#retrieveInstitutionProducts(Institution, List)}
     */
    @Test
    void testRetrieveInstitutionProducts() {
        Institution institution = new Institution();
        Onboarding onboarding = new Onboarding();
        onboarding.setStatus(RelationshipState.PENDING);
        List<RelationshipState> list = new ArrayList<>();
        list.add(RelationshipState.PENDING);
        institution.setOnboarding(List.of(onboarding));
        assertEquals(1, institutionServiceImpl.retrieveInstitutionProducts(institution, list).size());
    }

    /**
     * Method under test: {@link InstitutionServiceImpl#retrieveInstitutionProducts(Institution, List)}
     */
    @Test
    void testRetrieveInstitutionProducts3() {
        Institution institution = new Institution();
        Onboarding onboarding = new Onboarding();
        onboarding.setStatus(RelationshipState.PENDING);
        institution.setOnboarding(List.of(onboarding));
        List<RelationshipState> list = new ArrayList<>();
        assertEquals(1, institutionServiceImpl.retrieveInstitutionProducts(institution, list).size());
    }

    /**
     * Method under test: {@link InstitutionServiceImpl#retrieveInstitutionProduct(String, String)}
     */
    @Test
    void testRetrieveInstitutionProduct2() {

        Institution institution = new Institution();
        when(institutionConnector.findByExternalIdAndProductId(any(), any())).thenReturn(institution);


        assertSame(institution, institutionServiceImpl.retrieveInstitutionProduct("42", "42"));
        verify(institutionConnector).findByExternalIdAndProductId(any(), any());
    }

    /**
     * Method under test: {@link InstitutionServiceImpl#retrieveInstitutionGeoTaxonomies(Institution)}
     */
    @Test
    void testRetrieveInstitutionGeoTaxonomies2() {

        Institution institution = new Institution();
        institution.setGeographicTaxonomies(new ArrayList<>());
        Assertions.assertThrows(MsCoreException.class, () -> institutionServiceImpl.retrieveInstitutionGeoTaxonomies(institution));
    }

    /**
     * Method under test: {@link InstitutionServiceImpl#retrieveInstitutionGeoTaxonomies(Institution)}
     */
    @Test
    void testRetrieveInstitutionGeoTaxonomies4() {
        GeographicTaxonomies geographicTaxonomies = new GeographicTaxonomies();
        geographicTaxonomies.setGeotaxId("Code");
        geographicTaxonomies.setCountry("GB");
        geographicTaxonomies.setCountryAbbreviation("GB");
        geographicTaxonomies.setDescription("The characteristics of someone or something");
        geographicTaxonomies.setEnable(true);
        geographicTaxonomies.setIstatCode("");
        geographicTaxonomies.setProvinceId("Province");
        geographicTaxonomies.setProvinceAbbreviation("Province Abbreviation");
        geographicTaxonomies.setRegionId("us-east-2");

        when(partyRegistryProxyConnector.getExtByCode(any())).thenReturn(geographicTaxonomies);

        ArrayList<InstitutionGeographicTaxonomies> institutionGeographicTaxonomiesList = new ArrayList<>();
        institutionGeographicTaxonomiesList.add(new InstitutionGeographicTaxonomies(
                "Retrieving geographic taxonomies for institution {}", "The characteristics of someone or something"));

        Institution institution = new Institution();
        institution.setGeographicTaxonomies(institutionGeographicTaxonomiesList);
        assertEquals(1, institutionServiceImpl.retrieveInstitutionGeoTaxonomies(institution).size());
        verify(partyRegistryProxyConnector).getExtByCode(any());
    }

    /**
     * Method under test: {@link InstitutionService#updateInstitution(String, InstitutionUpdate, String)}
     */
    @Test
    void updateInstitution_shouldThrowExceptionIfGeotaxNotFound() {

        when(partyRegistryProxyConnector.getExtByCode(any()))
                .thenThrow(new ResourceNotFoundException("An error occurred", "Code"));

        ArrayList<InstitutionGeographicTaxonomies> institutionGeographicTaxonomiesList = new ArrayList<>();
        institutionGeographicTaxonomiesList
                .add(new InstitutionGeographicTaxonomies("Code", "The characteristics of someone or something"));

        InstitutionUpdate institutionUpdate = new InstitutionUpdate();
        institutionUpdate.setAddress("42 Main St");
        institutionUpdate.setBusinessRegisterPlace("Business Register Place");
        institutionUpdate
                .setDataProtectionOfficer(new DataProtectionOfficer("42 Main St", "jane.doe@example.org", "Pec"));
        institutionUpdate.setDescription("The characteristics of someone or something");
        institutionUpdate.setDigitalAddress("42 Main St");
        institutionUpdate.setGeographicTaxonomies(institutionGeographicTaxonomiesList);
        institutionUpdate.setImported(true);
        institutionUpdate.setInstitutionType(InstitutionType.PA);
        institutionUpdate
                .setPaymentServiceProvider(new PaymentServiceProvider("Abi Code", "42", "Legal Register Name", "42", true));
        institutionUpdate.setRea("Rea");
        institutionUpdate.setShareCapital("Share Capital");
        institutionUpdate.setSupportEmail("jane.doe@example.org");
        institutionUpdate.setSupportPhone("6625550144");
        institutionUpdate.setTaxCode("Tax Code");
        institutionUpdate.setZipCode("21654");
        assertThrows(MsCoreException.class,
                () -> institutionServiceImpl.updateInstitution("42", institutionUpdate, "42"));
        verify(partyRegistryProxyConnector).getExtByCode(any());
    }

    /**
     * Method under test: {@link InstitutionServiceImpl#updateInstitutionDelegation(String, boolean)} (String, InstitutionUpdate, String)}
     */
    @Test
    void testUpdateInstitutionDelegation() {
        InstitutionUpdate institutionUpdate = new InstitutionUpdate();
        institutionUpdate.setDelegation(true);
        assertDoesNotThrow(
                () -> institutionServiceImpl.updateInstitutionDelegation("42", true));
        verify(institutionConnector).findAndUpdate("42",null, institutionUpdate);
    }

    /**
     * Method under test: {@link InstitutionServiceImpl#retrieveInstitutionByExternalIds(List, String)}
     */
    @Test
    void testRetrieveInstitutionByExternalIds2() {

        when(institutionConnector.findByExternalIdsAndProductId(any(), any())).thenReturn(List.of());

        ArrayList<ValidInstitution> validInstitutionList = new ArrayList<>();
        List<ValidInstitution> actualRetrieveInstitutionByExternalIdsResult = institutionServiceImpl
                .retrieveInstitutionByExternalIds(validInstitutionList, "42");

        assertSame(validInstitutionList, actualRetrieveInstitutionByExternalIdsResult);
        assertTrue(actualRetrieveInstitutionByExternalIdsResult.isEmpty());
        verify(institutionConnector).findByExternalIdsAndProductId(any(), any());
    }

    /**
     * Method under test: {@link InstitutionServiceImpl#retrieveInstitutionByExternalIds(List, String)}
     */
    @Test
    void testRetrieveInstitutionByExternalIds3() {

        when(institutionConnector.findByExternalIdsAndProductId(any(), any())).thenReturn(List.of());

        ArrayList<ValidInstitution> validInstitutionList = new ArrayList<>();
        validInstitutionList.add(new ValidInstitution("42", "The characteristics of someone or something"));
        List<ValidInstitution> actualRetrieveInstitutionByExternalIdsResult = institutionServiceImpl
                .retrieveInstitutionByExternalIds(validInstitutionList, "42");
        assertSame(validInstitutionList, actualRetrieveInstitutionByExternalIdsResult);
        assertEquals(1, actualRetrieveInstitutionByExternalIdsResult.size());
        verify(institutionConnector).findByExternalIdsAndProductId(any(), any());
    }

    /**
     * Method under test: {@link InstitutionServiceImpl#retrieveInstitutionByExternalIds(List, String)}
     */
    @Test
    void testRetrieveInstitutionByExternalIds4() {


        when(institutionConnector.findByExternalIdsAndProductId(any(), any())).thenReturn(new ArrayList<>());

        ArrayList<ValidInstitution> validInstitutionList = new ArrayList<>();
        validInstitutionList.add(new ValidInstitution("42", "The characteristics of someone or something"));
        validInstitutionList.add(new ValidInstitution("42", "The characteristics of someone or something"));
        List<ValidInstitution> actualRetrieveInstitutionByExternalIdsResult = institutionServiceImpl
                .retrieveInstitutionByExternalIds(validInstitutionList, "42");
        assertSame(validInstitutionList, actualRetrieveInstitutionByExternalIdsResult);
        assertEquals(2, actualRetrieveInstitutionByExternalIdsResult.size());
        verify(institutionConnector).findByExternalIdsAndProductId(any(), any());
    }

    /**
     * Method under test: {@link InstitutionServiceImpl#retrieveInstitutionByExternalIds(List, String)}
     */
    @Test
    void testRetrieveInstitutionByExternalIds5() {

        ArrayList<String> stringList = new ArrayList<>();
        stringList.add("42");

        when(institutionConnector.findByExternalIdsAndProductId(any(), any())).thenReturn(stringList);

        ArrayList<ValidInstitution> validInstitutionList = new ArrayList<>();
        validInstitutionList.add(new ValidInstitution("42", "The characteristics of someone or something"));
        List<ValidInstitution> actualRetrieveInstitutionByExternalIdsResult = institutionServiceImpl
                .retrieveInstitutionByExternalIds(validInstitutionList, "42");
        assertSame(validInstitutionList, actualRetrieveInstitutionByExternalIdsResult);
        assertTrue(actualRetrieveInstitutionByExternalIdsResult.isEmpty());
        verify(institutionConnector).findByExternalIdsAndProductId(any(), any());
    }

    /**
     * Method under test: {@link InstitutionServiceImpl#findInstitutionsByGeoTaxonomies(String, SearchMode)}
     */
    @Test
    void testFindInstitutionsByGeoTaxonomies3() {

        ArrayList<Institution> institutionList = new ArrayList<>();
        when(institutionConnector.findByGeotaxonomies(any(), any()))
                .thenReturn(institutionList);
        List<Institution> actualFindInstitutionsByGeoTaxonomiesResult = institutionServiceImpl
                .findInstitutionsByGeoTaxonomies("Geo Taxonomies", SearchMode.ALL);
        assertSame(institutionList, actualFindInstitutionsByGeoTaxonomiesResult);
        assertTrue(actualFindInstitutionsByGeoTaxonomiesResult.isEmpty());
        verify(institutionConnector).findByGeotaxonomies(any(), any());
    }

    @Test
    void testFindInstitutionsByGeoTaxonomies1() {
        assertThrows(InvalidRequestException.class, () -> institutionServiceImpl.findInstitutionsByGeoTaxonomies("", SearchMode.ALL));
    }

    /**
     * Method under test: {@link InstitutionServiceImpl#findInstitutionsByProductId(String)}
     */
    @Test
    void testFindInstitutionsByProductId2() {
        when(institutionConnector.findByProductId(any())).thenReturn(new ArrayList<>());
        assertThrows(ResourceNotFoundException.class, () -> institutionServiceImpl.findInstitutionsByProductId("42"));
    }


    /**
     * Method under test: {@link InstitutionServiceImpl#findInstitutionsByProductId(String)}
     */
    @Test
    void testFindInstitutionsByProductId3() {
        when(institutionConnector.findByProductId(any())).thenReturn(List.of(new Institution()));
        assertDoesNotThrow(() -> institutionServiceImpl.findInstitutionsByProductId("42"));
    }


    /**
     * Method under test: {@link InstitutionServiceImpl#retrieveInstitutionByIds(List)}
     */
    @Test
    void testRetrieveInstitutionByIds2() {

        ArrayList<Institution> institutionList = new ArrayList<>();
        when(institutionConnector.findAllByIds(any())).thenReturn(institutionList);

        List<Institution> actualRetrieveInstitutionByIdsResult = institutionServiceImpl
                .retrieveInstitutionByIds(new ArrayList<>());
        assertSame(institutionList, actualRetrieveInstitutionByIdsResult);
        assertTrue(actualRetrieveInstitutionByIdsResult.isEmpty());
        verify(institutionConnector).findAllByIds(any());
    }

    /**
     * Method under test: {@link InstitutionServiceImpl#retrieveInstitutionProduct(String, String)}
     */
    @Test
    void testGetInstitutionProduct2() {
        when(institutionConnector.findByExternalIdAndProductId(any(), any()))
                .thenThrow(new ResourceNotFoundException("An error occurred", "Code"));
        assertThrows(ResourceNotFoundException.class, () -> institutionServiceImpl.retrieveInstitutionProduct("42", "42"));
        verify(institutionConnector).findByExternalIdAndProductId(any(), any());
    }

    /**
     * Method under test: {@link InstitutionServiceImpl#retrieveGeoTaxonomies(String)}
     */
    @Test
    void testGetGeoTaxonomies() {
        GeographicTaxonomies geographicTaxonomies = new GeographicTaxonomies();
        geographicTaxonomies.setGeotaxId("Code");
        geographicTaxonomies.setCountry("GB");
        geographicTaxonomies.setCountryAbbreviation("GB");
        geographicTaxonomies.setDescription("The characteristics of someone or something");
        geographicTaxonomies.setEnable(true);
        geographicTaxonomies.setIstatCode("");
        geographicTaxonomies.setProvinceId("Province");
        geographicTaxonomies.setProvinceAbbreviation("Province Abbreviation");
        geographicTaxonomies.setRegionId("us-east-2");
        when(partyRegistryProxyConnector.getExtByCode(any())).thenReturn(geographicTaxonomies);
        Optional<GeographicTaxonomies> optionalGeographicTaxonomies = institutionServiceImpl.retrieveGeoTaxonomies("Code");
        assertTrue(optionalGeographicTaxonomies.isPresent());
        assertSame(geographicTaxonomies,optionalGeographicTaxonomies.get());
        verify(partyRegistryProxyConnector).getExtByCode(any());
    }

    /**
     * Method under test: {@link InstitutionServiceImpl#retrieveGeoTaxonomies(String)}
     */
    @Test
    void getGeoTaxonomies_whenGeoTaxIsEmpty() {
        when(partyRegistryProxyConnector.getExtByCode(any()))
                .thenThrow(new ResourceNotFoundException("",""));
        assertTrue(institutionServiceImpl.retrieveGeoTaxonomies("Code").isEmpty());
    }

    /**
     * Method under test: {@link InstitutionServiceImpl#retrieveInstitutionsWithFilter(String, String, List)}
     */
    @Test
    void testRetrieveInstitutionsWithFilter() {
        when(institutionConnector.findWithFilter(any(), any(), any()))
                .thenReturn(new ArrayList<>());
        List<RelationshipState> list = List.of(RelationshipState.ACTIVE);
        assertThrows(ResourceNotFoundException.class,
                () -> institutionServiceImpl.retrieveInstitutionsWithFilter("42", "42", list));
        verify(institutionConnector).findWithFilter(any(), any(), any());
    }

    /**
     * Method under test: {@link InstitutionServiceImpl#retrieveInstitutionsWithFilter(String, String, List)}
     */
    @Test
    void testRetrieveInstitutionsWithFilter2() {
        ArrayList<Institution> institutionList = new ArrayList<>();
        institutionList.add(new Institution());
        when(institutionConnector.findWithFilter(any(), any(), any()))
                .thenReturn(institutionList);
        institutionServiceImpl.retrieveInstitutionsWithFilter("42", "42", new ArrayList<>());
        verify(institutionConnector).findWithFilter(any(), any(), any());
    }

    /**
     * Method under test: {@link InstitutionServiceImpl#retrieveInstitutionsWithFilter(String, String, List)}
     */
    @Test
    void testRetrieveInstitutionsWithFilter3() {
        when(institutionConnector.findWithFilter(any(), any(), any()))
                .thenThrow(new ResourceNotFoundException("An error occurred", "Code"));
        List<RelationshipState> list = List.of(RelationshipState.ACTIVE);
        assertThrows(ResourceNotFoundException.class,
                () -> institutionServiceImpl.retrieveInstitutionsWithFilter("42", "42", list));
        verify(institutionConnector).findWithFilter(any(), any(), any());
    }

    /**
     * Method under test: {@link InstitutionServiceImpl#retrieveInstitutionsWithFilter(String, String, List)}
     */
    @Test
    void testRetrieveInstitutionsWithFilter6() {

        when(institutionConnector.findWithFilter(any(), any(), any()))
                .thenReturn(List.of(new Institution()));

        institutionServiceImpl.retrieveInstitutionsWithFilter("42", "42", new ArrayList<>());
        verify(institutionConnector).findWithFilter(any(), any(), any());
    }

    @Test
    void testUpdateInstitutionDescription() {
        //given
        final String institutionId = "id";

        Institution updatedInstitution = new Institution();
        updatedInstitution.setId(institutionId);
        updatedInstitution.setDescription("newDesc");
        updatedInstitution.setParentDescription("newRootName");

        InstitutionUpdate institutionUpdate = new InstitutionUpdate();
        institutionUpdate.setDescription("newDesc");
        institutionUpdate.setParentDescription("newRootName");

        //when
        when(institutionConnector.findById(institutionId)).thenReturn(new Institution());
        when(institutionConnector.findAndUpdate(institutionId, Collections.emptyList(), institutionUpdate)).thenReturn(updatedInstitution);

        //then
        Institution result = institutionServiceImpl.updateInstitution(institutionId, institutionUpdate, "userId");
        verify(delegationConnector).updateDelegation(updatedInstitution);
        verify(userApiConnector).updateUserInstitution(institutionId, institutionUpdate);
        assertEquals(result, updatedInstitution);
    }

    @Test
    void testUpdateInstitution() {
        //given
        final String institutionId = "id";

        Billing billing = new Billing();
        billing.setVatNumber("newVatNumber");

        Onboarding onboarding = new Onboarding();
        onboarding.setBilling(billing);

        Institution updatedInstitution = new Institution();
        updatedInstitution.setId(institutionId);
        updatedInstitution.setDescription("newDesc");
        updatedInstitution.setParentDescription("newRootName");
        updatedInstitution.setDigitalAddress("newDigitalAddress");
        updatedInstitution.setAddress("newAddress");
        updatedInstitution.setZipCode("newZipCode");
        updatedInstitution.setOnboarding(List.of(onboarding));
        updatedInstitution.setRea("AA-00000");
        updatedInstitution.setShareCapital("9999");
        updatedInstitution.setBusinessRegisterPlace("registerPlace");
        updatedInstitution.setSupportEmail("test@test.test");

        InstitutionUpdate institutionUpdate = new InstitutionUpdate();
        institutionUpdate.setDescription("newDesc");
        institutionUpdate.setParentDescription("newRootName");
        institutionUpdate.setDigitalAddress("newDigitalAddress");
        institutionUpdate.setAddress("newAddress");
        institutionUpdate.setZipCode("newZipCode");
        institutionUpdate.setOnboardings(List.of(onboarding));
        institutionUpdate.setRea("AA-00000");
        institutionUpdate.setShareCapital("9999");
        institutionUpdate.setBusinessRegisterPlace("registerPlace");
        institutionUpdate.setSupportEmail("test@test.test");

        //when
        when(institutionConnector.findById(institutionId)).thenReturn(new Institution());
        when(institutionConnector.findAndUpdate(institutionId, Collections.emptyList(), institutionUpdate)).thenReturn(updatedInstitution);

        //then
        Institution result = institutionServiceImpl.updateInstitution(institutionId, institutionUpdate, "userId");
        verify(delegationConnector).updateDelegation(updatedInstitution);
        verify(userApiConnector).updateUserInstitution(institutionId, institutionUpdate);
        assertEquals(result, updatedInstitution);
    }

    @Test
    void testUpdateInstitutionDescription_updateDelegationsFails() {
        //given
        final String institutionId = "id";

        Institution outdatedInstitution = new Institution();
        outdatedInstitution.setId(institutionId);
        outdatedInstitution.setDescription("oldDesc");
        outdatedInstitution.setParentDescription("oldRootName");

        Institution updatedInstitution = new Institution();
        updatedInstitution.setId(institutionId);
        updatedInstitution.setDescription("newDesc");
        updatedInstitution.setParentDescription("newRootName");

        InstitutionUpdate institutionUpdate = new InstitutionUpdate();
        institutionUpdate.setDescription("newDesc");
        institutionUpdate.setParentDescription("newRootName");

        InstitutionUpdate institutionRollback = new InstitutionUpdate();
        institutionRollback.setDescription("oldDesc");
        institutionRollback.setParentDescription("oldRootName");

        //when
        when(institutionConnector.findById(institutionId)).thenReturn(outdatedInstitution);
        when(institutionConnector.findAndUpdate(institutionId, Collections.emptyList(), institutionUpdate)).thenReturn(updatedInstitution);
        doThrow(new RuntimeException()).when(delegationConnector).updateDelegation(updatedInstitution);

        Executable executable = () -> institutionServiceImpl.updateInstitution(institutionId, institutionUpdate, "userId");
        // Then
        assertThrows(MsCoreException.class, executable);
        verify(institutionConnector).findAndUpdate(institutionId, null, institutionRollback);
        verifyNoInteractions(userApiConnector);
    }

    @Test
    void testUpdateInstitutionDescription_updateUserInstitutionFails() {
        //given
        final String institutionId = "id";

        Institution outdatedInstitution = new Institution();
        outdatedInstitution.setId(institutionId);
        outdatedInstitution.setDescription("oldDesc");
        outdatedInstitution.setParentDescription("oldRootName");

        Institution updatedInstitution = new Institution();
        updatedInstitution.setId(institutionId);
        updatedInstitution.setDescription("newDesc");
        updatedInstitution.setParentDescription("newRootName");

        InstitutionUpdate institutionUpdate = new InstitutionUpdate();
        institutionUpdate.setDescription("newDesc");
        institutionUpdate.setParentDescription("newRootName");

        InstitutionUpdate institutionRollback = new InstitutionUpdate();
        institutionRollback.setDescription("oldDesc");
        institutionRollback.setParentDescription("oldRootName");

        //when
        when(institutionConnector.findById(institutionId)).thenReturn(outdatedInstitution);
        when(institutionConnector.findAndUpdate(institutionId, Collections.emptyList(), institutionUpdate)).thenReturn(updatedInstitution);
        doThrow(new RuntimeException()).when(userApiConnector).updateUserInstitution(institutionId, institutionUpdate);

        Executable executable = () -> institutionServiceImpl.updateInstitution(institutionId, institutionUpdate, "userId");

        //then
        assertThrows(MsCoreException.class, executable);
        verify(institutionConnector).findAndUpdate(institutionId,null, institutionRollback);
        verify(delegationConnector).updateDelegation(outdatedInstitution);
    }

    @Test
    void updateInstitution_noDescription(){
        //when
        when(institutionConnector.findById(any())).thenReturn(new Institution());
        when(institutionConnector.findAndUpdate(any(), any(), any())).thenReturn(new Institution());

        institutionServiceImpl.updateInstitution("id", new InstitutionUpdate(), "userId");

        // Then
        verifyNoInteractions(delegationConnector);
        verifyNoInteractions(userApiConnector);
    }

    @Test
    void updateCreatedAt() {
        // Given
        String institutionIdMock = "institutionIdMock";
        String productIdMock = "productId";
        OffsetDateTime createdAtMock = OffsetDateTime.parse("2020-11-01T02:15:30+01:00");
        OffsetDateTime activatedAtMock = OffsetDateTime.parse("2020-11-02T02:15:30+01:00");

        Onboarding onboardingMock1 = mockInstance(new Onboarding());
        onboardingMock1.setStatus(RelationshipState.ACTIVE);
        onboardingMock1.setClosedAt(null);
        onboardingMock1.setCreatedAt(OffsetDateTime.parse("2023-11-01T02:15:30+01:00"));
        Onboarding onboardingMock2 = mockInstance(new Onboarding());
        onboardingMock2.setProductId(productIdMock);
        onboardingMock2.setTokenId("222e4444-e99b-11d3-a446-422114890100");
        onboardingMock2.setStatus(RelationshipState.ACTIVE);
        onboardingMock2.setClosedAt(null);
        onboardingMock1.setCreatedAt(OffsetDateTime.parse("2020-11-01T02:15:30+01:00"));
        Onboarding onboardingMock3 = mockInstance(new Onboarding());
        onboardingMock3.setStatus(RelationshipState.ACTIVE);
        onboardingMock3.setClosedAt(null);
        onboardingMock1.setCreatedAt(OffsetDateTime.parse("2022-12-11T02:15:30+01:00"));

        Institution updatedInstitutionMock = mockInstance(new Institution());
        updatedInstitutionMock.setId("123e4567-e89b-12d3-a456-426614174000");
        updatedInstitutionMock.setExternalId("00099991238");
        updatedInstitutionMock.setDigitalAddress("DigitalAddress@example.com");
        updatedInstitutionMock.setTaxCode(updatedInstitutionMock.getExternalId());
        updatedInstitutionMock.setOnboarding(List.of(onboardingMock1, onboardingMock2, onboardingMock3));
        updatedInstitutionMock.setGeographicTaxonomies(Collections.emptyList());
        updatedInstitutionMock.setPaymentServiceProvider(null);
        updatedInstitutionMock.setDataProtectionOfficer(null);
        updatedInstitutionMock.setImported(false);
        updatedInstitutionMock.setCreatedAt(OffsetDateTime.parse("2019-11-01T02:15:30+01:00"));
        updatedInstitutionMock.setUpdatedAt(OffsetDateTime.now());

        TokenUser tokenUserMock1 = mockInstance(new TokenUser());
        tokenUserMock1.setUserId("999e9999-e89b-12d3-a456-426614174000");
        TokenUser tokenUserMock2 = mockInstance(new TokenUser());
        tokenUserMock2.setUserId("321e9876-e89b-12d3-a456-426614174000");
        tokenUserMock2.setRole(PartyRole.DELEGATE);

        when(institutionConnector.updateOnboardedProductCreatedAt(institutionIdMock, productIdMock, createdAtMock))
                .thenReturn(updatedInstitutionMock);

        // When
        institutionServiceImpl.updateCreatedAt(institutionIdMock, productIdMock, createdAtMock, activatedAtMock);
        // Then
        verify(institutionConnector, times(1))
                .updateOnboardedProductCreatedAt(institutionIdMock, productIdMock, createdAtMock);
        verifyNoMoreInteractions(institutionConnector);
    }

    @Test
    void updateCreatedAt_nullInstitutionId() {
        // Given
        String productIdMock = "productId";
        OffsetDateTime createdAtMock = OffsetDateTime.parse("2020-11-01T02:15:30+01:00");
        // When
        Executable executable = () -> institutionServiceImpl.updateCreatedAt(null, productIdMock, createdAtMock, null);
        // Then
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, executable);
        assertEquals("An institution ID is required.", illegalArgumentException.getMessage());
        verifyNoInteractions(institutionConnector);
    }

    @Test
    void updateCreatedAt_nullProductId() {
        // Given
        String institutionIdMock = "institutionId";
        OffsetDateTime createdAtMock = OffsetDateTime.parse("2020-11-01T02:15:30+01:00");
        // When
        Executable executable = () -> institutionServiceImpl.updateCreatedAt(institutionIdMock, null, createdAtMock, null);
        // Then
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, executable);
        assertEquals("A product ID is required.", illegalArgumentException.getMessage());
        verifyNoInteractions(institutionConnector);
    }

    @Test
    void updateCreatedAt_nullCreatedAt() {
        // Given
        String institutionIdMock = "institutionId";
        String productIdMock = "producttId";
        // When
        Executable executable = () -> institutionServiceImpl.updateCreatedAt(institutionIdMock, productIdMock, null, null);
        // Then
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, executable);
        assertEquals("A createdAt date is required.", illegalArgumentException.getMessage());
        verifyNoInteractions(institutionConnector);

    }

    @Test
    void updateCreatedAt_onboardingNotFound() {
        // Given
        String institutionIdMock = "institutionId";
        String productIdMock = "producttId";
        OffsetDateTime createdAtMock = OffsetDateTime.parse("2020-11-01T02:15:30+01:00");
        OffsetDateTime activatedAtMock = OffsetDateTime.parse("2020-11-02T02:15:30+01:00");

        Institution institutionMock = mockInstance(new Institution());
        institutionMock.setOnboarding(Collections.emptyList());
        when(institutionConnector.updateOnboardedProductCreatedAt(institutionIdMock, productIdMock, createdAtMock))
                .thenThrow(ResourceNotFoundException.class);
        // When
        Executable executable = () -> institutionServiceImpl.updateCreatedAt(institutionIdMock, productIdMock, createdAtMock, activatedAtMock);
        // Then
        assertThrows(ResourceNotFoundException.class, executable);
    }

    /**
     * Method under test: {@link InstitutionServiceImpl#getInstitutionBrokers(String, InstitutionType)}
     */
    @Test
    void getInstitutionBrokers() {

        Institution institution = new Institution();
        institution.setId("id");
        when(institutionConnector.findBrokers(any(), any())).thenReturn(List.of(institution));
        List<Institution> institutions = institutionServiceImpl.getInstitutionBrokers("42", InstitutionType.PT);
        assertNotNull(institutions);
        assertFalse(institutions.isEmpty());
        assertNotNull(institutions.get(0));
        assertEquals(institutions.get(0).getId(), institution.getId());
        verify(institutionConnector).findBrokers(any(), any());

    }

    /**
     * Method under test: {@link InstitutionServiceImpl#getInstitutions(String, String)}
     */
    @Test
    void getInstitutionsByTaxCode() {

        Institution institution = new Institution();
        institution.setId("id");
        when(institutionConnector.findByTaxCodeAndSubunitCode(any(), any(), eq(null))).thenReturn(List.of(institution));
        List<Institution> institutions = institutionServiceImpl.getInstitutions("1111111", null);
        assertNotNull(institutions);
        assertFalse(institutions.isEmpty());
        assertNotNull(institutions.get(0));
        assertEquals(institutions.get(0).getId(), institution.getId());
        verify(institutionConnector).findByTaxCodeAndSubunitCode(any(), any(), eq(null));

    }

    @Test
    void getLogoTest() {
        String institutionId = "institutionId";
        String expectedUrl = "https://test.it/institutions/institutionId/logo.png";
        String baseUrl = "https://test.it/institutions/";
        when(coreConfig.getLogoUrl()).thenReturn(baseUrl);
        String actual = institutionServiceImpl.getLogo(institutionId);
        assertEquals(expectedUrl, actual);
    }

}

