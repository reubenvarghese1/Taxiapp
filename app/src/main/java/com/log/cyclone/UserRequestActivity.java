package com.log.cyclone;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.log.cyclone.General.Globals;
import com.log.cyclone.General.UserInfo;
import com.log.cyclone.util.JSONParser;
import com.log.cyclone.util.ServerCallback;
import com.log.cyclone.util.Util;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class UserRequestActivity extends Activity {

    ListView requestList;
    Context con;
    JSONParser jparser = new JSONParser();
    ArrayList<HashMap<String, String>> rides;
    Spinner categoryFilter;
    String dropdownItems[] = {"All", "Pending Requests", "Accepted Requests", "Completed Rides", "Cancelled","Accept Rate"};
    SharedPreferences sh;

    private Menu menu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.user_request_layout);

        con = UserRequestActivity.this;

        sh = getSharedPreferences("CYCLONE_PREF", MODE_PRIVATE);

        categoryFilter = (Spinner) findViewById(R.id.userRequestListFilter);
        categoryFilter.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line, dropdownItems));

        requestList = (ListView) findViewById(R.id.userRequestList);
        new GetRequestedRides().execute();

        requestList.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int i,
                                    long arg3) {
                UserRequestDetailsActivity.request = rides.get(i);
                startActivityForResult(new Intent(con, UserRequestDetailsActivity.class), 101);
            }
        });


        categoryFilter.setOnItemSelectedListener(new OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1,
                                       int i, long arg3) {
                new GetRequestedRides().execute();
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {

            }
        });

        getActionBar().setHomeButtonEnabled(true);
        getActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.menu = menu;
        getMenuInflater().inflate(R.menu.usermenu, menu);
        menu.removeItem(R.id.userRidesMenu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.userProfileMenu:
                startActivity(new Intent(this, UserEditProfileActivity.class));
                break;
            case R.id.userbookmenu:
                startActivity(new Intent(this, BookNowUserActivity.class));
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

                                startActivity(new Intent(UserRequestActivity.this, MainActivity.class));

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

    class GetRequestedRides {
        ProgressDialog pDialog;
        String s = "";
        int success = -1;
        int error = 0;

        public GetRequestedRides() {
            pDialog = new ProgressDialog(con);
            pDialog.setMessage("Getting data. Please wait...");
            pDialog.setIndeterminate(false);
            pDialog.setCancelable(true);
        }

        public void execute() {
            Calendar cal = Calendar.getInstance();
            String datetime = cal.get(Calendar.YEAR) + "-" + (cal.get(Calendar.MONTH) + 1) + "-" + cal.get(Calendar.DAY_OF_MONTH) + " " +
                    cal.get(Calendar.HOUR_OF_DAY) + ":" + cal.get(Calendar.MINUTE) + ":" + cal.get(Calendar.SECOND);

            List<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair("user_id", UserInfo.getId()));
            params.add(new BasicNameValuePair("datetime", datetime));

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
                                map.put("rate", job.getString("rate"));
                                map.put("driver_id", job.getString("driver_id"));
                                map.put("sender_id", job.getString("sender_id"));
                                map.put("name", job.getString("name"));
                                map.put("phone", job.getString("phone"));
                                map.put("droplocation", job.getString("droplocation"));
                                map.put("location", job.getString("location"));
                                map.put("latitude", job.getString("latitude"));
                                map.put("group_id", job.getString("group_id"));
                                map.put("longitude", job.getString("longitude"));
                                startDate = df.parse(job.getString("timedate"));
                                map.put("timedate", df2.format(startDate));
                                map.put("accept", job.getString("accept"));
                                startDate = df.parse(job.getString("booking_time"));
                                map.put("booking_time", df2.format(startDate));

                                if (categoryFilter.getSelectedItemPosition() == 0)
                                    rides.add(map);
                                else if (categoryFilter.getSelectedItemPosition() == 1) {
                                    if (job.getString("accept").equals("0"))
                                        rides.add(map);
                                } else if (categoryFilter.getSelectedItemPosition() == 2) {
                                    if (job.getString("accept").equals("1"))
                                        rides.add(map);
                                } else if (categoryFilter.getSelectedItemPosition() == 3) {
                                    if (job.getString("accept").equals("2"))
                                        rides.add(map);
                                } else if (categoryFilter.getSelectedItemPosition() == 4) {
                                    if (job.getString("accept").equals("3"))
                                        rides.add(map);
                                }
                                else if (categoryFilter.getSelectedItemPosition() == 5) {
                                    if (job.getString("accept").equals("4"))
                                        rides.add(map);
                                }
                            }
                        }

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

                    if (success == 1) {
                        ListAdapter adapter = new ListAdapter();
                        requestList.setAdapter(adapter);
                    } else {
                        Toast.makeText(con, s, Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    class ListAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return rides.size();
        }

        @Override
        public Object getItem(int i) {
            return rides.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup parent) {
            LayoutInflater inflater = getLayoutInflater();
            View rowView = inflater.inflate(R.layout.request_ride_single_view, parent, false);
            TextView name = (TextView) rowView.findViewById(R.id.driverNameTv);
            TextView from = (TextView) rowView.findViewById(R.id.driverFromTv);
            TextView destination = (TextView) rowView.findViewById(R.id.driverDestinationTv);
            TextView timedate = (TextView) rowView.findViewById(R.id.drivertimeDate);
            TextView accept = (TextView) rowView.findViewById(R.id.driverAccept);
            TextView driverName = (TextView) rowView.findViewById(R.id.driverName);

            name.setText("Driver Name: " + rides.get(i).get("driver_name").trim());
            from.setText("Pickup Location : " + rides.get(i).get("location").trim());
            timedate.setText("Booking : " + rides.get(i).get("booking_time"));
            destination.setText("Drop Location: " + rides.get(i).get("droplocation").trim());
            driverName.setText("Driver : " + rides.get(i).get("driver_name").trim());

            if (rides.get(i).get("accept").equals("0")) {
                accept.setText("Not accepted yet");
                accept.setTextColor(Color.parseColor("#AD1400"));
            } else if (rides.get(i).get("accept").equals("1")) {
                accept.setText("Accepted");
                accept.setTextColor(Color.parseColor("#5AB83B"));
            } else if (rides.get(i).get("accept").equals("2")) {
                accept.setText("Completed");
            } else if (rides.get(i).get("accept").equals("3")) {
                accept.setText("Cancelled");
            }else if (rides.get(i).get("accept").equals("4")) {
                accept.setText("Rate Approve");
            }


            return rowView;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 101 && resultCode == RESULT_OK)
            new GetRequestedRides().execute();
    }
}
