package it.pagopa.selfcare.party.registry_proxy.connector.rest.decoder;


import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import feign.Util;
import feign.codec.Decoder;
import org.springframework.context.annotation.Bean;
public class FeignXmlDecoder {

    @Bean
    public Decoder feignDecoder() {
        return (response, type) -> {
            String bodyStr = Util.toString(response.body().asReader(Util.UTF_8));
            JavaType javaType = TypeFactory.defaultInstance().constructType(type);
            return new XmlMapper().readValue(bodyStr, javaType);
        };
    }
}
