package com.xinchen.netty.http.upload;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.util.CharsetUtil;

/**
 *
 * 处理程序只是从服务器转储响应的内容
 *
 * Handler that just dumps the contents of the response from the server
 */
public class HttpUploadClientHandler extends SimpleChannelInboundHandler<HttpObject> {

    private boolean readingChunks;

    @Override
    public void channelRead0(ChannelHandlerContext ctx, HttpObject msg) {
        // 如果是http response
        if (msg instanceof HttpResponse) {
            HttpResponse response = (HttpResponse) msg;

            // 打印状态吗和response version
            System.err.println("STATUS: " + response.status());
            System.err.println("VERSION: " + response.protocolVersion());


            // 打印header
            if (!response.headers().isEmpty()) {
                for (CharSequence name : response.headers().names()) {
                    for (CharSequence value : response.headers().getAll(name)) {
                        System.err.println("HEADER: " + name + " = " + value);
                    }
                }
            }

            if (response.status().code() == 200 && HttpUtil.isTransferEncodingChunked(response)) {
                // 如果返回成功并且有大文件，打印大文件内容
                readingChunks = true;
                System.err.println("CHUNKED CONTENT {");
            } else {
                System.err.println("CONTENT {");
            }
        }


        // 内容
        if (msg instanceof HttpContent) {
            HttpContent chunk = (HttpContent) msg;

            // 打印内容
            System.err.println(chunk.content().toString(CharsetUtil.UTF_8));


            if (chunk instanceof LastHttpContent) {
                if (readingChunks) {
                    System.err.println("} END OF CHUNKED CONTENT");
                } else {
                    System.err.println("} END OF CONTENT");
                }
                readingChunks = false;
            } else {
                System.err.println(chunk.content().toString(CharsetUtil.UTF_8));
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.channel().close();
    }
}
