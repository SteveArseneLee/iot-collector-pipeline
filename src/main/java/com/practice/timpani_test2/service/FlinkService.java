package com.practice.timpani_test2.service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.flink.api.common.eventtime.*;
import org.apache.flink.api.common.serialization.SimpleStringEncoder;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.api.connector.sink2.SinkWriter;
import org.apache.flink.connector.elasticsearch.sink.Elasticsearch7SinkBuilder;
import org.apache.flink.connector.elasticsearch.sink.RequestIndexer;
import org.apache.flink.connector.file.sink.FileSink;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.core.fs.Path;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.streaming.api.functions.sink.filesystem.rollingpolicies.DefaultRollingPolicy;
import org.apache.flink.util.Collector;
import org.apache.http.HttpHost;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.Requests;
import org.elasticsearch.common.xcontent.XContentType;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Service
public class FlinkService {
    private static final Gson gson = new Gson();

    /**
     * Kafka → Flink → 지연 감지 (공통 로직)
     * File, Elasticsearch, RDB 등으로 저장하기 전에 공통적으로 적용되는 로직
     */
    private DataStream<String> getProcessedStream(StreamExecutionEnvironment env, String topic) {
        // Kafka Source 설정
        KafkaSource<String> kafkaSource = KafkaSource.<String>builder()
//                .setBootstrapServers("KafkaContainer:9092")
                .setBootstrapServers("localhost:10000")
                .setTopics(topic)
                .setGroupId("flink-group")
                .setValueOnlyDeserializer(new SimpleStringSchema())
                .build();

        // Watermark 적용 (5초 Out-of-Order 허용)
        WatermarkStrategy<String> watermarkStrategy = WatermarkStrategy
                .<String>forBoundedOutOfOrderness(Duration.ofSeconds(5))
                .withTimestampAssigner((event, timestamp) -> extractTimestamp(event));

        // Kafka → Flink DataStream 변환
        DataStream<String> kafkaStream = env.fromSource(
                kafkaSource,
                watermarkStrategy,
                "Kafka Source"
        );

        // 지연 감지 적용 후 반환
        return kafkaStream.process(new DelayDetectionProcessFunction());
    }

