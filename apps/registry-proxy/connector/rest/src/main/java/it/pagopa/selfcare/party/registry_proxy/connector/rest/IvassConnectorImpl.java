package it.pagopa.selfcare.party.registry_proxy.connector.rest;

import it.pagopa.selfcare.party.registry_proxy.connector.api.FileStorageConnector;
import it.pagopa.selfcare.party.registry_proxy.connector.api.IvassDataConnector;
import it.pagopa.selfcare.party.registry_proxy.connector.model.InsuranceCompany;
import it.pagopa.selfcare.party.registry_proxy.connector.model.ResourceResponse;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.client.IvassRestClient;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.exception.IvassFileParseException;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.utils.IvassUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.PropertySource;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@PropertySource("classpath:config/ivass-rest-config.properties")
@ConditionalOnProperty(
        value = "ivass.file.connector.type",
        havingValue = "rest")
public class IvassConnectorImpl implements IvassDataConnector {
    private final IvassRestClient ivassRestClient;
    private final List<String> registryTypesAdmitted;
    private final List<String> workTypesAdmitted;
    private final IvassUtils ivassUtils;
    private final FileStorageConnector fileStorageConnector;
    private final String ivassAzureFilename;

    public IvassConnectorImpl(
            IvassRestClient ivassRestClient,
            @Value("#{'${ivass.registryTypes.admitted}'.split(',')}") List<String> registryTypes,
            @Value("#{'${ivass.workTypes.admitted}'.split(',')}") List<String> registryWorkTypes,
            IvassUtils ivassUtils,
            @Nullable FileStorageConnector fileStorageConnector,
            @Value("${blobStorage.ivass.filename:ivass-data.csv}") String ivassAzureFilename
    ) {
        this.ivassRestClient = ivassRestClient;
        this.registryTypesAdmitted = registryTypes;
        this.workTypesAdmitted = registryWorkTypes;
        this.ivassUtils = ivassUtils;
        this.fileStorageConnector = fileStorageConnector;
        this.ivassAzureFilename = ivassAzureFilename;
    }

    @Override
    public List<InsuranceCompany> getInsurances() {
        byte[] zip = ivassRestClient.getInsurancesZip();
        byte[] csv = ivassUtils.extractFirstEntryByteArrayFromZip(zip);
        csv = ivassUtils.manageUTF8BOM(csv);

        List<InsuranceCompany> companies;
        try {
            companies = ivassUtils.readCsv(csv);
            backupToAzure(csv, companies);
        } catch (IvassFileParseException e) {
            log.warn("IVASS file downloaded from REST endpoint is not compliant with the expected format. " +
                    "Attempting fallback to last available file in Azure Blob Storage.", e);
            companies = getInsurancesFromAzureFallback();
        }

        return filterCompanies(companies);
    }

    /**
     * Saves the successfully-parsed CSV to Azure Blob Storage so it can be used as a fallback
     * in case future downloads from the IVASS REST endpoint are malformed.
     */
    private void backupToAzure(byte[] csv, List<InsuranceCompany> companies) {
        if (companies.isEmpty() || fileStorageConnector == null) {
            return;
        }
        try {
            fileStorageConnector.uploadFile(new ByteArrayInputStream(csv), ivassAzureFilename);
            log.info("IVASS CSV successfully backed up to Azure Blob Storage as '{}'", ivassAzureFilename);
        } catch (Exception e) {
            log.warn("Unable to back up IVASS CSV to Azure Blob Storage (non-blocking): {}", e.getMessage(), e);
        }
    }

    /**
     * Retrieves IVASS insurance data from Azure Blob Storage as a fallback when the file
     * downloaded from the IVASS REST endpoint cannot be parsed.
     */
    private List<InsuranceCompany> getInsurancesFromAzureFallback() {
        if (fileStorageConnector == null) {
            log.error("Azure Blob Storage fallback is not available (FileStorageConnector not configured). " +
                    "Returning empty list — IVASS index will NOT be updated.");
            return Collections.emptyList();
        }
        try {
            log.info("Retrieving IVASS data from Azure Blob Storage fallback file '{}'", ivassAzureFilename);
            ResourceResponse response = fileStorageConnector.getFile(ivassAzureFilename);
            List<InsuranceCompany> companies = ivassUtils.readCsv(response.getData());
            log.info("Successfully retrieved {} IVASS companies from Azure Blob Storage fallback", companies.size());
            return companies;
        } catch (Exception e) {
            log.error("Azure Blob Storage fallback failed for IVASS data. " +
                    "Returning empty list — IVASS index will NOT be updated. Error: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    private List<InsuranceCompany> filterCompanies(List<InsuranceCompany> companies) {
        return companies
                .stream()
                .filter(company -> StringUtils.hasText(company.getDigitalAddress())
                        && workTypesAdmitted.contains(company.getWorkType())
                        && registryTypesAdmitted
                        .stream()
                        .anyMatch(StringUtils.trimAllWhitespace(company.getRegisterType()
                                .split("-")[0])::equals))
                .collect(Collectors.toList());
    }

}
