package com.xinchen.netty.telent;

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
 * @author xinchen
 * @version 1.0
 * @date 09/08/2019 16:16
 */
public class TelnetServerInitializer extends ChannelInitializer<SocketChannel> {

    private static final StringDecoder DECODER = new StringDecoder();
    private static final StringEncoder ENCODER = new StringEncoder();

    private static final TelnetServerHandler SERVER_HANDLER = new TelnetServerHandler();

    private final SslContext sslContext;

    public TelnetServerInitializer(SslContext sslContext) {
        this.sslContext = sslContext;
    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();

        if (null!=sslContext){
            pipeline.addLast(sslContext.newHandler(ch.alloc()));
        }

        pipeline.addLast(
                new DelimiterBasedFrameDecoder(8192, Delimiters.lineDelimiter()),
                DECODER,
                ENCODER,
                SERVER_HANDLER
        );
    }
}