    /**
     * Kafka → FileSink (CSV/JSON) 저장
     */
    public void loadToFile(String topic, String mode, String fileType) {
        try {
            StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

            // 공통 데이터 스트림 처리 (Kafka → Watermark → 지연 감지)
            DataStream<String> processedStream = getProcessedStream(env, topic);

            // 파일 저장 경로 생성
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String outputPath = "output/" + mode + "_" + timestamp + "." + fileType;

            // 파일 형식에 따라 적절한 Encoder 설정
            FileSink<String> fileSink;
            switch (fileType.toLowerCase()) {
                case "csv":
                    fileSink = FileSink.<String>forRowFormat(new Path(outputPath), new SimpleStringEncoder<>("UTF-8"))
                            .withRollingPolicy(DefaultRollingPolicy.builder()
                                    .withRolloverInterval(TimeUnit.MINUTES.toMillis(15)) // 15분마다 롤오버
                                    .withInactivityInterval(TimeUnit.MINUTES.toMillis(5)) // 5분간 활동 없으면 롤오버
                                    .withMaxPartSize(1024 * 1024 * 128) // 최대 파일 크기 128MB
                                    .build())
                            .build();
                    break;

                case "json":
                    fileSink = FileSink.<String>forRowFormat(new Path(outputPath),new SimpleStringEncoder<>("UTF-8")) // JSON 처리 로직 추가 가능
                            .withRollingPolicy(DefaultRollingPolicy.builder()
                                    .withRolloverInterval(TimeUnit.MINUTES.toMillis(15))
                                    .withInactivityInterval(TimeUnit.MINUTES.toMillis(5))
                                    .withMaxPartSize(1024 * 1024 * 128)
                                    .build())
                            .build();
                    break;

                default:
                    throw new IllegalArgumentException("Unsupported file type: " + fileType);
            }

            // Sink 연결
            processedStream.sinkTo(fileSink);

            // Flink Job 실행
            env.execute("Flink " + mode + " Job: Kafka to File");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Kafka → Elasticsearch 저장
     */
    public void loadToElasticsearch(String topic, String mode, String elasticsearchHost, String indexName) {
        try {
            StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
            env.enableCheckpointing(5000); // 5초마다 체크포인팅

            // 공통 데이터 스트림 처리 (Kafka → Watermark → 지연 감지)
            DataStream<String> processedStream = getProcessedStream(env, topic);

            // Elasticsearch Sink 설정
            Elasticsearch7SinkBuilder<String> sinkBuilder = new Elasticsearch7SinkBuilder<>();
            sinkBuilder.setHosts(HttpHost.create(elasticsearchHost));
            sinkBuilder.setBulkFlushBackoffStrategy(
                    org.apache.flink.connector.elasticsearch.sink.FlushBackoffType.EXPONENTIAL,
                    5, // 최대 5번 재시도
                    1000 // 1초 대기
            );
            if ("streaming".equalsIgnoreCase(mode)) {
                sinkBuilder.setBulkFlushMaxActions(500); // 500개의 요청마다 플러시
                sinkBuilder.setBulkFlushInterval(2000); // 2초마다 플러시
                sinkBuilder.setEmitter(new StreamingElasticsearchEmitter(indexName));
            } else if ("batch".equalsIgnoreCase(mode)) {
                sinkBuilder.setBulkFlushMaxActions(1000); // Batch 모드는 더 큰 bulk size를 사용
                sinkBuilder.setBulkFlushInterval(5000); // 5초마다 플러시
                sinkBuilder.setEmitter(new BatchElasticsearchEmitter(indexName));
            } else {
                throw new IllegalArgumentException("Invalid mode: " + mode);
            }

            // Sink 연결
            processedStream.sinkTo(sinkBuilder.build());

            // Flink Job 실행
            env.execute("Flink " + mode + " Job: Kafka to Elasticsearch");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 지연 감지 (Kafka → Flink 중간 처리)
     */
    private static class DelayDetectionProcessFunction extends ProcessFunction<String, String> {
        @Override
        public void processElement(String value, Context ctx, Collector<String> out) {
            long eventTime = extractTimestamp(value);
            long processingTime = System.currentTimeMillis();
            long delay = processingTime - eventTime;

            boolean isDelayed = delay > 10_000; // 10초 이상 지연된 경우

            if (isDelayed) {
                System.out.println("⚠ WARNING: 데이터 지연 발생! (지연시간: " + delay + "ms, 데이터: " + value + ")");
            }

            // JSON 변환 후 "delayed" 필드 추가
            JsonObject json = parseJson(value);
            json.addProperty("delayed", isDelayed);
            out.collect(json.toString());
        }
    }

    /**
     * Kafka 메시지에서 timestamp 추출
     */
    private static long extractTimestamp(String event) {
        try {
            JsonObject json = parseJson(event);
            if (json.has("timestamp")) {
                return json.get("timestamp").getAsLong();
            }
        } catch (Exception ignored) {
        }
        return System.currentTimeMillis(); // 파싱 실패 시 현재 시간 반환
    }

    /**
     * JSON 파싱 (Gson 적용)
     */
    private static JsonObject parseJson(String json) {
        try {
            return gson.fromJson(json, JsonObject.class);
        } catch (Exception e) {
            System.err.println("❌ JSON 파싱 오류: " + json);
            return new JsonObject();
        }
    }

    /**
     * Elasticsearch Emitter (지연 감지 데이터 포함)
     */
    private static class StreamingElasticsearchEmitter implements org.apache.flink.connector.elasticsearch.sink.ElasticsearchEmitter<String> {
        private final String indexName;

        public StreamingElasticsearchEmitter(String indexName) {
            this.indexName = indexName;
        }

        @Override
        public void emit(String element, SinkWriter.Context context, RequestIndexer indexer) {
            JsonObject json = parseJson(element);
            json.addProperty("timestamp", System.currentTimeMillis()); // 저장 시점 timestamp 추가

            IndexRequest request = Requests.indexRequest()
                    .index(indexName)
                    .source(json.toString(), XContentType.JSON);

            indexer.add(request);
        }
    }

    private static class BatchElasticsearchEmitter implements org.apache.flink.connector.elasticsearch.sink.ElasticsearchEmitter<String> {
        private final String indexName;

        public BatchElasticsearchEmitter(String indexName) {
            this.indexName = indexName;
        }

        @Override
        public void emit(String element, SinkWriter.Context context, RequestIndexer indexer) {
            JsonObject json = parseJson(element);
            json.addProperty("timestamp", System.currentTimeMillis()); // 저장 시점 timestamp 추가

            IndexRequest request = Requests.indexRequest()
                    .index(indexName)
                    .source(json.toString(), XContentType.JSON);

            indexer.add(request);
        }
    }


    public void loadToRDB(String topic,
                          String mode,
                          String rdbHost,
                          String rdbPort,
                          String rdbDatabase,
                          String rdbTable,
                          String rdbUsername,
                          String rdbPassword) {
        // TODO: RDB 로직 추가
        System.out.println("Loading to RDB is not implemented yet.");
    }

    public boolean stopAllJobs() {
        // 모든 Flink 작업 중지 로직
        return true;
    }

    public String getRunningJobsStatus() {
        // 실행 중인 작업 상태 반환 로직
        return "No running Flink jobs.";
    }
}
