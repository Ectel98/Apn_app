package com.example.splashscreen;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;


import com.example.splashscreen.database.DataEcg;
import com.example.splashscreen.database.DataEcgDatabase;

public class Results {

    private Context context;

    DataEcg table;

    private DataEcgDatabase ecg_database;
    DataEcgDatabase getDatabaseManager() {
        if (ecg_database==null)
            ecg_database=DataEcgDatabase.getDatabase(context);
        return ecg_database;
    }

    private int id;
    private int uf;
    private float data[];
    private long n_data;

    public Results (Context context) {
        this.context = context;
    }

    public void start_analisy() {

        load_data();
        linear_interpolation();
        detrend();
        ht();
        htfilt();

    };

    private void load_data() {

        data[1] = read_value();

    };


    private float read_value() {


        int index = 0;
        int new_index = 0;
        float value;

        n_data = getDatabaseManager().noteModel().loadNote(id).number_data;
        String data = getDatabaseManager().noteModel().loadNote(id).data;

        index = data.indexOf(data,new_index);
        value = Float.parseFloat(data.substring(new_index,index+2));
        new_index=index+2;

        return value;

    }

    private void linear_interpolation() {

        float[] x = new float[2];
        float[] y = new float[2];


        //A ogni chiamata restituisco un dato

    }

    private void detrend() {

    }

    private void ht() {}

    private void htfilt() {}

}
