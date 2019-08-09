package com.xinchen.netty.telent;

import com.xinchen.netty.common.SslContextMaker;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;

/**
 * Simplistic telnet server.
 *
 * @author xinchen
 * @version 1.0
 * @date 09/08/2019 16:38
 */
public class TelnetServer {

    static final boolean SSL = System.getProperty("ssl") != null;
    static final int PORT = Integer.parseInt(System.getProperty("port", SSL ? "8992" : "8023"));

    static final ServerBootstrap bootstrap = new ServerBootstrap();

    public static void main(String[] args) throws Exception {
        SslContext sslCtx = SSL ? SslContextMaker.server() : null;

        EventLoopGroup boosGroup = new NioEventLoopGroup();
        EventLoopGroup workGroup = new NioEventLoopGroup();

        try {
            bootstrap.group(boosGroup, workGroup)
                    .channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new TelnetServerInitializer(sslCtx));


            // start up
            bootstrap.bind(PORT).sync().channel().closeFuture().sync();

        } finally {
            boosGroup.shutdownGracefully();
            workGroup.shutdownGracefully();
        }

    }
}
