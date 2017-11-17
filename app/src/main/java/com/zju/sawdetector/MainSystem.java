package com.zju.sawdetector;
import android.annotation.SuppressLint;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import org.achartengine.GraphicalView;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Objects;
import java.util.UUID;


public class MainSystem extends AppCompatActivity {

    private InputStream mFreqInput;
    BluetoothSocket socketFreq;
    private OutputStream mFreqOutput;


    private InputStream mTempInput;
    BluetoothSocket socketTemp;
    private OutputStream mTempOutput;

    //SAW传感器温度读取
    int readTempTag = 0;
    boolean readTempFinishTag = false;
    int tempHigh, tempLow;
    double sawTemp;
    private TextView sawTempShow;


    //SAW传感器频率读取
    boolean freqCheckTag = false;
    int readFreqTag = 0;
    boolean readFreqFinishTag = false;
    long sensor2, sensor1, sensor0, ref2, ref1, ref0;

    long sensor, ref;

    double sawFreq;
    double sawDiff;
    private long freqBeginTime;
    private double freqTime;


    //温控板温度读取
    int TempTag;
    int Temp1, Temp2, Temp3, Temp4, Temp5, Temp6, Temp7, Temp8, Temp9, Temp10;
    double inletTemp, columnTemp, outletTemp, valveTemp, volume;
    private TextView inletTempShow;
    private TextView outletTempShow;
    private TextView columnTempShow;
    private TextView valveTempShow;
    private TextView volumeShow;


    Button mStartFrequency;
    Switch mSawTempWork;
    Switch mSawTempMode;
    CheckBox mSaveData;
    CheckBox mAutoMode;


    //频率数据保存
    FileOutputStream FreqSave;
    BufferedWriter FreqSaveWriter;



    public static final int updateSawTemp = 1;
    public static final int updateSawFreq = 2;

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


