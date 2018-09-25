package com.example.k.ctrl;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import java.util.Timer;
import java.util.TimerTask;

import android_serialport_api.ModbusMaster;


public class MainActivity extends AppCompatActivity {


    ModbusMaster modbusMaster = new ModbusMaster();
    private boolean send03Or16Flag;
    private Timer timer1 = new Timer();
    private TimerTask task1;

    TextView wenDuDisplay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        modbusMaster.start();
        timerTask();
        initView();
    }

    /**
     * 初始化控件
     */
    private void initView(){

        wenDuDisplay = findViewById(R.id.tv_wendu);
    }

    /**
     * 定时器任务
     */
    private void timerTask(){

        if (timer1!=null){
            if (task1!=null){
                task1.cancel();
            }
        }

        task1=new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {      // UI thread

                    public void run() {
                        modbusMonsterSend();
                        uiUpdate();
                    }
                });
            }
        };
        timer1.schedule(task1, 100, 100);
    }

    /**
     * 向串口发送数据
     */
    private void modbusMonsterSend(){

        if (send03Or16Flag){
            modbusMaster.sendDataMaster03((byte) 0,(byte)12);
            send03Or16Flag = false;
        }else {
            modbusMaster.sendDataMaster16((byte)16,(byte)7);
            send03Or16Flag = true;
        }
    }

    /**
     * 更新UI数据
     */
    private void uiUpdate(){

        wenDuDisplay.setText(modbusMaster.wenDu_valueIn+"");

    }
}


