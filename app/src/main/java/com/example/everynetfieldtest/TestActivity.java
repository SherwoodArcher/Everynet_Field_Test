package com.example.everynetfieldtest;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.CountDownTimer;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class TestActivity extends AppCompatActivity {
    public String accessToken, band, csv, deviceEui, loginEmail, loginRegion;
    public Button startTestB, logoutB;
    public TextInputLayout deviceEuiL;
    public EditText deviceEuiE;
    public TextView testT, bandT, dataratesT, actualDatarateT, testMessageT;
    public HashMap<String, String> headers;
    public List<Integer> datarate;
    public int actualDatarateIndex;
    private double latitude, longitude;
    public LocationManager lm;

    private final class DataAPIListener extends WebSocketListener {
        //private static final int NORMAL_CLOSURE_STATUS = 1000;

        @Override
        public void onOpen(@NotNull WebSocket webSocket, @NotNull Response response) {
            output("WebSocket Connection is open.");
            //webSocket.close(NORMAL_CLOSURE_STATUS, "Goodbye !");
        }

        @Override
        public void onMessage(@NotNull WebSocket webSocket, @NotNull String text) {
            try {
                JSONObject json = new JSONObject(text);
                identifyType(json);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            output("Receiving : " + text);
        }

        @Override
        public void onClosing(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
            //webSocket.close(NORMAL_CLOSURE_STATUS, null);
            output("Closing : " + code + " / " + reason);
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
        headers = new HashMap<>();
        headers.put("Content-Type", "application/json; charset=utf-8");
        headers.put("Cookie", "session_token=" + accessToken);
        logoutB = findViewById(R.id.logout_button);
        startTestB = findViewById(R.id.start_test);
        testMessageT = findViewById(R.id.test_message);
    }

    private void setUpListeners(Context context) {
        deviceEuiL.setEndIconOnClickListener(this::searchDeviceClick);
        logoutB.setOnClickListener(this::logout);
    }

    private void setUpGPSCoordinates(Context context) {
        lm = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            //Setup request permissions

            longitude = 0;
            latitude = 0;
        } else {
            Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            longitude = location.getLongitude();
            latitude = location.getLatitude();
        }
    }

    public void searchDeviceClick(View view) {
        deviceEui = deviceEuiE.getText().toString();
        deviceEui = "20635f02010000e1";
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
                startTestB.setOnClickListener(this::startTest);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

    }

    private void startTest(View view) {
        csv = "Device EUI,Latitude,Longitude,Gateway MAC, Type, Counter,DR,SNR,RSSI\n\n";
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url("wss://ns." + loginRegion + ".everynet.io/api/v1.0/data?access_token=" + accessToken + "&devices=" + deviceEui).build();
        DataAPIListener listener = new DataAPIListener();
        client.newWebSocket(request, listener);
        client.dispatcher().executorService().shutdown();

        //dataAPI = new DataAPI(loginRegion,accessToken,deviceEui,actualDatarateT, this);
        //dataAPI.run();

        //actualDatarateIndex = 0;
        //changeDR();
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
        actualDatarateT.setText(response.toString());
        if (actualDatarateIndex < datarate.size() - 1) {
            new CountDownTimer(5000, 1000) {

                public void onTick(long millisUntilFinished) {

                }

                public void onFinish() {
                    actualDatarateIndex++;
                    changeDR();
                }
            }.start();

        }
    }

    public void output(String message) {
        runOnUiThread(() -> actualDatarateT.setText(message));
    }

    private void identifyType(JSONObject json) throws JSONException {
        String type = json.optString("type", "");
        switch (type) {
            case "uplink":
                uplink(json);
                break;
            case "downlink":
                //testMessageT.setText("Downlink");
                break;
            case "downlink_request":
                //testMessageT.setText("Downlink Request");
                break;
            case "status_response":
                break;
            default:
        }
    }


    private void uplink(JSONObject json) throws JSONException {
        String line = "";
        char d = ',';
        line += deviceEui + d;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        //lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 10, locationListener);
        line += latitude + d;
        //line += longitude + d;
        //line += json.getJSONObject("meta").optString("gateway", "") + d;
        //line += json.getJSONObject("params").getInt("counter_up") + d;
        //line += json.getJSONObject("params").getJSONObject("radio").getInt("datarate") + d;
        //line += json.getJSONObject("params").getJSONObject("radio").getJSONObject("hardware").getDouble("snr") + d;
        //line += json.getJSONObject("params").getJSONObject("radio").getJSONObject("hardware").getDouble("rssi") + d;
        line += "\n\n";
        csv += line;
        testMessageT.setText(csv);
    }

    private final LocationListener locationListener = new LocationListener() {
        public void onLocationChanged(Location location) {
            longitude = location.getLongitude();
            latitude = location.getLatitude();
        }
    };

    public void logout(View view) {
        loginEmail = loginRegion = accessToken = null;
        Toast.makeText(getApplicationContext(), "Logged out", Toast.LENGTH_SHORT).show();
        Intent login = new Intent(TestActivity.this,MainActivity.class);
        startActivity(login);
    }
}