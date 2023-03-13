package com.rtbhouse.nosqllab.avro2json;

import java.time.Instant;
import java.time.LocalDate;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;

import com.fasterxml.jackson.annotation.JsonIgnoreType;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rtbhouse.nosqllab.avro2json.serde.AnyJsonDeserializer;
import com.rtbhouse.nosqllab.avro2json.serde.AnyJsonSerializer;
import com.rtbhouse.nosqllab.avro2json.serde.ArrayNodeSerializer;
import com.rtbhouse.nosqllab.avro2json.serde.InstantDeserializer;
import com.rtbhouse.nosqllab.avro2json.serde.InstantSerializer;
import com.rtbhouse.nosqllab.avro2json.serde.LocalDateDeserializer;
import com.rtbhouse.nosqllab.avro2json.serde.LocalDateSerializer;
import com.rtbhouse.nosqllab.avro2json.serde.ObjectNodeSerializer;

public final class AvroDomainJacksonSupport {

    private AvroDomainJacksonSupport() {
    }

    private static final class AvroDomainJacksonModule extends SimpleModule {
        private AvroDomainJacksonModule() {
            setMixInAnnotation(Schema.class, IgnoreType.class);
            setMixInAnnotation(GenericRecord.class, GenericRecordSerde.class);
            addDeserializer(Instant.class, new InstantDeserializer());
            addSerializer(Instant.class, new InstantSerializer());
            addDeserializer(LocalDate.class, new LocalDateDeserializer());
            addSerializer(LocalDate.class, new LocalDateSerializer());
            addDeserializer(AnyJson.class, new AnyJsonDeserializer());
            addSerializer(AnyJson.class, new AnyJsonSerializer());
            addSerializer(ObjectNode.class, new ObjectNodeSerializer());
            addSerializer(ArrayNode.class, new ArrayNodeSerializer());
        }

        @JsonIgnoreType
        private static class IgnoreType {
        }

        @JsonDeserialize(using = JacksonAvroDeserializer.class)
        @JsonSerialize(using = JacksonAvroSerializer.class)
        private static class GenericRecordSerde {
        }
    }

    public static ObjectMapper objectMapper() {
        return JsonMapper.builder()
                .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
                .build()
                .registerModule(new AvroDomainJacksonModule());
    }
}
