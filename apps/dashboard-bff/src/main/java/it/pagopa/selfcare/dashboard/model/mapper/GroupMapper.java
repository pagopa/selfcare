package it.pagopa.selfcare.dashboard.model.mapper;

import it.pagopa.selfcare.dashboard.model.groups.CreateUserGroup;
import it.pagopa.selfcare.dashboard.model.groups.UpdateUserGroup;
import it.pagopa.selfcare.dashboard.model.groups.UserGroup;
import it.pagopa.selfcare.dashboard.model.groups.UserGroupInfo;
import it.pagopa.selfcare.dashboard.model.user.CertifiedField;
import it.pagopa.selfcare.dashboard.model.user.User;
import it.pagopa.selfcare.dashboard.model.user.UserInfo;
import it.pagopa.selfcare.dashboard.model.user_groups.*;
import it.pagopa.selfcare.dashboard.utils.EncodingUtils;
import it.pagopa.selfcare.group.generated.openapi.v1.dto.CreateUserGroupDto;
import it.pagopa.selfcare.group.generated.openapi.v1.dto.UpdateUserGroupDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import jakarta.validation.ValidationException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", uses = {UserMapper.class})
public interface GroupMapper {

    CreateUserGroupDto toCreateUserGroupDto(CreateUserGroup createUserGroup);

    UpdateUserGroupDto toUpdateUserGroupDto(UpdateUserGroup updateUserGroup);

    UserGroup toUserGroup(it.pagopa.selfcare.group.generated.openapi.v1.dto.UserGroupResource userGroupResource);

    @Mapping(target = "createdBy", expression = "java(toUser(userGroupResource.getCreatedBy()))")
    @Mapping(target = "modifiedBy", expression = "java(toUser(userGroupResource.getModifiedBy()))")
    @Mapping(target = "members", expression = "java(toUserInfo(userGroupResource.getMembers()))")
    UserGroupInfo toUserGroupInfo(it.pagopa.selfcare.group.generated.openapi.v1.dto.UserGroupResource userGroupResource);

    @Named("toUserInfo")
    default List<UserInfo> toUserInfo(List<UUID> uuidList) {
        if (uuidList == null) {
            return Collections.emptyList();
        }
        return uuidList.stream().map(id -> {
            UserInfo member = new UserInfo();
            member.setId(id.toString());
            return member;
        }).toList();
    }

    @Named("toUser")
    default User toUser(String userId) {
        if (userId == null) {
            return null;
        }
        User user = new User();
        user.setId(userId);
        return user;
    }

    default CreateUserGroup fromDto(CreateUserGroupDto dto) {
        CreateUserGroup model = null;
        if (dto != null) {
            model = new CreateUserGroup();
            model.setName(dto.getName());
            model.setDescription(dto.getDescription());
            model.setInstitutionId(dto.getInstitutionId());
            model.setProductId(dto.getProductId());
            if (dto.getMembers() == null) {
                throw new ValidationException("Members list must not be null");
            }
            model.setMembers(dto.getMembers().stream().map(UUID::toString).collect(Collectors.toList()));
        }
        return model;
    }

    default UpdateUserGroup fromDto(UpdateUserGroupDto dto) {
        UpdateUserGroup model = null;
        if (dto != null) {
            model = new UpdateUserGroup();
            model.setDescription(dto.getDescription());
            model.setName(dto.getName());
            if (dto.getMembers() == null) {
                throw new ValidationException("Members list must not be null");
            }
            model.setMembers(dto.getMembers().stream().map(UUID::toString).collect(Collectors.toList()));
        }
        return model;
    }

    default UserGroupResource toResource(UserGroupInfo model, UserMapper userMapper) {
        UserGroupResource resource = null;
        if (model != null) {
            resource = new UserGroupResource();
            resource.setMembers(model.getMembers().stream()
                    .map(userMapper::toProductUser)
                    .collect(Collectors.toList()));
            resource.setId(model.getId());
            resource.setName(model.getName());
            resource.setStatus(model.getStatus());
            resource.setDescription(model.getDescription());
            resource.setCreatedAt(model.getCreatedAt());
            resource.setCreatedBy(toPlainResource(model.getCreatedBy()));
            resource.setModifiedAt(model.getModifiedAt());
            resource.setModifiedBy(toPlainResource(model.getModifiedBy()));
            resource.setInstitutionId(model.getInstitutionId());
            resource.setProductId(model.getProductId());
        }
        return resource;
    }

    default UserGroupIdResource toIdResource(String groupId) {
        UserGroupIdResource resource = null;
        if (groupId != null) {
            resource = new UserGroupIdResource();
            resource.setId(groupId);
        }
        return resource;
    }

    default PlainUserResource toPlainResource(User model) {
        return Optional.ofNullable(model)
                .map(user -> {
                    PlainUserResource resource = new PlainUserResource();
                    resource.setId(EncodingUtils.toUUIDOrNull(user.getId()));
                    Optional.ofNullable(user.getName())
                            .map(CertifiedField::getValue)
                            .ifPresent(resource::setName);
                    Optional.ofNullable(user.getFamilyName())
                            .map(CertifiedField::getValue)
                            .ifPresent(resource::setSurname);
                    return resource;
                }).orElse(null);
    }

    default UserGroupPlainResource toPlainGroupResource(UserGroupInfo model) {
        UserGroupPlainResource resource = null;
        if (model != null) {
            resource = new UserGroupPlainResource();
            resource.setId(model.getId());
            resource.setName(model.getName());
            resource.setDescription(model.getDescription());
            resource.setInstitutionId(model.getInstitutionId());
            resource.setProductId(model.getProductId());
            resource.setStatus(model.getStatus());
            resource.setCreatedBy(UUID.fromString(model.getCreatedBy().getId()));
            resource.setCreatedAt(model.getCreatedAt());
            if (model.getModifiedBy() != null) {
                resource.setModifiedBy(UUID.fromString(model.getModifiedBy().getId()));
                resource.setModifiedAt(model.getModifiedAt());
            }
            resource.setMembersCount(model.getMembers().size());
        }
        return resource;
    }
}