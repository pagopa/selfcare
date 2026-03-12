package it.pagopa.selfcare.onboarding.utils;

import com.openhtmltopdf.extend.FSStream;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class ClassPathStream implements FSStream {

    private final String uri;

    public ClassPathStream(String path) {
        this.uri = path;
    }

    @Override
    public InputStream getStream() {
        return this.getClass().getResourceAsStream(uri);
    }

    @Override
    public Reader getReader() {
        return new InputStreamReader(Objects.requireNonNull(this.getClass().getResourceAsStream(uri)), StandardCharsets.UTF_8);
    }
}
