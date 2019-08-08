package com.xinchen.netty.time;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 *
 * encoder
 *
 * @author xinchen
 * @version 1.0
 * @date 08/08/2019 16:14
 */
public class TimeEncoder extends MessageToByteEncoder<UnixTIme> {

    @Override
    protected void encode(ChannelHandlerContext ctx, UnixTIme msg, ByteBuf out) throws Exception {
        out.writeInt((int) msg.value());
    }
}
