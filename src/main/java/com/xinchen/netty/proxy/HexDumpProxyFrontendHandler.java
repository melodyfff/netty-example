package com.xinchen.netty.proxy;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelOption;

/**
 *
 * 请求转发前处理,主要处理出站数据
 *
 * @author xinchen
 * @version 1.0
 * @date 12/08/2019 16:52
 */
public class HexDumpProxyFrontendHandler extends ChannelInboundHandlerAdapter {

    private final String remoteHost;
    private final int remotePort;

    /** 出站网络抽象类 */
    private Channel outboundChannel;

    public HexDumpProxyFrontendHandler(String remoteHost, int remotePort) {
        this.remoteHost = remoteHost;
        this.remotePort = remotePort;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        final Channel inboundChannel = ctx.channel();

        // 开始尝试连接
        Bootstrap b = new Bootstrap();
        // 获取入站请求的eventLoop
        b.group(inboundChannel.eventLoop())
                .channel(ctx.channel().getClass())
                .handler(new HexDumpProxyBackendHandler(inboundChannel))
                // 关闭自动读取
                .option(ChannelOption.AUTO_READ, false);


        // 尝试将入站请求转发到代理地址
        final ChannelFuture f = b.connect(remoteHost, remotePort);
        outboundChannel = f.channel();

        f.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (future.isSuccess()){
                    // connection complete start to read first data
                    inboundChannel.read();
                } else {
                    // Close the connection if the connection attempt has failed.
                    inboundChannel.close();
                }
            }
        });


    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (outboundChannel.isActive()){
            outboundChannel.writeAndFlush(msg).addListener((ChannelFutureListener) future -> {
                if (future.isSuccess()){
                    // was able to flush out data ,start to read the next chunk
                    ctx.channel().read();
                } else {
                    future.channel().close();
                }
            });
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if (null != outboundChannel){
            closeOnFlush(outboundChannel);

        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }

    /**
     * Closes the specified channel after all queued write requests are flushed.
     * @param ch Channel
     */
    static void closeOnFlush(Channel ch){
        if(ch.isActive()){
            // 写入结束符，和添加关闭监听器
            ch.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        }
    }
}
