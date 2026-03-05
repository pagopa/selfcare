package it.pagopa.selfcare.onboarding.service.impl;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.azurestorage.AzureBlobClient;
import it.pagopa.selfcare.onboarding.common.InstitutionPaSubunitType;
import it.pagopa.selfcare.onboarding.conf.OnboardingMsConfig;
import it.pagopa.selfcare.onboarding.exception.InvalidRequestException;
import it.pagopa.selfcare.onboarding.exception.ResourceNotFoundException;
import it.pagopa.selfcare.onboarding.mapper.OnboardingMapper;
import it.pagopa.selfcare.onboarding.model.*;
import it.pagopa.selfcare.onboarding.service.AggregatesService;
import it.pagopa.selfcare.onboarding.service.util.CsvUtils;
import it.pagopa.selfcare.onboarding.util.Utils;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import lombok.extern.slf4j.Slf4j;
import net.jodah.expiringmap.ExpiringMap;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.reactive.RestResponse;
import org.openapi.quarkus.party_registry_proxy_json.api.AooApi;
import org.openapi.quarkus.party_registry_proxy_json.api.GeographicTaxonomiesApi;
import org.openapi.quarkus.party_registry_proxy_json.api.InstitutionApi;
import org.openapi.quarkus.party_registry_proxy_json.api.UoApi;
import org.openapi.quarkus.party_registry_proxy_json.model.AOOResource;
import org.openapi.quarkus.party_registry_proxy_json.model.InstitutionResource;
import org.openapi.quarkus.party_registry_proxy_json.model.UOResource;

import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static it.pagopa.selfcare.onboarding.common.InstitutionPaSubunitType.UO;
import static it.pagopa.selfcare.onboarding.constants.CustomError.*;

@ApplicationScoped
@Slf4j
public class AggregatesServiceDefault implements AggregatesService {

    @RestClient
    @Inject
    AooApi aooApi;

    @RestClient
    @Inject
    InstitutionApi institutionApi;

    @RestClient
    @Inject
    UoApi uoApi;

    @RestClient
    @Inject
    GeographicTaxonomiesApi geographicTaxonomiesApi;

    @Inject
    OnboardingMapper onboardingMapper;

    @Inject
    CsvUtils csvUtils;

    private final AzureBlobClient azureBlobClient;
    private final OnboardingMsConfig onboardingMsConfig;
    private final ExpiringMap<String, GeographicTaxonomyFromIstatCode> expiringMap;

    @Inject
    public AggregatesServiceDefault(AzureBlobClient azureBlobClient, OnboardingMsConfig onboardingMsConfig, @ConfigProperty(name = "onboarding-ms.istat-cache-duration-minutes") int cacheDuration) {
        this.azureBlobClient = azureBlobClient;
        this.onboardingMsConfig = onboardingMsConfig;
        this.expiringMap = ExpiringMap.builder()
                .expiration(cacheDuration, TimeUnit.MINUTES)
                .build();
    }

    protected static final String DESCRIPTION_TO_REPLACE_REGEX = " - COMUNE";
    private static final String FILE_NAME_AGGREGATES_CSV = "aggregates.csv";
    private static final String LOG_CSV_ROWS = "CSV file validated end: {} valid row and {} invalid row";
    private static final String PEC = "Pec";


    @Override
    public Uni<VerifyAggregateResponse> validateAppIoAggregatesCsv(File file) {
        AggregatesCsv<CsvAggregateAppIo> aggregatesCsv = csvUtils.readItemsFromCsv(file, CsvAggregateAppIo.class);
        List<CsvAggregateAppIo> csvAggregates = aggregatesCsv.getCsvAggregateList();
        VerifyAggregateResponse verifyAggregateAppIoResponse = new VerifyAggregateResponse();

        return Multi.createFrom().iterable(csvAggregates)
                .onItem().transformToUniAndMerge(csvAggregateAppIo ->
                        checkCsvAggregateAppIoAndFillAggregateOrErrorList(csvAggregateAppIo, verifyAggregateAppIoResponse))
                .collect().asList()
                .replaceWith(verifyAggregateAppIoResponse)
                .onItem().invoke(() -> logVerificationSummary(verifyAggregateAppIoResponse));
    }

