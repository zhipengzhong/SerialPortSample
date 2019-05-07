package com.young.serialportsample;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.young.serialport.SerialPortFinder;
import com.young.serialportsample.manage.SerialPortManage;
import com.young.serialportsample.util.HexUtil;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private Spinner mPort;
    private Spinner mBaudRate;
    private EditText mSendContent;
    private Button mSend;
    public static String[] sBaudRateCom = new String[]{"50"
            , "75"
            , "110"
            , "134"
            , "150"
            , "200"
            , "300"
            , "600"
            , "1200"
            , "1800"
            , "2400"
            , "4800"
            , "9600"
            , "19200"
            , "38400"
            , "57600"
            , "115200"
            , "230400"
            , "460800"
            , "500000"
            , "576000"
            , "921600"
            , "1000000"
            , "1152000"
            , "1500000"
            , "2000000"
            , "2500000"
            , "3000000"
            , "3500000"
            , "4000000"};
    private int mBaudRatePosition;
    private SerialPortFinder mSerialPortFinder;
    private String[] mEntryValues;
    private int mPosition;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        setListener();
    }

    private void initView() {
        mPort = findViewById(R.id.port);
        mBaudRate = findViewById(R.id.baud_rate);
        mSendContent = findViewById(R.id.send_content);
        mSend = findViewById(R.id.send);
    }


    private void setListener() {
        mSerialPortFinder = new SerialPortFinder();
        mEntryValues = mSerialPortFinder.getAllDevicesPath();
        List<String> allDevices = new ArrayList<String>();
        for (int i = 0; i < mEntryValues.length; i++) {
            allDevices.add(mEntryValues[i]);
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, allDevices);
        mPort.setAdapter(adapter);
        mPort.setSelection(mPosition);
        mPort.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mPosition = position;
                connectPrinter();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, sBaudRateCom);
        mBaudRate.setAdapter(adapter);
        mBaudRate.setSelection(mBaudRatePosition);
        mBaudRate.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mBaudRatePosition = position;
                connectPrinter();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });


        mSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SerialPortManage.getInstance().sendQueue(HexUtil.hexS2Bytes(mSendContent.getText().toString().trim()));
            }
        });
    }

    private void connectPrinter() {
        try {
            SerialPortManage.getInstance().open(mEntryValues[mPosition], sBaudRateCom[mBaudRatePosition]);
            Toast.makeText(this, "打开串口成功！", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "打开串口失败！", Toast.LENGTH_SHORT).show();
        }
    }

}
