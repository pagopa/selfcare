package it.pagopa.selfcare.party.registry_proxy.connector.rest.utils;

import it.pagopa.selfcare.party.registry_proxy.connector.model.InsuranceCompany;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class IvassUtilsTest {

    @InjectMocks
    private IvassUtils ivassUtil;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void extractSingleFileFromZip_shouldReturnByteArray_whenZipContainsSingleFile() throws IOException {
        ClassPathResource resource = new ClassPathResource("one_file.zip");
        byte[] zipBytes = java.nio.file.Files.readAllBytes(resource.getFile().toPath());

        byte[] result = ivassUtil.extractFirstEntryByteArrayFromZip(zipBytes);

        assertNotNull(result);
        assertTrue(result.length > 0);
    }

    @Test
    void extractSingleFileFromZip_shouldReturnEmptyByteArray_whenZipIsEmpty() throws IOException {
        ClassPathResource resource = new ClassPathResource("empty.zip");
        byte[] zipBytes = java.nio.file.Files.readAllBytes(resource.getFile().toPath());

        byte[] result = ivassUtil.extractFirstEntryByteArrayFromZip(zipBytes);

        assertNotNull(result);
        assertEquals(0, result.length);
    }

    @Test
    void readCsv_shouldReturnListOfCompanies_whenCsvIsValid() throws IOException {
        ClassPathResource resource = new ClassPathResource("ivass-open-data.csv");
        byte[] csvBytes = java.nio.file.Files.readAllBytes(resource.getFile().toPath());

        List<InsuranceCompany> result = ivassUtil.readCsv(csvBytes);

        // Assert that the result is not null and has the expected size
        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    void readCsv_shouldReturnEmptyList_whenCsvIsEmpty() {
        byte[] csvBytes = new byte[0];

        List<InsuranceCompany> result = ivassUtil.readCsv(csvBytes);

        // Assert that the result is not null and is empty
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void readCsv_shouldReturnEmptyList_whenCsvIsInvalid() throws IOException {
        ClassPathResource resource = new ClassPathResource("ivass-open-data-malformed.csv");
        byte[] csvBytes = java.nio.file.Files.readAllBytes(resource.getFile().toPath());

        List<InsuranceCompany> result = ivassUtil.readCsv(csvBytes);

        // Assert that the result is not null and is empty
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void manageUTF8BOM_shouldRemoveBOM_whenBOMPresent() {
        byte[] csvWithBOM = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF, 'a', 'b', 'c'};
        byte[] expectedCsv = {'a', 'b', 'c'};

        byte[] result = ivassUtil.manageUTF8BOM(csvWithBOM);

        assertArrayEquals(expectedCsv, result);
    }

    @Test
    void manageUTF8BOM_shouldReturnSameArray_whenBOMNotPresent() {
        byte[] csvWithoutBOM = {'a', 'b', 'c'};
        byte[] expectedCsv = {'a', 'b', 'c'};

        byte[] result = ivassUtil.manageUTF8BOM(csvWithoutBOM);

        assertArrayEquals(expectedCsv, result);
    }

}