package com.log.cyclone;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
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
import com.log.cyclone.slidingTabs.SlidePage;
import com.log.cyclone.slidingTabs.SlidePageAdapter;
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


public class MainActivity extends FragmentActivity {

    private static final int SLIDE_IMAGE_RESIDS[] = {
            R.drawable.slide_plain_1,
            R.drawable.slide_plain_1,
            R.drawable.slide_plain_1
    };

    private EditText emailText, passwordText;
    // private View                btnSignin;
    TextView driverTxt, btn_login, btn_signup;
    Button loginForgotPassBtn;
    // private RadioGroup          rgSteps;
    private ViewPager vpPager;
    private SlidePageAdapter adtPages;

    private Handler handlerSlideImages = new Handler();
    SharedPreferences sh;
    EditText drloginEmail;
    EditText drloginPassword;
    //TextView drloginResult;
    TextView loginSubmitBtn, drloginSubmitBtn, lostPasswordBtn, loginCancelButton;

    //Registration Fields
    EditText registrationName, registrationEmail, registrationPhone, registrationPassword, registrationConfirmPassword;
    TextView registrationResult;

    //LostPass Fields
    EditText lostPassEmail;
    TextView lostPassResltText, headerTxt;
    JSONParser jparser = new JSONParser();

    public static final String getDataURL = null;

    Context con;
    LinearLayout signUpViewParent, loginViewParent, driverloginViewParent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActionBar().hide();
        setContentView(R.layout.activity_main);
        con = MainActivity.this;

        sh = getSharedPreferences("CYCLONE_PREF", MODE_PRIVATE);
        passwordText = (EditText) findViewById(R.id.passwordText);
        emailText = (EditText) findViewById(R.id.emailText);
        driverTxt = (TextView) findViewById(R.id.driverTxt);
        driverTxt.setVisibility(View.GONE);
        headerTxt = (TextView) findViewById(R.id.headerTxt);
        btn_login = (TextView) findViewById(R.id.btn_login);
        btn_signup = (TextView) findViewById(R.id.btn_signup);
        loginForgotPassBtn = (Button) findViewById(R.id.loginForgotPassBtn);
        signUpViewParent = (LinearLayout) findViewById(R.id.signUpViewParent);
        loginViewParent = (LinearLayout) findViewById(R.id.loginViewParent);
        driverloginViewParent = (LinearLayout) findViewById(R.id.driverloginViewParent);
        vpPager = (ViewPager) findViewById(R.id.vp_pager);
        registrationName = (EditText) findViewById(R.id.registrationName);
        registrationEmail = (EditText) findViewById(R.id.registrationEmail);
        registrationPhone = (EditText) findViewById(R.id.registrationPhnNumber);
        registrationPassword = (EditText) findViewById(R.id.registrationPassword);
        registrationConfirmPassword = (EditText) findViewById(R.id.registrationConfirmPassword);
        registrationResult = (TextView) findViewById(R.id.registrationResultText);
        lostPasswordBtn = (TextView) findViewById(R.id.registerCancelButton);
        loginSubmitBtn = (TextView) findViewById(R.id.registerSubmitBtn);
        drloginEmail = (EditText) findViewById(R.id.loginEmailText);
        drloginPassword = (EditText) findViewById(R.id.loginPassword);
        //drloginResult=(TextView) findViewById(R.id.loginResultText);


        drloginSubmitBtn = (TextView) findViewById(R.id.loginSubmitButton);
        loginCancelButton = (TextView) findViewById(R.id.loginCancelButton);
        adtPages = new SlidePageAdapter(getSupportFragmentManager());
        vpPager.setAdapter(adtPages);
        String email = sh.getString("loginemail", null);
        String pass = sh.getString("loginpass", null);
        boolean driver = sh.getBoolean("type", false);

