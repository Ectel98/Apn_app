/*

References

Goldberger, A., Amaral, L., Glass, L., Hausdorff, J., Ivanov, P. C., Mark, R., ... & Stanley, H. E. (2000). PhysioBank, PhysioToolkit, and PhysioNet: Components of a new research resource for complex physiologic signals. Circulation [Online]. 101 (23), pp. e215–e220.

 */

package com.example.splashscreen;

import android.content.Context;

import com.example.splashscreen.database.DataEcgDatabase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class Results {

    private final Context context;


    private DataEcgDatabase ecg_database;

    DataEcgDatabase getDatabaseManager() {
        if (ecg_database == null)
            ecg_database = DataEcgDatabase.getDatabase(context);
        return ecg_database;
    }

    private long id;

    static class time_parameters {

        public List<Double> interval;
        public List<Double> time_pico;

        public time_parameters() {
            interval = new ArrayList<>();
            time_pico = new ArrayList<>();
        }
    }

    static class ht_parameters {

        public List<Double> time;
        public List<Double> ampl;
        public List<Double> omega;

        public ht_parameters() {
            time = new ArrayList<>();
            ampl = new ArrayList<>();
            omega = new ArrayList<>();
        }
    }

    static class win_data {

        public List<Double> start;
        public List<Double> avy;
        public List<Double> sdy;
        public List<Double> ydetw;
        public List<Double> avz;
        public List<Double> sdz;
        public List<Double> zdetw;
        public List<Boolean> result;
        public int numb_positive;

        public win_data() {
            start = new ArrayList<>();
            avy = new ArrayList<>();
            sdy = new ArrayList<>();
            ydetw = new ArrayList<>();
            avz  = new ArrayList<>();
            sdz = new ArrayList<>();
            zdetw = new ArrayList<>();
            result = new ArrayList<>();
        }

        public void limits() {

            final double AVAMP0=0.65;
            final double AVAMP1=2.5;
            final double SDAMP0=0;
            final double SDAMP1=0.6f;
            final double AMPTIME0=0.006;
            final double AMPTIME1=1;
            final double AVFREQ0=0.01;
            final double AVFREQ1=0.055;
            final double SDFREQ0=0;
            final double SDFREQ1=0.01;
            final double FREQTIME0=0.7;
            final double FREQTIME1=1;


            for (int i = 0;i<avy.size();i++) {
                if ((avy.get(i) >= AVAMP0 && avy.get(i) <= AVAMP1) && (sdy.get(i) >= SDAMP0 && sdy.get(i)<= SDAMP1) && (ydetw.get(i) >= AMPTIME0 && ydetw.get(i) <= AMPTIME1) && (avz.get(i) >= AVFREQ0 && avz.get(i) <= AVFREQ1) && (sdz.get(i) >= SDFREQ0 && sdz.get(i) <= SDFREQ1) && (zdetw.get(i) >= FREQTIME0 && zdetw.get(i) <= FREQTIME1))
                    result.add(true);
                else
                    result.add(false);
            }
        }
    }


    static class time_interval {

        private List<Date> st_time;
        private List<Date> end_time;
        private Date sum;
        private final Boolean error;

        public time_interval() {
            st_time = new ArrayList<>();
            end_time = new ArrayList<>();
            sum = new Date();
            error = false;
        }

        public time_interval(boolean e) {
            error = e;
        }

        public void add(long a,long b) {
            st_time.add(new Date(a));
            end_time.add(new Date(b));
        }

        public void add(long c) {
            sum = new Date(c);
        }

        public void data_out() {

            return;

        }

    }

    private long n_data;

    private int index = 0;                     // Indici di lettura
    private int new_index = 0;                 //


    public Results(Context context) {
        this.context = context;
    }


    public time_interval start_analisy(long identity) {

        time_parameters read,interp,detrend,smooth,smooth_in_s;
        ht_parameters ht,htfilt,amp_norm;
        win_data wdata;

        double thrs;

        id = identity;                        //ID tabella database

        read = first_load_data();

        if (n_data < 100) {
            System.out.print("Dati insufficenti");
            return new time_interval(true);
        }

        interp = linear_interpolation(read);

        detrend = fdetrend(interp);

        smooth = fsmooth(detrend);

        smooth_in_s = ms_to_s(smooth);

        ht = fht(smooth_in_s);

        htfilt = fhtfilt(ht);

        amp_norm = famp_norm(htfilt);

        thrs = fht_min_thr(amp_norm);

        wdata = htavsd(thrs,amp_norm);

        wdata.limits();

        return detruns(wdata);

    }

    private time_parameters first_load_data() {     //restituisce x e y per l'interpolazione

        time_parameters read = new time_parameters();

        read.time_pico.add(read_value());
        read.interval.add(read_value());
        read.time_pico.set(0,read.time_pico.get(0)+read.interval.get(0));

        read.interval.add(read_value());
        read.time_pico.set(1,read.time_pico.get(1)+read.interval.get(1));

        return read;

    }


    private double read_value() {

        double value;

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

    private time_parameters linear_interpolation(time_parameters read) { // Interpolazione lineare, Ricampiono ogni secondo

        // X -> time_pico [ms]
        // Y -> interval  [ms]

        double y0;
        double a;
        double b;
        double data;
        double x0;

        time_parameters interp = new time_parameters();

        interp.interval.add(read_value());
        read.time_pico.set(0,read.time_pico.get(0)+read.interval.get(0));

        x0 = read.time_pico.get(0) + 1000;

        while(true) {

            if (x0 > read.time_pico.get(0)) {

                read.time_pico.set(0,(read.time_pico.get(1)));
                read.interval.set(0,(read.interval.get(1)));

                data = read_value();

                if (data == 0) {  //Se sono finiti i dati
                    return interp; //Uscita
                } else {
                    read.interval.add(data);
                    read.time_pico.set(1,read.time_pico.get(1)+read.interval.get(1));
                }
            }

            b = (read.interval.get(1) - read.interval.get(0)) / (read.time_pico.get(1) - read.time_pico.get(0));
            a = read.interval.get(0) - b * read.time_pico.get(0);

            y0 = (b * x0 + a);

            interp.interval.add(y0);
            interp.time_pico.add(x0);

            x0+=1000;

        }

    }

    private time_parameters fdetrend(time_parameters interp) { //Calcola la regressione lineare e la sottrae: opera su una finestra mobile. Toglie la "tendenza"

        int hwin = 40;
        int win = 2*hwin +1;
        int i;

        float sumx,sumy,sumxy,sumx2;
        float a,b;

        time_parameters detrend = new time_parameters();

        sumx = sumy = sumxy = sumx2 = 0;

        for (i=0; i<win; i++) {
            sumx += interp.time_pico.get(i);
            sumy += interp.interval.get(i);
            sumxy += interp.time_pico.get(i)*interp.interval.get(i);
            sumx2 += interp.time_pico.get(i)*interp.time_pico.get(i);
        }

        b = (sumxy - sumx*sumy/win) / (sumx2 - sumx*sumx/win);
        a = sumy/win - b*sumx/win;

        for (i=0; i<=hwin; i++) {
            detrend.interval.add(interp.interval.get(i) - (a + b * interp.time_pico.get(i)));
            detrend.time_pico.add(interp.time_pico.get(i));
        }

        for (i=win ; i<interp.time_pico.size(); i++) {
            sumx += interp.time_pico.get(i)-interp.time_pico.get(i-win);
            sumy += interp.interval.get(i)-interp.interval.get(i-win);
            sumxy += interp.time_pico.get(i)*interp.interval.get(i)-interp.time_pico.get(i-win)*interp.interval.get(i-win);
            sumx2 += interp.time_pico.get(i)*interp.time_pico.get(i)-interp.time_pico.get(i-win)*interp.time_pico.get(i-win);

            b = (sumxy - sumx*sumy/win) / (sumx2 - sumx*sumx/win);
            a = sumy/win - b*sumx/win;

            detrend.interval.add(interp.interval.get(i-hwin) - (a + b*interp.time_pico.get(i-hwin)));
            detrend.time_pico.add(interp.time_pico.get(i-hwin));
        }

        for (i=i-hwin; i<interp.time_pico.size(); i++) {
            detrend.interval.add(interp.interval.get(i) - (a + b * interp.time_pico.get(i)));
            detrend.time_pico.add(interp.time_pico.get(i));
        }

        return detrend;

    }

    private time_parameters fsmooth(time_parameters detrend) { //Moving average filter

        double sumx,sumy;
        int i,win;

        time_parameters smooth = new time_parameters();

        win = 5;
        sumx = sumy = 0;

        for (i = 0;i<win;i++){
            sumx += detrend.time_pico.get(i);
            sumy += detrend.interval.get(i);
        }

        smooth.interval.add(sumy/win);
        smooth.time_pico.add(sumx/win);

        sumx -= detrend.time_pico.get(0);
        sumy -= detrend.interval.get(0);

        for (i = win;i<detrend.time_pico.size();i++) {

            sumx += detrend.time_pico.get(i);
            sumy += detrend.interval.get(i);

            smooth.interval.add(sumy/win);
            smooth.time_pico.add(sumx/win);

            sumx -= detrend.time_pico.get(i-win+1);
            sumy -= detrend.interval.get(i-win+1);

        }

        return smooth;

    }

    private time_parameters ms_to_s(time_parameters u) {

        time_parameters p;

        for (int i = 0; i<u.time_pico.size();i++) {
            u.time_pico.set(i,u.time_pico.get(i)/1000);
            u.interval.set(i,u.interval.get(i)/1000);
        }

        p = u;

        return p;

    }


    private ht_parameters fht(time_parameters smooth) { //Hilbert transform

        final int lfilt = 128;

        ht_parameters ht = new ht_parameters();

        int npt;

        //time -> time_pico
        //x -> interval
        final double[] xh = new double[smooth.time_pico.size()];
        final double[] phase = new double[smooth.time_pico.size()];
        final double[] hilb = new double[lfilt+1];
        final double[] omega = new double[smooth.time_pico.size()];
        final double[] ampl = new double[smooth.time_pico.size()];
        final double pi, pi2;
        double xt, xht, yt;

        pi = (float) Math.PI;
        pi2 = 2*pi;

        ht.ampl.add(0.);
        ht.omega.add(0.);

        for (int i=1; i<=lfilt; i++) {
            hilb[i] = (1 / ((i - lfilt / 2) - 0.5) / pi);
        }

        npt=smooth.time_pico.size()-1;

        for (int l=1; l<=npt-lfilt+1; l++) {
            yt = 0;
            for (int i=1; i<=lfilt; i++)
                yt = yt+smooth.interval.get(l+i-1)*hilb[lfilt+1-i];
            xh[l] = yt;
        }

        /* shifting lfilt/1+1/2 points */
        for (int i=1; i<=npt-lfilt; i++) {
            xh[i] = (float) 0.5*(xh[i]+xh[i+1]);
        }


        System.arraycopy(xh, 1, xh, 65, npt - lfilt);


        /* Ampl and phase */
        for (int i=lfilt/2+1; i<=npt-lfilt/2; i++) {
            xt = smooth.interval.get(i);
            xht = xh[i];
            ampl[i] = (Math.sqrt(xt*xt+xht*xht));
            phase[i] = Math.atan2(xht ,xt);
            if (phase[i] < phase[i-1])
                omega[i]=(phase[i]-phase[i-1]+pi2);
            else
                omega[i] = (phase[i]-phase[i-1]);
        }

        for (int i=lfilt/2+2; i<=npt-lfilt/2; i++) {
            ht.omega.add(omega[i] / pi2);
            ht.ampl.add(ampl[i]);
            ht.time.add(smooth.time_pico.get(i));
        }

        return ht;

    }

    private ht_parameters fhtfilt(ht_parameters ht) {

        ht_parameters htfilt = new ht_parameters();

        final int win=60;

        List<Double> v_sx;
        List<Double> v_sy;

        List<Double> sx = new ArrayList<>();
        List<Double> sy = new ArrayList<>();

        int j,hwin;

        //Carica un numero di dati pari a win
        // time, x = amp, y = omega

        j = hwin = win/2 -1;

        for (int k=0; k<win; k++) {
            sx.add(ht.ampl.get(k));
            sy.add(ht.omega.get(k));
        }

        v_sx = new ArrayList<>(sx);
        v_sy = new ArrayList<>(sy);

        Collections.sort(sx);
        Collections.sort(sy);

        htfilt.time.add(ht.time.get(j));
        htfilt.ampl.add(sx.get(hwin));
        htfilt.omega.add(sy.get(hwin));

        for (int e = win; e<ht.ampl.size(); e++) {

            for (int k=1; k<win; k++) {
                v_sx.set(k-1,v_sx.get(k));
                v_sy.set(k-1,v_sy.get(k));
            }

            v_sx.set(win-1,ht.ampl.get(e));
            v_sy.set(win-1,ht.ampl.get(e));

            j++;

            sx = new ArrayList<>(v_sx);
            sy = new ArrayList<>(v_sy);

            Collections.sort(sx);
            Collections.sort(sy);

            htfilt.time.add(ht.time.get(j));
            htfilt.ampl.add(sx.get(hwin));
            htfilt.omega.add(sy.get(hwin));

        }

        return htfilt;

    }

    private ht_parameters famp_norm(ht_parameters htfilt) {

        float av_amp,av;

        ht_parameters amp_norm = new ht_parameters();

        av_amp = 0;

        for (int i = 0; i< htfilt.ampl.size();i++) {
            av_amp += htfilt.ampl.get(i);
        }

        av = av_amp/htfilt.ampl.size();

        for (int i = 0; i< htfilt.ampl.size();i++) {
            amp_norm.ampl.add(htfilt.ampl.get(i)/av);
        }

        amp_norm.omega = new ArrayList<>(htfilt.omega);
        amp_norm.time = new ArrayList<>(htfilt.time);

        return amp_norm;

    }

    private double fht_min_thr(ht_parameters amp_norm) {

        double max,min,mid,thres;
        List<Double> ord_ampl= new ArrayList<>(amp_norm.ampl);

        Collections.sort(ord_ampl);

        min = ord_ampl.get(0);
        max = ord_ampl.get(ord_ampl.size()-1);

        mid = (max + min)/2;

        thres = (-0.555 + 1.3*(mid+1)/2);

        return thres;

    }

    private win_data htavsd (double thres, ht_parameters amp_norm) {

        win_data wdata;

        int e = 1;

        int i;

        final int incr = 60;     //Incremento

        final int win = 300;

        //x -> amp_norm.time
        //y -> amp_norm.ampl
        //z -> amp_norm.omega

        double start,sumy,sumz,sumzz,sumyy,avy,avz,sdy,sdz;
        int ydet,zdet;

        wdata = new win_data();

        ydet = zdet = 0;

        start = amp_norm.time.get(0);
        sumy = amp_norm.ampl.get(0);
        sumz = amp_norm.omega.get(0);
        sumyy = amp_norm.ampl.get(0)*amp_norm.ampl.get(0);
        sumzz = sumz*sumz;

        if (amp_norm.ampl.get(0) >= thres)
            ydet++;
        if (amp_norm.omega.get(0) <= 0.06)
            zdet++;

        for (i=1;  i<win; i++) {

            sumy += amp_norm.ampl.get(i);
            sumz += amp_norm.omega.get(i);
            sumyy += amp_norm.ampl.get(i)*amp_norm.ampl.get(i);
            sumzz += amp_norm.omega.get(i)*amp_norm.omega.get(i);

            if (amp_norm.ampl.get(i) >= thres)
                ydet++;
            if (amp_norm.omega.get(i) <= 0.06)
                zdet++;
        }

        avy = sumy/win;
        avz = sumz/win;
        sdy = Math.sqrt((sumyy - sumy*sumy/win)/(win-1));
        sdz = Math.sqrt((sumzz - sumz*sumz/win)/(win-1));

        wdata.start.add(start);
        wdata.avy.add(avy);
        wdata.sdy.add(sdy);
        wdata.ydetw.add(((double)ydet)/win);
        wdata.avz.add(avz);
        wdata.sdz.add(sdz);
        wdata.zdetw.add(((double)zdet)/win);

        for (int j=0; j<incr; j++) {
            sumy -= amp_norm.ampl.get(j);
            sumz -= amp_norm.omega.get(j);
            sumyy -= amp_norm.ampl.get(j)*amp_norm.ampl.get(j);
            sumzz -= amp_norm.omega.get(j)*amp_norm.omega.get(j);

            if (amp_norm.ampl.get(j) >= thres)
                ydet--;
            if (amp_norm.omega.get(j) <= 0.06)
                zdet--;
        }

        start += incr;

        i = win;

        while (i<amp_norm.ampl.size()) {   //Da sistemare: i arriva fino a win

            while (amp_norm.time.get(i)< start && i<amp_norm.ampl.size())  {
                i++;
            }

            if (amp_norm.time.size()-i<incr)
                break;

            sumy += amp_norm.ampl.get(i);
            sumz += amp_norm.omega.get(i);
            sumyy += amp_norm.ampl.get(i)*amp_norm.ampl.get(i);
            sumzz += amp_norm.omega.get(i)*amp_norm.omega.get(i);

            if (amp_norm.ampl.get(i) >= thres)
                ydet++;
            if (amp_norm.omega.get(i) <= 0.06)
                zdet++;


            for (int j=i+1; j<i+incr; j++) {

                sumy += amp_norm.ampl.get(j);
                sumz += amp_norm.omega.get(j);
                sumyy += amp_norm.ampl.get(j)*amp_norm.ampl.get(j);
                sumzz += amp_norm.omega.get(j)*amp_norm.omega.get(j);

                if (amp_norm.ampl.get(j) >= thres)
                    ydet++;
                if (amp_norm.omega.get(j) <= 0.06)
                    zdet++;
            }

            i+=incr;

            avy = sumy/win;
            avz = sumz/win;
            sdy = Math.sqrt((sumyy - sumy*sumy/win)/(win-1));
            sdz = Math.sqrt((sumzz - sumz*sumz/win)/(win-1));

            wdata.start.add(start);
            wdata.avy.add(avy);
            wdata.sdy.add(sdy);
            wdata.ydetw.add(((double)ydet)/(win*e));
            wdata.avz.add(avz);
            wdata.sdz.add(sdz);
            wdata.zdetw.add(((double)zdet)/(win*e));


            for (int j=i-win; j<(i-win)+incr; j++) {
                sumy -= amp_norm.ampl.get(j);
                sumz -= amp_norm.omega.get(j);
                sumyy -= amp_norm.ampl.get(j)*amp_norm.ampl.get(j);
                sumzz -= amp_norm.omega.get(j)*amp_norm.omega.get(j);
                if (amp_norm.ampl.get(j) >= thres)
                    ydet--;
                if (amp_norm.omega.get(j) <= 0.06)
                    zdet--;
            }


            start += incr;

        }

        return wdata;

    }

    private time_interval detruns (win_data wdata) {

        final int incr = 60;
        final int win = 300;
        int min = 900;

        time_interval t_inter = new time_interval();

        int i = 1;

        boolean runflag,runflag0;
        double runstart,lasttime,time,runstart0,runend0,sum;

        List<Double> times = new ArrayList<>();

        for (int e = 0; e<wdata.start.size();e++) {
            if (wdata.result.get(e)) {
                times.add(wdata.start.get(e));
            }
        }


        runflag = runflag0 = false;

        runstart = lasttime = times.get(0);

        runend0 = runstart0 = sum = 0;

        while (i<times.size()) {

            time = times.get(i);

            if (time - lasttime != incr) {
                if (lasttime - runstart + win >= min) {
                    if (!runflag0) {
                        runflag0 = true;
                        runstart0 = runstart;
                        runend0 = lasttime;
                    }
                    else if (runstart <= runend0 + win) {
                        runend0 = lasttime;
                    }
                    else {
                        t_inter.add((long)runstart0,(long)runend0+win);
                        sum += runend0-runstart0+win;
                        runstart0 = runstart;
                        runend0 = lasttime;
                    }
                }
                runstart = time;
            }

            lasttime = time;

            i++;
        }


        if (lasttime - runstart + win >= min)
            runflag = true;

        if (runflag0) {
            if (runflag && runstart <= runend0 + win) {
                runend0 = lasttime;
                runflag = false;
            }
            t_inter.add((long)runstart0,(long)runend0+win);
            sum += runend0-runstart0+win;
        }
        if (runflag) {
            t_inter.add((long)runstart,(long)lasttime+win);
            sum += lasttime-runstart+win;
        }
        t_inter.add((long)sum);

        return t_inter;

    }

}