package it.pagopa.selfcare.dashboard.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import it.pagopa.selfcare.commons.base.security.SelfCareUser;
import it.pagopa.selfcare.dashboard.model.SearchUserDto;
import it.pagopa.selfcare.dashboard.model.UpdateUserDto;
import it.pagopa.selfcare.dashboard.model.mapper.UserMapperImpl;
import it.pagopa.selfcare.dashboard.model.mapper.UserMapperV2Impl;
import it.pagopa.selfcare.dashboard.model.user.*;
import it.pagopa.selfcare.dashboard.service.UserV2Service;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.hamcrest.Matchers.emptyString;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserV2ControllerTest extends BaseControllerTest {

    @InjectMocks
    protected UserV2Controller userV2Controller;

    @Mock
    private UserV2Service userServiceMock;

    @Spy
    private UserMapperV2Impl userMapper;

    @Spy
    private UserMapperImpl userMapperImpl;

    private static final String BASE_URL = "/v2/users";

    private static final String FILE_JSON_PATH = "src/test/resources/json/";

    @BeforeEach
    void setUp() {
        super.setUp(userV2Controller);
    }

    @Test
    void suspendRelationship() throws Exception {
        // given
        String userId = "rel1";
        String institutionid = "id1";
        String productId = "prod-pagopa";
        String productRole = "admin";

        // when
        mockMvc.perform(MockMvcRequestBuilders
                        .post(BASE_URL + "/{userId}/suspend", userId)
                        .queryParam("institutionId", institutionid)
                        .queryParam("productId", productId)
                        .queryParam("productRole", productRole)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.status().isNoContent())
                .andReturn();
        // then
        Mockito.verify(userServiceMock, Mockito.times(1))
                .suspendUserProduct(userId, institutionid, productId, productRole);
        Mockito.verifyNoMoreInteractions(userServiceMock);
    }


    @Test
    void activateRelationship() throws Exception {
        // given
        String userId = "rel1";
        String institutionid = "id1";
        String productId = "prod-pagopa";
        String productRole = "admin";

        // when
        mockMvc.perform(MockMvcRequestBuilders
                        .post(BASE_URL + "/{userId}/activate", userId)
                        .queryParam("institutionId", institutionid)
                        .queryParam("productId", productId)
                        .queryParam("productRole", productRole)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.status().isNoContent())
                .andReturn();
        // then
        Mockito.verify(userServiceMock, Mockito.times(1))
                .activateUserProduct(userId, institutionid, productId, productRole);
        Mockito.verifyNoMoreInteractions(userServiceMock);
    }

    @Test
    void deleteRelationship() throws Exception {
        // given
        String userId = "rel1";
        String institutionid = "id1";
        String productId = "prod-pagopa";
        String productRole = "admin";

        // when
        mockMvc.perform(MockMvcRequestBuilders
                        .delete(BASE_URL + "/{userId}", userId)
                        .queryParam("institutionId", institutionid)
                        .queryParam("productId", productId)
                        .queryParam("productRole", productRole)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.status().isNoContent())
                .andReturn();
        // then
        Mockito.verify(userServiceMock, Mockito.times(1))
                .deleteUserProduct(userId, institutionid, productId, productRole);
        Mockito.verifyNoMoreInteractions(userServiceMock);
    }

    @Test
    void getUserById() throws Exception {
        //given
        final String userId = "userId";
        final String institutionId = "institutionId";
        final List<String> fields = List.of("fields");

        byte[] userStream = Files.readAllBytes(Paths.get(FILE_JSON_PATH + "User.json"));
        User user = objectMapper.readValue(userStream, new TypeReference<>() {
        });
        byte[] userInfoStream = Files.readAllBytes(Paths.get(FILE_JSON_PATH + "UserResource.json"));
        UserResource userResource = objectMapper.readValue(userInfoStream, new TypeReference<>() {
        });
        when(userServiceMock.getUserById(userId, institutionId, fields)).thenReturn(user);

        //when
        mockMvc.perform(MockMvcRequestBuilders
                        .get(BASE_URL + "/{id}", userId)
                        .param("institutionId", institutionId)
                        .param("fields", fields.get(0))
                        .contentType(APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(content().string(objectMapper.writeValueAsString(userResource)));

        //then
        verify(userServiceMock, times(1)).getUserById(userId, institutionId, fields);
    }

    @Test
    void search() throws Exception {
        //given
        final String externalId = "externalId";
        final String institutionId = "institutionId";
        SearchUserDto externalIdDto = new SearchUserDto();
        externalIdDto.setFiscalCode(externalId);

        byte[] userStream = Files.readAllBytes(Paths.get(FILE_JSON_PATH + "User.json"));
        User user = objectMapper.readValue(userStream, new TypeReference<>() {
        });
        byte[] userInfoStream = Files.readAllBytes(Paths.get(FILE_JSON_PATH + "UserResource.json"));
        UserResource userResource = objectMapper.readValue(userInfoStream, new TypeReference<>() {
        });

        Mockito.when(userServiceMock.searchUserByFiscalCode(externalId, institutionId))
                .thenReturn(user);
        //when
        mockMvc.perform(MockMvcRequestBuilders
                        .post(BASE_URL + "/search")
                        .param("institutionId", institutionId)
                        .content(objectMapper.writeValueAsString(externalIdDto))
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(content().string(objectMapper.writeValueAsString(userResource)));

        //then
        Mockito.verify(userServiceMock, Mockito.times(1))
                .searchUserByFiscalCode(externalId, institutionId);
        Mockito.verifyNoMoreInteractions(userServiceMock);
    }

    @Test
    void updateUser() throws Exception {
        //given
        final String id = "userId";
        final String institutionId = "institutionId";

        ClassPathResource pathUpdateUserDto = new ClassPathResource("stubs/updateUserDto.json");
        byte[] userStream = Files.readAllBytes(pathUpdateUserDto.getFile().toPath());
        UpdateUserDto updateUserDto = objectMapper.readValue(userStream, UpdateUserDto.class);

        //when
        mockMvc.perform(MockMvcRequestBuilders
                        .put(BASE_URL + "/{id}", id)
                        .queryParam("institutionId", institutionId)
                        .content(userStream)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isNoContent())
                .andExpect(content().string(emptyString()));
        //then
        verify(userServiceMock, times(1))
                .updateUser(id, institutionId, userMapper.fromUpdateUser(updateUserDto));

        Mockito.verifyNoMoreInteractions(userServiceMock);
    }

    @Test
    void updateUserInvalidMobilePhone() throws Exception {
        //given
        final String id = "userId";
        final String institutionId = "institutionId";

        ClassPathResource pathUpdateUserDto = new ClassPathResource("stubs/updateUserDto.json");
        byte[] userStream = Files.readAllBytes(pathUpdateUserDto.getFile().toPath());
        UpdateUserDto updateUserDto = objectMapper.readValue(userStream, UpdateUserDto.class);
        updateUserDto.setMobilePhone("12345678912345566788");

        //when
        mockMvc.perform(MockMvcRequestBuilders
                        .put(BASE_URL + "/{id}", id)
                        .queryParam("institutionId", institutionId)
                        .content(objectMapper.writeValueAsString(updateUserDto))
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isBadRequest());

        Mockito.verifyNoInteractions(userServiceMock);
    }

    @Test
    void updateUserInvalidMail() throws Exception {
        //given
        final String id = "userId";
        final String institutionId = "institutionId";

        ClassPathResource pathUpdateUserDto = new ClassPathResource("stubs/updateUserDto.json");
        byte[] userStream = Files.readAllBytes(pathUpdateUserDto.getFile().toPath());
        UpdateUserDto updateUserDto = objectMapper.readValue(userStream, UpdateUserDto.class);
        updateUserDto.setEmail("test");

        //when
        mockMvc.perform(MockMvcRequestBuilders
                        .put(BASE_URL + "/{id}", id)
                        .queryParam("institutionId", institutionId)
                        .content(objectMapper.writeValueAsString(updateUserDto))
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isBadRequest());

        Mockito.verifyNoInteractions(userServiceMock);
    }

    @Test
    void getUsers_institutionIdProductIdValid() throws Exception {
        // given
        final String institutionId = "institutionId";
        final String productId = "productId";
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(SelfCareUser.builder("userId").build());

        byte[] userInfoStream = Files.readAllBytes(Paths.get(FILE_JSON_PATH + "UserInfo.json"));
        UserInfo userInfo = objectMapper.readValue(userInfoStream, UserInfo.class);
        List<UserInfo> userInfos = List.of(userInfo);

        when(userServiceMock.getUsersByInstitutionId(institutionId, productId, null, List.of(PartyRole.MANAGER.name()), "userId")).thenReturn(userInfos);

        // when
        mockMvc.perform(MockMvcRequestBuilders
                        .get(BASE_URL + "/institution/" + institutionId)
                        .principal(authentication)
                        .queryParam("productId", productId)
                        .queryParam("roles", PartyRole.MANAGER.name())
                        .contentType(APPLICATION_JSON_VALUE)
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(content().json(new String(Files.readAllBytes(Paths.get(FILE_JSON_PATH + "ProductUserResource.json")))));

        // then
        verify(userServiceMock, times(1))
                .getUsersByInstitutionId(institutionId, productId, null, List.of(PartyRole.MANAGER.name()), "userId");
        verifyNoMoreInteractions(userServiceMock);
    }


    @Test
    void getAllUsers_institutionIdProductIdAndFiltersValid() throws Exception {
        // given
        final String institutionId = "institutionId";
        final String productId = "productId";
        Authentication authentication = mock(Authentication.class);

        Path userInstitutionRolesPath = Paths.get(FILE_JSON_PATH + "UserInstitutionRoles.json");
        byte[] userInstitutionRolesStream = Files.readAllBytes(userInstitutionRolesPath);
        UserInstitutionRole userInstitutionRole = objectMapper.readValue(userInstitutionRolesStream, UserInstitutionRole.class);
        List<UserInstitutionRole> userInstitutionRoles = List.of(userInstitutionRole);

        when(userServiceMock.getAllUsersByInstitutionId(institutionId, productId, List.of("ACTIVE", "DELETED"), List.of(PartyRole.MANAGER.name()))).thenReturn(userInstitutionRoles);

        // when
        mockMvc.perform(MockMvcRequestBuilders
                        .get(BASE_URL + "/all/institution/" + institutionId)
                        .principal(authentication)
                        .queryParam("productId", productId)
                        .queryParam("roles", PartyRole.MANAGER.name())
                        .queryParam("states", "ACTIVE,DELETED")
                        .contentType(APPLICATION_JSON_VALUE)
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(userInstitutionRoles)));

        // then
        verify(userServiceMock, times(1))
                .getAllUsersByInstitutionId(institutionId, productId, List.of("ACTIVE", "DELETED"), List.of(PartyRole.MANAGER.name()));
        verifyNoMoreInteractions(userServiceMock);
    }

    @Test
    void getAllUsers_institutionIdWithoutOptionalFilters() throws Exception {
        // given
        final String institutionId = "institutionId";
        final String productId = "productId";
        Authentication authentication = mock(Authentication.class);

        Path userInstitutionRolesPath = Paths.get(FILE_JSON_PATH + "UserInstitutionRoles.json");
        byte[] userInstitutionRolesStream = Files.readAllBytes(userInstitutionRolesPath);
        UserInstitutionRole userInstitutionRole = objectMapper.readValue(userInstitutionRolesStream, UserInstitutionRole.class);
        List<UserInstitutionRole> userInstitutionRoles = List.of(userInstitutionRole);

        when(userServiceMock.getAllUsersByInstitutionId(institutionId, productId, null, null)).thenReturn(userInstitutionRoles);

        // when
        mockMvc.perform(MockMvcRequestBuilders
                        .get(BASE_URL + "/all/institution/" + institutionId)
                        .principal(authentication)
                        .queryParam("productId", productId)
                        .contentType(APPLICATION_JSON_VALUE)
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(userInstitutionRoles)));

        // then
        verify(userServiceMock, times(1))
                .getAllUsersByInstitutionId(institutionId, productId, null, null);
        verifyNoMoreInteractions(userServiceMock);
    }


    @Test
    void getAllUsers_withoutProductId() throws Exception {
        final String institutionId = "institutionId";
        Authentication authentication = mock(Authentication.class);

        mockMvc.perform(MockMvcRequestBuilders
                        .get(BASE_URL + "/all/institution/" + institutionId)
                        .principal(authentication)
                        .contentType(APPLICATION_JSON_VALUE)
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isBadRequest());
    }

    @Test
    void suspendRelationship_EmptyObject() throws Exception {
        // given
        String userId = "rel1";
        String institutionid = "id1";
        String productId = "prod-pagopa";
        String productRole = "admin";

        mockMvc.perform(MockMvcRequestBuilders
                        .post(BASE_URL + "/{userId}/suspend", userId)
                        .queryParam("institutionId", institutionid)
                        .queryParam("productId", productId)
                        .queryParam("productRole", productRole)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.status().isNoContent())
                .andExpect(content().string(emptyString()));

        // then
        Mockito.verify(userServiceMock, Mockito.times(1))
                .suspendUserProduct(userId, institutionid, productId, productRole);
        Mockito.verifyNoMoreInteractions(userServiceMock);
    }

    @Test
    void activateRelationship_EmptyObject() throws Exception {
        // given
        String userId = "rel1";
        String institutionid = "id1";
        String productId = "prod-pagopa";
        String productRole = "admin";

        mockMvc.perform(MockMvcRequestBuilders
                        .post(BASE_URL + "/{userId}/activate", userId)
                        .queryParam("institutionId", institutionid)
                        .queryParam("productId", productId)
                        .queryParam("productRole", productRole)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.status().isNoContent())
                .andExpect(content().string(emptyString()));

        // then
        Mockito.verify(userServiceMock, Mockito.times(1))
                .activateUserProduct(userId, institutionid, productId, productRole);
        Mockito.verifyNoMoreInteractions(userServiceMock);
    }

    @Test
    void deleteRelationship_EmptyObject() throws Exception {
        // given
        String userId = "rel1";
        String institutionid = "id1";
        String productId = "prod-pagopa";
        String productRole = "admin";


        mockMvc.perform(MockMvcRequestBuilders
                        .delete(BASE_URL + "/{userId}", userId)
                        .queryParam("institutionId", institutionid)
                        .queryParam("productId", productId)
                        .queryParam("productRole", productRole)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.status().isNoContent())
                .andExpect(content().string(emptyString()));

        // then
        Mockito.verify(userServiceMock, Mockito.times(1))
                .deleteUserProduct(userId, institutionid, productId, productRole);
        Mockito.verifyNoMoreInteractions(userServiceMock);
    }

    @Test
    void getUserById_EmptyObject() throws Exception {
        //given
        final String userId = "userId";
        final String institutionId = "institutionId";
        final List<String> fields = List.of("fields");

        // when
        Mockito.when(userServiceMock.getUserById(userId, institutionId, fields))
                .thenReturn(new User());

        mockMvc.perform(MockMvcRequestBuilders
                        .get(BASE_URL + "/{id}", userId)
                        .param("institutionId", institutionId)
                        .param("fields", fields.get(0))
                        .contentType(APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(content().json("{}"));

        // then
        verify(userServiceMock, times(1)).getUserById(userId, institutionId, fields);
    }

    @Test
    void search_EmptyObject() throws Exception {
        //given
        final String externalId = "externalId";
        final String institutionId = "institutionId";
        SearchUserDto externalIdDto = new SearchUserDto();
        externalIdDto.setFiscalCode(externalId);

        Mockito.when(userServiceMock.searchUserByFiscalCode(externalId, institutionId))
                .thenReturn(null);
        //when
        mockMvc.perform(MockMvcRequestBuilders
                        .post(BASE_URL + "/search")
                        .param("institutionId", institutionId)
                        .content(objectMapper.writeValueAsString(externalIdDto))
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(content().string(""));

        //then
        Mockito.verify(userServiceMock, Mockito.times(1))
                .searchUserByFiscalCode(externalId, institutionId);
        Mockito.verifyNoMoreInteractions(userServiceMock);
    }

    @Test
    void getUsers_institutionIdProductIdValid_EmptyObject() throws Exception {
        // given
        final String institutionId = "institutionId";
        final String productId = "productId";
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(SelfCareUser.builder("userId").build());

        // when
        when(userServiceMock.getUsersByInstitutionId(institutionId, productId, null, null, "userId"))
                .thenReturn(List.of(new UserInfo()));

        // when
        mockMvc.perform(MockMvcRequestBuilders
                        .get(BASE_URL + "/institution/" + institutionId)
                        .principal(authentication)
                        .queryParam("productId", productId)
                        .contentType(APPLICATION_JSON_VALUE)
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        // then
        verify(userServiceMock, times(1))
                .getUsersByInstitutionId(institutionId, productId, null, null, "userId");
        verifyNoMoreInteractions(userServiceMock);
    }

    @Test
    void getUserOtpEmailInfoTest() throws Exception {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(SelfCareUser.builder("userId").build());
        when(userServiceMock.getUserOtpEmailInfo(anyString())).thenReturn(UserOtpEmailInfo.builder().build());

        mockMvc.perform(MockMvcRequestBuilders
                .get(BASE_URL + "/otp-info")
                .principal(authentication))
                .andExpect(status().isOk());

        verify(userServiceMock, times(1)).getUserOtpEmailInfo(anyString());
        verifyNoMoreInteractions(userServiceMock);
    }

}
