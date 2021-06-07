package com.example.everynetfieldtest;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputLayout;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

import static com.example.everynetfieldtest.R.string.logged_out;

public class TestActivity extends AppCompatActivity {
    public String accessToken, band, csv, deviceEui, fullJson, loginEmail, loginRegion;
    public Button startTestB, exportCsvB, exportJsonB, logoutB;
    public TextInputLayout deviceEuiL;
    public EditText deviceEuiE;
    public TextView testT, bandT, dataratesT, actualDatarateT, gpsCoordinatesT, wssMessageT;
    public HashMap<String, String> headers;
    public List<Integer> datarate;
    public int actualDatarateIndex;
    private GpsTracker gpsTracker;
    private double latitude, longitude;

    private final class DataAPIListener extends WebSocketListener {
        private final int NORMAL_CLOSURE_STATUS = 1000;

        @Override
        public void onOpen(@NotNull WebSocket webSocket, @NotNull Response response) {
            output("WebSocket Connection is open.");
        }

        @Override
        public void onMessage(@NotNull WebSocket webSocket, @NotNull String text) {
            try {
                JSONObject json = new JSONObject(text);
                identifyType(json);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onClosing(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
            output("WebSocket Connection is closed");
        }

        @Override
        public void onFailure(@NotNull WebSocket webSocket, Throwable t, Response response) {
            output("Error : " + t.getMessage());
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
        ManagementAPI.getInstance(this);

        setUpLoginData();
        setUpPublicVariables();
        setUpListeners(this);
        setUpGPSCoordinates(this);
    }

    private void setUpLoginData() {
        Intent intent = getIntent();
        loginEmail = intent.getStringExtra("loginEmail");
        loginRegion = intent.getStringExtra("loginRegion");
        accessToken = intent.getStringExtra("accessToken");
    }

    private void setUpPublicVariables() {
        actualDatarateT = findViewById(R.id.actual_data_rate);
        bandT = findViewById(R.id.band);
        dataratesT = findViewById(R.id.data_rates);
        deviceEuiE = findViewById(R.id.device_eui);
        deviceEuiL = findViewById(R.id.device_eui_l);
        gpsCoordinatesT = findViewById(R.id.gps_coordinates);
        headers = new HashMap<>();
        headers.put("Content-Type", "application/json; charset=utf-8");
        headers.put("Cookie", "session_token=" + accessToken);
        logoutB = findViewById(R.id.logout_button);
        startTestB = findViewById(R.id.start_test);
        exportCsvB = findViewById(R.id.export_csv);
        exportJsonB = findViewById(R.id.export_json);
        wssMessageT = findViewById(R.id.wss_message);
    }

    private void setUpListeners(Context context) {
        deviceEuiL.setEndIconOnClickListener(v -> searchDeviceClick());
        logoutB.setOnClickListener(v -> logout());
        startTestB.setOnClickListener(v -> startTest());
        exportCsvB.setOnClickListener(v -> export(true));
        exportJsonB.setOnClickListener(v -> export(false));
        wssMessageT.setMovementMethod(new ScrollingMovementMethod());
    }

    private void setUpGPSCoordinates(Context context) {
        try {
            if (ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED )
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 101);
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public void output(String message) {
        runOnUiThread(() -> wssMessageT.setText(message));
    }

    public void searchDeviceClick() {
        deviceEui = deviceEuiE.getText().toString();
        if (deviceEui.length() != 16) {
            Toast.makeText(getApplicationContext(), R.string.deviceEuiSize, Toast.LENGTH_SHORT).show();
        } else {
            bandT.setText("");
            dataratesT.setText("");
            startTestB.setVisibility(View.INVISIBLE);
            ManagementAPI.getInstance().request(0, loginRegion, "devices/" + deviceEui, new HashMap<>(), this::searchDevice, headers);
        }
    }

    private void searchDevice(JSONObject response) {
        if (response == null) {
            bandT.setText(getString(R.string.no_device));
        } else {
            try {
                band = response.getJSONObject("device").optString("band", "");
                bandT.setText(String.format("%s %s", getString(R.string.band_t), band));
                ManagementAPI.getInstance().request(0, loginRegion, "bands/" + band, new HashMap<>(), this::getBandData, headers);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private void getBandData(JSONObject response) {
        if (response == null) {
            Toast.makeText(getApplicationContext(), R.string.no_band, Toast.LENGTH_SHORT).show();
        } else {
            try {
                JSONArray channels = response.getJSONObject("band").getJSONArray("channels");
                datarate = new ArrayList<>();
                for (int a = 0; a < channels.length(); a++) {
                    JSONArray channel = channels.getJSONObject(a).getJSONArray("datarates");
                    for (int b = 0; b < channel.length(); b++) {
                        if (!datarate.contains(channel.getInt(b))) {
                            datarate.add(b);
                        }
                    }
                }
                dataratesT.setText(String.format("%s %s", getString(R.string.data_rates_t), datarate.toString()));
                startTestB.setVisibility(View.VISIBLE);
                exportCsvB.setVisibility(View.VISIBLE);
                exportJsonB.setVisibility(View.VISIBLE);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

    }

    private void startTest() {
        csv = "Date,Time,Device EUI,Latitude,Longitude,Gateway MAC, Type, Counter,DR,SNR,RSSI\n";
        fullJson = "";
        getLocation();
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url("wss://ns." + loginRegion + ".everynet.io/api/v1.0/data?access_token=" + accessToken + "&devices=" + deviceEui+"&radio=1").build();
        DataAPIListener listener = new DataAPIListener();
        client.newWebSocket(request, listener);
        client.dispatcher().executorService().shutdown();
        actualDatarateIndex = 0;
        changeDR();
    }

    public void getLocation(){
        GpsTracker gpsTracker = new GpsTracker(this);
        if(gpsTracker.canGetLocation()){
            latitude = gpsTracker.getLatitude();
            longitude = gpsTracker.getLongitude();
            gpsCoordinatesT.setText(String.format("%s %s %s %s",getString(R.string.lat),latitude,getString(R.string.longi), longitude));
        }else{
            gpsTracker.showSettingsAlert();
        }
    }

    private void changeDR() {
        HashMap<String, Object> param = new HashMap<>();
        param.put("block_uplink", false);
        param.put("block_downlink", false);
        HashMap<String, Object> adr = new HashMap<>();
        adr.put("datarate", datarate.get(actualDatarateIndex));
        adr.put("mode", "static");
        adr.put("tx_power", 6);
        param.put("adr", adr);
        ManagementAPI.getInstance().request(7, loginRegion, "devices/" + deviceEui, param, this::changedDR, headers);
    }

    private void changedDR(JSONObject response) {
        String dr = "";
        try{
            dr = response.getJSONObject("device").getJSONObject("adr").getString("datarate");
        }catch (JSONException e){
            e.printStackTrace();
        }
        actualDatarateT.setText(String.format("%s %s", getString(R.string.dr),dr));

        if (actualDatarateIndex < datarate.size() - 1) {
            new CountDownTimer(60000, 1000) {

                public void onTick(long millisUntilFinished) {

                }

                public void onFinish() {
                    actualDatarateIndex++;
                    changeDR();
                }
            }.start();

        }
    }

    private void identifyType(JSONObject json) throws JSONException {
        fullJson += json.toString() + "\n";
        String type = json.optString("type", "");
        if(type.contentEquals("uplink")){
            uplink(json);
        }
    }

    private void uplink(JSONObject json) throws JSONException {
        char d = ',';
        getLocation();
        StringBuilder line = new StringBuilder();
        SimpleDateFormat date = new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault());
        SimpleDateFormat time = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        time.setTimeZone(TimeZone.getDefault());
        line.append(date.format(new Date()));
        line.append(d);
        line.append(time.format(new Date()));
        line.append(d);
        line.append(deviceEui);
        line.append(d);
        line.append(latitude);
        line.append(d);
        line.append(longitude);
        line.append(d);
        String gateway = json.getJSONObject("meta").optString("gateway", "");
        line.append(gateway);
        line.append(d);
        line.append("Uplink");
        line.append(d);
        int counter = json.getJSONObject("params").getInt("counter_up");
        line.append(counter);
        line.append(d);
        int dr = json.getJSONObject("params").getJSONObject("radio").getInt("datarate");
        line.append(dr);
        line.append(d);
        double snr = json.getJSONObject("params").getJSONObject("radio").getJSONObject("hardware").getDouble("snr");
        line.append(snr);
        line.append(d);
        double rssi = json.getJSONObject("params").getJSONObject("radio").getJSONObject("hardware").getDouble("rssi");
        line.append(rssi);
        line.append("\n");
        csv += line;
        output(json.toString(4));
    }

    private void export(boolean type){
        String filetype = type ? "csv" : "json";
        String text = type? csv : fullJson;

        try {
            StringBuilder s = new StringBuilder();
            s.append("Everynet_Field_Test_Log_");
            s.append(deviceEui);
            s.append("_");
            SimpleDateFormat dateTime = new SimpleDateFormat("yyyy_MM_dd_HH_mm", Locale.getDefault());
            dateTime.setTimeZone(TimeZone.getDefault());
            s.append(dateTime.format(new Date()));
            s.append(".");
            s.append(filetype);
            String filename = String.valueOf(s);

            FileOutputStream out = openFileOutput(filename, Context.MODE_PRIVATE);
            out.write(text.getBytes());
            out.close();

            Context context = getApplicationContext();
            File filelocation = new File(getFilesDir(),filename);
            Uri path = FileProvider.getUriForFile(context,"com.example.everynetfieldtest.fileprovider", filelocation);
            Intent fileintent = new Intent(Intent.ACTION_SEND);
            fileintent.setType("text/"+filetype);
            fileintent.putExtra(Intent.EXTRA_SUBJECT,filename);
            fileintent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            fileintent.putExtra(Intent.EXTRA_STREAM,path);
            startActivity(Intent.createChooser(fileintent,"Export File"));
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void logout() {
        loginEmail = loginRegion = accessToken = null;
        gpsTracker.stopUsingGPS();
        Toast.makeText(getApplicationContext(), logged_out, Toast.LENGTH_SHORT).show();
        Intent login = new Intent(TestActivity.this,MainActivity.class);
        startActivity(login);
    }

}