package it.pagopa.selfcare.user_group.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.Explode;
import io.swagger.v3.oas.annotations.enums.ParameterStyle;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.pagopa.selfcare.commons.web.model.Page;
import it.pagopa.selfcare.commons.web.model.Problem;
import it.pagopa.selfcare.commons.web.model.mapper.PageMapper;
import it.pagopa.selfcare.user_group.api.UserGroupOperations;
import it.pagopa.selfcare.user_group.model.*;
import it.pagopa.selfcare.user_group.model.mapper.UserGroupMapper;
import it.pagopa.selfcare.user_group.service.UserGroupService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.owasp.encoder.Encode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping(value = "/v1/user-groups", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "UserGroup")
public class UserGroupV1Controller {

    private final UserGroupService groupService;
    private final UserGroupMapper userGroupMapper;

    @Autowired
    public UserGroupV1Controller(UserGroupService groupService,
                                 UserGroupMapper userGroupMapper) {
        this.groupService = groupService;
        this.userGroupMapper = userGroupMapper;
    }
    
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "createGroup", description = "${swagger.user-group.groups.api.createUserGroup}")
    @ApiResponse(responseCode = "409",
            description = "Conflict",
            content = {
                    @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = Problem.class))
            })
    public UserGroupResource createGroup(@RequestBody @Valid CreateUserGroupDto group) {
        log.trace("createGroup start");
        log.debug("createGroup group = {}", Encode.forJava(group.toString()));
        UserGroupOperations groupOperations = groupService.createGroup(userGroupMapper.fromDto(group));
        UserGroupResource result = userGroupMapper.toResource(groupOperations);
        log.debug("createGroup result = {}", result);
        log.trace("createGroup end");
        return result;
    }


    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "deleteGroup", description = "${swagger.user-group.groups.api.deleteUserGroup}")
    public void deleteGroup(@Parameter(description = "${swagger.user-group.model.id}")
                            @PathVariable("id") String id) {
        log.trace("deteleGroup start");
        log.debug("deleteGroup id = {}", Encode.forJava(id));
        groupService.deleteGroup(id);
        log.trace("deteleGroup end");

    }


    @PostMapping("/{id}/activate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "activateGroup", description = "${swagger.user-group.groups.api.activateUserGroup}")
    public void activateGroup(@Parameter(description = "${swagger.user-group.model.id}")
                              @PathVariable("id") String id) {
        log.trace("activateGroup start");
        log.debug("activateGroup id = {}", Encode.forJava(id));
        groupService.activateGroup(id);
        log.trace("activateGroup end");
    }


    @PostMapping("/{id}/suspend")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "suspendGroup", description = "${swagger.user-group.groups.api.suspendUserGroup}")
    public void suspendGroup(@Parameter(description = "${swagger.user-group.model.id}")
                             @PathVariable("id") String id) {
        log.trace("suspendGroup start");
        log.debug("suspendGroup id = {}", Encode.forJava(id));
        groupService.suspendGroup(id);
        log.trace("suspendGroup end");
    }


    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "updateUserGroup", description = "${swagger.user-group.groups.api.updateUserGroup}")
    @ApiResponse(responseCode = "409",
            description = "Conflict",
            content = {
                    @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = Problem.class))
            })
    public UserGroupResource updateUserGroup(@Parameter(description = "${swagger.user-group.model.id}")
                                             @PathVariable("id") String id,
                                             @RequestBody @Valid UpdateUserGroupDto groupDto) {
        log.trace("updateUserGroup start");
        log.debug("updateUserGroup id = {}", Encode.forJava(id));
        UserGroupOperations updatedGroup = groupService.updateGroup(id, userGroupMapper.toUserGroupOperations(groupDto));
        UserGroupResource result = userGroupMapper.toResource(updatedGroup);
        log.debug("updateUserGroup result = {}", result);
        log.trace("updateUserGroup end");
        return result;
    }


    @PutMapping(value = "/{id}/members/{memberId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "addMemberToUserGroup", description = "${swagger.user-group.groups.api.addMember}")
    public void addMemberToUserGroup(@Parameter(description = "${swagger.user-group.model.id}")
                                     @PathVariable("id") String id,
                                     @Parameter(description = "${swagger.user-group.model.memberId}")
                                     @PathVariable("memberId") UUID userId) {
        log.trace("addMemberToUserGroup start");
        log.debug("addMemberToUserGroup id = {}", Encode.forJava(id));
        groupService.addMember(id, userId);
        log.trace("addMemberToUserGroup end");
    }


    @Tag(name = "UserGroup")
    @Tag(name = "external-v2")
    @GetMapping(value = "/{id}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "getUserGroup", description = "${swagger.user-group.groups.api.getUserGroup}")
    public UserGroupResource getUserGroup(@Parameter(description = "${swagger.user-group.model.id}")
                                          @PathVariable("id") String id) {
        log.trace("getUserGroup start");
        log.debug("getUserGroup id = {}", Encode.forJava(id));
        UserGroupOperations group = groupService.getUserGroup(id);
        UserGroupResource groupResource = userGroupMapper.toResource(group);
        log.debug("getUserGroup result = {}", groupResource);
        log.trace("getUserGroup end");
        return groupResource;
    }


    @Tag(name = "support")
    @Tag(name = "external-v2")
    @Tag(name = "UserGroup")
    @Tag(name = "support-pnpg")
    @Tag(name = "external-pnpg")
    @GetMapping()
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "getUserGroups", description = "${swagger.user-group.groups.api.getUserGroups}")
    public PageOfUserGroupResource getUserGroups(@Parameter(description = "${swagger.user-group.model.institutionId}")
                                                 @RequestParam(value = "institutionId", required = false) String institutionId,
                                                 @Parameter(description = "${swagger.user-group.model.productId}")
                                                 @RequestParam(value = "productId", required = false) String productId,
                                                 @Parameter(description = "${swagger.user-group.model.memberId}")
                                                 @RequestParam(value = "userId", required = false) UUID memberId,
                                                 @Parameter(description = "${swagger.user-group.model.statusFilter}")
                                                 @RequestParam(value = "status", required = false) List<UserGroupStatus> status,
                                                 @Parameter(description = "${swagger.user-group.model.parentInstitutionId}")
                                                 @RequestParam(value = "parentInstitutionId", required = false) String parentInstitutionId,
                                                 @Parameter(description = "${swagger.pageable.page}", allowReserved = true)
                                                 @RequestParam(value = "page", required = false) Integer page,
                                                 @Parameter(description = "${swagger.pageable.size}", allowReserved = true)
                                                 @RequestParam(value = "size", required = false) Integer size,
                                                 @Parameter(
                                                         description = "${swagger.pageable.sort}",
                                                         allowReserved = true,
                                                         style = ParameterStyle.FORM,
                                                         explode = Explode.DEFAULT,
                                                         array = @ArraySchema(schema = @Schema(type = "array", implementation = String.class))
                                                 )
                                                 @RequestParam(value = "sort", required = false) String sort) {
        log.trace("getUserGroups start");
        final Pageable pageable = PageRequest.of(page != null ? page : 0, size != null ? size : 20, sort != null ? userGroupMapper.toSort(sort) : Sort.unsorted());
        log.debug("getUserGroups institutionId = {}, productId = {}, pageable = {}, status = {}", Encode.forJava(institutionId), Encode.forJava(productId), pageable, Encode.forJava(Objects.toString(status)));
        UserGroupFilter filter = new UserGroupFilter(institutionId, productId, memberId, status, parentInstitutionId);
        Page<UserGroupResource> result = PageMapper.map(groupService.getUserGroups(filter, pageable)
                .map(userGroupMapper::toResource));
        log.debug("getUserGroups result = {}", result);
        log.trace("getUserGroups end");
        return new PageOfUserGroupResource(result);
    }

    @DeleteMapping(value = "/{id}/members/{memberId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "deleteMemberFromUserGroup", description = "${swagger.user-group.groups.api.deleteMember}")
    @Tag(name = "UserGroup")
    @Tag(name = "support")
    public void deleteMemberFromUserGroup(@Parameter(description = "${swagger.user-group.model.id}")
                                          @PathVariable("id") String userGroupId,
                                          @Parameter(description = "${swagger.user-group.model.memberId}")
                                          @PathVariable("memberId") UUID memberId) {
        log.trace("deleteMemberFromUserGroup start");
        log.debug("deleteMemberFromUserGroup userGroupId = {}, memberId = {}", Encode.forJava(userGroupId), memberId);
        groupService.deleteMember(userGroupId, memberId.toString());
        log.trace("deleteMemberFromUserGroup end");
    }

    @DeleteMapping(value = "/members/{memberId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "deleteMemberFromUserGroups", description = "${swagger.user-group.groups.api.deleteMembers}")
    @Tag(name = "UserGroup")
    @Tag(name = "support")
    public void deleteMemberFromUserGroups(@Parameter(description = "${swagger.user-group.model.memberId}")
                                           @PathVariable("memberId") UUID memberId,
                                           @Parameter(description = "${swagger.user-group.model.institutionId}")
                                           @RequestParam(value = "institutionId") String institutionId,
                                           @Parameter(description = "${swagger.user-group.model.productId}")
                                           @RequestParam(value = "productId") String productId) {
        log.trace("deleteMemberFromUserGroups start");
        log.debug("deleteMemberFromUserGroups memberId = {}, institutionId = {}, productId = {}", memberId, Encode.forJava(institutionId), Encode.forJava(productId));
        groupService.deleteMembers(memberId.toString(), institutionId, productId);
        log.trace("deleteMemberFromUserGroups end");
    }


    @PutMapping(value = "/members", produces = MediaType.ALL_VALUE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Tag(name = "UserGroup")
    @Tag(name = "internal-v1")
    @Operation(summary = "addMembersToUserGroupWithParentInstitutionId", description = "${swagger.user-group.groups.api.addMembers}")
    public void addMembersToUserGroupWithParentInstitutionId(@RequestBody @Valid AddMembersToUserGroupDto groupDto) {
        log.trace("addMembersToUserGroup start");
        log.debug("addMembersToUserGroup institutionId = {}, parentInstitutionId = {}, productId = {}",
                Encode.forJava(groupDto.getInstitutionId()), Encode.forJava(groupDto.getParentInstitutionId()), Encode.forJava(groupDto.getProductId()));
        groupService.createGroupOrAddMembers(userGroupMapper.toUserGroupOperations(groupDto));
        log.trace("addMemberToUserGroup end");
    }

    @DeleteMapping(value = "/members", produces = MediaType.ALL_VALUE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Tag(name = "UserGroup")
    @Tag(name = "internal-v1")
    @Operation(summary = "deleteMembersFromUserGroupWithParentInstitutionId", description = "${swagger.user-group.groups.api.deleteMembersWithParentInstitutionId}")
    public void deleteMembersFromUserGroupWithParentInstitutionId(@RequestBody @Valid DeleteMembersFromUserGroupDto deleteMembersFromUserGroupDto) {
        log.trace("deleteMembersFromUserGroupWithParentInstitutionId start");
        log.debug("deleteMembersFromUserGroupWithParentInstitutionId institutionId = {}, parentInstitutionId = {}, productId = {}",
                Encode.forJava(deleteMembersFromUserGroupDto.getInstitutionId()), Encode.forJava(deleteMembersFromUserGroupDto.getParentInstitutionId()), Encode.forJava(deleteMembersFromUserGroupDto.getProductId()));
        groupService.deleteMembersWithParentInstitutionId(deleteMembersFromUserGroupDto.getInstitutionId(), deleteMembersFromUserGroupDto.getParentInstitutionId(), deleteMembersFromUserGroupDto.getProductId(), deleteMembersFromUserGroupDto.getMembers());
        log.trace("deleteMembersFromUserGroupWithParentInstitutionId end");
    }

}
