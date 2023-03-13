package com.rtbhouse.nosqllab.avro2json;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.Consumer;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonParser.NumberType;
import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.google.common.base.Preconditions;

public final class AnyJson extends JsonNode {
    private static final ObjectMapper MAPPER;
    private static final ObjectMapper SKIPPING_NULLS_MAPPER;
    static {
        MAPPER = AvroDomainJacksonSupport.objectMapper();
        MAPPER.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

        SKIPPING_NULLS_MAPPER = MAPPER.copy();
        SKIPPING_NULLS_MAPPER.setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL);
    }

    public static final AnyJson EMPTY = AnyJson.fromJsonNode(MAPPER.createObjectNode());

    public static AnyJson fromObject(Object object) {
        return object != null ? new AnyJson(MAPPER.convertValue(object, JsonNode.class)) : null;
    }

    public static AnyJson fromString(String json) {
        if (json != null) {
            try {
                return new AnyJson(MAPPER.readTree(json));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }

    public static AnyJson fromJsonNode(JsonNode jsonNode) {
        return jsonNode != null ? new AnyJson(jsonNode) : null;
    }

    private final JsonNode jsonNode;

    private transient volatile Integer hashCode;

    private AnyJson(JsonNode jsonNode) {
        this.jsonNode = Objects.requireNonNull(jsonNode);
    }

    public AnyJson wihoutNullProperties() {
        return new AnyJson(SKIPPING_NULLS_MAPPER.convertValue(jsonNode, JsonNode.class));
    }

    @Override
    public void serialize(JsonGenerator gen, SerializerProvider serializers) throws IOException {
        serializers.defaultSerializeValue(jsonNode, gen);
    }

    @Override
    public void serializeWithType(JsonGenerator gen, SerializerProvider serializers, TypeSerializer typeSer)
            throws IOException {
        serializers.defaultSerializeValue(jsonNode, gen);
    }

    @Override
    public void forEach(Consumer<? super JsonNode> action) {
        jsonNode.forEach(action);
    }

    @Override
    public JsonToken asToken() {
        return jsonNode.asToken();
    }

    @Override
    public NumberType numberType() {
        return jsonNode.numberType();
    }

    @Override
    public Spliterator<JsonNode> spliterator() {
        return jsonNode.spliterator();
    }

    @Override
    public <T extends JsonNode> T deepCopy() {
        return jsonNode.deepCopy();
    }

    @Override
    public boolean isEmpty(SerializerProvider serializers) {
        return jsonNode.isEmpty(serializers);
    }

    @Override
    public int size() {
        return jsonNode.size();
    }

    @Override
    public boolean isEmpty() {
        return jsonNode.isEmpty();
    }

    @Override
    public boolean isMissingNode() {
        return jsonNode.isMissingNode();
    }

    @Override
    public boolean isArray() {
        return jsonNode.isArray();
    }

    @Override
    public boolean isObject() {
        return jsonNode.isObject();
    }

    @Override
    public JsonNode get(int index) {
        return jsonNode.get(index);
    }

    @Override
    public JsonNode get(String fieldName) {
        return jsonNode.get(fieldName);
    }

    @Override
    public JsonNode path(String fieldName) {
        return jsonNode.path(fieldName);
    }

    @Override
    public JsonNode path(int index) {
        return jsonNode.path(index);
    }

    @Override
    public Iterator<String> fieldNames() {
        return jsonNode.fieldNames();
    }

    @Override
    public JsonNodeType getNodeType() {
        return jsonNode.getNodeType();
    }

    @Override
    public JsonParser traverse() {
        return jsonNode.traverse();
    }

    @Override
    public boolean isIntegralNumber() {
        return jsonNode.isIntegralNumber();
    }

    @Override
    public boolean isFloatingPointNumber() {
        return jsonNode.isFloatingPointNumber();
    }

    @Override
    public boolean isShort() {
        return jsonNode.isShort();
    }

    @Override
    public JsonParser traverse(ObjectCodec codec) {
        return jsonNode.traverse(codec);
    }

    @Override
    public boolean isInt() {
        return jsonNode.isInt();
    }

    @Override
    public boolean isLong() {
        return jsonNode.isLong();
    }

    @Override
    public boolean isFloat() {
        return jsonNode.isFloat();
    }

    @Override
    public boolean isDouble() {
        return jsonNode.isDouble();
    }

    @Override
    public boolean isBigDecimal() {
        return jsonNode.isBigDecimal();
    }

    @Override
    public boolean isBigInteger() {
        return jsonNode.isBigInteger();
    }

    @Override
    public boolean canConvertToInt() {
        return jsonNode.canConvertToInt();
    }

    @Override
    public boolean canConvertToLong() {
        return jsonNode.canConvertToLong();
    }

    @Override
    public String textValue() {
        return jsonNode.textValue();
    }

    @Override
    public byte[] binaryValue() throws IOException {
        return jsonNode.binaryValue();
    }

    @Override
    public boolean booleanValue() {
        return jsonNode.booleanValue();
    }

    @Override
    public Number numberValue() {
        return jsonNode.numberValue();
    }

    @Override
    public short shortValue() {
        return jsonNode.shortValue();
    }

    @Override
    public int intValue() {
        return jsonNode.intValue();
    }

    @Override
    public long longValue() {
        return jsonNode.longValue();
    }

    @Override
    public float floatValue() {
        return jsonNode.floatValue();
    }

    @Override
    public double doubleValue() {
        return jsonNode.doubleValue();
    }

    @Override
    public BigDecimal decimalValue() {
        return jsonNode.decimalValue();
    }

    @Override
    public BigInteger bigIntegerValue() {
        return jsonNode.bigIntegerValue();
    }

    @Override
    public String asText() {
        return jsonNode.asText();
    }

    @Override
    public String asText(String defaultValue) {
        return jsonNode.asText(defaultValue);
    }

    @Override
    public int asInt() {
        return jsonNode.asInt();
    }

    @Override
    public int asInt(int defaultValue) {
        return jsonNode.asInt(defaultValue);
    }

    @Override
    public long asLong() {
        return jsonNode.asLong();
    }

    @Override
    public long asLong(long defaultValue) {
        return jsonNode.asLong(defaultValue);
    }

    @Override
    public double asDouble() {
        return jsonNode.asDouble();
    }

    @Override
    public double asDouble(double defaultValue) {
        return jsonNode.asDouble(defaultValue);
    }

    @Override
    public boolean asBoolean() {
        return jsonNode.asBoolean();
    }

    @Override
    public boolean asBoolean(boolean defaultValue) {
        return jsonNode.asBoolean(defaultValue);
    }

    @Override
    public <T extends JsonNode> T require() throws IllegalArgumentException {
        return jsonNode.require();
    }

    @Override
    public <T extends JsonNode> T requireNonNull() throws IllegalArgumentException {
        return jsonNode.requireNonNull();
    }

    @Override
    public JsonNode required(String fieldName) throws IllegalArgumentException {
        return jsonNode.required(fieldName);
    }

    @Override
    public JsonNode required(int index) throws IllegalArgumentException {
        return jsonNode.required(index);
    }

    @Override
    public JsonNode requiredAt(String pathExpr) throws IllegalArgumentException {
        return jsonNode.requiredAt(pathExpr);
    }

    @Override
    public boolean has(String fieldName) {
        return jsonNode.has(fieldName);
    }

    @Override
    public boolean has(int index) {
        return jsonNode.has(index);
    }

    @Override
    public boolean hasNonNull(String fieldName) {
        return jsonNode.hasNonNull(fieldName);
    }

    @Override
    public boolean hasNonNull(int index) {
        return jsonNode.hasNonNull(index);
    }

    @Override
    public Iterator<JsonNode> elements() {
        return jsonNode.elements();
    }

    @Override
    public Iterator<Entry<String, JsonNode>> fields() {
        return jsonNode.fields();
    }

    @Override
    public JsonNode findValue(String fieldName) {
        return jsonNode.findValue(fieldName);
    }

    @Override
    public JsonNode findParent(String fieldName) {
        return jsonNode.findParent(fieldName);
    }

    @Override
    public List<JsonNode> findValues(String fieldName, List<JsonNode> foundSoFar) {
        return jsonNode.findValues(fieldName, foundSoFar);
    }

    @Override
    public List<String> findValuesAsText(String fieldName, List<String> foundSoFar) {
        return jsonNode.findValuesAsText(fieldName, foundSoFar);
    }

    @Override
    public List<JsonNode> findParents(String fieldName, List<JsonNode> foundSoFar) {
        return jsonNode.findParents(fieldName, foundSoFar);
    }

    @Override
    public JsonNode findPath(String fieldName) {
        return jsonNode.findPath(fieldName);
    }

    @Override
    public <T extends JsonNode> T with(String propertyName) {
        return jsonNode.with(propertyName);
    }

    @Override
    public <T extends JsonNode> T withArray(String propertyName) {
        return jsonNode.withArray(propertyName);
    }

    @Override
    protected JsonNode _at(JsonPointer ptr) {
        return jsonNode.at(ptr);
    }

    @Override
    public boolean equals(Comparator<JsonNode> comparator, JsonNode other) {
        return jsonNode.equals(comparator, other);
    }

    @Override
    public int hashCode() {
        if (hashCode == null) {
            hashCode = hash(jsonNode);
        }

        return hashCode;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof AnyJson)) {
            return false;
        }

        return equivalent(jsonNode, ((AnyJson) o).jsonNode);
    }

    @Override
    public String toString() {
        return jsonNode.toString();
    }

    @Override
    public String toPrettyString() {
        return jsonNode.toPrettyString();
    }

    private static boolean equivalent(JsonNode node1, JsonNode node2) {
        if (node1 == node2) {
            return true;
        }
        if (node1 == null || node2 == null) {
            return false;
        }
        return doEquivalent(convertIfNeeded(node1), convertIfNeeded(node2));
    }

    private static boolean doEquivalent(JsonNode node1, JsonNode node2) {
        if ((node1.isMissingNode() || node1.isNull())
                && (node2.isMissingNode() || node2.isNull())) {
            return true;
        }

        if (node1.isNumber() && node2.isNumber()) {
            return numEquals(node1, node2);
        }

        if (node1.isValueNode() && node2.isValueNode()) {
            return valueEquals(node1, node2);
        }

        if (node1.isArray() && node2.isArray()) {
            return arrayEquals(node1, node2);
        }

        if (node1.isContainerNode() && node2.isContainerNode()) {
            return objectEquals(node1, node2);
        }

        return false;
    }

    private static boolean numEquals(JsonNode node1, JsonNode node2) {
        Preconditions.checkArgument(node1.isNumber(), "only number nodes supported");
        Preconditions.checkArgument(node2.isNumber(), "only number nodes supported");

        if (node1.numberType() == node2.numberType()) {
            return node1.equals(node2);
        } else if (isIn(node1.numberType(), NumberType.INT, NumberType.LONG)
                && isIn(node2.numberType(), NumberType.INT, NumberType.LONG)) {
            return Long.compare(node1.longValue(), node2.longValue()) == 0;
        } else {
            return Double.compare(Double.parseDouble(node1.asText()), Double.parseDouble(node2.asText())) == 0;
        }
    }

    private static boolean valueEquals(JsonNode node1, JsonNode node2) {
        Preconditions.checkArgument(node1.isValueNode(), "only value nodes supported");
        Preconditions.checkArgument(node2.isValueNode(), "only value nodes supported");

        if (node1.getNodeType() == node2.getNodeType()) {
            return node1.equals(node2);
        } else {
            return node2.asText().equals(node2.asText());
        }
    }

    private static boolean arrayEquals(JsonNode node1, JsonNode node2) {
        Preconditions.checkArgument(node1.isArray(), "only array nodes supported");
        Preconditions.checkArgument(node2.isArray(), "only array nodes supported");

        if (node1.size() != node2.size()) {
            return false;
        }

        for (int i = 0; i < node1.size(); i++) {
            if (!equivalent(node1.get(i), node2.get(i))) {
                return false;
            }
        }

        return true;
    }

    private static boolean objectEquals(JsonNode node1, JsonNode node2) {
        if (node1.size() != node2.size()) {
            return false;
        }

        final Set<String> aFields = new HashSet<>();
        node1.fieldNames().forEachRemaining(aFields::add);

        final Set<String> bFields = new HashSet<>();
        node2.fieldNames().forEachRemaining(bFields::add);

        if (!bFields.equals(aFields)) {
            return false;
        }

        for (final String key : aFields) {
            if (!equivalent(node1.get(key), node2.get(key))) {
                return false;
            }
        }

        return true;
    }

    private static int hash(JsonNode node) {
        if (node == null) {
            return 0;
        }

        return doHash(convertIfNeeded(node));
    }

    private static int doHash(JsonNode node) {
        if (node.isNull() || node.isMissingNode()) {
            return 0;
        }

        if (node.isNumber()) {
            return numHash(node);
        }

        if (node.isValueNode()) {
            return valueHash(node);
        }

        if (node.isArray()) {
            return arrayHash(node);
        }

        return objectHash(node);
    }

    private static int numHash(JsonNode node) {
        Preconditions.checkArgument(node.isNumber(), "only number nodes supported");

        return Double.hashCode(Double.parseDouble(node.asText()));
    }

    private static int valueHash(JsonNode node) {
        Preconditions.checkArgument(node.isValueNode(), "only value nodes supported");

        return node.asText().hashCode();
    }

    private static int arrayHash(JsonNode node) {
        Preconditions.checkArgument(node.isArray(), "only array nodes supported");

        int hash = 1;
        for (final JsonNode element : node) {
            hash = 31 * hash + hash(element);
        }
        return hash;
    }

    private static int objectHash(JsonNode node) {
        Preconditions.checkArgument(node.isContainerNode(), "only container nodes supported");

        int hash = 0;
        for (Iterator<Entry<String, JsonNode>> iter = node.fields(); iter.hasNext();) {
            final Entry<String, JsonNode> entry = iter.next();
            hash = 31 * hash + (entry.getKey().hashCode() ^ hash(entry.getValue()));
        }

        return hash;
    }

    private static JsonNode convertIfNeeded(JsonNode node) {
        if (node.isPojo()) {
            try {
                return new AnyJson(MAPPER.readTree(node.toString()));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return node;
    }

    @SafeVarargs
    public static <D extends Enum<?>> boolean isIn(D toCheck, D... allowedLiterals) {
        if (toCheck == null || allowedLiterals == null) {
            return false;
        }
        for (D literal : allowedLiterals) {
            if (toCheck == literal) {
                return true;
            }
        }
        return false;
    }
}
