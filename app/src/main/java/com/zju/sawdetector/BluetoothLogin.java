package com.zju.sawdetector;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.os.Bundle;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.util.Set;

public class BluetoothLogin extends Activity {

    private BluetoothAdapter mAdapter;
    private BroadcastReceiver mReceiver;
    private ArrayAdapter<String> mDeviceList;
    boolean scanTag = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAdapter = BluetoothAdapter.getDefaultAdapter();
        setContentView(R.layout.bluetooth_login);
        ListView deviceListView = findViewById(R.id.listView1);
        mDeviceList = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        deviceListView.setAdapter(mDeviceList);

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
        mDeviceList.clear();
        Set<BluetoothDevice> pairedDevices = mAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            mDeviceList.add("已配对设备：");
            for (BluetoothDevice device : pairedDevices) {
                String newDevice= "设备名称:  "+device.getName()+"    Mac地址:  "+device.getAddress();
                mDeviceList.add(newDevice);
            }
        }
    }



    public void startDiscovery(View view) {
        mAdapter.startDiscovery();
        if (scanTag)
        {
            mDeviceList.add("周围扫描到的设备：");
            scanTag = false;
        }

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
                        mDeviceList.add("设备名称:  "+device.getName()+"    Mac地址:  "+device.getAddress());
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
        Toast.makeText(getApplicationContext(),"Discovery has been canceled",
            Toast.LENGTH_LONG).show();

    }

    public void enterSystem(View view) {

        Intent intent = new Intent(BluetoothLogin.this,MainSystem.class);
        startActivity(intent);

    }

}