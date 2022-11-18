package com.example.splashscreen;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.example.splashscreen.database.DataEcg;
import com.example.splashscreen.database.DataEcgDatabase;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

public class AnalizeData extends AppCompatActivity {

    private DataEcgDatabase ecg_database;
    private DataEcgDatabase getDatabaseManager() {
        if (ecg_database==null)
            ecg_database=DataEcgDatabase.getDatabase(this);
        return ecg_database;
    }

    TextView start_time;
    TextView end_time;
    TextView time;
    TextView n_samples;
    TextView heart_rate;
    TextView analisi_result;
    Button delete_data;
    Button results;
    Button plot;
    DataEcg table;


    Results res = new Results(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.analize_data_screen);

        String d,r;
        long e;

        Intent i;

        start_time = findViewById(R.id.text_st_time);
        end_time = findViewById(R.id.text_en_time);
        time = findViewById(R.id.tx_time);
        n_samples = findViewById(R.id.tx_n_samples);
        heart_rate = findViewById(R.id.tx_heart_rate);
        analisi_result = findViewById(R.id.tx_analisi_result);
        delete_data = findViewById(R.id.bt_delete_data);
        results = findViewById(R.id.bt_results);
        plot = findViewById(R.id.plot_button);

        i = getIntent();

        long id = i.getExtras().getInt("id");


        delete_data.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                int size = getDatabaseManager().noteModel().loadAllNotes().size();

                for (long i = id; i<size;i++) {                                 //shift
                    table = getDatabaseManager().noteModel().loadNote(i+1);
                    table.id = i;
                    getDatabaseManager().noteModel().insert(table);
                }
                getDatabaseManager().noteModel().deleteById(size);

                setResult(RESULT_OK, null);
                finish(); //torno alla DataSaved
            }
        });

        results.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                Results.time_interval t;
                t = res.start_analisy(id);
                String s = t.data_out();
                System.out.println(t.data_out());
                if (s.equals("Errore"))
                    analisi_result.setText("Errore: dati insufficienti");
                else {
                    analisi_result.setText("Risultati dell'analisi:" + '\n' +  s);
                    plot.setVisibility(View.VISIBLE);
                }
            }
        });

        plot.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            public void onClick(View v) {
                final double[] ari = res.smooth.get_data();
                Intent i = new Intent(AnalizeData.this,Plot.class);
                i.putExtra("ar", ari);
                AnalizeData.this.startActivity(i);
            }
        });

        if (id != 1000) {         //Se non sto eseguendo l'esempio

            d = getDatabaseManager().noteModel().loadNote(id).start_time;
            start_time.append(d.substring(4, d.length() - 15));

            r = getDatabaseManager().noteModel().loadNote(id).end_time;
            end_time.append(r.substring(4, d.length() - 15));

            time.append(duration(d,r));

            e = getDatabaseManager().noteModel().loadNote(id).number_data;
            n_samples.append(String.valueOf(e));

        }

        else {
            start_time.append("Questo è un esempio");
            end_time.append("Questo è un esempio");
            time.append("Questo è un esempio");
        }

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        if ((keyCode == KeyEvent.KEYCODE_BACK))
        {
            finish();
        }
        return super.onKeyDown(keyCode, event);
    }

    public String duration(String s_start,String s_end) {

        long end = (Integer.parseInt(s_end.substring(11, 13))) * 3600 + (Integer.parseInt(s_end.substring(14, 16))) * 60 + (Integer.parseInt(s_end.substring(17, 19)));
        long start = (Integer.parseInt(s_start.substring(11, 13))) * 3600 + (Integer.parseInt(s_start.substring(14, 16))) * 60 + (Integer.parseInt(s_start.substring(17, 19)));
        long midnight = 24*3600;

        int h,m,s;
        String out,ho,mi,se;

        if (end > start) {
            h = (int) ((end - start) / 3600);
            m = (int) ((end - start) - h * 3600) / 60;
            s = (int) ((end - start) - h * 3600 - m * 60);
        }

        else {
            h = (int) ((midnight - start) / 3600);
            m = (int) ((midnight - start) - h * 3600) / 60;
            s = (int) ((midnight - start) - h * 3600 - m * 60);

            h += (int) (end / 3600);
            m += (int) (end - h * 3600) / 60;
            s += (int) (end - h * 3600 - m * 60);
        }

        if (h<10) ho = '0' + Integer.toString(h);
        else ho = Integer.toString(h);
        if (m<10) mi = '0' + Integer.toString(m);
        else mi = Integer.toString(m);
        if (s<10) se = '0' + Integer.toString(s);
        else se = Integer.toString(s);

        return (ho + ':' + mi + ':' + se);

    }

}
