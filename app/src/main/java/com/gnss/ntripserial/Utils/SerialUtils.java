package com.gnss.ntripserial.Utils;

import android.content.Context;
import android.util.Log;
import android.widget.Spinner;
import android.widget.Toast;

import com.gnss.ntripserial.R;

import tw.com.prolific.driver.pl2303g.PL2303GDriver;

public class SerialUtils {

    private static final String TAG = "Serial operation";

    private static String error_info = "no error";

    public static boolean openUsbSerial(PL2303GDriver mSerial, PL2303GDriver.BaudRate mBaudrate, Context context) {
        Log.d(TAG, "Enter  openUsbSerial");

        if(mSerial==null) {
            Log.d(TAG, "No mSerial");
            error_info = "No mSerial";
            return false;
        }
        if (mBaudrate == null){
            Log.d(TAG,"no mBaudrate");
            error_info = "no mBaudrate";
        }

        if (mSerial.isConnected()) {
            Log.d(TAG, "openUsbSerial : isConnected ");
            if (!mSerial.InitByBaudRate(mBaudrate,700)) {
                if(!mSerial.PL2303Device_IsHasPermission()) {
                    Log.d(TAG,"no permission");
                    error_info = "no permission";
                    //Toast.makeText(context, "cannot open, maybe no permission", Toast.LENGTH_SHORT).show();
                }

                if(mSerial.PL2303Device_IsHasPermission() && (!mSerial.PL2303Device_IsSupportChip())) {
                    //Toast.makeText(context, "cannot open, maybe this chip has no support, please use PL2303G chip.", Toast.LENGTH_SHORT).show();
                    error_info = "cannot open, maybe this chip has no support, please use PL2303G chip.";
                    Log.d(TAG, "cannot open, maybe this chip has no support, please use PL2303G chip.");
                }
                return false;
            } else {
                //Toast.makeText(context, "connected : OK" , Toast.LENGTH_SHORT).show();
                Log.d(TAG, "connected : OK");
                Log.d(TAG, "Exit  openUsbSerial");
                return true;
            }
        }else {
            //Toast.makeText(context, "Connected failed, Please plug in PL2303 cable again!" , Toast.LENGTH_SHORT).show();
            Log.d(TAG, "connected failed, Please plug in PL2303 cable again!");
            error_info = "connected failed, Please plug in PL2303 cable again!";
            return false;
        }
    }//openUsbSerial


    public static String readDataFromSerial(PL2303GDriver mSerial) {
        String read;
        int len;
        byte[] rbuf = new byte[4096];
        StringBuffer sbHex=new StringBuffer();

        Log.d(TAG, "Enter readDataFromSerial");

        if(null==mSerial)
            return "serial is null";

        if(!mSerial.isConnected())
            return "not connected";

        len = mSerial.read(rbuf);
        if(len<0) {
            Log.d(TAG, "Fail to bulkTransfer(read data)");
            return "read len < 0";
        }

        if (len > 0) {
            Log.d(TAG, "read len : " + len);
            //rbuf[len] = 0;
            for (int j = 0; j < len; j++) {
                //String temp=Integer.toHexString(rbuf[j]&0x000000FF);
                //Log.i(TAG, "str_rbuf["+j+"]="+temp);
                //int decimal = Integer.parseInt(temp, 16);
                //Log.i(TAG, "dec["+j+"]="+decimal);
                //sbHex.append((char)decimal);
                //sbHex.append(temp);
                sbHex.append((char) (rbuf[j]&0x000000FF));
            }
            read = sbHex.toString();
//            Toast.makeText(this, "len="+len, Toast.LENGTH_SHORT).show();
        }
        else {
            Log.d(TAG, "read len : 0 ");
            return "read len is empty";
        }

        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "Leave readDataFromSerial");
        return read;
    }//readDataFromSerial

    public static void writeDataToSerial(PL2303GDriver mSerial, String strWrite) {

        Log.d(TAG, "Enter writeDataToSerial");

        if(null==mSerial)
            return;

        if(!mSerial.isConnected())
            return;
        int res = mSerial.write(strWrite.getBytes(), strWrite.length());
        if( res<0 ) {
            Log.d(TAG, "setup2: fail to controlTransfer: "+ res);
            return;
        }
        Log.d(TAG, "Leave writeDataToSerial");
    }//writeDataToSerial
    public static void writeDataToSerial(PL2303GDriver mSerial, byte[] bytes) {

        Log.d(TAG, "Enter writeDataToSerial");

        if(null==mSerial)
            return;

        if(!mSerial.isConnected())
            return;
        int res = mSerial.write(bytes, bytes.length);
        if( res<0 ) {
            Log.d(TAG, "setup2: fail to controlTransfer: "+ res);
            return;
        }
        Log.d(TAG, "Leave writeDataToSerial");
    }//writeDataToSerial

    public static String ShowPL2303G_SerialNmber(PL2303GDriver mSerial) {

        Log.d(TAG, "Enter ShowPL2303G_SerialNmber");

        if(null==mSerial)
            return "serial is null";

        if(!mSerial.isConnected())
            return "not connected";

        if(mSerial.PL2303Device_GetSerialNumber()!=null) {
            return mSerial.PL2303Device_GetSerialNumber();
        }
        else{
            return "No SN";
        }
    }//ShowPL2303G_SerialNmber

    public static String getError_info() {
        return error_info;
    }
}
