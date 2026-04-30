package it.pagopa.selfcare.mscore.core.strategy;

import it.pagopa.selfcare.mscore.api.InstitutionConnector;
import it.pagopa.selfcare.mscore.api.PartyRegistryProxyConnector;
import it.pagopa.selfcare.mscore.constant.Origin;
import it.pagopa.selfcare.mscore.core.mapper.InstitutionMapper;
import it.pagopa.selfcare.mscore.core.strategy.input.CreateInstitutionStrategyInput;
import it.pagopa.selfcare.mscore.core.util.InstitutionPaSubunitType;
import it.pagopa.selfcare.mscore.exception.MsCoreException;
import it.pagopa.selfcare.mscore.model.AreaOrganizzativaOmogenea;
import it.pagopa.selfcare.mscore.model.UnitaOrganizzativa;
import it.pagopa.selfcare.mscore.model.institution.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.owasp.encoder.Encode;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static it.pagopa.selfcare.mscore.constant.GenericError.CREATE_INSTITUTION_ERROR;

@Slf4j
@Component
public class CreateInstitutionStrategyIpa extends CreateInstitutionStrategyCommon implements CreateInstitutionStrategy {

    private final PartyRegistryProxyConnector partyRegistryProxyConnector;
    static final String DESCRIPTION_TO_REPLACE_REGEX = " - (COMUNE|PROVINCIA)";
    private final InstitutionMapper institutionMapper;

    public CreateInstitutionStrategyIpa(PartyRegistryProxyConnector partyRegistryProxyConnector,
                                        InstitutionConnector institutionConnector,
                                        InstitutionMapper institutionMapper) {
        super(institutionConnector);
        this.partyRegistryProxyConnector = partyRegistryProxyConnector;
        this.institutionMapper = institutionMapper;
    }


    @Override
    public Institution createInstitution(CreateInstitutionStrategyInput strategyInput) {

        List<Institution> institutions = institutionConnector.findByTaxCodeAndSubunitCode(strategyInput.getTaxCode(), strategyInput.getSubunitCode(), null);
        Institution toSavedOrUpdate;

        if (institutions.isEmpty()) {
            final InstitutionPaSubunitType subunitType = strategyInput.getSubunitType();
            final InstitutionProxyInfo institutionProxyInfo = partyRegistryProxyConnector.getInstitutionById(strategyInput.getTaxCode());
            final CategoryProxyInfo categoryProxyInfo = partyRegistryProxyConnector.getCategory(institutionProxyInfo.getOrigin(), institutionProxyInfo.getCategory());

            if (InstitutionPaSubunitType.AOO.equals(subunitType)) {
                Institution institutionEC = getOrSaveInstitutionEc(strategyInput, institutionProxyInfo, categoryProxyInfo);
                toSavedOrUpdate = mappingToInstitutionIPAAoo(strategyInput, institutionEC.getId(), institutionProxyInfo, categoryProxyInfo);
            } else if (InstitutionPaSubunitType.UO.equals(subunitType)) {
                Institution institutionEC = getOrSaveInstitutionEc(strategyInput, institutionProxyInfo, categoryProxyInfo);
                toSavedOrUpdate = mappingToInstitutionIPAUo(strategyInput, institutionEC.getId(), institutionProxyInfo, categoryProxyInfo);
            } else {
                log.info("createInstitution :: unsupported subunitType {}", Encode.forJava(String.valueOf(subunitType)));
                toSavedOrUpdate = getInstitutionEC(strategyInput.getTaxCode(), institutionProxyInfo, categoryProxyInfo);
            }
            toSavedOrUpdate.setGeographicTaxonomies(strategyInput.getGeographicTaxonomies());
            setContacts(strategyInput, toSavedOrUpdate);
            setAdditionalDataForIpa(strategyInput, toSavedOrUpdate);
        } else {
            //Institution exists but other fields could be updated
            toSavedOrUpdate = institutions.get(0);
            setUpdatedFields(strategyInput, institutions.get(0));
            setAdditionalDataForIpa(strategyInput, toSavedOrUpdate);
        }

        try {
            return institutionConnector.save(toSavedOrUpdate);
        } catch (Exception e) {
            throw new MsCoreException(CREATE_INSTITUTION_ERROR.getMessage(), CREATE_INSTITUTION_ERROR.getCode());
        }
    }

