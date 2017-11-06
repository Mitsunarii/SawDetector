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


import java.util.ArrayList;
import java.util.List;
import java.util.UUID;





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
    long sensor2,sensor1,sensor0,ref2,ref1,ref0;

    long sensor,ref;

    double sawFreq;
    double sawDiff;
    double sawDiffFilter[];
    private long freqBeginTime;
    private double freqTime;


    Button mStartFrequency;


    public static final int updateSawTemp = 1;
    public static final int updateSawFreq =2;

    //画图
    LinearLayout mFreqChart;
    private GraphicalView FreqChartView;
    private ChartService mFreqService;
   // private double minY;
    //private double maxY;
    Boolean StartFrequencyTag = false;

    LinearLayout mFreqDiffChart;
    private GraphicalView FreqDiffChartView;
    private ChartService mFreqDiffService;





    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler () {
        @SuppressLint({"DefaultLocale", "SetTextI18n"})
        public void handleMessage(Message msg){

            switch (msg.what){
                case updateSawTemp:
                     sawTempShow.setText(String.format("%.2f", sawTemp));
                    break;
                case updateSawFreq:
                     mFreqService.updateChart (freqTime,sawFreq);
                     mFreqDiffService.updateChart ( freqTime,sawDiff);

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
        mFreqService.setXYMultipleSeriesRenderer (20, 500000, "", "时间", "频率",
                Color.RED, Color.RED, Color.RED, Color.BLACK);
        FreqChartView = mFreqService.getGraphicalView ();
        mFreqChart.addView ( FreqChartView,new LinearLayout.LayoutParams
                ( LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.MATCH_PARENT));
        mFreqService.updateChart ( 0,0 );


        mFreqDiffChart = (LinearLayout)findViewById(R.id.frequency_diff);
        mFreqDiffService = new ChartService ( this );
        mFreqDiffService.setXYMultipleSeriesDataset ( " " );
        mFreqDiffService.setXYMultipleSeriesRenderer (20, 10000, "", "时间", "频率差分",
                Color.RED, Color.RED, Color.RED, Color.BLACK);
        FreqDiffChartView = mFreqDiffService.getGraphicalView ();
        mFreqDiffChart.addView ( FreqDiffChartView,new LinearLayout.LayoutParams
                ( LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.MATCH_PARENT));
        mFreqDiffService.updateChart ( 0,0 );



        sawTempShow = (TextView) findViewById(R.id.textViewSawTemp);
       // sawFreqShow = (TextView) findViewById (R.id.textViewSawFreq);
        mStartFrequency = (Button ) findViewById ( R.id.button_startFrequency );


        updateFreq.start ();
        threadFreq.start();//读取FrequencyDetector蓝牙数据




    }

    Thread updateFreq = new Thread ( ){
        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void run() {

                for (; ; ) {
                    try {
                        sleep ( 50 );
                    } catch (InterruptedException e) {
                        e.printStackTrace ();
                    }
                    if (StartFrequencyTag)
                    {
                       /* if (maxY < sawFreq)
                            maxY = sawFreq;
                        else if (minY > sawFreq)
                            minY = sawFreq;
                        mFreqService.multipleSeriesRenderer.setRange (new double[] { freqTime-2, freqTime, 150000, 250000 });
                        mFreqDiffService.multipleSeriesRenderer.setRange (new double[] { freqTime-2, freqTime, 0, 15000 });
                        sawDiff = mFreqService.calculateDiff ();*/
                        mFreqService.multipleSeriesRenderer.setRange (new double[] { freqTime-2, freqTime, 180000, 250000 });
                        mFreqDiffService.multipleSeriesRenderer.setRange (new double[] { freqTime-2, freqTime, 0, 10000 });
                        sawDiff = mFreqService.calculateDiff ();

                        Message message = new Message ();
                        message.what = updateSawFreq;
                        handler.sendMessage ( message );
                    }


                }
            }




    };







    Thread threadFreq = new Thread() {
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

                int tempInt = (buffer[0] & 0xff);
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
                            sensor1 = tempInt;
                            break;
                        case 4:
                            sensor0 = tempInt;
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
                    sawTemp = ((tempHigh << 8) + tempLow) * 0.0129 - 251.43;

                    Message message = new Message ();
                    message.what = updateSawTemp;
                    handler.sendMessage ( message );
                    readTempFinishTag = false;
                }

                if(readFreqFinishTag){
                    sensor = (sensor2 << 16) + (sensor1 << 8) + sensor0;
                    ref = (ref2 << 16) + (ref1 << 8) + ref0;



                    //System.out.println (sensor + "   " + ref );

                    if (ref != 0){
                        sawFreq = (100000000*((double)sensor)) / ((double)ref);
                    }else {
                        sawFreq = 0;
                    }

                    freqTime = (double) (System.currentTimeMillis () - freqBeginTime)/1000;

                    readFreqFinishTag = false;



                }



            }


        }

    };

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void startFrequency(View view) {
        if (socketFreq.isConnected ()){
            if ( mStartFrequency.getText ().toString ().equals ( "开始计频" )){


                //
                StartFrequencyTag = true;

                sendMessage ( 0xAA );
                sendMessage ( 0xAA );
                mStartFrequency.setText ( "停止计频" );
                freqBeginTime =System.currentTimeMillis ();

                mFreqChart.removeAllViews ();
                mFreqService.multipleSeriesDataset.removeSeries ( mFreqService.mSeries );
                mFreqService.mSeries.clear ();
                mFreqService.setXYMultipleSeriesDataset ( " " );
                mFreqService.setXYMultipleSeriesRenderer (10, 10, " ", "时间", "频率",
                        Color.RED, Color.RED, Color.RED, Color.BLACK);
                FreqChartView = mFreqService.getGraphicalView ();
                mFreqChart.addView ( FreqChartView,new LinearLayout.LayoutParams
                        ( LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.MATCH_PARENT));


                mFreqDiffChart.removeAllViews ();
                mFreqDiffService.multipleSeriesDataset.removeSeries ( mFreqService.mSeries );
                mFreqDiffService.mSeries.clear ();
                mFreqDiffService.setXYMultipleSeriesDataset ( " " );
                mFreqDiffService.setXYMultipleSeriesRenderer (10, 10, " ", "时间", "频率差分",
                        Color.RED, Color.RED, Color.RED, Color.BLACK);
                FreqDiffChartView = mFreqDiffService.getGraphicalView ();
                mFreqDiffChart.addView ( FreqDiffChartView,new LinearLayout.LayoutParams
                        ( LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.MATCH_PARENT));



                //updatefreq.start ();

                //minY = 2000000;
               // maxY = 0;



            } else if (mStartFrequency.getText ().toString ().equals ( "停止计频" ))
            {


                sendMessage(0xAB);
                sendMessage(0xAB);
                mStartFrequency.setText ( "开始计频" );
                //updatefreq.interrupt ();
                StartFrequencyTag = false;
                mFreqService.multipleSeriesRenderer.setRange(new double[] { 0, freqTime, mFreqService.mSeries.getMinY (), mFreqService.mSeries.getMaxY ()});
                mFreqService.mGraphicalView.repaint ();

                mFreqDiffService.multipleSeriesRenderer.setRange(new double[] { 0, freqTime, 0, mFreqDiffService.mSeries.getMaxY () });
                mFreqDiffService.mGraphicalView.repaint ();



            }
        }
        else {
            Toast.makeText ( getApplicationContext (),"Bluetooth is not connected, Please turn back!",Toast.LENGTH_LONG ).show ();
        }



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
