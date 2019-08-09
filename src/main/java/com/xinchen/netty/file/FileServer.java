package com.xinchen.netty.file;

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
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.util.CharsetUtil;

/**
 *
 * 输入一个文件路径，返回其内容
 *
 * @author xinchen
 * @version 1.0
 * @date 09/08/2019 15:54
 */
public class FileServer {

    static final boolean SSL = System.getProperty("ssl") != null;

    /** Use the same default port with the telnet example so that we can use the telnet client example to access it*/
    static final int PORT = Integer.parseInt(System.getProperty("port", SSL ? "8992" : "8023"));

    public static void main(String[] args) throws Exception {

        final SslContext sslCtx = SSL ? SslContextMaker.client() : null;

        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 100)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline pipeline = ch.pipeline();
                            if (sslCtx != null){
                                pipeline.addLast(sslCtx.newHandler(ch.alloc()));
                            }

                            pipeline.addLast(
                                    new StringEncoder(CharsetUtil.UTF_8),
                                    new LineBasedFrameDecoder(8192),
                                    new StringDecoder(CharsetUtil.UTF_8),
                                    new FileServerHandler()
                            );
                        }
                    });

            // start server
            ChannelFuture f = bootstrap.bind(PORT).sync();

            // wait until the server socket is closed.
            f.channel().closeFuture().sync();


        } finally {
            bossGroup.shutdownGracefully();
            workGroup.shutdownGracefully();
        }

    }
}
