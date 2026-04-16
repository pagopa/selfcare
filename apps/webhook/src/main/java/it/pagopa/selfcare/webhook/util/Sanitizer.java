package it.pagopa.selfcare.webhook.util;

public class Sanitizer {

  /**
   * Sanitize user input for safe logging: remove line breaks, control characters, and allow only
   * alphanumerics plus minimal punctuation (underscore, dash).
   */
  public static String sanitizeString(String input) {
    if (input == null) {
      return null;
    }
    // Remove all non-alphanumerics, dash, and underscore
    return input.replaceAll("[^A-Za-z0-9_-]", "");
  }
}
