package com.xinchen.netty.telent;

import com.xinchen.netty.common.SslContextMaker;
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
 *
 * Simplistic telnet client
 *
 * @author xinchen
 * @version 1.0
 * @date 09/08/2019 16:53
 */
public class TelnetClient {
    static final boolean SSL = System.getProperty("ssl") != null;
    static final String HOST = System.getProperty("host", "127.0.0.1");
    static final int PORT = Integer.parseInt(System.getProperty("port",SSL?"8992":"8023"));

    public static void main(String[] args) throws Exception {
        final SslContext sslCtx = SSL ? SslContextMaker.client() : null;

        EventLoopGroup group = new NioEventLoopGroup();

        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group)
                    .remoteAddress(HOST,PORT)
                    .channel(NioSocketChannel.class)
                    .handler(new TelnetClientInitializer(sslCtx));

            // start up
            Channel ch = bootstrap.connect().sync().channel();

            // Read commands from stdin
            readCommands(ch);

        }finally {
            group.shutdownGracefully();
        }

    }


    private static void readCommands(Channel channel) throws IOException, InterruptedException {
        ChannelFuture lastWriteFuture = null;

        // get stdin
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

        for (;;){
            String line = in.readLine();
            if (null == line){
                break;
            }

            // 将获取到数据刷入
            lastWriteFuture = channel.writeAndFlush(line + "\r\n");

            if ("bye".equalsIgnoreCase(line)){
                // 关闭连接
                channel.closeFuture().sync();
                break;
            }
        }



        if (null!= lastWriteFuture){
            lastWriteFuture.sync();
        }



    }

}
