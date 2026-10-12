package it.pagopa.selfcare.onboarding.parity;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MultipartTest {

  @Test
  void roundTripsFieldsAndBinaryFiles() {
    byte[] binary = new byte[256];
    for (int i = 0; i < binary.length; i++) {
      binary[i] = (byte) i;
    }
    Multipart.Builder builder =
        Multipart.body()
            .field("attachmentId", "att-1")
            .field("attachmentDescription", "descrizione con àccénti")
            .file("attachment", "doc.pdf", "application/pdf", binary);

    List<Multipart.Part> parts = Multipart.parse(builder.contentType(), builder.bytes());

    assertEquals(3, parts.size());
    assertEquals("attachmentId", parts.get(0).name());
    assertNull(parts.get(0).filename());
    assertNull(parts.get(0).contentType());
    assertEquals("att-1", parts.get(0).text());
    assertEquals("attachmentDescription", parts.get(1).name());
    assertEquals("descrizione con àccénti", parts.get(1).text());
    assertEquals("attachment", parts.get(2).name());
    assertEquals("doc.pdf", parts.get(2).filename());
    assertEquals("application/pdf", parts.get(2).contentType());
    assertArrayEquals(binary, parts.get(2).content());
  }

  @Test
  void parsesBodiesWithQuotedBoundaryAndPartNamesContainingFilename() {
    String body =
        "--XyZ\r\nContent-Disposition: form-data; name=\"filename\"\r\n\r\nvalue\r\n"
            + "--XyZ\r\nContent-Disposition: form-data; name=\"f\"; filename=\"a.p7m\"\r\n"
            + "Content-Type: application/pkcs7-mime\r\n\r\nSIG\r\n--XyZ--\r\n";

    List<Multipart.Part> parts = Multipart.parse("multipart/form-data; boundary=\"XyZ\"", body.getBytes(StandardCharsets.UTF_8));

    assertEquals(2, parts.size());
    assertEquals("filename", parts.get(0).name());
    assertNull(parts.get(0).filename());
    assertEquals("f", parts.get(1).name());
    assertEquals("a.p7m", parts.get(1).filename());
    assertEquals("application/pkcs7-mime", parts.get(1).contentType());
    assertEquals("SIG", parts.get(1).text());
  }

  @Test
  void nonMultipartContentIsNotParsed() {
    assertTrue(Multipart.parse(null, new byte[0]).isEmpty());
    assertTrue(Multipart.parse("application/json", "{}".getBytes(StandardCharsets.UTF_8)).isEmpty());
    assertTrue(Multipart.parse("multipart/form-data", "x".getBytes(StandardCharsets.UTF_8)).isEmpty());
  }

  @Test
  void preservesBoundaryLikeContentAndTrailingLineBreaks() {
    String content = "prefix--XyZsuffix\r\n--XyZnot-a-delimiter\r\n\r\n";
    String body =
        "--XyZ\r\nContent-Disposition: form-data; name=\"file\"; filename=\"data.bin\"\r\n"
            + "Content-Type: application/octet-stream\r\n\r\n"
            + content
            + "\r\n--XyZ--\r\n";

    List<Multipart.Part> parts =
        Multipart.parse("multipart/form-data; boundary=XyZ", body.getBytes(StandardCharsets.ISO_8859_1));

    assertEquals(1, parts.size());
    assertArrayEquals(content.getBytes(StandardCharsets.ISO_8859_1), parts.get(0).content());
  }

  @Test
  void queryParametersKeepRepeatedValuesOrderAndDecoding() {
    Map<String, List<String>> query = Multipart.queryParams("fl=name&fl=familyName&q=a%20b%2Bc&flag&empty=");

    assertEquals(List.of("name", "familyName"), query.get("fl"));
    assertEquals(List.of("a b+c"), query.get("q"));
    assertEquals(List.of(""), query.get("flag"));
    assertEquals(List.of(""), query.get("empty"));
    assertEquals(List.of("fl", "q", "flag", "empty"), List.copyOf(query.keySet()));
    assertTrue(Multipart.queryParams(null).isEmpty());
    assertTrue(Multipart.queryParams("").isEmpty());
  }
}
