package it.pagopa.selfcare.onboarding.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.exception.InvalidRequestException;
import it.pagopa.selfcare.onboarding.model.VerifyType;
import it.pagopa.selfcare.onboarding.model.dto.request.DownloadDocumentType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.UriInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class RequestParamsTest {

    private static final String SPRING_QUERY_PARAMETER =
            "@io.swagger.v3.oas.annotations.Parameter @org.springframework.web.bind.annotation.RequestParam ";

    enum Choice { VALUE }

    @Test
    void invalidDownloadTypeKeepsTheSpringConversionErrorOfTheRemovedWebModule() {
        // given
        String rawType = "bad";

        // when
        InvalidRequestException failure = assertThrows(InvalidRequestException.class,
                () -> RequestParams.requiredEnum("type", rawType, DownloadDocumentType.class));

        // then
        assertEquals("Failed to convert value of type 'java.lang.String' to required type "
                + "'it.pagopa.selfcare.onboarding.web.model.DownloadDocumentType'; "
                + "Failed to convert from type [java.lang.String] to type [" + SPRING_QUERY_PARAMETER
                + "it.pagopa.selfcare.onboarding.web.model.DownloadDocumentType] for value [bad]", failure.getMessage());
    }

    @Test
    void invalidVerifyTypeKeepsTheSpringConversionErrorOfTheRemovedWebModule() {
        // given
        String rawType = "internal";

        // when
        InvalidRequestException failure = assertThrows(InvalidRequestException.class,
                () -> RequestParams.optionalEnum("verifyType", rawType, VerifyType.class));

        // then
        assertEquals("Failed to convert value of type 'java.lang.String' to required type "
                + "'it.pagopa.selfcare.onboarding.web.model.VerifyType'; "
                + "Failed to convert from type [java.lang.String] to type [" + SPRING_QUERY_PARAMETER
                + "it.pagopa.selfcare.onboarding.web.model.VerifyType] for value [internal]", failure.getMessage());
    }

    @Test
    void invalidInstitutionTypeIsReportedAgainstTheOptionalParameterLikeSpring() {
        // given
        String rawType = "pa";

        // when
        InvalidRequestException failure = assertThrows(InvalidRequestException.class,
                () -> RequestParams.optionalEnum("institutionType", rawType, InstitutionType.class));

        // then
        assertEquals("Failed to convert value of type 'java.lang.String' to required type 'java.util.Optional'; "
                + "Failed to convert from type [java.lang.String] to type [" + SPRING_QUERY_PARAMETER
                + "it.pagopa.selfcare.onboarding.common.InstitutionType] for value [pa]", failure.getMessage());
    }

    @Test
    void optionalInstitutionTypeIsAbsentOnlyWhenEmptyAndTrimsPaddedConstants() {
        // given
        String paddedType = " PA ";

        // when
        InstitutionType padded = RequestParams.optionalEnum("institutionType", paddedType, InstitutionType.class);

        // then
        assertEquals(InstitutionType.PA, padded);
        assertNull(RequestParams.optionalEnum("institutionType", null, InstitutionType.class));
        assertNull(RequestParams.optionalEnum("institutionType", "", InstitutionType.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {" ", "\t", "\u2003"})
    void blankInstitutionTypeIsRejectedBecauseSpringConvertsTheOptionalElementWithoutBlankReset(String value) {
        // given
        String blankType = value;

        // when
        InvalidRequestException failure = assertThrows(InvalidRequestException.class,
                () -> RequestParams.optionalEnum("institutionType", blankType, InstitutionType.class));

        // then
        assertEquals("Failed to convert value of type 'java.lang.String' to required type 'java.util.Optional'; "
                + "Failed to convert from type [java.lang.String] to type [" + SPRING_QUERY_PARAMETER
                + "it.pagopa.selfcare.onboarding.common.InstitutionType] for value [" + value + "]",
                failure.getMessage());
    }

    @Test
    void blankPlainEnumIsAbsentWhileBlankInstitutionTypeIsNot() {
        // given
        String blank = " \t";

        // when
        Choice plain = RequestParams.optionalEnum("type", blank, Choice.class);

        // then
        assertNull(plain);
        assertThrows(InvalidRequestException.class,
                () -> RequestParams.optionalEnum("institutionType", blank, InstitutionType.class));
    }

    @Test
    void conversionErrorsEchoOnlyTheFirstHundredCharactersOfTheRejectedValue() {
        // given
        String exactLimit = "a".repeat(100);
        String overLimit = "b".repeat(101);

        // when
        InvalidRequestException atLimit = assertThrows(InvalidRequestException.class,
                () -> RequestParams.optionalEnum("type", exactLimit, Choice.class));
        InvalidRequestException truncated = assertThrows(InvalidRequestException.class,
                () -> RequestParams.optionalEnum("type", overLimit, Choice.class));

        // then
        assertEquals(enumMismatch(Choice.class.getName(), Choice.class.getName(), exactLimit), atLimit.getMessage());
        assertEquals(enumMismatch(Choice.class.getName(), Choice.class.getName(), "b".repeat(100) + " (truncated)..."),
                truncated.getMessage());
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "it.pagopa.selfcare.onboarding.model.dto.request.UserTaxCodeDto|it.pagopa.selfcare.onboarding.web.model.UserTaxCodeDto",
            "it.pagopa.selfcare.onboarding.model.dto.response.ProductResource|it.pagopa.selfcare.onboarding.web.model.ProductResource",
            "it.pagopa.selfcare.onboarding.model.AggregateInstitution|it.pagopa.selfcare.onboarding.web.model.AggregateInstitution",
            "it.pagopa.selfcare.onboarding.model.VerifyType|it.pagopa.selfcare.onboarding.web.model.VerifyType",
            "it.pagopa.selfcare.onboarding.client.model.GPUData|it.pagopa.selfcare.onboarding.connector.model.onboarding.GPUData",
            "java.util.ArrayList<it.pagopa.selfcare.onboarding.model.dto.request.UserDto>|java.util.ArrayList<it.pagopa.selfcare.onboarding.web.model.UserDto>",
            "it.pagopa.selfcare.onboarding.common.InstitutionType|it.pagopa.selfcare.onboarding.common.InstitutionType",
            "java.util.UUID|java.util.UUID"})
    void relocatedTypeNamesMapBackToTheNamesTheSpringBffPublished(String relocated, String published) {
        // given
        String typeName = relocated;

        // when
        String publishedName = RequestParams.springTypeName(typeName);

        // then
        assertEquals(published, publishedName);
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
    @CsvSource({"0x10,16", "#10,16", "-0X10,-16", "-0x10,-16", "-#10,-16", "010,10", "+010,10", "+16,16", "1 2,12",
            "-2147483648,-2147483648"})
    void integersUseSpringConversion(String value, int expected) {
        // given
        String rawPage = value;

        // when
        Integer page = RequestParams.optionalInt("page", rawPage);

        // then
        assertEquals(expected, page);
    }

    @ParameterizedTest
    @ValueSource(strings = {"+0x10", "+0X10", "+#10"})
    void signedPlusRadixPrefixesAreRejectedLikeSpringNumberUtils(String value) {
        // given
        String rawPage = value;

        // when
        InvalidRequestException failure = assertThrows(InvalidRequestException.class,
                () -> RequestParams.optionalInt("page", rawPage));

        // then
        assertEquals(integerMismatch("For input string: \"" + value + "\""), failure.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"\u20031", "1\u2003", "\u20281", "\u30001", "\u000B1", "\u001F1", "\u16801"})
    void unicodeWhitespaceAroundAnIntegerIsIgnoredLikeSpringTrimAllWhitespace(String value) {
        // given
        String rawPage = value;

        // when
        Integer page = RequestParams.optionalInt("page", rawPage);

        // then
        assertEquals(1, page);
    }

    @Test
    void unicodeWhitespaceInsideAnIntegerOrRadixPrefixIsRemovedBeforeConverting() {
        // given
        String splitDecimal = "1\u20032";
        String splitHexadecimal = "0\u2003x10";

        // when
        Integer decimal = RequestParams.optionalInt("page", splitDecimal);
        Integer hexadecimal = RequestParams.optionalInt("page", splitHexadecimal);

        // then
        assertEquals(12, decimal);
        assertEquals(16, hexadecimal);
    }

    @ParameterizedTest
    @ValueSource(strings = {"\u00A01", "1\u00A02", "\u00A0", "\u202F1", "\u20071", "\uFEFF1", "\u00851"})
    void nonBreakingAndNonWhitespaceSeparatorsAreRejectedAndEchoedUnchanged(String value) {
        // given
        String rawPage = value;

        // when
        InvalidRequestException failure = assertThrows(InvalidRequestException.class,
                () -> RequestParams.optionalInt("page", rawPage));

        // then
        assertEquals(integerMismatch("For input string: \"" + value + "\""), failure.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"1_0", "2147483648", "0x80000000"})
    void invalidIntegersDoNotBecomeDefaults(String value) {
        // given
        String rawPage = value;

        // when / then
        assertThrows(InvalidRequestException.class, () -> RequestParams.optionalInt("page", rawPage));
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

    @Test
    void requiredEnumReportsAnAbsentValueAsNotPresent() {
        // given
        String missingType = null;

        // when
        InvalidRequestException failure = assertThrows(InvalidRequestException.class,
                () -> RequestParams.requiredEnum("type", missingType, Choice.class));

        // then
        assertEquals("Required request parameter 'type' for method parameter type Choice is not present",
                failure.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", " \t"})
    void requiredEnumReportsAnEmptyOrBlankValueAsConvertedToNull(String value) {
        // given
        String emptyType = value;

        // when
        InvalidRequestException failure = assertThrows(InvalidRequestException.class,
                () -> RequestParams.requiredEnum("type", emptyType, Choice.class));

        // then
        assertEquals("Required request parameter 'type' for method parameter type Choice is present but converted to null",
                failure.getMessage());
    }

    @Test
    void requiredEnumTrimsTheValueBeforeConverting() {
        // given
        String paddedType = " VALUE ";

        // when
        Choice type = RequestParams.requiredEnum("type", paddedType, Choice.class);

        // then
        assertEquals(Choice.VALUE, type);
    }

    @Test
    void invalidEnumErrorEchoesTheRawValueWithTheTargetType() {
        // given
        String rawType = " bad ";

        // when
        InvalidRequestException failure = assertThrows(InvalidRequestException.class,
                () -> RequestParams.optionalEnum("type", rawType, Choice.class));

        // then
        assertEquals(enumMismatch(Choice.class.getName(), Choice.class.getName(), rawType), failure.getMessage());
    }

    @Test
    void invalidIntegerErrorReportsTheNumberFormatFailureOfTheWhitespaceFreeValue() {
        // given
        String rawPage = " 1_0 ";

        // when
        InvalidRequestException failure = assertThrows(InvalidRequestException.class,
                () -> RequestParams.optionalInt("page", rawPage));

        // then
        assertEquals(integerMismatch("For input string: \"1_0\""), failure.getMessage());
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "abc|For input string: \"abc\"",
            "1.5|For input string: \"1.5\"",
            "2147483648|For input string: \"2147483648\"",
            "0x|For input string: \"\" under radix 16",
            "-0x|For input string: \"-\" under radix 16"})
    void invalidIntegerErrorsCarryTheNumberFormatMessageLikeSpring(String value, String reason) {
        // given
        String rawPage = value;

        // when
        InvalidRequestException failure = assertThrows(InvalidRequestException.class,
                () -> RequestParams.optionalInt("page", rawPage));

        // then
        assertEquals(integerMismatch(reason), failure.getMessage());
    }

    @ParameterizedTest
    @CsvSource({"2147483647,2147483647", "0X7fffffff,2147483647", "0x1F,31", "-#A,-10", "-0,0", "000,0"})
    void integerBoundariesAndRadixPrefixesAreAccepted(String value, int expected) {
        // given
        String rawPage = value;

        // when
        Integer page = RequestParams.optionalInt("page", rawPage);

        // then
        assertEquals(expected, page);
    }

    @ParameterizedTest
    @ValueSource(strings = {"-2147483649", "0x", "#", "+", "-", "--1", "0x-1", "1.5", "abc"})
    void malformedIntegersAreBadRequestsInsteadOfDefaults(String value) {
        // given
        String rawPage = value;

        // when / then
        assertThrows(InvalidRequestException.class, () -> RequestParams.optionalInt("page", rawPage));
    }

    @Test
    void repeatedStringsKeepATrailingEmptyItem() {
        // given
        UriInfo uriInfo = mock(UriInfo.class);
        var query = new MultivaluedHashMap<String, String>();
        query.addAll("search", "a", "");
        when(uriInfo.getQueryParameters()).thenReturn(query);

        // when
        String search = RequestParams.stringQuery(uriInfo, "search", "a");

        // then
        assertEquals("a,", search);
    }

    @Test
    void requiredValuesRetainTheirTypeAndRejectOnlyMissingValues() {
        // given
        Object body = new Object();

        // when
        Object requiredBody = RequestParams.requiredBody(body, "public void legacy.Controller.method()");
        Object requiredPart = RequestParams.requiredPart("part", body);
        String requiredQuery = RequestParams.requiredQuery("query", "");

        // then
        assertSame(body, requiredBody);
        assertSame(body, requiredPart);
        assertEquals("", requiredQuery);
        assertThrows(InvalidRequestException.class,
                () -> RequestParams.requiredBody(null, "public void legacy.Controller.method()"));
        assertThrows(InvalidRequestException.class, () -> RequestParams.requiredPart("part", null));
        assertThrows(InvalidRequestException.class, () -> RequestParams.requiredQuery("query", null));
    }

    @Test
    void missingRequiredQueryKeepsThePublishedSpringMessage() {
        // given
        String missingQuery = null;

        // when
        InvalidRequestException failure = assertThrows(InvalidRequestException.class,
                () -> RequestParams.requiredQuery("query", missingQuery));

        // then
        assertEquals("Required request parameter 'query' for method parameter type String is not present",
                failure.getMessage());
    }

    @Test
    void missingRequiredPartKeepsThePublishedSpringMessage() {
        // given
        Object missingPart = null;

        // when
        InvalidRequestException failure = assertThrows(InvalidRequestException.class,
                () -> RequestParams.requiredPart("aggregates", missingPart));

        // then
        assertEquals("Required part 'aggregates' is not present.", failure.getMessage());
    }

    @Test
    void missingRequiredBodyKeepsThePublishedSpringMessage() {
        // given
        Object missingBody = null;

        // when
        InvalidRequestException failure = assertThrows(InvalidRequestException.class,
                () -> RequestParams.requiredBody(missingBody, "public void legacy.Controller.method()"));

        // then
        assertEquals("Required request body is missing: public void legacy.Controller.method()", failure.getMessage());
    }

    private static String integerMismatch(String reason) {
        return "Failed to convert value of type 'java.lang.String' to required type 'java.lang.Integer'; " + reason;
    }

    private static String enumMismatch(String requiredType, String enumType, String value) {
        return "Failed to convert value of type 'java.lang.String' to required type '" + requiredType
                + "'; Failed to convert from type [java.lang.String] to type [" + SPRING_QUERY_PARAMETER + enumType
                + "] for value [" + value + "]";
    }
}
