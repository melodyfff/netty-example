package com.xinchen.netty.http.upload;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.ssl.SslContext;

/**
 * @author xinchen
 * @version 1.0
 * @date 13/08/2019 13:34
 */
public class HttpUploadServerInitializer extends ChannelInitializer<SocketChannel> {

    private final SslContext sslCtx;

    public HttpUploadServerInitializer(SslContext sslCtx) {
        this.sslCtx = sslCtx;
    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();

        if (null!=sslCtx){
            pipeline.addLast(sslCtx.newHandler(ch.alloc()));
        }
        // 添加http request decoder
        pipeline.addLast(new HttpRequestDecoder())
                // 添加http response encoder
                .addLast(new HttpResponseEncoder())
                // 如果不想自动压缩内容，移除此列
                .addLast(new HttpContentCompressor())

                .addLast(new HttpUploadServerHandler());
    }
}
