package com.practice.timpani_test2.dummydata;

import net.datafaker.Faker;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class DummyDataGenerator {
    final private Faker faker = new Faker();

    public Map<String, Object> generateDummyData() {
        Map<String, Object> dummyData = new HashMap<>();
        int randomDeviceId = faker.number().numberBetween(101, 999);
        dummyData.put("deviceId", "device" + randomDeviceId);
        dummyData.put("temperature", faker.number().randomDouble(2, -10, 40));
        dummyData.put("humidity", faker.number().randomDouble(2, 0, 100));
        dummyData.put("timestamp", System.currentTimeMillis());
        return dummyData;
    }
}