package com.xinchen.netty.securechat;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.GlobalEventExecutor;

import java.net.InetAddress;

/**
 *
 * {@link GlobalEventExecutor} 是一个单线程
 *
 * @author xinchen
 * @version 1.0
 * @date 10/08/2019 11:13
 */
public class SecureChatServerHandler extends SimpleChannelInboundHandler<String> {

    static final ChannelGroup channels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {


        // 一旦会话收到保护，发送问候语句并将频道注册到全局
        // 列出所有频道接收到的其他人的消息
        ctx.pipeline().get(SslHandler.class).handshakeFuture().addListener(
                new GenericFutureListener<Future<? super Channel>>() {
                    @Override
                    public void operationComplete(Future<? super Channel> future) throws Exception {
                        // greeting
                        ctx.writeAndFlush("Welcome to " + InetAddress.getLocalHost().getHostName() + " secure chat service!\n");

                        ctx.writeAndFlush("Your session is protected by " + ctx.pipeline().get(SslHandler.class).engine().getSession().getCipherSuite() + "cipher suite. \n");

                        // 将频道添加到组内
                        channels.add(ctx.channel());
                    }
                }
        );
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
        // 发送接收到的所有频道的消息到当前频道
        channels.forEach(channel -> {
            if (channel != ctx.channel()){
                channel.writeAndFlush("[" + ctx.channel().remoteAddress() + "]" + msg + "\n");
            } else {
                channel.writeAndFlush("[you] " + msg + "\n");
            }
        });

        // 关闭客户端
        if ("bye".equalsIgnoreCase(msg)){
            ctx.close();
        }

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }

}
