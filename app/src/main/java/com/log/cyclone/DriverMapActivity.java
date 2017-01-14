package com.log.cyclone;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognizerIntent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.log.cyclone.General.DirectionsJSONParser;
import com.log.cyclone.General.Driver;
import com.log.cyclone.General.Globals;
import com.log.cyclone.util.GPSTracker;
import com.log.cyclone.util.JSONParser;
import com.log.cyclone.util.ServerCallback;
import com.log.cyclone.util.Util;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class DriverMapActivity extends Activity implements OnMarkerClickListener {

    // Google Map
    private GoogleMap googleMap;

    Context con;
String lat,longi,tarlat,tarlong;
    ArrayList<HashMap<String, String>> rides;
    ArrayList<Driver> drivers;
    JSONParser jparser = new JSONParser();
    GPSTracker gps;

    HashMap<Marker, Driver> markers;
    ArrayList<Marker> mapMarker;

    Handler handler;
    Runnable run;


    Driver selectedDriver;
    Button spchBtn,plot;
    private final int REQ_CODE_SPEECH_INPUT = 100;

    Intent alIntent;
    AlarmManager alarmManager;
    PendingIntent appIntent;

    SharedPreferences sh;

    boolean showdirection = false;
    double latitude = 0;
    double longitude = 0;
    double drop_latitude = 0;
    double drop_longitude = 0;

    Handler handlerUpdater;
    Runnable runUpdater;

    boolean check = true;
    private Menu menu;

    String driver_id, driver_email, driver_name;
    int group_id;

    LatLng pickup, dest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.driver_position_layout);
        lat=null;longi=null;tarlat=null;tarlong=null;
       /* Intent intent = getIntent();
        Bundle bd = intent.getExtras();
        if (bd != null) {
            latitude = (double) bd.get("lat");
            longitude = (double) bd.get("long");
            drop_latitude = (double) bd.get("drop_lat");
            drop_longitude = (double) bd.get("drop_long");
            showdirection = (boolean) bd.get("showdirection");
        }*/

        sh = getSharedPreferences("CYCLONE_PREF", MODE_PRIVATE);
        driver_id = sh.getString("id", "");
        driver_email = sh.getString("loginemail", "");
        driver_name = sh.getString("name", "");
        group_id = sh.getInt("group_id", 0);

        drivers = new ArrayList<Driver>();
        gps = new GPSTracker(this);
        con = DriverMapActivity.this;
        spchBtn = (Button) findViewById(R.id.speechBtn);
        plot=(Button)findViewById(R.id.plot);
        spchBtn.setVisibility(View.GONE);

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

        plot.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if(tarlat==null||tarlong==null||lat==null||longi==null){
                    Toast.makeText(con, "Please accept a ride first", Toast.LENGTH_LONG).show();
                }
                else{
                    hello(lat,longi,tarlat,tarlong);
                }


            }
        });

        new DriverMapActivity.GetLocations().execute();

        new GetRequestedRides().execute();

        startUpdateCheck();

        new SendModeData().execute("1");
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

    void hello(String currentLatitude,String currentLongitude,String targetLat,String targetLong){
        try {
            String url = "http://maps.google.com/maps?saddr=" + currentLatitude + "," + currentLongitude + "&daddr=" + targetLat + "," + targetLong + "&mode=driving";
            Intent intent = new Intent(android.content.Intent.ACTION_VIEW, Uri.parse(url));
            intent.setClassName("com.google.android.apps.maps", "com.google.android.maps.MapsActivity");
            startActivity(intent);
        }
        catch (ActivityNotFoundException e) {

            try {
                con.startActivity(new Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("market://details?id=com.google.android.apps.maps")));
            } catch (android.content.ActivityNotFoundException anfe) {
                con.startActivity(new Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("http://play.google.com/store/apps/details?id=com.google.android.apps.maps")));
            }

            e.printStackTrace();
        }
    }


    public void startUpdateCheck() {
        handlerUpdater = new Handler();
        runUpdater = new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(DriverMapActivity.this, UpdateReceiver.class);
                sendBroadcast(intent);
                handlerUpdater.postDelayed(runUpdater, 5000);
            }
        };
        handlerUpdater.postDelayed(runUpdater, 1000);
        /*
        alIntent= new Intent(this, UpdateReceiver.class);
		appIntent = PendingIntent.getBroadcast(this,1000, alIntent,PendingIntent.FLAG_UPDATE_CURRENT);
		alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

		// 1 min in millisecond= 1*60*1000;
		alarmManager.setRepeating(AlarmManager.RTC,System.currentTimeMillis(),30*1000, appIntent);
		*/
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.menu = menu;
        getMenuInflater().inflate(R.menu.driver_menu, menu);
        menu.removeItem(R.id.driverMap);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        SharedPreferences.Editor edit;
        AlertDialog.Builder build;
        AlertDialog alert;

        switch (item.getItemId()) {
            case R.id.driverChat:
                //new DriverActivity.GetRequestedRides().execute();
                Intent i = new Intent(this, ChatActivity.class);
                startActivity(i);
                finish();
                break;
            case R.id.bookStatus:
                startActivity(new Intent(this, BookNowDriverActivity.class));
                break;
            case R.id.driverPooledJobListMenu:
                startActivity(new Intent(this, DriverActivity.class));
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
            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 15);
            googleMap.animateCamera(cameraUpdate);

            googleMap.setOnMarkerClickListener(this);

            // check if map is created successfully or not
            if (googleMap == null) {
                Toast.makeText(getApplicationContext(),
                        "Sorry! unable to create maps", Toast.LENGTH_SHORT)
                        .show();
            }

            if (showdirection) {
                pickup = new LatLng(latitude, longitude);
                dest = new LatLng(drop_latitude, drop_longitude);

                String url = getDirectionsUrl(latLng, pickup, dest);

                DownloadTask downloadTask = new DownloadTask();

                // Start downloading json data from Google Directions API
                downloadTask.execute(url);
            }
        }
    }


    @Override
    public boolean onMarkerClick(Marker marker) {

        return true;
    }

    class GetLocations {

        //ProgressDialog pDialog;
        String toastText = "Internet Problem";
        String regiresult = "";
        int success = 0;
        int error = 0;
        String errmsg = "Server is down";

        public void execute(String... para) {

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
                            if (!drivers.get(i).getLatitude().equals("") && !drivers.get(i).getLongitude().equals("")) {
                                MarkerOptions mark = new MarkerOptions();
                                mark.position(new LatLng(Double.parseDouble(drivers.get(i).getLatitude()), Double.parseDouble(drivers.get(i).getLongitude())));
                                //mark.title(drivers.get(i).getName());
                                mark.icon(BitmapDescriptorFactory.fromResource(R.drawable.taxi));
                                //mark.snippet(drivers.get(i).getInfo()+","+drivers.get(i).getCost()+" Rs.pKm, Ph. "+drivers.get(i).getNumber());
                                Marker m = googleMap.addMarker(mark);
                                mapMarker.add(m);
                                markers.put(m, drivers.get(i));
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
                    gps = new GPSTracker(con);
                    new DriverMapActivity.GetLocations().execute();

                    new SendLocation().execute();
                    //Toast.makeText(con, gps.getLatitude()+" "+gps.getLongitude(), Toast.LENGTH_SHORT).show();

                } else {
                    Toast.makeText(con, "Internet is not active", Toast.LENGTH_SHORT).show();
                }
            }
        };
        handler.postDelayed(run, 30000);
    }

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

    private String getDirectionsUrl(LatLng origin, LatLng pickup, LatLng dest) {

        // Origin of route
        String str_origin = "origin=" + origin.latitude + "," + origin.longitude;

        // Destination of route
        String str_dest = "destination=" + dest.latitude + "," + dest.longitude;

        // Waypoint of route
        String str_waypoints = "waypoints=" + pickup.latitude + "," + pickup.longitude;

        // Sensor enabled
        String sensor = "sensor=false";

        // Building the parameters to the web service
        String parameters = str_origin + "&" + str_dest + "&" + str_waypoints + "&" + sensor;

        // Output format
        String output = "json";

        // Building the url to the web service
        String url = "https://maps.googleapis.com/maps/api/directions/" + output + "?" + parameters;

        return url;
    }

    /**
     * A method to download json data from url
     */
    private String downloadUrl(String strUrl) throws IOException {
        String data = "";
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(strUrl);

            // Creating an http connection to communicate with url
            urlConnection = (HttpURLConnection) url.openConnection();

            // Connecting to url
            urlConnection.connect();

            // Reading data from url
            iStream = urlConnection.getInputStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(iStream));

            StringBuffer sb = new StringBuffer();

            String line = "";
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

            data = sb.toString();

            br.close();

        } catch (Exception e) {
            //Log.d("Exception while downloading url", e.toString());
        } finally {
            iStream.close();
            urlConnection.disconnect();
        }
        return data;
    }

    // Fetches data from url passed
    private class DownloadTask {

        public void execute(String... url) {

            // For storing data from web service
            String data = "";

            try {
                // Fetching the data from web service
                data = downloadUrl(url[0]);
            } catch (Exception e) {
                //Log.d("Background Task",e.toString());
            }

            ParserTask parserTask = new ParserTask();

            // Invokes the thread for parsing the JSON data
            parserTask.execute(data);
        }
    }

    /**
     * A class to parse the Google Places in JSON format
     */
    private class ParserTask {

        public void execute(String... jsonData) {

            JSONObject jObject;
            List<List<HashMap<String, String>>> routes = null;

            try {
                jObject = new JSONObject(jsonData[0]);
                DirectionsJSONParser parser = new DirectionsJSONParser();

                // Starts parsing data
                routes = parser.parse(jObject);
            } catch (Exception e) {
                e.printStackTrace();
            }

            ArrayList<LatLng> points = null;
            PolylineOptions lineOptions = null;
            MarkerOptions markerOptions = new MarkerOptions();

            if (routes != null) {
                // Traversing through all the routes
                for (int i = 0; i < routes.size(); i++) {
                    points = new ArrayList<LatLng>();
                    lineOptions = new PolylineOptions();

                    // Fetching i-th route
                    List<HashMap<String, String>> path = routes.get(i);

                    // Fetching all the points in i-th route
                    for (int j = 0; j < path.size(); j++) {
                        HashMap<String, String> point = path.get(j);

                        double lat = Double.parseDouble(point.get("lat"));
                        double lng = Double.parseDouble(point.get("lng"));
                        LatLng position = new LatLng(lat, lng);

                        points.add(position);
                    }

                    // Adding all the points in the route to LineOptions
                    lineOptions.addAll(points);
                    lineOptions.width(10);
                    lineOptions.color(Color.BLUE);
                }

                if (routes.size() > 0) {
                    // Drawing polyline in the Google Map for the i-th route
                    googleMap.addPolyline(lineOptions);
                }
            }
        }
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
            params.add(new BasicNameValuePair("user_email", driver_email));
            params.add(new BasicNameValuePair("group_id", Integer.toString(group_id)));
            params.add(new BasicNameValuePair("datetime", datetime));

            jparser.makeHttpRequest(Globals.getRidesUrl, "POST", params, new ServerCallback() {

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
                                map.put("driver_id", job.getString("driver_id"));
                                map.put("sender_id", job.getString("sender_id"));
                                map.put("name", job.getString("name"));
                                map.put("phone", job.getString("phone"));
                                map.put("droplocation", job.getString("droplocation"));
                                map.put("location", job.getString("location"));
                                map.put("latitude", job.getString("latitude"));
                                map.put("longitude", job.getString("longitude"));
                                //map.put("timedate", job.getString("timedate"));
                                startDate = df.parse(job.getString("timedate"));
                                map.put("timedate", df2.format(startDate));
                                map.put("accept", job.getString("accept"));
                                map.put("group_id", job.getString("group_id"));
                                startDate = df.parse(job.getString("booking_time"));
                                map.put("booking_time", df2.format(startDate));
                                //"All","New Requests","Accepted Rides","Completed Rides","Cancelled Rides"
                                map.put("drop_latitude", job.getString("drop_latitude"));
                                map.put("drop_longitude", job.getString("drop_longitude"));
                                if (job.getString("accept").equals("1")){
                                  tarlat=  job.getString("drop_latitude");
                                    tarlong=job.getString("drop_longitude");
                                    lat=job.getString("latitude");
                                    longi=job.getString("longitude");
                                }

                                rides.add(map);
                            }
                        }
                    } catch (JSONException e) {
                        error = 1;
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
                    //notAccReq=sh.getInt("notaccreq", 0);

                    if (success == 1) {
                        for (int i = 0; i < rides.size(); i++) {
                            if (rides.get(i).get("driver_id").equals(driver_id) && rides.get(i).get("accept").equals("1")) {
                                if (!rides.get(i).get("latitude").equals("") && !rides.get(i).get("longitude").equals("")) {
                                    latitude = Double.valueOf(rides.get(i).get("latitude"));
                                    longitude = Double.valueOf(rides.get(i).get("longitude"));

                                    drop_latitude = Double.valueOf(rides.get(i).get("drop_latitude"));
                                    drop_longitude = Double.valueOf(rides.get(i).get("drop_longitude"));

                                    LatLng latLng = new LatLng(gps.getLatitude(), gps.getLongitude());
                                    pickup = new LatLng(latitude, longitude);
                                    dest = new LatLng(drop_latitude, drop_longitude);

                                    String url = getDirectionsUrl(latLng, pickup, dest);

                                    DownloadTask downloadTask = new DownloadTask();

                                    // Start downloading json data from Google Directions API
                                    downloadTask.execute(url);
                                }

                            }
                        }
                    } else {
                        //Toast.makeText(DriverMapActivity.this, s, Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
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
                    } catch (JSONException e) {
                        error = 1;
                    }

                    if (error == 1) {
                        if (Util.isConnectingToInternet(con)) {
                            Toast.makeText(con, "Server is down, Please try again later", Toast.LENGTH_SHORT).show();
                        } else
                            Util.showNoInternetDialog(con);
                        return;
                    }

                    //Toast.makeText(con, s, Toast.LENGTH_LONG).show();
                    if (success == 0) {

                    } else if (success == 1) {
                        scheduleThread();
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
        }

        public void execute(String... st) {
            if (st[0] == "-1") {
                isLogout = true;
                st[0] = "0";
            }

            List<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair("email", driver_email));
            params.add(new BasicNameValuePair("mode", st[0]));

            jparser.makeHttpRequest(Globals.modeSendUrl, "POST", params, new ServerCallback() {
                @Override
                public void onSuccess(JSONObject jsonObject) {
                    try {
                        success = jsonObject.getInt("success");
                        s = jsonObject.getString("message");
                    } catch (JSONException e) {
                        error = 1;
                    }

                    pDialog.dismiss();

                    //Toast.makeText(con, "On execute", Toast.LENGTH_LONG).show();

                    if (error == 1) {
                        if (Util.isConnectingToInternet(con)) {
                            Toast.makeText(con, "Server is down, Please try again later", Toast.LENGTH_SHORT).show();
                        } else
                            Util.showNoInternetDialog(con);
                        return;
                    } else {
                        //Toast.makeText(con, s, Toast.LENGTH_LONG).show();

                        if (isLogout) {
                            SharedPreferences.Editor edit = sh.edit();
                            edit.putString("loginemail", null);
                            edit.putString("loginpass", null);
                            edit.putBoolean("type", false);
                            edit.putInt("group_id", 0);
                            edit.commit();

                            startActivity(new Intent(DriverMapActivity.this, MainActivity.class));
                            finish();
                        }
                    }
                }
            });
        }
    }

}
