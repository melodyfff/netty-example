package com.xinchen.netty.time;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.util.Date;

/**
 * 32位整数是非常少量的数据，并且不太可能经常被分段
 * 然而，问题在于它可能是碎片化的，并且随着流量的增加，碎片化的可能性将增加
 *
 * 基于流的传输的缓冲区不是数据包队列而是字节队列
 * 这意味着，即使您将两条消息作为两个独立的数据包发送，操作系统也不会将它们视为两条消息，而只是一堆字节。
 * 因此，无法保证您所阅读的内容正是您的远程同行所写的内容
 *
 *
 * 解决方案一：
 *
 * 创建一个内部累积缓冲区，并等待所有4个字节都被接收到内部缓冲区
 *
 *
 * @author xinchen
 * @version 1.0
 * @date 08/08/2019 14:24
 */
public class TimeClientHandlerFIxTransport extends ChannelInboundHandlerAdapter {

    private ByteBuf buf;

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        ctx.alloc().buffer(4);
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        buf.release();
        buf = null;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        // 在TCP/IP中Netty将数据读入ByteBuf
        ByteBuf m = (ByteBuf) msg;

        // 将数据刷入内部缓冲区，并释放接收的对象
        buf.writeBytes(m);
        m.release();

        // 处理程序必须检查是否buf有足够的数据，在此示例中为4个字节，然后继续执行实际的业务逻辑。否则，channelRead()当更多数据到达时，Netty将再次调用该方法，最终将累计所有4个字节
        if (buf.readableBytes() >= 4){
            long currentTimeMillis = (m.readUnsignedInt() - 2208988800L) * 1000L;
            System.out.println(new Date(currentTimeMillis));
            ctx.close();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
