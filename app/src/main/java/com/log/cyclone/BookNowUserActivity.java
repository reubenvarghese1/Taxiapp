package com.log.cyclone;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
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
import android.widget.TextView;
import android.widget.Toast;

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
import java.util.List;

import static com.log.cyclone.R.id.spVehicle;

public class BookNowUserActivity extends FragmentActivity implements View.OnClickListener {

    EditText nameEt, phoneEt, etNumPassenger;
    Button rejectBtn, acceptBtn;
    Context con;
    JSONParser jparser = new JSONParser();
    TextView newReqResult;

    SharedPreferences sh;
    String driverId, driverName, driverEmail;
    boolean isdriver;
    long lastrequest;

    private Menu menu;

    Spinner spinner;
    String vehicle;
    Spinner spHour, spMin;
    String stHour, stMin;
    static String stDay, stMonth, stYear;
    static Button setDate;

    String senderId, name, phone, location, droplocation;
    double latitude, longitude, drop_latitude, drop_longitude;

    GPSTracker gps;
    PlacesAutoCompleteAdapter mPlacesAdapter;
    AutoCompleteTextView autoAddress;
    AutoCompleteTextView autoDestination;

    GoogleApiClient mGoogleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_now_user);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Places.GEO_DATA_API)
                .build();

        gps = new GPSTracker(this);

        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);
        stDay = String.format("%02d", day);
        stMonth = String.format("%02d", month + 1);
        stYear = Integer.toString(year);

        sh = getSharedPreferences("CYCLONE_PREF", MODE_PRIVATE);
        driverId = sh.getString("id", null);
        driverName = sh.getString("name", null);
        driverEmail = sh.getString("loginemail", null);
        isdriver = sh.getBoolean("type", false);
        lastrequest = sh.getLong("lastrequest", 0);

        Intent intent = getIntent();
        driverId = intent.getStringExtra("driver_id");
        driverEmail = intent.getStringExtra("driver_email");
        driverName = intent.getStringExtra("driver_name");
        senderId = intent.getStringExtra("sender_id");
        name = intent.getStringExtra("name");
        phone = intent.getStringExtra("phone");
        location = intent.getStringExtra("location");
        droplocation = intent.getStringExtra("droplocation");
        latitude = intent.getDoubleExtra("latitude", 0);
        longitude = intent.getDoubleExtra("longitude", 0);
        drop_latitude = intent.getDoubleExtra("drop_latitude", 0);
        drop_longitude = intent.getDoubleExtra("drop_longitude", 0);

        getActionBar().setHomeButtonEnabled(true);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        con = BookNowUserActivity.this;
        init();
    }

    private void init() {
        nameEt = (EditText) findViewById(R.id.nameEt);
        phoneEt = (EditText) findViewById(R.id.phoneEt);
        etNumPassenger = (EditText) findViewById(R.id.editNumPassengers);
        spinner = (Spinner) findViewById(spVehicle);

        rejectBtn = (Button) findViewById(R.id.editRequestReject);
        acceptBtn = (Button) findViewById(R.id.editRequestAccept);

        newReqResult = (TextView) findViewById(R.id.newReqResultText);

        spHour = (Spinner) findViewById(R.id.spHour);
        spMin = (Spinner) findViewById(R.id.spMin);

        autoAddress = (AutoCompleteTextView) findViewById(R.id.autoAddress);
        autoDestination = (AutoCompleteTextView) findViewById(R.id.autoDestination);

        nameEt.setText(name);
        phoneEt.setText(phone);
        autoAddress.setText(location);
        autoDestination.setText(droplocation);
        etNumPassenger.setText("1");

        rejectBtn.setOnClickListener(this);
        acceptBtn.setOnClickListener(this);

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

        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        setDate = (Button) findViewById(R.id.setDate);
        setDate.setText(year + "-" + String.format("%02d", month + 1) + "-" + String.format("%02d", day));
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.menu = menu;
        getMenuInflater().inflate(R.menu.usermenu, menu);
        menu.removeItem(R.id.userbookmenu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.userProfileMenu:
                startActivity(new Intent(this, UserEditProfileActivity.class));
                break;
            case R.id.userRidesMenu:
                startActivity(new Intent(this, UserRequestActivity.class));
                break;
            case R.id.userMap:
                startActivity(new Intent(this, DriverPositionActivity.class));
                break;
            case R.id.useroutmneu:
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

                                startActivity(new Intent(BookNowUserActivity.this, MainActivity.class));

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
        return false;
    }

    @Override
    public void onClick(View v) {
        if (Util.isConnectingToInternet(this)) {
            AlertDialog.Builder build = new AlertDialog.Builder(con);
            AlertDialog alert;

            switch (v.getId()) {
                case R.id.editRequestReject:
                    //finish();

                    break;
                case R.id.editRequestAccept:
                /*
                if(!TextUtils.isEmpty(nameEt.getText().toString())){
					if(!TextUtils.isEmpty(pickUpEt.getText().toString())){
						if(!TextUtils.isEmpty(destEt.getText().toString())){
							new UpdateInfo().execute();
						}else  Util.showToast(this, "Please enter your password");
					}else  Util.showToast(this, "Please enter your phone number");
				}else Util.showToast(this, "Please enter your name");
				*/
                    //do not allow new request if user has sent a request in the last 30 minutes
                    long now = System.currentTimeMillis();
                    if (now - lastrequest > Long.valueOf("1800000") || (now - lastrequest < 0)) {
                        //if (now - lastrequest > Long.valueOf("1") || (now - lastrequest < 0)) {
                        build.setTitle("New Request");
                        build.setMessage("Do you wanna submit this taxi request?");
                        build.setCancelable(true);
                        build.setPositiveButton("Yes", new Dialog.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                vehicle = String.valueOf(spinner.getSelectedItem());
                                spHour = (Spinner) findViewById(R.id.spHour);
                                spMin = (Spinner) findViewById(R.id.spMin);
                                stHour = spHour.getSelectedItem().toString();
                                stMin = spMin.getSelectedItem().toString();
                                String pickup = autoAddress.getText().toString();
                                String dest = autoDestination.getText().toString();

                                if (latitude == 0 && longitude == 0) {
                                    try {
                                        int i = 0;
                                        Geocoder geocoder = new Geocoder(BookNowUserActivity.this);
                                        List<Address> list = geocoder.getFromLocationName(pickup, 1);
                                        // anticipate failure when geocoding, do until it gets a result, max 3x
                                        while (i < 3 && list.size() < 1) {
                                            list = geocoder.getFromLocationName(pickup, 1);
                                            i++;
                                        }
                                        if (list.size() > 0) {
                                            latitude = list.get(0).getLatitude();
                                            longitude = list.get(0).getLongitude();
                                        } else {
                                            //Log.d("VIP","geocoder failed, address = " + address);
                                            //btnCustomerLocation.setVisibility(View.INVISIBLE);
                                        }
                                    } catch (Throwable e) {
                                        //Log.d("VIP","geocoder exception");
                                    }
                                }

                                if (drop_latitude == 0 && drop_longitude == 0) {
                                    try {
                                        int i = 0;
                                        Geocoder geocoder = new Geocoder(BookNowUserActivity.this);
                                        List<Address> list = geocoder.getFromLocationName(dest, 1);
                                        // anticipate failure when geocoding, do until it gets a result, max 3x
                                        while (i < 3 && list.size() < 1) {
                                            list = geocoder.getFromLocationName(dest, 1);
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
                                }

                                new NewRequest().execute();
                                //Toast.makeText(con, "You rejected this customer request.", Toast.LENGTH_LONG).show();
                                finish();
                            }
                        });

                        build.setNegativeButton("Cancel", new Dialog.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // TODO Auto-generated method stub
                                dialog.cancel();
                            }
                        });

                        alert = build.create();
                        alert.show();

                    } else {
                        build.setTitle("New Taxi Request");
                        build.setMessage("You already sent request in the last 30 minutes");
                        build.setCancelable(true);
                        build.setNegativeButton("OK", new Dialog.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        });

                        alert = build.create();
                        alert.show();
                    }
                    break;
            }
        } else {
            Util.showNoInternetDialog(this);
        }
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
            int month1=month+1;
            stYear = Integer.toString(year);
            stDay=""+day;
            stMonth=""+month1;
            stYear=""+year;
            setDate(stDay, stMonth, stYear);
        }
    }

    public static void setDate(String stDay, String stMonth, String stYear) {
        setDate.setText(stYear + "-" + stMonth + "-" + stDay);
    }

    class NewRequest {
        String toastText = "Internet Problem";
        String regiresult = "";
        int success = 0;
        int error = 0;
        String errmsg = "Server is down";

        public void execute() {

            String name = nameEt.getText().toString();
            String phone = phoneEt.getText().toString();
            String pickup = autoAddress.getText().toString();
            String dest = autoDestination.getText().toString();
            String numPassenger = etNumPassenger.getText().toString();

            String bookingTime = stYear + "-" + stMonth + "-" + stDay + " " + stHour + ":" + stMin + ":00";
            Toast.makeText(con, bookingTime, Toast.LENGTH_SHORT).show();
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
            params.add(new BasicNameValuePair("driver_id", driverId));
            params.add(new BasicNameValuePair("driver_email", driverEmail));
            params.add(new BasicNameValuePair("driver_name", driverName));
            params.add(new BasicNameValuePair("sender_id", UserInfo.getId()));
            params.add(new BasicNameValuePair("name", name));
            params.add(new BasicNameValuePair("phone", phone));
            params.add(new BasicNameValuePair("location", pickup));
            params.add(new BasicNameValuePair("droplocation", dest));
            params.add(new BasicNameValuePair("latitude", Double.toString(latitude)));
            params.add(new BasicNameValuePair("longitude", Double.toString(longitude)));
            params.add(new BasicNameValuePair("accept", "0"));
            params.add(new BasicNameValuePair("timedate", datetime));
            params.add(new BasicNameValuePair("num_passengers", numPassenger));
            params.add(new BasicNameValuePair("booking_time", bookingTime));
            params.add(new BasicNameValuePair("types", vehicle));
            params.add(new BasicNameValuePair("drop_latitude", Double.toString(drop_latitude)));
            params.add(new BasicNameValuePair("drop_longitude", Double.toString(drop_longitude)));

            jparser.makeHttpRequest(Globals.taxiRequUrl, "POST", params, new ServerCallback() {

                @Override
                public void onSuccess(JSONObject jsonObject) {
                    try {
                        success = jsonObject.getInt("success");
                        regiresult = jsonObject.getString("message");

                        toastText = "You successfully sent new taxi request.";

                        SharedPreferences.Editor edit = sh.edit();
                        edit.putLong("lastrequest", System.currentTimeMillis());    // to prevent sending many requests
                        edit.commit();

                    } catch (JSONException e) {
                        toastText = "There are some problems";
                        e.printStackTrace();
                    } catch (Exception e) {
                        error = 1;
                    }

                    if (error == 1) {
                        Toast.makeText(con, errmsg, Toast.LENGTH_SHORT).show();
                        if (Util.isConnectingToInternet(con)) {
                            newReqResult.setText("Server is down. Please try again later");
                            newReqResult.setVisibility(View.VISIBLE);
                        } else {
                            Util.showNoInternetDialog(con);
                        }

                        return;
                    }

                    if (success == 0) {
                        newReqResult.setText(regiresult);
                        newReqResult.setVisibility(View.VISIBLE);
                    } else if (success == 1) {
                        Toast.makeText(con, toastText, Toast.LENGTH_LONG).show();
                    }
                }
            });
        }
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
}
