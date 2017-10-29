package com.zju.sawdetector;

import android.bluetooth.BluetoothDevice;

/**
 * Created by Mitsunari on 2017/10/29.
 */

public class SystemConfig {
    private BluetoothDevice mTemperatureController;
    private BluetoothDevice mFrequencyDetector;

    private static SystemConfig self = new SystemConfig();

    private SystemConfig() {
    }

    public static BluetoothDevice getTemperatureController() {
        return self.mTemperatureController;
    }

    public static void setTemperatureController(BluetoothDevice controller) {
        self.mTemperatureController = controller;
    }

    public static BluetoothDevice getFrequencyDetector() {
        return self.mFrequencyDetector;
    }

    public static void setFrequencyDetector(BluetoothDevice detector) {
        self.mFrequencyDetector = detector;
    }
}
