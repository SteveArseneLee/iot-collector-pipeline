package com.practice.timpani_test2.controller;

import com.practice.timpani_test2.dummydata.DummyDataScheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/logstash")
public class LogstashController {

    @Autowired
    private DummyDataScheduler dummyDataScheduler;

    @PostMapping("/register")
    public String registerSource(
            @RequestParam String sourceName,
            @RequestParam(required = false) String ip,
            @RequestParam(required = false) Integer port,
            @RequestParam String topic) {

        if ("dummy".equalsIgnoreCase(sourceName)) {
            // Start DummyDataScheduler to send data to Logstash with the specified topic
            if (!dummyDataScheduler.isRunning()) {
                dummyDataScheduler.start(topic);
            }
            return "Dummy source registered and sending data to Logstash with topic: " + topic;
        } else if (ip != null && port != null) {
            // Configure Logstash for external source
            configureLogstashForExternalSource(sourceName, ip, port, topic);
            return "External source registered: " + sourceName + " (IP: " + ip + ", Port: " + port + ", Topic: " + topic + ")";
        } else {
            return "Invalid request. For external sources, both IP and Port are required.";
        }
    }

    @PostMapping("/stop")
    public String stopDummySource() {
        if (dummyDataScheduler.isRunning()) {
            dummyDataScheduler.stop();
            return "Dummy source stopped.";
        }
        return "Dummy source is not running.";
    }

    private void configureLogstashForExternalSource(String sourceName, String ip, int port, String topic) {
        // Logstash configuration for external source
        System.out.println("Configuring Logstash for external source: " +
                sourceName + " (IP: " + ip + ", Port: " + port + ", Topic: " + topic + ")");
        // TODO: Dynamically update Logstash configuration or notify Logstash
    }
}
