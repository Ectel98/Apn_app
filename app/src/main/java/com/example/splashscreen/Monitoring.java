package com.example.splashscreen;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
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
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;


public class Monitoring extends AppCompatActivity {

    TextView state;
    TextView time_start;
    TextView time_end;
    TextView time_total;

    Button start_end_moni;

    Boolean button_flag;
    String start_time;


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
                    time_end.setText("Fine: " + d.substring(4, d.length() - 15));

                    time_total.setText("Tempo: " + duration(start_time,d));

                }

            }
        });


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
                time_total.setText("Tempo: ");

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

            start_time = intent.getExtras().getString("time");

            time_start.setText("Inizio: "+start_time.substring(4,start_time.length()-15)); //Wed Nov 16 12:21:15

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