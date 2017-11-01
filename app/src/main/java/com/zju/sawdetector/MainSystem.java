package com.zju.sawdetector;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.achartengine.GraphicalView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


import java.util.Timer;
import java.util.UUID;

import com.zju.sawdetector.ChartService;





public class MainSystem extends AppCompatActivity {

    private InputStream mFreqInput;
    BluetoothSocket socketFreq;
    private OutputStream mFreqOutput;


    //SAW传感器温度读取
    int readTempTag =0;
    boolean readTempFinishTag  = false;
    int tempHigh, tempLow;
    double sawTemp;
    private TextView sawTempShow;

    //SAW传感器频率读取
    boolean freqCheckTag = false;
    int readFreqTag =0;
    boolean readFreqFinishTag  = false;
    int sensor2,sensor1,sensor0,ref2,ref1,ref0;
    long sensor,ref;
    double sawFreq;
    private TextView sawFreqShow;
    private long freqBeginTime;
    private double freqTime;

    Button mStartFrequency;


    public static final int updateSawTemp = 1;
    public static final int updateSawFreq =2;

    //画图
    LinearLayout mFreqChart;
    private GraphicalView FreqChartView;
    private ChartService mFreqService;
    private Timer timer;


    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler () {
        @SuppressLint({"DefaultLocale", "SetTextI18n"})
        public void handleMessage(Message msg){

            switch (msg.what){
                case updateSawTemp:
                     sawTempShow.setText(String.format("%.2f", sawTemp));
                    break;
                case updateSawFreq:
                     sawFreqShow.setText ("频率： " + String.format("%.2f", sawFreq)+ "  时间： " + String.valueOf ( freqTime ));
                     mFreqService.updateChart (freqTime,sawFreq);
                    break;
                default:
                    break;
            }
        }
    };



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main_system);


        mFreqChart = (LinearLayout)findViewById(R.id.frequency_curve);
        mFreqService = new ChartService ( this );
        mFreqService.setXYMultipleSeriesDataset ( " " );
        mFreqService.setXYMultipleSeriesRenderer (100, 100, " ", "时间", "频率",
                Color.RED, Color.RED, Color.RED, Color.BLACK);
        FreqChartView = mFreqService.getGraphicalView ();
        mFreqChart.addView ( FreqChartView,new LinearLayout.LayoutParams
                ( LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.MATCH_PARENT));






        sawTempShow = (TextView) findViewById(R.id.textViewSawTemp);
        sawFreqShow = (TextView) findViewById (R.id.textViewSawFreq);
        mStartFrequency = (Button ) findViewById ( R.id.button_startFrequency );
        threadfreq.start();//读取FrequencyDetector蓝牙数据



    }

    Thread threadfreq = new Thread() {
        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void run() {
            BluetoothDevice freq = SystemConfig.getFrequencyDetector ();
            //BluetoothDevice temp = SystemConfig.getTemperatureController();

            UUID uuid = UUID.fromString ( "00001101-0000-1000-8000-00805F9B34FB" );

            try {
                socketFreq = freq.createRfcommSocketToServiceRecord ( uuid );
            } catch (IOException e) {
                e.printStackTrace ();
                return;
            }

            try {
                socketFreq.connect ();
            } catch (IOException e) {
                e.printStackTrace ();
                return;
            }

            try {
                mFreqOutput = socketFreq.getOutputStream ();
            } catch (IOException e) {
                e.printStackTrace ();
            }

            try {
                mFreqInput = socketFreq.getInputStream ();
            } catch (IOException e) {
                e.printStackTrace ();
            }

            if (mFreqInput == null) {
                return;
            }

            byte[] buffer = new byte[1];
            int length;
            for (; ; ) {
                try {
                    length = mFreqInput.read ( buffer );
                } catch (IOException e) {
                    e.printStackTrace ();
                    return;
                }
                if (length <= 0) {
                    return;
                }
                //String tempStr;
               // tempStr = bytesToHexString ( buffer );
                int tempInt = getInt(buffer);
                if (tempInt == 0x9A && !freqCheckTag && readFreqTag ==0 && readTempTag == 0){
                    freqCheckTag = true;
                }
                else if (tempInt == 0xA9 && freqCheckTag){
                    readFreqTag =6;
                    readFreqFinishTag  = false;
                    freqCheckTag = false;
                }
                else if (tempInt ==0xAA && readTempTag == 0) {
                    readTempTag = 2;
                    readTempFinishTag = false;
                } else {

                    switch (readFreqTag)
                    {
                        case 6:
                            sensor2 = tempInt;
                            break;
                        case 5:
                            sensor2 = tempInt;
                            break;
                        case 4:
                            sensor2 = tempInt;
                            break;
                        case 3:
                            ref2 = tempInt;
                            break;
                        case 2:
                            ref1 = tempInt;
                            break;
                        case 1:
                            ref0 = tempInt;
                            readFreqFinishTag = true;
                            break;
                        default:
                            break;

                    }

                    readFreqTag--;
                    if (readFreqTag < 0)
                        readFreqTag = 0;

                    switch (readTempTag) {
                        case 2:
                            tempHigh = tempInt;
                            break;
                        case 1:
                            tempLow = tempInt;
                            readTempFinishTag = true;
                            break;
                        default:
                            break;
                    }



                    readTempTag--;
                    if (readTempTag < 0)
                        readTempTag = 0;
                }

                if (readTempFinishTag) {
                    sawTemp = tempHigh * 3.3024 + tempLow * 0.0129 - 251.43;

                    Message message = new Message ();
                    message.what = updateSawTemp;
                    handler.sendMessage ( message );
                    readTempFinishTag = false;
                }

                if(readFreqFinishTag){
                    sensor = (sensor2 << 16) + (sensor1 << 8) + sensor0;
                    ref = (ref2 << 16) + (ref1 << 8) + ref0;
                    if (ref != 0){
                        sawFreq = (100000000 * (double)sensor) / (double)ref;
                    }else {
                        sawFreq = 0;
                    }

                    freqTime = (double) (System.currentTimeMillis () - freqBeginTime)/1000;

                    Message message = new Message ();
                    message.what = updateSawFreq;
                    handler.sendMessage ( message );

                    readFreqFinishTag = false;
                }



            }


        }

    };

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void startFrequency(View view) {
        if (socketFreq.isConnected ()){
            if ( mStartFrequency.getText ().toString ().equals ( "开始计频" )){

                sendMessage ( 0xAA );
                sendMessage ( 0xAA );
                mStartFrequency.setText ( "停止计频" );
                freqBeginTime =System.currentTimeMillis ();




            } else if (mStartFrequency.getText ().toString ().equals ( "停止计频" ))
            {
                sendMessage(0xAB);
                sendMessage(0xAB);
                mStartFrequency.setText ( "开始计频" );
            }
        }
        else {
            Toast.makeText ( getApplicationContext (),"Bluetooth is not connected, Please turn back!",Toast.LENGTH_LONG ).show ();
        }



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

    public static int getInt(byte[] bytes)
    {
        return (0xff & bytes[0]);
    }

    public void sendMessage(int msg) {
        try {
            if (mFreqOutput == null) {
                Log.i("info", "null message");
                return;
            }
            // write message
            byte buffer = (byte) msg;
            mFreqOutput.write(buffer);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            try {
                if (mFreqOutput != null) {
                    mFreqOutput.flush();
                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }



}
