package com.xinchen.netty.securechat;

import com.xinchen.netty.common.SslContextMaker;
import com.xinchen.netty.telent.TelnetClient;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.SslContext;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Simple SSL chat client modified from {@link TelnetClient}.
 *
 * @author Xin Chen (xinchenmelody@gmail.com)
 * @version 1.0
 * @date Created In 2019/8/11 15:18
 */
public class SecureChatClient {
    static final String HOST = System.getProperty("host", "127.0.0.1");
    static final int PORT = Integer.parseInt(System.getProperty("port", "8992"));

    public static void main(String[] args) throws Exception {
        // Configure SSL
        final SslContext sslCtx = SslContextMaker.client();

        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new SecureChatClientInitializer(sslCtx));

            // start up
            Channel channel = bootstrap.connect(HOST, PORT).sync().channel();

            // Read command from the stdin
            readCommands(channel);

        } finally {
            group.shutdownGracefully();
        }
    }

    static void readCommands(Channel channel) throws IOException, InterruptedException {
        ChannelFuture lastWriteFuture = null;

        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        for (; ; ) {
            String line = in.readLine();
            if (null == line) {
                break;
            }


            // send the received line to the server
            lastWriteFuture = channel.writeAndFlush(line + "\r\n");


            // 'bye' command ,wait until the server closes the connection
            if ("bye".equalsIgnoreCase(line)){
                channel.closeFuture().sync();
                break;
            }
        }

        // wait until all messages are flushed before closing the channel
        if (null!=lastWriteFuture){
            lastWriteFuture.sync();
        }

    }
}