        if (email != null && pass != null && !driver) {
            emailText.setText(email);
            passwordText.setText(pass);
            btn_login.performClick();
        } else if (email != null && pass != null && driver) {
            showDriverLoginDialog();
            drloginEmail.setText(email);
            drloginPassword.setText(pass);
            loginSubmitBtn.performClick();
        }
        drloginSubmitBtn.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                // TODO Auto-generated method stub
                if (!TextUtils.isEmpty(drloginEmail.getText().toString()) && !TextUtils.isEmpty(drloginPassword.getText().toString())) {
                    Login1 login = new Login1();
                    login.execute1("driver");
                } else {
                    Toast.makeText(con, "Please enter value", Toast.LENGTH_SHORT).show();
                }
            }
        });
        vpPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                // rgSteps.check(STEP_RADIO_IDS[position]);
                resetAutoSliding();
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });

        btn_login.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                signIn();
            }
        });//
        loginCancelButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                headerTxt.setText("Login");
                signUpViewParent.setVisibility(View.GONE);
                loginViewParent.setVisibility(View.VISIBLE);
                //driverloginViewParent.setVisibility(View.GONE);
                //driverTxt.setVisibility(View.VISIBLE);
                //signIn();
            }
        });
        btn_signup.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                // TODO Auto-generated method stub
                //showRegisterDialog();
                headerTxt.setText("SignUp");
                signUpViewParent.setVisibility(View.VISIBLE);
                loginViewParent.setVisibility(View.GONE);
                driverloginViewParent.setVisibility(View.GONE);
            }
        });
        lostPasswordBtn.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                headerTxt.setText("Login");
                signUpViewParent.setVisibility(View.GONE);
                loginViewParent.setVisibility(View.VISIBLE);
                driverloginViewParent.setVisibility(View.GONE);
            }
        });
        //check this if need be
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
        loginForgotPassBtn.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                // TODO Auto-generated method stub
                showLostPasswordDialog();
            }
        });//
        driverTxt.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                // TODO Auto-generated method stub
                showDriverLoginDialog();
                headerTxt.setText("Driver Login");
                signUpViewParent.setVisibility(View.GONE);
                //loginViewParent.setVisibility(View.GONE);
                //driverloginViewParent.setVisibility(View.VISIBLE);
                //driverTxt.setVisibility(View.INVISIBLE);
            }
        });
        initialize();
    }

    public void showDriverLoginDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        //dialog.setTitle("Login");
        dialog.setContentView(R.layout.driver_login_layout);
        dialog.setCancelable(true);

        drloginEmail = (EditText) dialog.findViewById(R.id.loginEmailText);
        drloginPassword = (EditText) dialog.findViewById(R.id.loginPassword);
        //drloginResult=(TextView) dialog.findViewById(R.id.loginResultText);


        loginSubmitBtn = (Button) dialog.findViewById(R.id.loginSubmitButton);
        loginSubmitBtn.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {

                if (!TextUtils.isEmpty(drloginEmail.getText().toString()) && !TextUtils.isEmpty(drloginPassword.getText().toString())) {
                    Login1 login = new Login1();
                    login.execute1("driver");
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

    private void resetAutoSliding() {
        handlerSlideImages.removeCallbacksAndMessages(null);
        handlerSlideImages.postDelayed(new Runnable() {
            @Override
            public void run() {
                int pageIndex = vpPager.getCurrentItem() + 1;
                if (pageIndex >= adtPages.getCount())
                    pageIndex = 0;
                vpPager.setCurrentItem(pageIndex, true);
            }
        }, 3000);
    }

    public void initialize() {
        if (adtPages != null)
            adtPages.clear();

        for (int i = 0; i < SLIDE_IMAGE_RESIDS.length; i++)
            adtPages.add(new SlidePage(SLIDE_IMAGE_RESIDS[i]));
        adtPages.notifyDataSetChanged();
        resetAutoSliding();
    }

    public void signInDriver() {
        if (!TextUtils.isEmpty(drloginEmail.getText().toString()) && !TextUtils.isEmpty(drloginPassword.getText().toString())) {
            Login1 login1 = new Login1();
            login1.execute1("driver");
        } else {
            Toast.makeText(con, "Please enter value", Toast.LENGTH_SHORT).show();
        }
    }

    class Registration {
        ProgressDialog pDialog;
        String toastText = "Internet Problem";
        String regiresult = "";
        int success = 0;
        int error = 0;
        String errmsg = "Server is down";

        public Registration() {
            pDialog = new ProgressDialog(con);
            pDialog.setMessage("Registration is processing......");
            pDialog.setIndeterminate(false);
            pDialog.setCancelable(true);
            pDialog.show();
        }

        public void execute() {
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
                            startActivity(new Intent(con, DriverPositionActivity.class));
                            Toast.makeText(con, toastText, Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    } catch (JSONException e) {
                        toastText = "There are some problem";
                        e.printStackTrace();
                    } catch (Exception e) {
                        error = 1;
                    }
                }
            });
        }
    }

    void showLostPasswordDialog() {

        final Dialog dialog = new Dialog(this);
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
                    dialog.dismiss();
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
                    Toast.makeText(con, "emailing value", Toast.LENGTH_SHORT).show();




                } else {
                    lostPassResltText.setText("Please enter your email");
                    lostPassResltText.setVisibility(View.VISIBLE);
                }
            }
        });
        dialog.show();
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

    private void signIn() {
        if (!TextUtils.isEmpty(emailText.getText().toString())) {
            if (!TextUtils.isEmpty(passwordText.getText().toString())) {
                new Login().execute();
            } else Toast.makeText(this, "Please enter your password", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Please enter your email address", Toast.LENGTH_SHORT).show();
        }
        /*String email = emailText.getText().toString();
        String pinCode = passwordText.getText().toString();*/
        /*if (pinCode.isEmpty()) {
            Toast.makeText(this, "Please input PIN code!", Toast.LENGTH_LONG).show();
            return;
        }
        if (!pinCode.equals(Constants.LoginInfo.PIN_CODE)) {
            Toast.makeText(this, "PIN code is wrong. Please try again!", Toast.LENGTH_LONG).show();
            return;
        }


        Intent intent = new Intent(this, HomeActivity.class);
        intent.putExtra(HomeActivity.INTENT_IMAGE_RESID, SLIDE_IMAGE_RESIDS[vpPager.getCurrentItem()]);
        startActivity(intent);
        overridePendingTransition(R.anim.move_in_left, R.anim.move_out_left);
        finish();*/
    }

    class Login1 {
        ProgressDialog pDialog;
        String s = "";
        int success = -1;
        int error = 0;
        boolean driver = false;
        String email = "";
        String password = "";
        int group_id;

        public Login1() {
            pDialog = new ProgressDialog(con);
            pDialog.setMessage("Login is processing......");
            pDialog.setIndeterminate(false);
            pDialog.setCancelable(true);
            pDialog.show();
        }

        public void execute1(String... st) {
            if (st != null && st[0].equals("driver")) {
                email = drloginEmail.getText().toString();
                password = drloginPassword.getText().toString();
                driver = true;
            } else {
                email = emailText.getText().toString();
                password = passwordText.getText().toString();
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
                            group_id = job.getInt("group_id");
                        }

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

                            SharedPreferences.Editor edit = sh.edit();
                            edit.putString("loginemail", email);
                            edit.putString("loginpass", password);
                            edit.putBoolean("type", driver);
                            edit.putString("name", UserInfo.getName());
                            edit.putString("id", UserInfo.getId());
                            edit.putInt("group_id", group_id);
                            edit.commit();

                            Intent i = new Intent(con, DriverPositionActivity.class);
                            if (driver)
                                i = new Intent(con, DriverMapActivity.class);
                            startActivity(i);
                            Toast.makeText(con, s, Toast.LENGTH_SHORT).show();
                            finish();
                        }

                    } catch (JSONException e) {
                        //	e.printStackTrace();
                        error = 1;
                    } catch (Exception e) {
                        error = 1;
                    }
                }
            });
        }
    }


    class Login {
        ProgressDialog pDialog;
        String s = "";
        int success = -1;
        int error = 0;
        boolean driver = false;
        String email = "";
        String password = "";
        int group_id;
        String categoryofp;

        public Login() {
            pDialog = new ProgressDialog(con);
            pDialog.setMessage("Login is processing......");
            pDialog.setIndeterminate(false);
            pDialog.setCancelable(true);
            pDialog.show();
        }

        public void execute() {
            /*if (st != null && st[0].equals("driver")) {
                email = drloginEmail.getText().toString();
                password = drloginPassword.getText().toString();
                driver = true;
            } else {*/
                email = emailText.getText().toString();
                password = passwordText.getText().toString();
            //}

            List<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair("email", email));
            params.add(new BasicNameValuePair("password", password));

          /*  if (driver) params.add(new BasicNameValuePair("category", "driver"));
            else params.add(new BasicNameValuePair("category", "client"));*/

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
                            group_id = job.getInt("group_id");
                            categoryofp=job.getString("category");
                            if (categoryofp.equals("driver")){
                                driver=true;
                            }
                            else{
                                driver=false;
                            }
                        }

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

                            SharedPreferences.Editor edit = sh.edit();
                            edit.putString("loginemail", email);
                            edit.putString("loginpass", password);
                            edit.putBoolean("type", driver);
                            edit.putString("name", UserInfo.getName());
                            edit.putString("id", UserInfo.getId());
                            edit.putInt("group_id", group_id);
                            edit.commit();

                            Intent i = new Intent(con, DriverPositionActivity.class);
                            if (driver)
                            {    i = new Intent(con, DriverMapActivity.class);}



                            startActivity(i);
                            Toast.makeText(con, s, Toast.LENGTH_SHORT).show();
                            finish();
                        }

                    } catch (JSONException e) {
                        //	e.printStackTrace();
                        error = 1;
                    } catch (Exception e) {
                        error = 1;
                    }
                }
            });
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
                    } catch (JSONException e) {
                        e.printStackTrace();
                    } catch (Exception e) {
                        error = 1;
                    }
                }
            });
        }
    }*/

}
