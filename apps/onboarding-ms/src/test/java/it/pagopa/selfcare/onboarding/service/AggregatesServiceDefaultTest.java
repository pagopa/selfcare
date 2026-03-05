package it.pagopa.selfcare.onboarding.service;

import static org.mockito.Mockito.*;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import it.pagopa.selfcare.azurestorage.AzureBlobClient;
import it.pagopa.selfcare.onboarding.entity.Token;
import it.pagopa.selfcare.onboarding.mapper.OnboardingMapper;
import it.pagopa.selfcare.onboarding.model.Aggregate;
import it.pagopa.selfcare.onboarding.model.AggregateUser;
import it.pagopa.selfcare.onboarding.model.RowError;
import it.pagopa.selfcare.onboarding.model.VerifyAggregateResponse;
import it.pagopa.selfcare.onboarding.service.impl.AggregatesServiceDefault;
import it.pagopa.selfcare.onboarding.service.profile.OnboardingTestProfile;
import it.pagopa.selfcare.onboarding.service.util.CsvUtils;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import java.io.File;
import java.util.List;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.client.api.WebClientApplicationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openapi.quarkus.core_json.model.UserToOnboard;
import org.openapi.quarkus.party_registry_proxy_json.api.AooApi;
import org.openapi.quarkus.party_registry_proxy_json.api.GeographicTaxonomiesApi;
import org.openapi.quarkus.party_registry_proxy_json.api.InstitutionApi;
import org.openapi.quarkus.party_registry_proxy_json.api.UoApi;
import org.openapi.quarkus.party_registry_proxy_json.model.AOOResource;
import org.openapi.quarkus.party_registry_proxy_json.model.GeographicTaxonomyResource;
import org.openapi.quarkus.party_registry_proxy_json.model.InstitutionResource;
import org.openapi.quarkus.party_registry_proxy_json.model.UOResource;

@QuarkusTest
@TestProfile(OnboardingTestProfile.class)
class AggregatesServiceDefaultTest {

    @Inject
    AggregatesServiceDefault aggregatesServiceDefault;

    @Inject
    OnboardingMapper onboardingMapper;

    @RestClient
    @InjectMock
    GeographicTaxonomiesApi geographicTaxonomiesApi;

    @RestClient
    @InjectMock
    AooApi aooApi;

    @RestClient
    @InjectMock
    InstitutionApi institutionApi;

    @Inject
    CsvUtils csvUtils;

    @RestClient
    @InjectMock
    UoApi uoApi;

    @InjectMock
    AzureBlobClient azureBlobClient;

