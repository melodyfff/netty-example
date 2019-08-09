package com.xinchen.netty.redis;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.redis.RedisArrayAggregator;
import io.netty.handler.codec.redis.RedisBulkStringAggregator;
import io.netty.handler.codec.redis.RedisDecoder;
import io.netty.handler.codec.redis.RedisEncoder;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * @author xinchen
 * @version 1.0
 * @date 09/08/2019 13:03
 */
public class RedisClient {
    static final String HOST = System.getProperty("host", "127.0.0.1");
    static final int PORT = Integer.parseInt(System.getProperty("port", "6379"));

    static Bootstrap bootstrap = new Bootstrap();

    public static void main(String[] args) throws Exception {
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            bootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .remoteAddress(HOST, PORT)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline()
                                    // 解码器
                                    .addLast(new RedisDecoder())
                                    // redis数据处理聚合器
                                    .addLast(new RedisBulkStringAggregator())
                                    .addLast(new RedisArrayAggregator())
                                    // 转码器
                                    .addLast(new RedisEncoder())
                                    // 自定义处理器
                                    .addLast(new RedisCLientHandler());
                        }
                    });

            // start up
            Channel channel = bootstrap.connect().sync().channel();

            // Read commands from the stdin
            readCommands(channel);


        } finally {
            group.shutdownGracefully();
        }
    }

    static void readCommands(Channel channel) throws IOException, InterruptedException {
        System.out.println("Enter Redis commands (quit to end)");
        ChannelFuture lastWriteFuture;

        // get stdin
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

        for (; ; ) {
            final String input = in.readLine();
            final String line = input != null ? input.trim() : null;

            if (null == line || "quit".equalsIgnoreCase(line) || "q".equalsIgnoreCase(line)) {
                channel.close().sync();
                break;
            } else if (line.isEmpty()) {
                continue;
            }

            // send the received line to server
            lastWriteFuture = channel.writeAndFlush(line);

            lastWriteFuture.addListener(new GenericFutureListener<Future<? super Void>>() {
                @Override
                public void operationComplete(Future<? super Void> future) throws Exception {
                    if (!future.isSuccess()) {
                        System.err.print("write failed: ");
                        future.cause().printStackTrace(System.err);
                    }
                }
            });

            // 等待到所有消息发送完毕后关闭channel
            lastWriteFuture.sync();

        }
    }
}
