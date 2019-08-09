package com.xinchen.netty.telent;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.ssl.SslContext;

/**
 * @author xinchen
 * @version 1.0
 * @date 09/08/2019 16:44
 */
@Sharable
public class TelnetClientInitializer extends ChannelInitializer<SocketChannel> {

    private static final StringDecoder DECODER = new StringDecoder();
    private static final StringEncoder ENCODER = new StringEncoder();

    private static final TelnetClientHandler CLIENT_HANDLER = new TelnetClientHandler();

    private final SslContext sslCtx;

    public TelnetClientInitializer(SslContext sslCtx) {
        this.sslCtx = sslCtx;
    }


    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        if (null!=sslCtx){
            pipeline.addLast(sslCtx.newHandler(ch.alloc(),TelnetClient.HOST,TelnetClient.PORT));
        }
        pipeline.addLast(
                // 特殊字符编码器/r
                new DelimiterBasedFrameDecoder(8192, Delimiters.lineDelimiter()),
                DECODER,
                ENCODER,
                CLIENT_HANDLER
        );
    }
}
