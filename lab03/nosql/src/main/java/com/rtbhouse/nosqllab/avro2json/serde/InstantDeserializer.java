package com.rtbhouse.nosqllab.avro2json.serde;

import java.io.IOException;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

public class InstantDeserializer extends JsonDeserializer<Instant> {
    @Override
    public Instant deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String value = p.getValueAsString();
        if (value == null) {
            return null;
        }
        try {
            return DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(value, ZonedDateTime::from).toInstant();
        } catch (DateTimeException e) {
            throw new IllegalArgumentException(
                    String.format(
                            "Not allowed value: '%s'. Expected format is ISO_OFFSET_DATE_TIME for example: 2011-12-03T10:15:30Z",
                            value));
        }
    }
}
