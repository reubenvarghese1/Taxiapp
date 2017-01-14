package com.log.cyclone.util;

import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.apache.http.NameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JSONParser {

    /**
     * Get JSON Object from the given URL using GET or POST.
     *
     * @param url
     * @param method
     * @param params
     * @return
     */
    public void makeHttpRequest(final String url, String method, List<NameValuePair> params, final ServerCallback callback) {
        RequestQueue requestQueue = Volley.newRequestQueue(MyApplication.getAppContext());

        final Map<String, String> paramMap = new HashMap<>();

        for (NameValuePair nvp : params) {
            if (nvp.getValue() != null) {
                paramMap.put(nvp.getName(), nvp.getValue());
            } else {
                paramMap.put(nvp.getName(), "");
            }
        }

        int reqMethod = 1;

        if (method.equals("GET")) {
            reqMethod = Request.Method.GET;
        }

        StringRequest stringRequest = new StringRequest(reqMethod, url, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                JSONObject jsonObject = null;

                try {
                    jsonObject = new JSONObject(response);
                } catch (JSONException e) {
                    Log.e("JSONParser", "Error converting the response to JSON Object");
                }

                callback.onSuccess(jsonObject);
            }
        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("JSONParser", "Error retrieving JSON data from URL : " + url);
            }
        }) {

            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                return paramMap;
            }
        };

        // Adding the Volley Request to the request queue.
        requestQueue.add(stringRequest);
    }
}
