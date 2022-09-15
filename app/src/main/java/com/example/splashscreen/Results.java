package com.example.splashscreen;

import android.content.Context;


import com.example.splashscreen.database.DataEcg;
import com.example.splashscreen.database.DataEcgDatabase;

public class Results {

    private Context context;

    DataEcg table;

    private DataEcgDatabase ecg_database;

    DataEcgDatabase getDatabaseManager() {
        if (ecg_database == null)
            ecg_database = DataEcgDatabase.getDatabase(context);
        return ecg_database;
    }

    private long id;

    private float interval[] = new float[2];
    private float time_pico[] = new float[2];

    private float interval_interp[] = new float[43200];  //Max 12 ore
    private float time_pico_interp[] = new float[43200];
    private int index_interp = 0;

    private float interval_detrend[] = new float[43200];
    private float time_pico_detrend[] = new float[43200];

    private long n_data;

    private float x0;

    private int index = 0;                     // Indici di lettura
    private int new_index = 0;                 //


    public Results(Context context) {
        this.context = context;
    }

    public int start_analisy(long identity) {

        id = identity;                        //ID tabella database

        first_load_data();

        if (n_data < 100) {
            System.out.print("Dati insufficenti");
            return 1;
        }

        while (linear_interpolation());



        if(!detrend())
            return 1;

        smooth();
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
            index = data.indexOf(".", new_index);
        } catch (StringIndexOutOfBoundsException siobe) {
            return 0;                                                         //Se ho finito i dati da leggere
        }
        value = Float.parseFloat(data.substring(new_index, index + 2));
        new_index = index + 2;

        return value;

    }

    private boolean linear_interpolation() { //Interpolazione lineare, Ricampiono ogni secondo

        // X -> time_pico [ms]
        // Y -> interval  [ms]

        float y0;
        float a;
        float b;
        float data;

        x0 = time_pico[0] + 1000;


        if (x0 > time_pico[1]) {

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

        b = (interval[1] - interval[0]) / (time_pico[1] - time_pico[0]);
        a = interval[0] - b * time_pico[0];

        y0 = (b * x0 + a);

        x0++;
        index_interp++;

        interval_interp[index_interp] = y0;
        time_pico_interp[index_interp] = x0;

        return true;

    }

    private boolean detrend() { //Calcola la regressione lineare e la sottrae, opera su una finestra mobile. Toglie la "tendenza"

        int hwin = 40;
        int win = 2*hwin +1;
        int i = 0;

        float sumx,sumy,sumxy,sumx2;
        float a,b;

        sumx = sumy = sumxy = sumx2 = 0;

        if (index_interp >= 43200) {
            System.out.print("Index out of range");
            return false;
        }

        for (i=0; i<win; i++) {
            sumx += time_pico_interp[i];
            sumy += interval_interp[i];
            sumxy += time_pico_interp[i]*interval_interp[i];
            sumx2 += time_pico_interp[i]*time_pico_interp[i];
        }

        b = (sumxy - sumx*sumy/win) / (sumx2 - sumx*sumx/win);
        a = sumy/win - b*sumx/win;

        for (i=0; i<=hwin; i++) {
            interval_detrend[i] = interval_interp[i] - (a + b * time_pico_interp[i]);
            time_pico_detrend[i] = time_pico_interp[i];
        }

        for (i=win ; i<index_interp; i++) {
            sumx += time_pico_interp[i]-time_pico_interp[i-win];
            sumy += interval_interp[i]-interval_interp[i-win];
            sumxy += time_pico_interp[i]*interval_interp[i]-time_pico_interp[i-win]*interval_interp[i-win];
            sumx2 += time_pico_interp[i]*time_pico_interp[i]-time_pico_interp[i-win]*time_pico_interp[i-win];

            b = (sumxy - sumx*sumy/win) / (sumx2 - sumx*sumx/win);
            a = sumy/win - b*sumx/win;

            interval_detrend[i-hwin] = interval_interp[i-hwin] - (a + b*time_pico_interp[i-hwin]);
            time_pico_detrend[i-hwin] = time_pico_interp[i-hwin];
        }

        for (i=i-hwin; i<index_interp; i++) {
            interval_detrend[i] = interval_interp[i] - (a + b * time_pico_interp[i]);
            time_pico_detrend[i] = time_pico_interp[i];
        }

        return true;

    }

    private void smooth() {

    };

    private void ht() {};

    private void htfilt() {};

}
