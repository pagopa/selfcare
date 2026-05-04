package it.pagopa.selfcare.dashboard.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.pagopa.selfcare.commons.base.security.SelfCareUser;
import it.pagopa.selfcare.commons.web.model.Page;
import it.pagopa.selfcare.commons.web.model.mapper.PageMapper;
import it.pagopa.selfcare.dashboard.model.groups.CreateUserGroup;
import it.pagopa.selfcare.dashboard.model.groups.UserGroupInfo;
import it.pagopa.selfcare.dashboard.model.mapper.GroupMapper;
import it.pagopa.selfcare.dashboard.model.mapper.GroupMapperV2;
import it.pagopa.selfcare.dashboard.model.mapper.UserMapper;
import it.pagopa.selfcare.dashboard.model.user_groups.*;
import it.pagopa.selfcare.dashboard.service.UserGroupV2Service;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.owasp.encoder.Encode;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping(value = "/v2/user-groups", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Tag(name = "user-groups")
public class UserGroupV2Controller {

    private final UserGroupV2Service groupService;
    private final GroupMapper groupMapper;
    private final UserMapper userMapper;
    private final GroupMapperV2 groupMapperV2;

    @PostMapping(value = "/", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "createUserGroup", description = "${swagger.dashboard.user-group.api.createUserGroup}")
    @ApiResponse(responseCode = "409", description = "Conflict")
    @PreAuthorize("hasPermission(new it.pagopa.selfcare.dashboard.security.FilterAuthorityDomain(#group.getInstitutionId(), #group.getProductId(), null), 'Selc:ManageProductGroups')")
    public UserGroupIdResource createUserGroup(@RequestBody @Valid CreateUserGroupDto group) {
        log.trace("createGroup start");
        log.debug("createGroup group = {}", group);
        CreateUserGroup userGroup = groupMapperV2.fromDto(group);
        String groupId = groupService.createUserGroup(userGroup);
        UserGroupIdResource result = groupMapper.toIdResource(groupId);
        log.debug("createGroup result = {}", result);
        log.trace("createGroup end");
        return result;
    }

    @DeleteMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "deleteUserGroup", description = "${swagger.dashboard.user-group.api.deleteUserGroup}")
    @PreAuthorize("hasPermission(new it.pagopa.selfcare.dashboard.security.FilterAuthorityDomain(null, null, #id), 'Selc:ManageProductGroups')")
    public void deleteUserGroup(@Parameter(description = "${swagger.dashboard.user-group.model.id}")
                                @PathVariable("id") String id) {
        log.trace("deleteGroup start");
        log.debug("deleteGroup id = {}", Encode.forJava(id));
        groupService.delete(id);
        log.trace("deleteGroup end");

    }

    @PostMapping(value = "/{id}/activate", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "activateUserGroup", description = "${swagger.dashboard.user-group.api.activateUserGroup}")
    @PreAuthorize("hasPermission(new it.pagopa.selfcare.dashboard.security.FilterAuthorityDomain(null, null, #id), 'Selc:ManageProductGroups')")
    public void activateUserGroup(@Parameter(description = "${swagger.dashboard.user-group.model.id}")
                                  @PathVariable("id") String id) {
        log.trace("activateGroup start");
        log.debug("activateGroup id = {}", Encode.forJava(id));
        groupService.activate(id);
        log.trace("activateGroup end");
    }

    @PostMapping(value = "/{id}/suspend", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "suspendUserGroup", description = "${swagger.dashboard.user-group.api.suspendUserGroup}")
    @PreAuthorize("hasPermission(new it.pagopa.selfcare.dashboard.security.FilterAuthorityDomain(null, null, #id), 'Selc:ManageProductGroups')")
    public void suspendUserGroup(@Parameter(description = "${swagger.dashboard.user-group.model.id}")
                                 @PathVariable("id") String id) {
        log.trace("suspendGroup start");
        log.debug("suspendGroup id = {}", Encode.forJava(id));
        groupService.suspend(id);
        log.trace("suspendGroup end");
    }

    @PutMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "updateUserGroup", description = "${swagger.dashboard.user-group.api.updateUserGroup}")
    @ApiResponse(responseCode = "409", description = "Conflict")
    @PreAuthorize("hasPermission(new it.pagopa.selfcare.dashboard.security.FilterAuthorityDomain(null, null, #id), 'Selc:ManageProductGroups')")
    public void updateUserGroup(@Parameter(description = "${swagger.dashboard.user-group.model.id}")
                                @PathVariable("id") String id,
                                @RequestBody @Valid UpdateUserGroupDto groupDto) {
        log.trace("updateUserGroup start");
        log.debug("updateUserGroup id = {}, groupDto = {}", Encode.forJava(id), Encode.forJava(groupDto.toString()));
        groupService.updateUserGroup(id, groupMapperV2.fromDto(groupDto));
        log.debug("updateUserGroup result = {}", Encode.forJava(id));
        log.trace("updateUserGroup end");
    }

    @PostMapping(value = "/{id}/members/{userId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "addMemberToUserGroup", description = "${swagger.dashboard.user-group.api.addMember}")
    @PreAuthorize("hasPermission(new it.pagopa.selfcare.dashboard.security.FilterAuthorityDomain(null, null, #id), 'Selc:ManageProductGroups')")
    public void addMemberToUserGroup(@Parameter(description = "${swagger.dashboard.user-group.model.id}")
                                     @PathVariable("id") String id,
                                     @Parameter(description = "${swagger.dashboard.user.model.id}")
                                     @PathVariable("userId") UUID member) {
        log.trace("addMemberToUserGroup start");
        log.debug("addMemberToUserGroup id = {}", Encode.forJava(id));
        groupService.addMemberToUserGroup(id, member);
        log.trace("addMemberToUserGroup end");
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "getUserGroupById", description = "${swagger.dashboard.user-group.api.getUserGroup}")
    @PreAuthorize("hasPermission(new it.pagopa.selfcare.dashboard.security.FilterAuthorityDomain(null, null, #id), 'Selc:ListAllProductGroups')")
    public UserGroupResource getUserGroupById(@Parameter(description = "${swagger.dashboard.user-group.model.id}")
                                              @PathVariable("id") String id) {
        log.trace("getUserGroup start");
        log.debug("getUserGroup id = {}", Encode.forJava(id));
        UserGroupInfo groupInfo = groupService.getUserGroupById(id);
        UserGroupResource groupResource = groupMapper.toResource(groupInfo,userMapper);
        log.debug("getUserGroup result = {}", groupResource);
        log.trace("getUserGroup end");
        return groupResource;
    }

    @GetMapping(value = "/me/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "getMyUserGroupById", description = "${swagger.dashboard.user-group.api.getMyUserGroup}")
    @PreAuthorize("hasPermission(new it.pagopa.selfcare.dashboard.security.FilterAuthorityDomain(null, null, #id), 'Selc:ListProductGroups')")
    public UserGroupResource getMyUserGroupById(@Parameter(description = "${swagger.dashboard.user-group.model.id}")
                                                @PathVariable("id") String id,
                                                Authentication authentication) {
        log.trace("getMyUserGroupById start");
        SelfCareUser user = (SelfCareUser) authentication.getPrincipal();
        log.debug("getMyUserGroupById id = {}, memberId = {}", Encode.forJava(id), user.getId());
        UserGroupInfo groupInfo = groupService.getUserGroupById(id, user.getId());
        UserGroupResource groupResource = groupMapper.toResource(groupInfo,userMapper);
        log.debug("getMyUserGroupById result = {}", groupResource);
        log.trace("getMyUserGroupById end");
        return groupResource;
    }

    @GetMapping(value = "", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "getUserGroups", description = "${swagger.dashboard.user-group.api.getUserGroups}")
    @PreAuthorize("hasPermission(new it.pagopa.selfcare.dashboard.security.FilterAuthorityDomain(#institutionId, #productId, null), 'Selc:ListAllProductGroups')")
    public Page<UserGroupPlainResource> getUserGroups(@Parameter(description = "${swagger.dashboard.user-group.model.institutionId}")
                                                      @RequestParam(value = "institutionId") String institutionId,
                                                      @Parameter(description = "${swagger.dashboard.user-group.model.productId}")
                                                      @RequestParam(value = "productId") String productId,
                                                      @Parameter(description = "${swagger.dashboard.user.model.id}")
                                                      @RequestParam(value = "userId", required = false) UUID memberId,
                                                      @Parameter(hidden = true) Pageable pageable) {
        log.trace("getUserGroups start");
        log.debug("getUserGroups institutionId = {}, productId = {}, memberId= {}, pageable = {}", Encode.forJava(institutionId),
                Encode.forJava(productId), Encode.forJava(Optional.ofNullable(memberId).map(UUID::toString).orElse("")), Encode.forJava(pageable.toString()));
        Page<UserGroupPlainResource> groups = PageMapper.map(groupService.getUserGroups(institutionId, productId, memberId, pageable)
                .map(groupMapperV2::toPlainUserGroupResource));
        log.debug("getUserGroups result = {}", groups);
        log.trace("getUserGroups end");
        return groups;
    }

    @GetMapping(value = "/me", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "getMyUserGroups", description = "${swagger.dashboard.user-group.api.getMyUserGroups}")
    @PreAuthorize("hasPermission(new it.pagopa.selfcare.dashboard.security.FilterAuthorityDomain(#institutionId, #productId, null), 'Selc:ListProductGroups')")
    public Page<UserGroupPlainResource> getMyUserGroups(@Parameter(description = "${swagger.dashboard.user-group.model.institutionId}")
                                                        @RequestParam(value = "institutionId") String institutionId,
                                                        @Parameter(description = "${swagger.dashboard.user-group.model.productId}")
                                                        @RequestParam(value = "productId") String productId,
                                                        @Parameter(hidden = true) Pageable pageable,
                                                        Authentication authentication) {
        log.trace("getMyUserGroups start");
        SelfCareUser user = (SelfCareUser) authentication.getPrincipal();
        log.debug("getMyUserGroups institutionId = {}, productId = {}, memberId = {}, pageable = {}", Encode.forJava(institutionId), Encode.forJava(productId), Encode.forJava(user.getId()), Encode.forJava(pageable.toString()));
        Page<UserGroupPlainResource> groups = PageMapper.map(groupService.getUserGroups(institutionId, productId, UUID.fromString(user.getId()), pageable)
                .map(groupMapperV2::toPlainUserGroupResource));
        log.debug("getMyUserGroups result = {}", groups);
        log.trace("getMyUserGroups end");
        return groups;
    }

    @DeleteMapping(value = "/{userGroupId}/members/{userId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "deleteMemberFromUserGroup", description = "${swagger.dashboard.user-group.api.deleteMember}")
    @PreAuthorize("hasPermission(new it.pagopa.selfcare.dashboard.security.FilterAuthorityDomain(null, null, #userGroupId), 'Selc:ManageProductGroups')")
    public void deleteMemberFromUserGroup(@Parameter(description = "${swagger.dashboard.user-group.model.id}")
                                          @PathVariable("userGroupId") String userGroupId,
                                          @Parameter(description = "${swagger.dashboard.user.model.id}")
                                          @PathVariable("userId") UUID memberId) {
        log.trace("deleteMemberFromUserGroup start");
        log.debug("deleteMemberFromUserGroup userGroupId = {}, memberId = {}", Encode.forJava(userGroupId), Encode.forJava(memberId.toString()));
        groupService.deleteMemberFromUserGroup(userGroupId, memberId);
        log.trace("deleteMemberFromUserGroup end");
    }

}
