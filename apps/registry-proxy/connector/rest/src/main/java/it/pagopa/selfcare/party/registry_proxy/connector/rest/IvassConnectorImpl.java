package it.pagopa.selfcare.party.registry_proxy.connector.rest;

import it.pagopa.selfcare.party.registry_proxy.connector.api.IvassDataConnector;
import it.pagopa.selfcare.party.registry_proxy.connector.model.InsuranceCompany;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.client.IvassRestClient;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.utils.IvassUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

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

    public IvassConnectorImpl(
            IvassRestClient ivassRestClient,
            @Value("#{'${ivass.registryTypes.admitted}'.split(',')}") List<String> registryTypes,
            @Value("#{'${ivass.workTypes.admitted}'.split(',')}") List<String> registryWorkTypes,
            IvassUtils ivassUtils
    ) {
        this.ivassRestClient = ivassRestClient;
        this.registryTypesAdmitted = registryTypes;
        this.workTypesAdmitted = registryWorkTypes;
        this.ivassUtils = ivassUtils;
    }

    @Override
    public List<InsuranceCompany> getInsurances() {
        byte[] zip = ivassRestClient.getInsurancesZip();
        byte[] csv = ivassUtils.extractFirstEntryByteArrayFromZip(zip);
        csv = ivassUtils.manageUTF8BOM(csv);
        List<InsuranceCompany> companies = ivassUtils.readCsv(csv);
        return filterCompanies(companies);
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
