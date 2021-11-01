package com.gnss.ntripserial.net;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.CharsetUtil;

public class GGAHandler extends ChannelInboundHandlerAdapter {

    private ChannelCase channelCase;

    public GGAHandler(ChannelCase channelCase) {
        this.channelCase = channelCase;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        channelCase.onConnectionBuild();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf buf= (ByteBuf) msg;
        channelCase.onMsgReceived(buf.toString(CharsetUtil.UTF_8));
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
    public interface ChannelCase{
        void onConnectionBuild();
        void onMsgReceived(String s);
    }

}
