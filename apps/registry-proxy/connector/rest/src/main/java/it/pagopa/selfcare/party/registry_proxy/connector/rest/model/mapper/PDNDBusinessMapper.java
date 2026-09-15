package it.pagopa.selfcare.party.registry_proxy.connector.rest.model.mapper;

import it.pagopa.selfcare.party.registry_proxy.connector.model.national_registries_pdnd.PDNDBusiness;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.model.PDNDImpresa;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PDNDBusinessMapper {

    List<PDNDBusiness> toPDNDBusinesses(List<PDNDImpresa> pdndImpresaList);

    @Mapping(target = "city", source = "businessAddress.city")
    @Mapping(target = "county", source = "businessAddress.county")
    @Mapping(target = "zipCode", source = "businessAddress.zipCode")
    @Mapping(target = "digitalAddress", source = "digitalAddress")
    PDNDBusiness toPDNDBusiness(PDNDImpresa pdndImpresa);


}
