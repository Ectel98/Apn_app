package com.example.splashscreen;

import android.content.Context;

import com.example.splashscreen.database.DataEcgDatabase;

public class Results {

    private final Context context;


    private DataEcgDatabase ecg_database;

    DataEcgDatabase getDatabaseManager() {
        if (ecg_database == null)
            ecg_database = DataEcgDatabase.getDatabase(context);
        return ecg_database;
    }

    private long id;

    static class parameters {
        public float[] interval;
        public float[] time_pico;
        public parameters(int dimension) {
            interval = new float[dimension];
            time_pico = new float[dimension];
        }
    }

    private final parameters read = new parameters(2);
    private final parameters interp = new parameters(43200);
    private parameters detrend;
    private parameters smooth;

    private int index_interp = 0;

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

        detrend = new parameters(index_interp);

        if(!detrend())
            return 1;

        smooth = new parameters(index_interp);

        smooth();
        ht();
        htfilt();

        return 0;

    }

    private void first_load_data() {     //restituisce x e y per l'interpolazione

        read.time_pico[0] = read_value();
        read.interval[0] = read_value();
        read.time_pico[0] += read.interval[0];

        read.interval[1] = read_value();
        read.time_pico[1] += read.interval[1];

    }


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

    private boolean linear_interpolation() { // NON funziona così ! loop interno !  Interpolazione lineare, Ricampiono ogni secondo

        // X -> time_pico [ms]
        // Y -> interval  [ms]

        float y0;
        float a;
        float b;
        float data;

        x0 = read.time_pico[0] + 1000;


        if (x0 > read.time_pico[1]) {

            read.time_pico[0] = read.time_pico[1];
            read.interval[0] = read.interval[1];

            data = read_value();

            if (data == 0) {  //Se sono finiti i dati
                return false;
            } else {
                read.interval[1] = data;
                read.time_pico[1] += read.interval[1];
            }
        }

        b = (read.interval[1] - read.interval[0]) / (read.time_pico[1] - read.time_pico[0]);
        a = read.interval[0] - b * read.time_pico[0];


        y0 = (b * x0 + a);


        interp.interval[index_interp] = y0;
        interp.time_pico[index_interp] = x0;

        x0++;
        index_interp++;

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
            sumx += interp.time_pico[i];
            sumy += interp.interval[i];
            sumxy += interp.time_pico[i]*interp.interval[i];
            sumx2 += interp.time_pico[i]*interp.time_pico[i];
        }

        b = (sumxy - sumx*sumy/win) / (sumx2 - sumx*sumx/win);
        a = sumy/win - b*sumx/win;

        for (i=0; i<=hwin; i++) {
            detrend.interval[i] = interp.interval[i] - (a + b * interp.time_pico[i]);
            detrend.time_pico[i] = interp.time_pico[i];
        }

        for (i=win ; i<index_interp; i++) {
            sumx += interp.time_pico[i]-interp.time_pico[i-win];
            sumy += interp.interval[i]-interp.interval[i-win];
            sumxy += interp.time_pico[i]*interp.interval[i]-interp.time_pico[i-win]*interp.interval[i-win];
            sumx2 += interp.time_pico[i]*interp.time_pico[i]-interp.time_pico[i-win]*interp.time_pico[i-win];

            b = (sumxy - sumx*sumy/win) / (sumx2 - sumx*sumx/win);
            a = sumy/win - b*sumx/win;

            detrend.interval[i-hwin] = interp.interval[i-hwin] - (a + b*interp.time_pico[i-hwin]);
            detrend.time_pico[i-hwin] = interp.time_pico[i-hwin];
        }

        for (i=i-hwin; i<index_interp; i++) {
            detrend.interval[i] = interp.interval[i] - (a + b * interp.time_pico[i]);
            detrend.time_pico[i] = interp.time_pico[i];
        }

        return true;

    }

    private void smooth() { //Moving average filter Da rifare

        float sumx,sumy,win;

        int i= 0;

        win = 5;

        sumx = sumy = 0;

        for (i = 0;i<win;i++){
            sumx += detrend.time_pico[i];
            sumy += detrend.interval[i];
        }

        smooth.interval[i] = sumx/win;
        smooth.time_pico[i] = sumy/win;

        sumx -= detrend.time_pico[0];
        sumy -= detrend.interval[0];

        i = 0;

        while (i<index_interp) {

            sumx += detrend.time_pico[i];
            sumy += detrend.interval[i];

            smooth.interval[i] = sumy/win;
            smooth.time_pico[i] = sumx/win;

            if (++i >= win)
                i = 0;

            sumx -= detrend.time_pico[i];
            sumy -= detrend.interval[i];

        }
    }


    private void ht() { //Hilbert transform

    }

    private void htfilt() {}

}

