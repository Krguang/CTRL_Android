package android_serialport_api;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;

public class ModbusMaster extends Thread {

    private ArrayList<Byte> rxTemp = new ArrayList<Byte>();
    private Timer timer10ms=new Timer();

    private final String uartPath = "/dev/ttyS3";
    private final int SLAV_ADD = 1;


    private OutputStream mOutputStream = null;
    private InputStream mInputStream = null;
    private SerialPort mserialPort = null;

    int[] regHodingBuf = new int[256];
    int[] receiveBuff = new int[256];


    


    public ModbusMaster() {


        try {
            mserialPort = getSerialPort();
        } catch (InvalidParameterException e) {

            e.printStackTrace();
        } catch (SecurityException e) {

            e.printStackTrace();
        } catch (IOException e) {

            e.printStackTrace();
        }
        mInputStream = mserialPort.getInputStream();
        mOutputStream = mserialPort.getOutputStream();

    }

    public void sendDataMaster03(byte startAdd,byte num) {

        byte[] txBuf = new byte[16];
        CRC_16 crc = new CRC_16();
        txBuf[0] = SLAV_ADD;
        txBuf[1] = 0x03;
        txBuf[2] = 0x00;
        txBuf[3] = startAdd;
        txBuf[4] = 0x00;
        txBuf[5] = num;
        crc.update(txBuf, 6);
        int temp = crc.getValue();
        txBuf[6] = (byte)(temp >> 8);
        txBuf[7] = (byte) (temp & 0xff);
        onDataSend(txBuf, 8);
    }

    public void sendDataMaster16(byte startAdd,byte num) {
        int i,txCount;
        byte[] txBuf = new byte[256];
        CRC_16 crc = new CRC_16();

        slav_hand_10();

        txBuf[0] = SLAV_ADD;
        txBuf[1] = 0x10;
        txBuf[2] = 0x00;         //数据的起始地址；
        txBuf[3] = startAdd;
        txBuf[4] = 0x00;         //数据的个数；
        txBuf[5] = num;
        txBuf[6] = (byte) (num*2);         //数据的字节数；
        for (i = 0; i<txBuf[5]; i++) {
            txBuf[7 + 2 * i] = (byte) (regHodingBuf[i+ txBuf[3]] >> 8);
            txBuf[8 + 2 * i] = (byte)(regHodingBuf[i+ txBuf[3]] & 0xff);
        }
        crc.update(txBuf, txBuf[6] + 7);
        int temp = crc.getValue();
        txBuf[7 + txBuf[6]] = (byte)((temp >> 8) & 0xff);
        txBuf[8 + txBuf[6]] = (byte)(temp & 0xff);
        txCount = 9 + txBuf[6];
        onDataSend(txBuf, txCount);
    }


    public void closePort() throws IOException {
        mInputStream.close();
        mOutputStream.close();
    }

    /**
     * 数据等待接收
     */


    public void run() {
        super.run();
        timer10ms.schedule(taskPoll,10,10);//5ms后开始，每5ms轮询一次
        while (!isInterrupted()) {

            int size;
            try {
                byte[] reBuf = new byte[128];
                if (mInputStream == null) return;
                size = mInputStream.read(reBuf);
                if (size > 0) {
                    for (int i =0;i<size;i++){
                        rxTemp.add((reBuf[i]));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                try {
                    mInputStream.close();
                    mInputStream.close();
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            }
        }

    }

    /**
     *判断接收空闲，总线空闲时置位rxFlag
     */
    private TimerTask taskPoll=new TimerTask() {
        int txDataLengthTemp=0;
        int txIdleCount=0;
        public void run() {

            if(rxTemp.size()>0){
                if(txDataLengthTemp!=rxTemp.size()){
                    txDataLengthTemp=rxTemp.size();
                    txIdleCount=0;
                }
                if(txIdleCount<4){
                    txIdleCount++;
                    if (txIdleCount>=4){
                        txIdleCount=0;

                        try{

                            byte[] rxTempByteArray = new byte[rxTemp.size()+255];
                            int i=0;
                            Iterator<Byte> iterator = rxTemp.iterator();
                            while (iterator.hasNext()) {

                                if (i < rxTemp.size()+255){
                                    rxTempByteArray[i] = iterator.next();
                                    i++;
                                }
                            }
                            onDataReceived(rxTempByteArray,rxTemp.size());
                            rxTemp.clear();

                        }catch (Exception e){
                            rxTemp.clear();
                            e.printStackTrace();
                        }
                    }
                }
            }
            else {
                txDataLengthTemp=0;
            }
        }
    };


/***********************************************  *********************************************/

    /***
     *
     * @return mserialPort_1
     * @throws SecurityException
     * @throws IOException
     * @throws InvalidParameterException
     */


    public SerialPort getSerialPort() throws SecurityException, IOException, InvalidParameterException {
        if (mserialPort == null) {

            String path = uartPath;
            int baudrate = 9600;
            if ((path.length() == 0) || (baudrate == -1)) {
                throw new InvalidParameterException();
            }
            mserialPort = new SerialPort(new File(path), baudrate, 0);

        }
        return mserialPort;
    }


    private void onDataReceived(byte[] reBuf, int size) {

        if (!(SLAV_ADD == reBuf[0])) {
            return;
        }
        if (size <= 3)
            return;
        if (CRC_16.checkBuf(reBuf)) {

            if (0x03 == reBuf[1]){
                mod_Fun_03_Slav(reBuf,size);
            }
        }

    }

    public void onDataSend(byte[] seBuf, int size) {
        try {
            mOutputStream = mserialPort.getOutputStream();
            mOutputStream.write(seBuf, 0, size);

        } catch (Exception e) {

            e.printStackTrace();
        }
    }



    private void slav_hand_10() {

    }

    private void slav_hand_03() {

    }

    private void mod_Fun_03_Slav(byte[] reBuf,int size) {

        byte crch,crcl;

        CRC_16 crc = new CRC_16();

        if (reBuf[0] != SLAV_ADD) return;							//地址相符时，再对本帧数据进行校验
        if (reBuf[1] != 0x03) return;									//检验功能码
        crc.update(reBuf, size - 2);
        int value = crc.getValue();

        crch = (byte) (value >> 8);
        crcl = (byte) (value & 0xFF);
        if ((reBuf[size - 1] != crcl) || (reBuf[size - 2] != crch)){
            return;	//如CRC校验不符时直接退出
        }

        for (int i = 0; i < reBuf[2]/2; i++)
        {
            int highByte = crc.getUnsignedByte(reBuf[3 + 2*i]) << 8;
            int lowByte = crc.getUnsignedByte(reBuf[4 + 2*i]);
            receiveBuff[i] = highByte + lowByte;
            //  receiveBuff[i] = crc.getUnsignedByte((byte)(reBuf[3 + 2*i] << 8)) + crc.getUnsignedByte((byte)(reBuf[4 + 2*i]));
            //这句会导致高位丢失，因为reBuf[3 + 2*i] << 8后强制转换为byte后为0
        }

        slav_hand_03();
    }
}