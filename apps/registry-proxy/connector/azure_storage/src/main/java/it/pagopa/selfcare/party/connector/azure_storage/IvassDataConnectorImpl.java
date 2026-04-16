package it.pagopa.selfcare.party.connector.azure_storage;

import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import it.pagopa.selfcare.party.connector.azure_storage.model.IvassDataTemplate;
import it.pagopa.selfcare.party.registry_proxy.connector.api.FileStorageConnector;
import it.pagopa.selfcare.party.registry_proxy.connector.api.IvassDataConnector;
import it.pagopa.selfcare.party.registry_proxy.connector.model.InsuranceCompany;
import it.pagopa.selfcare.party.registry_proxy.connector.model.ResourceResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@PropertySource("classpath:config/ivass-config.properties")
@ConditionalOnProperty(
        value = "ivass.file.connector.type",
        havingValue = "azure",
        matchIfMissing = true)
public class IvassDataConnectorImpl implements IvassDataConnector {

    private final FileStorageConnector fileStorageConnector;
    private final String sourceFilename;
    private final List<String> registryTypesAdmitted;
    private final List<String> workTypesAdmitted;

    public IvassDataConnectorImpl(@Value("${blobStorage.ivass.filename}") String ivassCsvFileName,
                                  @Value("#{'${ivass.registryTypes.admitted}'.split(',')}") List<String> registryTypes,
                                  @Value("#{'${ivass.workTypes.admitted}'.split(',')}") List<String> registryWorkTypes,
                                  FileStorageConnector fileStorageConnector) {
        this.fileStorageConnector = fileStorageConnector;
        this.registryTypesAdmitted = registryTypes;
        this.workTypesAdmitted = registryWorkTypes;
        this.sourceFilename = ivassCsvFileName;
    }
    @Override
    public List<InsuranceCompany> getInsurances() {
        log.trace("getInsurances start");
        List<InsuranceCompany> companies = new ArrayList<>();
        ResourceResponse resourceResponse;
        try {
            resourceResponse = fileStorageConnector.getFile(sourceFilename);
        } catch (Exception e) {
            log.error("Impossible to retrieve file IVASS. Error: {}", e.getMessage(), e);
            return companies;
        }
        try (Reader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(resourceResponse.getData())))) {
            CsvToBean<InsuranceCompany> csvToBean = new CsvToBeanBuilder<InsuranceCompany>(reader)
                    .withType(IvassDataTemplate.class)
                    .withSeparator(';')
                    .build();
            companies = csvToBean.parse();
        } catch (Exception e) {
            log.error("Impossible to acquire data for IVASS. Error: {}", e.getMessage(), e);
        }
        log.trace("getInsurances end");
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
