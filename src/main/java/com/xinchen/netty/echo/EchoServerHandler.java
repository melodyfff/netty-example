package com.xinchen.netty.echo;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 *
 * Handler 为了输出接收到的消息
 *
 * {@link Sharable} 可以多次添加到一个或者多个{@link io.netty.channel.ChannelPipeline} 没有竞争条件
 * 如果未使用该注解，必须创建新的处理器Handler并每次都将它添加到Pipeline中,因为它已经取消共享状态如成员变量
 *
 * @author xinchen
 * @version 1.0
 * @date 08/08/2019 12:11
 */
@Sharable
public class EchoServerHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        // 表现为输入什么返回什么
        ctx.write(msg);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        // 当发生异常时关闭连接
        cause.printStackTrace();
        ctx.close();
    }
}
