package com.xinchen.netty.time;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 *
 * 时间服务处理
 *
 * @author xinchen
 * @version 1.0
 * @date 08/08/2019 13:46
 */
public class TimeServerHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        // 要发送新消息，我们需要分配一个包含消息的新缓冲区
        // 我们要写一个32位整数，因此我们需要一个ByteBuf容量至少为4个字节的数据
        // 获取当前ByteBufAllocator通道ChannelHandlerContext.alloc()并分配新缓冲区
        final ByteBuf time = ctx.alloc().buffer(4);

        time.writeInt((int) (System.currentTimeMillis() / 1000L + 2208988800L));

        // ChannelFuture表示尚未发生的I/O操作,
        // 因此需要close()在ChannelFuture完成后调用该方法
        final ChannelFuture f = ctx.writeAndFlush(time);


        // 简化版写法
//         f.addListener(ChannelFutureListener.CLOSE);

        f.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                // assert @since 1.4
                assert f == future;
                ctx.close();
            }
        });
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
