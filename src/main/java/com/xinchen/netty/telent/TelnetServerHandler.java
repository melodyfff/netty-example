package com.xinchen.netty.telent;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.net.InetAddress;
import java.util.Date;

/**
 *
 * handle a server-side channel
 *
 * @author xinchen
 * @version 1.0
 * @date 09/08/2019 16:22
 */
@Sharable
public class TelnetServerHandler extends SimpleChannelInboundHandler<String> {

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        // Send greeting for a new connection
        ctx.write("Welcome to " + InetAddress.getLocalHost().getHostName() + "!\r\n");
        ctx.write("It is " + new Date() + " now. \r\n");
        ctx.flush();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String request) throws Exception {
        // Generate and write a response
        String response;
        boolean close = false;

        if (request.isEmpty()){
            response = "Please type something. \r\n";
        } else if ("bye".equalsIgnoreCase(request)){
            response = "Have a good day \r\n";
            close = true;
        } else {
            response = "Did you say '" + request + "' ?\r\n";
        }

        // 这个地方不需要ChannelBuffer进行包裹处理
        // TelnetPipelineFactory中插入的编码转换器会自动进行装换
        ChannelFuture future = ctx.write(response);

        // 如果接收到'bye',发送'Have a good day' 并且关闭连接
        if (close){
            future.addListener(ChannelFutureListener.CLOSE);
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        // 读取完成刷新
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }

}
