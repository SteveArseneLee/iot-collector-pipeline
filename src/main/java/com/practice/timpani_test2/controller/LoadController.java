package com.practice.timpani_test2.controller;


import com.practice.timpani_test2.service.FlinkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/load")
public class LoadController {

    @Autowired
    private FlinkService flinkService;

    @PostMapping("/elasticsearch")
    public String loadToElasticSearch(@RequestParam String topic,
                                      @RequestParam String mode,
                                      @RequestParam String elasticsearchHost,
                                      @RequestParam String indexName
    ) {
        try {
            flinkService.loadToElasticsearch(topic, mode, elasticsearchHost, indexName);
            return "Data from Kafka topic [" + topic + "] started loading into Elasticsearch index 'iot-test1' in " + mode + " mode.";
        } catch (Exception e) {
            return "Failed to start loading to Elasticsearch: " + e.getMessage();
        }
    }

    @PostMapping("/file-store")
    public String loadToFile(@RequestParam String topic,
                             @RequestParam String mode,
                             @RequestParam String fileType) {
        try {
            flinkService.loadToFile(topic, mode, fileType);
            return "Data from Kafka topic [" + topic + "] started loading in file in " + mode + " mode.";
        } catch (Exception e) {
            return "Failed to start loading to file: " + e.getMessage();
        }
    }

    @PostMapping("/rdb")
    public String loadToRDB(@RequestParam String topic,
                            @RequestParam String mode,
                            @RequestParam String rdbHost,
                            @RequestParam String rdbPort,
                            @RequestParam String rdbDatabase,
                            @RequestParam String rdbTable,
                            @RequestParam String rdbUsername,
                            @RequestParam String rdbPassword) {
        try {
            flinkService.loadToRDB(topic, mode, rdbHost, rdbPort, rdbDatabase, rdbTable, rdbUsername, rdbPassword);
            return "Data from Kafka topic [" + topic + "] started loading in RDB in " + mode + " mode.";
        } catch (Exception e) {
            return "Failed to start loading to RDB: " + e.getMessage();
        }
    }

    @PostMapping("/stop")
    public String stopLoad() {
        boolean stopped = flinkService.stopAllJobs();
        return stopped ? "All jobs stopped." : "No jobs running.";
    }

    @GetMapping("/status")
    public String getStatus() {
        return flinkService.getRunningJobsStatus();
    }
}