    private Institution getOrSaveInstitutionEc(CreateInstitutionStrategyInput strategyInput,
                                               InstitutionProxyInfo institutionProxyInfo,
                                               CategoryProxyInfo categoryProxyInfo) {
        try {
            Optional<Institution> opt = institutionConnector.findByExternalId(strategyInput.getTaxCode());
            if (opt.isEmpty()) {
                Institution institutionEC = getInstitutionEC(strategyInput.getTaxCode(), institutionProxyInfo, categoryProxyInfo);
                return institutionConnector.save(institutionEC);
            } else {
                return opt.get();
            }
        } catch (Exception e) {
            throw new MsCoreException(CREATE_INSTITUTION_ERROR.getMessage(), CREATE_INSTITUTION_ERROR.getCode());
        }
    }

    private Institution getInstitutionEC(String taxCode, InstitutionProxyInfo institutionProxyInfo, CategoryProxyInfo categoryProxyInfo) {

        Institution newInstitution = institutionMapper.fromInstitutionProxyInfo(institutionProxyInfo);
        GeographicTaxonomies geotax = partyRegistryProxyConnector.getExtByCode(institutionProxyInfo.getIstatCode());

        newInstitution.setExternalId(taxCode);
        newInstitution.setOrigin(Origin.IPA.getValue());
        newInstitution.setCreatedAt(OffsetDateTime.now());
        newInstitution.setCity(Optional.ofNullable(geotax.getDescription())
                .map(s -> s.replaceAll(DESCRIPTION_TO_REPLACE_REGEX, ""))
                .orElse(null));
        newInstitution.setCounty(geotax.getProvinceAbbreviation());
        newInstitution.setCountry(geotax.getCountryAbbreviation());

        Attributes attributes = new Attributes();
        attributes.setOrigin(categoryProxyInfo.getOrigin());
        attributes.setCode(categoryProxyInfo.getCode());
        attributes.setDescription(categoryProxyInfo.getName());
        newInstitution.setAttributes(List.of(attributes));

        return newInstitution;
    }


    private Institution mappingToInstitutionIPAAoo(CreateInstitutionStrategyInput strategyInput,
                                                   String rootParentInstitutionId,
                                                   InstitutionProxyInfo institutionProxyInfo,
                                                   CategoryProxyInfo categoryProxyInfo) {

        AreaOrganizzativaOmogenea areaOrganizzativaOmogenea = partyRegistryProxyConnector.getAooById(strategyInput.getSubunitCode());
        GeographicTaxonomies geotax = partyRegistryProxyConnector.getExtByCode(areaOrganizzativaOmogenea.getCodiceComuneISTAT());
        Institution newInstitution = new Institution();
        newInstitution.setOriginId(areaOrganizzativaOmogenea.getId());
        newInstitution.setDescription(areaOrganizzativaOmogenea.getDenominazioneAoo());
        newInstitution.setDigitalAddress(TYPE_MAIL_PEC.equals(areaOrganizzativaOmogenea.getTipoMail1())
                ? areaOrganizzativaOmogenea.getMail1() : institutionProxyInfo.getDigitalAddress());
        newInstitution.setAddress(areaOrganizzativaOmogenea.getIndirizzo());
        newInstitution.setZipCode(areaOrganizzativaOmogenea.getCAP());
        newInstitution.setTaxCode(areaOrganizzativaOmogenea.getCodiceFiscaleEnte());
        newInstitution.setSubunitCode(strategyInput.getSubunitCode());
        newInstitution.setSubunitType(InstitutionPaSubunitType.AOO.name());
        newInstitution.setParentDescription(institutionProxyInfo.getDescription());
        newInstitution.setRootParentId(rootParentInstitutionId);
        newInstitution.setExternalId(getExternalId(strategyInput));
        newInstitution.setOrigin(Optional.ofNullable(areaOrganizzativaOmogenea.getOrigin())
                .map(Origin::name)
                .orElse(null));
        newInstitution.setCreatedAt(OffsetDateTime.now());
        newInstitution.setCounty(geotax.getProvinceAbbreviation());
        newInstitution.setCountry(geotax.getCountryAbbreviation());
        newInstitution.setCity(Optional.ofNullable(geotax.getDescription())
                .map(s -> s.replaceAll(DESCRIPTION_TO_REPLACE_REGEX, ""))
                .orElse(null));
        newInstitution.setIstatCode(areaOrganizzativaOmogenea.getCodiceComuneISTAT());
        Attributes attributes = new Attributes();
        attributes.setOrigin(categoryProxyInfo.getOrigin());
        attributes.setCode(categoryProxyInfo.getCode());
        attributes.setDescription(categoryProxyInfo.getName());
        newInstitution.setAttributes(List.of(attributes));

        return newInstitution;
    }

