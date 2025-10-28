package com.example.sdk;

public enum FirestoreEnv {
    DEFAULT("google-services.json"),
    LAB("firebase_lab.json"),
    RESEARCH("firebase_research.json");

    private final String configFile;

    FirestoreEnv(String configFile) {
        this.configFile = configFile;
    }

    public String getConfigFile() {
        return configFile;
    }
}
