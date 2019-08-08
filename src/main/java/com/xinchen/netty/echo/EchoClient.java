package com.xinchen.netty.echo;

import com.xinchen.netty.common.SslContextMaker;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.SslContext;

/**
 *
 * 在连接打开时发送一条消息，并且接收任何收到的消息
 *
 * ping-pong
 *
 * telnet 127.0.0.1 8007
 *
 * 回车后输入任意字符，可看见返回相同字符
 *
 * @author xinchen
 * @version 1.0
 * @date 08/08/2019 12:32
 */
public class EchoClient {
    static final boolean SSL = System.getProperty("ssl") != null;
    static final String HOST = System.getProperty("host", "127.0.0.1");
    static final int PORT = Integer.parseInt(System.getProperty("port", "8007"));
    static final int SIZE = Integer.parseInt(System.getProperty("size", "256"));

    public static void main(String[] args) throws Exception {
        // Configure SSL
        final SslContext sslCtx = SSL ? SslContextMaker.client() : null;

        // Configure client
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline pipeline = ch.pipeline();
                            if (null!= sslCtx){
                                pipeline.addLast(sslCtx.newHandler(ch.alloc(),HOST,PORT));
                            }
                            // 添加自定义处理器
                            pipeline.addLast(new EchoClientHandler());
                        }
                    });

            // start client
            ChannelFuture future = bootstrap.connect(HOST, PORT).sync();

            // 等到连接中断后关闭
            future.channel().closeFuture().sync();
        }finally {
            group.shutdownGracefully();
        }
    }
}
