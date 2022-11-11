package com.example.splashscreen;

import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.splashscreen.database.DataEcg;
import com.example.splashscreen.database.DataEcgDatabase;

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
    DataEcg table;

    Results res = new Results(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.analize_data_screen);

        String d;
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
                System.out.println(t.data_out());
            }
        });

        d = getDatabaseManager().noteModel().loadNote(id).start_time;
        d = d.substring(0, d.length() - 15);
        start_time.append(d);

        d = getDatabaseManager().noteModel().loadNote(id).end_time;
        d = d.substring(0, d.length() - 15);
        end_time.append(d);

        //time

        e = getDatabaseManager().noteModel().loadNote(id).number_data;
        n_samples.append(String.valueOf(e));

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


}
