package com.example.splashscreen;

import android.Manifest;
import android.app.AlertDialog;

import android.content.BroadcastReceiver;

import android.content.DialogInterface;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;


import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.Set;


public class ConnectionScreen extends AppCompatActivity {

    private static final int REQUEST_ENABLE_BT = 1;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;


    private Handler mHandler = new Handler();

    BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    //LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.connection_screen);

        boolean first_time = true;

        Intent intent = new Intent(this, BluetoothService.class);


        //  ----------------------------- CONTROLLO BT + PERMESSI -------------------------------------------


        //BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) { //Controlla se il dispositivo non supporta il bluetooth
            System.out.println("Il dispositivo non supporta il BL");
        }

        if (!bluetoothAdapter.isEnabled()) { //Controlla se il bluetooth è acceso e insiste
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        //Controlla i permessi per la posizione
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1001);

        /*
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            System.out.print("ciao");
        }

         */

        // -------------  Avvio il Servizio ------------------
        while (first_time) {
            if (ChekPermiss()) {
                startService(intent); //deve partire solo se tutti i permessi sono stati accettati
                first_time = false;
            }
        }

        // --------------------- Ricezione messaggi dal servizio ------------------------
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, new IntentFilter("send_string"));


    }

    public boolean ChekPermiss() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
            && bluetoothAdapter.isEnabled())
            return true;
        else
            return false;
    }



    //---------------Ricezione messaggi dal service-------------

    private BroadcastReceiver messageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String data;
            data = intent.getExtras().getString("send_string");
            if (data.equals("certo")) {
                LocalBroadcastManager.getInstance(context).unregisterReceiver(messageReceiver);
                Intent myIntent = new Intent(ConnectionScreen.this, ContentMain.class);
                ConnectionScreen.this.startActivity(myIntent);
            }
        }
    };

    // Procedura di uscita

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