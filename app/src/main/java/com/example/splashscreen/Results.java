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

    private int index_interp = 0;

    private long n_data;

    private float x0;

    private int index = 0;                     // Indici di lettura
    private int new_index = 0;                 //



    public Results(Context context) {
        this.context = context;
    }

    public int start_analisy(long identity) {

        parameters read,interp,detrend,smooth,ht,htfilt,amp_norm;
        double thrs;

        id = identity;                        //ID tabella database

        read = first_load_data();

        if (n_data < 100) {
            System.out.print("Dati insufficenti");
            return 1;
        }

        interp = linear_interpolation(read);

        detrend = fdetrend(interp);

        smooth = fsmooth(detrend);

        ht = fht(smooth);

        htfilt = fhtfilt(ht);

        amp_norm = famp_norm(htfilt);

        thrs = fht_min_thr(amp_norm);

        htavsd(thrs,amp_norm);

        return 0;

    }

    private parameters first_load_data() {     //restituisce x e y per l'interpolazione

        parameters read = new parameters(2);

        read.time_pico[0] = read_value();
        read.interval[0] = read_value();
        read.time_pico[0] += read.interval[0];

        read.interval[1] = read_value();
        read.time_pico[1] += read.interval[1];

        return read;

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

    private parameters linear_interpolation(parameters read) { // Interpolazione lineare, Ricampiono ogni secondo

        // X -> time_pico [ms]
        // Y -> interval  [ms]

        float y0;
        float a;
        float b;
        float data;

        parameters interp = new parameters(43200);

        interp.interval[0] = read_value();
        interp.time_pico[0] += interp.interval[0];

        x0 = read.time_pico[0] + 1000;

        while(true) {

            if (x0 > read.time_pico[1]) {

                read.time_pico[0] = read.time_pico[1];
                read.interval[0] = read.interval[1];

                data = read_value();

                if (data == 0) {  //Se sono finiti i dati
                    return interp; //Uscita
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

    private parameters fdetrend(parameters interp) { //Calcola la regressione lineare e la sottrae, opera su una finestra mobile. Toglie la "tendenza"

        int hwin = 40;
        int win = 2*hwin +1;
        int i;

        float sumx,sumy,sumxy,sumx2;
        float a,b;

        parameters detrend = new parameters(index_interp);

        sumx = sumy = sumxy = sumx2 = 0;

        if (index_interp >= 43200) {
            System.out.print("Error: Index out of range");
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

        return detrend;

    }

    private parameters fsmooth(parameters detrend) { //Moving average filter

        float sumx,sumy;
        int i,win;

        parameters smooth = new parameters(index_interp);

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

        return smooth;

    }


    private parameters fht(parameters smooth) { //Hilbert transform

        final int lfilt = 128;

        parameters ht = new parameters(index_interp,index_interp,index_interp);

        int npt;

        //time -> time_pico
        //x -> interval
        final float[] xh = new float[index_interp];
        final float[] phase = new float[index_interp];
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

        if (npt - lfilt >= 0)
            System.arraycopy(xh, 1, xh, 65, npt - lfilt);


        /* Ampl and phase */
        for (int i=lfilt/2+1; i<=npt-lfilt/2; i++) {
            xt = smooth.interval[i];
            xht = xh[i];
            ht.ampl[i] = (float)Math.sqrt(xt*xt+xht*xht);
            phase[i] = (float)Math.atan2(xht ,xt);
            if (phase[i] < phase[i-1])
                ht.omega[i] = phase[i]-phase[i-1]+pi2;
            else
                ht.omega[i] = phase[i]-phase[i-1];
        }

        for (int i=lfilt/2+2; i<=npt-lfilt/2; i++) {
            ht.omega[i] = ht.omega[i] / pi2;
        }

        ht.time = smooth.time_pico;

        return ht;

    }

    private parameters fhtfilt(parameters ht) {

        parameters htfilt = new parameters(index_interp,index_interp,index_interp);

        final int win=60;

        float[] sx,sy;

        sx = sy = new float[win];

        int i,j,hwin;

        //Carica un numero di dati pari a win
        // time, x = amp, y = omega

        i = 1;
        j = hwin = win/2 -1;

        for (int k=0; k<win; k++) {
            sx[k] = ht.ampl[k];
            sy[k] = ht.omega[k];
        }

        Arrays.sort(sx);
        Arrays.sort(sy);

        htfilt.time[0] = ht.time[j];
        htfilt.ampl[0] = sx[hwin];
        htfilt.omega[0] = sy[hwin];

        for (int e = 0; e<index_interp; e++) {

            if (e>win*i)
                i++;

            for (int k=(i-1)*win; k<win*i; k++) {
                sx[k] = ht.ampl[k];
                sy[k] = ht.omega[k];
            }

            Arrays.sort(sx);
            Arrays.sort(sy);

            htfilt.time[e] = ht.time[j];
            htfilt.ampl[e] = sx[hwin];
            htfilt.omega[e] = sy[hwin];

        }

        return htfilt;

    }

    private parameters famp_norm(parameters htfilt) {

        float av_amp,av;

        parameters amp_norm = new parameters(index_interp,index_interp,index_interp);

        av_amp = 0;

        for (int i = 0; i< index_interp;i++) {
            av_amp += htfilt.ampl[i];
        }

        av = av_amp/index_interp;

        for (int i = 0; i< index_interp;i++) {
            amp_norm.ampl[i] = htfilt.ampl[i]/av;
        }

        return amp_norm;

    }

    private double fht_min_thr(parameters amp_norm) {

        double max,min,mid,thres;
        float[] ord_ampl= amp_norm.ampl;

        Arrays.sort(ord_ampl);

        min = ord_ampl[0];
        max = ord_ampl[ord_ampl.length-1];

        mid = (max + min)/2;

        thres = (-0.555 + 1.3*(mid+1)/2);

        return thres;

    }

    private void htavsd (double thres, parameters amp_norm) {

        final int incr = 3600;     //Incremento

        final int win = 18000;

        //x -> amp_norm.time
        //y -> amp_norm.ampl
        //z -> amp_norm.omega

        double start,sumy,sumz,sumzz,sumyy,avy,avz,sdy,sdz;
        int ydet,zdet;

        ydet = zdet = 0;

        start = amp_norm.time[0];
        sumy = amp_norm.ampl[0];
        sumz = amp_norm.omega[0];
        sumyy = amp_norm.ampl[0]*amp_norm.ampl[0];
        sumzz = sumz*sumz;

        if (amp_norm.ampl[0] >= thres)
            ydet++;
        if (amp_norm.omega[0] <= 0.06)
            zdet++;

        for (int i=1;  i<win; i++) {

            sumy += amp_norm.ampl[i];
            sumz += amp_norm.omega[i];
            sumyy += amp_norm.ampl[i]*amp_norm.ampl[i];
            sumzz += amp_norm.omega[i]*amp_norm.omega[i];

            if (amp_norm.ampl[i] >= thres)
                ydet++;
            if (amp_norm.omega[i] <= 0.06)
                zdet++;
        }

        avy = sumy/win;
        avz = sumz/win;
        sdy = Math.sqrt((sumyy - sumy*sumy/win)/(win-1));
        sdz = Math.sqrt((sumzz - sumz*sumz/win)/(win-1));

        printf("%02d:%02d:%02d ", start/3600, (start%3600)/60, start%60);
        printf("%f %f %f %f %f %f\n", avy, sdy, ((double)ydet)/win, avz, sdz, ((double)zdet)/win);

        if (incr == win) {
            sumy = sumz =sumyy = sumzz = 0.0;
            ydet = zdet = 0;
        }

        else {
            for (int j=0; j<incr; j++) {
                if (j >= win)
                    j = 0;
                sumy -= amp_norm.ampl[j];
                sumz -= amp_norm.omega[j];
                sumyy -= amp_norm.ampl[j]*amp_norm.ampl[j];
                sumzz -= amp_norm.omega[j]*amp_norm.omega[j];

                if (amp_norm.ampl[j] >= thres)
                    ydet--;
                if (amp_norm.omega[j] <= 0.06)
                    zdet--;
            }
        }

        start += incr;

        for (int i = win; i<index_interp; i++) {   //Da sistemare: i arriva fino a win

            sumy += amp_norm.ampl[i];
            sumz += amp_norm.omega[i];
            sumyy += amp_norm.ampl[i]*amp_norm.ampl[i];
            sumzz += amp_norm.omega[i]*amp_norm.omega[i];

            if (amp_norm.ampl[i] >= thres)
                ydet++;
            if (amp_norm.omega[i] <= 0.06)
                zdet++;

            if (++i >= win)
                i = 0;

            for (int j=1; j<incr && j<win; i++, j++) {

                if (i >= win)
                    i = 0;

                sumy += amp_norm.ampl[i];
                sumz += amp_norm.omega[i];
                sumyy += amp_norm.ampl[i]*amp_norm.ampl[i];
                sumzz += amp_norm.omega[i]*amp_norm.omega[i];

                if (amp_norm.ampl[i] >= thres)
                    ydet++;
                if (amp_norm.omega[i] <= 0.06)
                    zdet++;
            }

            if (i >= win)
                i = 0;

            avy = sumy/win;
            avz = sumz/win;
            sdy = Math.sqrt((sumyy - sumy*sumy/win)/(win-1));
            sdz = Math.sqrt((sumzz - sumz*sumz/win)/(win-1));

            printf("%02d:%02d:%02d ", start/3600, (start%3600)/60, start%60);
            printf("%f %f %f %f %f %f\n", avy, sdy, ((double)ydet)/win, avz, sdz, ((double)zdet)/win);

            if (incr == win) {
                sumy = sumz =sumyy = sumzz = 0.0;
                ydet = zdet = 0;
            }

            else {
                for (int j=0, k=j+i; j<incr; j++, k++) {
                    if (k >= win)
                        k = 0;
                    sumy -= amp_norm.ampl[k];
                    sumz -= amp_norm.omega[k];
                    sumyy -= amp_norm.ampl[k]*amp_norm.ampl[k];
                    sumzz -= amp_norm.omega[k]*amp_norm.omega[k];
                    if (amp_norm.ampl[k] >= thres)
                        ydet--;
                    if (amp_norm.omega[k] <= 0.06)
                        zdet--;
                }
            }

            start += incr;

        }

    }

    private void limits () {}

    private void detruns () {}

}

