package it.pagopa.selfcare.onboarding.connector;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import it.pagopa.selfcare.onboarding.connector.model.institutions.Institution;
import it.pagopa.selfcare.onboarding.connector.model.onboarding.CreateInstitutionData;
import it.pagopa.selfcare.onboarding.connector.rest.client.MsCoreRestClient;
import it.pagopa.selfcare.onboarding.connector.rest.model.OnboardingInstitutionRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.TimeZone;

import static it.pagopa.selfcare.commons.utils.TestUtils.mockInstance;
import static it.pagopa.selfcare.commons.utils.TestUtils.reflectionEqualsByName;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MsCoreConnectorImplTest {

    protected static final String REQUIRED_INSTITUTION_ID_MESSAGE = "An Institution external id is required";

    protected static final String REQUIRED_PRODUCT_ID_MESSAGE = "A product Id is required";

    @InjectMocks
    private MsCoreConnectorImpl msCoreConnector;

    @Mock
    private MsCoreRestClient restClientMock;

    @Captor
    ArgumentCaptor<OnboardingInstitutionRequest> onboardingRequestCaptor;

    private final ObjectMapper mapper;

    public MsCoreConnectorImplTest() {
        mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.registerModule(new Jdk8Module());
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE);
        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.setTimeZone(TimeZone.getDefault());
    }

    @Test
    void getInstitution() {
        //given
        String institutionId = "institutionId";
        Institution institutionMock = mockInstance(new Institution());
        when(restClientMock.getInstitutionByExternalId(anyString()))
                .thenReturn(institutionMock);
        //when
        Institution institution = msCoreConnector.getInstitutionByExternalId(institutionId);
        //then
        assertNotNull(institution);
        assertEquals(institutionMock.getExternalId(), institution.getExternalId());
        assertEquals(institutionMock.getDescription(), institution.getDescription());
        assertEquals(institutionMock.getAddress(), institution.getAddress());
        assertEquals(institutionMock.getTaxCode(), institution.getTaxCode());
        assertEquals(institutionMock.getId(), institution.getId());
        assertEquals(institutionMock.getZipCode(), institution.getZipCode());
        assertEquals(institutionMock.getDigitalAddress(), institution.getDigitalAddress());
        assertEquals(institutionMock.getInstitutionType(), institution.getInstitutionType());
        verify(restClientMock, times(1))
                .getInstitutionByExternalId(institutionId);
        verifyNoMoreInteractions(restClientMock);
    }

    @Test
    void getInstitution_nullInstitutionId() {
        //given
        String institutionId = null;
        //when
        Executable exe = () -> msCoreConnector.getInstitutionByExternalId(institutionId);
        //then
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, exe);
        assertEquals(REQUIRED_INSTITUTION_ID_MESSAGE, e.getMessage());
        Mockito.verifyNoInteractions(restClientMock);
    }

    @Test
    void createPGInstitutionUsingExternalId_nullId() {
        //given
        String externalId = null;
        CreateInstitutionData createPnPGInstitutionData = mockInstance(new CreateInstitutionData(), "setTaxId");
        createPnPGInstitutionData.setTaxId(externalId);
        //when
        Executable executable = () -> msCoreConnector.createInstitutionUsingInstitutionData(createPnPGInstitutionData);
        //then
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, executable);
        assertEquals(REQUIRED_INSTITUTION_ID_MESSAGE, e.getMessage());
        verifyNoInteractions(restClientMock);
    }

    @Test
    void creatPGInstitutionUsingExternalId() {
        //given
        CreateInstitutionData createPnPGInstitutionData = mockInstance(new CreateInstitutionData());
        Institution institutionMock = mockInstance(new Institution());
        when(restClientMock.createInstitutionUsingInstitutionData(any()))
                .thenReturn(institutionMock);
        //when
        Institution institution = msCoreConnector.createInstitutionUsingInstitutionData(createPnPGInstitutionData);
        //then
        assertNotNull(institution);
        reflectionEqualsByName(institutionMock, institution);
        assertEquals(institutionMock.getExternalId(), institution.getExternalId());
        assertEquals(institutionMock.getDescription(), institution.getDescription());
        assertEquals(institutionMock.getAddress(), institution.getAddress());
        assertEquals(institutionMock.getTaxCode(), institution.getTaxCode());
        assertEquals(institutionMock.getId(), institution.getId());
        assertEquals(institutionMock.getZipCode(), institution.getZipCode());
        assertEquals(institutionMock.getDigitalAddress(), institution.getDigitalAddress());
        assertEquals(institutionMock.getInstitutionType(), institution.getInstitutionType());
        verify(restClientMock, times(1))
                .createInstitutionUsingInstitutionData(createPnPGInstitutionData);
    }


    @Test
    void verifyOnboarding_nullExternalInstitutionId() {
        // given
        final String externalInstitutionId = null;
        final String productId = "productId";
        // when
        final Executable executable = () -> msCoreConnector.verifyOnboarding(externalInstitutionId, productId);
        // then
        final Exception e = assertThrows(IllegalArgumentException.class, executable);
        assertEquals(REQUIRED_INSTITUTION_ID_MESSAGE, e.getMessage());
        verifyNoInteractions(restClientMock);
    }


    @Test
    void verifyOnboarding_nullProductId() {
        // given
        final String externalInstitutionId = "externalInstitutionId";
        final String productId = null;
        // when
        final Executable executable = () -> msCoreConnector.verifyOnboarding(externalInstitutionId, productId);
        // then
        final Exception e = assertThrows(IllegalArgumentException.class, executable);
        assertEquals(REQUIRED_PRODUCT_ID_MESSAGE, e.getMessage());
        verifyNoInteractions(restClientMock);
    }


    @Test
    void verifyOnboarding() {
        // given
        final String externalInstitutionId = "externalInstitutionId";
        final String productId = "productId";
        // when
        final Executable executable = () -> msCoreConnector.verifyOnboarding(externalInstitutionId, productId);
        // then
        assertDoesNotThrow(executable);
        verify(restClientMock, times(1))
                .verifyOnboarding(externalInstitutionId, productId);
        verifyNoMoreInteractions(restClientMock);
    }

}