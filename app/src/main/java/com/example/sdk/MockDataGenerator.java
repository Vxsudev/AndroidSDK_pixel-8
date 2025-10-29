package com.example.sdk;

import java.util.Random;

/**
 * Generates random smartwatch readings (for testing CSV + Fit fallback)
 */
public class MockDataGenerator {
    private final Random random = new Random();

    // Generate one fake reading
    public SmartWatchData generateData() {
        SmartWatchData data = new SmartWatchData();

        // Realistic ranges
        data.setHeartRate(70 + random.nextInt(20)); // 70–90 bpm
        data.setSpO2(95f + random.nextFloat() * 4f); // 95–99%
        data.setTemperature(36f + random.nextFloat() * 1f); // 36–37 °C
        data.setSteps(random.nextInt(200)); // random steps increment
        data.setTimestamp(System.currentTimeMillis());

        return data;
    }

    // Generate multiple fake readings
    public java.util.List<SmartWatchData> generateBatch(int count) {
        java.util.List<SmartWatchData> list = new java.util.ArrayList<>();
        long now = System.currentTimeMillis();
        for (int i = 0; i < count; i++) {
            SmartWatchData d = generateData();
            d.setTimestamp(now - i * 60000L); // each 1 min apart
            list.add(d);
        }
        return list;
    }
}