    //系统参数设置
    private EditText sawTempSet;
    private EditText inletTempSet;
    private EditText outletTempSet;
    private EditText columnTempSet;
    private EditText valveTempSet;
    private EditText volumeSet;


    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler () {
        @SuppressLint({"DefaultLocale", "SetTextI18n"})
        public void handleMessage(Message msg) {

            switch (msg.what) {
                case updateSawTemp:
                    sawTempShow.setText ( String.format ( "%.2f", sawTemp ) + "℃" );
                    inletTempShow.setText ( String.format ( "%.2f", inletTemp ) + "℃" );
                    outletTempShow.setText ( String.format ( "%.2f", outletTemp ) + "℃" );
                    valveTempShow.setText ( String.format ( "%.2f", valveTemp ) + "℃" );
                    columnTempShow.setText ( String.format ( "%.2f", columnTemp ) + "℃" );
                    volumeShow.setText ( String.format ( "%.2f", volume ) + "ml/min" );
                    break;
                case updateSawFreq:
                    mFreqService.updateChart ( freqTime, sawFreq );
                    mFreqDiffService.updateChart ( freqTime, sawDiff );
                    break;
                default:
                    break;
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate ( savedInstanceState );
        requestWindowFeature ( Window.FEATURE_NO_TITLE );
        setContentView ( R.layout.activity_main_system );


        mFreqChart = ( LinearLayout ) findViewById ( R.id.frequency_curve );
        mFreqService = new ChartService ( this );
        mFreqService.setXYMultipleSeriesDataset ( " " );
        mFreqService.setXYMultipleSeriesRenderer ( 20, 500000, "", "时间", "频率",
                Color.RED, Color.RED, Color.RED, Color.BLACK );
        FreqChartView = mFreqService.getGraphicalView ();
        mFreqChart.addView ( FreqChartView, new LinearLayout.LayoutParams
                ( LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT ) );
        mFreqService.updateChart ( 0, 0 );


        mFreqDiffChart = ( LinearLayout ) findViewById ( R.id.frequency_diff );
        mFreqDiffService = new ChartService ( this );
        mFreqDiffService.setXYMultipleSeriesDataset ( " " );
        mFreqDiffService.setXYMultipleSeriesRenderer ( 20, 10000, "", "时间", "频率差分",
                Color.RED, Color.RED, Color.RED, Color.BLACK );
        FreqDiffChartView = mFreqDiffService.getGraphicalView ();
        mFreqDiffChart.addView ( FreqDiffChartView, new LinearLayout.LayoutParams
                ( LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT ) );
        mFreqDiffService.updateChart ( 0, 0 );


        sawTempShow = ( TextView ) findViewById ( R.id.textViewSawTemp );
        valveTempShow = ( TextView ) findViewById ( R.id.textViewValveTemp );
        columnTempShow = ( TextView ) findViewById ( R.id.textViewColumnTemp );
        outletTempShow = ( TextView ) findViewById ( R.id.textViewOutletTemp );
        inletTempShow = ( TextView ) findViewById ( R.id.textViewInletTemp );
        volumeShow = ( TextView ) findViewById ( R.id.textViewVolume );

        sawTempSet = ( EditText ) findViewById ( R.id.editTextSawTemp );
        inletTempSet = ( EditText ) findViewById ( R.id.editTextInletTemp );
        outletTempSet = ( EditText ) findViewById ( R.id.editTextOutletTemp );
        columnTempSet = ( EditText ) findViewById ( R.id.editTextColumnTemp );
        valveTempSet = ( EditText ) findViewById ( R.id.editTextValveTemp );
        volumeSet = ( EditText ) findViewById ( R.id.editTextVolume );


        mStartFrequency = ( Button ) findViewById ( R.id.button_startFrequency );
        mSawTempWork = ( Switch ) findViewById ( R.id.switch_sawTempWork );
        mSawTempMode = ( Switch ) findViewById ( R.id.switch_sawTempMode );
        mSaveData = ( CheckBox ) findViewById ( R.id.checkbox_DataSave );
        mAutoMode = ( CheckBox ) findViewById ( R.id.checkbox_AutoMode );

        //FreqSave = new FileService ( MainSystem.this );


        updateFreq.start ();
        threadFreq.start ();//读取FrequencyDetector蓝牙数据
        threadTemp.start ();//读取TemperatureController蓝牙数据


    }


    //频率数据的实时更新
    Thread updateFreq = new Thread () {
        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void run() {

            for (; ; ) {
                try {
                    sleep ( 100 );
                } catch (InterruptedException e) {
                    e.printStackTrace ();
                }
                if (StartFrequencyTag && sawFreq > 100000 && sawFreq < 2000000) {
                    mFreqService.multipleSeriesRenderer.setRange ( new double[]{freqTime - 2, freqTime, 180000, 250000} );
                    mFreqDiffService.multipleSeriesRenderer.setRange ( new double[]{freqTime - 2, freqTime, 0, 10000} );
                    sawDiff = mFreqService.calculateDiff ();




                    Message message = new Message ();
                    message.what = updateSawFreq;
                    handler.sendMessage ( message );
                }


            }
        }


    };


    //计频蓝牙的连接与数据传输
    Thread threadFreq = new Thread () {
        @SuppressLint("DefaultLocale")
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
                if (tempInt == 0x9A && !freqCheckTag && readFreqTag == 0 && readTempTag == 0) {
                    freqCheckTag = true;
                } else if (tempInt == 0xA9 && freqCheckTag) {
                    readFreqTag = 6;
                    readFreqFinishTag = false;
                    freqCheckTag = false;
                } else if (tempInt == 0xAA && readTempTag == 0) {
                    readTempTag = 2;
                    readTempFinishTag = false;
                } else {

                    switch (readFreqTag) {
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

                if (readFreqFinishTag) {
                    sensor = (sensor2 << 16) + (sensor1 << 8) + sensor0;
                    ref = (ref2 << 16) + (ref1 << 8) + ref0;



                    if (ref != 0) {
                        sawFreq = (100000000 * (( double ) sensor)) / (( double ) ref);
                    } else {
                        sawFreq = 0;
                    }

                    freqTime = ( double ) (System.currentTimeMillis () - freqBeginTime) / 1000;


                    if (mSaveData.isChecked ()) {

                        try {
                            FreqSaveWriter.write ( "Time： " +String.format ( "%.2f", freqTime )+" Frequency: "+String.format ( "%.2f", sawFreq )+"\n"
                            );
                        } catch (IOException e) {
                            e.printStackTrace ();
                        }

                    }

                    readFreqFinishTag = false;




                }


            }


        }

    };


    //温控蓝牙的连接与数据传输
    Thread threadTemp = new Thread () {
        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void run() {
            //BluetoothDevice freq = SystemConfig.getFrequencyDetector ();
            BluetoothDevice temp = SystemConfig.getTemperatureController ();

            UUID uuid = UUID.fromString ( "00001101-0000-1000-8000-00805F9B34FB" );

            try {
                socketTemp = temp.createRfcommSocketToServiceRecord ( uuid );
            } catch (IOException e) {
                e.printStackTrace ();
                return;
            }

            try {
                socketTemp.connect ();
            } catch (IOException e) {
                e.printStackTrace ();
                return;
            }

            try {
                mTempOutput = socketTemp.getOutputStream ();
            } catch (IOException e) {
                e.printStackTrace ();
            }

            try {
                mTempInput = socketTemp.getInputStream ();
            } catch (IOException e) {
                e.printStackTrace ();
            }

            if (mTempInput == null) {
                return;
            }

            byte[] buffer = new byte[1];
            int length;
            for (; ; ) {
                try {
                    length = mTempInput.read ( buffer );
                } catch (IOException e) {
                    e.printStackTrace ();
                    return;
                }
                if (length <= 0) {
                    return;
                }
                int tempInt = (buffer[0] & 0xff);
                //System.out.println ( tempInt  );

                if (TempTag == 0)
                    switch (tempInt) {
                        case 0xAA:
                            TempTag = 15;
                            break;
                        case 0xAB:
                            TempTag = 12;
                            break;
                        case 0xAC:
                            TempTag = 9;
                            break;
                        case 0xAD:
                            TempTag = 6;
                            break;
                        case 0xAE:
                            TempTag = 3;
                            break;

                        default:
                            TempTag = 0;
                            break;
                    }
                else {
                    switch (TempTag) {
                        case 15:
                            Temp1 = tempInt;
                            TempTag--;
                            break;
                        case 14:
                            Temp2 = tempInt;
                            TempTag--;
                            break;
                        case 13:
                            outletTemp = ((Temp1 << 8) + Temp2) * 0.0129 - 251.43;
                            TempTag = 0;
                            //System.out.println ( outletTemp );
                            break;
                        case 12:
                            Temp3 = tempInt;
                            TempTag--;
                            break;
                        case 11:
                            Temp4 = tempInt;
                            TempTag--;
                            break;
                        case 10:
                            inletTemp = ((Temp3 << 8) + Temp4) * 0.0129 - 251.43;
                            TempTag = 0;
                            //System.out.println ( inletTemp );
                            break;
                        case 9:
                            Temp5 = tempInt;
                            TempTag--;
                            break;
                        case 8:
                            Temp6 = tempInt;
                            TempTag--;
                            break;
                        case 7:
                            valveTemp = ((Temp5 << 8) + Temp6) * 0.0129 - 251.43;
                            TempTag = 0;
                            //System.out.println ( valveTemp );
                            break;
                        case 6:
                            Temp7 = tempInt;
                            TempTag--;
                            break;
                        case 5:
                            Temp8 = tempInt;
                            TempTag--;
                            break;
                        case 4:
                            columnTemp = ((Temp7 << 8) + Temp8) * 0.0129 - 251.43;
                            TempTag = 0;
                            //System.out.println ( columnTemp );
                            break;
                        case 3:
                            Temp9 = tempInt;
                            TempTag--;
                            break;
                        case 2:
                            Temp10 = tempInt;
                            TempTag--;
                            break;
                        case 1:
                            volume = ((Temp9 << 8) + Temp10) / 1000;
                            TempTag = 0;
                            // System.out.println ( volume );


                            //   Message message = new Message ();
                            //   message.what = updateSystemTemp;
                            //  handler.sendMessage ( message );

                            break;

                        default:
                            TempTag = 0;
                            break;
                    }

                }


            }


        }

    };


    //Button 开始计频的onclick函数;
    @SuppressLint("SimpleDateFormat")
    @RequiresApi(api = Build.VERSION_CODES.O)
    public void startFrequency(View view) {
        if (socketFreq.isConnected ()) {
            if (mStartFrequency.getText ().toString ().equals ( "开始检测" )) {


                //
                StartFrequencyTag = true;

                sendFreqMessage ( 0xAA );
                sendFreqMessage ( 0xAA );
                mStartFrequency.setText ( "停止检测" );
                freqBeginTime = System.currentTimeMillis ();




                mFreqChart.removeAllViews ();
                mFreqService.mSeries.clear ();
                mFreqService.mSeries.clearSeriesValues ();
                mFreqService.setXYMultipleSeriesDataset ( " " );
                mFreqService.setXYMultipleSeriesRenderer ( 10, 10, " ", "时间", "频率",
                        Color.RED, Color.RED, Color.RED, Color.BLACK );
                FreqChartView = mFreqService.getGraphicalView ();
                mFreqChart.addView ( FreqChartView, new LinearLayout.LayoutParams
                        ( LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT ) );

                mFreqDiffChart.removeAllViews ();
                mFreqDiffService.mSeries.clear ();
                mFreqService.mSeries.clearSeriesValues ();
                mFreqDiffService.setXYMultipleSeriesDataset ( " " );
                mFreqDiffService.setXYMultipleSeriesRenderer ( 10, 10, " ", "时间", "频率差分",
                        Color.RED, Color.RED, Color.RED, Color.BLACK );
                FreqDiffChartView = mFreqDiffService.getGraphicalView ();
                mFreqDiffChart.addView ( FreqDiffChartView, new LinearLayout.LayoutParams
                        ( LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT ) );

                if (mSaveData.isChecked ())
                {
                    try {

                        SimpleDateFormat sDateFormat;
                        sDateFormat = new SimpleDateFormat ("yyyy-MM-dd    hh:mm:ss");
                        String DateTitle = sDateFormat.format(new java.util.Date());

                        File path = new File ( Environment.getExternalStorageDirectory () + "/Android"+"/data"+"/com.zju.sawdetector"+File.separator);
                        if (!path.exists ())
                        {
                            path.mkdirs();
                        }
                        File file = new File(path,DateTitle+".txt");
                       // Log.d ( "saveToSDCard", String.valueOf ( file ) );
                       // Log.d ( "saveToSDCard", String.valueOf ( file.exists () ) );
                        if (!file.exists ())
                        {
                            try {
                                Boolean Tag = file.createNewFile();
                                Log.d ( "saveToSDCard", String.valueOf ( Tag ) );
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }

                        FreqSave =  new FileOutputStream(file);
                        FreqSaveWriter = new BufferedWriter ( new OutputStreamWriter ( FreqSave ) );


                    } catch (FileNotFoundException e) {
                        e.printStackTrace ();
                    }
                }

            } else if (mStartFrequency.getText ().toString ().equals ( "停止检测" )) {


                sendFreqMessage ( 0xAB );
                sendFreqMessage ( 0xAB );
                mStartFrequency.setText ( "开始检测" );
                StartFrequencyTag = false;
                mFreqService.multipleSeriesRenderer.setRange ( new double[]{0, freqTime, mFreqService.mSeries.getMinY (), mFreqService.mSeries.getMaxY ()} );
                mFreqService.mGraphicalView.repaint ();

                mFreqDiffService.multipleSeriesRenderer.setRange ( new double[]{0, freqTime, 0, mFreqDiffService.mSeries.getMaxY ()} );
                mFreqDiffService.mGraphicalView.repaint ();

                if (mSaveData.isChecked ()) {
                    try {
                        FreqSaveWriter.close ();
                        FreqSave.close ();
                    } catch (IOException e) {
                        e.printStackTrace ();
                    }
                }


            }
        } else {
            Toast.makeText ( getApplicationContext (), "计频蓝牙未连接!", Toast.LENGTH_LONG ).show ();
        }


    }

    //Button 系统参数设置的onclick函数;
    public void setParameters(View view) {

        if (socketFreq.isConnected ()) {
            //传感器温度值设置
            int temp;
            if (!Objects.equals ( sawTempSet.getText ().toString (), "" )) {
                temp = Integer.parseInt ( sawTempSet.getText ().toString () );
                if (temp < 200 && temp > 0) {
                    sendFreqMessage ( 1 );
                    sendFreqMessage ( temp );
                } else {
                    Toast.makeText ( getApplicationContext (), "传感器温度设置不合法!", Toast.LENGTH_SHORT ).show ();
                }
            } else {
                Toast.makeText ( getApplicationContext (), "请输入传感器目标温度", Toast.LENGTH_SHORT ).show ();
            }

        } else {
            Toast.makeText ( getApplicationContext (), "计频蓝牙未连接!", Toast.LENGTH_LONG ).show ();
        }

        if (socketTemp.isConnected ()) {
            int temp;
            if (!Objects.equals ( inletTempSet.getText ().toString (), "" )) {
                temp = Integer.parseInt ( inletTempSet.getText ().toString () );
                if (temp < 200 && temp > 0) {
                    sendTempMessage ( 2 );
                    sendTempMessage ( temp );
                } else {
                    Toast.makeText ( getApplicationContext (), "进口温度设置不合法!", Toast.LENGTH_SHORT ).show ();
                }
            } else {
                Toast.makeText ( getApplicationContext (), "请输入进口目标温度", Toast.LENGTH_SHORT ).show ();
            }

            if (!Objects.equals ( valveTempSet.getText ().toString (), "" )) {
                temp = Integer.parseInt ( valveTempSet.getText ().toString () );
                if (temp < 200 && temp > 0) {
                    sendTempMessage ( 3 );
                    sendTempMessage ( temp );
                } else {
                    Toast.makeText ( getApplicationContext (), "阀体温度设置不合法!", Toast.LENGTH_SHORT ).show ();
                }
            } else {
                Toast.makeText ( getApplicationContext (), "请输入阀体目标温度", Toast.LENGTH_SHORT ).show ();
            }

            if (!Objects.equals ( outletTempSet.getText ().toString (), "" )) {
                temp = Integer.parseInt ( outletTempSet.getText ().toString () );
                if (temp < 200 && temp > 0) {
                    sendTempMessage ( 4 );
                    sendTempMessage ( temp );
                } else {
                    Toast.makeText ( getApplicationContext (), "喷口温度设置不合法!", Toast.LENGTH_SHORT ).show ();
                }
            } else {
                Toast.makeText ( getApplicationContext (), "请输入喷口目标温度", Toast.LENGTH_SHORT ).show ();
            }

            if (!Objects.equals ( columnTempSet.getText ().toString (), "" )) {
                temp = Integer.parseInt ( columnTempSet.getText ().toString () );
                if (temp < 200 && temp > 0) {
                    sendTempMessage ( 1 );
                    sendTempMessage ( temp );
                } else {
                    Toast.makeText ( getApplicationContext (), "毛细管温度设置不合法!", Toast.LENGTH_SHORT ).show ();
                }
            } else {
                Toast.makeText ( getApplicationContext (), "请输入毛细管目标温度", Toast.LENGTH_SHORT ).show ();
            }

            if (!Objects.equals ( volumeSet.getText ().toString (), "" )) {
                temp = Integer.parseInt ( volumeSet.getText ().toString () );

                temp = temp * 200;
                int v2 = temp % 256;
                int v1 = temp / 256;

                sendTempMessage ( 0x29 );
                sendTempMessage ( v1 );
                sendTempMessage ( v2 );
            } else {
                Toast.makeText ( getApplicationContext (), "请输入毛细管目标温度", Toast.LENGTH_SHORT ).show ();
            }


        } else {
            Toast.makeText ( getApplicationContext (), "温控蓝牙未连接!", Toast.LENGTH_LONG ).show ();
        }

    }


    //Switch 温控片是否工作的onclick函数;
    public void sawTempWork(View view) {
        if (socketFreq.isConnected ()) {
            if (mSawTempWork.isChecked ()) {
                sendFreqMessage ( 0x06 );
                sendFreqMessage ( 0 );

            } else {
                sendFreqMessage ( 0x05 );
                sendFreqMessage ( 0 );
            }
        }


    }

    //Switch 温控片工作模式的onclick函数;
    public void sawTempMode(View view) {
        if (socketFreq.isConnected ()) {
            if (mSawTempMode.isChecked ()) {
                sendFreqMessage ( 0x08 );
                sendFreqMessage ( 0 );

            } else {
                sendFreqMessage ( 0x07 );
                sendFreqMessage ( 0 );
            }
        }


    }


    //进入数据分析界面
    public void DataAnalysis(View view) {

        Intent intent = new Intent ( MainSystem.this, DataAnalysis.class );
        startActivity ( intent );

    }

    //进入流程设置界面
    public void WorkflowSetting(View view) {
        Intent intent = new Intent ( MainSystem.this, WorkflowSetting.class );
        startActivity ( intent );
    }


    public void sendFreqMessage(int msg) {
        try {
            if (mFreqOutput == null) {
                Log.i ( "info", "null message" );
                return;
            }
            // write message
            byte buffer = ( byte ) msg;
            mFreqOutput.write ( buffer );
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace ();
        } finally {
            try {
                if (mFreqOutput != null) {
                    mFreqOutput.flush ();
                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace ();
            }
        }
    }

    public void sendTempMessage(int msg) {
        try {
            if (mTempOutput == null) {
                Log.i ( "info", "null message" );
                return;
            }
            // write message
            byte buffer = ( byte ) msg;
            mTempOutput.write ( buffer );
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace ();
        } finally {
            try {
                if (mTempOutput != null) {
                    mTempOutput.flush ();
                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace ();
            }
        }
    }
}
