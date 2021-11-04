package com.gnss.ntripserial;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.gnss.ntripserial.Utils.FileIOUtils;
import com.gnss.ntripserial.Utils.GGAParse;
import com.gnss.ntripserial.net.NtripHandler;
import com.gnss.ntripserial.net.ReceiverThread;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.util.ArrayList;
import java.util.Locale;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.CharsetUtil;
import tw.com.prolific.driver.pl2303g.PL2303GDriver;

import static com.gnss.ntripserial.Utils.SerialUtils.getError_info;
import static com.gnss.ntripserial.Utils.SerialUtils.openUsbSerial;
import static com.gnss.ntripserial.Utils.SerialUtils.readDataFromSerial;
import static com.gnss.ntripserial.Utils.SerialUtils.writeDataToSerial;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "Main";
    //views
    private TextView tv_ntrip_info;
    private ImageView im_net_connect;
    private TextView tv_serial_open_info;
    private ImageView im_serial_open;
    private TextView tv_serial_info;
    private ImageView im_serial_connect;
    private TextView tv_login_info;
    private ImageView im_login;
    private ImageButton imb_start;
    private Spinner sp_baudRate;
    private EditText host;
    private EditText port;
    private EditText user;
    private EditText password;
    private EditText mount_point;
    private TextView tv_sat_num;
    private TextView tv_position;
    private TextView tv_status;
    private TextView tv_age;
    private TextView tv_utc;
    private Context context;

    //serial
    PL2303GDriver mSerial;

    //serial params
    //BaudRate.B4800, DataBits.D8, StopBits.S1, Parity.NONE, FlowControl.RTSCTS
    private PL2303GDriver.BaudRate mBaudrate = PL2303GDriver.BaudRate.B9600;
    private PL2303GDriver.DataBits mDataBits = PL2303GDriver.DataBits.D8;
    private PL2303GDriver.Parity mParity = PL2303GDriver.Parity.NONE;
    private PL2303GDriver.StopBits mStopBits = PL2303GDriver.StopBits.S1;
    private PL2303GDriver.FlowControl mFlowControl = PL2303GDriver.FlowControl.OFF;
    private boolean serial_opened=false;

    //Ntrip
    private ReceiverThread thread;
    private ChannelHandlerContext channelHandlerContext;
    private SerialListener serialListener;
    private String HostIP;
    private String Port;
    private String UserName;
    private String PassWord;
    private String MountPoint;
    private SharedPreferences accountInfo;

    private boolean is_powerON = false;

    private static final String ACTION_USB_PERMISSION = "com.prolific.PL2303Gsimpletest.USB_PERMISSION";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = this;

        sp_baudRate = findViewById(R.id.baud_rate_select);
        imb_start = findViewById(R.id.start);
        tv_ntrip_info = findViewById(R.id.ntrip_info);
        im_net_connect = findViewById(R.id.net_connect);
        tv_serial_open_info = findViewById(R.id.serial_open_info);
        im_serial_open = findViewById(R.id.serial_open);
        tv_serial_info = findViewById(R.id.serial_info);
        im_serial_connect = findViewById(R.id.serial_connect);
        tv_login_info = findViewById(R.id.login_info);
        im_login = findViewById(R.id.login);

        host = findViewById(R.id.host);
        port = findViewById(R.id.port);
        user = findViewById(R.id.user);
        password = findViewById(R.id.password);
        mount_point = findViewById(R.id.mount_point);

        tv_sat_num = findViewById(R.id.sat_num);
        tv_position = findViewById(R.id.sat_info_position);
        tv_status = findViewById(R.id.sat_info_status);
        tv_age = findViewById(R.id.sat_info_age);
        tv_utc = findViewById(R.id.timeUTC);

        accountInfo = super.getSharedPreferences("account_info",MODE_PRIVATE);
        autoFillAccount();

        initBaudSpinner();

        // get service
        mSerial = new PL2303GDriver((UsbManager) getSystemService(Context.USB_SERVICE),
                this, ACTION_USB_PERMISSION);

        // check USB host function.
        if (!mSerial.PL2303USBFeatureSupported()) {

            Toast.makeText(this, "No Support USB host API", Toast.LENGTH_SHORT)
                    .show();

            Log.d(TAG, "No Support USB host API");

            mSerial = null;

        }

        if( !mSerial.enumerate() ) {
            //Toast.makeText(this, "no more devices found", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "no more devices found");
        }

        initStatus();

        imb_start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!is_powerON){
                    //start login and receive message
                    if (getAccountInfo()) {
                        saveAccount();
                        connectNtripSerial();
                        StartButtonCtrl(true);
                        setNetworkStatus(false);
                        setSerialStatus(false);
                    }else Toast.makeText(context, "账户信息缺失", Toast.LENGTH_SHORT).show();
                }else {
                    //interrupt thread
                    if (serialListener!=null)
                    {
                        serialListener.interrupt();
                        serialListener = null;
                    }
                    //StartButtonCtrl(false);
                }
            }
        });


    }

    private boolean getAccountInfo(){
        HostIP = host.getText().toString();
        Port = port.getText().toString();
        UserName = user.getText().toString();
        PassWord = password.getText().toString();
        MountPoint = mount_point.getText().toString();
        return !("".equals(HostIP) || "".equals(Port) || "".equals(UserName) || "".equals(MountPoint));
    }

    private void saveAccount(){
        SharedPreferences.Editor editor=accountInfo.edit();
        editor.putString("HostIP",HostIP);
        editor.putString("Port",Port);
        editor.putString("UserName",UserName);
        editor.putString("MountPoint",MountPoint).apply();
    }

    private void autoFillAccount(){
        if (accountInfo!=null){
            String hostIP = accountInfo.getString("HostIP", "");
            if (!"".equals(hostIP))host.setText(hostIP);
            String port_save = accountInfo.getString("Port", "");
            if (!"".equals(port_save))port.setText(port_save);
            String userName = accountInfo.getString("UserName", "");
            if (!"".equals(userName))user.setText(userName);
            String mountPoint = accountInfo.getString("MountPoint", "");
            if (!"".equals(mountPoint))mount_point.setText(mountPoint);
        }
    }

    private void connectNtripSerial() {
        thread=new ReceiverThread(HostIP,Port,
                new NtripHandler(UserName, PassWord, MountPoint,
                        new NtripHandler.ChannelCase() {
                            @Override
                            public void onConnectionBuild() {

                            }

                            @Override
                            public void onMsgReceived(String s, ChannelHandlerContext ctx, ByteBuf rawdata) {
                                if (s.contains("ICY 200 OK")){
                                    channelHandlerContext=ctx;
                                    runOnUiThread(() -> {
                                        setLoginStatus(true);
                                        Toast.makeText(MainActivity.this, "登录成功", Toast.LENGTH_SHORT).show();
                                    });
                                    //todo open serial
                                    boolean success = openUsbSerial(mSerial, mBaudrate, context);
                                    if (success) {
                                        Log.d("serial","open succeed");
                                        serial_opened=true;
                                        runOnUiThread(() -> {
                                            setSerialOpenStatus(true);
                                            Toast.makeText(MainActivity.this, "串口打开成功", Toast.LENGTH_SHORT).show();
                                        });
                                        serialListener = new SerialListener();
                                        serialListener.start();
                                    }else {
                                        runOnUiThread(() -> {
                                            StartButtonCtrl(false);
                                            Toast.makeText(MainActivity.this,
                                                    "串口打开失败:"+getError_info(), Toast.LENGTH_SHORT).show();
                                        });
                                    }
                                }else if (s.contains("Unauthorized")){
                                    runOnUiThread(() -> {
                                        StartButtonCtrl(false);
                                        setLoginStatus(false);
                                        Toast.makeText(MainActivity.this,
                                                "登录失败:用户名密码错误", Toast.LENGTH_SHORT).show();
                                    });
                                }else{
                                    Log.d(TAG,s);
                                    if (serial_opened){
                                        runOnUiThread(() -> {
                                            setNetworkStatus(true);
                                            setSerialStatus(false);
                                            String net_receive = String.format(Locale.CHINA,"%d bytes",rawdata.array().length);
                                            tv_ntrip_info.setText(net_receive);
                                        });
                                        writeDataToSerial(mSerial,rawdata.array());
                                    }
                                }
                            }
                        }));
        thread.setReport_handler(new Handler(){
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);
                if (msg.what==ReceiverThread.NO_NETWORK)
                    setLoginStatus(false);
                    StartButtonCtrl(false);
                    Toast.makeText(MainActivity.this, "无网络", Toast.LENGTH_SHORT).show();
            }
        });
        thread.start();
    }

    private void setNetworkStatus(boolean on){
        if (on){
            im_net_connect.setImageResource(R.mipmap.network_blue);
        }else im_net_connect.setImageResource(R.mipmap.network_gray);
    }

    private void setSerialStatus(boolean on){
        if (on){
            im_serial_connect.setImageResource(R.mipmap.usb_blue);
        }else im_serial_connect.setImageResource(R.mipmap.usb_gray);
    }

    private void setLoginStatus(boolean on){
        if (on){
            im_login.setImageResource(R.mipmap.login_success);
            tv_login_info.setText("账号已登录");
        }else {
            im_login.setImageResource(R.mipmap.login_fail);
            tv_login_info.setText("账号未登录");
        }
    }

    private void setSerialOpenStatus(boolean on){
        if (on){
            im_serial_open.setImageResource(R.mipmap.login_success);
            tv_serial_open_info.setText("串口已打开");
        }else {
            im_serial_open.setImageResource(R.mipmap.login_fail);
            tv_serial_open_info.setText("串口未打开");
        }
    }
    private void initStatus(){
        setNetworkStatus(false);
        setSerialOpenStatus(false);
        setLoginStatus(channelHandlerContext != null);
        setSerialStatus(false);
        tv_ntrip_info.setText("0 bytes");
        tv_serial_info.setText("0 bytes");
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_NO) {
            Toast.makeText(this, "keyboard visible", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "keyboard visible");
        } else if (newConfig.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_YES) {
            Toast.makeText(this, "keyboard hidden", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "keyboard hidden");
        }

        if(newConfig.orientation == ActivityInfo.CONFIG_ORIENTATION)
        {
            Log.d(TAG, "CONFIG_ORIENTATION");
        }

        if(newConfig.keyboard == ActivityInfo.CONFIG_KEYBOARD)
        {
            Log.d(TAG, "CONFIG_KEYBOARD");
        }

        if(newConfig.keyboardHidden == ActivityInfo.CONFIG_KEYBOARD_HIDDEN)
        {
            Log.d(TAG, "CONFIG_KEYBOARD_HIDDEN");
        }

        Log.d(TAG, "Exit onConfigurationChanged");
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy");
        if(mSerial!=null) {
            mSerial.end();
            mSerial = null;
        }
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        String action =  getIntent().getAction();
        Log.d(TAG, "onResume:"+action);
        initStatus();
        //if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action))
        if(!mSerial.isConnected()) {

            if( !mSerial.enumerate() ) {
                Log.d(TAG, "no more devices found");
                //Toast.makeText(this, "no more devices found", Toast.LENGTH_SHORT).show();
                return;
            } else {
                Log.d(TAG, "onResume:enumerate succeeded!");
            }
            try {
                Thread.sleep(1500);
                openUsbSerial(mSerial,mBaudrate,context);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }//if isConnected
    }

    private void initBaudSpinner() {
        //波特率选择
        ArrayAdapter<CharSequence> baudAdapter = ArrayAdapter.createFromResource(this,R.array.BaudRate_Var,
                android.R.layout.simple_spinner_item);
        baudAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sp_baudRate.setAdapter(baudAdapter);
        sp_baudRate.setOnItemSelectedListener(new BaudSelectedListener());
        sp_baudRate.setSelection(2, true);
    }

    private void StartButtonCtrl(boolean on){
        if (on){
            imb_start.setBackground(ContextCompat.getDrawable(context,R.drawable.round_btn_selected));
        }else {
            imb_start.setBackground(ContextCompat.getDrawable(context,R.drawable.round_btn));
        }
        is_powerON = on;
    }

    public class BaudSelectedListener implements AdapterView.OnItemSelectedListener {
        private static final String TAG = "BaudSelect";

        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

            if(null==mSerial)
                return;

            if(!mSerial.isConnected())
                return;

            int baudRate=0;
            String newBaudRate;
            Toast.makeText(parent.getContext(), "newBaudRate is-" + parent.getItemAtPosition(position).toString(), Toast.LENGTH_LONG).show();
            newBaudRate= parent.getItemAtPosition(position).toString();

            try	{
                baudRate= Integer.parseInt(newBaudRate);
            }
            catch (NumberFormatException e)	{
                System.out.println(" parse int error!!  " + e);
            }

            switch (baudRate) {
                case 9600:
                    mBaudrate =PL2303GDriver.BaudRate.B9600;
                    break;
                case 19200:
                    mBaudrate =PL2303GDriver.BaudRate.B19200;
                    break;
                case 115200:
                    mBaudrate =PL2303GDriver.BaudRate.B115200;
                    break;
                default:
                    mBaudrate =PL2303GDriver.BaudRate.B9600;
                    break;
            }

            int res = 0;
            try {
                res = mSerial.setup(mBaudrate, mDataBits, mStopBits, mParity, mFlowControl);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            if( res<0 ) {
                Log.d(TAG, "fail to setup");
                return;
            }
        }
        public void onNothingSelected(AdapterView<?> parent) {
        }
    }//MyOnItemSelectedListener

    class SerialListener extends Thread {

        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        @Override
        public void run() {

            while (!isInterrupted()) {
                runOnUiThread(() -> {
                    setNetworkStatus(false);
                    setSerialStatus(true);
                });
                try{
                    Thread.sleep(1000);
                }catch(InterruptedException e){
                    e.printStackTrace();
                    break;
                }
                String finalString = readDataFromSerial(mSerial);
                runOnUiThread(() -> {
                    Log.d("serial","收到数据:"+finalString);
                    String serial_receive = String.format(Locale.CHINA,"%d bytes",finalString.length());
                    tv_serial_info.setText(serial_receive);
                    if (channelHandlerContext!=null && finalString.contains("GNGGA")) {
                        setSerialOpenStatus(true);
                        setLoginStatus(true);
                        try {
                            GGAParse ggaParse = new GGAParse(finalString).parse();
                            String satNum = ggaParse.getSat_num()+"颗";
                            tv_sat_num.setText(satNum);
                            tv_position.setText(String.format("经纬高：%s %s %s",
                                    ggaParse.getLongitude(),ggaParse.getLatitude(),ggaParse.getH()));
                            tv_status.setText(String.format("状态：%s",ggaParse.getStatus()));
                            tv_age.setText(String.format("差分龄期：%s",ggaParse.getAge()));
                            tv_utc.setText(ggaParse.getUTC());
                            channelHandlerContext.writeAndFlush(Unpooled.copiedBuffer(finalString, CharsetUtil.UTF_8)).sync();
                        }catch (RuntimeException e){
                            e.printStackTrace();
                        }catch (InterruptedException e) {
                            FileIOUtils.WriteErrReport(getExternalFilesDir(null),e,"channel send");
                            e.printStackTrace();
                        }
                    }
                    if (finalString.contains("not connected")){
                        setSerialOpenStatus(false);
                        Toast.makeText(context, "串口已断开", Toast.LENGTH_SHORT).show();
                        interrupt();
                    }
                });

            }
            Log.d(TAG,"串口关闭");
            serial_opened = false;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    setSerialOpenStatus(false);
                    StartButtonCtrl(false);
                    Toast.makeText(MainActivity.this, "串口已关闭", Toast.LENGTH_SHORT).show();
                }

            });
        }
    }
}