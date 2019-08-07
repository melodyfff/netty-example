package com.xinchen.netty.discard;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 *
 * 处理服务端通道channel
 *
 * 实现discard protocol(https://tools.ietf.org/html/rfc863),忽略所有收到的数据
 *
 * @author xinchen
 * @version 1.0
 * @date 07/08/2019 14:44
 */
public class DiscardServerHandler extends SimpleChannelInboundHandler<Object> {


    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        // discard 忽略
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        // 当异常发生时，关闭连接
        cause.printStackTrace();
        ctx.close();
    }
}
