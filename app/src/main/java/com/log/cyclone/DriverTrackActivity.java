package com.log.cyclone;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.log.cyclone.General.Driver;
import com.log.cyclone.General.Globals;
import com.log.cyclone.util.JSONParser;
import com.log.cyclone.util.ServerCallback;
import com.log.cyclone.util.Util;

import org.apache.http.NameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DriverTrackActivity extends Activity {

    JSONParser jparser = new JSONParser();
    public static String driver_email = "";
    ArrayList<Driver> drivers;

    Handler handler;
    Runnable run;

    HashMap<Marker, Driver> markers;
    ArrayList<Marker> mapMarker;

    // Google Map
    private GoogleMap googleMap;
    Context con;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.driver_position_layout);

        con = DriverTrackActivity.this;
        driver_email = getIntent().getStringExtra("driver_email");
        markers = new HashMap<Marker, Driver>();
        mapMarker = new ArrayList<Marker>();
        try {
            // Loading map
            initilizeMap();

        } catch (Exception e) {
            e.printStackTrace();
        }
        getActionBar().setHomeButtonEnabled(true);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        new GetLocations().execute();
    }

    private void initilizeMap() {
        if (googleMap == null) {
            googleMap = ((MapFragment) getFragmentManager().findFragmentById(
                    R.id.map)).getMap();

            googleMap.setMyLocationEnabled(true);


            // check if map is created successfully or not
            if (googleMap == null) {
                Toast.makeText(getApplicationContext(),
                        "Sorry! unable to create maps", Toast.LENGTH_SHORT)
                        .show();
            }
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

    class GetLocations {

        ProgressDialog pDialog;
        String toastText = "Internet Problem";
        String regiresult = "";
        int success = 0;
        int error = 0;
        String errmsg = "Server is down";

        public GetLocations() {
            pDialog = new ProgressDialog(con);
            pDialog.setMessage("Updating drivers locations. Please wait...");
            pDialog.setIndeterminate(false);
            pDialog.setCancelable(true);
            pDialog.show();
        }

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

                    pDialog.dismiss();

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
                            if (drivers.get(i).getEmail().equals(driver_email)) {
                                MarkerOptions mark = new MarkerOptions();
                                mark.position(new LatLng(Double.parseDouble(drivers.get(i).getLatitude()), Double.parseDouble(drivers.get(i).getLongitude())));
                                //mark.title(drivers.get(i).getName());
                                mark.icon(BitmapDescriptorFactory.fromResource(R.drawable.taxi));
                                Marker m = googleMap.addMarker(mark);
                                mapMarker.add(m);
                                markers.put(m, drivers.get(i));

                                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(Double.parseDouble(drivers.get(i).getLatitude()), Double.parseDouble(drivers.get(i).getLongitude())),
                                        16));
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
                    new GetLocations().execute();
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
}
