package it.pagopa.selfcare.onboarding.web.controller;

import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.selfcare.commons.base.security.SelfCareUser;
import it.pagopa.selfcare.commons.web.security.JwtAuthenticationToken;
import it.pagopa.selfcare.onboarding.connector.model.onboarding.CheckManagerData;
import it.pagopa.selfcare.onboarding.connector.model.onboarding.OnboardingData;
import it.pagopa.selfcare.onboarding.connector.model.user.UserId;
import it.pagopa.selfcare.onboarding.core.UserService;
import it.pagopa.selfcare.onboarding.core.exception.InvalidUserFieldsException;
import it.pagopa.selfcare.onboarding.web.config.WebTestConfig;
import it.pagopa.selfcare.onboarding.web.handler.OnboardingExceptionHandler;
import it.pagopa.selfcare.onboarding.web.model.UserDataValidationDto;
import it.pagopa.selfcare.onboarding.web.model.UserTaxCodeDto;
import it.pagopa.selfcare.onboarding.web.model.mapper.OnboardingResourceMapperImpl;
import it.pagopa.selfcare.onboarding.web.model.mapper.UserResourceMapper;
import it.pagopa.selfcare.onboarding.web.model.mapper.UserResourceMapperImpl;
import java.security.Principal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@WebMvcTest(value = {UserController.class}, excludeAutoConfiguration = SecurityAutoConfiguration.class)
@ContextConfiguration(classes = {
        UserController.class,
        OnboardingResourceMapperImpl.class,
        WebTestConfig.class,
        OnboardingExceptionHandler.class,
        UserResourceMapperImpl.class,
        UserResourceMapperImpl.class
})
class UserControllerTest {

    private static final String BASE_URL = "/v1/users";

    @Autowired
    protected MockMvc mvc;

    @MockBean
    protected UserService userServiceMock;

    @MockBean
    private UserResourceMapper userResourceMapper;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Method under test: {@link UserController#validate(UserDataValidationDto)}
     */
    @Test
    void validate_OK(@Value("classpath:stubs/userDataValidationDto.json") Resource userDataValidationDto) throws Exception {
        //given
        //when
        mvc.perform(MockMvcRequestBuilders
                .post(BASE_URL + "/validate")
                .content(userDataValidationDto.getInputStream().readAllBytes())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isNoContent())
                .andExpect(content().string(emptyString()));
        //then
        Mockito.verify(userServiceMock, Mockito.times(1))
                .validate(any());
        Mockito.verifyNoMoreInteractions(userServiceMock);
    }


    @Test
    void validate_conflict(@Value("classpath:stubs/userDataValidationDto.json") Resource userDataValidationDto) throws Exception {
        //given
        Mockito.doThrow(InvalidUserFieldsException.class)
                .when(userServiceMock)
                .validate(any());
        //when
        mvc.perform(MockMvcRequestBuilders
                .post(BASE_URL + "/validate")
                .content(userDataValidationDto.getInputStream().readAllBytes())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(content().string(not(emptyString())));
        //then
        Mockito.verify(userServiceMock, Mockito.times(1))
                .validate(any());
        Mockito.verifyNoMoreInteractions(userServiceMock);
    }

    @Test
    void onboardingUsers(@Value("classpath:stubs/onboardingUsers.json") Resource onboardinUserDto) throws Exception {
        // when
        mvc.perform(MockMvcRequestBuilders
                        .post(BASE_URL + "/onboarding")
                        .content(onboardinUserDto.getInputStream().readAllBytes())
                        .contentType(APPLICATION_JSON_VALUE)
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isCreated())
                .andExpect(content().string(emptyString()));
        // then
        verify(userServiceMock, times(1))
                .onboardingUsers(any(OnboardingData.class));
        verifyNoMoreInteractions(userServiceMock);
    }


    @Test
    void onboardingUsersAggregator(@Value("classpath:stubs/onboardingUsers.json") Resource onboardinUserDto) throws Exception {
        // when
        mvc.perform(MockMvcRequestBuilders
                        .post(BASE_URL + "/onboarding/aggregator")
                        .content(onboardinUserDto.getInputStream().readAllBytes())
                        .contentType(APPLICATION_JSON_VALUE)
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isCreated())
                .andExpect(content().string(emptyString()));
        // then
        verify(userServiceMock, times(1))
                .onboardingUsersAggregator(any(OnboardingData.class));
        verifyNoMoreInteractions(userServiceMock);
    }

    /**
     * Method under test: {@link UserController#checkManager(it.pagopa.selfcare.onboarding.web.model.CheckManagerDto)}
     */
    @Test
    void checkManager(@Value("classpath:stubs/checkManagerDto.json") Resource checkManagerDto) throws Exception {
        // when
        mvc.perform(MockMvcRequestBuilders
                        .post(BASE_URL + "/check-manager")
                        .content(checkManagerDto.getInputStream().readAllBytes())
                        .contentType(APPLICATION_JSON_VALUE)
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isOk());
        // then
        verify(userServiceMock, times(1))
                .checkManager(any(CheckManagerData.class));
        verifyNoMoreInteractions(userServiceMock);
    }

    /**
     * Method under test: {@link UserController#getManagerInfo(String, Principal)}
     */
    @Test
    void getManagerInfo() throws Exception {
        //given
        JwtAuthenticationToken mockPrincipal = Mockito.mock(JwtAuthenticationToken.class);
        SelfCareUser selfCareUser = SelfCareUser.builder("example")
                .fiscalCode("fiscalCode")
                .build();
        Mockito.when(mockPrincipal.getPrincipal()).thenReturn(selfCareUser);

        // when
        mvc.perform(MockMvcRequestBuilders
                        .get(BASE_URL + "/onboarding/onboarding-test-id/manager")
                        .principal(mockPrincipal)
                        .contentType(APPLICATION_JSON_VALUE)
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isOk());
        // then
        verify(userServiceMock, times(1))
                .getManagerInfo(any(), any());
        verifyNoMoreInteractions(userServiceMock);
    }

    @Test
    void searchUser_returnsUserId_whenValidRequest() throws Exception {
        // given
        UserTaxCodeDto requestDto = new UserTaxCodeDto();
        requestDto.setTaxCode("ABCDEF12G34H567I");

        String taxCodeAsString = "ABCDEF12G34H567I";
        UUID uuid = UUID.randomUUID();
        UserId userId = new UserId();
        userId.setId(uuid);

        when(userResourceMapper.toString(requestDto)).thenReturn(taxCodeAsString);
        when(userServiceMock.searchUser(taxCodeAsString)).thenReturn(userId);

        // when
        mvc.perform(MockMvcRequestBuilders
                        .post(BASE_URL+ "/search-user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(userId)));

        // then
        verify(userResourceMapper).toString(requestDto);
        verify(userServiceMock).searchUser(taxCodeAsString);
    }

}