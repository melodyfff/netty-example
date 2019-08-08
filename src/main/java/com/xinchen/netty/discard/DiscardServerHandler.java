package com.xinchen.netty.discard;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.ReferenceCountUtil;

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

        // 这里采用输出接收到的信息
        // telnet localhost 8009
        // 回车后输入消息即可，ctrl + ] 即可退出
        ByteBuf in = (ByteBuf) msg;
        System.out.print(ctx);
        try {
            while (in.isReadable()){
                System.out.print((char)in.readByte());
                System.out.flush();
            }
        } finally {

        }
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        // 当异常发生时，关闭连接
        cause.printStackTrace();
        ctx.close();
    }
}
