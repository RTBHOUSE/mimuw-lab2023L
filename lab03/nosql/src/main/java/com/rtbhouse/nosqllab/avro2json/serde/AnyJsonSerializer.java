package com.rtbhouse.nosqllab.avro2json.serde;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.rtbhouse.nosqllab.avro2json.AnyJson;

public class AnyJsonSerializer extends JsonSerializer<AnyJson> {
    @Override
    public void serialize(AnyJson value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (value != null) {
            value.serialize(gen, serializers);
        } else {
            gen.writeNull();
        }
    }
}
