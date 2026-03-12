package it.pagopa.selfcare.onboarding.util;


import static org.junit.jupiter.api.Assertions.*;

import it.pagopa.selfcare.onboarding.exception.InvalidRequestException;
import java.io.File;
import org.jboss.resteasy.reactive.server.core.multipart.FormData;
import org.junit.jupiter.api.Test;

public class UtilsTest {

  @Test
  void retrieveContractFromFormData() {
    File file = new File("test.pdf");
    FormData formData = new FormData(2);
    formData.add("contract", "test");
    formData.add("contract", file.getName());

    assertThrows(InvalidRequestException.class,
            () -> Utils.retrieveContractFromFormData(formData, file));
  }

  @Test
  void getFileExtension() {
    final var filename = "test.pdf";
    var extension = Utils.getFileExtension(filename);
    assertEquals("pdf", extension);
  }

  @Test
  void getFileExtensionWithTwoParts() {
    final var filename = "index/test.pdf";
    var extension = Utils.getFileExtension(filename);
    assertEquals("pdf", extension);
  }

  @Test
  void getFileExtensionWithMoreThanTwoParts() {
    final var filename = "index/test/test.pdf";
    var extension = Utils.getFileExtension(filename);
    assertEquals("pdf", extension);
  }

  @Test
  void replaceExtension() {
    final var filename = "index/test/test.pdf";
    var extension = Utils.replaceFileExtension(filename, "p7m");
    assertEquals("index/test/test.p7m", extension);
  }

  @Test
  void extractName() {
    final var filename = "index/test/test.pdf";
    var extension = Utils.extractFileName(filename);
    assertEquals("test.pdf", extension);
  }

  @Test
  void extractNameWithNullPath() {
    var extension = Utils.extractFileName(null);
    assertNull(extension);
  }

  @Test
  void extractFileNameEmptyPath() {
    assertNull(Utils.extractFileName(""));
  }

  @Test
  void extractFileNameBlankPath() {
    assertNull(Utils.extractFileName("   "));
  }

  @Test
  void extractFileNameSimpleFileName() {
    assertEquals("test.pdf", Utils.extractFileName("test.pdf"));
  }

  @Test
  void extractFileNameSingleDirectoryPath() {
    assertEquals("test.pdf", Utils.extractFileName("dir/test.pdf"));
  }

  @Test
  void extractFileNameMultipleDirectoryPath() {
    assertEquals("test.pdf", Utils.extractFileName("dir/subdir/another/test.pdf"));
  }

  @Test
  void extractFileNamePathEndsWithSlash() {
    assertEquals("", Utils.extractFileName("dir/subdir/"));
  }

  @Test
  void extractFileNameRootPathOnly() {
    assertEquals("", Utils.extractFileName("/"));
  }

  @Test
  void extractFileNameWithoutExtension() {
    assertEquals("file", Utils.extractFileName("dir/file"));
  }

  @Test
  void extractFileNameWithMultipleDots() {
    assertEquals("my.file.name.pdf", Utils.extractFileName("path/to/my.file.name.pdf"));
  }


}
