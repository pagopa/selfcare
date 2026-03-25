package it.pagopa.selfcare.party.registry_proxy.core;

import it.pagopa.selfcare.party.registry_proxy.connector.api.NationalRegistriesConnector;
import it.pagopa.selfcare.party.registry_proxy.connector.constant.AdEResultDetailEnum;
import it.pagopa.selfcare.party.registry_proxy.connector.exception.BadGatewayException;
import it.pagopa.selfcare.party.registry_proxy.connector.exception.InvalidRequestException;
import it.pagopa.selfcare.party.registry_proxy.connector.exception.ResourceNotFoundException;
import it.pagopa.selfcare.party.registry_proxy.connector.model.national_registries.Businesses;
import it.pagopa.selfcare.party.registry_proxy.connector.model.national_registries.LegalAddressProfessionalResponse;
import it.pagopa.selfcare.party.registry_proxy.connector.model.national_registries.LegalAddressResponse;
import it.pagopa.selfcare.party.registry_proxy.connector.model.national_registries.VerifyLegalResponse;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.utils.MaskDataUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.ArrayList;

@Slf4j
@Service
class NationalRegistriesServiceImpl implements NationalRegistriesService {

    private final NationalRegistriesConnector nationalRegistriesConnector;

    NationalRegistriesServiceImpl(NationalRegistriesConnector nationalRegistriesConnector) {
        this.nationalRegistriesConnector = nationalRegistriesConnector;
    }


    @Override
    public LegalAddressProfessionalResponse getLegalAddress(String taxId) {
        log.info("getLegalAddress for TaxId: {}", MaskDataUtils.maskString(taxId));
        LegalAddressResponse response = nationalRegistriesConnector.getLegalAddress(taxId);
        if (response == null ||
                response.getProfessionalAddress() == null ||
                !StringUtils.hasText(response.getProfessionalAddress().getAddress())) {
            throw new ResourceNotFoundException("Legal Address not found");
        }
        return toNationalRegistriesProfessionalResponse(response);
    }

    @Override
    public VerifyLegalResponse verifyLegal(String taxId, String vatNumber) {
        log.info("verify legal {} for vatNumber: {}", MaskDataUtils.maskString(taxId), MaskDataUtils.maskString(vatNumber));
        VerifyLegalResponse verifyLegalResponse = nationalRegistriesConnector.verifyLegal(taxId, vatNumber);
        return checkResponseErrorCode(verifyLegalResponse);
    }

    private VerifyLegalResponse checkResponseErrorCode(VerifyLegalResponse verifyLegalResponse) {
        Assert.notNull(verifyLegalResponse.getVerifyLegalResultDetail(), "verifyLegalResultDetail is required");
        AdEResultDetailEnum adEResultDetailEnum = AdEResultDetailEnum.fromValue(verifyLegalResponse.getVerifyLegalResultDetail().getValue());
        if(AdEResultDetailEnum.XX00 == adEResultDetailEnum){
            return verifyLegalResponse;
        }else if(AdEResultDetailEnum.XX01 == adEResultDetailEnum || AdEResultDetailEnum.XX02 == adEResultDetailEnum
        || AdEResultDetailEnum.XXXX == adEResultDetailEnum){
            throw new InvalidRequestException("Formato dati non corretto");
        }
        throw new BadGatewayException(verifyLegalResponse.getVerifyLegalResultDetailMessage());
    }

    @Override
    public Businesses institutionsByLegalTaxId(String legalTaxId) {
        log.info("institutionsByLegalTaxId for legalTaxId: {}", MaskDataUtils.maskString(legalTaxId));
        Businesses response = this.nationalRegistriesConnector.institutionsByLegalTaxId(legalTaxId);
        if(checkIfResponseIsInfoCamereNotFoundError(response)) {
            log.info("institutions not found for legalTaxId: {}",MaskDataUtils.maskString(legalTaxId));
            return createOkResponse(legalTaxId);
        }

        return response;
    }

    private boolean checkIfResponseIsInfoCamereNotFoundError(Businesses response) {
        return (
                response.getCode() != null && !"".equals(response.getCode())
                        || response.getDescription() != null && !"".equals(response.getDescription())
                        //|| response.getTimestamp() != null && !"".equals(response.getTimestamp())
                        || response.getAppName() != null && !"".equals(response.getAppName())
        );
    }

    private Businesses createOkResponse(String legalTaxId) {
        Businesses newResponse = new Businesses();
        newResponse.setLegalTaxId(legalTaxId);
        newResponse.setBusinessList(new ArrayList<>());
        newResponse.setDateTimeExtraction(OffsetDateTime.now().toString());
        return newResponse;
    }

    private LegalAddressProfessionalResponse toNationalRegistriesProfessionalResponse(LegalAddressResponse response) {
        LegalAddressProfessionalResponse result = new LegalAddressProfessionalResponse();
        if (response.getProfessionalAddress() != null) {
            result.setAddress(response.getProfessionalAddress().getAddress());
            result.setZip(response.getProfessionalAddress().getZip());
            result.setDescription(response.getProfessionalAddress().getDescription());
            result.setMunicipality(response.getProfessionalAddress().getMunicipality());
            result.setProvince(response.getProfessionalAddress().getProvince());
        }
        return result;
    }

}