    @Test
    @RunOnVertxContext
    void testValidateAppIoAggregatesCsv() {
        File file = new File("src/test/resources/aggregates-appio.csv");

        UOResource uoResource = new UOResource();
        uoResource.setMail1("pec@Pec");
        uoResource.setTipoMail1("Pec");
        uoResource.setCodiceUniAoo("18SU3S");
        uoResource.setCap("00100");
        uoResource.setDescrizioneUo("denominazione");
        uoResource.setIndirizzo("Palazzo Vecchio Piazza Della Signoria");
        uoResource.setCodiceComuneISTAT("123");
        uoResource.setCodiceFiscaleEnte("13071104841");

        AOOResource aooResource = new AOOResource();
        aooResource.setTipoMail1("Altro");
        aooResource.setMail1("pec@Pec");
        aooResource.setCodiceUniAoo("18SU3S");
        aooResource.setDenominazioneAoo("denominazione");
        aooResource.setCap("00100");
        aooResource.setCodiceFiscaleEnte("13071104841");
        aooResource.setIndirizzo("Palazzo Vecchio Piazza Della Signoria");
        aooResource.setCodiceComuneISTAT("123");
        aooResource.setCodiceFiscaleEnte("13071104841");

        InstitutionResource institutionResource = mock(InstitutionResource.class);
        when(institutionResource.getIstatCode()).thenReturn("123");
        when(institutionResource.getOriginId()).thenReturn("test");
        when(institutionResource.getDigitalAddress()).thenReturn("pec@Pec");
        when(institutionResource.getDescription()).thenReturn("descrizione ente");

        GeographicTaxonomyResource geographicTaxonomyResource = new GeographicTaxonomyResource();
        geographicTaxonomyResource.setCode("123");
        geographicTaxonomyResource.setDesc("città");
        geographicTaxonomyResource.setProvinceAbbreviation("Provincia");

        WebClientApplicationException webClientApplicationException = mock(WebClientApplicationException.class);
        Response response = mock(Response.class);
        when(webClientApplicationException.getResponse()).thenReturn(response);
        when(response.getStatus()).thenReturn(404);
        when(aooApi.findByUnicodeUsingGET("18SU3S", null)).thenReturn(Uni.createFrom().item(aooResource));
        when(aooApi.findByUnicodeUsingGET("18SU3R", null)).thenReturn(Uni.createFrom().failure(webClientApplicationException));
        when(institutionApi.findInstitutionUsingGET("13071104841", null, null)).thenReturn(Uni.createFrom().item(institutionResource));
        when(uoApi.findByUnicodeUsingGET1("18SU3R", null)).thenReturn(Uni.createFrom().item(uoResource));
        when(geographicTaxonomiesApi.retrieveGeoTaxonomiesByCodeUsingGET("123")).thenReturn(Uni.createFrom().item(geographicTaxonomyResource));

        VerifyAggregateResponse verifyAggregateResponse = mockResponseForIO();

        UniAssertSubscriber<VerifyAggregateResponse> resp = aggregatesServiceDefault.validateAppIoAggregatesCsv(file)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertCompleted();

        Assertions.assertEquals(3, resp.getItem().getAggregates().size());
        Assertions.assertEquals(verifyAggregateResponse.getAggregates().get(0), resp.getItem().getAggregates().get(0));
        Assertions.assertEquals(verifyAggregateResponse.getAggregates().get(1), resp.getItem().getAggregates().get(1));
        Assertions.assertEquals(verifyAggregateResponse.getAggregates().get(2), resp.getItem().getAggregates().get(2));
        Assertions.assertEquals(6, resp.getItem().getErrors().size());
        Assertions.assertEquals(verifyAggregateResponse.getErrors(), resp.getItem().getErrors());

        verify(geographicTaxonomiesApi, times(1)).retrieveGeoTaxonomiesByCodeUsingGET("123");
        verify(aooApi, times(1)).findByUnicodeUsingGET("18SU3R", null);
        verify(aooApi, times(1)).findByUnicodeUsingGET("18SU3S", null);
        verify(institutionApi, times(3)).findInstitutionUsingGET("13071104841", null, null);

    }

    @Test
    void testValidatePagoPaAggregatesCsv() {

        File file = new File("src/test/resources/aggregates-pagopa.csv");

        WebClientApplicationException webClientApplicationException = mock(WebClientApplicationException.class);
        Response response = mock(Response.class);
        when(webClientApplicationException.getResponse()).thenReturn(response);
        when(response.getStatus()).thenReturn(404);

        InstitutionResource institutionResource = mock(InstitutionResource.class);
        when(institutionResource.getIstatCode()).thenReturn("789");
        when(institutionResource.getOriginId()).thenReturn("test");
        when(institutionResource.getDigitalAddress()).thenReturn("pec@Pec");

        GeographicTaxonomyResource geographicTaxonomyResource = new GeographicTaxonomyResource();
        geographicTaxonomyResource.setCode("789");
        geographicTaxonomyResource.setDesc("città");
        geographicTaxonomyResource.setProvinceAbbreviation("Provincia");

        VerifyAggregateResponse verifiyAggregateResponse = mockPagoPaResponse();
        when(institutionApi.findInstitutionUsingGET("12345678901", null, null)).thenReturn(Uni.createFrom().item(institutionResource));
        when(institutionApi.findInstitutionUsingGET("12345901", null, null)).thenReturn(Uni.createFrom().failure(webClientApplicationException));
        when(geographicTaxonomiesApi.retrieveGeoTaxonomiesByCodeUsingGET("789")).thenReturn(Uni.createFrom().item(geographicTaxonomyResource));

        UniAssertSubscriber<VerifyAggregateResponse> resp = aggregatesServiceDefault.validatePagoPaAggregatesCsv(file)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertCompleted();

        Assertions.assertEquals(1, resp.getItem().getAggregates().size());
        Assertions.assertEquals(verifiyAggregateResponse.getAggregates().get(0), resp.getItem().getAggregates().get(0));
        Assertions.assertEquals(5, resp.getItem().getErrors().size());
        Assertions.assertEquals(verifiyAggregateResponse.getErrors(), resp.getItem().getErrors());

    }

