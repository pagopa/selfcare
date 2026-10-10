package it.pagopa.selfcare.onboarding.parity;

import java.io.ByteArrayOutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Minimal multipart/form-data builder and parser plus query-string helpers for the harness. */
public final class Multipart {

  /** A parsed (or to-be-sent) part. */
  public record Part(String name, String filename, String contentType, byte[] content) {

    public String text() {
      return new String(content, StandardCharsets.UTF_8);
    }
  }

  private Multipart() {}

  /** Request body builder: {@code Multipart.body().field("a","b").file("contract","c.pdf", "application/pdf", bytes)}. */
  public static Builder body() {
    return new Builder();
  }

  public static final class Builder {
    private final String boundary = "----parity" + UUID.randomUUID().toString().replace("-", "");
    private final ByteArrayOutputStream out = new ByteArrayOutputStream();

    public Builder field(String name, String value) {
      write("--" + boundary + "\r\n");
      write("Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n");
      write(value);
      write("\r\n");
      return this;
    }

    public Builder file(String name, String filename, String contentType, byte[] content) {
      write("--" + boundary + "\r\n");
      write(
          "Content-Disposition: form-data; name=\""
              + name
              + "\"; filename=\""
              + filename
              + "\"\r\n");
      write("Content-Type: " + contentType + "\r\n\r\n");
      out.writeBytes(content);
      write("\r\n");
      return this;
    }

    public String contentType() {
      return "multipart/form-data; boundary=" + boundary;
    }

    public byte[] bytes() {
      ByteArrayOutputStream copy = new ByteArrayOutputStream();
      copy.writeBytes(out.toByteArray());
      copy.writeBytes(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
      return copy.toByteArray();
    }

    private void write(String text) {
      out.writeBytes(text.getBytes(StandardCharsets.UTF_8));
    }
  }

  private static final Pattern BOUNDARY = Pattern.compile("boundary=\"?([^\";]+)\"?");
  private static final Pattern NAME = Pattern.compile("[; ]name=\"([^\"]*)\"");
  private static final Pattern FILENAME = Pattern.compile("filename=\"([^\"]*)\"");

  /** Parses a multipart body as received by a downstream; empty when it is not multipart. */
  public static List<Part> parse(String contentType, byte[] body) {
    List<Part> parts = new ArrayList<>();
    if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("multipart/")) {
      return parts;
    }
    Matcher boundaryMatcher = BOUNDARY.matcher(contentType);
    if (!boundaryMatcher.find()) {
      return parts;
    }
    String text = new String(body, StandardCharsets.ISO_8859_1);
    String delimiter = "--" + boundaryMatcher.group(1);
    String separator = "(?:\\A|(?<=\\r\\n))" + Pattern.quote(delimiter) + "(?=\\r\\n|--(?:\\r\\n|\\z))";
    for (String chunk : text.split(separator)) {
      if (chunk.isBlank() || chunk.startsWith("--")) {
        continue;
      }
      int headersEnd = chunk.indexOf("\r\n\r\n");
      if (headersEnd < 0) {
        continue;
      }
      String headerBlock = chunk.substring(0, headersEnd);
      String content = chunk.substring(headersEnd + 4);
      if (content.endsWith("\r\n")) {
        content = content.substring(0, content.length() - 2);
      }
      String disposition = null;
      String partType = null;
      for (String line : headerBlock.split("\r\n")) {
        String lower = line.toLowerCase(Locale.ROOT);
        if (lower.startsWith("content-disposition:")) {
          disposition = line;
        } else if (lower.startsWith("content-type:")) {
          partType = line.substring("content-type:".length()).trim();
        }
      }
      if (disposition == null) {
        continue;
      }
      Matcher nameMatcher = NAME.matcher(disposition);
      Matcher filenameMatcher = FILENAME.matcher(disposition);
      parts.add(
          new Part(
              nameMatcher.find() ? nameMatcher.group(1) : null,
              filenameMatcher.find() ? filenameMatcher.group(1) : null,
              partType,
              content.getBytes(StandardCharsets.ISO_8859_1)));
    }
    return parts;
  }

  public static Map<String, List<String>> queryParams(String rawQuery) {
    Map<String, List<String>> result = new LinkedHashMap<>();
    if (rawQuery == null || rawQuery.isEmpty()) {
      return result;
    }
    for (String pair : rawQuery.split("&")) {
      int idx = pair.indexOf('=');
      String key = URLDecoder.decode(idx < 0 ? pair : pair.substring(0, idx), StandardCharsets.UTF_8);
      String value =
          idx < 0 ? "" : URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8);
      result.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
    }
    return result;
  }
}
