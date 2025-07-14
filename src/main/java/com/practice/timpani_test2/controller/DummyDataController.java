package com.practice.timpani_test2.controller;

import com.practice.timpani_test2.dummydata.DummyDataScheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dummydata")
public class DummyDataController {

    @Autowired
    private DummyDataScheduler dummyDataScheduler;

    /**
     * Start generating dummy data and sending it to Logstash
     *
     * @param topic The Kafka topic to which the data should be sent
     * @return Status message
     */
    @PostMapping("/start")
    public String startDummyData(@RequestParam String topic) {
        if (!dummyDataScheduler.isRunning()) {
            dummyDataScheduler.start(topic);
            return "Dummy data generation started for topic: " + topic;
        } else {
            return "Dummy data generation is already running.";
        }
    }

    /**
     * Stop generating dummy data
     *
     * @return Status message
     */
    @PostMapping("/stop")
    public String stopDummyData() {
        if (dummyDataScheduler.isRunning()) {
            dummyDataScheduler.stop();
            return "Dummy data generation stopped.";
        } else {
            return "Dummy data generation is not running.";
        }
    }

    /**
     * Check the status of the dummy data scheduler
     *
     * @return Running or stopped status
     */
    @GetMapping("/status")
    public String getStatus() {
        return dummyDataScheduler.isRunning()
                ? "Dummy data generation is running."
                : "Dummy data generation is stopped.";
    }
}