package it.pagopa.selfcare.mscore.core.strategy;


import it.pagopa.selfcare.mscore.api.InstitutionConnector;
import it.pagopa.selfcare.mscore.api.PartyRegistryProxyConnector;
import it.pagopa.selfcare.mscore.constant.Origin;
import it.pagopa.selfcare.mscore.core.mapper.InstitutionMapper;
import it.pagopa.selfcare.mscore.core.mapper.InstitutionMapperImpl;
import it.pagopa.selfcare.mscore.core.strategy.factory.CreateInstitutionStrategyFactory;
import it.pagopa.selfcare.mscore.core.strategy.input.CreateInstitutionStrategyInput;
import it.pagopa.selfcare.mscore.core.util.InstitutionPaSubunitType;
import it.pagopa.selfcare.mscore.core.util.TestUtils;
import it.pagopa.selfcare.mscore.exception.ResourceNotFoundException;
import it.pagopa.selfcare.mscore.model.AreaOrganizzativaOmogenea;
import it.pagopa.selfcare.mscore.model.UnitaOrganizzativa;
import it.pagopa.selfcare.mscore.model.institution.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateInstitutionStrategyTest {

    @InjectMocks
    CreateInstitutionStrategyFactory strategyFactory;

    @Mock
    InstitutionConnector institutionConnector;
    @Mock
    PartyRegistryProxyConnector partyRegistryProxyConnector;
    @Spy
    InstitutionMapper institutionMapper = new InstitutionMapperImpl();

    @InjectMocks
    CreateInstitutionStrategyIvass createInstitutionStrategyIvass;


    private static final InstitutionProxyInfo dummyInstitutionProxyInfo;
    private static final CategoryProxyInfo dummyCategoryProxyInfo;

    private static final AreaOrganizzativaOmogenea dummyAreaOrganizzativaOmogenea;
    private static final GeographicTaxonomies dummyGeotaxonomies;
    private static final InstitutionGeographicTaxonomies dummyInstitutionGeotaxonomies;
    public static final String SUPPORT_EMAIL = "email";
    public static final String SUPPORT_PHONE = "phone";

    static {
        dummyInstitutionProxyInfo = new InstitutionProxyInfo();
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
        dummyInstitutionProxyInfo.setIstatCode("ecIstatCode");

        dummyCategoryProxyInfo = new CategoryProxyInfo();
        dummyCategoryProxyInfo.setCode("Code");
        dummyCategoryProxyInfo.setKind("Kind");
        dummyCategoryProxyInfo.setName("Name");
        dummyCategoryProxyInfo.setOrigin("Origin");

        dummyAreaOrganizzativaOmogenea = new AreaOrganizzativaOmogenea();
        dummyAreaOrganizzativaOmogenea.setOrigin(Origin.IPA);
        dummyAreaOrganizzativaOmogenea.setDenominazioneAoo("Aoo");
        dummyAreaOrganizzativaOmogenea.setIndirizzo("Address");
        dummyAreaOrganizzativaOmogenea.setCAP("12345");
        dummyAreaOrganizzativaOmogenea.setCodiceFiscaleEnte(dummyInstitutionProxyInfo.getTaxCode());
        dummyAreaOrganizzativaOmogenea.setCodAoo("AOO");
        dummyAreaOrganizzativaOmogenea.setCodiceComuneISTAT("codIstatAOO");

        dummyGeotaxonomies = new GeographicTaxonomies();
        dummyGeotaxonomies.setDescription("nomeCittà - COMUNE");
        dummyGeotaxonomies.setProvinceAbbreviation("proAbbrv");
        dummyGeotaxonomies.setCountryAbbreviation("countryAbbrv");

        dummyInstitutionGeotaxonomies = new InstitutionGeographicTaxonomies();
        dummyInstitutionGeotaxonomies.setCode("code");
        dummyInstitutionGeotaxonomies.setDesc("desc");
    }

    private UnitaOrganizzativa dummyUnitaOrganizzativa() {

        UnitaOrganizzativa dummyUnitaOrganizzativa = new UnitaOrganizzativa();
        dummyUnitaOrganizzativa.setOrigin(Origin.IPA);
        dummyUnitaOrganizzativa.setDescrizioneUo("Uo");
        dummyUnitaOrganizzativa.setIndirizzo("Address");
        dummyUnitaOrganizzativa.setCAP("12345");
        dummyUnitaOrganizzativa.setCodiceFiscaleEnte(dummyInstitutionProxyInfo.getTaxCode());
        dummyUnitaOrganizzativa.setCodiceUniUo("UO");
        dummyUnitaOrganizzativa.setCodiceUniAoo("AOO");
        dummyUnitaOrganizzativa.setCodiceComuneISTAT("codIstatUO");
        return dummyUnitaOrganizzativa;
    }

    @Test
    void shouldThrowExceptionOnCreateInstitutionFromIpaIfAlreadyExists() {

        Institution institution = new Institution();
        institution.setId("id");

        when(institutionConnector.findByTaxCodeAndSubunitCode(any(), any(), eq(null))). thenReturn(List.of(institution));
        when(institutionConnector.save(any())).thenAnswer(args -> args.getArguments()[0]);

        Institution actual = strategyFactory.createInstitutionStrategyIpa()
                .createInstitution(CreateInstitutionStrategyInput.builder()
                        .taxCode(dummyAreaOrganizzativaOmogenea.getCodiceFiscaleEnte())
                        .subunitType(InstitutionPaSubunitType.AOO)
                        .subunitCode(dummyAreaOrganizzativaOmogenea.getCodAoo())
                        .geographicTaxonomies(List.of(dummyInstitutionGeotaxonomies))
                        .supportEmail(SUPPORT_EMAIL)
                        .supportPhone(SUPPORT_PHONE)
                        .build());

        assertThat(actual.getSupportEmail()).isEqualTo(SUPPORT_EMAIL);
        assertThat(actual.getSupportPhone()).isEqualTo(SUPPORT_PHONE);
        verifyNoInteractions(partyRegistryProxyConnector);

    }

    @Test
    void shouldCreateInstitutionFromIvassIfAlreadyExists() {
        String origin = "IVASS";
        String originId = "12345";

        Institution institution = new Institution();
        institution.setId("id");
        institution.setOrigin(origin);
        institution.setOriginId(originId);
        institution.setIstatCode("42");

        when(institutionConnector.findByOriginAndOriginId(any(), any(), eq(null))). thenReturn(List.of(institution));
        when(institutionConnector.save(any())).thenAnswer(args -> args.getArguments()[0]);


        Institution actual = strategyFactory.createInstitutionStrategyIvass(new Institution())
                .createInstitution(CreateInstitutionStrategyInput.builder()
                        .ivassCode(originId)
                        .istatCode("42")
                        .supportEmail(SUPPORT_EMAIL)
                        .supportPhone(SUPPORT_PHONE)
                        .build());

        assertThat(actual.getSupportEmail()).isEqualTo(SUPPORT_EMAIL);
        assertThat(actual.getSupportPhone()).isEqualTo(SUPPORT_PHONE);
        assertThat(actual.getIstatCode()).isEqualTo(institution.getIstatCode());
        verifyNoInteractions(partyRegistryProxyConnector);
    }

    @Test
    void shouldThrowExceptionOnCreateInstitutionIfAlreadyExists() {

        Institution institution = new Institution();
        institution.setId("id");

        when(institutionConnector.findByTaxCodeAndSubunitCode(any(), any(), eq(null))). thenReturn(List.of(institution));
        when(institutionConnector.save(any())).thenAnswer(args -> args.getArguments()[0]);

        when(institutionConnector.findByTaxCodeAndSubunitCode(any(), any(), eq(null)))
                .thenReturn(List.of(new Institution()));

        Institution actual = strategyFactory.createInstitutionStrategy(new Institution())
                .createInstitution(CreateInstitutionStrategyInput.builder()
                        .supportEmail(SUPPORT_EMAIL)
                        .supportPhone(SUPPORT_PHONE)
                        .build());

        assertThat(actual.getSupportEmail()).isEqualTo(SUPPORT_EMAIL);
        assertThat(actual.getSupportPhone()).isEqualTo(SUPPORT_PHONE);
        verifyNoInteractions(partyRegistryProxyConnector);
    }

    /**
     * Method under test: {@link CreateInstitutionStrategy#createInstitution(CreateInstitutionStrategyInput)}
     */
    @Test
    void shouldCreateInstitutionFromAnacIfAlreadyExists() {

        Institution institution = new Institution();
        institution.setId("id");
        institution.setIstatCode("42");

        when(institutionConnector.findByTaxCodeAndSubunitCode(any(), any(), eq(null))). thenReturn(List.of(institution));
        when(institutionConnector.save(any())).thenAnswer(args -> args.getArguments()[0]);

        //When
        Institution actual = strategyFactory.createInstitutionStrategyAnac(institution)
                .createInstitution(CreateInstitutionStrategyInput.builder()
                        .taxCode(institution.getTaxCode())
                        .istatCode("42")
                        .supportPhone(SUPPORT_PHONE)
                        .supportEmail(SUPPORT_EMAIL)
                        .build());

        //Then
        assertThat(actual.getTaxCode()).isEqualTo(institution.getTaxCode());
        assertThat(actual.getSubunitCode()).isNull();
        assertThat(actual.getIstatCode()).isEqualTo(institution.getIstatCode());
        assertThat(actual.getSupportPhone()).isEqualTo(SUPPORT_PHONE);
        assertThat(actual.getSupportEmail()).isEqualTo(SUPPORT_EMAIL);

        verify(institutionConnector).save(any());
    }

    /**
     * Method under test: {@link CreateInstitutionStrategy#createInstitution(CreateInstitutionStrategyInput)}
     */
    @Test
    void shouldCreateInstitutionFromAnac() {

        Institution institution = TestUtils.dummyInstitutionSa();
        SaResource saResource = new SaResource();
        saResource.setOriginId("originId");
        saResource.setTaxCode("taxCode");
        saResource.setDescription("desc");
        saResource.setDigitalAddress("test@test.it");
        when(partyRegistryProxyConnector.getSAFromAnac(any())).thenReturn(saResource);
        when(institutionConnector.save(any())).thenReturn(institution);
        //When
        Institution actual = strategyFactory.createInstitutionStrategyAnac(institution)
                .createInstitution(CreateInstitutionStrategyInput.builder()
                        .taxCode(institution.getTaxCode())
                        .istatCode("istatCode")
                        .supportPhone(SUPPORT_PHONE)
                        .supportEmail(SUPPORT_EMAIL)
                        .build());

        //Then
        assertThat(actual.getDescription()).isEqualTo(institution.getDescription());
        assertThat(actual.getDigitalAddress()).isEqualTo(institution.getDigitalAddress());
        assertThat(actual.getAddress()).isEqualTo(institution.getAddress());
        assertThat(actual.getZipCode()).isEqualTo(institution.getZipCode());
        assertThat(actual.getTaxCode()).isEqualTo(institution.getTaxCode());
        assertThat(actual.getSubunitCode()).isNull();
        assertThat(actual.getSubunitType()).isNull();
        assertThat(actual.getSubunitType()).isNull();
        assertThat(actual.getSupportPhone()).isEqualTo(SUPPORT_PHONE);
        assertThat(actual.getSupportEmail()).isEqualTo(SUPPORT_EMAIL);
        assertThat(actual.getIstatCode()).isEqualTo(institution.getIstatCode());

        verify(institutionConnector).save(any());
    }

    /**
     * Method under test: {@link CreateInstitutionStrategy#createInstitution(CreateInstitutionStrategyInput)}
     */
    @Test
    void shouldCreateInstitutionFromIvass() {

        Institution institution = TestUtils.dummyInstitutionAs();
        ASResource asResource = new ASResource();
        asResource.setOriginId("originId");
        asResource.setTaxCode("taxCode");
        asResource.setDescription("desc");
        asResource.setDigitalAddress("test@test.it");
        when(partyRegistryProxyConnector.getASFromIvass(any())).thenReturn(asResource);
        when(institutionConnector.save(any())).thenReturn(institution);
        //When
        Institution actual = strategyFactory.createInstitutionStrategyIvass(institution)
                .createInstitution(CreateInstitutionStrategyInput.builder()
                        .taxCode(institution.getTaxCode())
                        .istatCode("istatCode")
                        .build());

        //Then
        assertThat(actual.getDescription()).isEqualTo(institution.getDescription());
        assertThat(actual.getDigitalAddress()).isEqualTo(institution.getDigitalAddress());
        assertThat(actual.getAddress()).isEqualTo(institution.getAddress());
        assertThat(actual.getZipCode()).isEqualTo(institution.getZipCode());
        assertThat(actual.getTaxCode()).isEqualTo(institution.getTaxCode());
        assertThat(actual.getSubunitCode()).isNull();
        assertThat(actual.getSubunitType()).isNull();
        assertThat(actual.getSubunitType()).isNull();
        assertThat(actual.getIstatCode()).isEqualTo(institution.getIstatCode());

        verify(institutionConnector).save(any());
    }

    /**
     * Method under test: {@link CreateInstitutionStrategy#createInstitution(CreateInstitutionStrategyInput)}
     */
    @Test
    void shouldCreateInstitution() {
        //Given
        when(institutionConnector.save(any())).thenAnswer(args -> args.getArguments()[0]);
        when(institutionConnector.findByTaxCodeAndSubunitCode(anyString(), any(), eq(null)))
                .thenReturn(List.of());

        Institution institution = TestUtils.dummyInstitutionGsp();
        institution.setLegalForm("legalForm");

        //When
        Institution actual = strategyFactory.createInstitutionStrategy(institution)
                .createInstitution(CreateInstitutionStrategyInput.builder()
                        .taxCode(institution.getTaxCode())
                        .istatCode("istatCode")
                        .build());

        //Then
        assertThat(actual.getDescription()).isEqualTo(institution.getDescription());
        assertThat(actual.getDigitalAddress()).isEqualTo(institution.getDigitalAddress());
        assertThat(actual.getAddress()).isEqualTo(institution.getAddress());
        assertThat(actual.getZipCode()).isEqualTo(institution.getZipCode());
        assertThat(actual.getTaxCode()).isEqualTo(institution.getTaxCode());
        assertThat(actual.getSubunitCode()).isNull();
        assertThat(actual.getSubunitType()).isNull();
        assertThat(actual.getSubunitType()).isNull();
        assertThat(actual.getIstatCode()).isEqualTo(institution.getIstatCode());
        assertThat(actual.getLegalForm()).isEqualTo(institution.getLegalForm());

        verify(institutionConnector).save(any());
        verify(institutionConnector).findByTaxCodeAndSubunitCode(anyString(), any(), eq(null));
    }

    /**
     * Method under test: {@link CreateInstitutionStrategy#createInstitution(CreateInstitutionStrategyInput)}
     */
    @Test
    void shouldCreateInstitutionFromIpaAoo() {
        //Given
        when(institutionConnector.save(any())).thenAnswer(args -> args.getArguments()[0]);
        when(institutionConnector.findByTaxCodeAndSubunitCode(anyString(), anyString(), eq(null)))
                .thenReturn(List.of());

        when(partyRegistryProxyConnector.getCategory(any(), any())).thenReturn(dummyCategoryProxyInfo);
        when(partyRegistryProxyConnector.getInstitutionById(any())).thenReturn(dummyInstitutionProxyInfo);
        when(partyRegistryProxyConnector.getAooById(any())).thenReturn(dummyAreaOrganizzativaOmogenea);
        when(partyRegistryProxyConnector.getExtByCode(anyString())).thenReturn(dummyGeotaxonomies).thenReturn(dummyGeotaxonomies);
        //When
        Institution actual = strategyFactory.createInstitutionStrategyIpa()
                .createInstitution(CreateInstitutionStrategyInput.builder()
                        .taxCode(dummyAreaOrganizzativaOmogenea.getCodiceFiscaleEnte())
                        .subunitType(InstitutionPaSubunitType.AOO)
                        .subunitCode(dummyAreaOrganizzativaOmogenea.getCodAoo())
                        .geographicTaxonomies(List.of(dummyInstitutionGeotaxonomies))
                        .supportEmail(SUPPORT_EMAIL)
                        .supportPhone(SUPPORT_PHONE)
                        .build());

        //Then
        assertThat(actual.getOriginId()).isEqualTo(dummyAreaOrganizzativaOmogenea.getId());
        assertThat(actual.getDescription()).isEqualTo(dummyAreaOrganizzativaOmogenea.getDenominazioneAoo());
        assertThat(actual.getDigitalAddress()).isEqualTo(dummyInstitutionProxyInfo.getDigitalAddress());
        assertThat(actual.getAddress()).isEqualTo(dummyAreaOrganizzativaOmogenea.getIndirizzo());
        assertThat(actual.getZipCode()).isEqualTo(dummyAreaOrganizzativaOmogenea.getCAP());
        assertThat(actual.getTaxCode()).isEqualTo(dummyAreaOrganizzativaOmogenea.getCodiceFiscaleEnte());
        assertThat(actual.getSubunitCode()).isEqualTo(dummyAreaOrganizzativaOmogenea.getCodAoo());
        assertThat(actual.getSubunitType()).isEqualTo(InstitutionPaSubunitType.AOO.name());
        assertThat(actual.getParentDescription()).isEqualTo(dummyInstitutionProxyInfo.getDescription());
        assertThat(actual.getCity()).isEqualTo(dummyGeotaxonomies.getDescription().replace(" - COMUNE", ""));
        assertThat(actual.getGeographicTaxonomies().size()).isEqualTo(1);
        assertThat(actual.getGeographicTaxonomies().get(0).getCode()).isEqualTo(dummyInstitutionGeotaxonomies.getCode());
        assertThat(actual.getSupportEmail()).isEqualTo(SUPPORT_EMAIL);
        assertThat(actual.getSupportPhone()).isEqualTo(SUPPORT_PHONE);

        verify(institutionConnector, times(2)).save(any());
        verify(institutionConnector).findByTaxCodeAndSubunitCode(anyString(), anyString(), eq(null));
        verify(partyRegistryProxyConnector).getCategory(any(), any());
        verify(partyRegistryProxyConnector).getInstitutionById(any());
        verify(partyRegistryProxyConnector, times(1)).getExtByCode(dummyInstitutionProxyInfo.getIstatCode());
        verify(partyRegistryProxyConnector, times(1)).getExtByCode(dummyAreaOrganizzativaOmogenea.getCodiceComuneISTAT());
    }

    /**
     * Method under test: {@link CreateInstitutionStrategy#createInstitution(CreateInstitutionStrategyInput)}
     */
    @Test
    void shouldCreateInstitutionFromIpaUo() {

        UnitaOrganizzativa dummyUnitaOrganizzativa = dummyUnitaOrganizzativa();

        when(institutionConnector.save(any())).thenAnswer(args -> args.getArguments()[0]);
        when(institutionConnector.findByTaxCodeAndSubunitCode(anyString(), anyString(), eq(null)))
                .thenReturn(List.of());

        when(partyRegistryProxyConnector.getCategory(any(), any())).thenReturn(dummyCategoryProxyInfo);
        when(partyRegistryProxyConnector.getInstitutionById(any())).thenReturn(dummyInstitutionProxyInfo);
        when(partyRegistryProxyConnector.getUoById(any())).thenReturn(dummyUnitaOrganizzativa);
        when(partyRegistryProxyConnector.getExtByCode(anyString())).thenReturn(dummyGeotaxonomies).thenReturn(dummyGeotaxonomies);

        Institution actual = strategyFactory.createInstitutionStrategyIpa()
                .createInstitution(CreateInstitutionStrategyInput.builder()
                        .taxCode(dummyUnitaOrganizzativa.getCodiceFiscaleEnte())
                        .subunitType(InstitutionPaSubunitType.UO)
                        .subunitCode(dummyUnitaOrganizzativa.getCodiceUniUo())
                        .build());

        //Then
        assertThat(actual.getOriginId()).isEqualTo(dummyUnitaOrganizzativa.getId());
        assertThat(actual.getDescription()).isEqualTo(dummyUnitaOrganizzativa.getDescrizioneUo());

        assertThat(actual.getDigitalAddress()).isEqualTo(dummyInstitutionProxyInfo.getDigitalAddress());
        assertThat(actual.getAddress()).isEqualTo(dummyUnitaOrganizzativa.getIndirizzo());
        assertThat(actual.getZipCode()).isEqualTo(dummyUnitaOrganizzativa.getCAP());
        assertThat(actual.getTaxCode()).isEqualTo(dummyUnitaOrganizzativa.getCodiceFiscaleEnte());
        assertThat(actual.getSubunitCode()).isEqualTo(dummyUnitaOrganizzativa.getCodiceUniUo());
        assertThat(actual.getSubunitType()).isEqualTo(InstitutionPaSubunitType.UO.name());
        assertThat(actual.getParentDescription()).isEqualTo(dummyInstitutionProxyInfo.getDescription());
        assertThat(actual.getPaAttributes().getAooParentCode()).isEqualTo(dummyUnitaOrganizzativa.getCodiceUniAoo());
        assertThat(actual.getCity()).isEqualTo(dummyGeotaxonomies.getDescription().replace(" - COMUNE", ""));

        verify(institutionConnector, times(2)).save(any());
        verify(institutionConnector).findByTaxCodeAndSubunitCode(anyString(), anyString(), eq(null));
        verify(partyRegistryProxyConnector).getCategory(any(), any());
        verify(partyRegistryProxyConnector).getInstitutionById(any());
        verify(partyRegistryProxyConnector, times(1)).getExtByCode(dummyInstitutionProxyInfo.getIstatCode());
        verify(partyRegistryProxyConnector, times(1)).getExtByCode(dummyUnitaOrganizzativa.getCodiceComuneISTAT());

    }

    @Test
    void createAooFromIpa_nullGeotax() {
        //Given
        when(institutionConnector.save(any())).thenAnswer(args -> args.getArguments()[0]);
        when(institutionConnector.findByTaxCodeAndSubunitCode(anyString(), anyString(), eq(null)))
                .thenReturn(List.of());

        when(partyRegistryProxyConnector.getCategory(any(), any())).thenReturn(dummyCategoryProxyInfo);
        when(partyRegistryProxyConnector.getInstitutionById(any())).thenReturn(dummyInstitutionProxyInfo);
        when(partyRegistryProxyConnector.getAooById(any())).thenReturn(dummyAreaOrganizzativaOmogenea);
        when(partyRegistryProxyConnector.getExtByCode(anyString())).thenReturn(dummyGeotaxonomies).thenReturn(new GeographicTaxonomies());
        //When
        Institution actual = strategyFactory.createInstitutionStrategyIpa()
                .createInstitution(CreateInstitutionStrategyInput.builder()
                        .taxCode(dummyAreaOrganizzativaOmogenea.getCodiceFiscaleEnte())
                        .subunitType(InstitutionPaSubunitType.AOO)
                        .subunitCode(dummyAreaOrganizzativaOmogenea.getCodAoo())
                        .build());

        //Then
        assertThat(actual.getCity()).isEqualTo(null);
        verify(institutionConnector, times(2)).save(any());
        verify(institutionConnector).findByTaxCodeAndSubunitCode(anyString(), anyString(), eq(null));
        verify(partyRegistryProxyConnector).getCategory(any(), any());
        verify(partyRegistryProxyConnector).getInstitutionById(any());
        verify(partyRegistryProxyConnector, times(1)).getExtByCode(dummyInstitutionProxyInfo.getIstatCode());
        verify(partyRegistryProxyConnector, times(1)).getExtByCode(dummyAreaOrganizzativaOmogenea.getCodiceComuneISTAT());

    }

    /**
     * Method under test: {@link CreateInstitutionStrategy#createInstitution(CreateInstitutionStrategyInput)}
     */
    @Test
    void shouldCreateInstitutionFromIpaUoRetrievingExistingEc() {

        UnitaOrganizzativa dummyUnitaOrganizzativa = dummyUnitaOrganizzativa();

        when(institutionConnector.save(any())).thenAnswer(args -> args.getArguments()[0]);
        when(institutionConnector.findByTaxCodeAndSubunitCode(anyString(), anyString(), eq(null)))
                .thenReturn(List.of());

        when(partyRegistryProxyConnector.getCategory(any(), any())).thenReturn(dummyCategoryProxyInfo);
        when(partyRegistryProxyConnector.getInstitutionById(any())).thenReturn(dummyInstitutionProxyInfo);
        when(partyRegistryProxyConnector.getUoById(any())).thenReturn(dummyUnitaOrganizzativa);
        when(institutionConnector.findByExternalId(any())).thenReturn(Optional.of(new Institution()));
        when(partyRegistryProxyConnector.getExtByCode(anyString())).thenReturn(dummyGeotaxonomies);
        Institution actual = strategyFactory.createInstitutionStrategyIpa()
                .createInstitution(CreateInstitutionStrategyInput.builder()
                        .taxCode(dummyUnitaOrganizzativa.getCodiceFiscaleEnte())
                        .subunitType(InstitutionPaSubunitType.UO)
                        .subunitCode(dummyUnitaOrganizzativa.getCodiceUniUo())
                        .build());

        //Then
        assertThat(actual.getOriginId()).isEqualTo(dummyUnitaOrganizzativa.getId());
        assertThat(actual.getDescription()).isEqualTo(dummyUnitaOrganizzativa.getDescrizioneUo());

        assertThat(actual.getDigitalAddress()).isEqualTo(dummyInstitutionProxyInfo.getDigitalAddress());
        assertThat(actual.getAddress()).isEqualTo(dummyUnitaOrganizzativa.getIndirizzo());
        assertThat(actual.getZipCode()).isEqualTo(dummyUnitaOrganizzativa.getCAP());
        assertThat(actual.getTaxCode()).isEqualTo(dummyUnitaOrganizzativa.getCodiceFiscaleEnte());
        assertThat(actual.getSubunitCode()).isEqualTo(dummyUnitaOrganizzativa.getCodiceUniUo());
        assertThat(actual.getSubunitType()).isEqualTo(InstitutionPaSubunitType.UO.name());
        assertThat(actual.getParentDescription()).isEqualTo(dummyInstitutionProxyInfo.getDescription());
        assertThat(actual.getPaAttributes().getAooParentCode()).isEqualTo(dummyUnitaOrganizzativa.getCodiceUniAoo());
        assertThat(actual.getCity()).isEqualTo(dummyGeotaxonomies.getDescription().replace(" - COMUNE", ""));

        verify(institutionConnector, times(1)).save(any());
        verify(institutionConnector).findByTaxCodeAndSubunitCode(anyString(), anyString(), eq(null));
        verify(partyRegistryProxyConnector).getCategory(any(), any());
        verify(partyRegistryProxyConnector).getInstitutionById(any());
    }

    /**
     * Method under test: {@link CreateInstitutionStrategy#createInstitution(CreateInstitutionStrategyInput)}
     */
    @Test
    void shouldCreateInstitutionFromIpaUoWithoutTaxCodeSfe() {

        UnitaOrganizzativa dummyUnitaOrganizzativa = dummyUnitaOrganizzativa();

        when(institutionConnector.save(any())).thenAnswer(args -> args.getArguments()[0]);
        when(institutionConnector.findByTaxCodeAndSubunitCode(anyString(), anyString(), eq(null)))
                .thenReturn(List.of());

        when(partyRegistryProxyConnector.getCategory(any(), any())).thenReturn(dummyCategoryProxyInfo);
        when(partyRegistryProxyConnector.getInstitutionById(any())).thenReturn(dummyInstitutionProxyInfo);
        when(partyRegistryProxyConnector.getUoById(any())).thenReturn(dummyUnitaOrganizzativa);
        when(partyRegistryProxyConnector.getExtByCode(anyString())).thenReturn(dummyGeotaxonomies).thenReturn(dummyGeotaxonomies);

        Institution actual = strategyFactory.createInstitutionStrategyIpa()
                .createInstitution(CreateInstitutionStrategyInput.builder()
                        .taxCode(dummyUnitaOrganizzativa.getCodiceFiscaleEnte())
                        .subunitType(InstitutionPaSubunitType.UO)
                        .subunitCode(dummyUnitaOrganizzativa.getCodiceUniUo())
                        .build());

        //Then
        assertThat(actual.getOriginId()).isEqualTo(dummyUnitaOrganizzativa.getId());
        assertThat(actual.getDescription()).isEqualTo(dummyUnitaOrganizzativa.getDescrizioneUo());

        assertThat(actual.getDigitalAddress()).isEqualTo(dummyInstitutionProxyInfo.getDigitalAddress());
        assertThat(actual.getAddress()).isEqualTo(dummyUnitaOrganizzativa.getIndirizzo());
        assertThat(actual.getZipCode()).isEqualTo(dummyUnitaOrganizzativa.getCAP());
        assertThat(actual.getTaxCode()).isEqualTo(dummyUnitaOrganizzativa.getCodiceFiscaleEnte());
        assertThat(actual.getSubunitCode()).isEqualTo(dummyUnitaOrganizzativa.getCodiceUniUo());
        assertThat(actual.getSubunitType()).isEqualTo(InstitutionPaSubunitType.UO.name());
        assertThat(actual.getParentDescription()).isEqualTo(dummyInstitutionProxyInfo.getDescription());
        assertThat(actual.getPaAttributes().getAooParentCode()).isEqualTo(dummyUnitaOrganizzativa.getCodiceUniAoo());

        verify(institutionConnector, times(2)).save(any());
        verify(institutionConnector).findByTaxCodeAndSubunitCode(anyString(), anyString(), eq(null));
        verify(partyRegistryProxyConnector).getCategory(any(), any());
        verify(partyRegistryProxyConnector).getInstitutionById(any());
    }

    /**
     * Method under test: {@link CreateInstitutionStrategy#createInstitution(CreateInstitutionStrategyInput)}
     */
    @Test
    void shouldGetExistingEC() {

        Institution institutionToReturn = new Institution();
        institutionToReturn.setId("id");
        institutionToReturn.setDescription("test");

        //Given
        when(institutionConnector.findByTaxCodeAndSubunitCode(anyString(), anyString(), eq(null)))
                .thenReturn(List.of());

        when(partyRegistryProxyConnector.getCategory(any(), any())).thenReturn(dummyCategoryProxyInfo);
        when(partyRegistryProxyConnector.getInstitutionById(any())).thenReturn(dummyInstitutionProxyInfo);
        when(institutionConnector.save(any())).thenReturn(institutionToReturn);
        when(partyRegistryProxyConnector.getExtByCode(anyString())).thenReturn(dummyGeotaxonomies);

        //When
        Institution actual = strategyFactory.createInstitutionStrategyIpa()
                .createInstitution(CreateInstitutionStrategyInput.builder()
                        .taxCode(dummyAreaOrganizzativaOmogenea.getCodiceFiscaleEnte())
                        .subunitCode(dummyAreaOrganizzativaOmogenea.getCodAoo())
                        .build());

        assertThat(actual.getId()).isEqualTo(institutionToReturn.getId());
        assertThat(actual.getDescription()).isEqualTo(institutionToReturn.getDescription());

        verify(institutionConnector).findByTaxCodeAndSubunitCode(anyString(), anyString(), eq(null));
        verify(partyRegistryProxyConnector).getCategory(any(), any());
        verify(partyRegistryProxyConnector).getInstitutionById(any());
        verify(partyRegistryProxyConnector, times(1)).getExtByCode(dummyInstitutionProxyInfo.getIstatCode());
    }


    /**
     * Method under test: {@link CreateInstitutionStrategy#createInstitution(CreateInstitutionStrategyInput)}
     */
    @Test
    void shouldCreateInstitutionFromIpaUoWhenCodAooEmptyAndTipoMail() {

        UnitaOrganizzativa dummyUnitaOrganizzativa = dummyUnitaOrganizzativa();
        dummyUnitaOrganizzativa.setCodiceUniAoo(null);
        dummyUnitaOrganizzativa.setTipoMail1("Pec");
        dummyUnitaOrganizzativa.setMail1("example@pec.it");

        when(institutionConnector.save(any())).thenAnswer(args -> args.getArguments()[0]);
        when(institutionConnector.findByTaxCodeAndSubunitCode(anyString(), anyString(), eq(null)))
                .thenReturn(List.of());

        when(partyRegistryProxyConnector.getCategory(any(), any())).thenReturn(dummyCategoryProxyInfo);
        when(partyRegistryProxyConnector.getInstitutionById(any())).thenReturn(dummyInstitutionProxyInfo);
        when(partyRegistryProxyConnector.getUoById(any())).thenReturn(dummyUnitaOrganizzativa);
        when(partyRegistryProxyConnector.getExtByCode(anyString())).thenReturn(dummyGeotaxonomies);
        Institution actual = strategyFactory.createInstitutionStrategyIpa()
                .createInstitution(CreateInstitutionStrategyInput.builder()
                        .taxCode(dummyUnitaOrganizzativa.getCodiceFiscaleEnte())
                        .subunitType(InstitutionPaSubunitType.UO)
                        .subunitCode(dummyUnitaOrganizzativa.getCodiceUniUo())
                        .build());

        //Then
        assertThat(actual.getOriginId()).isEqualTo(dummyUnitaOrganizzativa.getId());
        assertThat(actual.getDescription()).isEqualTo(dummyUnitaOrganizzativa.getDescrizioneUo());

        assertThat(actual.getDigitalAddress()).isEqualTo(dummyUnitaOrganizzativa.getMail1());
        assertThat(actual.getAddress()).isEqualTo(dummyUnitaOrganizzativa.getIndirizzo());
        assertThat(actual.getZipCode()).isEqualTo(dummyUnitaOrganizzativa.getCAP());
        assertThat(actual.getTaxCode()).isEqualTo(dummyUnitaOrganizzativa.getCodiceFiscaleEnte());
        assertThat(actual.getSubunitCode()).isEqualTo(dummyUnitaOrganizzativa.getCodiceUniUo());
        assertThat(actual.getSubunitType()).isEqualTo(InstitutionPaSubunitType.UO.name());
        assertThat(actual.getPaAttributes()).isNull();

        verify(institutionConnector, times(2)).save(any());
        verify(institutionConnector).findByTaxCodeAndSubunitCode(anyString(), anyString(), eq(null));
        verify(partyRegistryProxyConnector).getCategory(any(), any());
        verify(partyRegistryProxyConnector).getInstitutionById(any());
    }

    /**
     * Method under test: {@link CreateInstitutionStrategy#createInstitution(CreateInstitutionStrategyInput)}
     */
    @Test
    void shouldCreateInstitutionWithOriginInfocamereWhenLegalAddressIsFound() {

        Institution institution = TestUtils.dummyInstitutionPg();

        NationalRegistriesProfessionalAddress nationalRegistriesProfessionalAddress = new NationalRegistriesProfessionalAddress();
        nationalRegistriesProfessionalAddress.setAddress("test address");
        nationalRegistriesProfessionalAddress.setZipCode("00000");
        when(partyRegistryProxyConnector.getLegalAddress(any())).thenReturn(nationalRegistriesProfessionalAddress);

        when(institutionConnector.save(any())).thenAnswer(args -> args.getArguments()[0]);
        //When
        Institution actual = strategyFactory.createInstitutionStrategyInfocamere(institution)
                .createInstitution(CreateInstitutionStrategyInput.builder()
                        .taxCode(institution.getTaxCode())
                        .description(institution.getDescription())
                        .build());

        //Then
        assertThat(actual.getDescription()).isEqualTo(institution.getDescription());
        assertThat(actual.getDigitalAddress()).isEqualTo(institution.getDigitalAddress());
        assertThat(actual.getAddress()).isEqualTo(nationalRegistriesProfessionalAddress.getAddress());
        assertThat(actual.getZipCode()).isEqualTo(nationalRegistriesProfessionalAddress.getZipCode());
        assertThat(actual.getTaxCode()).isEqualTo(institution.getTaxCode());
        assertThat(actual.getSubunitCode()).isNull();
        assertThat(actual.getSubunitType()).isNull();
        assertThat(actual.getSubunitType()).isNull();
        assertThat(actual.getOrigin()).isEqualTo(Origin.INFOCAMERE.getValue());

        verify(institutionConnector).save(any());
    }


    /**
     * Method under test: {@link CreateInstitutionStrategy#createInstitution(CreateInstitutionStrategyInput)}
     */
    @Test
    void createInstitutionWithOriginInfocamere_WhenInstitutionExists() {

        Institution institution = TestUtils.dummyInstitutionPg();
        final String description = "UPDATE DESCRIPTION";

        when(institutionConnector.findByTaxCodeAndSubunitCode(institution.getTaxCode(), null, null))
                .thenReturn(List.of(institution));

        when(institutionConnector.save(any())).thenAnswer(args -> args.getArguments()[0]);
        //When
        Institution actual = strategyFactory.createInstitutionStrategyInfocamere(institution)
                .createInstitution(CreateInstitutionStrategyInput.builder()
                        .taxCode(institution.getTaxCode())
                        .description(description)
                        .build());

        //Then
        assertThat(actual.getDescription()).isEqualTo(description);
        assertThat(actual.getDigitalAddress()).isEqualTo(institution.getDigitalAddress());
        assertThat(actual.getAddress()).isEqualTo(institution.getAddress());
        assertThat(actual.getZipCode()).isEqualTo(institution.getZipCode());
        assertThat(actual.getTaxCode()).isEqualTo(institution.getTaxCode());
        assertThat(actual.getSubunitCode()).isNull();
        assertThat(actual.getSubunitType()).isNull();
        assertThat(actual.getSubunitType()).isNull();
        assertThat(actual.getOrigin()).isEqualTo(institution.getOrigin());

        verify(institutionConnector).save(any());
    }

    /**
     * Method under test: {@link CreateInstitutionStrategy#createInstitution(CreateInstitutionStrategyInput)}
     */
    @Test
    void shouldCreateInstitutionWithOriginSelcWhenLegalAddressIsNotFound() {

        Institution institution = TestUtils.dummyInstitutionPg();

        when(partyRegistryProxyConnector.getLegalAddress(any())).thenThrow(ResourceNotFoundException.class);

        when(institutionConnector.save(any())).thenReturn(institution);
        //When
        Institution actual = strategyFactory.createInstitutionStrategyInfocamere(institution)
                .createInstitution(CreateInstitutionStrategyInput.builder()
                        .taxCode(institution.getTaxCode())
                        .description(institution.getDescription())
                        .build());

        //Then
        assertThat(actual.getTaxCode()).isEqualTo(institution.getTaxCode());
        assertThat(actual.getSubunitCode()).isNull();
        assertThat(actual.getSubunitType()).isNull();
        assertThat(actual.getSubunitType()).isNull();
        assertThat(actual.getOriginId()).isEqualTo(institution.getTaxCode());

        verify(institutionConnector).save(any());
    }

}
