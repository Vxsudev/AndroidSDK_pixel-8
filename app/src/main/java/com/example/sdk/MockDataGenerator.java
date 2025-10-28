package com.example.sdk;

import java.util.Random;

public class MockDataGenerator {

    private final Random random = new Random();

    // ✅ Generates random smartwatch readings
    public SmartWatchData generateData() {
        SmartWatchData data = new SmartWatchData();

        // realistic ranges
        data.setHeartRate(70 + random.nextInt(20));         // 70–90 bpm
        data.setSpo2(95 + random.nextInt(4));               // 95–98%
        data.setTemperature(36.0 + random.nextDouble());    // 36.0–37.0 °C
        data.setSteps(1000 + random.nextInt(500));          // 1000–1500 steps

        return data;
    }
}
