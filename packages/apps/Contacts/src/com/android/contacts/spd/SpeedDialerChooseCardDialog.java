package com.android.contacts.spd;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.content.DialogInterface;
import android.graphics.Typeface;

import com.android.contacts.R;

public class SpeedDialerChooseCardDialog extends AlertDialog implements AdapterView.OnItemClickListener{

    private String[] networks = {Utils.DEFAULT_CARD_G, Utils.DEFAULT_CARD_C};
    private String number;

    public SpeedDialerChooseCardDialog(Context context) {
        super(context, THEME_DEVICE_DEFAULT_DARK);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTitle(R.string.chooseDefaultCallCard);
        View view = View.inflate(getContext(), R.layout.speed_dail_default_card_choice, null);
        ListView listView = (ListView)view.findViewById(R.id.network_list);
        listView.setOnItemClickListener(this);
        setInverseBackgroundForced(true);
        setView(view);
        super.onCreate(savedInstanceState);
    }

    private ListAdapter createListAdapter() {
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getContext(),
                R.layout.speed_dial_set_default_card, networks){
            public View getView(int position, View convertView, ViewGroup parent){
                convertView = View.inflate(getContext(), R.layout.speed_dial_set_default_card, null);
                int lastCallType = Utils.getLastCallCardType(getContext(), number);
                TextView network = (TextView)convertView.findViewById(R.id.networks);
                int contactLocation = Utils.getContactLocation(getContext(), number);
                if (lastCallType == position + 1) {
                    network.setTypeface(null, Typeface.BOLD);
                } else if (TelephonyManager.PHONE_TYPE_NONE == lastCallType && contactLocation == position + 1) {
                    network.setTypeface(null, Typeface.BOLD);
                } else {
                    network.setTypeface(null, Typeface.NORMAL);
                }
                network.setText(networks[position]);
                return convertView;
            }
        };
        return adapter;
    }

    public void setData(String phoneNumber) {
        ListView listView = (ListView)findViewById(R.id.network_list);
        listView.setAdapter(createListAdapter());
        number = phoneNumber;
    }

    public void onItemClick(AdapterView<?> l, View v, int position, long id){

    }
}