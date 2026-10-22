package it.pagopa.selfcare.commons.tenant.mongodb;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.convert.NoOpDbRefResolver;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;

class TenantMongoConfigurationTest {

    @Test
    void usesConfiguredConverterAndRetainsCustomConversions() throws Exception {
        MongoCustomConversions conversions = new MongoCustomConversions(List.of(
                new OffsetDateTimeToStringConverter(),
                new StringToOffsetDateTimeConverter()));
        MappingMongoConverter converter = mappingConverter(conversions);
        MongoTemplate template = new TenantMongoConfiguration()
                .mongoTemplate(mock(MongoDatabaseFactory.class), converter);
        OffsetDateTime value = OffsetDateTime.parse("2025-03-04T12:30:00+03:00");
        Document document = new Document();

        template.getConverter().write(new SampleEntity(value, "kept"), document);

        assertSame(converter, template.getConverter());
        assertEquals("2025-03-04T09:30:00Z", document.getString("createdAt"));
        assertEquals("kept", document.getString("label"));
        assertEquals(value.toInstant(), template.getConverter()
                .read(SampleEntity.class, document).createdAt().toInstant());
    }

    @Test
    void retainsStandardMappingWhenNoCustomConversionsAreRegistered() throws Exception {
        MappingMongoConverter converter = mappingConverter(MongoCustomConversions.create(adapter -> {}));
        MongoTemplate template = new TenantMongoConfiguration()
                .mongoTemplate(mock(MongoDatabaseFactory.class), converter);
        Document document = new Document();

        template.getConverter().write(new SampleEntity(null, "kept"), document);

        assertSame(converter, template.getConverter());
        assertEquals("kept", document.getString("label"));
        assertEquals("kept", template.getConverter().read(SampleEntity.class, document).label());
    }

    private MappingMongoConverter mappingConverter(MongoCustomConversions conversions)
            throws Exception {
        MongoMappingContext mappingContext = new MongoMappingContext();
        mappingContext.setSimpleTypeHolder(conversions.getSimpleTypeHolder());
        mappingContext.afterPropertiesSet();
        MappingMongoConverter converter =
                new MappingMongoConverter(NoOpDbRefResolver.INSTANCE, mappingContext);
        converter.setCustomConversions(conversions);
        converter.afterPropertiesSet();
        return converter;
    }

    private record SampleEntity(OffsetDateTime createdAt, String label) {}

    private static class OffsetDateTimeToStringConverter
            implements Converter<OffsetDateTime, String> {
        @Override
        public String convert(OffsetDateTime source) {
            return source.toInstant().atOffset(ZoneOffset.UTC)
                    .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        }
    }

    private static class StringToOffsetDateTimeConverter
            implements Converter<String, OffsetDateTime> {
        @Override
        public OffsetDateTime convert(String source) {
            return OffsetDateTime.parse(source);
        }
    }
}
