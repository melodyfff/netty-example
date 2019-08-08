package com.xinchen.netty.time;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

/**
 *
 * 获取服务器时间
 *
 * unix命令： rdate -p localhost -o 8006
 *
 * @author xinchen
 * @version 1.0
 * @date 08/08/2019 14:02
 */
public class TimeServer {
    static final int PORT = Integer.parseInt(System.getProperty("port", "8006"));

    public static void main(String[] args) throws Exception {
        // configure server
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workGroup)
                    .channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline pipeline = ch.pipeline();
                            // 添加自定义时间服务处理
                            pipeline.addLast(new TimeEncoder(),new TimeServerHandler());
                        }
                    });

            // 启动服务
            ChannelFuture f = bootstrap.bind(PORT).sync();

            // 等服务socket中断后停止
            f.channel().closeFuture().sync();

        } finally {
            bossGroup.shutdownGracefully();
            workGroup.shutdownGracefully();
        }
    }
}
