package com.xinchen.netty.proxy;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 *
 * 请求转发后台处理,主要处理入站数据
 *
 * @author xinchen
 * @version 1.0
 * @date 12/08/2019 16:46
 */
public class HexDumpProxyBackendHandler extends ChannelInboundHandlerAdapter {

    /** 入站网络抽象类 */
    private final Channel inboundChannel;

    public HexDumpProxyBackendHandler(Channel inboundChannel) {
        this.inboundChannel = inboundChannel;
    }



    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        // 当channel连上时读取数据
        ctx.read();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        // 直接写入和返回消息并添加监听器
        inboundChannel.writeAndFlush(msg).addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (future.isSuccess()){
                    ctx.channel().read();
                } else {
                    future.channel().close();
                }
            }
        });

    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        // 关闭入站请求
        HexDumpProxyFrontendHandler.closeOnFlush(inboundChannel);
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        HexDumpProxyFrontendHandler.closeOnFlush(ctx.channel());
    }
}
