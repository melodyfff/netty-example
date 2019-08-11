package com.xinchen.netty.securechat;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.ssl.SslContext;

/**
 *
 * Creates a newly configured {@link ChannelPipeline} for a new channel.
 *
 * @author Xin Chen (xinchenmelody@gmail.com)
 * @version 1.0
 * @date Created In 2019/8/11 14:42
 */
public class SecureChatServerInitializer extends ChannelInitializer<SocketChannel> {

    private final SslContext sslCtx;

    public SecureChatServerInitializer(SslContext sslCtx) {
        this.sslCtx = sslCtx;
    }

    @Override
    protected void initChannel(SocketChannel socketChannel) throws Exception {
        ChannelPipeline pipeline = socketChannel.pipeline();


        // 添加SSL handler最先去 encrypt 和 decrypt

        // 在这个实例中，我们在服务端使用伪造证书并接受客户端的任何无效证书
        pipeline.addLast(sslCtx.newHandler(socketChannel.alloc()));

        // 文本行编解码器
        pipeline.addLast(new DelimiterBasedFrameDecoder(8192, Delimiters.lineDelimiter()));
        pipeline.addLast(new StringDecoder());
        pipeline.addLast(new StringEncoder());


        // 添加业务逻辑处理类
        pipeline.addLast(new SecureChatServerHandler());
    }
}
