package it.pagopa.selfcare.user_group.model.mapper;

import it.pagopa.selfcare.user_group.api.UserGroupOperations;
import it.pagopa.selfcare.user_group.model.CreateUserGroupDto;
import it.pagopa.selfcare.user_group.model.GroupDto;
import it.pagopa.selfcare.user_group.model.UpdateUserGroupDto;
import it.pagopa.selfcare.user_group.model.UserGroupResource;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface UserGroupMapper {

    @Mapping(source = ".", target = "members", qualifiedByName = "getMembersUUID")
    UserGroupResource toResource(UserGroupOperations entity);

    @Mapping(source = ".", target = "members", qualifiedByName = "getMembers")
    GroupDto fromDto(CreateUserGroupDto dto);

    @Mapping(source = ".", target = "members", qualifiedByName = "getOperationMembers")
    GroupDto toUserGroupOperations(UpdateUserGroupDto dto);

    @Named("getMembersUUID")
    default List<UUID> getMembersUUID(UserGroupOperations entity) {
        return entity.getMembers()
                .stream()
                .map(UUID::fromString)
                .collect(Collectors.toList());
    }

    @Named("getMembers")
    default Set<String> getMembers(CreateUserGroupDto entity) {
        return entity.getMembers()
                .stream()
                .map(UUID::toString)
                .collect(Collectors.toSet());
    }

    @Named("getOperationMembers")
    default Set<String> getOperationMembers(UpdateUserGroupDto entity) {
        return entity.getMembers()
                .stream()
                .map(UUID::toString)
                .collect(Collectors.toSet());
    }

}
