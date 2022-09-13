package com.example.splashscreen;

import android.content.Context;


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

    private float interval[] = new float[2];
    private float time_pico[] = new float[2];

    private long n_data;

    private int index = 0;                     // Indici di lettura
    private int new_index = 0;                 //


    public Results (Context context) {
        this.context = context;
    }

    public int start_analisy() {

        first_load_data();

        if (n_data<100) {
            System.out.print("Dati insufficenti");
            return 1;
        }

        linear_interpolation();
        detrend();
        ht();
        htfilt();

        return 0;

    };

    private void first_load_data() {     //restituisce x e y per l'interpolazione

        time_pico[0] = read_value();
        interval[0] = read_value();
        time_pico[0] += interval[0];

        interval[1] = read_value();
        time_pico[1] += interval[1];

    };


    private float read_value() {

        float value;

        n_data = getDatabaseManager().noteModel().loadNote(id).number_data;   // Lettura dati dal database
        String data = getDatabaseManager().noteModel().loadNote(id).data;     //

        index = data.indexOf(data,new_index);
        value = Float.parseFloat(data.substring(new_index,index+2));
        new_index=index+2;

        return value;

    }

    private void linear_interpolation() {




        //A ogni chiamata restituisco un dato

    }

    private void detrend() {}

    private void ht() {}

    private void htfilt() {}

}
