package com.log.cyclone;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.maps.android.SphericalUtil;
import com.log.cyclone.General.Globals;
import com.log.cyclone.General.PlacesAutoCompleteAdapter;
import com.log.cyclone.General.UserInfo;
import com.log.cyclone.util.GPSTracker;
import com.log.cyclone.util.JSONParser;
import com.log.cyclone.util.ServerCallback;
import com.log.cyclone.util.Util;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static com.log.cyclone.R.id.spVehicle;

public class BookNowDriverActivity extends FragmentActivity {
    EditText editName, editPhnNumber, editNumPassengers,rate12;
    Button payNow, bookNow;
    private Menu menu;
    Context con;
    JSONParser jparser = new JSONParser();
    boolean check = true;
    GPSTracker gps;
    SharedPreferences sh;
    int group_id;
    Spinner spinner;
    String vehicle;
    Spinner spHour, spMin;
    String stHour, stMin;
    static String stDay, stMonth, stYear;
    static Button setDate;
    Handler handler;
    Runnable run;
    String driver_id, driver_email, driver_name;

    PlacesAutoCompleteAdapter mPlacesAdapter;
    AutoCompleteTextView autoAddress;
    AutoCompleteTextView autoDestination;

    GoogleApiClient mGoogleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        setContentView(R.layout.book_now_driver_side);
        getActionBar().setDisplayShowTitleEnabled(false);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Places.GEO_DATA_API)
                .build();

        con = BookNowDriverActivity.this;
        gps = new GPSTracker(this);

        sh = getSharedPreferences("CYCLONE_PREF", MODE_PRIVATE);
        sh = getSharedPreferences("CYCLONE_PREF", MODE_PRIVATE);
        driver_id = sh.getString("id", "");
        driver_name = sh.getString("name", "");
        group_id = sh.getInt("group_id", 0);
        driver_email = sh.getString("loginemail", "");
        rate12=(EditText)findViewById(R.id.rate) ;
        editName = (EditText) findViewById(R.id.editName);
        editPhnNumber = (EditText) findViewById(R.id.editPhnNumber);
        editNumPassengers = (EditText) findViewById(R.id.editNumPassengers);
        spinner = (Spinner) findViewById(spVehicle);
        spHour = (Spinner) findViewById(R.id.spHour);
        spMin = (Spinner) findViewById(R.id.spMin);

        payNow = (Button) findViewById(R.id.payNow);
        bookNow = (Button) findViewById(R.id.bookNow);
        bookNow.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                // TODO Auto-generated method stub
                showDialog();
            }
        });

        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);
        stDay = String.format("%02d", day);
        stMonth = String.format("%02d", month + 1);
        stYear = Integer.toString(year);
        //Toast.makeText(con,stDay+stMonth+stYear, Toast.LENGTH_LONG).show();
        setDate = (Button) findViewById(R.id.setDate);
        setDate.setText(year + "-" + String.format("%02d", month + 1) + "-" + String.format("%02d", day));

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.vehicle, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        ArrayAdapter<CharSequence> adapterHour = ArrayAdapter.createFromResource(this,
                R.array.bookingtime_hour, android.R.layout.simple_spinner_item);
        adapterHour.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spHour.setAdapter(adapterHour);

        ArrayAdapter<CharSequence> adapterMin = ArrayAdapter.createFromResource(this,
                R.array.bookingtime_minute, android.R.layout.simple_spinner_item);
        adapterMin.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spMin.setAdapter(adapterMin);

        autoAddress = (AutoCompleteTextView) findViewById(R.id.autoAddress);
        autoDestination = (AutoCompleteTextView) findViewById(R.id.autoDestination);
    }

    public void showDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Do you wanna accept customer request?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        //BookNowDriverActivity.this.finish();

                        spHour = (Spinner) findViewById(R.id.spHour);
                        spMin = (Spinner) findViewById(R.id.spMin);
                        stHour = spHour.getSelectedItem().toString();
                        stMin = spMin.getSelectedItem().toString();

                        String rate=rate12.getText().toString().trim();
                        String name = editName.getText().toString().trim();
                        String PhnNumber = editPhnNumber.getText().toString().trim();
                        String address = autoAddress.getText().toString().trim();
                        String destination = autoDestination.getText().toString().trim();
                        String numPassengers = editNumPassengers.getText().toString().trim();
                        double latitude = 0, longitude = 0, drop_latitude = 0, drop_longitude = 0;
                        registerUser(PhnNumber,rate);
                        if (name.equalsIgnoreCase("") || PhnNumber.equalsIgnoreCase("") || address.equalsIgnoreCase("") || destination.equalsIgnoreCase("") || numPassengers.equalsIgnoreCase("")) {
                            Toast.makeText(BookNowDriverActivity.this, "Please enter all fields", Toast.LENGTH_LONG).show();
                        } else {
                            vehicle = String.valueOf(spinner.getSelectedItem());

                            try {
                                int i = 0;
                                Geocoder geocoder = new Geocoder(BookNowDriverActivity.this);
                                List<Address> list = geocoder.getFromLocationName(address, 1);
                                // anticipate failure when geocoding, do until it gets a result, max 3x
                                while (i < 3 && list.size() < 1) {
                                    list = geocoder.getFromLocationName(address, 1);
                                    i++;
                                }
                                if (list.size() > 0) {
                                    latitude = list.get(0).getLatitude();
                                    longitude = list.get(0).getLongitude();
                                } else {
                                    //Log.d("VIP","geocoder failed, address = " + address);
                                    //btnCustomerLocation.setVisibility(View.INVISIBLE);
                                }

                                i = 0;
                                list = geocoder.getFromLocationName(destination, 1);
                                // anticipate failure when geocoding, do until it gets a result, max 3x
                                while (i < 3 && list.size() < 1) {
                                    list = geocoder.getFromLocationName(destination, 1);
                                    i++;
                                }
                                if (list.size() > 0) {
                                    drop_latitude = list.get(0).getLatitude();
                                    drop_longitude = list.get(0).getLongitude();
                                } else {
                                    //Log.d("VIP","geocoder failed, address = " + address);
                                    //btnCustomerLocation.setVisibility(View.INVISIBLE);
                                }
                            } catch (Throwable e) {
                                //Log.d("VIP","geocoder exception");
                            }

                            new RequestTaxi(name, PhnNumber, address, destination, "1", numPassengers, latitude, longitude, drop_latitude, drop_longitude,rate).execute();
                        }
                        dialog.cancel();
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                        spHour = (Spinner) findViewById(R.id.spHour);
                        spMin = (Spinner) findViewById(R.id.spMin);
                        stHour = spHour.getSelectedItem().toString();
                        stMin = spMin.getSelectedItem().toString();
                        String rate=rate12.getText().toString().trim();
                        String name = editName.getText().toString().trim();
                        String PhnNumber = editPhnNumber.getText().toString().trim();
                        String address = autoAddress.getText().toString().trim();
                        String destination = autoDestination.getText().toString().trim();
                        String numPassengers = editNumPassengers.getText().toString().trim();
                        double latitude = 0, longitude = 0, drop_latitude = 0, drop_longitude = 0;

                        if (name.equalsIgnoreCase("") || PhnNumber.equalsIgnoreCase("") || address.equalsIgnoreCase("") || destination.equalsIgnoreCase("") || numPassengers.equalsIgnoreCase("")) {
                            Toast.makeText(BookNowDriverActivity.this, "Please enter all fields", Toast.LENGTH_LONG).show();
                        } else {
                            vehicle = String.valueOf(spinner.getSelectedItem());

                            try {
                                int i = 0;
                                Geocoder geocoder = new Geocoder(BookNowDriverActivity.this);
                                List<Address> list = geocoder.getFromLocationName(address, 1);
                                // anticipate failure when geocoding, do until it gets a result, max 3x
                                while (i < 3 && list.size() < 1) {
                                    list = geocoder.getFromLocationName(address, 1);
                                    i++;
                                }
                                if (list.size() > 0) {
                                    latitude = list.get(0).getLatitude();
                                    longitude = list.get(0).getLongitude();
                                } else {
                                    //Log.d("VIP","geocoder failed, address = " + address);
                                    //btnCustomerLocation.setVisibility(View.INVISIBLE);
                                }

                                i = 0;
                                list = geocoder.getFromLocationName(destination, 1);
                                // anticipate failure when geocoding, do until it gets a result, max 3x
                                while (i < 3 && list.size() < 1) {
                                    list = geocoder.getFromLocationName(destination, 1);
                                    i++;
                                }
                                if (list.size() > 0) {
                                    drop_latitude = list.get(0).getLatitude();
                                    drop_longitude = list.get(0).getLongitude();
                                } else {
                                    //Log.d("VIP","geocoder failed, address = " + address);
                                    //btnCustomerLocation.setVisibility(View.INVISIBLE);
                                }
                            } catch (Throwable e) {
                                //Log.d("VIP","geocoder exception");
                            }

                            new RejectRequestTaxi(name, PhnNumber, address, destination, "0", numPassengers, latitude, longitude, drop_latitude, drop_longitude,rate).execute();
                        }
                        dialog.cancel();
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

    public void showDatePickerDialog(View v) {
        DialogFragment newFragment = new DatePickerFragment();
        newFragment.show(getSupportFragmentManager(), "datePicker");
    }

    public static class DatePickerFragment extends DialogFragment
            implements DatePickerDialog.OnDateSetListener {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the current date as the default date in the picker
            final Calendar c = Calendar.getInstance();
            int year = c.get(Calendar.YEAR);
            int month = c.get(Calendar.MONTH);
            int day = c.get(Calendar.DAY_OF_MONTH);

            // Create a new instance of DatePickerDialog and return it
            return new DatePickerDialog(getActivity(), this, year, month, day);
        }

        public void onDateSet(DatePicker view, int year, int month, int day) {
            // Do something with the date chosen by the user
            stDay = String.format("%02d", day);
            stMonth = String.format("%02d", month + 1);
            stYear = Integer.toString(year);

            setDate(stDay, stMonth, stYear);
        }
    }

    public static void setDate(String stDay, String stMonth, String stYear) {
        setDate.setText(stYear + "-" + stMonth + "-" + stDay);
    }

    private void registerUser(final String phone123,String rate12345){
        String bookingTime = stYear + "-" + stMonth + "-" + stDay + " " + stHour + ":" + stMin + ":00";

        final String msg="Dear customer we are pleased to inform you that your taxi has been booked for at "+bookingTime+" for "+rate12345;
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
                params.put("number",phone123);
                return params;
            }

        };

        RequestQueue requestQueue = Volley.newRequestQueue(this);
        requestQueue.add(stringRequest);
    }

    class RejectRequestTaxi {
        ProgressDialog pDialog;
        String toastText = "Internet Problem";
        String regiresult = "";
        int success = 0;
        int error = 0;
        String msg = "";
        String name, phone, location, droplocation, accept, timedate, numPassengers,rate;
        double latitude, longitude, drop_latitude, drop_longitude;

        RejectRequestTaxi(String name, String phone, String location, String droplocation, String accept, String numPassengers, double latitude, double longitude, double drop_latitude, double drop_longitude,String rate) {
            this.name = name;
            this.phone = phone;
            this.location = location;
            this.droplocation = droplocation;
            this.latitude = latitude;
            this.longitude = longitude;
            this.accept = accept;
            this.timedate = timedate;
            this.numPassengers = numPassengers;
            this.drop_latitude = drop_latitude;
            this.drop_longitude = drop_longitude;
            this.rate=rate;
            pDialog = new ProgressDialog(con);
            pDialog.setMessage("Requesting for Taxi. Please wait...");
            pDialog.setIndeterminate(false);
            pDialog.setCancelable(true);
            pDialog.show();
        }

        public void execute() {
            Calendar cal = Calendar.getInstance();
            String datetime = cal.get(Calendar.YEAR) + "-" + (cal.get(Calendar.MONTH) + 1) + "-" + cal.get(Calendar.DAY_OF_MONTH) + " " +
                    cal.get(Calendar.HOUR_OF_DAY) + ":" + cal.get(Calendar.MINUTE) + ":" + cal.get(Calendar.SECOND);

            String bookingTime = stYear + "-" + stMonth + "-" + stDay + " " + stHour + ":" + stMin + ":00";
            Toast.makeText(con,bookingTime, Toast.LENGTH_LONG).show();
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date bookDate;
            long timestamp = 0;

            try {
                bookDate = df.parse(bookingTime);

                timestamp = bookDate.getTime();
            } catch (Exception e) {
            }



            if (timestamp == 0) {

                bookingTime = datetime;
            }

            List<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair("driver_id","0"));
            params.add(new BasicNameValuePair("driver_email", null));
            params.add(new BasicNameValuePair("driver_name", null));
            params.add(new BasicNameValuePair("sender_id", getRandomInt(1, 1000) + ""));
            params.add(new BasicNameValuePair("name", name));
            params.add(new BasicNameValuePair("phone", phone));
            params.add(new BasicNameValuePair("location", location));
            params.add(new BasicNameValuePair("droplocation", droplocation));
            //params.add(new BasicNameValuePair("latitude", gps.getLatitude()+""));
            //params.add(new BasicNameValuePair("longitude", gps.getLongitude()+""));
            params.add(new BasicNameValuePair("latitude", Double.toString(latitude)));
            params.add(new BasicNameValuePair("longitude", Double.toString(longitude)));
            params.add(new BasicNameValuePair("accept", accept));
            params.add(new BasicNameValuePair("timedate", datetime));
            params.add(new BasicNameValuePair("num_passengers", numPassengers));
            params.add(new BasicNameValuePair("group_id", Integer.toString(group_id)));
            params.add(new BasicNameValuePair("booking_time", bookingTime));
            params.add(new BasicNameValuePair("types", vehicle));
            params.add(new BasicNameValuePair("drop_latitude", Double.toString(drop_latitude)));
            params.add(new BasicNameValuePair("drop_longitude", Double.toString(drop_longitude)));
            params.add(new BasicNameValuePair("rate",rate));
            params.add(new BasicNameValuePair("position","2"));
            jparser.makeHttpRequest(Globals.taxiRequUrl, "POST", params, new ServerCallback() {
                @Override
                public void onSuccess(JSONObject jsonObject) {
                    try {
                        success = jsonObject.getInt("success");
                        msg = jsonObject.getString("message");

                        pDialog.dismiss();
                        //Toast.makeText(con, msg, Toast.LENGTH_SHORT).show();

                        if (error == 1) {
                            if (Util.isConnectingToInternet(con)) {
                                Toast.makeText(con, "Server is down. Please try again", Toast.LENGTH_SHORT).show();
                            } else {
                                Util.showNoInternetDialog(con);
                            }
                            return;
                        }

                        if (success == 1) {

                            if (accept.equals("1")) {
                                Intent intent = new Intent(BookNowDriverActivity.this, DriverMapActivity.class);
                                intent.putExtra("lat", latitude);
                                intent.putExtra("long", longitude);
                                intent.putExtra("drop_lat", drop_latitude);
                                intent.putExtra("drop_long", drop_longitude);
                                intent.putExtra("showdirection", true);
                                startActivity(intent);

                            }
                            finish();
                        }
                    } catch (JSONException e) {
                        error = 1;
                        msg = e.getMessage();
                    }
                }
            });
        }
    }

    class RequestTaxi {
        ProgressDialog pDialog;
        String toastText = "Internet Problem";
        String regiresult = "";
        int success = 0;
        int error = 0;
        String msg = "";
        String name, phone, location, droplocation, accept, timedate, numPassengers,rate;
        double latitude, longitude, drop_latitude, drop_longitude;

        RequestTaxi(String name, String phone, String location, String droplocation, String accept, String numPassengers, double latitude, double longitude, double drop_latitude, double drop_longitude,String rate) {
            this.name = name;
            this.phone = phone;
            this.location = location;
            this.droplocation = droplocation;
            this.latitude = latitude;
            this.longitude = longitude;
            this.accept = accept;
            this.rate=rate;
            this.timedate = timedate;
            this.numPassengers = numPassengers;
            this.drop_latitude = drop_latitude;
            this.drop_longitude = drop_longitude;
            pDialog = new ProgressDialog(con);
            pDialog.setMessage("Requesting for Taxi. Please wait...");
            pDialog.setIndeterminate(false);
            pDialog.setCancelable(true);
            pDialog.show();
        }

        public void execute() {
            String bookingTime = stYear + "-" + stMonth + "-" + stDay + " " + stHour + ":" + stMin + ":00";
           Toast.makeText(con,bookingTime, Toast.LENGTH_SHORT).show();
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date bookDate;
            long timestamp = 0;

            try {

                bookDate = df.parse(bookingTime);

                timestamp = bookDate.getTime();
            } catch (Exception e) {
            }

            Calendar cal = Calendar.getInstance();
            String datetime = cal.get(Calendar.YEAR) + "-" + (cal.get(Calendar.MONTH) + 1) + "-" + cal.get(Calendar.DAY_OF_MONTH) + " " +
                    cal.get(Calendar.HOUR_OF_DAY) + ":" + cal.get(Calendar.MINUTE) + ":" + cal.get(Calendar.SECOND);
            if (timestamp == 0) {
                bookingTime = datetime;
            }

            List<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair("driver_id", driver_id));
            params.add(new BasicNameValuePair("driver_email", driver_email));
            params.add(new BasicNameValuePair("driver_name",driver_name ));
            params.add(new BasicNameValuePair("sender_id", getRandomInt(1, 1000) + ""));
            params.add(new BasicNameValuePair("name", name));
            params.add(new BasicNameValuePair("phone", phone));
            params.add(new BasicNameValuePair("location", location));
            params.add(new BasicNameValuePair("droplocation", droplocation));
            //params.add(new BasicNameValuePair("latitude", gps.getLatitude()+""));
            //params.add(new BasicNameValuePair("longitude", gps.getLongitude()+""));
            params.add(new BasicNameValuePair("latitude", Double.toString(latitude)));
            params.add(new BasicNameValuePair("longitude", Double.toString(longitude)));
            params.add(new BasicNameValuePair("accept", accept));
            params.add(new BasicNameValuePair("timedate", datetime));
            params.add(new BasicNameValuePair("num_passengers", numPassengers));
            params.add(new BasicNameValuePair("group_id", Integer.toString(group_id)));
            params.add(new BasicNameValuePair("booking_time", bookingTime));
            params.add(new BasicNameValuePair("types", vehicle));
            params.add(new BasicNameValuePair("drop_latitude", Double.toString(drop_latitude)));
            params.add(new BasicNameValuePair("drop_longitude", Double.toString(drop_longitude)));
            params.add(new BasicNameValuePair("rate",rate));
            params.add(new BasicNameValuePair("position","2"));
            jparser.makeHttpRequest(Globals.taxiRequUrl, "POST", params, new ServerCallback() {
                @Override
                public void onSuccess(JSONObject jsonObject) {
                    try {
                        success = jsonObject.getInt("success");
                        msg = jsonObject.getString("message");

                        pDialog.dismiss();
                        //Toast.makeText(con, msg, Toast.LENGTH_SHORT).show();

                        if (error == 1) {
                            if (Util.isConnectingToInternet(con)) {
                                Toast.makeText(con, "Server is down. Please try again", Toast.LENGTH_SHORT).show();
                            } else {
                                Util.showNoInternetDialog(con);
                            }
                            return;
                        }

                        if (success == 1) {

                            if (accept.equals("1")) {
                                Intent intent = new Intent(BookNowDriverActivity.this, DriverMapActivity.class);
                                intent.putExtra("lat", latitude);
                                intent.putExtra("long", longitude);
                                intent.putExtra("drop_lat", drop_latitude);
                                intent.putExtra("drop_long", drop_longitude);
                                intent.putExtra("showdirection", true);
                                startActivity(intent);

                            }
                            finish();
                        }
                    } catch (JSONException e) {
                        error = 1;
                        msg = e.getMessage();
                    }
                }
            });
        }
    }

    public static int getRandomInt(int min, int max) {
        Random random = new Random();

        return random.nextInt((max - min) + 1) + min;
    }

    public static ArrayList<Integer> getRandomNonRepeatingIntegers(int size, int min,
                                                                   int max) {
        ArrayList<Integer> numbers = new ArrayList<Integer>();

        while (numbers.size() < size) {
            int random = getRandomInt(min, max);

            if (!numbers.contains(random)) {
                numbers.add(random);
            }
        }

        return numbers;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.menu = menu;
        getMenuInflater().inflate(R.menu.driver_menu, menu);
        menu.removeItem(R.id.bookStatus);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        SharedPreferences.Editor edit;
        AlertDialog.Builder build;
        AlertDialog alert;

        switch (item.getItemId()) {
            case R.id.driverChat:
                //new GetRequestedRides().execute();
                Intent i = new Intent(this, ChatActivity.class);
                startActivity(i);
                finish();
                break;
            case R.id.driverPooledJobListMenu:
                startActivity(new Intent(this, DriverActivity.class));
                finish();
                break;
            case R.id.driverMap:
                startActivity(new Intent(this, DriverMapActivity.class));
                finish();
                break;
            case R.id.driverStatus:
                if (!check) {
                    check = true;
                    new SendModeData().execute("1");
                    menu.getItem(3).setIcon(getResources().getDrawable(R.drawable.green_dot));
                } else {
                    new SendModeData().execute("0");
                    check = false;
                    menu.getItem(3).setIcon(getResources().getDrawable(R.drawable.red_dot));
                }
                break;
            case R.id.driverLogoutMenu:
                build = new AlertDialog.Builder(con);
                build.setTitle("Logout");
                build.setMessage("Do you really wanna logout?");
                build.setCancelable(true);
                build.setPositiveButton("Yes", new Dialog.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        new SendModeData().execute("-1");
                    }
                });

                build.setNegativeButton("Cancel", new Dialog.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

                alert = build.create();
                alert.show();

                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();

        mPlacesAdapter = new PlacesAutoCompleteAdapter(this, android.R.layout.simple_list_item_1,
                mGoogleApiClient, toBounds(new LatLng(gps.getLatitude(), gps.getLongitude()), 1000000), null);    // 1,000 km radius from current location

        autoAddress.setOnItemClickListener(mAutocompleteClickListener);
        autoAddress.setAdapter(mPlacesAdapter);
        autoDestination.setOnItemClickListener(mAutocompleteClickListener);
        autoDestination.setAdapter(mPlacesAdapter);
    }

    @Override
    protected void onStop() {
        mPlacesAdapter = null;
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    private AdapterView.OnItemClickListener mAutocompleteClickListener
            = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            final PlacesAutoCompleteAdapter.PlaceAutocomplete item = mPlacesAdapter.getItem(position);
            final String placeId = String.valueOf(item.placeId);
            PendingResult<PlaceBuffer> placeResult = Places.GeoDataApi
                    .getPlaceById(mGoogleApiClient, placeId);
            placeResult.setResultCallback(mUpdatePlaceDetailsCallback);
        }
    };
    private ResultCallback<PlaceBuffer> mUpdatePlaceDetailsCallback
            = new ResultCallback<PlaceBuffer>() {
        @Override
        public void onResult(PlaceBuffer places) {
            if (!places.getStatus().isSuccess()) {
                Log.e("place", "Place query did not complete. Error: " +
                        places.getStatus().toString());
                return;
            }
            // Selecting the first object buffer.
            final Place place = places.get(0);
        }
    };

    public LatLngBounds toBounds(LatLng center, double radius) {
        LatLng southwest = SphericalUtil.computeOffset(center, radius * Math.sqrt(2.0), 225);
        LatLng northeast = SphericalUtil.computeOffset(center, radius * Math.sqrt(2.0), 45);
        return new LatLngBounds(southwest, northeast);
    }

    public void scheduleThread() {
        handler = new Handler();
        run = new Runnable() {

            @Override
            public void run() {
                if (Util.isConnectingToInternet(BookNowDriverActivity.this)) {
                    gps = new GPSTracker(con);
                    new SendLocation().execute();

                    //Toast.makeText(con, gps.getLatitude()+" "+gps.getLongitude(), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(con, "Internet is not active", Toast.LENGTH_SHORT).show();
                }
            }
        };
        handler.postDelayed(run, 30000);
    }

    class SendLocation {
        ProgressDialog pDialog;
        String s = "";
        int success = -1;
        int error = 0;
        boolean driver = false;

        public void execute() {
            List<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair("email", driver_email));
            params.add(new BasicNameValuePair("latitude", gps.getLatitude() + ""));
            params.add(new BasicNameValuePair("longitude", gps.getLongitude() + ""));

            jparser.makeHttpRequest(Globals.dataSendUrl, "POST", params, new ServerCallback() {
                @Override
                public void onSuccess(JSONObject jsonObject) {
                    try {
                        success = jsonObject.getInt("success");
                        s = jsonObject.getString("message");

                        if (success == 0) {

                        } else if (success == 1) {
                            scheduleThread();
                        }
                    } catch (JSONException e) {
                        if (Util.isConnectingToInternet(con)) {
                            Toast.makeText(con, "Server is down, Please try again later", Toast.LENGTH_SHORT).show();
                        } else
                            Util.showNoInternetDialog(con);
                        return;
                    }
                }
            });
        }
    }

    class SendModeData {
        ProgressDialog pDialog;
        String s = "";
        int success = -1;
        int error = 0;
        boolean isLogout = false;

        public SendModeData() {
            pDialog = new ProgressDialog(con);
            pDialog.setMessage("Sending data. Please wait...");
            pDialog.setIndeterminate(false);
            pDialog.setCancelable(true);
            pDialog.show();
        }

        public void execute(String... st) {
            if (st[0] == "-1") {
                isLogout = true;
                st[0] = "0";
            }

            List<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair("email", UserInfo.getEmail()));
            params.add(new BasicNameValuePair("mode", st[0]));

            jparser.makeHttpRequest(Globals.modeSendUrl, "POST", params, new ServerCallback() {
                @Override
                public void onSuccess(JSONObject jsonObject) {
                    try {
                        success = jsonObject.getInt("success");
                        s = jsonObject.getString("message");

                        pDialog.dismiss();

                        if (isLogout) {
                            SharedPreferences.Editor edit = sh.edit();
                            edit.putString("loginemail", null);
                            edit.putString("loginpass", null);
                            edit.putBoolean("type", false);
                            edit.putInt("group_id", 0);
                            edit.commit();

                            startActivity(new Intent(BookNowDriverActivity.this, MainActivity.class));
                            finish();
                        }
                    } catch (JSONException e) {
                        if (Util.isConnectingToInternet(con)) {
                            Toast.makeText(con, "Server is down, Please try again later", Toast.LENGTH_SHORT).show();
                        } else
                            Util.showNoInternetDialog(con);
                        return;
                    }
                }
            });
        }
    }
}
