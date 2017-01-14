package com.log.cyclone;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class LoginActivity extends Activity implements OnClickListener {

    final Dialog dialog = new Dialog(this);
    Button submitBtn, registerBtn, forgotpassBtn;
    EditText emailEt, passwordEt;
    //SessionManager session;

    //Driver Login Fields
    EditText drloginEmail;
    EditText drloginPassword;
    TextView drloginResult;
    Button loginSubmitBtn;

    //Registration Fields
    EditText registrationName, registrationEmail, registrationPhone, registrationPassword, registrationConfirmPassword;
    TextView registrationResult;

    //LostPass Fields
    EditText lostPassEmail;
    TextView lostPassResltText;

    SharedPreferences sh;

    JSONParser jparser = new JSONParser();

    public static final String getDataURL = null;

    Context con;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_layout);

        con = LoginActivity.this;

        sh = getSharedPreferences("CYCLONE_PREF", MODE_PRIVATE);

        init();

        String email = sh.getString("loginemail", null);
        String pass = sh.getString("loginpass", null);
        boolean driver = sh.getBoolean("type", false);

        if (email != null && pass != null && !driver) {
            emailEt.setText(email);
            passwordEt.setText(pass);
            submitBtn.performClick();
        } else if (email != null && pass != null && driver) {
            showDriverLoginDialog();
            drloginEmail.setText(email);
            drloginPassword.setText(pass);
            loginSubmitBtn.performClick();
        }

    }

    private void init() {

        submitBtn = (Button) findViewById(R.id.loginSubmitBtn);
        registerBtn = (Button) findViewById(R.id.loginRegisterBtn);
        forgotpassBtn = (Button) findViewById(R.id.loginForgotPassBtn);

        submitBtn.setOnClickListener(this);
        registerBtn.setOnClickListener(this);
        forgotpassBtn.setOnClickListener(this);

        emailEt = (EditText) findViewById(R.id.loginEmailEt);
        passwordEt = (EditText) findViewById(R.id.loginPassEt);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.login, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.driverloginmenu:
                showDriverLoginDialog();
                break;
        }

        return super.onOptionsItemSelected(item);

    }


    @Override
    public void onClick(View v) {
        if (!Util.isConnectingToInternet(this)) {
            Util.showNoInternetDialog(this);
            return;
        }

        switch (v.getId()) {
            case R.id.loginSubmitBtn:
                if (!TextUtils.isEmpty(emailEt.getText().toString())) {
                    if (!TextUtils.isEmpty(passwordEt.getText().toString())) {
                        new Login().execute("client");
                    } else
                        Toast.makeText(this, "Please enter your password", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Please enter your email address", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.loginRegisterBtn:
                showRegisterDialog();
                break;
            case R.id.loginForgotPassBtn:
                showLostPasswordDialog();
                break;
        }
    }

    public void showRegisterDialog() {

        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        //dialog.setTitle("Sign Up");
        dialog.setContentView(R.layout.register_layout);
        dialog.setCancelable(true);
        registrationName = (EditText) dialog.findViewById(R.id.registrationName);
        registrationEmail = (EditText) dialog.findViewById(R.id.registrationEmail);
        registrationPhone = (EditText) dialog.findViewById(R.id.registrationPhnNumber);
        registrationPassword = (EditText) dialog.findViewById(R.id.registrationPassword);
        registrationConfirmPassword = (EditText) dialog.findViewById(R.id.registrationConfirmPassword);
        registrationResult = (TextView) dialog.findViewById(R.id.registrationResultText);

        Button loginSubmitBtn = (Button) dialog.findViewById(R.id.registerSubmitBtn);
        loginSubmitBtn.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {

                String toastText = "Please Wait";
                if (!TextUtils.isEmpty(registrationName.getText().toString())) {
                    if (!TextUtils.isEmpty(registrationEmail.getText().toString()) && Util.validEmail(registrationEmail.getText().toString())) {
                        if (!TextUtils.isEmpty(registrationPhone.getText().toString())) {
                            if ((registrationPassword.getText().toString()).equals(registrationConfirmPassword.getText().toString()) && !TextUtils.isEmpty(registrationPassword.getText().toString())) {
                                Registration reg = new Registration();
                                reg.execute();
                            } else {
                                toastText = "Your password is not the same";
                            }
                        } else {
                            toastText = "Please enter your mobile number";
                        }
                    } else {
                        toastText = "Please enter your email address";
                    }
                } else {
                    toastText = "Please enter your name";
                }

                registrationResult.setText(toastText);
                registrationResult.setVisibility(View.VISIBLE);
            }
        });
        Button lostPasswordBtn = (Button) dialog.findViewById(R.id.registerCancelButton);
        lostPasswordBtn.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {

                dialog.cancel();
            }
        });
        dialog.show();
    }


    void showLostPasswordDialog() {
        Toast.makeText(con, "emailing value", Toast.LENGTH_SHORT).show();

        //dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setTitle("Recover Password");
        dialog.setContentView(R.layout.lostpassword_layout);
        dialog.setCancelable(true);

        lostPassEmail = (EditText) dialog.findViewById(R.id.lostPassEmailEditText);
        lostPassResltText = (TextView) dialog.findViewById(R.id.lostPassworResultText);

        Button lostPasssubmitBtn = (Button) dialog.findViewById(R.id.lostPasswordSubmitButton);
        lostPasssubmitBtn.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                if (!TextUtils.isEmpty(lostPassEmail.getText().toString())) {

                    //email function
                    StringRequest stringRequest = new StringRequest(Request.Method.POST, Globals.recoverPasswordURL,
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
                            params.put("email", lostPassEmail.getText().toString());
                            return params;
                        }

                    };

                    RequestQueue requestQueue = Volley.newRequestQueue(con);
                    requestQueue.add(stringRequest);
                   // Toast.makeText(con, "emailing value", Toast.LENGTH_SHORT).show();
                    dialog.cancel();
                } else {
                    lostPassResltText.setText("Please enter your email");
                    lostPassResltText.setVisibility(View.VISIBLE);
                }
            }
        });
        dialog.show();
    }

    class Registration extends AsyncTask<String, String, String> {


        ProgressDialog pDialog;
        String toastText = "Internet Problem";
        String regiresult = "";
        int success = 0;
        int error = 0;
        String errmsg = "Server is down";

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pDialog = new ProgressDialog(con);
            pDialog.setMessage("Registration is processing......");
            pDialog.setIndeterminate(false);
            pDialog.setCancelable(true);
            pDialog.show();
        }

        @Override
        protected String doInBackground(String... arg0) {
            String name = registrationName.getText().toString();
            String email = registrationEmail.getText().toString();
            String password = registrationPassword.getText().toString();
            String number = registrationPhone.getText().toString();

            List<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair("name", name));
            params.add(new BasicNameValuePair("email", email));
            params.add(new BasicNameValuePair("password", password));
            params.add(new BasicNameValuePair("number", number));
            params.add(new BasicNameValuePair("category", "client"));

            UserInfo.setEmail(email);
            UserInfo.setName(name);
            UserInfo.setPassword(password);
            UserInfo.setPhonenumber(number);

            jparser.makeHttpRequest(Globals.regiURL, "POST", params, new ServerCallback() {

                @Override
                public void onSuccess(JSONObject jsonObject) {
                    try {
                        success = jsonObject.getInt("success");

                        if (success == 1) {
                            toastText = "Registration complete";
                        } else if (success == 0) {
                            regiresult = jsonObject.getString("message");
                            toastText = "Problem in registration";
                        } else {
                            toastText = "Link not found";
                        }
                    } catch (JSONException e) {
                        toastText = "There are some problem";
                        e.printStackTrace();
                    } catch (Exception e) {
                        error = 1;
                    }
                }
            });

            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            //Toast.makeText(MainActivity.this, s+" "+result, Toast.LENGTH_SHORT).show();
            pDialog.dismiss();

            if (error == 1) {
                Toast.makeText(con, errmsg, Toast.LENGTH_SHORT).show();
                if (Util.isConnectingToInternet(con)) {
                    registrationResult.setText("Server is down. Please try again later");
                    registrationResult.setVisibility(View.VISIBLE);
                } else {
                    Util.showNoInternetDialog(con);
                }
                return;
            }

            if (success == 0) {
                registrationResult.setText(regiresult);
                registrationResult.setVisibility(View.VISIBLE);
            } else if (success == 1) {

				/*GetUserData data=new GetUserData();
                data.execute();

				Intent i=new Intent(con, MainScreenActivity.class);
				startActivity(i);

				registrationResult.setVisibility(View.GONE);
				finish();*/
                startActivity(new Intent(con, DriverPositionActivity.class));
                Toast.makeText(con, toastText, Toast.LENGTH_SHORT).show();
                finish();


            }
        }

    }

    public void showDriverLoginDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        //dialog.setTitle("Login");
        dialog.setContentView(R.layout.driver_login_layout);
        dialog.setCancelable(true);

        drloginEmail = (EditText) dialog.findViewById(R.id.loginEmailText);
        drloginPassword = (EditText) dialog.findViewById(R.id.loginPassword);
        drloginResult = (TextView) dialog.findViewById(R.id.loginResultText);


        loginSubmitBtn = (Button) dialog.findViewById(R.id.loginSubmitButton);
        loginSubmitBtn.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {

                if (!TextUtils.isEmpty(drloginEmail.getText().toString()) && !TextUtils.isEmpty(drloginPassword.getText().toString())) {
                    Login login = new Login();
                    login.execute("driver");
                } else {
                    Toast.makeText(con, "Please enter value", Toast.LENGTH_SHORT).show();
                }
            }
        });
        Button lostPasswordBtn = (Button) dialog.findViewById(R.id.lostPasswordButton);
        lostPasswordBtn.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {

                showLostPasswordDialog();
                dialog.dismiss();
            }
        });
        dialog.show();

        Button loginCancelButton = (Button) dialog.findViewById(R.id.loginCancelButton);
        loginCancelButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {

                dialog.cancel();
            }
        });
    }


    class Login extends AsyncTask<String, String, String> {
        ProgressDialog pDialog;
        String s = "";
        int success = -1;
        int error = 0;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pDialog = new ProgressDialog(con);
            pDialog.setMessage("Login is processing......");
            pDialog.setIndeterminate(false);
            pDialog.setCancelable(true);
            pDialog.show();
        }

        boolean driver = false;
        String email = "";
        String password = "";

        @Override
        protected String doInBackground(String... st) {


            if (st != null && st[0].equals("driver")) {
                email = drloginEmail.getText().toString();
                password = drloginPassword.getText().toString();
                driver = true;
            } else {
                email = emailEt.getText().toString();
                password = passwordEt.getText().toString();
            }

            List<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair("email", email));
            params.add(new BasicNameValuePair("password", password));

            if (driver) params.add(new BasicNameValuePair("category", "driver"));
            else params.add(new BasicNameValuePair("category", "client"));

            UserInfo.setEmail(email);
            UserInfo.setPassword(password);

            jparser.makeHttpRequest(Globals.loginURL, "POST", params, new ServerCallback() {

                @Override
                public void onSuccess(JSONObject jsonObject) {
                    try {
                        success = jsonObject.getInt("success");
                        s = jsonObject.getString("message");

                        if (success == 1) {
                            JSONObject job = jsonObject.getJSONArray("info").getJSONObject(0);
                            UserInfo.setName(job.getString("name"));
                            UserInfo.setPhonenumber(job.getString("number"));
                            UserInfo.setId(job.getString("id"));
                        }
                    } catch (JSONException e) {
                        error = 1;
                    } catch (Exception e) {
                        error = 1;
                    }
                }
            });

            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            pDialog.dismiss();

            if (error == 1) {
                if (Util.isConnectingToInternet(con)) {
                    Toast.makeText(con, "Server is down, Please try again later", Toast.LENGTH_SHORT).show();
                } else
                    Util.showNoInternetDialog(con);
                return;
            }

            if (success == 0) {
                Toast.makeText(con, s, Toast.LENGTH_LONG).show();
            } else if (success == 1) {
                /*GetUserData data=new GetUserData();
                data.execute();*/

                SharedPreferences.Editor edit = sh.edit();
                edit.putString("loginemail", email);
                edit.putString("loginpass", password);
                edit.putBoolean("type", driver);
                edit.putString("name", UserInfo.getName());
                edit.putString("id", UserInfo.getId());
                edit.commit();

                Intent i = new Intent(con, DriverPositionActivity.class);
                if (driver)
                    i = new Intent(con, DriverActivity.class);

                startActivity(i);
                Toast.makeText(con, s, Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

   /* class RecoverPassword {
        int success = 0;
        String toastText = "Internet is not available";
        String message = "";
        ProgressDialog pDialog;
        int error = 0;

        public RecoverPassword() {
            pDialog = new ProgressDialog(con);
            pDialog.setMessage("Sending Information.Please wait......");
            pDialog.setIndeterminate(false);
            pDialog.setCancelable(false);
            pDialog.show();
        }

        public void execute() {
            List<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair("email", lostPassEmail.getText().toString()));

            jparser.makeHttpRequest(Globals.recoverPasswordURL, "POST", params, new ServerCallback() {

                @Override
                public void onSuccess(JSONObject jsonObject) {
                    try {
                        success = jsonObject.getInt("success");
                        message = jsonObject.getString("message");
                    } catch (JSONException e) {
                        e.printStackTrace();
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

                    lostPassResltText.setText(message);
                    lostPassResltText.setVisibility(View.VISIBLE);
                }
            });

        }
    }*/
}
