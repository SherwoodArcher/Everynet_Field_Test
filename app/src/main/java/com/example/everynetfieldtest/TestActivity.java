package com.example.everynetfieldtest;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestActivity extends AppCompatActivity {
    public String loginEmail, loginRegion, accessToken, deviceEui, band;
    public Button startTestB, logoutB;
    public TextInputLayout deviceEuiL;
    public EditText deviceEuiE;
    public TextView testT, bandT, dataratesT, actualDatarateT;
    HashMap<String, String> headers;
    public List<Integer> datarate;
    public int actualDatarateIndex;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
        API.getInstance(this);

        Intent intent = getIntent();
        loginEmail = intent.getStringExtra("loginEmail");
        loginRegion = intent.getStringExtra("loginRegion");
        accessToken = intent.getStringExtra("accessToken");

        deviceEuiE = findViewById(R.id.deviceEui);
        deviceEuiL = findViewById(R.id.deviceEuiL);
        deviceEuiL.setEndIconOnClickListener(this::searchDeviceClick);
        testT = findViewById(R.id.testMessage);
        bandT = findViewById(R.id.band);
        dataratesT = findViewById(R.id.datarates);
        actualDatarateT = findViewById(R.id.actualDatarate);

        startTestB = (Button) findViewById(R.id.startTest);
        logoutB = (Button) findViewById(R.id.logoutButton);
        logoutB.setOnClickListener(this::logout);

        headers = new HashMap<>();
        headers.put("Content-Type", "application/json; charset=utf-8");
        headers.put("Cookie", "session_token=" + accessToken);
    }

    public void searchDeviceClick(View view){
        deviceEui = deviceEuiE.getText().toString();
        deviceEui = "70b3d54b100039bb";
        if(deviceEui.length() != 16) {
            Toast.makeText(getApplicationContext(), R.string.deviceEuiSize, Toast.LENGTH_SHORT).show();
        }else {
            bandT.setText("");
            dataratesT.setText("");
            startTestB.setVisibility(View.INVISIBLE);
            API.getInstance().request(0, loginRegion, "devices/" + deviceEui, new HashMap<>(), this::searchDevice, headers);
        }
    }

    private void searchDevice(JSONObject response) {
        if(response == null){
            bandT.setText(getString(R.string.noDevice));
        } else {
            try {
                band = response.getJSONObject("device").optString("band","");
                bandT.setText(String.format("%s %s", getString(R.string.bandT), band));
                API.getInstance().request(0, loginRegion, "bands/" + band, new HashMap<>(), this::getBandData, headers);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private void getBandData(JSONObject response){
        if(response == null){
            Toast.makeText(getApplicationContext(), R.string.noBand, Toast.LENGTH_SHORT).show();
        } else {
            try {
               JSONArray channels = response.getJSONObject("band").getJSONArray("channels");
               datarate = new ArrayList<>();
               for(int a=0; a < channels.length(); a++){
                   JSONArray channel = channels.getJSONObject(a).getJSONArray("datarates");
                   for(int b=0; b < channel.length(); b++){
                        if(!datarate.contains(channel.getInt(b))){
                            datarate.add(b);
                        }
                   }
               }
               dataratesT.setText(String.format("%s %s", getString(R.string.dataratesT), datarate.toString()));
               startTestB.setVisibility(View.VISIBLE);
               startTestB.setOnClickListener(this::startTest);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

    }

    private void startTest(View view) {
        actualDatarateIndex = 0;
        changeDR();
    }

    private void changeDR(){
        HashMap<String, Object> param = new HashMap<>();
            param.put("block_uplink", false);
            param.put("block_downlink", false);
        HashMap<String, Object> adr = new HashMap<>();
            adr.put("datarate",datarate.get(actualDatarateIndex));
            adr.put("mode","static");
            adr.put("tx_power",6);
        param.put("adr",adr);
        API.getInstance().request(7, loginRegion, "devices/" + deviceEui, param, this::changedDR, headers);
    }

    private void changedDR(JSONObject response) {
        actualDatarateT.setText(response.toString());
        if(actualDatarateIndex < datarate.size()-1){
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


    public void logout(View view) {
        loginEmail = loginRegion = accessToken = null;
        Toast.makeText(getApplicationContext(), "Logged out", Toast.LENGTH_SHORT).show();
        Intent login = new Intent(TestActivity.this,MainActivity.class);
        startActivity(login);
    }
}