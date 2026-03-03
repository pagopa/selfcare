package it.pagopa.selfcare.onboarding.mapper;

import it.pagopa.selfcare.onboarding.model.*;
import org.junit.jupiter.api.Test;
import org.openapi.quarkus.onboarding_functions_json.model.PartyRole;
import org.openapi.quarkus.onboarding_functions_json.model.WorkflowType;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OnboardingMapperTest {

    private final OnboardingMapper mapper = new OnboardingMapperImpl();

    @Test
    void toUpperCase_withNonNullString_returnsUpperCase() {
        String input = "recipientCode";
        String result = mapper.toUpperCase(input);
        assertEquals("RECIPIENTCODE", result);
    }

    @Test
    void toUpperCase_withNullString_returnsNull() {
        String result = mapper.toUpperCase(null);
        assertNull(result);
    }

    @Test
    void toUpperCase_withEmptyString_returnsEmptyString() {
        String input = "";
        String result = mapper.toUpperCase(input);
        assertEquals("", result);
    }

    @Test
    void toWorkflowType_withValidWorkflowType_returnsMappedWorkflowType() {
        it.pagopa.selfcare.onboarding.common.WorkflowType input = it.pagopa.selfcare.onboarding.common.WorkflowType.FOR_APPROVE;
        org.openapi.quarkus.onboarding_functions_json.model.WorkflowType result = mapper.toWorkflowType(input);
        assertEquals(WorkflowType.FOR_APPROVE, result);
    }

    @Test
    void toWorkflowType_withNullWorkflowType_returnsNull() {
        it.pagopa.selfcare.onboarding.common.WorkflowType input = null;
        org.openapi.quarkus.onboarding_functions_json.model.WorkflowType result = mapper.toWorkflowType(input);
        assertNull(result);
    }

    @Test
    void toUsers_withValidUsersList_returnsMappedUsersList() {
        List<it.pagopa.selfcare.onboarding.entity.User> users = List.of(
                new it.pagopa.selfcare.onboarding.entity.User(),
                new it.pagopa.selfcare.onboarding.entity.User()
        );
        List<org.openapi.quarkus.onboarding_functions_json.model.User> result = mapper.toUsers(users);
        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    void toUsers_withEmptyUsersList_returnsNull() {
        List<it.pagopa.selfcare.onboarding.entity.User> users = List.of();
        List<org.openapi.quarkus.onboarding_functions_json.model.User> result = mapper.toUsers(users);
        assertNull(result);
    }

    @Test
    void toUsers_withNullUsersList_returnsNull() {
        List<it.pagopa.selfcare.onboarding.entity.User> users = null;
        List<org.openapi.quarkus.onboarding_functions_json.model.User> result = mapper.toUsers(users);
        assertNull(result);
    }

    @Test
    void toPartyRole_withValidRole_returnsMappedPartyRole() {
        it.pagopa.selfcare.onboarding.common.PartyRole input = it.pagopa.selfcare.onboarding.common.PartyRole.MANAGER;
        PartyRole result = mapper.toPartyRole(input);
        assertEquals(PartyRole.MANAGER, result);
    }

    @Test
    void toPartyRole_withNullRole_returnsNull() {
        it.pagopa.selfcare.onboarding.common.PartyRole input = null;
        PartyRole result = mapper.toPartyRole(input);
        assertNull(result);
    }

    @Test
    void toOffsetDateTime_withValidLocalDateTime_returnsOffsetDateTime() {
        LocalDateTime input = LocalDateTime.of(2023, 10, 1, 12, 0);
        OffsetDateTime result = mapper.toOffsetDateTime(input);
        assertNotNull(result);
        assertEquals(OffsetDateTime.of(2023, 10, 1, 12, 0, 0, 0, java.time.ZoneOffset.UTC), result);
    }

    @Test
    void toOffsetDateTime_withNullLocalDateTime_returnsNull() {
        LocalDateTime input = null;
        OffsetDateTime result = mapper.toOffsetDateTime(input);
        assertNull(result);
    }

    @Test
    void mapCsvAppIoAggregateToAggregates_withValidList_returnsMappedList() {
        List<CsvAggregateAppIo> input = List.of(new CsvAggregateAppIo());
        List<Aggregate> result = mapper.mapCsvAppIoAggregatesToAggregates(input);
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void mapCsvAppIoAggregateToAggregates_withNullList_returnsEmptyList() {
        List<CsvAggregateAppIo> input = null;
        List<Aggregate> result = mapper.mapCsvAppIoAggregatesToAggregates(input);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void mapCsvSendAggregateToAggregates_withValidList_returnsMappedList() {
        List<CsvAggregateSend> input = List.of(new CsvAggregateSend());
        List<Aggregate> result = mapper.mapCsvSendAggregatesToAggregates(input);
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void mapCsvSendAggregateToAggregates_withNullList_returnsNull() {
        List<CsvAggregateSend> input = null;
        List<Aggregate> result = mapper.mapCsvSendAggregatesToAggregates(input);
        assertNull(result);
    }

    @Test
    void mapUsers_withValidCsvAggregateSend_returnsMappedUserList() {
        CsvAggregateSend input = new CsvAggregateSend();
        input.setAdminAggregateName("John");
        input.setAdminAggregateSurname("Doe");
        input.setAdminAggregateTaxCode("TAXCODE123");
        input.setAdminAggregateEmail("john.doe@example.com");
        List<AggregateUser> result = mapper.mapUsers(input);
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("John", result.get(0).getName());
        assertEquals("Doe", result.get(0).getSurname());
        assertEquals("TAXCODE123", result.get(0).getTaxCode());
        assertEquals("john.doe@example.com", result.get(0).getEmail());
    }

    @Test
    void mapUsers_withNullCsvAggregateSend_returnsEmptyList() {
        CsvAggregateSend input = null;
        List<AggregateUser> result = mapper.mapUsers(input);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}

