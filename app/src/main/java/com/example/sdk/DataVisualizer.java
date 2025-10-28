package com.example.sdk;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.widget.LinearLayout;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles rendering of smartwatch data charts.
 */
public class DataVisualizer {

    private static final String TAG = "DataVisualizer";

    /**
     * Renders a set of charts (Heart Rate, SpO₂, Temperature, Steps)
     * inside a provided LinearLayout container.
     */
    public void renderCharts(Context context, LinearLayout container, List<SmartWatchData> dataList) {
        try {
            if (dataList == null || dataList.isEmpty()) {
                Log.w(TAG, "⚠️ No data available for visualization");
                return;
            }

            container.removeAllViews();

            // Extract entries for each metric
            List<Entry> heartRateEntries = new ArrayList<>();
            List<Entry> spo2Entries = new ArrayList<>();
            List<Entry> temperatureEntries = new ArrayList<>();
            List<Entry> stepsEntries = new ArrayList<>();

            for (int i = 0; i < dataList.size(); i++) {
                SmartWatchData data = dataList.get(i);
                heartRateEntries.add(new Entry(i, data.getHeartRate()));
                spo2Entries.add(new Entry(i, data.getSpo2()));
                temperatureEntries.add(new Entry(i, (float) data.getTemperature()));
                stepsEntries.add(new Entry(i, data.getSteps()));
            }

            // Create and add four charts
            container.addView(createLineChart(context, heartRateEntries, "Heart Rate (bpm)", Color.MAGENTA));
            container.addView(createLineChart(context, spo2Entries, "SpO₂ (%)", Color.BLUE));
            container.addView(createLineChart(context, temperatureEntries, "Temperature (°C)", Color.RED));
            container.addView(createLineChart(context, stepsEntries, "Steps", Color.GREEN));

        } catch (Exception e) {
            Log.e(TAG, "❌ Chart rendering failed", e);
        }
    }

    /**
     * Helper: builds a single styled LineChart.
     */
    private LineChart createLineChart(Context context, List<Entry> entries, String label, int color) {
        LineChart chart = new LineChart(context);
        chart.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                400));

        LineDataSet dataSet = new LineDataSet(entries, label);
        dataSet.setColor(color);
        dataSet.setLineWidth(2f);
        dataSet.setCircleColor(color);
        dataSet.setValueTextColor(Color.DKGRAY);
        dataSet.setValueTextSize(8f);
        dataSet.setDrawValues(false);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);

        LineData lineData = new LineData(dataSet);
        chart.setData(lineData);
        chart.invalidate();

        // Style chart
        Description desc = new Description();
        desc.setText("");
        chart.setDescription(desc);
        chart.setDrawGridBackground(false);
        chart.getXAxis().setDrawGridLines(false);
        chart.getAxisLeft().setDrawGridLines(false);
        chart.getAxisRight().setDrawGridLines(false);
        chart.getXAxis().setTextColor(Color.DKGRAY);
        chart.getAxisLeft().setTextColor(Color.DKGRAY);
        chart.getAxisRight().setTextColor(Color.DKGRAY);

        Legend legend = chart.getLegend();
        legend.setTextColor(Color.DKGRAY);
        legend.setTextSize(12f);

        chart.animateX(700);
        return chart;
    }
}
