package it.pagopa.selfcare.party.registry_proxy.core;

import it.pagopa.selfcare.party.registry_proxy.connector.api.PDNDInfoCamereConnector;
import it.pagopa.selfcare.party.registry_proxy.connector.model.national_registries_pdnd.PDNDBusiness;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.List;

@Slf4j
@Service
class PDNDInfoCamereServiceImpl implements PDNDInfoCamereService {

    private final PDNDInfoCamereConnector pdndInfoCamereConnector;

    @Autowired
    PDNDInfoCamereServiceImpl(PDNDInfoCamereConnector pdndInfoCamereConnector) {
        this.pdndInfoCamereConnector = pdndInfoCamereConnector;
    }


    @Override
    public List<PDNDBusiness> retrieveInstitutionsPdndByDescription(String description){
        Assert.hasText(description, "Description is required");
        return pdndInfoCamereConnector.retrieveInstitutionsPdndByDescription(description);
    }

    @Override
    public PDNDBusiness retrieveInstitutionPdndByTaxCode(String taxCode){
        Assert.hasText(taxCode, "TaxCode is required");
        return pdndInfoCamereConnector.retrieveInstitutionPdndByTaxCode(taxCode);
    }

    @Override
    public PDNDBusiness retrieveInstitutionDetail(String taxCode) {
        return pdndInfoCamereConnector.retrieveInstitutionDetail(taxCode);
    }

    @Override
    public byte[] retrieveInstitutionDocument(String taxCode) {
        return pdndInfoCamereConnector.retrieveInstitutionDocument(taxCode);
    }

    @Override
    public PDNDBusiness retrieveInstitutionFromRea(String county, String rea) {
        Assert.hasText(rea, "Rea is required");
        Assert.hasText(county, "county is required");
        return pdndInfoCamereConnector.retrieveInstitutionFromRea(county, rea);
    }

}
