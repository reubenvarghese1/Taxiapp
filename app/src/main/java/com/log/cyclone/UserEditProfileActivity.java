package com.log.cyclone;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.log.cyclone.General.Globals;
import com.log.cyclone.General.UserInfo;
import com.log.cyclone.util.JSONParser;
import com.log.cyclone.util.ServerCallback;
import com.log.cyclone.util.Util;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class UserEditProfileActivity extends Activity implements OnClickListener {

    EditText nameEt, numberEt, passwordEt;
    Button cancelBtn, saveBtn;
    Context con;
    JSONParser jparser = new JSONParser();
    SharedPreferences sh;

    private Menu menu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.user_edit_profile_layout);

        con = UserEditProfileActivity.this;

        sh = getSharedPreferences("CYCLONE_PREF", MODE_PRIVATE);

        init();

        if (UserInfo.getName() != null)
            nameEt.setText(UserInfo.getName());
        if (UserInfo.getPhonenumber() != null)
            numberEt.setText(UserInfo.getPhonenumber());
        if (UserInfo.getPassword() != null)
            passwordEt.setText(UserInfo.getPassword());


        getActionBar().setHomeButtonEnabled(true);
        getActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private void init() {
        nameEt = (EditText) findViewById(R.id.userChangeProfileNameEt);
        numberEt = (EditText) findViewById(R.id.userChangeProfileNumberEt);
        passwordEt = (EditText) findViewById(R.id.userChangeProfilePasswordEt);

        cancelBtn = (Button) findViewById(R.id.userChangeProfileCancelBtn);
        saveBtn = (Button) findViewById(R.id.userChangeProfileSaveBtn);
        cancelBtn.setOnClickListener(this);
        saveBtn.setOnClickListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.menu = menu;
        getMenuInflater().inflate(R.menu.usermenu, menu);
        menu.removeItem(R.id.userProfileMenu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.userbookmenu:
                startActivity(new Intent(this, BookNowUserActivity.class));
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

                                startActivity(new Intent(UserEditProfileActivity.this, MainActivity.class));

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
        switch (v.getId()) {
            case R.id.userChangeProfileCancelBtn:
                finish();
                break;
            case R.id.userChangeProfileSaveBtn:
                if (Util.isConnectingToInternet(this)) {
                    if (!TextUtils.isEmpty(nameEt.getText().toString())) {
                        if (!TextUtils.isEmpty(numberEt.getText().toString())) {
                            if (!TextUtils.isEmpty(passwordEt.getText().toString())) {
                                new UpdateInfo().execute();
                            } else Util.showToast(this, "Please enter your password");
                        } else Util.showToast(this, "Please enter your phone number");
                    } else Util.showToast(this, "Please enter your name");
                } else Util.showNoInternetDialog(this);
                break;
        }
    }

    class UpdateInfo {
        ProgressDialog pDialog;
        String s = "";
        int success = -1;
        int error = 0;

        public UpdateInfo() {
            pDialog = new ProgressDialog(con);
            pDialog.setMessage("Data updating, Please wait...");
            pDialog.setIndeterminate(false);
            pDialog.setCancelable(true);
            pDialog.show();
        }

        public void execute() {
            String email = UserInfo.getEmail();
            final String password = passwordEt.getText().toString();
            final String name = nameEt.getText().toString();
            final String number = numberEt.getText().toString();

            List<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair("email", email));
            params.add(new BasicNameValuePair("name", name));
            params.add(new BasicNameValuePair("password", password));
            params.add(new BasicNameValuePair("number", number));

            jparser.makeHttpRequest(Globals.updateURL, "POST", params, new ServerCallback() {
                @Override
                public void onSuccess(JSONObject jsonObject) {
                    try {
                        success = jsonObject.getInt("success");
                        s = jsonObject.getString("message");

                        if (success == 1) {
                            UserInfo.setName(name);
                            UserInfo.setPhonenumber(number);
                            UserInfo.setPassword(password);
                        }
                    } catch (JSONException e) {
                        //	e.printStackTrace();
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

                    Toast.makeText(con, s, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

}
