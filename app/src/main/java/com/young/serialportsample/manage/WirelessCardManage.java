package com.young.serialportsample.manage;

import android.util.Log;

import com.young.serialport.SerialPort;
import com.young.serialportsample.observable.FrequencyBandObservable;
import com.young.serialportsample.util.HexUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.security.InvalidParameterException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WirelessCardManage {

    private static final String TAG = "WirelessCardManage";

    private static WirelessCardManage sSerialPortManage;

    private SerialPort mSerialPort;
    private OutputStream mOutputStream;
    private InputStream mInputStream;
    private ReadThread mReadThread;
    private ExecutorService mSendService;
    private String mPort = "/dev/s3c2410_serial0";
    private int mBaudRate = 9600;
    private boolean isOpen = false;
    private int mDelay = 50;

    private WirelessCardManage() {
    }

    public static WirelessCardManage getInstance() {
        if (sSerialPortManage == null) {
            synchronized (WirelessCardManage.class) {
                if (sSerialPortManage == null) {
                    sSerialPortManage = new WirelessCardManage();
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

    public void sendQueue(final byte[] bytes, final byte address) {
        if (!isOpen()) return;
        mSendService.execute(new Runnable() {
            @Override
            public void run() {
                send(formatResult(bytes, address));
            }
        });
    }

    private void send(byte[] bytes) {
        synchronized (WirelessCardManage.class) {
            for (int i = 0; i < 3; i++) {
                try {
                    mOutputStream.write(bytes);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    public void send(byte[] bytes, byte address) {
        if (!isOpen()) return;
        send(formatResult(bytes, address));
    }

    private byte[] formatResult(byte[] bytes, byte address) {
        int len = bytes.length + 2;
        byte[] result = new byte[len + 3];
        result[0] = (byte) 0xAA;
        result[1] = (byte) 0x55;
        result[2] = (byte) len;
        result[3] = address;
        result[4] = (byte) 0xF8;
        System.arraycopy(bytes, 0, result, 5, bytes.length);
        return result;
    }

    private class ReadThread extends Thread {

        private final byte[] mMsgHead = new byte[]{(byte) 0xAA, 0x55};

        @Override
        public void run() {
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            int len;
            int bodyLen = 0;
            byte[] head = new byte[2];
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
                    byte[] buf = new byte[available];
                    len = mInputStream.read(buf);
                    buffer.put(buf, 0, len);
                    buffer.flip();
                    while (true) {
                        buffer.position(0);
                        int limit = buffer.limit();
                        if (bodyLen > 0) {
                            if (limit >= bodyLen + 3) {
                                byte[] bytes = new byte[3];
                                buffer.get(bytes);
                                bodyLen = bytes[2];
                                buffer.position(0);
                                bytes = new byte[bodyLen + 3];
                                buffer.get(bytes);

                                buffer.compact();
                                buffer.flip();
                                bodyLen = 0;

                                ComBean ComRecData = new ComBean(mPort, bytes);
                                onDataReceived(ComRecData);
                            } else {
                                break;
                            }
                        } else {
                            if (limit >= 3) {
                                byte[] bytes = new byte[3];
                                buffer.get(bytes);
                                System.arraycopy(bytes, 0, head, 0, 2);
                                if (Arrays.equals(head, mMsgHead)) {
                                    bodyLen = bytes[2];
                                    if (bodyLen == 0) {
                                        buffer.compact();
                                        buffer.flip();
                                    }
                                } else {
                                    buffer.position(1);
                                    buffer.compact();
                                    buffer.flip();
                                }
                            } else {
                                break;
                            }
                        }
                    }
                    buffer.position(0);
                    buffer.compact();
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }
            }
        }
    }

    private void onDataReceived(ComBean comRecData) {
        Log.d(TAG, "onDataReceived: " + HexUtil.bytes2String(comRecData.mRec));
        Log.d(TAG, "mCommand: " + HexUtil.byte2String(comRecData.mCommand));
        Log.d(TAG, "mAddress: " + HexUtil.byte2Int(comRecData.mAddress));
        try {
            Log.d(TAG, "onDataReceived: " + new String(comRecData.mRec, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "onDataReceived: " + comRecData.mRec.length);

        if (comRecData.mCommand == (byte) 0xCC) {
            FrequencyBandObservable.getInstance().notifyDataChange(HexUtil.byte2Int(comRecData.mTarget), HexUtil.byte2Int(comRecData.mAddress));
        }
    }

    public class ComBean {
        public byte[] mRec;
        public byte mAddress;
        public byte mTarget;
        public byte mLen;
        public byte[] mHead = new byte[2];
        public byte mCommand;
        public String mRecTime;
        public String mComPort;

        public ComBean(String port, byte[] buffer) {
            mComPort = port;
            mLen = buffer[2];
            mTarget = buffer[3];
            mAddress = buffer[4];
            mCommand = buffer[5];
            mRec = new byte[buffer.length - 6];
            System.arraycopy(buffer, 0, mHead, 0, 2);
            System.arraycopy(buffer, 6, mRec, 0, buffer.length - 6);
            SimpleDateFormat sDateFormat = new SimpleDateFormat("hh:mm:ss");
            mRecTime = sDateFormat.format(new java.util.Date());
        }
    }
}
