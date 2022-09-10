package com.example.splashscreen;

import android.content.Context;

import com.example.splashscreen.database.DataEcg;
import com.example.splashscreen.database.DataEcgDatabase;

public class Results {

    DataEcg table;

    private DataEcgDatabase ecg_database;
    private DataEcgDatabase getDatabaseManager() {
        if (ecg_database==null)
            ecg_database=DataEcgDatabase.getDatabase();
        return ecg_database;
    }

    private int id;
    private int uf;
    private float data[];
    private long n_data;


    public float read_value() {

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

    public void linear_interpolation() {

        float[] x = new float[2];
        float[] y = new float[2];


        //A ogni chiamata restituisco un dato

    }

    public void detrend() {

    }

    public void ht() {}

    public void htfilt() {}


}
