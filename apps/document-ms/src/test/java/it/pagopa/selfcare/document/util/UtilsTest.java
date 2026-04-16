package it.pagopa.selfcare.document.util;

import it.pagopa.selfcare.document.exception.InvalidRequestException;
import it.pagopa.selfcare.document.model.FormItem;
import org.jboss.resteasy.reactive.server.core.multipart.FormData;
import org.jboss.resteasy.reactive.server.multipart.FormValue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.File;
import java.util.ArrayDeque;
import java.util.Deque;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UtilsTest {

    // ---- CONTRACT_FILENAME_FUNC ----

    @Test
    void contractFilenameFunc_shouldReplacePlaceholderWithNormalizedProductName() {
        String result = Utils.CONTRACT_FILENAME_FUNC.apply("contract_%s.pdf", "My Product");
        assertEquals("contract_My_Product.pdf", result);
    }

    @Test
    void contractFilenameFunc_shouldRemoveAccentsFromProductName() {
        String result = Utils.CONTRACT_FILENAME_FUNC.apply("contract_%s.pdf", "Prodòttò Àccéntuàtò");
        assertEquals("contract_Prodotto_Accentuato.pdf", result);
    }

    @Test
    void contractFilenameFunc_shouldReturnFilenameUnchangedWhenNoPlaceholder() {
        String result = Utils.CONTRACT_FILENAME_FUNC.apply("contract_fixed.pdf", "My Product");
        assertEquals("contract_fixed.pdf", result);
    }

    @Test
    void contractFilenameFunc_shouldReturnNullFilenameWhenFilenameIsNull() {
        String result = Utils.CONTRACT_FILENAME_FUNC.apply(null, "My Product");
        assertNull(result);
    }

    @Test
    void contractFilenameFunc_shouldReplaceMultipleSpacesWithSingleUnderscore() {
        String result = Utils.CONTRACT_FILENAME_FUNC.apply("%s_doc.pdf", "My   Product   Name");
        assertEquals("My_Product_Name_doc.pdf", result);
    }

    // ---- retrieveAttachmentFromFormData ----

    @Test
    void retrieveAttachmentFromFormData_shouldReturnFormItemWithSingleAttachment() {
        FormData formData = mock(FormData.class);
        FormValue formValue = mock(FormValue.class);
        File file = mock(File.class);
        String expectedFileName = "test.pdf";

        Deque<FormValue> deque = new ArrayDeque<>();
        deque.add(formValue);

        when(formData.get("file")).thenReturn(deque);
        when(formValue.getFileName()).thenReturn(expectedFileName);

        FormItem result = Utils.retrieveAttachmentFromFormData(formData, file);

        assertNotNull(result);
        assertEquals(file, result.getFile());
        assertEquals(expectedFileName, result.getFileName());
    }

    @Test
    void retrieveAttachmentFromFormData_shouldThrowInvalidRequestExceptionWhenTooManyAttachments() {
        FormData formData = mock(FormData.class);
        FormValue formValue1 = mock(FormValue.class);
        FormValue formValue2 = mock(FormValue.class);
        File file = mock(File.class);

        Deque<FormValue> deque = new ArrayDeque<>();
        deque.add(formValue1);
        deque.add(formValue2);

        when(formData.get("file")).thenReturn(deque);

        InvalidRequestException ex = assertThrows(
                InvalidRequestException.class,
                () -> Utils.retrieveAttachmentFromFormData(formData, file)
        );

        assertEquals("Too many attachments", ex.getMessage());
        assertEquals("0000", ex.getCode());
    }

    // ---- extractFileName ----

    @Test
    void extractFileName_shouldReturnFileNameFromPath() {
        String result = Utils.extractFileName("/some/path/to/file.pdf");
        assertEquals("file.pdf", result);
    }

    @Test
    void extractFileName_shouldReturnPathItselfWhenNoSlash() {
        String result = Utils.extractFileName("file.pdf");
        assertEquals("file.pdf", result);
    }

    @Test
    void extractFileName_shouldReturnNullWhenPathIsNull() {
        assertNull(Utils.extractFileName(null));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void extractFileName_shouldReturnNullWhenPathIsNullOrBlank(String path) {
        assertNull(Utils.extractFileName(path));
    }

    @Test
    void extractFileName_shouldReturnEmptyStringWhenPathEndsWithSlash() {
        String result = Utils.extractFileName("/some/path/");
        assertEquals("", result);
    }

    @Test
    void extractFileName_shouldHandleSingleFileNameWithoutDirectory() {
        String result = Utils.extractFileName("onlyfilename.txt");
        assertEquals("onlyfilename.txt", result);
    }
}

