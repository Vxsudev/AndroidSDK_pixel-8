package com.example.sdk;

public class SmartWatchData {

    private int heartRate;
    private int spo2;
    private double temperature;
    private int steps;

    public SmartWatchData() {}

    public SmartWatchData(int heartRate, int spo2, double temperature, int steps) {
        this.heartRate = heartRate;
        this.spo2 = spo2;
        this.temperature = temperature;
        this.steps = steps;
    }

    public int getHeartRate() { return heartRate; }
    public void setHeartRate(int heartRate) { this.heartRate = heartRate; }

    public int getSpo2() { return spo2; }
    public void setSpo2(int spo2) { this.spo2 = spo2; }

    public double getTemperature() { return temperature; }
    public void setTemperature(double temperature) { this.temperature = temperature; }

    public int getSteps() { return steps; }
    public void setSteps(int steps) { this.steps = steps; }

    @Override
    public String toString() {
        return "SmartWatchData{" +
                "heartRate=" + heartRate +
                ", spo2=" + spo2 +
                ", temperature=" + temperature +
                ", steps=" + steps +
                '}';
    }
}
