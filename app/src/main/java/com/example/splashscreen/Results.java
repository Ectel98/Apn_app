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

    private long id;

    private float interval[] = new float[2];
    private float time_pico[] = new float[2];


    private float interval_interp[] = new float[43200];
    private float time_pico_interp[] = new float[43200];
    private int index_interp = 0;

    private long n_data;

    private int index = 0;                     // Indici di lettura
    private int new_index = 0;                 //


    public Results (Context context) {
        this.context = context;
    }

    public int start_analisy(long identity) {

        id = identity;                        //ID tabella database

        first_load_data();

        if (n_data<100) {
            System.out.print("Dati insufficenti");
            return 1;
        }

        while (linear_interpolation());

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

        try {
            index = data.indexOf(".",new_index);
        }catch (StringIndexOutOfBoundsException siobe) {
            return 0;
        }
        value = Float.parseFloat(data.substring(new_index,index+2));
        new_index=index+2;

        return value;

    }

    private boolean linear_interpolation() {

        // X -> time_pico
        // Y -> interval

        float x0 = time_pico[0] + 1;
        float data;


        if (x0 > time_pico[1] ) {

            time_pico[0] = time_pico[1];
            interval[0] = interval[1];

            data = read_value();

            if (data == 0) {  //Se sono finiti i dati
                return false;
            } else {
                interval[1] = data;
                time_pico[1] += interval[1];
            }
        }

        float b = (interval[1] - interval[0]) / (time_pico[1] - time_pico[0]);
        float a = interval[0] - b*time_pico[0];

        float y0 = (b*x0 + a);

        x0++;
        index_interp++;

        interval_interp[index_interp] = y0;
        time_pico_interp[index_interp] = x0;

        return true;

    }

    private void detrend() {}

    private void ht() {}

    private void htfilt() {}

}
