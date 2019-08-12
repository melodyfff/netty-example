package com.xinchen.netty.http.file;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.stream.ChunkedWriteHandler;


/**
 * @author xinchen
 * @version 1.0
 * @date 12/08/2019 13:41
 */
public class HttpStaticFileServerInitializer extends ChannelInitializer<SocketChannel> {

    private final SslContext sslCtx;

    public HttpStaticFileServerInitializer(SslContext sslCtx) {
        this.sslCtx = sslCtx;
    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        final ChannelPipeline pipeline = ch.pipeline();

        if (sslCtx!=null){
            pipeline.addLast(sslCtx.newHandler(ch.alloc()));
        }
        // {@link HttpRequestDecoder} 和 {@link HttpResponseEncoder}的结合
        pipeline.addLast(new HttpServerCodec())
                // 汇总HttpMessage 成为单个{@link FullHttpRequest}或{@link FullHttpResponse}
                .addLast(new HttpObjectAggregator(65536))
                .addLast(new ChunkedWriteHandler())
                .addLast(new HttpStaticFileServerHandler());
    }
}
