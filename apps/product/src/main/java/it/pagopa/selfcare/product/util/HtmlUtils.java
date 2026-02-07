package it.pagopa.selfcare.product.util;

import java.io.File;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Cleaner;
import org.jsoup.safety.Safelist;

@Slf4j
public class HtmlUtils {

  private static final Safelist HTML_SAFELIST =
      Safelist.relaxed().addTags("section", "article", "hr").removeTags("img");

  private HtmlUtils() {}

  public static boolean isValidHTML(File file) {
    try {
      final Document dirtyDocument = Jsoup.parse(file);
      final Cleaner cleaner = new Cleaner(HTML_SAFELIST);
      return cleaner.isValid(dirtyDocument);
    } catch (Exception ex) {
      log.error("Unable to validate HTML file", ex);
      return false;
    }
  }

  public static String getCleanHTML(File file) {
    try {
      final Document dirtyDocument = Jsoup.parse(file);
      final Cleaner cleaner = new Cleaner(HTML_SAFELIST);
      final Document cleanDocument = cleaner.clean(dirtyDocument);
      cleanDocument.outputSettings().prettyPrint(true).indentAmount(4);
      return cleanDocument.body().html();
    } catch (Exception ex) {
      log.error("Unable to clean HTML file", ex);
      return null;
    }
  }
}
