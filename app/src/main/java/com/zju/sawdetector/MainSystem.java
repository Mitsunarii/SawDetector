package com.zju.sawdetector;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MainSystem extends AppCompatActivity {

    private InputStream mFreqInput;

    //SAW传感器温度读取
    int readTempTag;
    boolean readFinishTag  = false;
    int tempHigh, tempLow;
    double sawTemp;

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

                byte[] buffer = new byte[1];
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
                    String tempStr;
                    tempStr = bytesToHexString(buffer);
                    int tempInt = strToInt(tempStr);

                    if (tempInt == 539 && readTempTag == 0)
                    {
                        readTempTag = 2;
                        readFinishTag = false;
                    }
                    else
                    {
                        switch (readTempTag){
                            case 2:
                                tempHigh = tempInt;
                                break;
                            case 1:
                                tempLow = tempInt;
                                readFinishTag = true;
                                break;
                            default:
                                break;
                        }
                        readTempTag --;
                        if (readTempTag < 0)
                            readTempTag = 0;
                    }

                    if (readFinishTag)
                    {
                        sawTemp = tempHigh * 3.3024 + tempLow * 0.0129 - 251.43;
                    }






                }
            }
        };
        threadfreq.start();
    }


    public static String bytesToHexString( byte[] b) {
        String a = "";
        for (int i = 0; i < b.length; i++) {
            String hex = Integer.toHexString(b[i] & 0xFF);
            if (hex.length() == 1) {
                hex = '0' + hex;
            }

            a = a+hex;
        }

        return a;
    }

    public static int strToInt( String str ){
        int i = 0;
        int num = 0;
        boolean isNeg = false;

        //Check for negative sign; if it's there, set the isNeg flag
        if (str.charAt(0) == '-') {
            isNeg = true;
            i = 1;
        }

        //Process each character of the string;
        while( i < str.length()) {
            num *= 10;
            num += str.charAt(i++) - '0'; //Minus the ASCII code of '0' to get the value of the charAt(i++).
        }

        if (isNeg)
            num = -num;
        return num;
    }

}
