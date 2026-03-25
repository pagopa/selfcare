package it.pagopa.selfcare.party.registry_proxy.web.model.mapper;

import it.pagopa.selfcare.party.registry_proxy.connector.model.national_registries_pdnd.PDNDBusiness;
import it.pagopa.selfcare.party.registry_proxy.web.model.PDNDBusinessResource;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PDNDInfoCamereBusinessMapper {

    List<PDNDBusinessResource> toResources(List<PDNDBusiness> pdndBusiness);

    PDNDBusinessResource toResource(PDNDBusiness pdndBusiness);

}
