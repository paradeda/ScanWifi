package com.example.diegoparadeda.scanwifiservice;

import android.Manifest;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    private WifiManager wifiManager;
    private BroadcastReceiver broadcastReceiver;

    /*
    private ListView listView;
    private ArrayList<String> arrayList2 = new ArrayList<>();
    private ArrayAdapter adapter;
    */
    Button btnStart, btnStop, btnSend;
    String coordenada_aux = "";
    private EditText thread_time;

    public static String mac;
    int TAG_CODE_PERMISSION_LOCATION;

    /*
    MyMainReceiver myMainReceiver;
    */
    Intent myIntent = null;
    TextView textstatus, text_latitude, text_longitude;
    public static final int REQUEST_ID_MULTIPLE_PERMISSIONS = 1;
    public static String time_execute = "1000";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        checkAndRequestPermissions();


        setContentView(R.layout.activity_main);
        btnStart = (Button)findViewById(R.id.startservice);
        btnStop = (Button)findViewById(R.id.stopservice);
        /*
        listView = findViewById(R.id.wifiList);
        */
        textstatus = (TextView)findViewById(R.id.status);
        //text_latitude = (TextView)findViewById(R.id.latitude);
        //text_longitude =(TextView)findViewById(R.id.longitude);
        thread_time = (EditText)findViewById(R.id.timethread);
        thread_time.setText("1000");

        /*Check service status*/
        if (!isMyServiceRunning(MyService.class)){
            textstatus.setText("Serviço: Offline");
            textstatus.setTextColor(Color.RED);
            thread_time.setEnabled(true);
        }else{
            textstatus.setText("Serviço: Online");
            textstatus.setTextColor(Color.GREEN);
            thread_time.setEnabled(false);
        }

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (!wifiManager.isWifiEnabled()) {
            Toast.makeText(this, "WiFi is disabled ... We need to enable it", Toast.LENGTH_LONG).show();
            wifiManager.setWifiEnabled(true);
        }
        mac = macAddress.recupAdresseMAC(wifiManager).toString();


        /*Criar o arquivo se o mesmo não existir*/
        /*Criar arquivo*/
        double release = Double.parseDouble(Build.VERSION.RELEASE.replaceAll("(\\d+[.]\\d+)(.*)", "$1"));
        if (!arquivo_existe()) {

            String codeName = "Unsupported";//below Jelly bean OR above Oreo
            saveTextAsFile("Endereço MAC: " + mac + "\n");
            if (release >= 4.1 && release < 4.4)
                saveTextAsFile("OS: Jelly Bean - " + Build.VERSION.RELEASE + "\n");
            else if (release < 5)
                saveTextAsFile("OS: Kit Kat - " + Build.VERSION.RELEASE + "\n");
            else if (release < 6)
                saveTextAsFile("OS: Lollipop - " + Build.VERSION.RELEASE + "\n");
            else if (release < 7)
                saveTextAsFile("OS: Marshmallow - " + Build.VERSION.RELEASE + "\n");
            else if (release < 8)
                saveTextAsFile("OS: Nougat - " + Build.VERSION.RELEASE + "\n");
            else if (release < 9) saveTextAsFile("OS: Oreo - " + Build.VERSION.RELEASE + "\n");
            else if (release == 9) saveTextAsFile("OS: Pie - " + Build.VERSION.RELEASE + "\n");
        }



        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                time_execute = thread_time.getText().toString();
                thread_time.setEnabled(false);
                startService();
                statusservice();

            }
        });

        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopService();
                statusservice();
                thread_time.setEnabled(true);
            }
        });
    }

    private void startService(){
        myIntent = new Intent(MainActivity.this, MyService.class);
        startService(myIntent);
    }

    private void stopService(){

        myIntent = new Intent(MainActivity.this, MyService.class);
        stopService(myIntent);

    }

    @Override
    protected void onStart() {
        /*
        myMainReceiver = new MyMainReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(MyService.ACTION_UPDATE_CNT);
        intentFilter.addAction(MyService.ACTION_UPDATE_ARRAY);
        intentFilter.addAction(MyService.SERVICE_STATUS);
        registerReceiver(myMainReceiver, intentFilter);
        */
        super.onStart();
    }

    @Override
    protected void onStop() {
        /*
        unregisterReceiver(myMainReceiver);
        */
        super.onStop();
    }




    @Override
    protected void onPostResume() {
        super.onPostResume();

        statusservice();

    }
    /*
    private class MyMainReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(MyService.ACTION_UPDATE_ARRAY)) {
                arrayList2 = intent.getStringArrayListExtra(MyService.KEY_ARRAY_FROM_SERVICE);
                adapter = new ArrayAdapter<String>(context, android.R.layout.simple_list_item_1, arrayList2);
                listView.setAdapter(adapter);
                adapter.notifyDataSetChanged();
                listView.setSelection(adapter.getCount() - 1);

            }
        }
    }
    */

    /*Request Permissions*/
    private  boolean checkAndRequestPermissions() {
        int permitionwrite = ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int permitionread = ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE);
        int permissionSendMessage = ContextCompat.checkSelfPermission(this,
                Manifest.permission.SEND_SMS);
        int permitiongps = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION);
        List<String> listPermissionsNeeded = new ArrayList<>();
        if (permitionwrite != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if (permitionread != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }
        if (permitiongps != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }
        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]),REQUEST_ID_MULTIPLE_PERMISSIONS);
            return false;
        }

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) ==
                        PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        } else {
            ActivityCompat.requestPermissions(this, new String[] {
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION },
                    TAG_CODE_PERMISSION_LOCATION);
        }
        return true;
    }

    /*Checa se o arquivo existe*/
    public boolean arquivo_existe() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1000);
        }else{
            File download_folder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File arquivo = new File(download_folder, mac + "_OSVersion.txt");
            Uri uri = Uri.fromFile(arquivo);
            Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri);
            sendBroadcast(intent);

            if (!arquivo.exists()) {
                return false;
            }
        }
        return true;

    }

    //Dados de GPS
    /*
    public void configurarServico(){
        try {
            LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

            LocationListener locationListener = new LocationListener() {
                public void onLocationChanged(Location location) {
                    atualizar(location);
                }

                public void onStatusChanged(String provider, int status, Bundle extras) { }

                public void onProviderEnabled(String provider) { }

                public void onProviderDisabled(String provider) { }
            };
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        }catch(SecurityException ex){
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            //Toast.makeText(this, "teste" + ex.getMessage(), Toast.LENGTH_LONG).show();
        }
    }


    public void pedirPermissoes() {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }
        else
            configurarServico();
    }

    public void atualizar(Location location)
    {
        Double latPoint = location.getLatitude();
        Double lngPoint = location.getLongitude();

        //Toast.makeText(this, latPoint.toString(), Toast.LENGTH_LONG).show();
        //Toast.makeText(this, lngPoint.toString(), Toast.LENGTH_LONG).show();
        text_latitude.setText(latPoint.toString());
        text_longitude.setText(lngPoint.toString());
    }
    */
    /*Salvar o arquivo*/
    private void saveTextAsFile(String content) {

        //File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), "data.txt");
        //File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), mac_address() + "_data.txt");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1000);
        } else {
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
    }

    private void statusservice(){
        if (!isMyServiceRunning(MyService.class)){
            textstatus.setText("Serviço: Offline");
            textstatus.setTextColor(Color.RED);
        }else{
            textstatus.setText("Serviço: Online");
            textstatus.setTextColor(Color.GREEN);
        }
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

}