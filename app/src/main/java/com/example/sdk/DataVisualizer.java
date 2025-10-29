package com.example.sdk;

import android.content.Context;
import android.graphics.Color;
import android.widget.LinearLayout;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;
import java.util.List;

/**
 * Renders smartwatch metric charts dynamically.
 */
public class DataVisualizer {

    public void renderCharts(Context context, LinearLayout container, List<SmartWatchData> dataList) {
        container.removeAllViews();

        List<Entry> hrEntries = new ArrayList<>();
        List<Entry> spO2Entries = new ArrayList<>();
        List<Entry> tempEntries = new ArrayList<>();
        List<Entry> stepEntries = new ArrayList<>();

        for (int i = 0; i < dataList.size(); i++) {
            SmartWatchData d = dataList.get(i);
            hrEntries.add(new Entry(i, d.getHeartRate()));
            spO2Entries.add(new Entry(i, d.getSpO2()));
            tempEntries.add(new Entry(i, d.getTemperature()));
            stepEntries.add(new Entry(i, d.getSteps()));
        }

        container.addView(createChart(context, hrEntries, "Heart Rate (bpm)", Color.MAGENTA));
        container.addView(createChart(context, spO2Entries, "SpO₂ (%)", Color.BLUE));
        container.addView(createChart(context, tempEntries, "Temperature (°C)", Color.RED));
        container.addView(createChart(context, stepEntries, "Steps", Color.GREEN));
    }

    private LineChart createChart(Context context, List<Entry> entries, String label, int color) {
        LineChart chart = new LineChart(context);
        chart.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                500
        ));

        LineDataSet dataSet = new LineDataSet(entries, label);
        dataSet.setColor(color);
        dataSet.setLineWidth(2f);
        dataSet.setCircleColor(color);
        dataSet.setCircleRadius(3f);
        dataSet.setDrawValues(false);

        LineData lineData = new LineData(dataSet);
        chart.setData(lineData);

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);

        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        chart.getAxisRight().setEnabled(false);

        Legend legend = chart.getLegend();
        legend.setEnabled(true);

        chart.invalidate();
        return chart;
    }
}
