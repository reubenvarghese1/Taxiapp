package com.log.cyclone;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.NotificationCompat;
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
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class DriverActivity extends Activity {
    final Context context = this;
    Context con;
    JSONParser jparser = new JSONParser();
    GPSTracker gps;
    Handler handler;
    Runnable run;
    ListView rideList;
    ToggleButton modeBtn;

    ArrayList<HashMap<String, String>> rides;
    boolean check = true;

    SharedPreferences sh;
    int notAccReq = 0;
    private Menu menu;
    Spinner categoryFilter;
    String dropdownItems[] = {"All", "New Requests", "Accepted Rides", "Completed Rides", "Cancelled Rides"};
    String driver_id, driver_email, driver_name;
    int group_id;

    String latitude, longitude, drop_latitude, drop_longitude;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActionBar().setDisplayShowTitleEnabled(false);
        setContentView(R.layout.driver_activity_layout);
        gps = new GPSTracker(this);
        con = DriverActivity.this;

        rideList = (ListView) findViewById(R.id.driverRequestedRides);
        modeBtn = (ToggleButton) findViewById(R.id.modeToggleBtn);
        categoryFilter = (Spinner) findViewById(R.id.driverRequestedRidesListFilter);
        categoryFilter.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line, dropdownItems));

        modeBtn.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton arg0, boolean check) {
                if (check) {
                    new SendModeData().execute("1");
                } else new SendModeData().execute("0");
            }
        });
        modeBtn.setChecked(true);

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

        sh = getSharedPreferences("CYCLONE_PREF", MODE_PRIVATE);
        notAccReq = sh.getInt("notaccreq", 0);
        driver_id = sh.getString("id", "");
        driver_email = sh.getString("loginemail", "");
        driver_name = sh.getString("name", "");
        group_id = sh.getInt("group_id", 0);

        new GetRequestedRides().execute();
        new SendLocation().execute();

        rideList.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int i,
                                    long arg3) {
                RequestedUserActivity.request = rides.get(i);
                Intent in = new Intent(DriverActivity.this, RequestedUserActivity.class);
                startActivityForResult(in, 101);
            }
        });

        if (!Util.isGPSOn(this)) {
            GPSTracker.showSettingsAlert(this);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.menu = menu;
        getMenuInflater().inflate(R.menu.driver_menu, menu);
        menu.removeItem(R.id.driverPooledJobListMenu);
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
                Intent i = new Intent(DriverActivity.this, ChatActivity.class);
                startActivity(i);
                finish();
                break;
            case R.id.bookStatus:
                Intent i1 = new Intent(DriverActivity.this, BookNowDriverActivity.class);
                startActivity(i1);
                new GetRequestedRides().execute();
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
                                map.put("drop_latitude", job.getString("drop_latitude"));
                                map.put("drop_longitude", job.getString("drop_longitude"));
                                map.put("rate", job.getString("rate"));
                                map.put("position", job.getString("position"));
                                //"All","New Requests","Accepted Rides","Completed Rides","Cancelled Rides"
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
                                } else if (categoryFilter.getSelectedItemPosition() == 5) {
                                    if (job.getString("accept").equals("4"))    //assigned
                                        rides.add(map);
                                }
                            }
                        }
                    } catch (JSONException e) {
                        error = 1;
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
                    notAccReq = sh.getInt("notaccreq", 0);

                    if (success == 1) {
                        ListAdapter adapter = new ListAdapter();
                        rideList.setAdapter(adapter);
                        if (categoryFilter.getSelectedItemPosition() == 0 || categoryFilter.getSelectedItemPosition() == 2) {
                            int count = 0;
                            for (int i = 0; i < rides.size(); i++) {
                                if (rides.get(i).get("accept").equals("0")) count++;
                            }
                            if (count > notAccReq) showNotification();

                            SharedPreferences.Editor edit = sh.edit();
                            edit.putInt("notaccreq", count);
                            edit.commit();
                        }
                    } else {
                        //Toast.makeText(DriverActivity.this, s, Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    private void showNotification() {
        NotificationCompat.Builder noti1 = new NotificationCompat.Builder(con);
        Intent i1 = new Intent(con, DriverActivity.class);
        noti1.setContentTitle("New taxi request!");
        noti1.setContentText("Click to see the customer information ");
        noti1.setSmallIcon(android.R.drawable.ic_dialog_alert);
        noti1.setTicker("New notification from Cyclone");

        Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        noti1.setSound(notification);

        PendingIntent pi1 = PendingIntent.getActivity(con, 100, i1, PendingIntent.FLAG_UPDATE_CURRENT);
        noti1.setAutoCancel(true);
        noti1.setContentIntent(pi1);

        NotificationManager mgr = (NotificationManager) con.getSystemService(Context.NOTIFICATION_SERVICE);
        mgr.notify(110, noti1.build());
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
        public View getView(final int i, View view, ViewGroup parent) {
            LayoutInflater inflater = getLayoutInflater();
            /*View rowView;
            if (rowView == null) {
				rowView = inflater.inflate(R.layout.request_ride_single_view, parent, false);
				holder = new ViewHolder();
				holder.txtName = (TextView) convertView.findViewById(R.id.lostandfoundtextname);
				holder.txtdate = (TextView) convertView.findViewById(R.id.lostandfoundtextdate);
				holder.imageView=(ImageView)convertView.findViewById(R.id.lostandfoundtextphoto);
				holder.descriptionTxt=(TextView)convertView.findViewById(R.id.descriptionTxt);
				holder.found=(Button)convertView.findViewById(R.id.founditem);

				convertView.setTag(holder);
			}
			else {
				holder = (ViewHolder) convertView.getTag();
			}*/
            View rowView = inflater.inflate(R.layout.request_ride_single_view, parent, false);
            TextView name = (TextView) rowView.findViewById(R.id.driverNameTv);
            TextView from = (TextView) rowView.findViewById(R.id.driverFromTv);
            TextView destination = (TextView) rowView.findViewById(R.id.driverDestinationTv);
            TextView timeDate = (TextView) rowView.findViewById(R.id.drivertimeDate);
            TextView accept = (TextView) rowView.findViewById(R.id.driverAccept);
            TextView driveracceptTop = (TextView) rowView.findViewById(R.id.driveracceptTop);

            name.setText("Passenger Name: " + rides.get(i).get("name").trim());
            from.setText("Pickup Location : " + rides.get(i).get("location").trim());
            timeDate.setText("Booking : " + rides.get(i).get("booking_time"));
            destination.setText("Drop Location: " + rides.get(i).get("droplocation").trim());

            //"All","New Requests","Accepted Rides","Completed Rides","Cancelled Rides"
            if (rides.get(i).get("accept").equals("0")) {
                accept.setText("Accept");
                driveracceptTop.setText("");
                accept.setTextColor(Color.parseColor("#AD1400"));
                driveracceptTop.setTextColor(Color.parseColor("#AD1400"));
                driveracceptTop.setVisibility(View.VISIBLE);
                driveracceptTop.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View arg0) {
                        // TODO Auto-generated method stub
                        //new postAcceptReject(rides.get(i).get("id")).execute();
                        if (rides.get(i).get("driver_id").equals(driver_id)) {
                            latitude = rides.get(i).get("latitude");
                            longitude = rides.get(i).get("longitude");
                            drop_latitude = rides.get(i).get("drop_latitude");
                            drop_longitude = rides.get(i).get("drop_longitude");
                            new SendData(rides.get(i).get("id")).execute("1", rides.get(i).get("id"));
                        }
                    }
                });
                accept.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View arg0) {
                        // TODO Auto-generated method stub
                        //new postAcceptReject(rides.get(i).get("id")).execute();
                        if (rides.get(i).get("driver_id").equals(driver_id)) {
                            new SendData(rides.get(i).get("id")).execute("0", rides.get(i).get("id"));
                        }
                    }
                });
            } else if (rides.get(i).get("accept").equals("1")) {
                accept.setText("Accepted");
                driveracceptTop.setVisibility(View.GONE);
                accept.setTextColor(Color.parseColor("#5AB83B"));

            } else if (rides.get(i).get("accept").equals("2")) {
                accept.setText("Completed");
                driveracceptTop.setVisibility(View.GONE);

            } else if (rides.get(i).get("accept").equals("3")) {
                accept.setText("Cancelled");
                driveracceptTop.setVisibility(View.GONE);
            }
            rowView.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View arg0) {
                    if (rides.get(i).get("driver_id").equals(driver_id) || rides.get(i).get("group_id").equals("0") || rides.get(i).get("driver_id").equals("0")) {
                        // TODO Auto-generated method stub
                        //new postAcceptReject(rides.get(i).get("id")).execute();
                        RequestedUserActivity.request = rides.get(i);
                        Intent in = new Intent(DriverActivity.this, RequestedUserActivity.class);
                        startActivityForResult(in, 101);
                        /*if(rides.get(i).get("accept").equals("1")){

                        }
                        else{
                            RequestedUserActivity.request = rides.get(i);
                            Intent in = new Intent(DriverActivity.this, RequestedUserActivity.class);
                            startActivityForResult(in, 101);
                        }*/

                    }

                }
            });

            return rowView;
        }
    }




    class SendData {
        ProgressDialog pDialog;
        String s = "";
        int success = -1;
        int error = 0;
        boolean driver = false;
        String Id = "";

        public SendData(String Id) {
            this.Id = Id;

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

            List<NameValuePair> params = new ArrayList<NameValuePair>();
            //params.add(new BasicNameValuePair("id", Id));
            params.add(new BasicNameValuePair("id", st[1]));
            params.add(new BasicNameValuePair("accept", st[0]));
            params.add(new BasicNameValuePair("driver_id", driver_id));
            params.add(new BasicNameValuePair("driver_email", driver_email));
            params.add(new BasicNameValuePair("driver_name", driver_name));
            params.add(new BasicNameValuePair("group_id", Integer.toString(group_id)));
            params.add(new BasicNameValuePair("accepted_datetime", datetime));

            jparser.makeHttpRequest(Globals.dataSendUrl1, "POST", params, new ServerCallback() {
                @Override
                public void onSuccess(JSONObject jsonObject) {
                    try {
                        success = jsonObject.getInt("success");
                        s = jsonObject.getString("message");
                    } catch (JSONException e) {
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

                    //Toast.makeText(con, s, Toast.LENGTH_LONG).show();
                    if (success == 0) {

                    } else if (success == 1) {
                        setResult(RESULT_OK);

                        Intent intent = new Intent(DriverActivity.this, DriverMapActivity.class);
                        intent.putExtra("lat", latitude);
                        intent.putExtra("long", longitude);
                        intent.putExtra("drop_lat", drop_latitude);
                        intent.putExtra("drop_long", drop_longitude);
                        intent.putExtra("showdirection", true);
                        startActivity(intent);

                        finish();
                    }
                }
            });
        }
    }

    public void scheduleThread() {
        handler = new Handler();
        run = new Runnable() {

            @Override
            public void run() {
                if (Util.isConnectingToInternet(DriverActivity.this)) {
                    gps = new GPSTracker(con);
                    new SendLocation().execute();
                    new GetRequestedRides().execute();

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

        public SendLocation() {
            pDialog = new ProgressDialog(con);
            pDialog.setMessage("Updating locations. Please wait...");
            pDialog.setIndeterminate(false);
            pDialog.setCancelable(true);
        }

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

                    pDialog.dismiss();

                    //Toast.makeText(con, "On execute", Toast.LENGTH_LONG).show();

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

                            startActivity(new Intent(DriverActivity.this, MainActivity.class));
                            finish();
                        }
                    }
                }
            });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (handler != null) {
            handler.removeCallbacks(run);
            run = null;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 101 && resultCode == RESULT_OK) {
            new GetRequestedRides().execute();
        }
    }
}
