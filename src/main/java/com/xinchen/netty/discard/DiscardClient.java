package com.xinchen.netty.discard;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;


/**
 * 发送随机数据到指定地址
 *
 * @author xinchen
 * @version 1.0
 * @date 08/08/2019 09:26
 */
public class DiscardClient {

    static final boolean SSL = System.getProperty("ssl") != null;

    static final String HOST = System.getProperty("host", "127.0.0.1");
    static final int PORT = Integer.parseInt(System.getProperty("port", "8009"));
    static final int SIZE = Integer.parseInt(System.getProperty("size", "256"));

    public static void main(String[] args) throws Exception {
        // configure SSL
        final SslContext sslCtx = SSL ? SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build() : null;


        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();

            b.group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline pipeline = ch.pipeline();
                            if (sslCtx!=null){
                                pipeline.addLast(sslCtx.newHandler(ch.alloc(), HOST, PORT));
                            }
                            pipeline.addLast(new DiscardClientHandler());
                        }
                    });

            // 尝试连接
            ChannelFuture future = b.connect(HOST, PORT).sync();

            // 等待到连接关闭
            future.channel().closeFuture().sync();
        } finally {
            group.shutdownGracefully();
        }

    }
}