    @Override
    public Uni<VerifyAggregateResponse> validatePagoPaAggregatesCsv(File file) {
        AggregatesCsv<CsvAggregatePagoPa> aggregatesCsv = csvUtils.readItemsFromCsv(file, CsvAggregatePagoPa.class);
        List<CsvAggregatePagoPa> csvAggregates = aggregatesCsv.getCsvAggregateList();
        VerifyAggregateResponse verifyAggregatePagoPaResponse = new VerifyAggregateResponse();

        return Multi.createFrom().iterable(csvAggregates)
                .onItem().transformToUniAndMerge(csvAggregatePagoPa -> checkCsvAggregatePagoPaAndFillAggregateOrErrorList(csvAggregatePagoPa, verifyAggregatePagoPaResponse))
                .collect().asList()
                .replaceWith(verifyAggregatePagoPaResponse)
                .onItem().invoke(() -> logVerificationSummary(verifyAggregatePagoPaResponse));
    }

    @Override
    public Uni<VerifyAggregateResponse> validateSendAggregatesCsv(File file) {
        AggregatesCsv<CsvAggregateSend> aggregatesCsv = csvUtils.readItemsFromCsv(file, CsvAggregateSend.class);
        List<CsvAggregateSend> csvAggregates = aggregatesCsv.getCsvAggregateList();
        VerifyAggregateResponse verifyAggregateSendResponse = new VerifyAggregateResponse();

        return Multi.createFrom().iterable(csvAggregates)
                .onItem().transformToUniAndMerge(csvAggregateSend -> checkCsvAggregateSendAndFillAggregateOrErrorList(csvAggregateSend, verifyAggregateSendResponse))
                .collect().asList()
                .replaceWith(verifyAggregateSendResponse)
                .onItem().invoke(() -> logVerificationSummary(verifyAggregateSendResponse));
    }

