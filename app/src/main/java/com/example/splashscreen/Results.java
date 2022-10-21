/*

References

Goldberger, A., Amaral, L., Glass, L., Hausdorff, J., Ivanov, P. C., Mark, R., ... & Stanley, H. E. (2000). PhysioBank, PhysioToolkit, and PhysioNet: Components of a new research resource for complex physiologic signals. Circulation [Online]. 101 (23), pp. e215–e220.

 */

package com.example.splashscreen;

import android.content.Context;
import com.example.splashscreen.database.DataEcgDatabase;

import java.util.Arrays;

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

        public float[] time;
        public float[] ampl;
        public float[] omega;

        public parameters(int dimension) {
            interval = new float[dimension];
            time_pico = new float[dimension];
        }
        public parameters(int dimension,int dimension2,int dimension3) {
            time = new float[dimension];
            ampl = new float[dimension2];
            omega = new float[dimension3];
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


    private float[] ampl;
    private float[] omega;


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

        linear_interpolation();

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

    private boolean linear_interpolation() { // Interpolazione lineare, Ricampiono ogni secondo

        // X -> time_pico [ms]
        // Y -> interval  [ms]

        float y0;
        float a;
        float b;
        float data;

        interp.interval[0] = read_value();
        interp.time_pico[0] += interp.interval[0];

        x0 = read.time_pico[0] + 1000;

        while(true) {

            if (x0 > read.time_pico[1]) {

                read.time_pico[0] = read.time_pico[1];
                read.interval[0] = read.interval[1];

                data = read_value();

                if (data == 0) {  //Se sono finiti i dati
                    return true; //Uscita
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

        }

    }

    private boolean detrend() { //Calcola la regressione lineare e la sottrae, opera su una finestra mobile. Toglie la "tendenza"

        int hwin = 40;
        int win = 2*hwin +1;
        int i;

        float sumx,sumy,sumxy,sumx2;
        float a,b;

        sumx = sumy = sumxy = sumx2 = 0;

        if (index_interp >= 43200) {
            System.out.print("Error: Index out of range");
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

    private void smooth() { //Moving average filter

        float sumx,sumy;
        int i,win;

        win = 5;
        sumx = sumy = 0;

        for (i = 0;i<win;i++){
            sumx += detrend.time_pico[i];
            sumy += detrend.interval[i];
        }

        smooth.interval[0] = sumx/win;
        smooth.time_pico[0] = sumy/win;

        sumx -= detrend.time_pico[0];
        sumy -= detrend.interval[0];

        for (;i<index_interp;i++) {

            sumx += detrend.time_pico[i];
            sumy += detrend.interval[i];

            smooth.interval[i-win+1] = sumy/win;
            smooth.time_pico[i-win+1] = sumx/win;

            sumx -= detrend.time_pico[i-win+1];
            sumy -= detrend.interval[i-win+1];

        }

    }


    private void ht() { //Hilbert transform

        final int lfilt = 128;

        int npt;

        //time -> time_pico
        //x -> interval
        final float[] xh = new float[index_interp];
        ampl = new float[index_interp];
        final float[] phase = new float[index_interp];
        omega = new float[index_interp];
        final float[] hilb = new float[lfilt+1];
        final float pi, pi2;
        float xt, xht, yt;

        pi = (float) Math.PI;
        pi2 = 2*pi;

        for (int i=1; i<=lfilt; i++) {
            hilb[i] = (float) (1 / ((i - lfilt / 2) - 0.5) / pi);
        }

        npt=index_interp-1;

        for (int l=1; l<=npt-lfilt+1; l++) {
            yt = 0;
            for (int i=1; i<=lfilt; i++)
                yt = yt+smooth.interval[l+i-1]*hilb[lfilt+1-i];
            xh[l] = yt;
        }

        /* shifting lfilt/1+1/2 points */
        for (int i=1; i<=npt-lfilt; i++) {
            xh[i] = (float) 0.5*(xh[i]+xh[i+1]);
        }
        for (int i=npt-lfilt; i>=1; i--)
            xh[i+lfilt/2]=xh[i];

        /* writing zeros */
        for (int i=1; i<=lfilt/2; i++) {
            xh[i] = 0;
            xh[npt+1-i] = 0;
        }


        /* Ampl and phase */
        for (int i=lfilt/2+1; i<=npt-lfilt/2; i++) {
            xt = smooth.interval[i];
            xht = xh[i];
            ampl[i] = (float)Math.sqrt(xt*xt+xht*xht);
            phase[i] = (float)Math.atan2(xht ,xt);
            if (phase[i] < phase[i-1])
                omega[i] = phase[i]-phase[i-1]+pi2;
            else
                omega[i] = phase[i]-phase[i-1];
        }

        for (int i=lfilt/2+2; i<=npt-lfilt/2; i++) {
            omega[i] = omega[i] / pi2;
        }

    }

    private void htfilt() {

        final int win=0;

        float x[],y[],sx[],sy[];

        x = y = sx = sy =new float[win];

        int i,j,hwin;

        //Carica un numero di dati pari a win
        // time, x = amp, y = omega

        if (++i >= win)
            i = 0;
        j = hwin = win/2 -1;

        for (int k=0; k<win; k++) {
            sx[k] = x[k];
            sy[k] = y[k];
        }

        Arrays.sort(sx);
        Arrays.sort(sy);

        printf("%g %g %g\n", time[j], sx[hwin], sy[hwin]);

        while (scanf("%lf %lf %lf", &time[i], &x[i], &y[i]) == 3) {

            if (++i >= win)
                i = 0;
            if (++j >= win)
                j = 0;

            for (int k=0; k<win; k++) {
                sx[k] = x[k];
                sy[k] = y[k];
            }

            Arrays.sort(sx);
            Arrays.sort(sy);
            printf("%g %g %g\n", time[j], sx[hwin], sy[hwin]);

            ampl = sx;
            omega = sy;

        }

    }

    private void amp_norm() {

        float av_amp,av;

        av = av_amp = 0;

        for (int i = 0; i< ampl.length;i++) {
            av_amp += ampl[i];
        }

        av = av_amp/ampl.length;

        for (int i = 0; i< ampl.length;i++) {
            ampl[i] = ampl[i]/av;
        }

    }

    private void ht_min_thr() {

        double max,min,mid,thres;
        float[] ord_ampl= ampl;

        Arrays.sort(ord_ampl);

        min = ord_ampl[0];
        max = ord_ampl[ord_ampl.length-1];

        mid = (max + min)/2;

        thres = (-0.555 + 1.3*(mid+1)/2);

    }

    private void st_deviation() {}

    private void mean() {}

    private void detected() {}

}

