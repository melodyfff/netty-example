package com.xinchen.netty.proxy;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

/**
 * @author xinchen
 * @version 1.0
 * @date 12/08/2019 17:22
 */
public class HexDumpProxy {

    static final int LOCAL_PORT = Integer.parseInt(System.getProperty("localPort", "8443"));
    static final String REMOTE_HOST = System.getProperty("remoteHost", "www.baidu.com");
    static final int REMOTE_PORT = Integer.parseInt(System.getProperty("remotePort", "443"));


    public static void main(String[] args) throws InterruptedException {
        System.err.println("Proxying *:" + LOCAL_PORT + " to " + REMOTE_HOST + ":" + REMOTE_PORT + " ...");

        //Configure the bootstrap

        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workGroup)
                    .channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new HexDumpProxyFrontendHandler(REMOTE_HOST, REMOTE_PORT))
                    .bind(LOCAL_PORT).sync().channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workGroup.shutdownGracefully();
        }

    }

}
