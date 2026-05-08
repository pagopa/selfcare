package it.pagopa.selfcare.dashboard.model.mapper;

import it.pagopa.selfcare.dashboard.model.backoffice.BrokerInfo;
import it.pagopa.selfcare.dashboard.model.product.BrokerResource;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface BrokerResourceMapper {

    BrokerResource toResource(BrokerInfo brokerInfo);
    List<BrokerResource> toResourceList(List<BrokerInfo> brokerInfos);
    
}
