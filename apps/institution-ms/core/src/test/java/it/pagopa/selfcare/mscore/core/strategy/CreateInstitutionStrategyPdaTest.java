package it.pagopa.selfcare.mscore.core.strategy;

import it.pagopa.selfcare.mscore.api.InstitutionConnector;
import it.pagopa.selfcare.mscore.api.PartyRegistryProxyConnector;
import it.pagopa.selfcare.mscore.constant.Origin;
import it.pagopa.selfcare.mscore.core.mapper.InstitutionMapper;
import it.pagopa.selfcare.mscore.core.mapper.InstitutionMapperImpl;
import it.pagopa.selfcare.mscore.core.strategy.factory.CreateInstitutionStrategyFactory;
import it.pagopa.selfcare.mscore.core.strategy.input.CreateInstitutionStrategyInput;
import it.pagopa.selfcare.mscore.exception.MsCoreException;
import it.pagopa.selfcare.mscore.exception.ResourceConflictException;
import it.pagopa.selfcare.mscore.exception.ResourceNotFoundException;
import it.pagopa.selfcare.mscore.model.AreaOrganizzativaOmogenea;
import it.pagopa.selfcare.mscore.model.institution.CategoryProxyInfo;
import it.pagopa.selfcare.mscore.model.institution.Institution;
import it.pagopa.selfcare.mscore.model.institution.InstitutionProxyInfo;
import it.pagopa.selfcare.mscore.model.institution.NationalRegistriesProfessionalAddress;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CreateInstitutionStrategyPdaTest {
    @InjectMocks
    CreateInstitutionStrategyFactory strategyFactory;
    @Mock
    InstitutionConnector institutionConnector;
    @Mock
    PartyRegistryProxyConnector partyRegistryProxyConnector;
    @Spy
    InstitutionMapper institutionMapper = new InstitutionMapperImpl();

    private static final InstitutionProxyInfo dummyInstitutionProxyInfo;
    private static final CategoryProxyInfo dummyCategoryProxyInfo;
    private static final AreaOrganizzativaOmogenea dummyAreaOrganizzativaOmogenea;

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
    }

    /**
     * Method under test: {@link CreateInstitutionStrategy#createInstitution(CreateInstitutionStrategyInput)}
     */
    @Test
    void shouldThrowConflictCreateInstitutionForECFromIpaWithPda() {

        Institution institutionToReturn = new Institution();
        institutionToReturn.setId("id");
        institutionToReturn.setDescription("test");

        //Given
        when(institutionConnector.findByTaxCodeAndSubunitCode(any(), any(), eq(null)))
                .thenReturn(List.of(new Institution()));


        //When
        assertThrows(ResourceConflictException.class, () -> strategyFactory.createInstitutionStrategyPda("EC")
                .createInstitution(CreateInstitutionStrategyInput.builder()
                        .taxCode("test")
                        .build()));

        verify(institutionConnector).findByTaxCodeAndSubunitCode(any(), any(), eq(null));
    }

    /**
     * Method under test: {@link CreateInstitutionStrategy#createInstitution(CreateInstitutionStrategyInput)}
     */
    @Test
    void shouldCreateInstitutionForECFromIpaWithPda() {

        Institution institutionToReturn = new Institution();
        institutionToReturn.setId("id");
        institutionToReturn.setDescription("test");
        institutionToReturn.setIstatCode("42");

        //Given
        when(institutionConnector.findByTaxCodeAndSubunitCode(any(), any(), eq(null)))
                .thenReturn(List.of());

        when(partyRegistryProxyConnector.getCategory(any(), any())).thenReturn(dummyCategoryProxyInfo);
        when(partyRegistryProxyConnector.getInstitutionById(any())).thenReturn(dummyInstitutionProxyInfo);
        when(institutionConnector.save(any())).thenReturn(institutionToReturn);

        //When
        Institution actual = strategyFactory.createInstitutionStrategyPda("EC")
                .createInstitution(CreateInstitutionStrategyInput.builder()
                        .taxCode("test")
                        .build());

        assertThat(actual.getId()).isEqualTo(institutionToReturn.getId());
        assertThat(actual.getDescription()).isEqualTo(institutionToReturn.getDescription());
        assertThat(actual.getIstatCode()).isEqualTo(institutionToReturn.getIstatCode());

        verify(institutionConnector).findByTaxCodeAndSubunitCode(any(), any(), eq(null));
        verify(partyRegistryProxyConnector).getCategory(any(), any());
        verify(partyRegistryProxyConnector).getInstitutionById(any());
    }

    /**
     * Method under test: {@link CreateInstitutionStrategy#createInstitution(CreateInstitutionStrategyInput)}
     */
    @Test
    void shouldCreateInstitutionForECFromInfocamereWithPda() {

        Institution institutionToReturn = new Institution();
        institutionToReturn.setId("id");
        institutionToReturn.setDescription("test");
        institutionToReturn.setIstatCode("42");

        NationalRegistriesProfessionalAddress nationalRegistriesProfessionalAddress = new NationalRegistriesProfessionalAddress();
        nationalRegistriesProfessionalAddress.setZipCode("test");
        nationalRegistriesProfessionalAddress.setAddress("test");

        //Given
        when(institutionConnector.findByTaxCodeAndSubunitCode(any(), any(), eq(null)))
                .thenReturn(List.of());

        when(partyRegistryProxyConnector.getInstitutionById(any())).thenThrow(new MsCoreException("NOT_FOUND", "404"));
        when(partyRegistryProxyConnector.getLegalAddress(any())).thenReturn(nationalRegistriesProfessionalAddress);
        when(institutionConnector.save(any())).thenReturn(institutionToReturn);

        //When
        Institution actual = strategyFactory.createInstitutionStrategyPda("EC")
                .createInstitution(CreateInstitutionStrategyInput.builder()
                        .taxCode("test")
                        .istatCode("42")
                        .build());

        assertThat(actual.getId()).isEqualTo(institutionToReturn.getId());
        assertThat(actual.getDescription()).isEqualTo(institutionToReturn.getDescription());
        assertThat(actual.getIstatCode()).isEqualTo(institutionToReturn.getIstatCode());

        verify(institutionConnector).findByTaxCodeAndSubunitCode(any(), any(), eq(null));
        verify(partyRegistryProxyConnector).getInstitutionById(any());
        verify(partyRegistryProxyConnector).getLegalAddress(any());
    }

    /**
     * Method under test: {@link CreateInstitutionStrategy#createInstitution(CreateInstitutionStrategyInput)}
     */
    @Test
    void shouldCreateInstitutionForECFromInfocamereWithPda2() {

        Institution institutionToReturn = new Institution();
        institutionToReturn.setId("id");
        institutionToReturn.setDescription("test");
        institutionToReturn.setIstatCode("42");

        NationalRegistriesProfessionalAddress nationalRegistriesProfessionalAddress = new NationalRegistriesProfessionalAddress();
        nationalRegistriesProfessionalAddress.setZipCode("test");
        nationalRegistriesProfessionalAddress.setAddress("test");

        //Given
        when(institutionConnector.findByTaxCodeAndSubunitCode(any(), any(), eq(null)))
                .thenReturn(List.of());

        when(partyRegistryProxyConnector.getInstitutionById(any())).thenThrow(new ResourceNotFoundException("NOT_FOUND", "404"));
        when(partyRegistryProxyConnector.getLegalAddress(any())).thenReturn(nationalRegistriesProfessionalAddress);
        when(institutionConnector.save(any())).thenReturn(institutionToReturn);

        //When
        Institution actual = strategyFactory.createInstitutionStrategyPda("EC")
                .createInstitution(CreateInstitutionStrategyInput.builder()
                        .taxCode("test")
                        .istatCode("42")
                        .build());

        assertThat(actual.getId()).isEqualTo(institutionToReturn.getId());
        assertThat(actual.getDescription()).isEqualTo(institutionToReturn.getDescription());
        assertThat(actual.getIstatCode()).isEqualTo(institutionToReturn.getIstatCode());

        verify(institutionConnector).findByTaxCodeAndSubunitCode(any(), any(), eq(null));
        verify(partyRegistryProxyConnector).getInstitutionById(any());
        verify(partyRegistryProxyConnector).getLegalAddress(any());
    }

    /**
     * Method under test: {@link CreateInstitutionStrategy#createInstitution(CreateInstitutionStrategyInput)}
     */
    @Test
    void shouldThrowErrorWhenInstitutionDoesNotExistsOnIpaAndInfocamere() {

        Institution institutionToReturn = new Institution();
        institutionToReturn.setId("id");
        institutionToReturn.setDescription("test");

        NationalRegistriesProfessionalAddress nationalRegistriesProfessionalAddress = new NationalRegistriesProfessionalAddress();
        nationalRegistriesProfessionalAddress.setZipCode("test");
        nationalRegistriesProfessionalAddress.setAddress("test");

        //Given
        when(institutionConnector.findByTaxCodeAndSubunitCode(any(), any(), eq(null)))
                .thenReturn(List.of());

        when(partyRegistryProxyConnector.getInstitutionById(any())).thenThrow(new MsCoreException("NOT_FOUND", "404"));
        when(partyRegistryProxyConnector.getLegalAddress(any())).thenThrow(new MsCoreException("NOT_FOUND", "404"));

        //When
        assertThrows(ResourceNotFoundException.class, () -> strategyFactory.createInstitutionStrategyPda("EC")
                .createInstitution(CreateInstitutionStrategyInput.builder()
                        .taxCode("test")
                        .build()));
        verify(institutionConnector).findByTaxCodeAndSubunitCode(any(), any(), eq(null));
        verify(partyRegistryProxyConnector).getInstitutionById(any());
        verify(partyRegistryProxyConnector).getLegalAddress(any());
    }
}