    @Test
    @RunOnVertxContext
    void validateSendAggregates() {

        File file = new File("src/test/resources/aggregates-send.csv");

        UOResource uoResource = new UOResource();
        uoResource.setMail1("pec@Pec");
        uoResource.setTipoMail1("Pec");
        uoResource.setCodiceUniAoo("18SU3S");
        uoResource.setCap("00100");
        uoResource.setDescrizioneUo("denominazione");
        uoResource.setIndirizzo("Palazzo Vecchio Piazza Della Signoria");
        uoResource.setCodiceComuneISTAT("456");
        uoResource.setCodiceFiscaleEnte("13071104841");

        AOOResource aooResource = new AOOResource();
        aooResource.setTipoMail1("Pec");
        aooResource.setMail1("pec@Pec");
        aooResource.setCodiceUniAoo("18SU3S");
        aooResource.setDenominazioneAoo("denominazione");
        aooResource.setCap("00100");
        aooResource.setIndirizzo("Palazzo Vecchio Piazza Della Signoria");
        aooResource.setCodiceComuneISTAT("456");
        aooResource.setCodiceFiscaleEnte("13071104841");

        InstitutionResource institutionResource = mock(InstitutionResource.class);
        when(institutionResource.getIstatCode()).thenReturn("456");
        when(institutionResource.getOriginId()).thenReturn("test");
        when(institutionResource.getDescription()).thenReturn("testDenominazioneEnte");

        GeographicTaxonomyResource geographicTaxonomyResource = new GeographicTaxonomyResource();
        geographicTaxonomyResource.setCode("456");
        geographicTaxonomyResource.setDesc("città");
        geographicTaxonomyResource.setProvinceAbbreviation("Provincia");

        WebClientApplicationException webClientApplicationException = mock(WebClientApplicationException.class);
        Response response = mock(Response.class);
        when(webClientApplicationException.getResponse()).thenReturn(response);
        when(response.getStatus()).thenReturn(404);
        when(aooApi.findByUnicodeUsingGET("18SU3S", null)).thenReturn(Uni.createFrom().item(aooResource));
        when(aooApi.findByUnicodeUsingGET("18SU3R", null)).thenReturn(Uni.createFrom().failure(webClientApplicationException));
        when(institutionApi.findInstitutionUsingGET("13071104841", null, null)).thenReturn(Uni.createFrom().item(institutionResource));
        when(uoApi.findByUnicodeUsingGET1("18SU3R", null)).thenReturn(Uni.createFrom().item(uoResource));
        when(geographicTaxonomiesApi.retrieveGeoTaxonomiesByCodeUsingGET("456")).thenReturn(Uni.createFrom().item(geographicTaxonomyResource));

        VerifyAggregateResponse verifyAggregateResponse = mockResponseForSEND();

        UniAssertSubscriber<VerifyAggregateResponse> resp = aggregatesServiceDefault.validateSendAggregatesCsv(file)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertCompleted();

        Assertions.assertEquals(3, resp.getItem().getAggregates().size());
        Assertions.assertEquals(verifyAggregateResponse.getAggregates(), resp.getItem().getAggregates());
        Assertions.assertEquals(verifyAggregateResponse.getAggregates().get(0), resp.getItem().getAggregates().get(0));
        Assertions.assertEquals(verifyAggregateResponse.getAggregates().get(1), resp.getItem().getAggregates().get(1));
        Assertions.assertEquals(verifyAggregateResponse.getAggregates().get(2), resp.getItem().getAggregates().get(2));
        Assertions.assertEquals(13, resp.getItem().getErrors().size());
        Assertions.assertEquals(verifyAggregateResponse.getErrors(), resp.getItem().getErrors());

        verify(geographicTaxonomiesApi, times(1)).retrieveGeoTaxonomiesByCodeUsingGET("456");
        verify(aooApi, times(1)).findByUnicodeUsingGET("18SU3R", null);
        verify(aooApi, times(1)).findByUnicodeUsingGET("18SU3S", null);
        verify(institutionApi, times(3)).findInstitutionUsingGET("13071104841", null, null);
    }

