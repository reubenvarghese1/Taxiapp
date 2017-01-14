package com.log.cyclone;

import android.content.Context;

import com.log.cyclone.General.Globals;
import com.log.cyclone.General.UserInfo;
import com.log.cyclone.util.JSONParser;
import com.log.cyclone.util.ServerCallback;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class SendModeData {
    //ProgressDialog pDialog;
    String s = "";
    int success = -1;
    int error = 0;
    Context con;
    JSONParser jparser = new JSONParser();

    public void execute(String... st) {
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("email", UserInfo.getEmail()));
        params.add(new BasicNameValuePair("mode", st[0]));

        jparser.makeHttpRequest(Globals.modeSendUrl, "POST", params, new ServerCallback() {
            @Override
            public void onSuccess(JSONObject jsonObject) {
                try {
                    success = jsonObject.getInt("success");
                    s = jsonObject.getString("message");

                } catch (Exception e) {
                    error = 1;
                }
            }
        });
    }
}
