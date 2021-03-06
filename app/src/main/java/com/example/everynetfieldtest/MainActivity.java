package com.example.everynetfieldtest;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    public String loginEmail, loginPassword, loginRegion, accessToken;
    public EditText loginEmailE, loginPasswordE;
    public TextView loginMessageT;
    public Spinner loginRegionS;
    public Button loginB;
    public HashMap<String, String> headers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ManagementAPI.getInstance(this);
        Objects.requireNonNull(this.getSupportActionBar()).hide();
        setContentView(R.layout.activity_main);

        setUpPublicVariables();
        setUpListeners(this);
    }

    private void setUpPublicVariables() {
        loginRegionS = findViewById(R.id.login_region);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.regions, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        loginRegionS.setAdapter(adapter);
        loginMessageT = findViewById(R.id.login_message);
        loginB = findViewById(R.id.login_button);
        headers = new HashMap<>();
        headers.put("Content-Type", "application/json; charset=utf-8");
    }

    private void setUpListeners(Context context){
        loginB.setOnClickListener(this::loginClick);
    }

    public void loginClick(View view) {
        loginEmailE = findViewById(R.id.login_email);
        loginPasswordE = findViewById(R.id.login_password);
        loginEmail = loginEmailE.getText().toString();
        loginPassword = loginPasswordE.getText().toString();
        loginRegion = loginRegionS.getSelectedItem().toString().toLowerCase();

        HashMap<String, String> postParam = new HashMap<>();
        postParam.put("email", loginEmail);
        postParam.put("password", loginPassword);
        ManagementAPI.getInstance().request(1,loginRegion,"auth", postParam, this::login, headers);
    }

    private void login(JSONObject response){
        if(response == null){
            Toast.makeText(getApplicationContext(), "Login Failed", Toast.LENGTH_SHORT).show();
        } else {
            JSONArray permissions;
            try {
                permissions = response.getJSONObject("permissions").getJSONObject("devices").getJSONArray("*");
                boolean permission = permissions.toString().contains("read") && permissions.toString().contains("update");
                accessToken = response.optString("access_token", "");
                if (permission && !accessToken.equals("")) {
                    Intent test = new Intent(MainActivity.this, TestActivity.class);
                    Toast.makeText(getApplicationContext(), "Logged In", Toast.LENGTH_SHORT).show();
                    test.putExtra("loginEmail", loginEmail);
                    test.putExtra("loginRegion", loginRegion);
                    test.putExtra("accessToken", accessToken);
                    startActivity(test);
                } else {
                    loginMessageT.setText(getString(R.string.error_permissions));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
}