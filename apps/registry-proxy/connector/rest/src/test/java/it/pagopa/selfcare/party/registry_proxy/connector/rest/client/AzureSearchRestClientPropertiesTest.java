package it.pagopa.selfcare.party.registry_proxy.connector.rest.client;

import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Guards against missing path properties: an unresolved placeholder in a Feign mapping is not a startup error,
 * it silently becomes a wrong URL at runtime (e.g. "https://host/$?...").
 */
class AzureSearchRestClientPropertiesTest {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{([^}:]+)}");

    @Test
    void allMappedPathsAreDefinedInProperties() throws IOException {
        Properties properties = new Properties();
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("config/azure-ai-search-rest-client.properties")) {
            assertNotNull(is, "azure-ai-search-rest-client.properties not found on classpath");
            properties.load(is);
        }

        List<String> keys = new ArrayList<>();
        for (Method method : AzureSearchRestClient.class.getDeclaredMethods()) {
            GetMapping get = method.getAnnotation(GetMapping.class);
            PostMapping post = method.getAnnotation(PostMapping.class);
            String[] values = get != null ? concat(get.value(), get.path())
                    : post != null ? concat(post.value(), post.path()) : new String[0];
            for (String value : values) {
                Matcher matcher = PLACEHOLDER.matcher(value);
                while (matcher.find()) {
                    keys.add(matcher.group(1));
                }
            }
        }

        assertFalse(keys.isEmpty());
        assertTrue(keys.contains("rest-client.ai-search.geotax-istat.search.path"));
        for (String key : keys) {
            assertTrue(properties.containsKey(key), "Missing property: " + key);
        }
    }

    private static String[] concat(String[] a, String[] b) {
        String[] result = new String[a.length + b.length];
        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        return result;
    }
}