    private static VerifyAggregateResponse mockResponseForIO() {
        VerifyAggregateResponse verifyAggregateResponse = new VerifyAggregateResponse();
        Aggregate aggregateUO = new Aggregate();
        aggregateUO.setSubunitCode("18SU3R");
        aggregateUO.setSubunitType("UO");
        aggregateUO.setDescription("denominazione");
        aggregateUO.setDigitalAddress("pec@Pec");
        aggregateUO.setTaxCode("13071104841");
        aggregateUO.setVatNumber("13071104841");
        aggregateUO.setAddress("Palazzo Vecchio Piazza Della Signoria");
        aggregateUO.setCity("città");
        aggregateUO.setCounty("Provincia");
        aggregateUO.setZipCode("00100");
        aggregateUO.setOrigin("IPA");
        aggregateUO.setRowNumber(1);
        aggregateUO.setParentDescription("descrizione ente");

        Aggregate aggregateAOO = new Aggregate();
        aggregateAOO.setSubunitCode("18SU3S");
        aggregateAOO.setSubunitType("AOO");
        aggregateAOO.setDescription("denominazione");
        aggregateAOO.setDigitalAddress("pec@Pec");
        aggregateAOO.setTaxCode("13071104841");
        aggregateAOO.setVatNumber("13071104841");
        aggregateAOO.setAddress("Palazzo Vecchio Piazza Della Signoria");
        aggregateAOO.setCity("città");
        aggregateAOO.setCounty("Provincia");
        aggregateAOO.setZipCode("00100");
        aggregateAOO.setOrigin("IPA");
        aggregateAOO.setRowNumber(5);
        aggregateAOO.setParentDescription("descrizione ente");

        Aggregate aggregate = new Aggregate();
        aggregate.setSubunitCode(null);
        aggregate.setSubunitType(null);
        aggregate.setDescription("descrizione ente");
        aggregate.setDigitalAddress("pec@Pec");
        aggregate.setTaxCode("13071104841");
        aggregate.setVatNumber("13071104841");
        aggregate.setAddress(null);
        aggregate.setCity("città");
        aggregate.setCounty("Provincia");
        aggregate.setZipCode(null);
        aggregate.setOriginId(null);
        aggregate.setOrigin("IPA");
        aggregate.setOriginId("test");
        aggregate.setRowNumber(6);

        verifyAggregateResponse.setAggregates(List.of(aggregateUO, aggregateAOO, aggregate));

        RowError error0 = new RowError(2, "13071104841", "SubunitType non valido");
        RowError error1 = new RowError(3, "13071104841", "La Partita IVA è obbligatoria");
        RowError error2 = new RowError(7, "13071104841", "In caso di AOO/UO è necessario specificare la tipologia e il codice univoco IPA AOO/UO");
        RowError error3 = new RowError(8, "1307110484", "Il Codice Fiscale non è valido");
        RowError error4 = new RowError(4, "13071104841", "Codice Fiscale non presente su IPA");
        RowError error5 = new RowError(9, "13071104841", "La Partita IVA non è valida");

        verifyAggregateResponse.setErrors(List.of(error0, error1, error4, error2, error3, error5));
        return verifyAggregateResponse;
    }

