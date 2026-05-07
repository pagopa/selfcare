package it.pagopa.selfcare.dashboard.model.mapper;

import it.pagopa.selfcare.commons.base.security.SelfCareUser;
import it.pagopa.selfcare.dashboard.model.support.SupportRequest;
import it.pagopa.selfcare.dashboard.model.support.SupportRequestDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface SupportMapper {

    @Mapping(source = "supportRequest.email", target = "email")
    @Mapping(source = "user", target = "name", qualifiedByName = "concatName")
    @Mapping(source = "user.fiscalCode", target = "userFields.aux_data")
    SupportRequest toZendeskRequest(SupportRequestDto supportRequest, SelfCareUser user);

    @Named("concatName")
    static String concatName(SelfCareUser user) {
        return user.getUserName() + " " + user.getSurname();
    }

}
