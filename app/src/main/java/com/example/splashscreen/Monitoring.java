package com.example.splashscreen;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.StrictMode;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class Monitoring extends AppCompatActivity {

    TextView state;
    TextView time_start;
    TextView time_end;
    TextView time_total;

    Button start_end_moni;

    Boolean button_flag;


    public Monitoring() {
        if(BuildConfig.DEBUG)
            StrictMode.enableDefaults();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.monitoring_screen);

        button_flag = true;

        state = (TextView) findViewById(R.id.state_text);
        time_start = (TextView) findViewById(R.id.time_start_text);
        time_end = (TextView) findViewById(R.id.time_end_text);
        time_total = (TextView) findViewById(R.id.time_text);

        start_end_moni = (Button) findViewById(R.id.start_end_button);

        start_end_moni.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                if (button_flag)
                    send_message_to_service("start_monitoring");

                else
                    send_message_to_service("end_monitoring");

            }
        });

        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, new IntentFilter("send_string"));

        send_message_to_service("stato"); //invio messaggio all'esp "start_monitoring_activity" e aspetto un responso con lo stato del monitoraggio
    }

    private void send_message_to_service(String message_to_send) {        //Invia messaggi al servizio
        Intent intent = new Intent(this, BluetoothService.class);
        intent.putExtra("message_to_send_esp", message_to_send);
        startService(intent);
    }


    private BroadcastReceiver messageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            String data;
            data = intent.getExtras().getString("send_string");

            if (data.equals("started")) {

                button_flag = false;
                start_end_moni.setText(R.string.text_button_end_sleep);
                state.setText("Stato: Monitoraggio attivo");

            }

            else if (data.equals("ended")) {

                button_flag = true;
                start_end_moni.setText(R.string.text_button_start_sleep);
                state.setText("Stato:  Monitoraggio non attivo");

            }

            else if (data.equals("Error: ld")) {

                state.setText("Error: Leads disconnected");


            }

            else if (data.equals("Error: temp")) {

                state.setText("Error: Temp out of range");


            }

            else if (data.equals("Error: sup")) {

                state.setText("Error: battery is too low or battery is in charging");


            }

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