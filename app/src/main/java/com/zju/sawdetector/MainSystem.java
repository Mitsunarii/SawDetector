package com.zju.sawdetector;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

public class MainSystem extends AppCompatActivity {

    private InputStream mFreqInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        Thread threadfreq = new Thread(){
            @Override
            public void run() {
                BluetoothDevice freq = SystemConfig.getFrequencyDetector();
                BluetoothDevice temp = SystemConfig.getTemperatureController();

                UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
                BluetoothSocket socket;
                try {
                    socket = freq.createRfcommSocketToServiceRecord(uuid);
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }

                try {
                    socket.connect();
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }

                try {
                    mFreqInput = socket.getInputStream();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if (mFreqInput == null) {
                    return;
                }

                byte[] buffer = new byte[128];
                int length;
                for (;;) {
                    try {
                        length = mFreqInput.read(buffer);
                    } catch (IOException e) {
                        e.printStackTrace();
                        return;
                    }
                    if (length <= 0) {
                        return;
                    }

                    for (int i = 0; i + 1 < length; i++) {

                    }

                    Log.i("FREQ", new String(buffer));
                }
            }
        };
        threadfreq.start();
    }
}
