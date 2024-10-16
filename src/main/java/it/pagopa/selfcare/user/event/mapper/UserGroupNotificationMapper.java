package it.pagopa.selfcare.user.event.mapper;

import it.pagopa.selfcare.user.event.entity.UserGroupEntity;
import it.pagopa.selfcare.user.model.UserGroupNotificationToSend;
import org.mapstruct.Mapper;

@Mapper(componentModel = "cdi")
public interface UserGroupNotificationMapper {

    UserGroupNotificationToSend toUserGroupNotificationToSend(UserGroupEntity userGroupEntity);
}