    private Institution mappingToInstitutionIPAUo(CreateInstitutionStrategyInput strategyInput,
                                                  String rootParentInstitutionId,
                                                  InstitutionProxyInfo institutionProxyInfo,
                                                  CategoryProxyInfo categoryProxyInfo) {

        UnitaOrganizzativa unitaOrganizzativa = partyRegistryProxyConnector.getUoById(strategyInput.getSubunitCode());
        GeographicTaxonomies geotax = partyRegistryProxyConnector.getExtByCode(unitaOrganizzativa.getCodiceComuneISTAT());
        Institution newInstitution = new Institution();
        newInstitution.setOriginId(unitaOrganizzativa.getId());
        newInstitution.setDescription(unitaOrganizzativa.getDescrizioneUo());
        newInstitution.setDigitalAddress(TYPE_MAIL_PEC.equals(unitaOrganizzativa.getTipoMail1())
                ? unitaOrganizzativa.getMail1() : institutionProxyInfo.getDigitalAddress());
        newInstitution.setAddress(unitaOrganizzativa.getIndirizzo());
        newInstitution.setZipCode(unitaOrganizzativa.getCAP());
        newInstitution.setTaxCode(unitaOrganizzativa.getCodiceFiscaleEnte());
        newInstitution.setSubunitCode(strategyInput.getSubunitCode());
        newInstitution.setSubunitType(InstitutionPaSubunitType.UO.name());
        newInstitution.setParentDescription(institutionProxyInfo.getDescription());
        newInstitution.setRootParentId(rootParentInstitutionId);
        newInstitution.setCity(Optional.ofNullable(geotax.getDescription())
                .map(s -> s.replaceAll(DESCRIPTION_TO_REPLACE_REGEX, ""))
                .orElse(null));
        newInstitution.setCounty(geotax.getProvinceAbbreviation());
        newInstitution.setCountry(geotax.getCountryAbbreviation());
        newInstitution.setIstatCode(unitaOrganizzativa.getCodiceComuneISTAT());
        if (StringUtils.isNotBlank(unitaOrganizzativa.getCodiceUniAoo())) {
            PaAttributes paAttributes = new PaAttributes();
            paAttributes.setAooParentCode(unitaOrganizzativa.getCodiceUniAoo());
            newInstitution.setPaAttributes(paAttributes);
        }

        newInstitution.setExternalId(getExternalId(strategyInput));
        newInstitution.setOrigin(Optional.ofNullable(unitaOrganizzativa.getOrigin())
                .map(Origin::name)
                .orElse(null));
        newInstitution.setCreatedAt(OffsetDateTime.now());

        Attributes attributes = new Attributes();
        attributes.setOrigin(categoryProxyInfo.getOrigin());
        attributes.setCode(categoryProxyInfo.getCode());
        attributes.setDescription(categoryProxyInfo.getName());
        newInstitution.setAttributes(List.of(attributes));

        return newInstitution;
    }

}
