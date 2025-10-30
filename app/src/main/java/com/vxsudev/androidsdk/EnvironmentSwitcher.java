package com.vxsudev.androidsdk;

import android.content.Context;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles environment switching (Default, Dev, Prod, etc.)
 * and triggers Firebase reinitialization dynamically.
 */
public class EnvironmentSwitcher {

    private static final String TAG = "EnvironmentSwitcher";
    private final Context context;
    private final Spinner envSpinner;
    private List<Environment> environments;
    private Environment selectedEnv;

    public EnvironmentSwitcher(Context context, Spinner envSpinner) {
        this.context = context;
        this.envSpinner = envSpinner;
    }

    public void setup() {
        environments = new ArrayList<>();
        environments.add(new Environment("DEFAULT", "google-services.json"));
        environments.add(new Environment("DEV", "google-services-dev.json"));
        environments.add(new Environment("PROD", "google-services-prod.json"));

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                context,
                android.R.layout.simple_spinner_item,
                getEnvNames()
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        envSpinner.setAdapter(adapter);

        selectedEnv = environments.get(0); // default selection
    }

    private List<String> getEnvNames() {
        List<String> names = new ArrayList<>();
        for (Environment e : environments) names.add(e.getName());
        return names;
    }

    public void applyEnvironment() {
        int position = envSpinner.getSelectedItemPosition();
        selectedEnv = environments.get(position);
        try {
            Log.d(TAG, "⚙️ Switching to " + selectedEnv.getName() +
                    " with config " + selectedEnv.getConfigFile());
            FirebaseAppLoader.initialize(context, selectedEnv.getConfigFile());
            Toast.makeText(context, "✅ Switched to " + selectedEnv.getName(), Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "❌ Environment switch failed", e);
            Toast.makeText(context, "Switch failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    public Environment getSelectedEnv() {
        return selectedEnv;
    }

    public static class Environment {
        private final String name;
        private final String configFile;

        public Environment(String name, String configFile) {
            this.name = name;
            this.configFile = configFile;
        }

        public String getName() {
            return name;
        }

        public String getConfigFile() {
            return configFile;
        }
    }
}
