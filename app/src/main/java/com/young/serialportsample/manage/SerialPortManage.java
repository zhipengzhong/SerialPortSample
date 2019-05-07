package com.young.serialportsample.manage;

import android.util.Log;

import com.young.serialport.SerialPort;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidParameterException;
import java.text.SimpleDateFormat;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SerialPortManage {

    private static final String TAG = "SerialPortManage";

    private static SerialPortManage sSerialPortManage;

    private SerialPort mSerialPort;
    private OutputStream mOutputStream;
    private InputStream mInputStream;
    private ReadThread mReadThread;
    private ExecutorService mSendService;
    private String mPort = "/dev/s3c2410_serial0";
    private int mBaudRate = 9600;
    private boolean isOpen = false;
    private int mDelay = 50;

    private SerialPortManage() {
    }

    public static SerialPortManage getInstance() {
        if (sSerialPortManage == null) {
            synchronized (SerialPortManage.class) {
                if (sSerialPortManage == null) {
                    sSerialPortManage = new SerialPortManage();
                }
            }
        }
        return sSerialPortManage;
    }

    public void open(String port, int baudRate) throws SecurityException, IOException, InvalidParameterException {
        this.mPort = port;
        this.mBaudRate = baudRate;
        open();
    }

    public void open(String port, String baudRate) throws SecurityException, IOException, InvalidParameterException {
        open(port, Integer.parseInt(baudRate));
    }


    public void open(String port) throws SecurityException, IOException, InvalidParameterException {
        open(port, 9600);
    }


    private void open() throws SecurityException, IOException, InvalidParameterException {
        if (isOpen()) {
            close();
        }
        mSerialPort = new SerialPort(new File(mPort), mBaudRate, 0);
        mOutputStream = mSerialPort.getOutputStream();
        mInputStream = mSerialPort.getInputStream();
        mReadThread = new ReadThread();
        mReadThread.start();
        mSendService = Executors.newSingleThreadExecutor();
        isOpen = true;
    }

    public void close() {
        if (mReadThread != null) {
            mReadThread.interrupt();
            try {
                mReadThread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            mReadThread = null;
        }
        if (mSendService != null) {
            mSendService.shutdownNow();
            mSendService = null;
        }
        if (mSerialPort != null) {
            mSerialPort.close();
            mSerialPort = null;
        }
        isOpen = false;
    }

    public boolean isOpen() {
        return isOpen;
    }

    public void sendQueue(final byte[] bytes) {
        if (!isOpen()) return;
        mSendService.execute(new Runnable() {
            @Override
            public void run() {
                send(bytes);
            }
        });
    }

    private void send(byte[] bytes) {
        synchronized (SerialPortManage.class) {
            try {
                mOutputStream.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class ReadThread extends Thread {

        @Override
        public void run() {
            while (!isInterrupted()) {
                try {
                    if (mInputStream == null) return;
                    int available = mInputStream.available();
                    if (available == 0) {
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                        continue;
                    }
                    byte[] buffer = new byte[512];
                    int size = mInputStream.read(buffer);
                    if (size > 0) {
                        ComBean ComRecData = new ComBean(mPort, buffer, size);
                        onDataReceived(ComRecData);
                    }
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } catch (Throwable e) {
                    e.printStackTrace();
                    return;
                }
            }
        }
    }

    private void onDataReceived(ComBean comRecData) {
        Log.d(TAG, "onDataReceived: port:" + comRecData.mComPort);
        Log.d(TAG, "onDataReceived: time:" + comRecData.mRecTime);
        Log.d(TAG, "onDataReceived: data:" + new String(comRecData.mRec));
    }

    public class ComBean {
        public byte[] mRec;
        public String mRecTime;
        public String mComPort;

        public ComBean(String port, byte[] buffer, int size) {
            mComPort = port;
            mRec = new byte[size];
            System.arraycopy(buffer, 0, mRec, 0, size);
            SimpleDateFormat sDateFormat = new SimpleDateFormat("hh:mm:ss");
            mRecTime = sDateFormat.format(new java.util.Date());
        }
    }
}
