package it.pagopa.selfcare.user_group.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.selfcare.user_group.connector.api.UserGroupOperations;
import it.pagopa.selfcare.user_group.connector.exception.ResourceNotFoundException;
import it.pagopa.selfcare.user_group.connector.model.UserGroupFilter;
import it.pagopa.selfcare.user_group.connector.model.UserGroupStatus;
import it.pagopa.selfcare.user_group.core.UserGroupService;
import it.pagopa.selfcare.user_group.web.config.WebTestConfig;
import it.pagopa.selfcare.user_group.web.handler.UserGroupExceptionHandler;
import it.pagopa.selfcare.user_group.web.model.*;
import it.pagopa.selfcare.user_group.web.model.mapper.UserGroupMapperImpl;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static it.pagopa.selfcare.commons.utils.TestUtils.mockInstance;
import static java.util.UUID.randomUUID;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.data.support.PageableExecutionUtils.getPage;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(value = {UserGroupV1Controller.class}, excludeAutoConfiguration = SecurityAutoConfiguration.class)
@ContextConfiguration(classes = {
        UserGroupV1Controller.class,
        UserGroupExceptionHandler.class,
        WebTestConfig.class,
        UserGroupMapperImpl.class
})
class UserGroupV1ControllerTest {

    private static final DummyCreateUserGroupDto CREATE_USER_GROUP_DTO = mockInstance(new DummyCreateUserGroupDto());
    private static final DummyUpdateUserGroupDto UPDATE_USER_GROUP_DTO = mockInstance(new DummyUpdateUserGroupDto());
    private static final String BASE_URL = "/v1/user-groups";

    @MockBean
    private UserGroupService groupServiceMock;

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected ObjectMapper mapper;

    @Captor
    private ArgumentCaptor<Pageable> pageableCaptor;

