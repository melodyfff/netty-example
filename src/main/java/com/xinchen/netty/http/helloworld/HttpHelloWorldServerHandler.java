package com.xinchen.netty.http.helloworld;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;



/**
 *
 * 关于http的keep-alive: https://www.cnblogs.com/skynet/archive/2010/12/11/1903347.html
 *
 * @author xinchen
 * @version 1.0
 * @date 12/08/2019 08:56
 */
public class HttpHelloWorldServerHandler extends SimpleChannelInboundHandler<HttpObject> {

    private static final byte[] CONTENT = {'H', 'e', 'l', 'l', 'o', ' ', 'W', 'o', 'r', 'l', 'd'};

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HttpObject msg) throws Exception {
        // 筛选HttpRequest请求
        if (msg instanceof HttpRequest) {
            HttpRequest req = (HttpRequest) msg;

            boolean keepAlive = HttpUtil.isKeepAlive(req);

            // 创建返回，内容通过Unpooled包裹
            FullHttpResponse response = new DefaultFullHttpResponse(req.protocolVersion(), HttpResponseStatus.OK, Unpooled.wrappedBuffer(CONTENT));

            // 添加response的header
            response.headers()
                    .set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.TEXT_PLAIN)
                    // Content-Length表示实体内容长度，客户端（服务器）可以根据这个值来判断数据是否接收完成
                    .setInt(HttpHeaderNames.CONTENT_LENGTH,response.content().readableBytes());


            //
            if (keepAlive){
                // 判断当前http协议是否默认开启keepAlive (http1.1默认开启)
                if (!req.protocolVersion().isKeepAliveDefault()){
                    // 开启keep alive
                    response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
                }
            } else {
                // 告诉客户端即将关闭连接
                response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
            }

            // 写入数据
            ChannelFuture f = ctx.write(response);


            // 如果不是keepAlive状态，添加监听器再发送完毕后关闭连接
            if (!keepAlive){
                f.addListener(ChannelFutureListener.CLOSE);
            }

        }
    }


    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        // Request to flush all pending messages via this ChannelOutboundInvoker.
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
