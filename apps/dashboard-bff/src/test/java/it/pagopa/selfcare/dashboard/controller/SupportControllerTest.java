package it.pagopa.selfcare.dashboard.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import it.pagopa.selfcare.commons.base.security.SelfCareUser;
import it.pagopa.selfcare.dashboard.model.mapper.SupportMapper;
import it.pagopa.selfcare.dashboard.model.mapper.SupportMapperImpl;
import it.pagopa.selfcare.dashboard.model.support.SupportRequest;
import it.pagopa.selfcare.dashboard.model.support.SupportRequestDto;
import it.pagopa.selfcare.dashboard.model.support.SupportResponse;
import it.pagopa.selfcare.dashboard.service.SupportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.nio.file.Files;
import java.nio.file.Paths;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class SupportControllerTest extends BaseControllerTest {

    @InjectMocks
    private SupportController supportController;

    @Mock
    private SupportService supportService;

    @Spy
    private SupportMapper supportMapper = new SupportMapperImpl();


    private static final SelfCareUser user;
    private static final String FILE_JSON_PATH = "src/test/resources/json/";


    static {
        user = SelfCareUser.builder("id")
                .surname("surname")
                .name("name")
                .fiscalCode("FFFFFFFF")
                .build();
    }

    @BeforeEach
    void setUp() {
        super.setUp(supportController);
    }

    /**
     * Method under test: {@link SupportController#sendSupportRequest(SupportRequestDto, Authentication)}
     */
    @Test
    void testSendSupportRequest() throws Exception {
        Authentication authentication = Mockito.mock(Authentication.class);
        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        SecurityContextHolder.setContext(securityContext);

        byte[] requestStream = Files.readAllBytes(Paths.get(FILE_JSON_PATH + "SupportRequest.json"));
        SupportRequest supportRequest = objectMapper.readValue(requestStream, new TypeReference<>() {});

        byte[] responseStream = Files.readAllBytes(Paths.get(FILE_JSON_PATH + "SupportResponse.json"));
        SupportResponse supportResponse = objectMapper.readValue(responseStream,  new TypeReference<>() {});

        byte[] apiRequestStream = Files.readAllBytes(Paths.get(FILE_JSON_PATH + "SupportRequestDto.json"));
        SupportRequestDto supportRequestDto = objectMapper.readValue(apiRequestStream,  new TypeReference<>() {});

        when(authentication.getPrincipal()).thenReturn(user);

        when(supportService.sendRequest(supportRequest)).thenReturn(supportResponse);

        String content = objectMapper.writeValueAsString(supportRequestDto);
        mockMvc.perform(MockMvcRequestBuilders
                        .post("/v1/support")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(content))
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(supportResponse)))
                .andReturn();
    }

    @Test
    void testSendBadRequest() throws Exception {
        SupportRequestDto supportRequest = new SupportRequestDto();
        supportRequest.setEmail("pp");
        String content = objectMapper.writeValueAsString(supportRequest);
        mockMvc.perform(MockMvcRequestBuilders
                        .post("/v1/support")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(content))
                .andExpect(status().isBadRequest())
                .andReturn();
    }
}
