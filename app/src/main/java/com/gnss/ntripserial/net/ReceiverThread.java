package com.gnss.ntripserial.net;

import android.os.Handler;
import android.os.Message;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

public class ReceiverThread extends Thread {
    private String IP;
    private String port;
    private ChannelInboundHandlerAdapter ChannelHandler;
    private Handler report_handler;
    public static final int NO_NETWORK = 0x00;

    public ReceiverThread(String ip, String port, ChannelInboundHandlerAdapter channelHandler) {
        this.IP=ip;
        this.port=port;
        this.ChannelHandler=channelHandler;
    }

    public void setReport_handler(Handler report_handler) {
        this.report_handler = report_handler;
    }

    @Override
    public void run() {
        super.run();

        EventLoopGroup group=new NioEventLoopGroup();
        Bootstrap client= new Bootstrap();
        client.group(group)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast(ChannelHandler);
                    }
                });
        Message message=null;
        if (report_handler!=null) {
            message = report_handler.obtainMessage();
        }
        try {
            ChannelFuture channelFuture = client.connect(IP, Integer.parseInt(port)).sync();
            channelFuture.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }catch (Exception e){
            e.printStackTrace();
            if (message!=null){
                message.what=NO_NETWORK;
                report_handler.sendMessage(message);
            }
        }
        finally {
            group.shutdownGracefully();
        }
    }
}
