package it.pagopa.selfcare.dashboard.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.selfcare.commons.base.security.SelfCareUser;
import it.pagopa.selfcare.commons.utils.TestUtils;
import it.pagopa.selfcare.commons.web.model.Page;
import it.pagopa.selfcare.dashboard.model.groups.CreateUserGroup;
import it.pagopa.selfcare.dashboard.model.groups.UserGroup;
import it.pagopa.selfcare.dashboard.model.groups.UserGroupInfo;
import it.pagopa.selfcare.dashboard.model.mapper.GroupMapperImpl;
import it.pagopa.selfcare.dashboard.model.mapper.GroupMapperV2Impl;
import it.pagopa.selfcare.dashboard.model.mapper.UserMapperImpl;
import it.pagopa.selfcare.dashboard.model.user_groups.CreateUserGroupDto;
import it.pagopa.selfcare.dashboard.model.user_groups.UpdateUserGroupDto;
import it.pagopa.selfcare.dashboard.model.user_groups.UserGroupIdResource;
import it.pagopa.selfcare.dashboard.model.user_groups.UserGroupPlainResource;
import it.pagopa.selfcare.dashboard.service.UserGroupV2Service;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;
import static org.springframework.data.support.PageableExecutionUtils.getPage;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserGroupV2ControllerTest extends BaseControllerTest {
    private static final String BASE_URL = "/v2/user-groups";

    private static final String FILE_JSON_PATH = "src/test/resources/json/";

    @InjectMocks
    private UserGroupV2Controller userGroupV2Controller;

    @Mock
    private UserGroupV2Service groupServiceMock;

    @Spy
    private GroupMapperV2Impl groupMapperV2;

    @Spy
    private GroupMapperImpl groupMapper;

    @Spy
    private UserMapperImpl userMapper;

    @BeforeEach
    void setUp() {
        super.setUp(userGroupV2Controller);
    }

    @Test
    void createGroup() throws Exception {
        // given
        Authentication authentication = mock(Authentication.class);

        byte[] createUserGroupDtoStream = Files.readAllBytes(Paths.get(FILE_JSON_PATH + "CreateUserGroupDto.json"));
        CreateUserGroupDto createUserGroupDto = objectMapper.readValue(createUserGroupDtoStream, new TypeReference<>() {
        });

        byte[] createUserGroupStream = Files.readAllBytes(Paths.get(FILE_JSON_PATH + "CreateUserGroup.json"));
        CreateUserGroup createUserGroup = objectMapper.readValue(createUserGroupStream, new TypeReference<>() {
        });

        String groupId = "groupId";
        when(groupServiceMock.createUserGroup(createUserGroup)).thenReturn(groupId);

        // when
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders
                        .post(BASE_URL + "/")
                        .principal(authentication)
                        .content(objectMapper.writeValueAsString(createUserGroupDto))
                        .contentType(APPLICATION_JSON_VALUE)
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isCreated())
                .andReturn();

        // then
        UserGroupIdResource resource = objectMapper.readValue(result.getResponse().getContentAsString(), UserGroupIdResource.class);
        assertNotNull(resource);
        assertEquals(groupId, resource.getId());
        verify(groupServiceMock, times(1)).createUserGroup(Mockito.notNull());
        verifyNoMoreInteractions(groupServiceMock);
    }

    @Test
    void createGroup_EmptyObject() throws Exception {
        // given
        Authentication authentication = mock(Authentication.class);

        byte[] createUserGroupDtoStream = Files.readAllBytes(Paths.get(FILE_JSON_PATH + "CreateUserGroupDto.json"));
        CreateUserGroupDto createUserGroupDto = objectMapper.readValue(createUserGroupDtoStream, new TypeReference<>() {
        });

        byte[] createUserGroupStream = Files.readAllBytes(Paths.get(FILE_JSON_PATH + "CreateUserGroup.json"));
        CreateUserGroup createUserGroup = objectMapper.readValue(createUserGroupStream, new TypeReference<>() {
        });

        // when
        when(groupServiceMock.createUserGroup(createUserGroup)).thenReturn(null);

        mockMvc.perform(MockMvcRequestBuilders
                        .post(BASE_URL + "/")
                        .principal(authentication)
                        .content(objectMapper.writeValueAsString(createUserGroupDto))
                        .contentType(APPLICATION_JSON_VALUE)
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isCreated())
                .andExpect(content().string(""));
        verify(groupServiceMock, times(1)).createUserGroup(Mockito.notNull());
        verifyNoMoreInteractions(groupServiceMock);
    }

    @Test
    void deleteGroup() throws Exception {
        // given
        String groupId = "groupId";

        // when
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders
                        .delete(BASE_URL + "/" + groupId)
                        .contentType(APPLICATION_JSON_VALUE)
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isNoContent())
                .andReturn();

        // then
        assertEquals(0, result.getResponse().getContentLength());
        verify(groupServiceMock, times(1)).delete(groupId);
        verifyNoMoreInteractions(groupServiceMock);
    }

    @Test
    void deleteGroup_EmptyObject() throws Exception {
        // given
        String groupId = "groupId";

        // when
        doNothing().when(groupServiceMock).delete(groupId);

        mockMvc.perform(MockMvcRequestBuilders
                        .delete(BASE_URL + "/" + groupId)
                        .contentType(APPLICATION_JSON_VALUE)
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
        verify(groupServiceMock, times(1)).delete(groupId);
        verifyNoMoreInteractions(groupServiceMock);
    }

    @Test
    void activateUserGroup() throws Exception {
        // given
        String groupId = "groupId";

        // when
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders
                        .post(BASE_URL + "/" + groupId + "/activate")
                        .contentType(APPLICATION_JSON_VALUE)
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isNoContent())
                .andReturn();

        // then
        assertEquals(0, result.getResponse().getContentLength());
        verify(groupServiceMock, times(1)).activate(groupId);
        verifyNoMoreInteractions(groupServiceMock);
    }

    @Test
    void activateUserGroup_EmptyObject() throws Exception {
        // given
        String groupId = "groupId";

        // when
        doNothing().when(groupServiceMock).activate(groupId);

        mockMvc.perform(MockMvcRequestBuilders
                        .post(BASE_URL + "/" + groupId + "/activate")
                        .contentType(APPLICATION_JSON_VALUE)
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
        verify(groupServiceMock, times(1)).activate(groupId);
        verifyNoMoreInteractions(groupServiceMock);
    }

    @Test
    void suspendUserGroup() throws Exception {
        // given
        String groupId = "groupId";

        // when
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders
                        .post(BASE_URL + "/" + groupId + "/suspend")
                        .contentType(APPLICATION_JSON_VALUE)
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isNoContent())
                .andReturn();

        // then
        assertEquals(0, result.getResponse().getContentLength());
        verify(groupServiceMock, times(1)).suspend(groupId);
        verifyNoMoreInteractions(groupServiceMock);
    }

    @Test
    void suspendUserGroup_EmptyObject() throws Exception {
        // given
        String groupId = "groupId";

        // when
        doNothing().when(groupServiceMock).suspend(groupId);

        mockMvc.perform(MockMvcRequestBuilders
                        .post(BASE_URL + "/" + groupId + "/suspend")
                        .contentType(APPLICATION_JSON_VALUE)
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
        verify(groupServiceMock, times(1)).suspend(groupId);
        verifyNoMoreInteractions(groupServiceMock);
    }

    @Test
    void updateUserGroup() throws Exception {
        // given
        String groupId = "groupId";
        UpdateUserGroupDto groupDto = TestUtils.mockInstance(new UpdateUserGroupDto());
        Set<UUID> mockMembers = Set.of(UUID.randomUUID());
        groupDto.setMembers(mockMembers);

        // when
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders
                        .put(BASE_URL + "/" + groupId)
                        .content(objectMapper.writeValueAsString(groupDto))
                        .contentType(APPLICATION_JSON_VALUE)
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isNoContent())
                .andReturn();

        // then
        assertEquals(0, result.getResponse().getContentLength());
        verify(groupServiceMock, times(1)).updateUserGroup(eq(groupId), Mockito.notNull());
        verifyNoMoreInteractions(groupServiceMock);
    }

    @Test
    void updateUserGroup_EmptyObject() throws Exception {
        // given
        String groupId = "groupId";
        UpdateUserGroupDto groupDto = TestUtils.mockInstance(new UpdateUserGroupDto());
        Set<UUID> mockMembers = Set.of(UUID.randomUUID());
        groupDto.setMembers(mockMembers);

        // when
        doNothing().when(groupServiceMock).updateUserGroup(eq(groupId), Mockito.notNull());

        mockMvc.perform(MockMvcRequestBuilders
                        .put(BASE_URL + "/" + groupId)
                        .content(objectMapper.writeValueAsString(groupDto))
                        .contentType(APPLICATION_JSON_VALUE)
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
        verify(groupServiceMock, times(1)).updateUserGroup(eq(groupId), Mockito.notNull());
        verifyNoMoreInteractions(groupServiceMock);
    }

    @Test
    void getUserGroup() throws Exception {
        // given
        String groupId = "groupId";

        ClassPathResource resource = new ClassPathResource("json/UserGroupInfo.json");
        byte[] userGroupInfoStream;
        try {
            userGroupInfoStream = Files.readAllBytes(resource.getFile().toPath());
        } catch (IOException e) {
            throw new RuntimeException("Failed to read resource file", e);
        }
        UserGroupInfo userGroupInfo = new ObjectMapper().readValue(userGroupInfoStream, UserGroupInfo.class);

        when(groupServiceMock.getUserGroupById(groupId)).thenReturn(userGroupInfo);

        ClassPathResource userGroupResource = new ClassPathResource("json/UserGroupResource.json");
        byte[] userGroupByte = Files.readAllBytes(userGroupResource.getFile().toPath());

        // when
        mockMvc.perform(MockMvcRequestBuilders
                        .get(BASE_URL + "/" + groupId)
                        .contentType(APPLICATION_JSON_VALUE)
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(content().json(new String(userGroupByte)));

        // then
        verify(groupServiceMock, times(1)).getUserGroupById(groupId);
        verifyNoMoreInteractions(groupServiceMock);
    }

    @Test
    void getUserGroupWithoutMailUuid() throws Exception {
        // given
        String groupId = "groupId";

        ClassPathResource resource = new ClassPathResource("json/UserGroupInfoWithoutMailUuid.json");
        byte[] userGroupInfoStream;
        try {
            userGroupInfoStream = Files.readAllBytes(resource.getFile().toPath());
        } catch (IOException e) {
            throw new RuntimeException("Failed to read resource file", e);
        }
        UserGroupInfo userGroupInfo = new ObjectMapper().readValue(userGroupInfoStream, UserGroupInfo.class);

        when(groupServiceMock.getUserGroupById(groupId)).thenReturn(userGroupInfo);

        ClassPathResource userGroupResource = new ClassPathResource("json/UserGroupResource.json");
        byte[] userGroupByte = Files.readAllBytes(userGroupResource.getFile().toPath());

        // when
        mockMvc.perform(MockMvcRequestBuilders
                        .get(BASE_URL + "/" + groupId)
                        .contentType(APPLICATION_JSON_VALUE)
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(content().json(new String(userGroupByte)));

        // then
        verify(groupServiceMock, times(1)).getUserGroupById(groupId);
        verifyNoMoreInteractions(groupServiceMock);
    }

    @Test
    void getMyUserGroup() throws Exception {
        // given
        String groupId = "groupId";
        String userId = "userId";
        final SelfCareUser user = SelfCareUser.builder(userId).build();
        final Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(user);

        ClassPathResource resource = new ClassPathResource("json/UserGroupInfo.json");
        byte[] userGroupInfoStream;
        try {
            userGroupInfoStream = Files.readAllBytes(resource.getFile().toPath());
        } catch (IOException e) {
            throw new RuntimeException("Failed to read resource file", e);
        }
        UserGroupInfo userGroupInfo = new ObjectMapper().readValue(userGroupInfoStream, UserGroupInfo.class);

        when(groupServiceMock.getUserGroupById(groupId, userId)).thenReturn(userGroupInfo);

        ClassPathResource userGroupResource = new ClassPathResource("json/UserGroupResource.json");
        byte[] userGroupByte = Files.readAllBytes(userGroupResource.getFile().toPath());

        // when
        mockMvc.perform(MockMvcRequestBuilders
                        .get(BASE_URL + "/me/" + groupId)
                        .principal(authentication)
                        .contentType(APPLICATION_JSON_VALUE)
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(content().json(new String(userGroupByte)));

        // then
        verify(groupServiceMock, times(1)).getUserGroupById(groupId, userId);
        verifyNoMoreInteractions(groupServiceMock);
    }

    @Test
    void addMemberToUserGroup() throws Exception {
        // given
        String groupId = "groupId";
        UUID memberId = UUID.randomUUID();

        // when
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders
                        .post(BASE_URL + "/" + groupId + "/members/" + memberId)
                        .contentType(APPLICATION_JSON_VALUE)
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isNoContent())
                .andReturn();

        // then
        assertEquals(0, result.getResponse().getContentLength());
        verify(groupServiceMock, times(1)).addMemberToUserGroup(groupId, memberId);
        verifyNoMoreInteractions(groupServiceMock);
    }

    @Test
    void addMemberToUserGroup_EmptyObject() throws Exception {
        // given
        String groupId = "groupId";
        UUID memberId = UUID.randomUUID();

        // when
        doNothing().when(groupServiceMock).addMemberToUserGroup(groupId, memberId);

        mockMvc.perform(MockMvcRequestBuilders
                        .post(BASE_URL + "/" + groupId + "/members/" + memberId)
                        .contentType(APPLICATION_JSON_VALUE)
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
        verify(groupServiceMock, times(1)).addMemberToUserGroup(groupId, memberId);
        verifyNoMoreInteractions(groupServiceMock);
    }

    @Test
    void deleteMemberFromUserGroup() throws Exception {
        // given
        String groupId = "groupId";
        UUID memberId = UUID.randomUUID();

        // when
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders
                        .delete(BASE_URL + "/" + groupId + "/members/" + memberId)
                        .contentType(APPLICATION_JSON_VALUE)
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isNoContent())
                .andReturn();

        // then
        assertEquals(0, result.getResponse().getContentLength());
        verify(groupServiceMock, times(1)).deleteMemberFromUserGroup(groupId, memberId);
        verifyNoMoreInteractions(groupServiceMock);
    }

    @Test
    void deleteMemberFromUserGroup_EmptyObject() throws Exception {
        // given
        String groupId = "groupId";
        UUID memberId = UUID.randomUUID();

        // when
        doNothing().when(groupServiceMock).deleteMemberFromUserGroup(groupId, memberId);

        mockMvc.perform(MockMvcRequestBuilders
                        .delete(BASE_URL + "/" + groupId + "/members/" + memberId)
                        .contentType(APPLICATION_JSON_VALUE)
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
        verify(groupServiceMock, times(1)).deleteMemberFromUserGroup(groupId, memberId);
        verifyNoMoreInteractions(groupServiceMock);
    }

    @Test
    void getUserGroups() throws Exception {
        String instId = "institutionId";
        String productId = "prod-io";
        UUID userId = UUID.randomUUID();
        // given
        byte[] userGroupStream = Files.readAllBytes(Paths.get(FILE_JSON_PATH + "UserGroup.json"));
        UserGroup userGroup = objectMapper.readValue(userGroupStream, new TypeReference<>() {
        });

        when(groupServiceMock.getUserGroups(eq(instId), eq(productId), eq(userId), any(Pageable.class)))
                .thenAnswer(invocation -> getPage(List.of(userGroup), invocation.getArgument(3, Pageable.class), () -> 1L));

        Pageable pageable = mock(Pageable.class);
        Page<UserGroupPlainResource> pages = userGroupV2Controller.getUserGroups(instId, productId, userId, pageable);

        // then
        Assertions.assertEquals(1, pages.getTotalPages());
        Assertions.assertEquals(1, pages.getContent().size());
        verifyNoMoreInteractions(groupServiceMock);
    }

    @Test
    void getUserGroups_EmptyObject() {
        String instId = "institutionId";
        String productId = "prod-io";
        UUID userId = UUID.randomUUID();
        Pageable pageable = mock(Pageable.class);

        // given
        when(groupServiceMock.getUserGroups(eq(instId), eq(productId), eq(userId), any(Pageable.class)))
                .thenAnswer(invocation -> getPage(Collections.emptyList(), invocation.getArgument(3, Pageable.class), () -> 0L));

        Page<UserGroupPlainResource> pages = userGroupV2Controller.getUserGroups(instId, productId, userId, pageable);

        // when
        Assertions.assertEquals(1, pages.getTotalPages());
        Assertions.assertEquals(Collections.emptyList(), pages.getContent());
        verifyNoMoreInteractions(groupServiceMock);
    }

    @Test
    void getMyUserGroups() throws Exception {
        String instId = "institutionId";
        String productId = "prod-io";
        UUID userId = UUID.randomUUID();
        Pageable pageable = mock(Pageable.class);

        final SelfCareUser user = SelfCareUser.builder(userId.toString()).build();
        final Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(user);

        byte[] userGroupStream = Files.readAllBytes(Paths.get(FILE_JSON_PATH + "UserGroup.json"));
        UserGroup userGroup = objectMapper.readValue(userGroupStream, new TypeReference<>() {});
        when(groupServiceMock.getUserGroups(eq(instId), eq(productId), eq(userId), any(Pageable.class)))
                .thenAnswer(invocation -> getPage(List.of(userGroup), invocation.getArgument(3, Pageable.class), () -> 1L));

        Page<UserGroupPlainResource> pages = userGroupV2Controller.getMyUserGroups(instId, productId, pageable, authentication);
        Assertions.assertEquals(1, pages.getTotalPages());
        Assertions.assertEquals(1, pages.getContent().size());
        verifyNoMoreInteractions(groupServiceMock);
    }

}
