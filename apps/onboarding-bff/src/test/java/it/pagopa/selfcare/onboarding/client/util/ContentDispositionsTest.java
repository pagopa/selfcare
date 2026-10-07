package it.pagopa.selfcare.onboarding.client.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class ContentDispositionsTest {

    @Test
    void missingOrBlankHeaderHasNoFilename() {
        assertNull(ContentDispositions.filename(null));
        assertNull(ContentDispositions.filename(" "));
        assertNull(ContentDispositions.filename("attachment"));
    }

    @Test
    void quotedFilenameLosesTheQuotes() {
        assertEquals("contract.pdf", ContentDispositions.filename("attachment; filename=\"contract.pdf\""));
    }

    @Test
    void unquotedFilenameIsKept() {
        assertEquals("contract.pdf", ContentDispositions.filename("attachment; filename=contract.pdf"));
    }

    @Test
    void semicolonInsideQuotesDoesNotSplitTheAttribute() {
        assertEquals("a;b.pdf", ContentDispositions.filename("attachment; filename=\"a;b.pdf\"; size=10"));
    }

    @Test
    void escapedQuoteIsUnescaped() {
        assertEquals("a\"b.pdf", ContentDispositions.filename("attachment; filename=\"a\\\"b.pdf\""));
    }

    @Test
    void extendedFilenameWinsOverPlainFilename() {
        assertEquals("caf\u00e9.pdf",
                ContentDispositions.filename("attachment; filename=\"fallback.pdf\"; filename*=UTF-8''caf%C3%A9.pdf"));
    }

    @Test
    void unsupportedExtendedCharsetFailsEvenWhenAPlainFilenameExists() {
        assertThrows(IllegalArgumentException.class,
                () -> ContentDispositions.filename("attachment; filename=\"fallback.pdf\"; filename*=UTF-16''x.pdf"));
    }

    @Test
    void parameterNamesKeepTheSpringCaseSensitivity() {
        assertNull(ContentDispositions.filename("attachment; FILENAME=\"contract.pdf\""));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', quoteCharacter = '~', textBlock = """
            attachment; filename="=?UTF-8?B?Y2Fmw6kucGRm?="|caf\u00e9.pdf
            attachment; filename="=?UTF-8?Q?caf=C3=A9_foo.pdf?="|caf\u00e9 foo.pdf
            attachment; filename="=?UTF-8?Q?a?= =?UTF-8?Q?b?="|ab
            attachment; filename="=?UTF-8?Q?a?= raw"|a
            attachment; filename="prefix =?UTF-8?Q?a?="|prefix =?UTF-8?Q?a?=
            attachment; filename="=?UTF-8?b?Y2Fmw6kucGRm?="|=?UTF-8?b?Y2Fmw6kucGRm?=
            attachment; filename*=abc.pdf|abc.pdf
            attachment; filename*="UTF-8''foo.pdf"|foo.pdf
            attachment; filename*=ISO-8859-1'en'caf%E9.pdf|caf\u00e9.pdf
            attachment; filename*=UTF-8''a%C3.pdf|a\uFFFD.pdf
            attachment; filename*=UTF-8''a+b.pdf|a+b.pdf
            attachment; filename="first.pdf"; filename="second.pdf"|first.pdf
            attachment; filename*=UTF-8''one; filename*=UTF-8''two|two
            attachment; filename*=UTF-8''extended.pdf; filename="last.pdf"|extended.pdf
            attachment; filename="open.pdf|"open.pdf
            attachment;; filename=x.pdf;|x.pdf
            """)
    void filenamesMatchTheSpringParser(String header, String expected) {
        assertEquals(expected, ContentDispositions.filename(header));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "; filename=x.pdf", "attachment; missing", "attachment; filename*=bad''x.pdf",
            "attachment; filename*=US-ASCII''plain.pdf", "attachment; filename*=UTF-8'x.pdf",
            "attachment; filename*=UTF-8''a%ZZ.pdf", "attachment; filename*=UTF-8''a%.pdf",
            "attachment; filename*=UTF-8''a b.pdf", "attachment; filename*=UTF-8''a/b.pdf",
            "attachment; filename*=UTF-8''caf\u00e9.pdf", "attachment; filename=\"=?UTF-8?Q?bad=ZZ?=\""
    })
    void malformedHeadersFailInsteadOfSilentlyFallingBack(String header) {
        assertThrows(IllegalArgumentException.class, () -> ContentDispositions.filename(header));
    }
}
