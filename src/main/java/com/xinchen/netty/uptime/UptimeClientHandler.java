package com.xinchen.netty.uptime;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

/**
 *
 * 保持连接到服务器，同时打印出当前正常运行的时间
 *
 *
 * @author xinchen
 * @version 1.0
 * @date 08/08/2019 16:52
 */
public class UptimeClientHandler extends SimpleChannelInboundHandler<Object> {

    long startTIme = -1;


    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        if (startTIme < 0){
            startTIme = System.currentTimeMillis();
        }
        println("Connect to:"+ ctx.channel().remoteAddress());
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        // Discard received data
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (!(evt instanceof IdleStateEvent)){
            return;
        }

        IdleStateEvent e = (IdleStateEvent) evt;
        if (e.state() == IdleState.READER_IDLE){
            // 连接正常，但是上一段时间没有数据流量
            println("Disconnecting due to no inbound traffic");
            ctx.close();
        }

    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        println("Disconnected from: " + ctx.channel().remoteAddress());
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        println("Sleeping for: "+ UptimeClient.RECONNECT_DELAY + 's');
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }

    void println(String msg){
        if (startTIme < 0){
            System.err.format("[SERVER IS DOWN] %s%n", msg);
        } else {
            System.err.format("[UPTIME: %5ds] %s%n",(System.currentTimeMillis() -startTIme) /1000L, msg);
        }
    }

}
