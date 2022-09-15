package com.example.splashscreen;


import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import java.util.Calendar;


import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.splashscreen.database.DataEcg;
import com.example.splashscreen.database.DataEcgDatabase;

import java.util.List;
import java.util.UUID;


public class BluetoothService extends Service {


    private static final int NOTIF_ID = 1;
    private static final String NOTIF_CHANNEL_ID = "Channel_Id";

    private DataEcg ecg_data_table;
    private DataEcgDatabase ecg_database;
    private DataEcgDatabase getDatabaseManager() {
        if (ecg_database==null)
            ecg_database=DataEcgDatabase.getDatabase(this);
        return ecg_database;
    }


    BluetoothManager btManager;
    BluetoothAdapter btAdapter;
    BluetoothLeScanner btScanner;
    BluetoothGatt bluetoothGatt;
    BluetoothDevice device;

    Context context = this;

    Handler handler;

    String message_to_esp;

    int mStartMode;

    Boolean first_time_flag;
    Boolean connec_flag;
    Boolean monitoring_flag;   //Monitoraggio lato app
    Boolean monitoring_esp_flag;
    Boolean foreground_flag;

    IBinder mBinder;

    boolean mAllowRebind;

    protected static final UUID CHARACTERISTIC_UPDATE_NOTIFICATION_DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");


    @Override
    public void onCreate() {

        btManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        btAdapter = btManager.getAdapter();
        btScanner = btAdapter.getBluetoothLeScanner();
        mBinder = new BluetoothBinder();
        handler = new Handler();

        first_time_flag = true;
        connec_flag = false;
        monitoring_flag = false;
        monitoring_esp_flag = false;
        foreground_flag = false;

    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) { //Viene eseguito allo "start" dell'activity

        if (intent.getExtras() == null) {
            if (connec_flag)  //Controlla se ci sono dispositivi accopiati, se ci sono fa il cambio di activity subito
                send_message_to_activity("certo");
            btScanner.startScan(leScanCallback);  //Avvio la scansione
            first_time_flag = true;
        }

        else {
            message_to_esp = (intent.getExtras().getString("message_to_send_esp"));
            first_time_flag = false;

            if (message_to_esp.equals("start_monitoring")) {

                monitoring_flag = true;
                bluetoothGatt.discoverServices();

            }

            else if (message_to_esp.equals("end_monitoring")) {
                stop_monit();
                bluetoothGatt.discoverServices();
            }


            else if (message_to_esp.equals("close_app")) {
                if (!foreground_flag) {
                    if (connec_flag) //Se sono connesso
                        bluetoothGatt.disconnect();  // disconnetti dall'esp
                    else
                        btScanner.stopScan(leScanCallback);
                    stopSelf(); //termina la service
                }
            }

        }

        return mStartMode;

    }


    public void startForeground() {
        Intent notificationIntent = new Intent(this, ContentMain.class);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        startForeground(NOTIF_ID, new NotificationCompat.Builder(this, NOTIF_CHANNEL_ID)
                .setOngoing(true)
                .setContentTitle("Sleep Apnea Detecor")
                .setContentText("BLE reciver in background")
                .setContentIntent(pendingIntent)
                .build());
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void startMyOwnForeground(){
        String NOTIFICATION_CHANNEL_ID = "com.example.simpleapp";
        String channelName = "Sleep Apnea Detecor";
        NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_NONE);

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        assert manager != null;
        manager.createNotificationChannel(chan);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
        Notification notification = notificationBuilder.setOngoing(true)
                .setContentTitle("Sleep Apnea Detecor")
                .setPriority(NotificationManager.IMPORTANCE_MIN)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build();
        startForeground(2, notification);
    }



    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public boolean onUnbind(Intent intent) {
        return mAllowRebind;
    }


    @Override
    public void onRebind(Intent intent) {

    }


    @Override
    public void onDestroy() {

        stopForeground(true);

    }

    public class BluetoothBinder extends Binder {
        BluetoothService getService() {
            return BluetoothService.this;
        }
    }

    //--------------------------- Scan callback-----------------------

