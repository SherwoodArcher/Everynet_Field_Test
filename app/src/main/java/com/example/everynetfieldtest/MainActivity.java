package com.example.everynetfieldtest;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    public String loginEmail, loginPassword, loginRegion, accessToken;
    public EditText loginEmailT, loginPasswordT;
    public Spinner loginRegionS;
    public boolean permission;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        loginRegionS = (Spinner) findViewById(R.id.loginRegion);
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.regions, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        loginRegionS.setAdapter(adapter);

    }

    public void login(View view) {

        loginEmailT= (EditText) findViewById(R.id.loginEmail);
        loginPasswordT= (EditText) findViewById(R.id.loginPassword);
        loginEmail = loginEmailT.getText().toString();
        loginPassword = loginPasswordT.getText().toString();
        loginRegion = loginRegionS.getSelectedItem().toString();

        final TextView textView = (TextView) findViewById(R.id.text);
        textView.setText("Email: "+ loginEmail + " Password: " +loginPassword+" Region: "+loginRegion);

        //Opening Request
        RequestQueue queue = Volley.newRequestQueue(this);

        //Creating JSON
        Map<String, String> postParam = new HashMap<String, String>();
        postParam.put("email", loginEmail);
        postParam.put("password", loginPassword);

        //Sending Request
        JsonObjectRequest jsonObjReq = new JsonObjectRequest(Request.Method.POST,
                "https://ns." + loginRegion + ".everynet.io/api/v1.0/auth", new JSONObject(postParam),
            new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    JSONArray permissions;
                    try {
                        permissions = response.getJSONObject("permissions").getJSONObject("devices").getJSONArray("*");
                        Toast.makeText(getApplicationContext(), permissions.toString(), Toast.LENGTH_LONG).show();
                        accessToken = response.optString("access_token","");
                        permission = permissions.toString().contains("read") && permissions.toString().contains("update");
                        if(permission) {
                            Toast.makeText(getApplicationContext(), "Logged In", Toast.LENGTH_LONG).show();
                            textView.setText("SIM");
                        }else {
                            textView.setText("You don't have the necessary permissions use this application");
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            },
            new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Toast.makeText(getApplicationContext(), "Login Failed", Toast.LENGTH_LONG).show();
                }
            }
        ){
            /**
             * Passing some request headers
             */
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<String, String>();
                headers.put("Content-Type", "application/json; charset=utf-8");
                return headers;
            }
        };
        //jsonObjReq.setTag(TAG);
        // Adding request to request queue
        queue.add(jsonObjReq);
    }

}