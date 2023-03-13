package com.rtbhouse.nosqllab.avro2json.serde;

import java.io.IOException;
import java.time.DateTimeException;
import java.time.LocalDate;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

public class LocalDateDeserializer extends JsonDeserializer<LocalDate> {
    @Override
    public LocalDate deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String value = p.getValueAsString();
        if (value == null) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeException e) {
            throw new IllegalArgumentException(
                    String.format(
                            "Not allowed value: '%s'. Expected format is ISO_LOCAL_DATE for example: 2011-12-03",
                            value));
        }
    }
}
