package com.gnss.ntripserial.net;


import android.util.Base64;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.CharsetUtil;

public class NtripHandler extends ChannelInboundHandlerAdapter {
    private ChannelCase channelCase;
    private String User;
    private String PassWord;
    private String MountPoint;
    public static final String Author_format="%s:%s";
    public static final String Header_format="GET /%s %s\r\nUser-Agent: %s %s\r\nAuthorization: Basic %s\r\n\r\n";

    public NtripHandler(String User, String PassWord, String mountPoint, ChannelCase channelCase) {
        this.channelCase = channelCase;
        this.User=User;
        this.PassWord=PassWord;
        this.MountPoint=mountPoint;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        channelCase.onConnectionBuild();
        String auth=String.format(Author_format,User,PassWord);
        String auth_encoded = Base64.encodeToString(auth.getBytes(),
                Base64.NO_WRAP | Base64.NO_PADDING | Base64.URL_SAFE);
        String header = String.format(Header_format, MountPoint, "HTTP/1.1", "NTRIP", "GNSSInternetRadio/1.4.10", auth_encoded);
        ctx.writeAndFlush(Unpooled.copiedBuffer(header,CharsetUtil.UTF_8));
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf buf= (ByteBuf) msg;
        channelCase.onMsgReceived(buf.toString(CharsetUtil.UTF_8),
                ctx,buf);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }

    public interface ChannelCase{
        void onConnectionBuild();
        void onMsgReceived(String s, ChannelHandlerContext ctx,ByteBuf rawdata);
    }
}
