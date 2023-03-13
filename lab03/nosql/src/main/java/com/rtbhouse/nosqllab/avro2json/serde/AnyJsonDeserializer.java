package com.rtbhouse.nosqllab.avro2json.serde;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.rtbhouse.nosqllab.avro2json.AnyJson;

public class AnyJsonDeserializer extends JsonDeserializer<AnyJson> {
    @Override
    public AnyJson deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        return AnyJson.fromJsonNode(p.readValueAsTree());
    }
}
