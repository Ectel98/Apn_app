package com.example.splashscreen;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class Monitoring extends AppCompatActivity {

    TextView state;
    TextView time_start;
    TextView time_end;
    TextView time_total;

    Button start_end_moni;

    Boolean button_flag;
    LocalDateTime stat_date;


    public Monitoring() {
        if(BuildConfig.DEBUG)
            StrictMode.enableDefaults();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.monitoring_screen);

        button_flag = true;

        state =  findViewById(R.id.state_text);
        time_start = findViewById(R.id.time_start_text);
        time_end =  findViewById(R.id.time_end_text);
        time_total =  findViewById(R.id.time_text);

        start_end_moni = findViewById(R.id.start_end_button);

        start_end_moni.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                if (button_flag)
                    send_message_to_service("start_monitoring");

                else {
                    send_message_to_service("end_monitoring");

                    String d = Calendar.getInstance().getTime().toString();
                    d = d.substring(0, d.length() - 15);
                    time_end.setText("Fine: " + d);

                }

            }
        });

        if (button_flag) {
            Duration total_data;
            Instant acutal_data =  Instant.now();
            total_data = Duration.between(stat_date,acutal_data);

            time_total.setText("Tempo: " + total_data.toString());
        }

        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, new IntentFilter("send_string"));
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiverTime, new IntentFilter("send_time"));

        send_message_to_service("stato"); //invio messaggio all'esp "start_monitoring_activity" e aspetto un responso con lo stato del monitoraggio

    }

    private void send_message_to_service(String message_to_send) {        //Invia messaggi al servizio
        System.out.println("Messaggio inviato alla Service: " + message_to_send);
        Intent intent = new Intent(this, BluetoothService.class);
        intent.putExtra("message_to_send_esp", message_to_send);
        startService(intent);
    }


    private final BroadcastReceiver messageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            String data = intent.getExtras().getString("state");

            if (data.equals("started")) {

                button_flag = false;
                start_end_moni.setText(R.string.text_button_end_sleep);
                state.setText("Stato: Monitoraggio attivo");

                time_end.setText("Fine: ");

            } else if (data.equals("ended")) {

                button_flag = true;
                start_end_moni.setText(R.string.text_button_start_sleep);
                state.setText("Stato:  Monitoraggio non attivo");

            } else if (data.equals("Error: ld")) {

                state.setText("Error: Leads disconnected");


            } else if (data.equals("Error: temp")) {

                state.setText("Error: Temp out of range");

            } else if (data.equals("Error: sup")) {

                state.setText("Error: battery is too low or battery is in charging");

            }

        }

    };

    private final BroadcastReceiver messageReceiverTime = new BroadcastReceiver() {
        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void onReceive(Context context, Intent intent) {

            String time = intent.getExtras().getString("time");

            time_start.setText("Inizio: "+time.substring(0,time.length()-15));

            DateTimeFormatter f = DateTimeFormatter.ofPattern("hh:mm a, EEE M/d/uuuu");

            stat_date = LocalDateTime.parse(time,f);

        }

    };


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK))
        {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(messageReceiver);
            finish();
        }
        return super.onKeyDown(keyCode, event);
    }

}