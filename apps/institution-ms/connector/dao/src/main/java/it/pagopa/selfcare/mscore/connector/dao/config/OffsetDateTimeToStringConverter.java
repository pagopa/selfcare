package it.pagopa.selfcare.mscore.connector.dao.config;

import org.springframework.core.convert.converter.Converter;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

class OffsetDateTimeToStringConverter implements Converter<OffsetDateTime, String> {

    @Override
    public String convert(OffsetDateTime source) {
        return source.toInstant().atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

}
