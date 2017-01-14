package com.log.cyclone;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.log.cyclone.General.Chat;
import com.log.cyclone.General.Globals;
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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class ChatActivity extends Activity {
    EditText chatBox;
    ListView listView;
    ArrayList<Chat> chatList;
    TextView sendButton;
    ChatAdapter adapter;
    boolean check = true;
    private Menu menu;
    SharedPreferences sh;
    Context con;
    JSONParser jparser = new JSONParser();
    Handler handler;
    Runnable run;
    String message;
    GPSTracker gps;
    String driver_email;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chat_activity);

        con = ChatActivity.this;
        gps = new GPSTracker(this);

        sh = getSharedPreferences("CYCLONE_PREF", MODE_PRIVATE);
        driver_email = sh.getString("loginemail", "");

        chatBox = (EditText) findViewById(R.id.chatBox);
        listView = (ListView) findViewById(R.id.listView);
        sendButton = (TextView) findViewById(R.id.sendButton);

        chatList = new ArrayList<Chat>();

		/*
        Chat c=new Chat();
		c.setMessage("Hello for test.");
		c.setSent("Y");
		chatList.add(c);

		Chat c1=new Chat();
		c1.setMessage("Recived text");
		 c1.setSent("N");
		 chatList.add(c1);
		 */

        refreshChat();

        adapter = new ChatAdapter(ChatActivity.this, R.layout.chat_row, chatList);
        listView.setAdapter(adapter);

        sendButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                // TODO Auto-generated method stub
                message = chatBox.getText().toString();
                /*

				Chat c=new Chat();
				c.setMessage(message);
				c.setSent("N");
				chatList.add(c);
				adapter.notifyDataSetChanged();
				chatBox.setText("");
				scrollMyListViewToBottom();
				*/

                new SendChatMessage().execute();
                chatBox.setText("");
            }
        });

    }

    private void scrollMyListViewToBottom() {
        listView.post(new Runnable() {
            @Override
            public void run() {
                // Select the last row so it will scroll into view...
                listView.setSelection(adapter.getCount() - 1);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.menu = menu;
        getMenuInflater().inflate(R.menu.driver_menu, menu);
        menu.removeItem(R.id.driverChat);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        SharedPreferences.Editor edit;
        AlertDialog.Builder build;
        AlertDialog alert;

        switch (item.getItemId()) {
            case R.id.bookStatus:
                startActivity(new Intent(this, BookNowDriverActivity.class));
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
        return false;
    }

    class SendChatMessage {
        String s = "";
        int success = -1;
        int error = 0;

        public void execute() {
            Calendar cal = Calendar.getInstance();
            String datetime = cal.get(Calendar.YEAR) + "-" + (cal.get(Calendar.MONTH) + 1) + "-" + cal.get(Calendar.DAY_OF_MONTH) + " " +
                    cal.get(Calendar.HOUR_OF_DAY) + ":" + cal.get(Calendar.MINUTE) + ":" + cal.get(Calendar.SECOND);

            List<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair("user_id", UserInfo.getId()));
            params.add(new BasicNameValuePair("fromadmin", "0"));
            params.add(new BasicNameValuePair("message", message));
            params.add(new BasicNameValuePair("datetime", datetime));

            jparser.makeHttpRequest(Globals.sendChatMessageUrl, "POST", params, new ServerCallback() {

                @Override
                public void onSuccess(JSONObject jsonObject) {
                    try {
                        success = jsonObject.getInt("success");
                        s = jsonObject.getString("message");
                    } catch (JSONException e) {
                        if (Util.isConnectingToInternet(con)) {
                            Toast.makeText(con, "Server is down, Please try again later", Toast.LENGTH_SHORT).show();
                        } else
                            Util.showNoInternetDialog(con);
                        return;
                    } finally {
                        Toast.makeText(ChatActivity.this, s, Toast.LENGTH_LONG).show();
                    }
                }
            });
        }
    }

    public void refreshChat() {
        handler = new Handler();
        run = new Runnable() {

            @Override
            public void run() {
                new GetChatMessages().execute();
                handler.postDelayed(run, 5000);    //execute every 5 seconds
            }
        };
        handler.postDelayed(run, 100);    //execute immediately
    }

    class GetChatMessages {
        //ProgressDialog pDialog;
        String s = "";
        int success = -1;
        int error = 0;

        public void execute() {
            List<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair("user_id", UserInfo.getId()));

            jparser.makeHttpRequest(Globals.getChatMessagesUrl, "POST", params, new ServerCallback() {

                @Override
                public void onSuccess(JSONObject jsonObject) {
                    try {
                        success = jsonObject.getInt("success");
                        s = jsonObject.getString("message");

                        if (success == 1) {
                            chatList.clear();

                            JSONArray jAr = jsonObject.getJSONArray("messages");
                            for (int i = 0; i < jAr.length(); i++) {
                                JSONObject job = jAr.getJSONObject(i);
                                Chat c = new Chat();
                                c.setMessage(job.getString("message"));
                                if (job.getString("fromadmin").equals("1")) {
                                    //message from admin
                                    c.setSent("N");
                                } else {
                                    //message from user
                                    c.setSent("Y");
                                }
                                chatList.add(c);
                            }

                            adapter.notifyDataSetChanged();
                            scrollMyListViewToBottom();
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

    public void scheduleThread() {
        handler = new Handler();
        run = new Runnable() {

            @Override
            public void run() {
                if (Util.isConnectingToInternet(ChatActivity.this)) {
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
                    } catch (JSONException e) {
                        if (Util.isConnectingToInternet(con)) {
                            Toast.makeText(con, "Server is down, Please try again later", Toast.LENGTH_SHORT).show();
                        } else
                            Util.showNoInternetDialog(con);
                        return;
                    } finally {
                        Toast.makeText(con, s, Toast.LENGTH_LONG).show();
                        if (success == 0) {

                        } else if (success == 1) {
                            scheduleThread();
                        }
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
            params.add(new BasicNameValuePair("email", UserInfo.getEmail()));
            params.add(new BasicNameValuePair("mode", st[0]));

            jparser.makeHttpRequest(Globals.modeSendUrl, "POST", params, new ServerCallback() {
                @Override
                public void onSuccess(JSONObject jsonObject) {
                    try {
                        success = jsonObject.getInt("success");
                        s = jsonObject.getString("message");

                        pDialog.dismiss();
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
                        Toast.makeText(con, s, Toast.LENGTH_LONG).show();

                        if (isLogout) {
                            SharedPreferences.Editor edit = sh.edit();
                            edit.putString("loginemail", null);
                            edit.putString("loginpass", null);
                            edit.putBoolean("type", false);
                            edit.putInt("group_id", 0);
                            edit.commit();

                            startActivity(new Intent(ChatActivity.this, MainActivity.class));
                            finish();
                        }
                    }
                }
            });
        }
    }

}