    @Override
    public Uni<RestResponse<File>> retrieveAggregatesCsv(String onboardingId, String productId) {
        return Uni.createFrom().item(() -> azureBlobClient.getFileAsPdf(String.format("%s%s/%s/%s", onboardingMsConfig.getAggregatesPath(), onboardingId, productId, FILE_NAME_AGGREGATES_CSV)))
                .runSubscriptionOn(Executors.newSingleThreadExecutor())
                .onItem().transform(csv -> {
                    RestResponse.ResponseBuilder<File> response = RestResponse.ResponseBuilder.ok(csv, MediaType.APPLICATION_OCTET_STREAM);
                    response.header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + FILE_NAME_AGGREGATES_CSV);
                    return response.build();
                });
    }

    private Uni<Void> checkCsvAggregateAppIoAndFillAggregateOrErrorList(CsvAggregateAppIo csvAggregateAppIo, VerifyAggregateResponse verifyAggregateAppIoResponse) {

        return checkCsvAggregateAppIo(csvAggregateAppIo)
                .onItem().invoke(aggregateAppIo -> verifyAggregateAppIoResponse.getAggregates().add(aggregateAppIo))
                .onFailure(ResourceNotFoundException.class)
                .recoverWithUni(throwable -> {
                    verifyAggregateAppIoResponse.getErrors().add(mapToErrorRow(csvAggregateAppIo.getRowNumber(), csvAggregateAppIo.getTaxCode(), throwable));
                    return Uni.createFrom().nullItem();
                })
                .onFailure(InvalidRequestException.class)
                .recoverWithUni(throwable -> {
                    verifyAggregateAppIoResponse.getErrors().add(mapToErrorRow(csvAggregateAppIo.getRowNumber(), csvAggregateAppIo.getTaxCode(), throwable));
                    return Uni.createFrom().nullItem();
                })
                .replaceWithVoid();
    }

    private Uni<Void> checkCsvAggregatePagoPaAndFillAggregateOrErrorList(CsvAggregatePagoPa csvAggregatePagoPa, VerifyAggregateResponse verifyAggregatePagoPaResponse) {
        return checkCsvAggregatePagoPa(csvAggregatePagoPa)
                .onItem().invoke(aggregateSend -> verifyAggregatePagoPaResponse.getAggregates().add(aggregateSend))
                .onFailure(ResourceNotFoundException.class)
                .recoverWithUni(throwable -> {
                    verifyAggregatePagoPaResponse.getErrors().add(mapToErrorRow(csvAggregatePagoPa.getRowNumber(), csvAggregatePagoPa.getTaxCode(), throwable));
                    return Uni.createFrom().nullItem();
                })
                .onFailure(InvalidRequestException.class)
                .recoverWithUni(throwable -> {
                    verifyAggregatePagoPaResponse.getErrors().add(mapToErrorRow(csvAggregatePagoPa.getRowNumber(), csvAggregatePagoPa.getTaxCode(), throwable));
                    return Uni.createFrom().nullItem();
                })
                .replaceWithVoid();
    }

    private Uni<Void> checkCsvAggregateSendAndFillAggregateOrErrorList(CsvAggregateSend csvAggregateSend, VerifyAggregateResponse verifyAggregateSendResponse) {
        return checkCsvAggregateSend(csvAggregateSend)
                .onItem().invoke(aggregateSend -> verifyAggregateSendResponse.getAggregates().add(aggregateSend))
                .onFailure(ResourceNotFoundException.class)
                .recoverWithUni(throwable -> {
                    verifyAggregateSendResponse.getErrors().add(mapToErrorRow(csvAggregateSend.getRowNumber(), csvAggregateSend.getTaxCode(), throwable));
                    return Uni.createFrom().nullItem();
                })
                .onFailure(InvalidRequestException.class)
                .recoverWithUni(throwable -> {
                    verifyAggregateSendResponse.getErrors().add(mapToErrorRow(csvAggregateSend.getRowNumber(), csvAggregateSend.getTaxCode(), throwable));
                    return Uni.createFrom().nullItem();
                })
                .replaceWithVoid();
    }

    private static RowError mapToErrorRow(Integer rowNumber, String taxCode, Throwable throwable) {
        return new RowError(rowNumber, taxCode, throwable.getMessage());
    }

    private Uni<Aggregate> checkCsvAggregateAppIo(CsvAggregateAppIo csvAggregateAppIo) {
        return checkRequiredFieldsAppIo(csvAggregateAppIo)
                .onItem().transformToUni(unused -> formalCheckTaxCodeAndVatNumber(csvAggregateAppIo.getTaxCode(), csvAggregateAppIo.getVatNumber()))
                .onItem().transformToUni(unused -> retrieveDataFromIpa(onboardingMapper.csvToAggregateAppIo(csvAggregateAppIo)));
    }

    private Uni<Aggregate> checkCsvAggregateSend(CsvAggregateSend csvAggregateSend) {
        return checkRequiredFieldsSend(csvAggregateSend)
                .onItem().transformToUni(unused -> formalCheckTaxCodeAndVatNumber(csvAggregateSend.getTaxCode(), csvAggregateSend.getVatNumber()))
                .onItem().transformToUni(unused -> checkAdminTaxCode(csvAggregateSend))
                .onItem().transformToUni(unused -> retrieveDataFromIpa(onboardingMapper.csvToAggregateSend(csvAggregateSend)));
    }

    private Uni<Aggregate> checkCsvAggregatePagoPa(CsvAggregatePagoPa csvAggregatePagoPa) {
        return checkRequiredFieldsPagoPa(csvAggregatePagoPa)
                .onItem().transformToUni(unused -> formalCheckTaxCodeAndVatNumber(csvAggregatePagoPa.getTaxCode(), csvAggregatePagoPa.getVatNumber()))
                .onItem().transformToUni(unused -> retrieveDataFromIpa(onboardingMapper.csvToAggregatePagoPa(csvAggregatePagoPa)));
    }

    private Uni<Aggregate> retrieveDataFromIpa(Aggregate aggregate) {
        aggregate.setOrigin(InstitutionResource.OriginEnum.IPA.value());

        if (StringUtils.isEmpty(aggregate.getSubunitType())) {
            return institutionApi.findInstitutionUsingGET(aggregate.getTaxCode(), null, null)
                    .onFailure(this::checkIfNotFound).recoverWithUni(Uni.createFrom().failure(new ResourceNotFoundException(ERROR_IPA.getMessage())))
                    .onItem().transformToUni(institutionResource -> retrieveCityCountyAndMapIpaFieldForPA(institutionResource, aggregate));
        } else if (InstitutionPaSubunitType.AOO.name().equalsIgnoreCase(aggregate.getSubunitType())) {
            return aooApi.findByUnicodeUsingGET(aggregate.getSubunitCode(), null)
                    .onFailure(this::checkIfNotFound)
                    .recoverWithUni(Uni.createFrom().failure(new ResourceNotFoundException(ERROR_IPA.getMessage())))
                    .onItem().transformToUni(aooResource -> retrieveCityCountyAndMapIpaFieldForAOO(aooResource, aggregate));
        } else if (UO.name().equalsIgnoreCase(aggregate.getSubunitType())) {
            return uoApi.findByUnicodeUsingGET1(aggregate.getSubunitCode(), null)
                    .onFailure(this::checkIfNotFound)
                    .recoverWithUni(Uni.createFrom().failure(new ResourceNotFoundException(ERROR_IPA.getMessage())))
                    .onItem().transformToUni(uoResource -> retrieveCityCountyAndMapIpaFieldForUO(uoResource, aggregate));
        } else {
            return Uni.createFrom().failure(new InvalidRequestException(ERROR_SUBUNIT_TYPE.getMessage()));
        }
    }

    private Uni<Aggregate> retrieveCityCountyAndMapIpaFieldForUO(UOResource uoResource, Aggregate aggregateAppIo) {
        return Uni.combine().all()
                .unis(retrieveGeographicTaxonomies(uoResource.getCodiceComuneISTAT()), institutionApi.findInstitutionUsingGET(uoResource.getCodiceFiscaleEnte(), null, null))
                .asTuple()
                .onItem().transformToUni(tuple -> {
                    GeographicTaxonomyFromIstatCode geographicTaxonomyResource = tuple.getItem1();
                    InstitutionResource institutionResource = tuple.getItem2();
                    mapIpaField(uoResource.getDescrizioneUo(), uoResource.getIndirizzo(), uoResource.getCap(), null, aggregateAppIo, institutionResource.getDescription(), geographicTaxonomyResource);
                    return retrieveDigitalAddress(uoResource.getTipoMail1(), uoResource.getMail1(), institutionResource, aggregateAppIo);
                });
    }

    private Uni<Aggregate> retrieveCityCountyAndMapIpaFieldForAOO(AOOResource aooResource, Aggregate aggregateAppIo) {
        return Uni.combine().all()
                .unis(retrieveGeographicTaxonomies(aooResource.getCodiceComuneISTAT()), institutionApi.findInstitutionUsingGET(aooResource.getCodiceFiscaleEnte(), null, null))
                .asTuple()
                .onItem().transformToUni(tuple -> {
                    GeographicTaxonomyFromIstatCode geographicTaxonomyResource = tuple.getItem1();
                    InstitutionResource institutionResource = tuple.getItem2();
                    mapIpaField(aooResource.getDenominazioneAoo(), aooResource.getIndirizzo(), aooResource.getCap(), null, aggregateAppIo, institutionResource.getDescription(), geographicTaxonomyResource);
                    return retrieveDigitalAddress(aooResource.getTipoMail1(), aooResource.getMail1(), institutionResource, aggregateAppIo);
                });
    }

    private Uni<Aggregate> retrieveCityCountyAndMapIpaFieldForPA(InstitutionResource institutionResource, Aggregate aggregateAppIo) {
        return retrieveGeographicTaxonomies(institutionResource.getIstatCode())
                .onItem().transform(geographicTaxonomyResource -> {
                    mapIpaField(institutionResource.getDescription(), institutionResource.getAddress(), institutionResource.getZipCode(), institutionResource.getOriginId(), aggregateAppIo, null, geographicTaxonomyResource);
                    aggregateAppIo.setDigitalAddress(institutionResource.getDigitalAddress());
                    return aggregateAppIo;
                });
    }

    private Uni<Aggregate> retrieveDigitalAddress(String mailType, String mail, InstitutionResource institutionResource, Aggregate aggregateAppIo) {
        if (Objects.equals(mailType, PEC)) {
            aggregateAppIo.setDigitalAddress(mail);
        } else {
            aggregateAppIo.setDigitalAddress(institutionResource.getDigitalAddress());
        }
        return Uni.createFrom().item(aggregateAppIo);
    }

    private static void mapIpaField(String description, String address, String zipCode, String originId, Aggregate aggregateAppIo, String parentDescription ,GeographicTaxonomyFromIstatCode geographicTaxonomyFromIstatCode) {
        if(Objects.nonNull(geographicTaxonomyFromIstatCode)) {
            aggregateAppIo.setCounty(geographicTaxonomyFromIstatCode.getCounty());
            aggregateAppIo.setCity(geographicTaxonomyFromIstatCode.getCity());
        }
        aggregateAppIo.setDescription(description);
        aggregateAppIo.setAddress(address);
        aggregateAppIo.setZipCode(zipCode);
        aggregateAppIo.setOriginId(originId);
        aggregateAppIo.setParentDescription(parentDescription);
    }

    private Uni<GeographicTaxonomyFromIstatCode> retrieveGeographicTaxonomies(String codiceIstat) {
        GeographicTaxonomyFromIstatCode geographicTaxonomyFromIstatCode = expiringMap.get(codiceIstat);

        if (Objects.isNull(geographicTaxonomyFromIstatCode)) {
            return geographicTaxonomiesApi.retrieveGeoTaxonomiesByCodeUsingGET(codiceIstat)
                    .map(geographicTaxonomyResource -> GeographicTaxonomyFromIstatCode.builder()
                            .city(Optional.ofNullable(geographicTaxonomyResource.getDesc()).orElse("").replace(DESCRIPTION_TO_REPLACE_REGEX, ""))
                            .county(geographicTaxonomyResource.getProvinceAbbreviation())
                            .build())
                    .onItem().invoke(entity -> expiringMap.put(codiceIstat, entity));
        }
        return Uni.createFrom().item(geographicTaxonomyFromIstatCode);
    }

    private boolean checkIfNotFound(Throwable throwable) {
        return throwable instanceof WebApplicationException webApplicationException && webApplicationException.getResponse().getStatus() == 404;
    }

    private Uni<Void> checkRequiredFieldsAppIo(CsvAggregateAppIo csvAggregateAppIo) {

        if (StringUtils.isEmpty(csvAggregateAppIo.getTaxCode())) {
            return Uni.createFrom().failure(new InvalidRequestException(ERROR_TAXCODE.getMessage()));
        } else if (StringUtils.isEmpty(csvAggregateAppIo.getVatNumber())) {
            return Uni.createFrom().failure(new InvalidRequestException(ERROR_VATNUMBER.getMessage()));
        } else if ((StringUtils.isEmpty(csvAggregateAppIo.getSubunitType()) && StringUtils.isNotEmpty(csvAggregateAppIo.getSubunitCode()))
                || (StringUtils.isNotEmpty(csvAggregateAppIo.getSubunitType()) && StringUtils.isEmpty(csvAggregateAppIo.getSubunitCode()))) {
            return Uni.createFrom().failure(new InvalidRequestException(ERROR_AOO_UO.getMessage()));
        }
        return Uni.createFrom().voidItem();
    }

    private Uni<Void> checkRequiredFieldsPagoPa(CsvAggregatePagoPa csvAggregate) {

        if (StringUtils.isEmpty(csvAggregate.getTaxCode())) {
            return Uni.createFrom().failure(new InvalidRequestException(ERROR_TAXCODE.getMessage()));
        } else if (StringUtils.isEmpty(csvAggregate.getVatNumber())) {
            return Uni.createFrom().failure(new InvalidRequestException(ERROR_VATNUMBER.getMessage()));
        } else if (StringUtils.isEmpty(csvAggregate.getIban())) {
            return Uni.createFrom().failure(new InvalidRequestException(ERROR_IBAN.getMessage()));
        }
        return Uni.createFrom().voidItem();
    }

    private Uni<Void> checkRequiredFieldsSend(CsvAggregateSend csvAggregate) {

        if (StringUtils.isEmpty(csvAggregate.getTaxCode())) {
            return Uni.createFrom().failure(new InvalidRequestException(ERROR_TAXCODE.getMessage()));
        } else if (StringUtils.isEmpty(csvAggregate.getVatNumber())) {
            return Uni.createFrom().failure(new InvalidRequestException(ERROR_VATNUMBER.getMessage()));
        } else if ((StringUtils.isEmpty(csvAggregate.getSubunitType()) && StringUtils.isNotEmpty(csvAggregate.getSubunitCode()))
                || (StringUtils.isNotEmpty(csvAggregate.getSubunitType()) && StringUtils.isEmpty(csvAggregate.getSubunitCode()))) {
            return Uni.createFrom().failure(new InvalidRequestException(ERROR_AOO_UO.getMessage()));
        } else if (StringUtils.isEmpty(csvAggregate.getAdminAggregateName())) {
            return Uni.createFrom().failure(new InvalidRequestException(ERROR_ADMIN_NAME.getMessage()));
        } else if (StringUtils.isEmpty(csvAggregate.getAdminAggregateSurname())) {
            return Uni.createFrom().failure(new InvalidRequestException(ERROR_ADMIN_SURNAME.getMessage()));
        } else if (StringUtils.isEmpty(csvAggregate.getAdminAggregateTaxCode())) {
            return Uni.createFrom().failure(new InvalidRequestException(ERROR_ADMIN_TAXCODE.getMessage()));
        } else if (StringUtils.isEmpty(csvAggregate.getAdminAggregateEmail())) {
            return Uni.createFrom().failure(new InvalidRequestException(ERROR_ADMIN_EMAIL.getMessage()));
        } else if(StringUtils.isEmpty(csvAggregate.getRecipientCode())){
            return Uni.createFrom().failure(new InvalidRequestException(ERROR_CODICE_SDI.getMessage()));

        }
        return Uni.createFrom().voidItem();
    }

    private Uni<Void> checkAdminTaxCode(CsvAggregateSend csvAggregate) {

        String expectedSurnamePart = Utils.extractSurnamePart(csvAggregate.getAdminAggregateSurname());
        String expectedNamePart = Utils.extractNamePart(csvAggregate.getAdminAggregateName());

        String taxCode = csvAggregate.getAdminAggregateTaxCode();
        String taxCodeSurnamePart = taxCode.substring(0, 3).toUpperCase();
        String taxCodeNamePart = taxCode.substring(3, 6).toUpperCase();

        if (!taxCodeSurnamePart.equals(expectedSurnamePart)) {
            return Uni.createFrom().failure(new InvalidRequestException(ERROR_ADMIN_SURNAME_MISMATCH.getMessage()));
        }

        if (!taxCodeNamePart.equals(expectedNamePart)) {
            return Uni.createFrom().failure(new InvalidRequestException(ERROR_ADMIN_NAME_MISMATCH.getMessage()));
        }

        return Uni.createFrom().voidItem();
    }

    private Uni<Void> formalCheckTaxCodeAndVatNumber(String taxCode, String vatNumber) {

        if (taxCode.length() < 11) {
            return Uni.createFrom().failure(new InvalidRequestException(ERROR_TAXCODE_LENGTH.getMessage()));
        }

        if (vatNumber.length() < 11) {
            return Uni.createFrom().failure(new InvalidRequestException(ERROR_VATNUMBER_LENGTH.getMessage()));
        }

        return Uni.createFrom().voidItem();
    }

    private static void logVerificationSummary(VerifyAggregateResponse verifyAggregatePagoPaResponse) {
        log.info(LOG_CSV_ROWS,
                verifyAggregatePagoPaResponse.getAggregates().size(),
                verifyAggregatePagoPaResponse.getErrors().size());
    }
}
