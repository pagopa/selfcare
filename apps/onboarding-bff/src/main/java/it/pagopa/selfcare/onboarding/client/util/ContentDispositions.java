package it.pagopa.selfcare.onboarding.client.util;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Extracts the file name of a {@code Content-Disposition} header: the RFC 5987 {@code filename*} parameter
 * wins over the plain {@code filename}, whose surrounding quotes are removed.
 */
public final class ContentDispositions {

    private static final String INVALID_EXTENDED =
            "Invalid header field parameter format (as defined in RFC 5987)";
    private static final Pattern BASE64_WORD = Pattern.compile("=\\?([^?]+)\\?B\\?([^?]+)\\?=");
    private static final Pattern QUOTED_WORD = Pattern.compile("=\\?([^?]+)\\?Q\\?([^?]+)\\?=");

    private ContentDispositions() {
    }

    public static String filename(String contentDisposition) {
        if (contentDisposition == null || contentDisposition.isBlank()) {
            return null;
        }
        String filename = null;
        String extendedFilename = null;
        List<String> attributes = splitAttributes(contentDisposition);
        if (attributes.get(0).isBlank()) {
            throw new IllegalArgumentException("Content-Disposition header must not be empty");
        }
        for (int index = 1; index < attributes.size(); index++) {
            String attribute = attributes.get(index).trim();
            if (attribute.isEmpty()) {
                continue;
            }
            int separator = attribute.indexOf('=');
            if (separator < 0) {
                throw new IllegalArgumentException("Invalid content disposition format");
            }
            String name = attribute.substring(0, separator).trim();
            String value = unquote(attribute.substring(separator + 1).trim());
            if ("filename*".equals(name)) {
                extendedFilename = decodeExtendedValue(value);
            } else if ("filename".equals(name) && filename == null) {
                filename = decodeEncodedWords(value);
            }
        }
        return extendedFilename != null ? extendedFilename : filename;
    }

    private static List<String> splitAttributes(String header) {
        List<String> attributes = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < header.length(); i++) {
            char c = header.charAt(i);
            if (c == '\\' && quoted && i + 1 < header.length()) {
                current.append(c).append(header.charAt(++i));
                continue;
            }
            if (c == '"') {
                quoted = !quoted;
            }
            if (c == ';' && !quoted) {
                attributes.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        attributes.add(current.toString());
        return attributes;
    }

    private static String unquote(String value) {
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1).replace("\\\"", "\"").replace("\\\\", "\\");
        }
        return value;
    }

    private static String decodeExtendedValue(String value) {
        int charsetEnd = value.indexOf('\'');
        int languageEnd = charsetEnd < 0 ? -1 : value.indexOf('\'', charsetEnd + 1);
        Charset charset = StandardCharsets.US_ASCII;
        String encoded = value;
        if (charsetEnd >= 0) {
            if (languageEnd < 0) {
                throw new IllegalArgumentException(INVALID_EXTENDED);
            }
            charset = Charset.forName(value.substring(0, charsetEnd));
            if (!StandardCharsets.UTF_8.equals(charset) && !StandardCharsets.ISO_8859_1.equals(charset)) {
                throw new IllegalArgumentException("Charset must be UTF-8 or ISO-8859-1");
            }
            encoded = value.substring(languageEnd + 1);
        }
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        for (int i = 0; i < encoded.length(); i++) {
            char c = encoded.charAt(i);
            if (c == '%') {
                if (i + 2 >= encoded.length() || !isHex(encoded.charAt(i + 1)) || !isHex(encoded.charAt(i + 2))) {
                    throw new IllegalArgumentException(INVALID_EXTENDED);
                }
                bytes.write(Integer.parseInt(encoded.substring(i + 1, i + 3), 16));
                i += 2;
            } else if (c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9'
                    || "!#$&+-.^_`|~".indexOf(c) >= 0) {
                bytes.write(c);
            } else {
                throw new IllegalArgumentException(INVALID_EXTENDED);
            }
        }
        return bytes.toString(charset);
    }

    private static boolean isHex(char c) {
        return c >= '0' && c <= '9' || c >= 'a' && c <= 'f' || c >= 'A' && c <= 'F';
    }

    private static String decodeEncodedWords(String value) {
        if (!value.startsWith("=?")) {
            return value;
        }
        var base64 = BASE64_WORD.matcher(value);
        var quoted = QUOTED_WORD.matcher(value);
        boolean isBase64 = base64.find();
        if (!isBase64 && !quoted.find()) {
            return value;
        }
        var matcher = isBase64 ? base64 : quoted;
        StringBuilder decoded = new StringBuilder();
        do {
            Charset charset = Charset.forName(matcher.group(1));
            byte[] bytes = isBase64 ? Base64.getDecoder().decode(matcher.group(2))
                    : decodeQuotedPrintable(matcher.group(2));
            decoded.append(new String(bytes, charset));
        } while (matcher.find());
        return decoded.toString();
    }

    private static byte[] decodeQuotedPrintable(String value) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '=') {
                if (i + 2 >= value.length() || !isHex(value.charAt(i + 1)) || !isHex(value.charAt(i + 2))) {
                    throw new IllegalArgumentException("Not a valid hex sequence: " + value.substring(i, Math.min(i + 3, value.length())));
                }
                bytes.write(Integer.parseInt(value.substring(i + 1, i + 3), 16));
                i += 2;
            } else {
                bytes.write(c == '_' ? ' ' : c);
            }
        }
        return bytes.toByteArray();
    }
}