    private VerifyAggregateResponse mockPagoPaResponse() {
        VerifyAggregateResponse verifyAggregateResponse = new VerifyAggregateResponse();
        Aggregate aggregate = new Aggregate();
        aggregate.setSubunitCode(null);
        aggregate.setSubunitType(null);
        aggregate.setDescription(null);
        aggregate.setDigitalAddress("pec@Pec");
        aggregate.setTaxCode("12345678901");
        aggregate.setVatNumber("12345678901");
        aggregate.setAddress(null);
        aggregate.setCity("città");
        aggregate.setCounty("Provincia");
        aggregate.setZipCode(null);
        aggregate.setOriginId(null);
        aggregate.setOrigin("IPA");
        aggregate.setOriginId("test");
        aggregate.setIban("IT60 X054 2811 1010 0000 0123 456");
        aggregate.setRowNumber(1);

        verifyAggregateResponse.setAggregates(List.of(aggregate));

        RowError error0 = new RowError(2, null, "Il Codice Fiscale è obbligatorio");
        RowError error1 = new RowError(3, "12345678901", "La Partita IVA è obbligatoria");
        RowError error3 = new RowError(4, "12345678901", "IBAN è obbligatorio");
        RowError error6 = new RowError(5, "12345901", "Il Codice Fiscale non è valido");
        RowError error7 = new RowError(6, "12345678901", "La Partita IVA non è valida");

        verifyAggregateResponse.setErrors(List.of(error0, error1, error3, error6, error7));
        return verifyAggregateResponse;
    }

