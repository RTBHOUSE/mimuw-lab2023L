package com.rtbhouse.nosqllab.avro;

import java.io.ByteArrayOutputStream;

import org.apache.avro.Schema;
import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.avro.specific.SpecificRecord;

public class SerDe<T extends SpecificRecord> {

    private Schema readerSchema;
    private SpecificDatumWriter<T> specificDatumWriter;

    public SerDe(Schema readerSchema) {
        this.readerSchema = readerSchema;
        this.specificDatumWriter = new SpecificDatumWriter<>(readerSchema);
    }

    public byte[] serialize(T specificRecord) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BinaryEncoder binaryEncoder = EncoderFactory.get().binaryEncoder(baos, null);
        try {
            specificDatumWriter.write(specificRecord, binaryEncoder);
            binaryEncoder.flush();

            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public T deserialize(byte[] data, Schema writerSchema) {
        BinaryDecoder binaryDecoder = DecoderFactory.get().binaryDecoder(data, null);
        SpecificDatumReader<T> specificDatumReader = new SpecificDatumReader<>(writerSchema, readerSchema);
        try {
            return specificDatumReader.read(null, binaryDecoder);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
