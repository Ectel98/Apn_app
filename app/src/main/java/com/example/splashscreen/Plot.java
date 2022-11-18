package com.example.splashscreen;


import android.content.Intent;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.PanZoom;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.StepMode;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYSeries;

import java.util.Arrays;

public class Plot extends AppCompatActivity  {

    XYPlot plot;

    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.plot_screen);

        plot = (XYPlot) findViewById(R.id.plot);

        Intent i = getIntent();

        double[] interval = i.getExtras().getDoubleArray("ar");

        Number[] n = new Number[interval.length];

        for (int e = 0; e < interval.length; e++) {
            n[e] = (Number) interval[e];
        }

        XYSeries stime = new SimpleXYSeries(Arrays.asList(n), SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, "");

        LineAndPointFormatter inter = new LineAndPointFormatter(this, R.xml.line_point_formatter_with_labels);

        plot.getLegend().setVisible(false);

        plot.setDomainStep(StepMode.SUBDIVIDE, 10.0);
        plot.setRangeStep(StepMode.SUBDIVIDE, 10);

        plot.addSeries(stime, inter);

        PanZoom.attach(plot, PanZoom.Pan.HORIZONTAL, PanZoom.Zoom.STRETCH_HORIZONTAL);

        plot.getOuterLimits().set(0, interval.length, -1, 1);

        plot.redraw();

    }
}