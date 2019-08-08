package com.xinchen.netty.discard;

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
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;


/**
 * 丢弃任何传入的数据
 *
 * 这里采用输出接收到的信息
 * telnet localhost 8009
 * 回车后输入消息即可，ctrl + ] 即可退出
 *
 * @author xinchen
 * @version 1.0
 * @date 07/08/2019 14:52
 */
public final class DiscardServer {

    /***/
    static final boolean SSL = System.getProperty("ssl") != null;


    /**
     * 查找端口号，默认8009
     */
    static final int PORT = Integer.parseInt(System.getProperty("port", "8009"));


    public static void main(String[] args) throws Exception {
        // 配置SLL
        final SslContext sslCtx;

        if (SSL) {
            // 生成临时自签名证书以进行测试
            SelfSignedCertificate ssc = new SelfSignedCertificate();
            sslCtx = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build();
        } else {
            sslCtx = null;
        }

        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap b = new ServerBootstrap();
            // 为父（接受者）和子（客户端）设置{@link EventLoopGroup}
            // {@link EventLoopGroup}用于处理{@link ServerChannel}的所有事件和IO {@link Channel}
            b.group(bossGroup, workGroup)
             .channel(NioServerSocketChannel.class)
             .handler(new LoggingHandler(LogLevel.INFO))
             .childHandler(new ChannelInitializer<SocketChannel>() {
                 @Override
                 protected void initChannel(SocketChannel ch) throws Exception {
                     ChannelPipeline pipeline = ch.pipeline();
                     // 如果ssl不为空
                     if (null!=sslCtx){
                         pipeline.addLast(sslCtx.newHandler(ch.alloc()));
                     }
                     // 添加自定的discard处理
                     pipeline.addLast(new DiscardServerHandler());
                 }
             });

            // 绑定端口并开始接受传入连接
            ChannelFuture sync = b.bind(PORT).sync();


            // 等到服务socket关闭
            // 本示例中不会发生，但可以自己关闭
            sync.channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workGroup.shutdownGracefully();
        }

    }

}
