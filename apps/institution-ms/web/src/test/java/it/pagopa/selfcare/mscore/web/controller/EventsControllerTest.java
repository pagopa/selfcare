package it.pagopa.selfcare.mscore.web.controller;

import it.pagopa.selfcare.mscore.core.EventsService;
import it.pagopa.selfcare.mscore.web.config.WebConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;

@ExtendWith(MockitoExtension.class)
public class EventsControllerTest {

    @InjectMocks
    private EventsController eventsController;

    @Mock
    private EventsService eventsService;

    @Test
    void sendDelegationEventsTest() throws Exception {
        doNothing().when(eventsService).sendDelegationEvents(any());
        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders
                .post("/events/delegations?fromDate=2023-10-10T10:00:00Z");
        MockMvcBuilders.standaloneSetup(eventsController)
                .setConversionService(getFormattingConversionService())
                .build()
                .perform(requestBuilder)
                .andExpect(MockMvcResultMatchers.status().isNoContent());
    }

    @Test
    void sendDelegationEventsBadRequestTest() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders
                .post("/events/delegations?fromDate=invalid-date");
        MockMvcBuilders.standaloneSetup(eventsController)
                .setConversionService(getFormattingConversionService())
                .build()
                .perform(requestBuilder)
                .andExpect(MockMvcResultMatchers.status().isBadRequest());
    }

    @Test
    void sendDelegationEventsMissingArgumentTest() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders
                .post("/events/delegations");
        MockMvcBuilders.standaloneSetup(eventsController)
                .setConversionService(getFormattingConversionService())
                .build()
                .perform(requestBuilder)
                .andExpect(MockMvcResultMatchers.status().isBadRequest());
    }

    private FormattingConversionService getFormattingConversionService() {
        final FormattingConversionService fcs = new FormattingConversionService();
        fcs.addConverter(new WebConfig.StringToOffsetDateTimeConverter());
        return fcs;
    }

}
