package com.gnss.ntripserial.net;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class SenderThread extends Thread {
    private String target_ip;
    private int target_port;
    private String content;

    public SenderThread(String target_ip,int target_port, String content) {
        this.target_ip = target_ip;
        this.content = content;
        this.target_port=target_port;
    }


    @Override
    public void run() {
        try {
            Socket socket=new Socket(target_ip,target_port);
            DataOutputStream out=new DataOutputStream(socket.getOutputStream());
            out.writeUTF(content);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
