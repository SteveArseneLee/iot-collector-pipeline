//package com.practice.timpani_test2.service;
//
//import org.apache.flink.api.common.eventtime.WatermarkStrategy;
//import org.apache.flink.api.common.serialization.SimpleStringSchema;
//import org.apache.flink.cdc.connectors.mysql.source.MySqlSource;
//import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
//import org.apache.flink.connector.kafka.sink.KafkaSink;
//import org.apache.flink.streaming.api.datastream.DataStream;
//import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
//import org.apache.kafka.clients.producer.ProducerConfig;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.stereotype.Service;
//
//import java.util.Properties;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//
//@Service
//public class FlinkCDCService {
//    private static final Logger logger = LoggerFactory.getLogger(FlinkCDCService.class);
//    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
//    private volatile boolean isRunning = false;
//
//    /**
//     * Flink CDC Job 실행
//     */
//    public void startCDCJob() {
//        if (isRunning) {
//            logger.warn("⚠ Flink CDC Job이 이미 실행 중입니다!");
//            return;
//        }
//
//        executorService.submit(() -> {
//            try {
//                isRunning = true;
//                logger.info("Flink CDC Job 실행 시작...");
//                runFlinkCDC();
//            } catch (Exception e) {
//                logger.error("Flink CDC Job 실행 중 오류 발생", e);
//            } finally {
//                isRunning = false;
//            }
//        });
//    }
//
//    /**
//     * Flink CDC Job 실행 로직
//     */
//    private void runFlinkCDC() throws Exception {
//        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
//        env.enableCheckpointing(5000); // 5초마다 체크포인트 생성
//
//        // ✅ MySQL CDC Source 설정
//        MySqlSource<String> mySqlSource = MySqlSource.<String>builder()
//                .hostname("172.18.0.6")  // MySQL 호스트
//                .port(3306)
//                .databaseList("scada_db") // 감시할 DB
//                .tableList("scada_db.gateway_metadata") // 감시할 테이블
//                .username("admin")
//                .password("pass")
//                .deserializer(new MyFlinkCDCDeserializationSchema()) // CDC 데이터를 JSON 변환
//                .build();
//
//        // ✅ Kafka Sink 설정 (CDC 데이터를 Kafka로 전송)
//        KafkaSink<String> kafkaSink = KafkaSink.<String>builder()
//                .setBootstrapServers("localhost:10000")  // Kafka 서버 주소 수정
//                .setKafkaProducerConfig(getKafkaProperties())
//                .setRecordSerializer(KafkaRecordSerializationSchema.builder()
//                        .setTopic("cdc_events") // CDC 이벤트를 저장할 Kafka 토픽
//                        .setValueSerializationSchema(new SimpleStringSchema())
//                        .build()
//                )
//                .build();
//
//        // ✅ CDC 데이터 스트림 생성
//        DataStream<String> cdcStream = env.fromSource(
//                mySqlSource, WatermarkStrategy.noWatermarks(), "MySQL CDC Source");
//
//        // ✅ CDC 데이터를 Kafka로 전송
//        cdcStream.sinkTo(kafkaSink);
//
//        env.execute("🚀 MySQL CDC to Kafka Job");
//    }
//
//    /**
//     * Kafka Producer 설정
//     */
//    private Properties getKafkaProperties() {
//        Properties properties = new Properties();
//        properties.put(ProducerConfig.ACKS_CONFIG, "all");  // 데이터 손실 방지를 위해 ACK 설정
//        properties.put(ProducerConfig.RETRIES_CONFIG, 3);  // 최대 3번 재시도
//        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "127.0.0.1:10000"); // Kafka 서버 주소
//        properties.put(ProducerConfig.LINGER_MS_CONFIG, "1"); // 메시지 지연 최소화
//        properties.put(ProducerConfig.BATCH_SIZE_CONFIG, "16384"); // 배치 전송 크기
//        properties.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy"); // 압축 사용
//        return properties;
//    }
//
//    /**
//     * Flink CDC Job 상태 확인
//     */
//    public boolean isCDCJobRunning() {
//        return isRunning;
//    }
//}
