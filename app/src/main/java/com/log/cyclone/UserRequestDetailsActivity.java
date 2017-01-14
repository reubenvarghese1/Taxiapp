package com.log.cyclone;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.log.cyclone.General.Globals;
import com.log.cyclone.util.JSONParser;
import com.log.cyclone.util.ServerCallback;
import com.log.cyclone.util.Util;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

import static android.view.View.GONE;

/**
 * @author Logan
 */
public class UserRequestDetailsActivity extends Activity implements OnClickListener {

    TextView nameTv, numberTv, pickupTv, dropTv,rate;
    Button trackBtn,acp,ccl;
    public static HashMap<String, String> request;
    Context con;
    JSONParser jparser = new JSONParser();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.user_request_details_layout);
        con = UserRequestDetailsActivity.this;
        init();
        nameTv.setText("Driver name : " + request.get("driver_name").trim());
        numberTv.setText("Number : " + request.get("phone").trim());
        pickupTv.setText("Pickup Location : " + request.get("location").trim());
        dropTv.setText("Drop Location : " + request.get("droplocation").trim());
        rate.setText("Cost of Ride : " + request.get("rate").trim());
        getActionBar().setHomeButtonEnabled(true);
        getActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private void init() {
        nameTv = (TextView) findViewById(R.id.userRequestDetailsName);
        numberTv = (TextView) findViewById(R.id.userRequestDetailsNumber);
        pickupTv = (TextView) findViewById(R.id.userRequestDetailsPickupLocation);
        rate=(TextView)findViewById(R.id.rat);
        dropTv = (TextView) findViewById(R.id.userRequestDetailsDropLocation);
        acp=(Button)findViewById(R.id.accept1);
        ccl=(Button)findViewById(R.id.cancel1);
        trackBtn = (Button) findViewById(R.id.driverTrackBtn);
        acp.setOnClickListener(this);
        ccl.setOnClickListener(this);
        trackBtn.setOnClickListener(this);

        if (request.get("accept").equals("1")) {
            //accepted rides
            acp.setVisibility(GONE);
            ccl.setVisibility(GONE);
            trackBtn.setOnClickListener(this);
        }
        else if (request.get("accept").equals("0")){
            //not accepted yet rides
            acp.setVisibility(GONE);
            trackBtn.setVisibility(GONE);
        }
        else if (request.get("accept").equals("2")){
            //completed rides
            acp.setVisibility(GONE);
            ccl.setVisibility(GONE);
            trackBtn.setVisibility(GONE);
        }
        else if (request.get("accept").equals("3")){
              //cancelled rides
            acp.setVisibility(GONE);
            ccl.setVisibility(GONE);
            trackBtn.setVisibility(GONE);
        }
        else {
            trackBtn.setVisibility(GONE);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.driverTrackBtn:
                if (request.get("accept").equals("1")) {

                    Intent i = new Intent(this, DriverTrackActivity.class);
                    i.putExtra("driver_email", request.get("driver_email"));
                    startActivity(i);
                } else {
                    Util.showToast(con, "Only accepted requests are allowed to track");
                }
                break;
            case R.id.accept1:
                registerUser1();
                new SendData1().execute("1");

                break;
            case R.id.cancel1:
                new SendData1().execute("3");


                break;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }
    private void registerUser1(){

       final String msg="Dear customer we are pleased to inform you that your taxi has been booked with "+request.get("driver_name")+" for "+request.get("booking_time")+"costing" +request.get("rate");
        StringRequest stringRequest = new StringRequest(Request.Method.POST, Globals.smsURL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Toast.makeText(con,response,Toast.LENGTH_LONG).show();
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(con,error.toString(),Toast.LENGTH_LONG).show();
                    }
                }){
            @Override
            protected Map<String,String> getParams(){
                Map<String,String> params = new HashMap<String, String>();
                params.put("msg", msg);
                params.put("number",request.get("phone"));
                return params;
            }

        };

        RequestQueue requestQueue = Volley.newRequestQueue(this);
        requestQueue.add(stringRequest);
    }



    class SendData1 {
        ProgressDialog pDialog;
        String s = "";
        int success = -1;
        int error = 0;

        String accept = "0";
        boolean driver = false;

        public SendData1() {
            pDialog = new ProgressDialog(con);
            pDialog.setMessage("Sending data. Please wait...");
            pDialog.setIndeterminate(false);
            pDialog.setCancelable(true);
            pDialog.show();
        }

        public void execute(String... st) {
            Calendar cal = Calendar.getInstance();
            String datetime = cal.get(Calendar.YEAR) + "-" + (cal.get(Calendar.MONTH) + 1) + "-" + cal.get(Calendar.DAY_OF_MONTH) + " " +
                    cal.get(Calendar.HOUR_OF_DAY) + ":" + cal.get(Calendar.MINUTE) + ":" + cal.get(Calendar.SECOND);

            accept = st[0];

            List<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair("id", request.get("id")));

            params.add(new BasicNameValuePair("accept", st[0]));
            params.add(new BasicNameValuePair("rate", request.get("rate")));
            params.add(new BasicNameValuePair("driver_id", request.get("driver_id")));
            params.add(new BasicNameValuePair("driver_email",request.get("driver_email")));
            params.add(new BasicNameValuePair("driver_name", request.get("driver_name")));
            params.add(new BasicNameValuePair("driver_name", request.get("driver_name")));
            params.add(new BasicNameValuePair("group_id",request.get("group_id") ));
            params.add(new BasicNameValuePair("accepted_datetime", datetime));

            jparser.makeHttpRequest(Globals.dataSendUrl1, "POST", params, new ServerCallback() {
                @Override
                public void onSuccess(JSONObject jsonObject) {
                    try {
                        success = jsonObject.getInt("success");
                        s = jsonObject.getString("message");

                    } catch (Exception e) {
                        error = 1;
                    }
                    pDialog.dismiss();

                    if (error == 1) {
                        if (Util.isConnectingToInternet(con)) {
                            Toast.makeText(con, "Server is down, Please try again later", Toast.LENGTH_SHORT).show();
                        } else
                            Util.showNoInternetDialog(con);
                        return;
                    }

                    Toast.makeText(con, s, Toast.LENGTH_LONG).show();
                    if (success == 0) {

                    } else if (success == 1) {
                        setResult(RESULT_OK);




                        Intent d = new Intent(con, DriverPositionActivity.class);
                        startActivity(d);

                    }
                }
            });
        }
    }
}

