package it.pagopa.selfcare.user.event.mapper;

import it.pagopa.selfcare.user.model.OnboardedProduct;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.control.DeepClone;

@Mapper(componentModel = "cdi", mappingControl = DeepClone.class)
public interface CloningMapper {

    @Mapping(target = "toAddOnAggregates", ignore = true)
    void copy(OnboardedProduct source, @MappingTarget OnboardedProduct target);

}
