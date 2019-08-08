package com.xinchen.netty.time;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * 时间服务处理
 *
 * @author xinchen
 * @version 1.0
 * @date 08/08/2019 13:46
 */
public class TimeServerHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        // ChannelFuture表示尚未发生的I/O操作,
        // 因此需要close()在ChannelFuture完成后调用该方法
        final ChannelFuture f = ctx.writeAndFlush(new UnixTIme());

        f.addListener(ChannelFutureListener.CLOSE);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
