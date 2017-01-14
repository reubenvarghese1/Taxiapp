package com.log.cyclone;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class UpdateReceiver extends BroadcastReceiver {

    Context con;
    JSONParser jparser = new JSONParser();
    ArrayList<HashMap<String, String>> reqRides;
    SharedPreferences sh;
    int rideAcceptNo = 0;

    @Override
    public void onReceive(Context context, Intent intent) {
        con = context;
        sh = con.getSharedPreferences("CYCLONE_PREF", Context.MODE_PRIVATE);
        rideAcceptNo = sh.getInt("rideaccno", 0);
        //Toast.makeText(con, "On receiver", Toast.LENGTH_SHORT).show();
        new GetRequestedRides().execute();
    }

    class GetRequestedRides {
        ProgressDialog pDialog;
        String s = "";
        int success = -1;
        int error = 0;
        String driver_name, vehicle_info;

        public GetRequestedRides() {
            pDialog = new ProgressDialog(con);
            pDialog.setMessage("Getting data. Please wait...");
            pDialog.setIndeterminate(false);
            pDialog.setCancelable(true);
        }

        public void execute() {
            List<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair("user_id", UserInfo.getId()));

            jparser.makeHttpRequest(Globals.getUserRidesUrl, "POST", params, new ServerCallback() {
                @Override
                public void onSuccess(JSONObject jsonObject) {
                    try {
                        success = jsonObject.getInt("success");
                        s = jsonObject.getString("message");

                        if (success == 1) {
                            reqRides = new ArrayList<HashMap<String, String>>();
                            JSONArray jAr = jsonObject.getJSONArray("ridelist");
                            for (int i = 0; i < jAr.length(); i++) {
                                JSONObject job = jAr.getJSONObject(i);
                                HashMap<String, String> map = new HashMap<String, String>();
                                map.put("id", job.getString("id"));
                                map.put("driver_name", job.getString("driver_name"));
                                map.put("sender_id", job.getString("sender_id"));
                                map.put("name", job.getString("name"));
                                map.put("phone", job.getString("phone"));
                                map.put("droplocation", job.getString("droplocation"));
                                map.put("location", job.getString("location"));
                                map.put("latitude", job.getString("latitude"));
                                map.put("longitude", job.getString("longitude"));
                                map.put("accept", job.getString("accept"));
                                map.put("booking_time", job.getString("booking_time"));
                                map.put("vehicleinfo", job.getString("vehicleinfo"));
                                map.put("rate",job.getString("rate"));//getting rate and also updated the ridelistphp
                                reqRides.add(map);
                            }
                        }
                    } catch (Exception e) {
                        error = 1;
                    }

                    rideAcceptNo = sh.getInt("rideaccno", 0);

                    if (error == 1) {
                        if (Util.isConnectingToInternet(con)) {
                            Toast.makeText(con, "Server is down, Please try again later", Toast.LENGTH_SHORT).show();
                        } else
                            Util.showNoInternetDialog(con);
                        return;
                    }

                    if (success == 1) {
                        int count = 0;
                        for (int i = 0; i < reqRides.size(); i++) {
                            if (reqRides.get(i).get("accept").equals("1")) {
                                driver_name = reqRides.get(i).get("driver_name");
                                vehicle_info = reqRides.get(i).get("vehicleinfo");
                            }
                            if (reqRides.get(i).get("accept").equals("4")) {
                                count++;
                                driver_name = reqRides.get(i).get("driver_name");
                                vehicle_info = reqRides.get(i).get("vehicleinfo");
                            }
                        }
                        if (count > rideAcceptNo) {
                            showNotification();
                        }
                        SharedPreferences.Editor edit = sh.edit();
                        edit.putInt("rideaccno", count);
                        edit.commit();
                    } else {
                        //Toast.makeText(con, s, Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

        private void showNotification() {
            NotificationCompat.Builder noti1 = new NotificationCompat.Builder(con);
            Intent i1 = new Intent(con, UserRequestActivity.class);
            noti1.setContentTitle("Driver Has sent a rate!");
            noti1.setContentText("Driver: " + driver_name + " (" + vehicle_info + ")");
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
    }

}
