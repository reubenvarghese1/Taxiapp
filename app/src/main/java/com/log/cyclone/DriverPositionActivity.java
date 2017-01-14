package com.log.cyclone;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognizerIntent;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.SphericalUtil;
import com.log.cyclone.General.Driver;
import com.log.cyclone.General.Globals;
import com.log.cyclone.General.PlacesAutoCompleteAdapter;
import com.log.cyclone.General.UserInfo;
import com.log.cyclone.util.GPSTracker;
import com.log.cyclone.util.JSONParser;
import com.log.cyclone.util.ServerCallback;
import com.log.cyclone.util.Util;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class DriverPositionActivity extends Activity implements OnMarkerClickListener {

    // Google Map
    private GoogleMap googleMap;

    Context con;

    ArrayList<HashMap<String, String>> rides;
    ArrayList<Driver> drivers;
    JSONParser jparser = new JSONParser();
    GPSTracker gps;

    HashMap<Marker, Driver> markers;
    ArrayList<Marker> mapMarker;

    Handler handler;
    Runnable run;

    Driver selectedDriver;
    Button spchBtn;
    private final int REQ_CODE_SPEECH_INPUT = 100;

    Intent alIntent;
    AlarmManager alarmManager;
    PendingIntent appIntent;

    SharedPreferences sh;

    private Menu menu;
    double latitude, longitude;

    String driverEmail;
    float distance;
    double pickupLatitude, pickupLongitude;

    long lastnotifytime;

    PlacesAutoCompleteAdapter mPlacesAdapter;
    AutoCompleteTextView autoAddress;
    AutoCompleteTextView autoDestination;

    GoogleApiClient mGoogleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.driver_position_layout);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Places.GEO_DATA_API)
                .build();

        sh = getSharedPreferences("CYCLONE_PREF", MODE_PRIVATE);

        drivers = new ArrayList<Driver>();
        gps = new GPSTracker(this);
        con = DriverPositionActivity.this;
        spchBtn = (Button) findViewById(R.id.speechBtn);

        if (!Util.isGPSOn(this)) {
            GPSTracker.showSettingsAlert(this);
        }

        try {
            // Loading map
            initilizeMap();

        } catch (Exception e) {
            e.printStackTrace();
        }

        markers = new HashMap<Marker, Driver>();
        mapMarker = new ArrayList<Marker>();

        spchBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
                intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
                        getString(R.string.speech_prompt));
                try {
                    startActivityForResult(intent, REQ_CODE_SPEECH_INPUT);
                } catch (ActivityNotFoundException a) {
                    Toast.makeText(getApplicationContext(),
                            getString(R.string.speech_not_supported),
                            Toast.LENGTH_SHORT).show();
                }
            }
        });

        new GetRequestedRides().execute();

        startUpdateCheck();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();

        mPlacesAdapter = new PlacesAutoCompleteAdapter(this, android.R.layout.simple_list_item_1,
                mGoogleApiClient, toBounds(new LatLng(gps.getLatitude(), gps.getLongitude()), 1000000), null);    // 1,000 km radius from current location

    }

    @Override
    protected void onStop() {
        mPlacesAdapter = null;
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQ_CODE_SPEECH_INPUT:
                if (resultCode == RESULT_OK && null != data) {
                    ArrayList<String> result = data
                            .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    String talk = result.get(0);
                    Toast.makeText(con, talk, Toast.LENGTH_LONG).show();

                    if ((talk.contains("driver near to my location")) ||
                            (talk.contains("driver near to me")) ||
                            (talk.contains("nearest taxi")) ||
                            (talk.contains("nearest cab")) ||
                            (talk.contains("taxi")) ||
                            (talk.contains("nearest driver")) ||
                            (talk.contains("who is near to me")) ||
                            (talk.contains("nearest driver to my location")) ||
                            (talk.contains("nearest driver to me")) ||
                            (talk.contains("request nearest driver")) ||
                            (talk.contains("who is the nearest driver")) ||
                            (talk.contains("closest driver to me")) ||
                            (talk.contains("find the closest driver"))) {
                        findTheNearestDriver(null);
                    } else if ((talk.contains("the driver near")) ||
                            (talk.contains("driver near")) ||
                            (talk.contains("taxi near to")) ||
                            (talk.contains("driver near to")) ||
                            (talk.contains("taxi near")) ||
                            (talk.contains("driver at")) ||
                            (talk.contains("taxi at")) ||
                            (talk.contains("cabs near")) ||
                            (talk.contains("cab near")) ||
                            (talk.contains("cabs at")) ||
                            (talk.contains("cab near to")) ||
                            (talk.contains("cabs near to")))

                    {
                        Toast.makeText(con, talk.substring(15), Toast.LENGTH_LONG).show();
                        findTheNearestDriver(talk.substring(15).trim());
                    }
                }
                break;
        }
    }

    public void findTheNearestDriver(String loc) {

        Location my = new Location("My");
        if (loc == null) {
            my.setLatitude(gps.getLatitude());
            my.setLongitude(gps.getLongitude());
        } else {
            my = new Location(loc);
        }

        ArrayList<Float> distance = new ArrayList<Float>();
        for (int i = 0; i < mapMarker.size(); i++) {
            LatLng pos = mapMarker.get(i).getPosition();
            Location markLoc = new Location("Driver");
            markLoc.setLatitude(pos.latitude);
            markLoc.setLongitude(pos.longitude);
            distance.add(my.distanceTo(markLoc));
        }
        int min = 0;
        float minDis = distance.get(min);
        for (int i = 0; i < distance.size(); i++) {
            if (distance.get(i) < minDis) {
                minDis = distance.get(i);
                min = i;
            }
        }
        Marker m = mapMarker.get(min);
        onMarkerClick(m);
    }

    private void showNotification() {
        NotificationCompat.Builder noti1 = new NotificationCompat.Builder(con);
        Intent i1 = new Intent(con, DriverPositionActivity.class);
        noti1.setContentTitle("Driver is less than 2 km!");
        //noti1.setContentText("Click to see the taxi information ");
        noti1.setSmallIcon(android.R.drawable.ic_dialog_alert);
        noti1.setTicker("New Notification from Cyclone");

        Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        noti1.setSound(notification);

        PendingIntent pi1 = PendingIntent.getActivity(con, 100, i1, PendingIntent.FLAG_UPDATE_CURRENT);
        noti1.setAutoCancel(true);
        noti1.setContentIntent(pi1);

        NotificationManager mgr = (NotificationManager) con.getSystemService(Context.NOTIFICATION_SERVICE);
        mgr.notify(110, noti1.build());
    }

    public void startUpdateCheck() {
        alIntent = new Intent(this, UpdateReceiver.class);
        appIntent = PendingIntent.getBroadcast(this, 1000, alIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

        // 1 min in millisecond= 1*60*1000;
        alarmManager.setRepeating(AlarmManager.RTC, System.currentTimeMillis(), 30 * 1000, appIntent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.menu = menu;
        getMenuInflater().inflate(R.menu.usermenu, menu);
        menu.removeItem(R.id.userMap);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.userProfileMenu:
                startActivity(new Intent(this, UserEditProfileActivity.class));
                finish();
                break;
            case R.id.userbookmenu:
                startActivity(new Intent(this, BookNowUserActivity.class));
                break;
            case R.id.userRidesMenu:
                startActivity(new Intent(this, UserRequestActivity.class));
                finish();
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

                                startActivity(new Intent(DriverPositionActivity.this, MainActivity.class));

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

    /**
     * function to load map. If map is not created it will create it for you
     */
    private void initilizeMap() {
        if (googleMap == null) {
            googleMap = ((MapFragment) getFragmentManager().findFragmentById(
                    R.id.map)).getMap();

            googleMap.setMyLocationEnabled(true);

            LatLng latLng = new LatLng(gps.getLatitude(), gps.getLongitude());
            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 14);
            googleMap.animateCamera(cameraUpdate);

            googleMap.setOnMarkerClickListener(this);
            // check if map is created successfully or not
            if (googleMap == null) {
                Toast.makeText(getApplicationContext(),
                        "Sorry! unable to create maps", Toast.LENGTH_SHORT)
                        .show();
            }
        }
    }


    @Override
    public boolean onMarkerClick(Marker marker) {
        Toast.makeText(con, "Loading vehicle information..", Toast.LENGTH_SHORT).show();
        Driver dv = markers.get(marker);
        selectedDriver = dv;
        showDriverDetailsWindow(marker, dv.getId(), dv.getInfo(), dv.getCost(), dv.getNumber());
        return true;
    }

    public void showDriverDetailsWindow(Marker mark, String id, String info, String cost, String phone) {

        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        //dialog.setTitle("Request Driver");
        dialog.setContentView(R.layout.driver_details_layout);
        dialog.setCancelable(true);

        TextView vehicleInfo = (TextView) dialog.findViewById(R.id.driverVehicleInfo);
        TextView costkm = (TextView) dialog.findViewById(R.id.driverCostPerkm);
        TextView phn = (TextView) dialog.findViewById(R.id.driverPhone);
        TextView loc = (TextView) dialog.findViewById(R.id.driverCurrentLocation);
        //final EditText dropLoc=(EditText) dialog.findViewById(R.id.driverDropLoc);
        final LinearLayout reqLay = (LinearLayout) dialog.findViewById(R.id.driverSubmitLinLay);
        Button reqBtn = (Button) dialog.findViewById(R.id.driverReqBtn);
        Button cancelBtn = (Button) dialog.findViewById(R.id.driverCancelBtn);
        final RadioButton myloc = (RadioButton) dialog.findViewById(R.id.myenterloc);
        RadioButton gpsloc = (RadioButton) dialog.findViewById(R.id.mygpsloc);
        //final EditText pickme=(EditText) dialog.findViewById(R.id.mypickloc);

        autoAddress = (AutoCompleteTextView) dialog.findViewById(R.id.autoAddress);
        autoDestination = (AutoCompleteTextView) dialog.findViewById(R.id.autoDestination);

        autoAddress.setOnItemClickListener(mAutocompleteClickListener);
        autoAddress.setAdapter(mPlacesAdapter);
        autoDestination.setOnItemClickListener(mAutocompleteClickListener);
        autoDestination.setAdapter(mPlacesAdapter);

        myloc.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {

                //pickme.setVisibility(View.VISIBLE);
                autoAddress.setVisibility(View.VISIBLE);
            }
        });

        gpsloc.setOnClickListener(new OnClickListener() {

            public void onClick(View v) {

                //pickme.setVisibility(View.GONE);
                autoAddress.setVisibility(View.GONE);
            }

        });

        vehicleInfo.setText(info);
        costkm.setText("Rs " + cost);
        phn.setText(phone);

        Geocoder gcd = new Geocoder(con, Locale.getDefault());
        List<Address> addresses;
        try {
            addresses = gcd.getFromLocation(mark.getPosition().latitude, mark.getPosition().longitude, 1);
            if (addresses.size() > 0) {
                String strCompleteAddress = "";
                if (addresses.size() > 0) {
                    for (int i = 0; i < addresses.get(0).getMaxAddressLineIndex(); i++)
                        strCompleteAddress += addresses.get(0).getAddressLine(i) + "\n";
                    loc.setText(strCompleteAddress.trim());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        final Button selectDropLoc = (Button) dialog.findViewById(R.id.driverDropLocBtn);
        selectDropLoc.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                selectDropLoc.setVisibility(View.GONE);
                reqLay.setVisibility(View.VISIBLE);
                //dropLoc.setVisibility(View.VISIBLE);
                autoDestination.setVisibility(View.VISIBLE);
            }
        });

        reqBtn.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {

                double drop_latitude = 0, drop_longitude = 0;

                if (TextUtils.isEmpty(autoDestination.getText().toString())) {
                    Toast.makeText(con, "Please enter the drop location", Toast.LENGTH_SHORT).show();
                    return;
                }

                try {
                    int i = 0;
                    Geocoder geocoder = new Geocoder(DriverPositionActivity.this);
                    List<Address> list = geocoder.getFromLocationName(autoDestination.getText().toString(), 1);
                    // anticipate failure when geocoding, do until it gets a result, max 3x
                    while (i < 3 && list.size() < 1) {
                        list = geocoder.getFromLocationName(autoDestination.getText().toString(), 1);
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

                if (myloc.isChecked()) {
                    // enter pick up location

                    if (TextUtils.isEmpty(autoAddress.getText().toString())) {
                        Toast.makeText(con, "Please enter your pickup address", Toast.LENGTH_SHORT).show();
                        return;
                    } else {

                        try {
                            int i = 0;
                            String address = autoAddress.getText().toString();
                            Geocoder geocoder = new Geocoder(DriverPositionActivity.this);
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
                        } catch (Throwable e) {
                            //Log.d("VIP","geocoder exception");
                        }

                        //new RequestTaxi().execute(pickme.getText().toString(),dropLoc.getText().toString());
                        Intent intent = new Intent(DriverPositionActivity.this, BookNowUserActivity.class);
                        intent.putExtra("driver_id", selectedDriver.getId());
                        intent.putExtra("driver_email", selectedDriver.getEmail());
                        intent.putExtra("driver_name", selectedDriver.getName());
                        intent.putExtra("sender_id", UserInfo.getId());
                        intent.putExtra("name", UserInfo.getName());
                        intent.putExtra("phone", UserInfo.getPhonenumber());
                        intent.putExtra("location", autoAddress.getText().toString());
                        intent.putExtra("droplocation", autoDestination.getText().toString());
                        intent.putExtra("latitude", latitude);
                        intent.putExtra("longitude", longitude);
                        intent.putExtra("drop_latitude", drop_latitude);
                        intent.putExtra("drop_longitude", drop_longitude);
                        startActivity(intent);
                    }
                } else {
                    //use passenger location as pick up

                    Geocoder gcd = new Geocoder(con, Locale.getDefault());
                    List<Address> addresses;
                    gps = new GPSTracker(con);
                    try {
                        addresses = gcd.getFromLocation(gps.getLatitude(), gps.getLongitude(), 1);
                        if (addresses.size() > 0) {
                            String strCompleteAddress = "";
                            if (addresses.size() > 0) {
                                for (int i = 0; i < addresses.get(0).getMaxAddressLineIndex(); i++)
                                    strCompleteAddress += addresses.get(0).getAddressLine(i) + "\n";
                            }
                            //Toast.makeText(con, "dhk", Toast.LENGTH_LONG).show();
                            //new RequestTaxi().execute(strCompleteAddress,dropLoc.getText().toString());
                            Intent intent = new Intent(DriverPositionActivity.this, BookNowUserActivity.class);
                            intent.putExtra("driver_id", selectedDriver.getId());
                            intent.putExtra("driver_email", selectedDriver.getEmail());
                            intent.putExtra("driver_name", selectedDriver.getName());
                            intent.putExtra("sender_id", UserInfo.getId());
                            intent.putExtra("name", UserInfo.getName());
                            intent.putExtra("phone", UserInfo.getPhonenumber());
                            intent.putExtra("location", strCompleteAddress);
                            intent.putExtra("droplocation", autoDestination.getText().toString());
                            intent.putExtra("latitude", gps.getLatitude());
                            intent.putExtra("longitude", gps.getLongitude());
                            intent.putExtra("drop_latitude", drop_latitude);
                            intent.putExtra("drop_longitude", drop_longitude);
                            startActivity(intent);
                        }
                    } catch (IOException e) {
                        Util.showToast(con, e.getMessage());
                        //new RequestTaxi().execute("",dropLoc.getText().toString());
                        e.printStackTrace();
                    }
                }
                dialog.cancel();
            }
        });


        cancelBtn.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                dialog.cancel();
            }
        });

        dialog.show();
    }

    class GetLocations {

        //ProgressDialog pDialog;
        String toastText = "Internet Problem";
        String regiresult = "";
        int success = 0;
        int error = 0;
        String errmsg = "Server is down";

        public void execute() {
            List<NameValuePair> params = new ArrayList<NameValuePair>();

            jparser.makeHttpRequest(Globals.getDataUrl, "POST", params, new ServerCallback() {
                @Override
                public void onSuccess(JSONObject jsonObject) {
                    try {
                        success = jsonObject.getInt("success");

                        if (success == 1) {

                            drivers = new ArrayList<Driver>();

                            JSONArray sounds = jsonObject.getJSONArray("location");
                            for (int i = 0; i < sounds.length(); i++) {
                                JSONObject jobj = sounds.getJSONObject(i);
                                Driver d = new Driver();
                                d.setId(jobj.getString("id"));
                                d.setName(jobj.getString("name"));
                                d.setEmail(jobj.getString("email"));
                                d.setNumber(jobj.getString("number"));
                                d.setLatitude(jobj.getString("latitude"));
                                d.setLongitude(jobj.getString("longitude"));
                                d.setInfo(jobj.getString("info"));
                                d.setCost(jobj.getString("cost"));
                                drivers.add(d);
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        error = 1;
                    } catch (Exception e) {
                        error = 1;
                    }

                    if (error == 1) {
                        if (Util.isConnectingToInternet(con)) {
                            Toast.makeText(con, "Server is down. Please try again", Toast.LENGTH_SHORT).show();
                        } else {
                            Util.showNoInternetDialog(con);
                        }
                        return;
                    }

                    if (success == 0) {
                        Toast.makeText(con, "Data loading failed", Toast.LENGTH_SHORT).show();
                    } else if (success == 1) {
                        markers = new HashMap<Marker, Driver>();
                        removeMarkers();

                        for (int i = 0; i < drivers.size(); i++) {
                            MarkerOptions mark = new MarkerOptions();
                            mark.position(new LatLng(Double.parseDouble(drivers.get(i).getLatitude()), Double.parseDouble(drivers.get(i).getLongitude())));
                            //mark.title(drivers.get(i).getName());
                            mark.icon(BitmapDescriptorFactory.fromResource(R.drawable.taxi));
                            //mark.snippet(drivers.get(i).getInfo()+","+drivers.get(i).getCost()+" Rs.pKm, Ph. "+drivers.get(i).getNumber());
                            Marker m = googleMap.addMarker(mark);
                            mapMarker.add(m);
                            markers.put(m, drivers.get(i));

                            if (drivers.get(i).getEmail().equals(driverEmail)) {

                                Location my = new Location("My");

                                my.setLatitude(pickupLatitude);
                                my.setLongitude(pickupLongitude);

                                Location markLoc = new Location("Driver");
                                markLoc.setLatitude(Double.valueOf(drivers.get(i).getLatitude()));
                                markLoc.setLongitude(Double.valueOf(drivers.get(i).getLongitude()));
                                distance = my.distanceTo(markLoc);

                                if (distance < 2000) {
                                    long now = System.currentTimeMillis();
                                    if (now - lastnotifytime > Long.valueOf("60000") || (now - lastnotifytime < 0)) {
                                        showNotification();
                                        lastnotifytime = System.currentTimeMillis();
                                    }
                                }
                            }
                        }
                        scheduleThread();
                    }
                }
            });
        }
    }

    private void removeMarkers() {
        for (Marker m : mapMarker) {
            m.remove();
        }
        mapMarker.clear();
    }

    public void scheduleThread() {
        handler = new Handler();
        run = new Runnable() {

            @Override
            public void run() {
                // This method will be executed once the timer is over
                if (Util.isConnectingToInternet(con)) {
                    new GetRequestedRides().execute();

                } else {
                    Toast.makeText(con, "Internet is not active", Toast.LENGTH_SHORT).show();
                }
            }
        };
        handler.postDelayed(run, 30000);
    }

	/*
    class RequestTaxi extends AsyncTask<String, String, String>{

		ProgressDialog pDialog;
		String toastText="Internet Problem";
		String regiresult="";
		int success=0;
		int error=0;
		String msg="";

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			pDialog=new ProgressDialog(con);
			pDialog.setMessage("Requesting for Taxi. Please wait...");
			pDialog.setIndeterminate(false);
			pDialog.setCancelable(true);
			pDialog.show();
		}

		@Override
		protected String doInBackground(String... para) {

			Calendar cal=Calendar.getInstance();
			String datetime=cal.get(Calendar.YEAR)+"-"+(cal.get(Calendar.MONTH)+1)+"-"+cal.get(Calendar.DAY_OF_MONTH)+" "+
					cal.get(Calendar.HOUR_OF_DAY)+":"+cal.get(Calendar.MINUTE)+":"+cal.get(Calendar.SECOND);

			List<NameValuePair> params=new ArrayList<NameValuePair>();
			params.add(new BasicNameValuePair("driver_id", selectedDriver.getId()));
			params.add(new BasicNameValuePair("driver_email", selectedDriver.getEmail()));
			params.add(new BasicNameValuePair("driver_name", selectedDriver.getName()));
			params.add(new BasicNameValuePair("sender_id", UserInfo.getId()));
			params.add(new BasicNameValuePair("name", UserInfo.getName()));
			params.add(new BasicNameValuePair("phone", UserInfo.getPhonenumber()));
			params.add(new BasicNameValuePair("location", para[0]));
			params.add(new BasicNameValuePair("droplocation", para[1]));
			params.add(new BasicNameValuePair("latitude", gps.getLatitude()+""));
			params.add(new BasicNameValuePair("longitude", gps.getLongitude()+""));
			//params.add(new BasicNameValuePair("accept","0"));
			params.add(new BasicNameValuePair("timedate",datetime));
			//params.add(new BasicNameValuePair("num_passengers","1"));
			//params.add(new BasicNameValuePair("booking_time",datetime));
			//params.add(new BasicNameValuePair("types",""));

			JSONObject json=jparser.makeHttpRequest(Globals.taxiRequUrl, "POST", params);

			try {
				success=json.getInt("success");
				msg=json.getString("message");

			}catch (Exception e) {
				error=1;
				msg=e.getMessage();
			}
			return null;
		}

		@Override
		protected void onPostExecute(String result) {
			super.onPostExecute(result);
			//Toast.makeText(MainActivity.this, s+" "+result, Toast.LENGTH_SHORT).show();
			pDialog.dismiss();
			Toast.makeText(con, msg, Toast.LENGTH_SHORT).show();

			if(error==1){
				if(Util.isConnectingToInternet(con)){
					Toast.makeText(con,"Server is down. Please try again", Toast.LENGTH_SHORT).show();
				}else{
					Util.showNoInternetDialog(con);
				}
				return;
			}
		}
	}
	*/

    @Override
    protected void onPause() {
        super.onPause();
        if (handler != null) {
            handler.removeCallbacks(run);
            run = null;
            handler = null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        initilizeMap();
        if (handler == null) {
            scheduleThread();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        alIntent = new Intent(this, UpdateReceiver.class);
        appIntent = PendingIntent.getBroadcast(this, 1000, alIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        alarmManager.cancel(appIntent);
    }

    class GetRequestedRides {
        ProgressDialog pDialog;
        String s = "";
        int success = -1;
        int error = 0;

        public void execute() {

            Calendar cal = Calendar.getInstance();
            String datetime = cal.get(Calendar.YEAR) + "-" + (cal.get(Calendar.MONTH) + 1) + "-" + cal.get(Calendar.DAY_OF_MONTH) + " " +
                    cal.get(Calendar.HOUR_OF_DAY) + ":" + cal.get(Calendar.MINUTE) + ":" + cal.get(Calendar.SECOND);

            List<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair("user_id", UserInfo.getId()));
            //params.add(new BasicNameValuePair("datetime", datetime));

            jparser.makeHttpRequest(Globals.getUserRidesUrl, "POST", params, new ServerCallback() {

                @Override
                public void onSuccess(JSONObject jsonObject) {
                    try {
                        success = jsonObject.getInt("success");
                        s = jsonObject.getString("message");
                        if (success == 1) {
                            DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                            DateFormat df2 = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
                            Date startDate;

                            rides = new ArrayList<HashMap<String, String>>();
                            JSONArray jAr = jsonObject.getJSONArray("ridelist");
                            for (int i = 0; i < jAr.length(); i++) {
                                JSONObject job = jAr.getJSONObject(i);
                                HashMap<String, String> map = new HashMap<String, String>();
                                map.put("id", job.getString("id"));
                                map.put("driver_name", job.getString("driver_name"));
                                map.put("driver_email", job.getString("driver_email"));
                                map.put("sender_id", job.getString("sender_id"));
                                map.put("name", job.getString("name"));
                                map.put("phone", job.getString("phone"));
                                map.put("droplocation", job.getString("droplocation"));
                                map.put("location", job.getString("location"));
                                map.put("latitude", job.getString("latitude"));
                                map.put("longitude", job.getString("longitude"));
                                startDate = df.parse(job.getString("timedate"));
                                map.put("timedate", df2.format(startDate));
                                map.put("accept", job.getString("accept"));
                                startDate = df.parse(job.getString("booking_time"));
                                map.put("booking_time", df2.format(startDate));
                                rides.add(map);
                            }
                        }
                    } catch (Exception e) {
                        error = 1;
                    }

                    if (error == 1) {
                        if (Util.isConnectingToInternet(con)) {
                            Toast.makeText(con, "Server is down, Please try again later", Toast.LENGTH_SHORT).show();
                        } else
                            Util.showNoInternetDialog(con);
                        return;
                    }

                    if (success == 1) {
                        for (int i = 0; i < rides.size(); i++) {
                            if (rides.get(i).get("accept").equals("1") && !rides.get(i).get("latitude").equals("") && !rides.get(i).get("longitude").equals("")) {
                                driverEmail = rides.get(i).get("driver_email");
                                pickupLatitude = Double.valueOf(rides.get(i).get("latitude"));
                                pickupLongitude = Double.valueOf(rides.get(i).get("longitude"));

                                new GetLocations().execute();

                            }
                        }

                    } else {
                        Toast.makeText(con, s, Toast.LENGTH_SHORT).show();
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