    private static VerifyAggregateResponse mockResponseForSEND() {
        AggregateUser aggregateUserUO = new AggregateUser();
        aggregateUserUO.setName("Mario");
        aggregateUserUO.setSurname("Rossi");
        aggregateUserUO.setTaxCode("RSSMRA66A01H501W");
        aggregateUserUO.setEmail("mario.rossi@acme.it");
        aggregateUserUO.setRole(UserToOnboard.RoleEnum.DELEGATE.name());

        AggregateUser aggregateUser = new AggregateUser();
        aggregateUser.setName("Guendalina");
        aggregateUser.setSurname("Giordano");
        aggregateUser.setTaxCode("GRDGDL66A01H501W");
        aggregateUser.setEmail("guendalina.giordano@test.it");
        aggregateUser.setRole(UserToOnboard.RoleEnum.DELEGATE.name());

        AggregateUser aggregateUserAOO = new AggregateUser();
        aggregateUserAOO.setName("Ma");
        aggregateUserAOO.setSurname("Re");
        aggregateUserAOO.setTaxCode("REXMAX66A01H501W");
        aggregateUserAOO.setEmail("ma.re@test.it");
        aggregateUserAOO.setRole(UserToOnboard.RoleEnum.DELEGATE.name());

        VerifyAggregateResponse verifyAggregateResponse = new VerifyAggregateResponse();
        Aggregate aggregateUO = new Aggregate();
        aggregateUO.setSubunitCode("18SU3R");
        aggregateUO.setSubunitType("UO");
        aggregateUO.setDescription("denominazione");
        aggregateUO.setDigitalAddress("pec@Pec");
        aggregateUO.setTaxCode("13071104841");
        aggregateUO.setVatNumber("18644400191");
        aggregateUO.setAddress("Palazzo Vecchio Piazza Della Signoria");
        aggregateUO.setCity("città");
        aggregateUO.setCounty("Provincia");
        aggregateUO.setZipCode("00100");
        aggregateUO.setOrigin("IPA");
        aggregateUO.setRecipientCode("NYJTPK");
        aggregateUO.setRowNumber(1);
        aggregateUO.setUsers(List.of(aggregateUserUO));
        aggregateUO.setParentDescription("testDenominazioneEnte");

        Aggregate aggregateAOO = new Aggregate();
        aggregateAOO.setSubunitCode("18SU3S");
        aggregateAOO.setSubunitType("AOO");
        aggregateAOO.setDescription("denominazione");
        aggregateAOO.setDigitalAddress("pec@Pec");
        aggregateAOO.setTaxCode("13071104841");
        aggregateAOO.setVatNumber("18644400191");
        aggregateAOO.setAddress("Palazzo Vecchio Piazza Della Signoria");
        aggregateAOO.setCity("città");
        aggregateAOO.setCounty("Provincia");
        aggregateAOO.setZipCode("00100");
        aggregateAOO.setRecipientCode("NYJTPK");
        aggregateAOO.setOrigin("IPA");
        aggregateAOO.setRowNumber(9);
        aggregateAOO.setUsers(List.of(aggregateUserAOO));
        aggregateAOO.setParentDescription("testDenominazioneEnte");

        Aggregate aggregate = new Aggregate();
        aggregate.setSubunitCode(null);
        aggregate.setSubunitType(null);
        aggregate.setDescription("testDenominazioneEnte");
        aggregate.setDigitalAddress(null);
        aggregate.setTaxCode("13071104841");
        aggregate.setVatNumber("18644400191");
        aggregate.setAddress(null);
        aggregate.setCity("città");
        aggregate.setCounty("Provincia");
        aggregate.setZipCode(null);
        aggregate.setOriginId(null);
        aggregate.setRecipientCode("NYJTPK");
        aggregate.setOrigin("IPA");
        aggregate.setOriginId("test");
        aggregate.setRowNumber(11);
        aggregate.setUsers(List.of(aggregateUser));


        verifyAggregateResponse.setAggregates(List.of(aggregateUO, aggregateAOO, aggregate));

        RowError error0 = new RowError(2, "13071104841", "SubunitType non valido");
        RowError error1 = new RowError(3, "13071104841", "Email Amministratore Ente Aggregato è obbligatorio");
        RowError error2 = new RowError(5, "13071104841", "Cognome Amministratore Ente Aggregato è obbligatorio");
        RowError error3 = new RowError(7, "13071104841", "Nome Amministratore Ente Aggregato è obbligatorio");
        RowError error4 = new RowError(4, "13071104841", "Codice Fiscale Amministratore Ente Aggregato è obbligatorio");
        RowError error5 = new RowError(8, "13071104841", "In caso di AOO/UO è necessario specificare la tipologia e il codice univoco IPA AOO/UO");
        RowError error6 = new RowError(12, "13071104841", "Codice SDI è obbligatorio");
        RowError error7 = new RowError(13, "1307110484", "Il Codice Fiscale non è valido");
        RowError error8 = new RowError(14, "13071104841", "La Partita IVA non è valida");
        RowError error9 = new RowError(10, "13071104841", "Codice Fiscale non presente su IPA");
        RowError error10 = new RowError(15, "13071104841", "Cognome non corretto o diverso dal Codice Fiscale");
        RowError error11 = new RowError(16, "13071104841", "Nome non corretto o diverso dal Codice Fiscale");
        RowError error12 = new RowError(6, "13071104841", "La Partita IVA è obbligatoria");

        verifyAggregateResponse.setErrors(List.of(error0, error1, error4, error2, error12, error3, error5, error9, error6, error7, error8, error10, error11));
        return verifyAggregateResponse;
    }

    @Test
    void retrieveContractNotSigned() {
        Token token = new Token();
        token.setContractFilename("fileName");
        final String onboardingId = "onboardingId";
        final String productId = "productId";

        when(azureBlobClient.getFileAsPdf(anyString())).thenReturn(new File("fileName"));

        UniAssertSubscriber<RestResponse<File>> subscriber = aggregatesServiceDefault.retrieveAggregatesCsv(onboardingId, productId)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        RestResponse<File> actual = subscriber.awaitItem().getItem();
        Assertions.assertNotNull(actual);
        Assertions.assertEquals(RestResponse.Status.OK.getStatusCode(), actual.getStatus());
    }
}
