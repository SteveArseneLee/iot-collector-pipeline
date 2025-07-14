//package com.practice.timpani_test2.controller;
//
//import com.practice.timpani_test2.service.FlinkCDCService;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//@RestController
//@RequestMapping("/flink-cdc")
//public class FlinkCDCController {
//    private final FlinkCDCService flinkCDCService;
//
//    public FlinkCDCController(FlinkCDCService flinkCDCService) {
//        this.flinkCDCService = flinkCDCService;
//    }
//
//    /**
//     * Flink CDC 실행 API
//     */
//    @PostMapping("/start")
//    public ResponseEntity<String> startCDCJob() {
//        flinkCDCService.startCDCJob();
//        return ResponseEntity.ok("🚀 Flink CDC Job 실행 시작!");
//    }
//
//    /**
//     * Flink CDC 실행 상태 확인 API
//     */
//    @GetMapping("/status")
//    public ResponseEntity<String> checkStatus() {
//        boolean running = flinkCDCService.isCDCJobRunning();
//        return ResponseEntity.ok(running ? "✅ Flink CDC Job 실행 중!" : "❌ Flink CDC Job 정지됨!");
//    }
//}
