package com.example.splashscreen;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYSeries;

import java.util.Arrays;

public class Plot extends AppCompatActivity {

    private XYPlot plot;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.plot_screen);

        plot = (XYPlot) findViewById(R.id.plot);

        Intent i = getIntent();

        double[] interval = i.getExtras().getDoubleArray("ar");

        Number[] n = new Number[interval.length];

        for (int e = 0;e<interval.length;e++) {
            n[e] = (Number)interval[e];
        }

        XYSeries stime = new SimpleXYSeries(Arrays.asList(n),SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, "");

        LineAndPointFormatter inter = new LineAndPointFormatter(this, R.xml.line_point_formatter_with_labels);

        plot.addSeries(stime, inter);

    }
}
