package it.pagopa.selfcare.party.registry_proxy.connector.rest;

import io.github.resilience4j.retry.annotation.Retry;
import it.pagopa.selfcare.party.registry_proxy.connector.api.NationalRegistriesConnector;
import it.pagopa.selfcare.party.registry_proxy.connector.model.national_registries.*;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.client.NationalRegistriesRestClient;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Slf4j
@Service
public class NationalRegistriesConnectorImpl implements NationalRegistriesConnector {

    private final NationalRegistriesRestClient nationalRegistriesRestClient;

    public NationalRegistriesConnectorImpl(NationalRegistriesRestClient nationalRegistriesRestClient) {
        this.nationalRegistriesRestClient = nationalRegistriesRestClient;
    }

    @Override
    @Retry(name = "retryServiceUnavailable")
    public LegalAddressResponse getLegalAddress(String taxCode) {
        LegalAddressRequest legalAddressRequest = createLegalAddressRequest(taxCode);
        return toLegalAddressResponse(nationalRegistriesRestClient.getLegalAddress(legalAddressRequest));
    }

    @Override
    @Retry(name = "retryServiceUnavailable")
    public VerifyLegalResponse verifyLegal(String taxId, String vatNumber) {
        AdELegalRequestBodyDto request = createRequest(taxId, vatNumber);
        return toVerifyLegalResponse(nationalRegistriesRestClient.verifyLegal(request));
    }

    @Override
    @Retry(name = "retryServiceUnavailable")
    public Businesses institutionsByLegalTaxId(String legalTaxId) {
        log.info("start institutionsByLegalTaxId");
        LegalInstitutionsRequestBody request = createLegalInstitutionsRequest(legalTaxId);
        return nationalRegistriesRestClient.getLegalInstitutions(request);
    }


    private LegalAddressResponse toLegalAddressResponse(GetAddressRegistroImpreseOKDto legalAddress) {
        LegalAddressResponse legalAddressResponse = new LegalAddressResponse();
        legalAddressResponse.setTaxId(legalAddress.getTaxId());
        legalAddressResponse.setDateTimeExtraction(legalAddress.getDateTimeExtraction());
        if(legalAddress.getProfessionalAddress() != null) {
            legalAddressResponse.setProfessionalAddress(toProfessionalAddress(legalAddress.getProfessionalAddress()));
        }
        return legalAddressResponse;
    }

    private LegalAddressProfessionalResponse toProfessionalAddress(ProfessionalAddressDto professionalAddress) {
        LegalAddressProfessionalResponse legalAddressProfessionalResponse = new LegalAddressProfessionalResponse();
        legalAddressProfessionalResponse.setAddress(professionalAddress.getAddress());
        legalAddressProfessionalResponse.setZip(professionalAddress.getZip());
        legalAddressProfessionalResponse.setProvince(professionalAddress.getProvince());
        legalAddressProfessionalResponse.setMunicipality(professionalAddress.getMunicipality());
        legalAddressProfessionalResponse.setDescription(professionalAddress.getDescription());
        return legalAddressProfessionalResponse;
    }

    private VerifyLegalResponse toVerifyLegalResponse(AdELegalOKDto adELegalOKDto) {
        Assert.notNull(adELegalOKDto.getResultDetail(), "ResultDetail is required");
        Assert.notNull(adELegalOKDto.getResultCode(), "ResultCode is required");
        VerifyLegalResponse verifyLegalResponse = new VerifyLegalResponse();
        if(adELegalOKDto.getVerificationResult() != null){
            verifyLegalResponse.setVerificationResult(adELegalOKDto.getVerificationResult());
        }
        verifyLegalResponse.setVerifyLegalResultDetail(adELegalOKDto.getResultDetail());
        verifyLegalResponse.setVerifyLegalResultCode(adELegalOKDto.getResultCode());
        verifyLegalResponse.setVerifyLegalResultDetailMessage(adELegalOKDto.getResultDetailMessage());
        return verifyLegalResponse;
    }

    private LegalAddressRequest createLegalAddressRequest(String taxCode) {
        LegalAddressRequest legalAddressRequest = new LegalAddressRequest();
        LegalAddressFilter filter = new LegalAddressFilter();
        filter.setTaxId(taxCode);
        legalAddressRequest.setFilter(filter);
        return legalAddressRequest;
    }

    private LegalInstitutionsRequestBody createLegalInstitutionsRequest(String taxCode) {
        LegalInstitutionsRequestBody legalInstitutionsRequest = new LegalInstitutionsRequestBody();
        LegalInstutionsRequestBodyFilterDto filter = new LegalInstutionsRequestBodyFilterDto();
        filter.setTaxId(taxCode);
        legalInstitutionsRequest.setFilter(filter);
        return legalInstitutionsRequest;
    }

    private AdELegalRequestBodyDto createRequest(String taxCode, String vatNumber) {
        AdELegalRequestBodyDto requestBodyDto = new AdELegalRequestBodyDto();
        AdELegalRequestBodyFilterDto filter = new AdELegalRequestBodyFilterDto();
        filter.setTaxId(taxCode);
        filter.setVatNumber(vatNumber);
        requestBodyDto.setFilter(filter);
        return requestBodyDto;
    }
}
