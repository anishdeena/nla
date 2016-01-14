package com.example.anish.testchat.other;

/**
 * Created by anish on 31/10/15.
 */
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

public class Utils {

    private Context context;
    private SharedPreferences sharedPref;

    private static final String KEY_SHARED_PREF = "ANDROID_WEB_CHAT";
    private static final int KEY_MODE_PRIVATE = 0;
    private static final String KEY_SESSION_ID = "sessionId",
            FLAG_MESSAGE = "message", OP_CALL = "call";

    public Utils(Context context) {
        this.context = context;
        sharedPref = this.context.getSharedPreferences(KEY_SHARED_PREF,
                KEY_MODE_PRIVATE);
    }

    public void storeSessionId(String sessionId) {
        Editor editor = sharedPref.edit();
        editor.putString(KEY_SESSION_ID, sessionId);
        editor.commit();
    }

    public String getSessionId() {
        return sharedPref.getString(KEY_SESSION_ID, null);
    }

    public String convertToJSONString(String message) {
        String json = null;

        try {
            JSONObject jObj = new JSONObject();

            String[] words = message.split(" ");
            int op_length = words[0].length();
            Log.v("operation", words[0]);
            if(words[0].equalsIgnoreCase(OP_CALL))
                jObj.put("operation", OP_CALL);
            String query = message.substring(op_length);
            query = query.trim();
            Log.v("query", query);
            jObj.put("query", query);

            json = jObj.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return json;
    }

}
