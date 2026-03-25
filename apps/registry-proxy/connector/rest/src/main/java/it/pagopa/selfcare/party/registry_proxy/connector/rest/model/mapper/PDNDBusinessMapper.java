package it.pagopa.selfcare.party.registry_proxy.connector.rest.model.mapper;

import it.pagopa.selfcare.party.registry_proxy.connector.model.national_registries_pdnd.PDNDBusiness;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.config.PDNDConfig;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.model.PDNDImpresa;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.model.PDNDVisuraImpresa;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.model.visura.ClassificazioneAteco;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.model.visura.Localizzazione;

import java.util.*;
import java.util.stream.Collectors;

import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface PDNDBusinessMapper {

    List<PDNDBusiness> toPDNDBusinesses(List<PDNDImpresa> pdndImpresaList);

    @Mapping(target = "city", source = "businessAddress.city")
    @Mapping(target = "county", source = "businessAddress.county")
    @Mapping(target = "zipCode", source = "businessAddress.zipCode")
    PDNDBusiness toPDNDBusiness(PDNDImpresa pdndImpresa);

    @Mapping(target = ".", source = "datiIdentificativiImpresa")
    @Mapping(target = "city", source = "datiIdentificativiImpresa.localizzazione.comune")
    @Mapping(target = "county", source = "datiIdentificativiImpresa.localizzazione.provincia")
    @Mapping(target = "zipCode", source = "datiIdentificativiImpresa.localizzazione.cap")
    @Mapping(target = "vatNumber", source = "datiIdentificativiImpresa.vatNumber")
    @Mapping(target = "legalForm", source = "datiIdentificativiImpresa.legalForm")
    @Mapping(target = "atecoCodes", source = ".", qualifiedByName = "mapAtecoCodes")
    @Mapping(target = "address", source = "datiIdentificativiImpresa.localizzazione", qualifiedByName = "mapAddress")
    @Mapping(target = "disabledStateInstitution", source = "infoAttivita.disabledStateInstitution")
    @Mapping(target = "descriptionStateInstitution", source = "infoAttivita.descriptionStateInstitution")
    @Mapping(target = "statusCompanyRI", source = "datiIdentificativiImpresa.statusCompanyRI")
    @Mapping(target = "statusCompanyRD", source = "datiIdentificativiImpresa.statusCompanyRD")
    PDNDBusiness toPDNDBusiness(PDNDVisuraImpresa pdndImpresa, @Context PDNDConfig config);

    @Named("mapAtecoCodes")
    default List<String> mapAtecoCodes(PDNDVisuraImpresa pdndVisuraImpresa, @Context PDNDConfig config) {
        Set<String> atecoCodes = new HashSet<>();
        if (Objects.nonNull(pdndVisuraImpresa.getInfoAttivita())) {
            var classificazioniAteco = pdndVisuraImpresa.getInfoAttivita().getClassificazioniAteco();
            if (Objects.nonNull(classificazioniAteco)
                    && Objects.nonNull(classificazioniAteco.getClassificazioniAteco())) {
                atecoCodes.addAll(
                        classificazioniAteco.getClassificazioniAteco().stream()
                                .map(ClassificazioneAteco::getCodiceAttivita)
                                .filter(Objects::nonNull)
                                .collect(Collectors.toSet()));
            }
        }

        if (Boolean.FALSE.equals(config.getSkipLocalizzazioneNodes())) {
            var pointOfSales = pdndVisuraImpresa.getPointOfSales();
            if (Objects.nonNull(pointOfSales) && Objects.nonNull(pointOfSales.getLocalizzazioni())) {
                atecoCodes.addAll(
                        pointOfSales.getLocalizzazioni().stream()
                                .filter(
                                        loc ->
                                                Objects.nonNull(loc.getClassificazioniAteco())
                                                        && Objects.nonNull(
                                                        loc.getClassificazioniAteco().getClassificazioniAteco()))
                                .flatMap(
                                        loc ->
                                                loc.getClassificazioniAteco().getClassificazioniAteco().stream()
                                                        .map(ClassificazioneAteco::getCodiceAttivita)
                                                        .filter(Objects::nonNull))
                                .collect(Collectors.toSet()));
            }
        }

        return new ArrayList<>(atecoCodes);
    }


    @Named("mapAddress")
    default String mapAddress(Localizzazione localizzazione) {
        return localizzazione.getToponimo() + " " + localizzazione.getVia() + ", " + localizzazione.getCivico();
    }

}
