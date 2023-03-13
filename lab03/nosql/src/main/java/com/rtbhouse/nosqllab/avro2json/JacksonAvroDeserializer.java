package com.rtbhouse.nosqllab.avro2json;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import org.apache.avro.Conversion;
import org.apache.avro.LogicalType;
import org.apache.avro.LogicalTypes;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.specific.SpecificData;
import org.apache.avro.specific.SpecificRecordBase;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

class JacksonAvroDeserializer extends StdDeserializer<Object> implements ContextualDeserializer {

    private static final Map<Schema, JavaType> TYPE_CACHE = new ConcurrentHashMap<>();
    private static final Map<JavaType, Schema> SCHEMA_CACHE = new ConcurrentHashMap<>();

    private final JavaType type;

    // this deserializer is always contextualized via ContextualDeserializer callback
    // so this constructor exists only for Mixin purposes as deserializer passed in JsonDeserializer
    // must have 0-arg constructor
    public JacksonAvroDeserializer() {
        super(Object.class);
        this.type = null;
    }

    protected JacksonAvroDeserializer(JavaType type) {
        super(type.getClass());
        this.type = type;
    }

    @Override
    public GenericRecord deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        return doDeserialize(p, ctxt, getSchemaFromType());
    }

    private GenericRecord doDeserialize(JsonParser p, DeserializationContext ctxt, Schema schema) throws IOException {
        GenericRecord instance = (GenericRecord) SpecificData.newInstance(type.getRawClass(), schema);

        while (true) {
            JsonToken token = p.nextToken(); // advance to field name
            if (token == JsonToken.END_OBJECT) {
                break;
            }
            if (token != JsonToken.FIELD_NAME) {
                throw new JsonParseException(p, "Field name expected");
            }
            String jsonFieldName = p.getCurrentName();
            p.nextToken(); // advance to field value
            Schema.Field field = getSchemaFieldForJsonField(schema, jsonFieldName);
            if (field == null) {
                if (ctxt.isEnabled(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)) {
                    throw new JsonMappingException(p, "Unrecognized field '" + jsonFieldName + "'");
                } else {
                    p.skipChildren();
                }
            } else {
                if (p.currentToken() == JsonToken.VALUE_NULL) {
                    instance.put(field.pos(), null);
                } else {
                    try {
                        instance.put(field.pos(),
                                ctxt.readValue(p, cachedTypeOf(instance, field.schema(), ctxt)));
                    } catch (Exception e) {
                        throw JsonMappingException.wrapWithPath(e, instance, field.name());
                    }
                }
            }
        }
        return instance;
    }

    protected Schema.Field getSchemaFieldForJsonField(Schema schema, String jsonField) {
        return schema.getField(jsonField);
    }

    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) {
        return new JacksonAvroDeserializer(ctxt.getContextualType());
    }

    private Schema getSchemaFromType() {
        return computeIfAbsent(SCHEMA_CACHE, type, key -> {
            try {
                // getClass() is instance method so field is accessed directly
                return (Schema) type.getRawClass().getDeclaredField("SCHEMA$").get(null);
            } catch (IllegalAccessException | NoSuchFieldException e) {
                throw new RuntimeException("No SCHEMA$ field on avro object.");
            }
        });
    }

    private static JavaType cachedTypeOf(GenericRecord instance, Schema schema, DeserializationContext ctxt) {
        return computeIfAbsent(TYPE_CACHE, schema, key -> typeOf(instance, schema, ctxt));
    }

    private static <K, V> V computeIfAbsent(Map<K, V> map, K key, Function<? super K, ? extends V> mappingFunction) {
        V value = map.get(key);
        if (value != null) {
            return value;
        }
        return map.computeIfAbsent(key, mappingFunction);
    }

    private static JavaType typeOf(GenericRecord instance, Schema schema, DeserializationContext ctxt) {
        // if type is logical type try to resolve by registered conversions
        if (schema.getLogicalType() != null) {
            Class<?> resolvedLogicalType = resolveLogicalType(instance, schema);
            if (resolvedLogicalType != null) {
                return ctxt.constructType(resolvedLogicalType);
            }
        }

        // otherwise try standard mapping
        switch (schema.getType()) {
        case INT:
            return ctxt.constructType(Integer.class);
        case LONG:
            return ctxt.constructType(Long.class);
        case BYTES:
            return ctxt.constructType(byte[].class);
        case STRING:
            return ctxt.constructType(String.class);
        case FLOAT:
            return ctxt.constructType(Float.class);
        case DOUBLE:
            return ctxt.constructType(Double.class);
        case BOOLEAN:
            return ctxt.constructType(Boolean.class);
        case UNION:
            List<Schema> types = schema.getTypes();
            if (types.size() != 2 || types.get(0).getType() != Schema.Type.NULL) {
                throw new UnsupportedOperationException(
                        "Custom jackson deserializer supports only [\"null\", type] unions");
            }
            return typeOf(instance, types.get(1), ctxt);
        case ARRAY:
            return ctxt.getTypeFactory().constructCollectionLikeType(List.class,
                    typeOf(instance, schema.getElementType(), ctxt));
        case MAP:
            return ctxt.getTypeFactory().constructMapLikeType(Map.class, ctxt.constructType(String.class),
                    typeOf(instance, schema.getValueType(), ctxt));
        case ENUM:
        case RECORD:
            return ctxt.constructType(classFor(schema.getFullName()));
        default:
            throw new UnsupportedOperationException(
                    "Custom jackson deserializer does not support avro type " + schema.getType());
        }
    }

    private static Class<?> resolveLogicalType(GenericRecord instance, Schema schema) {
        LogicalType logicalType = LogicalTypes.fromSchemaIgnoreInvalid(schema);
        if (logicalType != null && instance instanceof SpecificRecordBase) {
            SpecificRecordBase specificRecord = (SpecificRecordBase) instance;
            Conversion<?> conversion = specificRecord.getSpecificData().getConversionFor(logicalType);
            if (conversion != null) {
                return conversion.getConvertedType();
            }
        }
        return null;
    }

    private static Class<?> classFor(String qualifiedClassName) {
        try {
            return Class.forName(qualifiedClassName);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("No class found for " + qualifiedClassName);
        }
    }

}
