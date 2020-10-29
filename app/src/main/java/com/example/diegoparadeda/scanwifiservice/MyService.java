package com.example.diegoparadeda.scanwifiservice;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class MyService extends Service {

    //from MyService to MainActivity
    final static String KEY_INT_FROM_SERVICE = "KEY_INT_FROM_SERVICE";
    final static String KEY_ARRAY_FROM_SERVICE = "KEY_ARRAY_FROM_SERVICE";
    final static String ACTION_UPDATE_CNT = "UPDATE_CNT";
    final static String ACTION_UPDATE_ARRAY = "UPDATE_ARRAY";
    final static String SERVICE_STATUS = "SERVICE_STATUS";

    //from MainActivity to MyService
    final static String KEY_MSG_TO_SERVICE = "KEY_MSG_TO_SERVICE";
    final static String ACTION_MSG_TO_SERVICE = "MSG_TO_SERVICE";
    private static final int NOTIF_ID = 1;
    private static final String NOTIF_CHANNEL_ID = "Channel_Id";
    private static final String TAG = "MyActivity";
    private static final int TODO = 0;


    MyServiceReceiver myServiceReceiver;
    MyServiceThread myServiceThread;
    WifiManager wifiManager;
    ConnectivityManager connectivityManager;
    NetworkInfo mobileInfo;
    List<ScanResult> results;
    ArrayList<String> arrayList = new ArrayList<>();
    int cnt = 0;
    int batLevel = 0;
    private int x, y = 0;
    String mac;
    BatteryManager bm;
    NetworkInfo wifiinfo;

    //Para pegar dados do GPS
    LocationListener listener;
    LocationManager locationManager;
    Location location;

    String store_aux = "";


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        Toast.makeText(getApplicationContext(),
                "Iniciando Serviço", Toast.LENGTH_LONG).show();
        myServiceReceiver = new MyServiceReceiver();
        super.onCreate();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(getApplicationContext(),
                "Iniciando comando", Toast.LENGTH_LONG).show();
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            bm = (BatteryManager) getSystemService(BATTERY_SERVICE);
        }


        //Listener do GPS

        listener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {

                String store = location.getLongitude() + " " + location.getLatitude();
                if (store.equals(store_aux)) {

                }else{
                    store_aux = store;
                    saveTextAsFileCoordenadas(store + "\n");
                }
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {
                Intent i = new Intent(Settings.ACTION_LOCALE_SETTINGS);
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(i);
            }
        };
        //Fim do listener do gps

        //Abre o Location Manager
        locationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        //Considerando que ja esta ativado
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return TODO;
        }else {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 3000, 0, listener);
        }
        //location= locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        //Fim do location manager



        mac = macAddress.recupAdresseMAC(wifiManager).toString();
        scanwifi();

        myServiceThread = new MyServiceThread();
        myServiceThread.start();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            startMyOwnForeground();
        else
            startForeground(1, new Notification());
        return super.onStartCommand(intent, flags, startId);
    }


    @Override
    public void onDestroy() {
        Toast.makeText(getApplicationContext(),
                "Desativando Serviço", Toast.LENGTH_LONG).show();
        myServiceThread.setRunning(false);
        super.onDestroy();
    }


    private final static ArrayList<Integer> channelsFrequency = new ArrayList<Integer>(
            Arrays.asList(0, 2412, 2417, 2422, 2427, 2432, 2437, 2442, 2447,
                    2452, 2457, 2462, 2467, 2472, 2484));

    public static Integer getFrequencyFromChannel(int channel) {
        return channelsFrequency.get(channel);
    }

    public static int getChannelFromFrequency(int frequency) {
        return channelsFrequency.indexOf(Integer.valueOf(frequency));
    }

    public class MyServiceReceiver extends BroadcastReceiver {

        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void onReceive(Context context, Intent intent) {
            cnt++;
            results = wifiManager.getScanResults();
            unregisterReceiver(this);
            Calendar c = Calendar.getInstance();
            SimpleDateFormat dateformat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
            String currentTime = dateformat.format(c.getTime());

            //Pegar dados do GPS
            LocationManager locationManager2 = (LocationManager)  getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
            @SuppressLint("MissingPermission")
            Location location_atual = locationManager2.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            String coordenadas = location_atual.getLatitude()+";"+location_atual.getLongitude();
            //String coordenadas = "";

            //Pegar dados wifi
            mobileInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
            wifiinfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                batLevel = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
            }

            String mobileConnected = mobileInfo.getState().toString();
            Boolean wifiConncected = wifiinfo.isConnected();


            for (ScanResult scanResult : results) {
                long actualTimestamp = System.currentTimeMillis() - (scanResult.timestamp / 1000);
                Date date = new Date(actualTimestamp); // *1000 is to convert seconds to milliseconds
                SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss"); // the format of your date
                String formattedDate = sdf.format(date);

                arrayList.add(cnt + ";" + coordenadas +  "; " + wifiConncected + ";" + mobileConnected + ";" + batLevel +";" + currentTime + ";" + scanResult.BSSID + ";" + currentTime + ";" + System.currentTimeMillis() + ";" + scanResult.timestamp + ";" + scanResult.SSID + ";" + getChannelFromFrequency(scanResult.frequency) + ";" + scanResult.level);
            }

            if (cnt == 1){
                String headers = "SCAN;LAT;LONG;WIFI_STATUS;3G/4G_STATUS;BATERIA;HORA;MACROUTER;HORA ATUAL;TIMESTAMP ATUAL;TIMESTAMP SCAN;NOME DA REDE;CANAL;SINAL";
                saveTextAsFile(headers + "\n");
            }

            Object[] mStringArray = arrayList.toArray();
            for (int i = x; i < mStringArray.length; i++) {
                saveTextAsFile((String) mStringArray[i] + "\n");
                y = i;
            }
            x = y;


            /*Send back to Main*/
            /*
            Intent i = new Intent();
            i.setAction(ACTION_UPDATE_ARRAY);
            i.putStringArrayListExtra(KEY_ARRAY_FROM_SERVICE, arrayList);
            sendBroadcast(i);
            */
        }
    }


    //Thread do servico wifi
    private class MyServiceThread extends Thread {

        private boolean running;

        public void setRunning(boolean running) {
            this.running = running;
        }

        @Override
        public void run() {
            running = true;
            while (running) {
                try {

                    Thread.sleep(Integer.parseInt(MainActivity.time_execute));
                    scanwifi();

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    //Scan wifi results
    private void scanwifi() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(wifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        registerReceiver(myServiceReceiver, intentFilter);

    }

    /*Salvar o arquivo*/
    private void saveTextAsFile(String content) {

        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), mac + "_OSVersion.txt");
        try {
            //FileOutputStream fos = openFileOutput("data.txt", this.MODE_APPEND);
            FileOutputStream fos = new FileOutputStream(file, true);
            fos.write(content.getBytes());
            fos.close();
            /* Toast.makeText(this, "Salvo", Toast.LENGTH_SHORT).show();*/
            Uri uri = Uri.fromFile(file);
            Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri);
            sendBroadcast(intent);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Toast.makeText(this, "Arquivo não encontrado", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Erro ao salvar", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveTextAsFileCoordenadas(String content) {

        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), mac + "_Coordenadas.txt");
        try {
            //FileOutputStream fos = openFileOutput("data.txt", this.MODE_APPEND);
            FileOutputStream fos = new FileOutputStream(file, true);
            fos.write(content.getBytes());
            fos.close();
            /* Toast.makeText(this, "Salvo", Toast.LENGTH_SHORT).show();*/
            Uri uri = Uri.fromFile(file);
            Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri);
            sendBroadcast(intent);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Toast.makeText(this, "Arquivo não encontrado", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Erro ao salvar", Toast.LENGTH_SHORT).show();
        }
    }

    /*Para android 9 manter o */
    private void startForeground() {

        Intent notificationIntent = new Intent(this, MainActivity.class);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0);

        startForeground(NOTIF_ID, new NotificationCompat.Builder(this,
                NOTIF_CHANNEL_ID) // don't forget create a notification channel first
                .setOngoing(true)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(getString(R.string.app_name))
                .setContentText("Service is running background")
                .setContentIntent(pendingIntent)
                .build());
    }
    //Para android acima de 9
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void startMyOwnForeground(){
        String NOTIFICATION_CHANNEL_ID = "com.example.simpleapp";
        String channelName = "My Background Service";
        NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_NONE);
        chan.setLightColor(Color.BLUE);
        chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        assert manager != null;
        manager.createNotificationChannel(chan);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
        Notification notification = notificationBuilder.setOngoing(true)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Service is running in background")
                .setPriority(NotificationManager.IMPORTANCE_MIN)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build();
        startForeground(2, notification);
    }
}
