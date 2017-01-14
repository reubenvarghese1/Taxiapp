package com.log.cyclone;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
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
import java.util.Map;

public class RequestedUserActivity extends Activity implements OnClickListener {
    final Context context = this;
    TextView nameTv, numberTv, pickupTv, dropLocTv,rate,admin,driver;
    Button cancelBtn, accBtn, completeBtn, rejectBtn;
    Context con;
    JSONParser jparser = new JSONParser();
    String driver_id, driver_email, driver_name;
    int group_id;

    public static HashMap<String, String> request;
    SharedPreferences sh;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.requesteduser_details_layout);
        con = RequestedUserActivity.this;
        init();
        sh = getSharedPreferences("CYCLONE_PREF", MODE_PRIVATE);
        driver_id = sh.getString("id", "");
        driver_email = sh.getString("loginemail", "");
        driver_name = sh.getString("name", "");
        group_id = sh.getInt("group_id", 0);

        nameTv.setText("Name : " + request.get("name"));
        numberTv.setText("Phone no. : " + request.get("phone"));
        pickupTv.setText("Pickup Location : " + request.get("location").trim());
        dropLocTv.setText("Drop Location : " + request.get("droplocation").trim());
        rate.setText("Rate : " + request.get("rate").trim());
        getActionBar().setHomeButtonEnabled(true);
        getActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.driver_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;

		/*case R.id.driverRefreshMenu:
            //new GetRequestedRides().execute();
			break;*/
            case R.id.driverLogoutMenu:
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                        this);
                alertDialogBuilder
                        .setMessage("Do you really wanna logout?");
                alertDialogBuilder.setPositiveButton("yes",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface arg0, int arg1) {

                                SharedPreferences.Editor edit = sh.edit();
                                edit.putString("loginemail", null);
                                edit.putString("loginpass", null);
                                edit.putBoolean("type", false);
                                edit.commit();

                                startActivity(new Intent(con, LoginActivity.class));

                                finish();
                            }
                        });

                alertDialogBuilder.setNegativeButton("No",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                                int which) {

                                dialog.cancel();
                            }
                        });
                AlertDialog alertDialog = alertDialogBuilder.create();

                alertDialog.show();

                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void init() {
        nameTv = (TextView) findViewById(R.id.requestDetailsName);
        numberTv = (TextView) findViewById(R.id.requestDetailsPhone);
        pickupTv = (TextView) findViewById(R.id.requestDetailsPickup);
        dropLocTv = (TextView) findViewById(R.id.requestDetailsDrop);
        rate=(TextView)findViewById(R.id.rate123);
        admin=(TextView)findViewById(R.id.admin);
        cancelBtn = (Button) findViewById(R.id.requestDetailsCancelBtn);
        accBtn = (Button) findViewById(R.id.requestDetailsAccBtn);
        completeBtn = (Button) findViewById(R.id.requestDetailsCompleteBtn);
        rejectBtn = (Button) findViewById(R.id.requestDetailsRejectBtn);
        driver=(TextView)findViewById(R.id.driver);
        accBtn.setOnClickListener(this);
        cancelBtn.setOnClickListener(this);
        completeBtn.setOnClickListener(this);
        rejectBtn.setOnClickListener(this);
        rate.setVisibility(View.GONE);
        admin.setVisibility(View.GONE);
        driver.setVisibility(View.GONE);
      if(request.get("position").equals("0")||request.get("position").equals("2")){
            rate.setVisibility(View.VISIBLE);
          if (request.get("position").equals("0")){
          admin.setVisibility(View.VISIBLE);
          }
          if (request.get("position").equals("2")){
              driver.setVisibility(View.VISIBLE);
          }
        }

        if (request.get("accept").equals("0")) {
            completeBtn.setVisibility(View.GONE);


        }
        if (request.get("accept").equals("1")) {
            accBtn.setVisibility(View.GONE);

        }
        if (request.get("accept").equals("2")) {
            accBtn.setVisibility(View.GONE);
            cancelBtn.setVisibility(View.GONE);
            completeBtn.setVisibility(View.GONE);
            rejectBtn.setVisibility(View.GONE);
        }
        if (request.get("accept").equals("3")) {
            accBtn.setVisibility(View.GONE);
            cancelBtn.setVisibility(View.GONE);
            completeBtn.setVisibility(View.GONE);
            rejectBtn.setVisibility(View.GONE);
        }

    }

	/*@Override
    public boolean onOptionsItemSelected(MenuItem item) {
	        switch (item.getItemId()) {
	        case android.R.id.home: 
	           finish();
	            return true;
	        }

	    return super.onOptionsItemSelected(item);
	}*/

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.requestDetailsCancelBtn:
                new SendData().execute("3");
                Intent i4 = new Intent(con, DriverMapActivity.class);
                startActivity(i4);
                break;
            case R.id.requestDetailsAccBtn:
                if(request.get("position").equals("0")) {
                    //put condition herer!!!!!!!!!!!!!

                    SharedPreferences.Editor edit = sh.edit();
                    edit.putString("rate", request.get("rate").trim());
                    edit.commit();
                    //create new cost request and then send it to the server
                   new SendData().execute("1");//added 14 as a pending request

                    /*Intent i5 = new Intent(con, DriverMapActivity.class);
                    startActivity(i5);*/
                }
                else if(request.get("position").equals("2")){
                SharedPreferences.Editor edit = sh.edit();
                edit.putString("rate", request.get("rate").trim());
                edit.commit();
                //create new cost request and then send it to the server
                new SendData().execute("1");//added 14 as a pending request

            }
                else{
                LayoutInflater li = LayoutInflater.from(context);
                View promptsView = li.inflate(R.layout.custom_alert, null);

                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                        context);

                // set prompts.xml to alertdialog builder
                alertDialogBuilder.setView(promptsView);

                final EditText userInput = (EditText) promptsView
                        .findViewById(R.id.dia1);
                final TextView as=(TextView)promptsView.findViewById(R.id.qw);
                // set dialog message
                alertDialogBuilder
                        .setCancelable(false)
                        .setPositiveButton("OK",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog,int id) {
                                        // get user input and set it to result
                                        String ma=userInput.getText().toString();
                                        SharedPreferences.Editor edit = sh.edit();
                                        edit.putString("rate", ma);
                                        edit.commit();
                                        new SendData().execute("4");//added 4 as a pending request
                                        Toast.makeText(context,ma, Toast.LENGTH_SHORT).show();
                                        Intent i1 = new Intent(con, DriverMapActivity.class);
                                        startActivity(i1);
                                    }
                                })
                        .setNegativeButton("Cancel",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog,int id) {
                                        dialog.cancel();
                                    }
                                });

                // create alert dialog
                AlertDialog alertDialog = alertDialogBuilder.create();

                // show it
                alertDialog.show();
                }

                break;
            case R.id.requestDetailsCompleteBtn:
                new SendData().execute("2");
                Intent i3 = new Intent(con, DriverMapActivity.class);
                startActivity(i3);
                break;
            case R.id.requestDetailsRejectBtn:
                new SendData().execute("0");
                Intent i2 = new Intent(con, DriverMapActivity.class);
                startActivity(i2);

                break;
        }
    }


    private void registerUser(){

        final String msg="Your taxi has been booked with "+driver_name+" for "+request.get("booking_time")+" and will cost "+sh.getString("rate","0");
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


    class SendData {
        ProgressDialog pDialog;
        String s = "";
        int success = -1;
        int error = 0;

        String accept = "0";
        boolean driver = false;

        public SendData() {
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
            params.add(new BasicNameValuePair("rate", sh.getString("rate","0")));
            params.add(new BasicNameValuePair("driver_id", driver_id));
            params.add(new BasicNameValuePair("driver_email", driver_email));
            params.add(new BasicNameValuePair("driver_name", driver_name));
           // params.add(new BasicNameValuePair("driver_name", driver_name));
            params.add(new BasicNameValuePair("group_id", Integer.toString(group_id)));
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

                        if (accept.equals("1")) {
                           registerUser();
                            //Intent intent = new Intent(RequestedUserActivity.this, DriverPositionActivity.class);
/*mapactivi

                            if (request.get("latitude") != null)
                                intent.putExtra("lat", Double.valueOf(request.get("latitude")));
                            else intent.putExtra("lat", "0");

                            if (request.get("longitude") != null)
                                intent.putExtra("long", Double.valueOf(request.get("longitude")));
                            else intent.putExtra("long", "0");

                            if (request.get("drop_latitude") != null)
                                intent.putExtra("drop_lat", Double.valueOf(request.get("drop_latitude")));
                            else intent.putExtra("drop_lat", "0");

                            if (request.get("drop_longitude") != null)
                                intent.putExtra("drop_long", Double.valueOf(request.get("drop_longitude")));
                            else intent.putExtra("drop_long", "0");
*/

                           // intent.putExtra("showdirection", true);
                            Intent d = new Intent(con, DriverMapActivity.class);
                            startActivity(d);



                        }

                        finish();
                    }
                }
            });
        }
    }

}
