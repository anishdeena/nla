package com.example.anish.testchat;

/**
 * Created by anish on 31/10/15.
 */

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.example.anish.testchat.other.Message;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MessagesListAdapter extends BaseAdapter {

    private Context context;
    private List<Message> messagesItems;
    private List<ApplicationInfo> applist = new ArrayList<ApplicationInfo>();
    private PackageManager packageManager;
    private static final String OP_SEARCH = "search";

    public MessagesListAdapter(Context context, List<Message> navDrawerItems) {
        this.context = context;
        this.messagesItems = navDrawerItems;
    }

    public void clearList() {
        messagesItems.clear();
    }

    @Override
    public int getCount() {
        return messagesItems.size();
    }

    @Override
    public Object getItem(int position) {
        return messagesItems.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public void setAppList(List<ApplicationInfo> apps) {
        applist = apps;
    }

    public void setPackageManager(PackageManager pm) { packageManager = pm; }

    @SuppressLint("InflateParams")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        Message m = messagesItems.get(position);

        LayoutInflater mInflater = (LayoutInflater) context
                .getSystemService(Activity.LAYOUT_INFLATER_SERVICE);

        // Identifying the message owner
        if (messagesItems.get(position).isSelf()) {
            // message belongs to you, so load the right aligned layout
            convertView = mInflater.inflate(R.layout.list_item_message_right,
                    null);
        } else {
            // message belongs to other person, load the left aligned layout
            convertView = mInflater.inflate(R.layout.list_item_message_left,
                    null);
        }

        TextView lblFrom = (TextView) convertView.findViewById(R.id.lblMsgFrom);
        TextView txtMsg = (TextView) convertView.findViewById(R.id.txtMsg);

        txtMsg.setText(m.getMessage());
        lblFrom.setText(m.getFromName());

        return convertView;
    }

    // Filter Class
    public void filter(String charText) {
        charText = charText.toLowerCase(Locale.getDefault());
        messagesItems.clear();
        if (charText.length() == 0) {
            //do nothing
        }
        else
        {
            Log.v("testing", "inside filter search");
            for (ApplicationInfo app : applist)
            {
                //Log.v("search matches", messagesItems.size() + "");
                JSONObject obj = new JSONObject();
                String result = "";
                String app_name = app.loadLabel(packageManager).toString();
                if (app_name.toLowerCase(Locale.getDefault()).contains(charText))
                {
                    try {

                        obj.put("app_info", app);
                        obj.put("operation", OP_SEARCH);
                        obj.put("app_package_name", app.packageName);
                        result = obj.toString();
                    }
                    catch (JSONException e) {
                        e.printStackTrace();
                    }
                    messagesItems.add(new Message("Bajji", app_name, false, result));

                }
            }
        }
        notifyDataSetChanged();
    }
}