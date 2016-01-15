package com.example.anish.testchat;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.ContentProviderOperation;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.example.anish.testchat.other.Message;
import com.example.anish.testchat.other.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity {


    //contact search variables
    private String searchString;
    private String callString;
    private final static int MY_PERMISSIONS_REQUEST_CALL_PHONE=1;
    private final static int MY_PERMISSIONS_REQUEST_WRITE_CONTACTS=3;
    private final static int MY_PERMISSIONS_REQUEST_SEARCH_CONTACTS=4;


    // LogCat tag
    private static final String TAG = MainActivity.class.getSimpleName();

    private Button btnSend, btnAction;
    private EditText inputMsg;

    // Chat messages list adapter
    private MessagesListAdapter adapter;
    private List<Message> listMessages;
    private ListView listViewMessages;

    private Utils utils;

    // Client name
    private String name = null;

    // JSON flags to identify the kind of JSON response
    private static final String TAG_SELF = "self", TAG_NEW = "new",
            TAG_MESSAGE = "message", TAG_EXIT = "exit", OP_CALL = "call", OP_ADDCONTACT = "add", OP_SEARCH = "search";


    //Contact add global variables. Must change
    private String ctName;
    private String ctNumber;

    //App search variables
    private PackageManager packageManager = null;
    private List<ApplicationInfo> applist = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getActionBar().hide();

        btnSend = (Button) findViewById(R.id.btnSend);
        btnAction = (Button) findViewById(R.id.btnAction);

        inputMsg = (EditText) findViewById(R.id.inputMsg);
        listViewMessages = (ListView) findViewById(R.id.list_view_messages);

        utils = new Utils(getApplicationContext());

        // Getting the person name from previous screen
        Intent i = getIntent();
        name = "Bajji";//i.getStringExtra("name");

        //Code to list installed apps and also get their intents

        packageManager = getPackageManager();

        /*List<ApplicationInfo> packages = packageManager
                .getInstalledApplications(PackageManager.GET_META_DATA);

        for (ApplicationInfo packageInfo : packages) {

            Log.d(TAG, "Installed package :" + packageInfo.packageName);
            Log.d(TAG,
                    "Launch Activity :"
                            + packageManager.getLaunchIntentForPackage(packageInfo.packageName));

        }*/

        new LoadApplications().execute();



        //Event listeners

        btnSend.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // Sending message to web socket server
                sendMessageToProcessor(utils.convertToJSONString(inputMsg.getText()
                        .toString()), name);

                // Clearing the input filed once message was sent
                inputMsg.setText("");
            }
        });

        btnAction.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // Sending message to web socket server
                checkPermissionsAndWrite(ctName, ctNumber);

                // Clearing the input filed once message was sent
                inputMsg.setText("");
            }
        });

        inputMsg.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {

                String[] words = inputMsg.getText().toString().split(" ");
                if(words.length > 1 && words[1].length() >= 3) {
                    // Sending message to web socket server
                    sendMessageToProcessor(utils.convertToJSONString(inputMsg.getText()
                            .toString()), name);

                }

            }

            @Override
            public void afterTextChanged(Editable edit) {
                if (edit.length() != 0) {
                    // Business logic for search here
                }
            }
        });

        listMessages = new ArrayList<Message>();

        adapter = new MessagesListAdapter(this, listMessages);
        listViewMessages.setAdapter(adapter);

        adapter.setPackageManager(packageManager);

        listViewMessages.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Message clickedItem = (Message) listViewMessages.getItemAtPosition(position);
                Log.v("message", clickedItem.getMessage());
                Log.v("payload", clickedItem.getPayload());
                try {
                    JSONObject payloadJson = new JSONObject(clickedItem.getPayload());


                    if(payloadJson.getString("operation").equalsIgnoreCase(OP_CALL)) {
                        checkPermissionsAndCall(payloadJson.getString("phone"));
                    }
                    else if(payloadJson.getString("operation").equalsIgnoreCase(OP_SEARCH)) {
                        try {
                            JSONObject obj = new JSONObject(clickedItem.getPayload());
                            String appPackageName = (String) obj.get("app_package_name");
                            Intent intent = packageManager
                                    .getLaunchIntentForPackage(appPackageName);

                            if (null != intent) {
                                startActivity(intent);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
                catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

        /**
         * Creating web socket client. This will have callback methods
         * */
    }

    /**
     * Method to send message to web socket server
     * */
    private void sendMessageToProcessor(String msg, String username) {
        //Stub
        try {
            JSONObject jObj = new JSONObject(msg);

            //set name
            jObj.put("name", username);

            String operation = jObj.getString("operation");
            adapter.clearList();

            if (operation.equalsIgnoreCase(OP_CALL)) {
                // It is a call operation
                String fromName = jObj.getString("name");
                String query = jObj.getString("query");
                boolean isSelf = false;

                ArrayList<String> searchResults;

                //Call contact search here
                searchResults = checkPermissionsAndSearchContacts(query);

                //Parsing the arraylist
                for(int i = 0; i< searchResults.size(); i++) {
                    Log.v("searchItem", searchResults.get(i));
                    try {
                        JSONObject searchItem = new JSONObject(searchResults.get(i));
                        JSONObject payload = new JSONObject();
                        payload.put("operation", operation);
                        payload.put("query", query);
                        payload.put("name", searchItem.getString("name"));
                        payload.put("phone", searchItem.getString("phone"));
                        Message m = new Message(fromName, searchItem.getString("name") + " " + searchItem.getString("phone"), isSelf, payload.toString());
                        appendMessage(m);
                    }
                    catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                //Message m = new Message(fromName, message, isSelf);
                //Log.v("logging", m.getFromName() + " $$ " + m.getMessage() + " $$ " + m.isSelf());
                // Appending the message to chat list
                //appendMessage(m);
            }

            if (operation.equalsIgnoreCase(OP_ADDCONTACT)) {
                String fromName = jObj.getString("name");
                String query = jObj.getString("query");
                Pattern phoneNumberPattern = Pattern.compile("([\\+0-9-( )]+)$"); //[\+?]
                Matcher matcher = phoneNumberPattern.matcher(query);
                String phoneNumber = "";
                String phoneName = "";
                if(matcher.find()) {
                    phoneNumber = matcher.group(0);
                    Log.v("Phone extracted", matcher.group(0));
                }
                if(!phoneNumber.equalsIgnoreCase("")) {
                    phoneName = query.replace(phoneNumber, "");
                    phoneName = phoneName.trim();
                }
                Log.v("Extracted add info", phoneName + " --|-- " + phoneNumber);

                ctName = phoneName;
                ctNumber = phoneNumber;


            }

            if (operation.equalsIgnoreCase(OP_SEARCH)) {

                String fromName = jObj.getString("name");
                String query = jObj.getString("query");
                Log.v("Entering search", "query");
                adapter.filter(query);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
        Log.v("logging", msg);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

    }

    /**
     * Appending message to list view
     * */
    private void appendMessage(final Message m) {
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                listMessages.add(m);

                adapter.notifyDataSetChanged();

                // Playing device's notification
                playBeep();
            }
        });
    }

    private void showToast(final String message) {

        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), message,
                        Toast.LENGTH_LONG).show();
            }
        });

    }

    /**
     * Plays device's default notification sound
     * */
    public void playBeep() {

        try {
            Uri notification = RingtoneManager
                    .getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            Ringtone r = RingtoneManager.getRingtone(getApplicationContext(),
                    notification);
            r.play();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }


    // *** CORE UTIL FUNCTIONS ***


    //SEARCH CONTACTS

    private ArrayList<String> checkPermissionsAndSearchContacts(String query)
    {
        //set global variable
        searchString = query;
        ArrayList<String> searchResults;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Toast.makeText(getApplicationContext(), "Version 23", Toast.LENGTH_SHORT).show();
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_CONTACTS}, MY_PERMISSIONS_REQUEST_SEARCH_CONTACTS);
                return null;
            }
            else
                Log.e("already Permitted", "Search already Permitted");
            Toast.makeText(getApplicationContext(), "Search already Permitted", Toast.LENGTH_SHORT).show();
            searchResults = searchContacts(query);
            return searchResults;
        }
        else
            Toast.makeText(getApplicationContext(), "Lower Version", Toast.LENGTH_SHORT).show();
        searchResults = searchContacts(query);
        return searchResults;
    }


    //Stub to read from public variable for permissions callback. TODO: implement closure
    private void searchContactsCaller()
    {
        String query = searchString;
        ArrayList<String> searchResults;
        searchResults = searchContacts(query);
    }

    private ArrayList<String> searchContacts(String query)
    {
        ArrayList<String> results = new ArrayList<>();
        JSONObject jObj = new JSONObject();

        Uri lkup = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_FILTER_URI, query);
        Cursor idCursor = getContentResolver().query(lkup, null, null, null, null);
        while (idCursor.moveToNext()) {
            String id = idCursor.getString(idCursor.getColumnIndex(ContactsContract.Contacts._ID));
            String key = idCursor.getString(idCursor.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY));
            String name = idCursor.getString(idCursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
            //Log.d(LOG_TAG, "search: "+id + " key: "+key + " name: "+name);
            //Toast.makeText(getApplicationContext(),name+" "+key, Toast.LENGTH_SHORT).show();

            ArrayList<String> phones = new ArrayList<String>();


            Cursor cursor = getContentResolver().query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    null,
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                    new String[]{id}, null);

            while (cursor.moveToNext())
            {
                phones.add(cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)));
                String phoneNumber = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                //Toast.makeText(getApplicationContext(),name+" "+phoneNumber, Toast.LENGTH_SHORT).show();
                try
                {
                    jObj.put("name", name);
                    jObj.put("phone", phoneNumber);
                    results.add(jObj.toString());
                }
                catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            cursor.close();

        }
        idCursor.close();

        return results;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {

            case MY_PERMISSIONS_REQUEST_CALL_PHONE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //M case problem
                    //call();
                    Toast.makeText(getApplicationContext(), "Call intiatied after Permission", Toast.LENGTH_SHORT).show();
                    Log.e("Call init aftr Permsn","Call init aftr Permsn");

                } else {

                    Toast.makeText(getApplicationContext(), "Permission denied", Toast.LENGTH_SHORT).show();
                    Log.e("Permission Denies", "Perminssion Denied");

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }

            }
            case MY_PERMISSIONS_REQUEST_SEARCH_CONTACTS: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //M case problem
                    searchContactsCaller();
                    //permission granted
                } else {

                    Toast.makeText(getApplicationContext(), "Read contact denied", Toast.LENGTH_SHORT).show();
                    Log.e("read contact Denied", "Perminssion Denied");
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }
            case MY_PERMISSIONS_REQUEST_WRITE_CONTACTS:{

                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //M case problem
                    //writeContact();
                    Toast.makeText(getApplicationContext(), "Addcontact permitted", Toast.LENGTH_SHORT).show();
                    Log.e("Addcontact permitted", "Addcontact permitted");    //permission granted
                } else {

                    Toast.makeText(getApplicationContext(), "Addcontact denied", Toast.LENGTH_SHORT).show();
                    Log.e("addcontact Denied", "addcontact Denied");

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;

            }
        }
    }

    //CALL
    private void call(String phone) {

        callString = "tel:" + phone;

        try {
            Intent callIntent = new Intent(Intent.ACTION_CALL);
            callIntent.setData(Uri.parse(callString));
            //noinspection ResourceType
            startActivity(callIntent);

        } catch (ActivityNotFoundException e) {
            Toast.makeText(getApplicationContext(), "Call Failed", Toast.LENGTH_SHORT).show();
            Log.e("Call Failed", "Call failed");
        }

    }

    private void checkPermissionsAndCall(String phone)
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Toast.makeText(getApplicationContext(), "Version 23", Toast.LENGTH_SHORT).show();
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CALL_PHONE},MY_PERMISSIONS_REQUEST_CALL_PHONE);
                return;
            }
            else

                Log.e("Call already Permitted", "Call already Permitted");
            Toast.makeText(getApplicationContext(), "Call already Permitted", Toast.LENGTH_SHORT).show();
            call(phone);
            return;
        }
        else
            Toast.makeText(getApplicationContext(), "Lower Version", Toast.LENGTH_SHORT).show();
        call(phone);
        return;
    }

    //ADD CONTACT

    private void writeContact(String phoneName, String phoneNumber) {
        ArrayList contentProviderOperations = new ArrayList();
        //insert raw contact using RawContacts.CONTENT_URI
        contentProviderOperations.add(ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null).withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null).build());
        //insert contact display name using Data.CONTENT_URI
        contentProviderOperations.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0).withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, phoneName).build());
        //insert mobile number using Data.CONTENT_URI
        contentProviderOperations.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0).withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phoneNumber).withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE).build());
        try {
            getApplicationContext().getContentResolver().
                    applyBatch(ContactsContract.AUTHORITY, contentProviderOperations);
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (OperationApplicationException e) {
            e.printStackTrace();
        }
    }


    private void checkPermissionsAndWrite(String phoneName, String phoneNumber)
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Toast.makeText(getApplicationContext(), "Version 23", Toast.LENGTH_SHORT).show();
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.WRITE_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_CONTACTS},MY_PERMISSIONS_REQUEST_WRITE_CONTACTS);
                return;
            }
            else

                Log.e("Contacts wr Permitted", "Contacts already Permitted");
            Toast.makeText(getApplicationContext(), "Contacts already Permitted", Toast.LENGTH_SHORT).show();
            writeContact(phoneName, phoneNumber);
            return;
        }
        else
            Toast.makeText(getApplicationContext(), "Lower Version", Toast.LENGTH_SHORT).show();
        writeContact(phoneName, phoneNumber);
        return;
    }


    //Data intensive operations like loading app data, etc
    private List<ApplicationInfo> checkForLaunchIntent(List<ApplicationInfo> list) {
        ArrayList<ApplicationInfo> applist = new ArrayList<ApplicationInfo>();
        for (ApplicationInfo info : list) {
            try {
                if (null != packageManager.getLaunchIntentForPackage(info.packageName)) {
                    applist.add(info);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return applist;
    }

    private class LoadApplications extends AsyncTask<Void, Void, Void> {
        private ProgressDialog progress = null;

        @Override
        protected Void doInBackground(Void... params) {
            applist = checkForLaunchIntent(packageManager.getInstalledApplications(PackageManager.GET_META_DATA));
            adapter.setAppList(applist);
            return null;
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
        }
    }


}