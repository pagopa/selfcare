package it.pagopa.selfcare.onboarding.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import it.pagopa.selfcare.onboarding.exception.InvalidRequestException;
import it.pagopa.selfcare.onboarding.model.dto.request.DownloadDocumentType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.UriInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class RequestParamsTest {

    enum Choice { VALUE }

    @Test
    void relocatedDownloadEnumKeepsThePublishedConversionErrorType() {
        String typeName = "it.pagopa.selfcare.onboarding.controller.request.DownloadDocumentType";

        InvalidRequestException failure = assertThrows(InvalidRequestException.class,
                () -> RequestParams.requiredEnum("type", "bad", DownloadDocumentType.class));

        assertEquals("Method parameter 'type': Failed to convert value of type 'java.lang.String' to required type '"
                + typeName + "'; Failed to convert from type [java.lang.String] to type [" + typeName
                + "] for value [bad]", failure.getMessage());
    }

    @Test
    void repeatedStringsKeepOrderDuplicatesAndEmptyItems() {
        UriInfo uriInfo = mock(UriInfo.class);
        var query = new MultivaluedHashMap<String, String>();
        query.addAll("search", "", "beta", "beta");
        when(uriInfo.getQueryParameters()).thenReturn(query);
        assertEquals(",beta,beta", RequestParams.stringQuery(uriInfo, "search", null));
        assertNull(RequestParams.stringQuery(uriInfo, "missing", null));
        assertEquals("bound", RequestParams.stringQuery(null, "search", "bound"));
        query.putSingle("search", "");
        assertNull(RequestParams.stringQuery(uriInfo, "search", null));
        assertEquals("", RequestParams.stringQuery(uriInfo, "search", ""));
    }

    @ParameterizedTest
    @CsvSource({"0x10,16", "#10,16", "-0X10,-16", "010,10", "+010,10", "1 2,12", "-2147483648,-2147483648"})
    void integersUseSpringConversion(String value, int expected) {
        assertEquals(expected, RequestParams.optionalInt("page", value));
    }

    @ParameterizedTest
    @ValueSource(strings = {"1_0", "2147483648", "0x80000000"})
    void invalidIntegersDoNotBecomeDefaults(String value) {
        assertThrows(InvalidRequestException.class, () -> RequestParams.optionalInt("page", value));
    }

    @Test
    void blankEnumsUseAsciiTrimming() {
        assertNull(RequestParams.optionalInt("page", null));
        assertNull(RequestParams.optionalInt("page", ""));
        assertNull(RequestParams.optionalInt("page", " \t\u2003"));
        assertNull(RequestParams.optionalEnum("type", null, Choice.class));
        assertNull(RequestParams.optionalEnum("type", "", Choice.class));
        assertEquals(Choice.VALUE, RequestParams.optionalEnum("type", " VALUE ", Choice.class));
        assertNull(RequestParams.optionalEnum("type", " \t", Choice.class));
        assertThrows(InvalidRequestException.class, () -> RequestParams.optionalEnum("type", "\u2003", Choice.class));
        assertThrows(InvalidRequestException.class, () -> RequestParams.optionalEnum("type", "value", Choice.class));
        assertEquals("Required request parameter 'type' for method parameter type Choice is not present",
                assertThrows(InvalidRequestException.class, () -> RequestParams.requiredEnum("type", null, Choice.class)).getMessage());
    }

    @ParameterizedTest
    @CsvSource({"true,true", "ON,true", "yes,true", "1,true", "false,false", "off,false", "NO,false", "0,false"})
    void booleansUseSpringConversion(String value, boolean expected) {
        assertEquals(expected, RequestParams.optionalBoolean("flag", value));
    }

    @Test
    void booleansTrimAsciiWhitespaceOnly() {
        assertNull(RequestParams.optionalBoolean("flag", " \t "));
        assertThrows(InvalidRequestException.class, () -> RequestParams.optionalBoolean("flag", "\u2003"));
        assertThrows(InvalidRequestException.class, () -> RequestParams.optionalBoolean("flag", "wrong"));
    }

    @Test
    void requiredValuesRetainTheirTypeAndRejectOnlyMissingValues() {
        Object body = new Object();
        assertSame(body, RequestParams.requiredBody(body));
        assertSame(body, RequestParams.requiredPart("part", body));
        assertEquals("", RequestParams.requiredQuery("query", ""));
        assertEquals("", RequestParams.requiredHeader("header", ""));
        assertThrows(InvalidRequestException.class, () -> RequestParams.requiredBody(null));
        assertThrows(InvalidRequestException.class, () -> RequestParams.requiredPart("part", null));
        assertThrows(InvalidRequestException.class, () -> RequestParams.requiredQuery("query", null));
        assertThrows(InvalidRequestException.class, () -> RequestParams.requiredHeader("header", null));
    }
}
