package com.xinchen.netty.proxy;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

/**
 * @author xinchen
 * @version 1.0
 * @date 12/08/2019 17:20
 */
public class HexDumpProxyInitializer extends ChannelInitializer<SocketChannel> {

    private final String remoteHost;
    private final int port;

    public HexDumpProxyInitializer(String remoteHost, int port) {
        this.remoteHost = remoteHost;
        this.port = port;
    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ch.pipeline().addLast(new LoggingHandler(LogLevel.INFO))
                .addLast(new HexDumpProxyFrontendHandler(remoteHost, port));
    }
}
