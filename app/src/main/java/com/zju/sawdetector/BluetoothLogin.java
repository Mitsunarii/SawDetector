package com.zju.sawdetector;

import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.os.Bundle;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class BluetoothLogin extends Activity {

    private BluetoothAdapter mAdapter;
    private BroadcastReceiver mReceiver;
    private ArrayAdapter<String> mDeviceNames;
    private List<BluetoothDevice> mDevices = new ArrayList<>();
    private boolean mIsPairedList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAdapter = BluetoothAdapter.getDefaultAdapter();
        setContentView(R.layout.bluetooth_login);
        ListView deviceListView = findViewById(R.id.listView1);
        mDeviceNames = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        deviceListView.setAdapter(mDeviceNames);

        deviceListView.setOnItemClickListener(new ListView.OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (mDevices.isEmpty() || position >= mDevices.size()) {
                    return;
                }

                BluetoothDevice device = mDevices.get(position);
                if (!mIsPairedList) {
                    if (device.createBond()) {
                        Toast.makeText(getApplicationContext(), "Bluetooth device is paired.", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getApplicationContext(), "Failed to pair the bluetooth device.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    if (SystemConfig.getFrequencyDetector() == null) {
                        SystemConfig.setFrequencyDetector(device);
                        Toast.makeText(getApplicationContext(), "FrequencyDetector is selected", Toast.LENGTH_SHORT).show();
                    }

                    else {
                        SystemConfig.setTemperatureController(device);
                        Toast.makeText(getApplicationContext(), "TemperatureController is selected", Toast.LENGTH_SHORT).show();

                    }
                }


            }
        });
    }

    public void turnOn(View view){
        if (!mAdapter.isEnabled()) {
            Intent turnOn = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(turnOn, 0);
            Toast.makeText(getApplicationContext(),"Turned on"
                    ,Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(getApplicationContext(),"Already on",
                    Toast.LENGTH_LONG).show();
        }
    }


    public void showPaired (View view){
        mDeviceNames.clear();
        mDevices.clear();
        mIsPairedList = true;
        Set<BluetoothDevice> pairedDevices = mAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                String newDevice= "设备名称:  "+device.getName()+"    Mac地址:  "+device.getAddress();
                mDeviceNames.add(newDevice);
                mDevices.add(device);
            }
        }
    }

    public void startDiscovery(View view) {
        mDeviceNames.clear();
        mDevices.clear();
        mIsPairedList = false;
        mAdapter.startDiscovery();

        if (mReceiver == null) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(BluetoothDevice.ACTION_FOUND);
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);

            mReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action.equals(BluetoothDevice.ACTION_FOUND)) {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    mDeviceNames.add("设备名称:  "+device.getName()+"    Mac地址:  "+device.getAddress());
                    mDevices.add(device);
                }
                }
            };

            registerReceiver(mReceiver, filter);
        }
        Toast.makeText(getApplicationContext(),"Discovering Bluetooth Devices",
                Toast.LENGTH_LONG).show();
    }

    public void cancelDiscovery(View view) {
        mAdapter.cancelDiscovery();
        unregisterReceiver(mReceiver);
        mReceiver = null;
        Toast.makeText(getApplicationContext(),"Discovery has been canceled",
            Toast.LENGTH_LONG).show();
    }

    public void enterSystem(View view) {
        if (SystemConfig.getFrequencyDetector() == null || SystemConfig.getTemperatureController() == null)
        {
            return;
        }
        Intent intent = new Intent(BluetoothLogin.this,MainSystem.class);
        startActivity(intent);

    }

}