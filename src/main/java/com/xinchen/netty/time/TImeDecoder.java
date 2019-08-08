package com.xinchen.netty.time;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandler;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.ReplayingDecoder;

import java.util.List;

/**
 * 用于解决碎片问题
 * <p>
 * 解决方案二： 装饰器
 * <p>
 * 通过继承{@link ByteToMessageDecoder}
 *
 * {@link ByteToMessageDecoder} 实现了 {@link ChannelInboundHandler}，可以很容易地处理碎片问题
 *
 *
 * 然后在客户端添加Pipeline时，加入装饰器即可
 * <pre>
 * b.handler(new ChannelInitializer<SocketChannel>() {
 *     @Override
 *     public void initChannel(SocketChannel ch) throws Exception {
 *         ch.pipeline().addLast(new TimeDecoder(), new TimeClientHandler());
 *     }
 * });
 * </pre>
 *
 *
 *
 *
 * 这里还可以用更简单的方式，通过继承 {@link ReplayingDecoder}
 *
 * @author xinchen
 * @version 1.0
 * @date 08/08/2019 15:21
 */
public class TImeDecoder extends ByteToMessageDecoder {

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        // 此方法在当没有收到足够大小（4）的数据时不做任何事情，即out中不添加任何数据
        // 当接收到更多数据时，会再次Call这个方法
        if (in.readableBytes() < 4) {
            return;
        }


        // 如果decode()向out中添加数据，意味着成功解码了数据
        // ByteToMessageDecoder将丢弃累积缓冲区的读取部分
        // ByteToMessageDecoder将继续调用该decode()方法，直到它不添加任何内容out
        out.add(in.readBytes(4));
    }
}
