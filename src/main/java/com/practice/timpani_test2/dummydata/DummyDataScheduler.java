package com.practice.timpani_test2.dummydata;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.concurrent.ScheduledFuture;

@Component
public class DummyDataScheduler {

    @Autowired
    private DummyDataGenerator dummyDataGenerator;

    private ThreadPoolTaskScheduler scheduler;
    private ScheduledFuture<?> future;

    private final RestTemplate restTemplate = new RestTemplate();
    private final String logstashUrl = "http://127.0.0.1:8080"; // Logstash HTTP Input endpoint

    public void start(String topic) {
        if (scheduler == null) {
            scheduler = new ThreadPoolTaskScheduler();
            scheduler.initialize();
        }

        future = scheduler.scheduleAtFixedRate(() -> {
            Map<String, Object> data = dummyDataGenerator.generateDummyData();
            data.put("topic", topic); // Include the topic in the data
            sendDataToLogstash(data);
        }, 1000);
        System.out.println("Dummy data scheduler started for topic: " + topic);
    }

    private void sendDataToLogstash(Map<String, Object> data) {
        try {
            restTemplate.postForObject(logstashUrl, data, String.class);
            System.out.println("Data sent to Logstash: " + data);
        } catch (Exception e) {
            System.err.println("Failed to send data to Logstash: " + e.getMessage());
        }
    }

    public void stop() {
        if (future != null) {
            future.cancel(true);
            System.out.println("Dummy data scheduler stopped.");
        }
    }

    public boolean isRunning() {
        return future != null && !future.isCancelled();
    }
}