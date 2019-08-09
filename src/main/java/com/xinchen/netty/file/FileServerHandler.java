package com.xinchen.netty.file;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.DefaultFileRegion;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.stream.ChunkedFile;

import java.io.RandomAccessFile;

/**
 *
 * 文件服务器处理器
 *
 * @author xinchen
 * @version 1.0
 * @date 09/08/2019 14:50
 */
public class FileServerHandler extends SimpleChannelInboundHandler<String> {
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ctx.writeAndFlush("HELLO: type the path of the file to retrieve. \n");
    }


    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
        RandomAccessFile raf = null;
        long length = -1;

        try {
            // 读文件，msg为文件路径
            raf = new RandomAccessFile(msg, "r");
            length = raf.length();
        } catch (Exception e){
            ctx.writeAndFlush("ERR: " + e.getClass().getSimpleName() + ":" + e.getMessage() + "\n");
            return;
        } finally {
            if (length < 0 && raf != null) {
                // 如果读取过程中出错关闭io流
                raf.close();
            }
        }


        ctx.write("OK: " + raf.length() + '\n');

        if (ctx.pipeline().get(SslHandler.class) == null){
            // SSL未启用可以使用零拷贝文件传输
            ctx.write(new DefaultFileRegion(raf.getChannel(), 0, length));
        } else {
            // SSL启用不能使用零拷贝传输
            ctx.write(new ChunkedFile(raf));
        }

        ctx.writeAndFlush("\n");
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
