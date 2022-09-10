package com.example.splashscreen;

import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class ContentMain extends AppCompatActivity {

    Button start_m_button;
    Button d_button;
    Button st_button;

    Intent myIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.content_main);

        start_m_button = (Button) findViewById(R.id.start_monit_button);
        start_m_button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                myIntent = new Intent(ContentMain.this, Monitoring.class);
                ContentMain.this.startActivity(myIntent);
            }
        });

        d_button = (Button) findViewById(R.id.data_button);
        d_button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                myIntent = new Intent(ContentMain.this, DataSaved.class);
                ContentMain.this.startActivity(myIntent);
            }
        });


        st_button = (Button) findViewById(R.id.settings_button);
        st_button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                myIntent = new Intent(ContentMain.this, Settings.class);
                ContentMain.this.startActivity(myIntent);
            }
        });


    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        if ((keyCode == KeyEvent.KEYCODE_BACK))
        {
            send_message_to_service("close_app");
            finishAffinity();
        }
        return super.onKeyDown(keyCode, event);
    }

    private void send_message_to_service(String message_to_send) {        //Invia messaggi al servizio
        Intent intent = new Intent(this, BluetoothService.class);
        intent.putExtra("message_to_send_esp", message_to_send);
        startService(intent);
    }

}