    private ScanCallback leScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {

            System.out.println("Name:" + result.getDevice().getName() + " MAC: " + result.getDevice().getAddress());

            if (result.getDevice().getAddress().equals("78:21:84:98:72:02")) {  //Esp trovato ?

                btScanner.stopScan(leScanCallback);  //Termino la scansione

                device = result.getDevice();

                bluetoothGatt = device.connectGatt(context, true, gattCallback);  //Stabiliscono la connessione con l'esp
                bluetoothGatt.connect();
                connec_flag = true;

                handler.postDelayed(new Runnable() {           //Aspetto 600ms per avviare il discover
                    @Override
                    public void run() {
                        bluetoothGatt.discoverServices();
                        send_message_to_activity("certo");
                    }
                }, 600);
            }
        }
    };

    //Routine di avvio

    private void start_monit() {

        create_table();         //crea la nuova tabella                //Creare funzione da chiamare
        foreground_flag = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            startMyOwnForeground();
        else
            startForeground();
        monitoring_esp_flag = true;

    }

    //Routine di fine

    private void stop_monit() {

        foreground_flag = false;
        stopForeground(true);
        end_table();
        monitoring_flag = false; //falg di start monitoring spenta
        monitoring_esp_flag = false;

    }



    //--------------------------Invia messaggi all'activity-----------------------

    private void send_message_to_activity(String message_to_send) {
        Intent intent = new Intent("send_string");
        intent.putExtra("send_string", message_to_send);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }


    //--------------------------Invia messaggi all'esp-----------------------

    private void send_message_to_esp(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        byte[] strBytes = message_to_esp.getBytes();
        characteristic.setValue(strBytes);
        characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
        gatt.writeCharacteristic(characteristic);
    }

    //---------------------------Invia messaggio al database----------------

    private void create_table() {
        ecg_data_table = new DataEcg();
        ecg_data_table.id =  getDatabaseManager().noteModel().loadAllNotes().size() + 1;//leggo l'ultimo id scritto
        ecg_data_table.start_time = Calendar.getInstance().getTime().toString();        //Tempo di inizio
        ecg_data_table.number_data = 0;
        System.out.println(ecg_data_table.start_time);
        ecg_data_table.data = "";
        getDatabaseManager().noteModel().insert(ecg_data_table);                        // Inserisco la tabella
    }

    private void end_table() { //Chiamato quando la monitoring mi comunica la fine del monitoraggio
        if (ecg_data_table!=null) {
            ecg_data_table.end_time = Calendar.getInstance().getTime().toString();         //Tempo di fine
            getDatabaseManager().noteModel().insert(ecg_data_table);                       // Sovrascrivo la tabella
            ecg_data_table = null;
        }
    }

    private void send_message_to_database(String data) {
        ecg_data_table.number_data++;
        ecg_data_table.data += data;                                                  // Aggiungo il dato
        getDatabaseManager().noteModel().insert(ecg_data_table);                      // Sovrascrivo la tabella
    }

    //----------------------------Imposta le notifiche----------------------

    public boolean setCharacteristicNotification(BluetoothGatt bluetoothGatt, BluetoothGattCharacteristic characteristic,boolean enable) {
        bluetoothGatt.setCharacteristicNotification(characteristic, enable);
        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CHARACTERISTIC_UPDATE_NOTIFICATION_DESCRIPTOR_UUID);
        descriptor.setValue(enable ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE : new byte[]{0x00, 0x00});
        return bluetoothGatt.writeDescriptor(descriptor);
    }

    //--------------------------BluetoothGatt Callback-----------------------

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            byte[] dataint = characteristic.getValue();
            String datastr = "";

            for (int i=0; i<dataint.length;i++)
                datastr += ((char)dataint[i]);

            System.out.println(datastr);


            if (monitoring_flag) {

                if (datastr.equals("started")) { //lo fa partire se non è già partito
                    if (!monitoring_esp_flag)
                        start_monit();
                    send_message_to_activity(datastr);
                }

                if (datastr.equals("Error: ld")|| datastr.equals("Error: temp")|| datastr.equals("Error: sup")) {
                    if (monitoring_esp_flag) {
                        stop_monit();
                        monitoring_flag = false;
                        monitoring_esp_flag = false;
                    }
                    send_message_to_activity(datastr);
                }

                if (datastr.equals("ended")) {
                    if (monitoring_esp_flag) {
                        monitoring_flag = false;
                        stop_monit();
                    }
                    send_message_to_activity(datastr);
                }


                if (datastr.matches("-?\\d+(\\.\\d+)?")) {
                  send_message_to_database(datastr);
                }

            }
            else if (!monitoring_flag)
                send_message_to_activity(datastr);
        }


        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            gatt.readCharacteristic(characteristic);
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);

            if (status == BluetoothGatt.GATT_SUCCESS) {

                List<BluetoothGattService> services = gatt.getServices();

                for (BluetoothGattService service : services) {

                    List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();

                    for (BluetoothGattCharacteristic SingleCharacteristic : characteristics) {

                        if (SingleCharacteristic.getUuid().toString().equals("6e400003-b5a3-f393-e0a9-e50e24dcca9e") && first_time_flag)
                            setCharacteristicNotification(gatt, SingleCharacteristic, true);

                        if (SingleCharacteristic.getUuid().toString().equals("6e400002-b5a3-f393-e0a9-e50e24dcca9e") && !first_time_flag)
                            send_message_to_esp(gatt, SingleCharacteristic);

                    }
                }
            }
        }
    };
}