package it.pagopa.selfcare.user_group.model.mapper;

import it.pagopa.selfcare.user_group.api.UserGroupOperations;
import it.pagopa.selfcare.user_group.model.*;
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

    @Mapping(target = "members", expression = "java(getMembers(dto.getMembers()))")
    GroupDto fromDto(CreateUserGroupDto dto);

    @Mapping(target = "members", expression = "java(getMembers(dto.getMembers()))")
    GroupDto toUserGroupOperations(UpdateUserGroupDto dto);

    @Mapping(target = "members", expression = "java(getMembers(dto.getMembers()))")
    GroupDto toUserGroupOperations(AddMembersToUserGroupDto dto);

    @Named("getMembersUUID")
    default List<UUID> getMembersUUID(UserGroupOperations entity) {
        return entity.getMembers()
                .stream()
                .map(UUID::fromString)
                .toList();
    }

    default Set<String> getMembers(Set<UUID> members) {
        return members
                .stream()
                .map(UUID::toString)
                .collect(Collectors.toSet());
    }

}