    @Test
    void createGroup() throws Exception {
        //given
        when(groupServiceMock.createGroup(any(UserGroupOperations.class)))
                .thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0, UserGroupOperations.class));
        //when
        MvcResult result = mvc.perform(MockMvcRequestBuilders
                .post(BASE_URL)
                .content(mapper.writeValueAsString(CREATE_USER_GROUP_DTO))
                .contentType(APPLICATION_JSON_VALUE)
                .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isCreated())
                .andReturn();
        //then
        UserGroupResource group = mapper.readValue(result.getResponse().getContentAsString(), UserGroupResource.class);
        assertNotNull(group);
        assertEquals(CREATE_USER_GROUP_DTO.getDescription(), group.getDescription());
        assertEquals(CREATE_USER_GROUP_DTO.getName(), group.getName());
        assertEquals(CREATE_USER_GROUP_DTO.getInstitutionId(), group.getInstitutionId());
        assertEquals(CREATE_USER_GROUP_DTO.getProductId(), group.getProductId());
        assertEquals(CREATE_USER_GROUP_DTO.getStatus(), group.getStatus());
        assertEquals(CREATE_USER_GROUP_DTO.getMembers(), group.getMembers());
    }

    @Test
    void deleteGroup_doesNotExists() throws Exception {
        //given
        Mockito.doThrow(ResourceNotFoundException.class)
                .when(groupServiceMock).deleteGroup(Mockito.anyString());
        //when
        mvc.perform(MockMvcRequestBuilders
                .delete(BASE_URL + "/id")
                .contentType(APPLICATION_JSON_VALUE)
                .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(APPLICATION_PROBLEM_JSON))
                .andExpect(content().string(not(emptyString())));
        //then
    }

    @Test
    void deleteGroup() throws Exception {
        Mockito.doNothing()
                .when(groupServiceMock).deleteGroup(Mockito.anyString());
        // when
        MvcResult result = mvc.perform(MockMvcRequestBuilders
                .delete(BASE_URL + "/id")
                .contentType(APPLICATION_JSON_VALUE)
                .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().is2xxSuccessful())
                .andReturn();
        // then
        assertEquals("", result.getResponse().getContentAsString());
        verify(groupServiceMock, times(1))
                .deleteGroup(Mockito.anyString());
        Mockito.verifyNoMoreInteractions(groupServiceMock);

    }

    @Test
    void activateGroup_doesNotExists() throws Exception {
        //given
        String groupId = "groupId";
        Mockito.doThrow(ResourceNotFoundException.class)
                .when(groupServiceMock).activateGroup(Mockito.anyString());
        //when
        mvc.perform(MockMvcRequestBuilders
                .post(BASE_URL + "/{id}/activate", groupId)
                .contentType(APPLICATION_JSON_VALUE)
                .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(APPLICATION_PROBLEM_JSON))
                .andExpect(content().string(not(emptyString())));
        //then
    }

    @Test
    void activateGroup() throws Exception {
        //given
        String groupId = "groupId";
        //when
        MvcResult result = mvc.perform(MockMvcRequestBuilders
                .post(BASE_URL + "/{id}/activate", groupId)
                .contentType(APPLICATION_JSON_VALUE)
                .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isNoContent())
                .andReturn();
        //then
        assertEquals(0, result.getResponse().getContentLength());
        verify(groupServiceMock, times(1))
                .activateGroup(groupId);
        Mockito.verifyNoMoreInteractions(groupServiceMock);
    }

    @Test
    void suspendGroup_doesNotExists() throws Exception {
        //given
        String groupId = "groupId";
        Mockito.doThrow(ResourceNotFoundException.class)
                .when(groupServiceMock).suspendGroup(Mockito.anyString());
        //when
        mvc.perform(MockMvcRequestBuilders
                .post(BASE_URL + "/{id}/suspend", groupId)
                .contentType(APPLICATION_JSON_VALUE)
                .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(APPLICATION_PROBLEM_JSON))
                .andExpect(content().string(not(emptyString())));
        //then
    }

    @Test
    void suspendGroup() throws Exception {
        //given
        String groupId = "groupId";
        //when
        MvcResult result = mvc.perform(MockMvcRequestBuilders
                .post(BASE_URL + "/{id}/suspend", groupId)
                .contentType(APPLICATION_JSON_VALUE)
                .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isNoContent())
                .andReturn();
        //then
        assertEquals(0, result.getResponse().getContentLength());
        verify(groupServiceMock, times(1))
                .suspendGroup(groupId);
        Mockito.verifyNoMoreInteractions(groupServiceMock);
    }

    @Test
    void updateGroup_exists() throws Exception {
        //given
        when(groupServiceMock.updateGroup(Mockito.anyString(), any(UserGroupOperations.class)))
                .thenAnswer(invocationOnMock -> {
                    String id = invocationOnMock.getArgument(0, String.class);
                    UserGroupOperations group = invocationOnMock.getArgument(1, UserGroupOperations.class);
                    group.setId(id);
                    group.setMembers(Set.of(randomUUID().toString(), randomUUID().toString()));
                    return group;
                });
        //when
        MvcResult result = mvc.perform(MockMvcRequestBuilders
                .put(BASE_URL + "/id")
                .content(mapper.writeValueAsString(UPDATE_USER_GROUP_DTO))
                .contentType(APPLICATION_JSON_VALUE)
                .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().is2xxSuccessful())
                .andReturn();
        //then
        UserGroupResource group = mapper.readValue(result.getResponse().getContentAsString(), UserGroupResource.class);

        assertNotNull(group);
        assertEquals(UPDATE_USER_GROUP_DTO.getDescription(), group.getDescription());
        assertEquals(UPDATE_USER_GROUP_DTO.getName(), group.getName());
        assertNotEquals(UPDATE_USER_GROUP_DTO.getMembers().size(), group.getMembers().size());
    }

    @Test
    void addMember() throws Exception {
        //given
        String groupId = "groupId";
        MemberUUID member = new MemberUUID();
        member.setMember(randomUUID());
        //when
        MvcResult result = mvc.perform(MockMvcRequestBuilders
                .put(BASE_URL + "/groupId/members/" + member.getMember())
                .contentType(APPLICATION_JSON_VALUE)
                .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().is2xxSuccessful())
                .andReturn();
        //then
        assertEquals(0, result.getResponse().getContentLength());
        verify(groupServiceMock, times(1))
                .addMember(groupId, member.getMember());
        Mockito.verifyNoMoreInteractions(groupServiceMock);
    }

    @Test
    void deleteMember() throws Exception {
        //given
        String groupId = "groupId";
        UUID memberId = randomUUID();
        //when
        MvcResult result = mvc.perform(MockMvcRequestBuilders
                .delete(BASE_URL + "/groupId/members/" + memberId)
                .contentType(APPLICATION_JSON_VALUE)
                .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isNoContent())
                .andReturn();
        //then
        assertEquals(0, result.getResponse().getContentLength());
        verify(groupServiceMock, times(1))
                .deleteMember(groupId, memberId.toString());
        Mockito.verifyNoMoreInteractions(groupServiceMock);
    }

    @Test
    void getUserGroup() throws Exception {
        //given
        String InstitutionId = "institutionId";
        String productId = "productId";
        when(groupServiceMock.getUserGroup(Mockito.anyString()))
                .thenAnswer(invocationOnMock -> {
                    String id = invocationOnMock.getArgument(0, String.class);
                    UserGroupOperations group = mockInstance(new GroupDto(), "setId");
                    group.setId(id);
                    group.setMembers(Set.of(randomUUID().toString(), randomUUID().toString()));
                    return group;
                });
        //when
        MvcResult result = mvc.perform(MockMvcRequestBuilders
                .get(BASE_URL + "/groupId")
                .contentType(APPLICATION_JSON_VALUE)
                .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().is2xxSuccessful())
                .andReturn();
        //then
        UserGroupResource group = mapper.readValue(result.getResponse().getContentAsString(), UserGroupResource.class);
        assertNotNull(group);
    }

    @Test
    void getUserGroups() throws Exception {
        //given
        String institutionId = "institutionId";
        String productId = "productId";
        String userId = randomUUID().toString();
        UserGroupStatus allowedStatus = UserGroupStatus.ACTIVE;
        final int page = 1;
        final int size = 3;
        UserGroupOperations groupOperations = mockInstance(new GroupDto());
        groupOperations.setMembers(Set.of(randomUUID().toString()));
        when(groupServiceMock.getUserGroups(any(), any()))
                .thenAnswer(invocation -> {
                    final Pageable pageable = invocation.getArgument(1, Pageable.class);
                    return getPage(List.of(groupOperations), pageable, () -> pageable.isPaged()
                            ? (long) pageable.getPageSize() * pageable.getPageNumber() + 1
                            : 1);
                });
        //when
        mvc.perform(MockMvcRequestBuilders
                .get(BASE_URL + "/")
                .param("institutionId", institutionId)
                .param("productId", productId)
                .param("userId", userId)
                .param("status", String.valueOf(allowedStatus))
                .param("page", String.valueOf(page))
                .param("size", String.valueOf(size))
                .contentType(APPLICATION_JSON_VALUE)
                .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.number", notNullValue()))
                .andExpect(jsonPath("$.size", notNullValue()))
                .andExpect(jsonPath("$.totalElements", notNullValue()))
                .andExpect(jsonPath("$.totalPages", notNullValue()))
                .andExpect(jsonPath("$.content", notNullValue()))
                .andExpect(jsonPath("$.content[0].id", notNullValue()))
                .andExpect(jsonPath("$.content[0].institutionId", notNullValue()))
                .andExpect(jsonPath("$.content[0].productId", notNullValue()))
                .andExpect(jsonPath("$.content[0].name", notNullValue()))
                .andExpect(jsonPath("$.content[0].description", notNullValue()))
                .andExpect(jsonPath("$.content[0].status", notNullValue()))
                .andExpect(jsonPath("$.content[0].members", not(emptyArray())))
                .andExpect(jsonPath("$.content[0].createdAt", notNullValue()))
                .andExpect(jsonPath("$.content[0].createdBy", notNullValue()))
                .andExpect(jsonPath("$.content[0].modifiedAt", notNullValue()))
                .andExpect(jsonPath("$.content[0].modifiedBy", notNullValue()));
        //then
        ArgumentCaptor<UserGroupFilter> filterCaptor = ArgumentCaptor.forClass(UserGroupFilter.class);
        verify(groupServiceMock, times(1))
                .getUserGroups(filterCaptor.capture(), pageableCaptor.capture());
        UserGroupFilter capturedFilter = filterCaptor.getValue();
        assertEquals(capturedFilter.getProductId(), productId);
        assertEquals(capturedFilter.getInstitutionId(), institutionId);
        assertEquals(capturedFilter.getUserId(), userId);
        Pageable capturedPageable = pageableCaptor.getValue();
        assertTrue(capturedPageable.getSort().isUnsorted());
        assertEquals(page, capturedPageable.getPageNumber());
        assertEquals(size, capturedPageable.getPageSize());
    }

    @Test
    void deleteMembers() throws Exception {
        //given
        UUID memberId = randomUUID();
        String institutionId = "institutionId";
        String productId = "productId";
        //when
        MvcResult result = mvc.perform(MockMvcRequestBuilders
                .delete(BASE_URL + "/members/" + memberId)
                .param("institutionId", institutionId)
                .param("productId", productId)
                .contentType(APPLICATION_JSON_VALUE)
                .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isNoContent())
                .andReturn();
        //then
        assertEquals(0, result.getResponse().getContentLength());
        verify(groupServiceMock, times(1))
                .deleteMembers(memberId.toString(), institutionId, productId);
        Mockito.verifyNoMoreInteractions(groupServiceMock);
    }

}