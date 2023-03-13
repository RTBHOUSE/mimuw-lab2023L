package com.rtbhouse.nosqllab.avro2json;

import static org.springframework.http.converter.StringHttpMessageConverter.DEFAULT_CHARSET;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.avro.specific.SpecificRecordBase;
import org.apache.commons.compress.utils.IOUtils;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;

public class AvroJsonHttpMessageConverter<T extends SpecificRecordBase> extends AbstractHttpMessageConverter<T> {

    private static final String APPLICATION = "application";
    private static final String AVRO_JSON_SUBTYPE = "avro-json";

    static final MediaType AVRO_JSON_MEDIA_TYPE = new MediaType(APPLICATION, AVRO_JSON_SUBTYPE, DEFAULT_CHARSET);

    public static final String AVRO_JSON = APPLICATION + "/" + AVRO_JSON_SUBTYPE;

    public AvroJsonHttpMessageConverter() {
        super(AVRO_JSON_MEDIA_TYPE);
    }

    /**
     * Reading accepted message and providing deserialization
     *
     * @param aClass       - target Avro object type
     * @param inputMessage - accepted message
     * @return {@code T} - converted Avro object
     * @throws IOException - on read/write issues
     */
    @SuppressWarnings("unchecked")
    @Override
    protected T readInternal(final Class<? extends T> aClass, HttpInputMessage inputMessage) throws IOException {
        final byte[] data = IOUtils.toByteArray(inputMessage.getBody());
        if (data != null && data.length > 0) {
            return AvroDomainJacksonSupport.objectMapper().readValue(data, aClass);
        }
        return null;
    }

    /**
     * Serializing and writing Avro object into outgoing message
     *
     * @param t                 - Avro object
     * @param httpOutputMessage - outgoing message
     * @throws IOException - on read/write issues
     */
    @Override
    protected void writeInternal(final T t, final HttpOutputMessage httpOutputMessage) throws IOException {
        final byte[] data = (t.toString() + "\n").getBytes(StandardCharsets.UTF_8);
        httpOutputMessage.getBody().write(data);
    }

    /**
     * Indicates whether the given converter is suitable for a class type
     *
     * @param aClass - class type
     * @return {@code boolean} value indicating whether objects of the type {@code SpecificRecordBase}
     * can be assigned to objects of {@code aClass}
     */
    @Override
    protected boolean supports(final Class<?> aClass) {
        return SpecificRecordBase.class.isAssignableFrom(aClass);
    }
}