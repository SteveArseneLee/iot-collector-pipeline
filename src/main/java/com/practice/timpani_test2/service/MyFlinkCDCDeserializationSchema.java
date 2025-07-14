//package com.practice.timpani_test2.service;
//
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.fasterxml.jackson.databind.node.ObjectNode;
//import io.debezium.data.Envelope;
//import org.apache.flink.api.common.typeinfo.TypeInformation;
//import org.apache.flink.cdc.debezium.DebeziumDeserializationSchema;
//import org.apache.flink.util.Collector;
//import org.apache.kafka.connect.data.Struct;
//import org.apache.kafka.connect.source.SourceRecord;
//
///**
// * Debezium CDC 데이터를 JSON으로 변환하는 Deserializer
// */
//public class MyFlinkCDCDeserializationSchema implements DebeziumDeserializationSchema<String> {
//
//    private static final ObjectMapper objectMapper = new ObjectMapper();
//
//    @Override
//    public void deserialize(SourceRecord sourceRecord, Collector<String> collector) throws Exception {
//        if (sourceRecord == null || sourceRecord.value() == null) {
//            System.out.println("sourceRecord.value() is null");
//            return;
//        }
//
//        // ✅ `value`가 Struct인지 확인 후 변환
//        if (!(sourceRecord.value() instanceof Struct)) {
//            System.out.println("sourceRecord.value() is NOT an instance of Struct.");
//            return;
//        }
//
//        Struct value = (Struct) sourceRecord.value();
//
//        // ✅ Debezium Envelope에서 변경 유형(Operation) 가져오기
//        Envelope.Operation operation = Envelope.operationFor(sourceRecord);
//
//        // ✅ 변경 전(before) & 변경 후(after) 데이터 추출
//        Struct beforeStruct = null;
//        Struct afterStruct = null;
//
//        if (value.schema().field("before") != null && value.get("before") instanceof Struct) {
//            beforeStruct = (Struct) value.get("before");
//        }
//        if (value.schema().field("after") != null && value.get("after") instanceof Struct) {
//            afterStruct = (Struct) value.get("after");
//        }
//
//        // JSON 변환 (Operation 포함)
//        ObjectNode jsonNode = objectMapper.createObjectNode();
//        jsonNode.put("operation", operation.name()); // ✅ 변경 유형 (CREATE, UPDATE, DELETE)
//
//        if (beforeStruct != null) {
//            jsonNode.set("before", objectMapper.readTree(beforeStruct.toString())); // ✅ 변경 전 데이터
//        }
//        if (afterStruct != null) {
//            jsonNode.set("after", objectMapper.readTree(afterStruct.toString())); // ✅ 변경 후 데이터
//        }
//
//        String json = objectMapper.writeValueAsString(jsonNode);
//        collector.collect(json);
//    }
//
//    @Override
//    public TypeInformation<String> getProducedType() {
//        return TypeInformation.of(String.class);
//    }
//}