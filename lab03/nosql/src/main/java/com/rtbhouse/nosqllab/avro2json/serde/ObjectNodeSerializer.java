package com.rtbhouse.nosqllab.avro2json.serde;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Iterators;

public class ObjectNodeSerializer extends JsonSerializer<ObjectNode> {

    @Override
    public void serialize(ObjectNode value, JsonGenerator gen, SerializerProvider provider)
            throws IOException {
        if (value == null) {
            gen.writeNull();
            return;
        }

        // map serializing is aware of parameter mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        Map<String, JsonNode> valueAsMap = new AbstractMap<>() {
            @Override
            public Set<Entry<String, JsonNode>> entrySet() {
                return new AbstractSet<>() {
                    @Override
                    public Iterator<Entry<String, JsonNode>> iterator() {
                        return Iterators.transform(value.fields(),
                                field -> Pair.of(field.getKey(), Optional.of(field.getValue())
                                        .map(val -> val.isNull() ? null : val).orElse(null)));
                    }

                    @Override
                    public int size() {
                        return value.size();
                    }
                };
            }
        };

        provider.defaultSerializeValue(valueAsMap, gen);
    }
}
