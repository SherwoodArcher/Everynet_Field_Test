package com.example.everynetfieldtest;

import android.content.Context;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

import java.util.HashMap;

public class API {
    private static API instance = null;
    private static final String prefixURL = "https://ns.";
    private static final String middlefixURL = ".everynet.io/api/v1.0/";

    public RequestQueue requestQueue;

    private API(Context context)
    {
        requestQueue = Volley.newRequestQueue(context.getApplicationContext());
    }

    public static synchronized API getInstance(Context context)
    {
        if (null == instance)
            instance = new API(context);
        return instance;
    }

    public static synchronized API getInstance()
    {
        if (null == instance)
        {
            throw new IllegalStateException(API.class.getSimpleName() +
                    " is not initialized, call getInstance(...) first");
        }
        return instance;
    }

    public Runnable request(int method, String region, String postfixURL, HashMap param, final Listener<JSONObject> listener, HashMap headers){
        String url = prefixURL + region + middlefixURL + postfixURL;
        //method Get = 0 Post = 1 Patch = 7
        JsonObjectRequest request = new JsonObjectRequest(method, url, new JSONObject(param),
                response -> { if(null != response) listener.getResult(response); },
                error -> { if (null != error.networkResponse) { listener.getResult(null);}
                }){
                    @Override
                    public HashMap<String, String> getHeaders() {
                        return headers;
                    }
                };
        requestQueue.add(request);
        return null;
    }
}

