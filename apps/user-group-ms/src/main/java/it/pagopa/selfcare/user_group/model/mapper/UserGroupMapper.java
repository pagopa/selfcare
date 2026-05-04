package it.pagopa.selfcare.user_group.model.mapper;

import it.pagopa.selfcare.user_group.api.UserGroupOperations;
import it.pagopa.selfcare.user_group.model.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    default Sort toSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return Sort.unsorted();
        }

        final List<String> sortParams = Stream.of(sort.split(",")).map(String::trim).toList();
        final List<Sort.Order> orders = new ArrayList<>();
        String prop = null;
        for (String s : sortParams) {
            if (prop == null) {
                prop = s;
            } else if (s.equalsIgnoreCase(Sort.Direction.ASC.name()) || s.equalsIgnoreCase(Sort.Direction.DESC.name())) {
                orders.add(new Sort.Order(Sort.Direction.fromString(s), prop));
                prop = null;
            } else {
                orders.add(new Sort.Order(Sort.Direction.ASC, prop));
                prop = s;
            }
        }

        if (prop != null) {
            orders.add(new Sort.Order(Sort.Direction.ASC, prop));
        }

        return Sort.by(orders);
    }

}
