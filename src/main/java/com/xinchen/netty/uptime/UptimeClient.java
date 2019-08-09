package com.xinchen.netty.uptime;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;

/**
 *
 *
 *
 * @author xinchen
 * @version 1.0
 * @date 08/08/2019 17:18
 */
public class UptimeClient {

    static final String HOST = System.getProperty("host", "127.0.0.1");
    static final int PORT = Integer.parseInt(System.getProperty("port", "8080"));

    /** sleep 5 在尝试重新连接之前 */
    static final int RECONNECT_DELAY = Integer.parseInt(System.getProperty("reconnectDelay", "5"));
    /** 超时时间 */
    static final int READ_TIMEOUT = Integer.parseInt(System.getProperty("readTimeout", "10"));

    static final UptimeClientHandler handler = new UptimeClientHandler();
    static final Bootstrap bs = new Bootstrap();

    public static void main(String[] args) {
        EventLoopGroup group = new NioEventLoopGroup();
        bs.group(group)
                .channel(NioSocketChannel.class)
                .remoteAddress(HOST, PORT)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        // IdleStateHandler 空闲等待一段时间触发
                        ch.pipeline().addLast(new IdleStateHandler(READ_TIMEOUT,0,0), handler);
                    }
                });

        // start client
        bs.connect();
    }

    static void connect(){
        // 尝试连接并且添加listener
        bs.connect().addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                // 如果尝试连接时发生异常
                if (future.cause()!=null){
                    handler.startTIme = -1;
                    handler.println("Failed to connect: "+ future.cause());
                }
            }
        });
    }
}
