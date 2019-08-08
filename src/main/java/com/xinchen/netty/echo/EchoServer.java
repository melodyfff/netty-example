package com.xinchen.netty.echo;

import com.xinchen.netty.common.SslContextMaker;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;


/**
 *
 * 输出任何从客户端接受到的消息
 *
 * @author xinchen
 * @version 1.0
 * @date 08/08/2019 12:17
 */
public class EchoServer {
    static final boolean SSL = System.getProperty("ssl") != null;
    static final int PORT = Integer.parseInt(System.getProperty("port", "8007"));

    public static void main(String[] args) throws Exception {
        // configure SSL
        final SslContext sslCtx = SSL ? SslContextMaker.server() : null;


        // configure server
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workGroup = new NioEventLoopGroup();
        final EchoServerHandler handler = new EchoServerHandler();

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workGroup)
                    .channel(NioServerSocketChannel.class)

                    .option(ChannelOption.SO_BACKLOG, 100)
                    // 绑定日志输出
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline pipeline = ch.pipeline();
                            if (null!=sslCtx){
                                pipeline.addLast(sslCtx.newHandler(ch.alloc()));
                            }
                            pipeline.addLast(handler);
                        }
                    });

            // 启动服务
            ChannelFuture future = bootstrap.bind(PORT).sync();

            // 等到服务socket中断
            future.channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workGroup.shutdownGracefully();
        }

    }
}
