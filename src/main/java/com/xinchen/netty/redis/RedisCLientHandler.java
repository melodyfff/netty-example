package com.xinchen.netty.redis;

import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.CodecException;
import io.netty.handler.codec.redis.ArrayRedisMessage;
import io.netty.handler.codec.redis.ErrorRedisMessage;
import io.netty.handler.codec.redis.FullBulkStringRedisMessage;
import io.netty.handler.codec.redis.IntegerRedisMessage;
import io.netty.handler.codec.redis.RedisMessage;
import io.netty.handler.codec.redis.SimpleStringRedisMessage;
import io.netty.util.CharsetUtil;
import io.netty.util.ReferenceCountUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link FullBulkStringRedisMessage} 是基于RESP协议对redis消息的封装
 *
 * @author xinchen
 * @version 1.0
 * @date 09/08/2019 12:40
 */
public class RedisCLientHandler extends ChannelDuplexHandler {

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        String[] commands = ((String) msg).split("\\s+");
        List<RedisMessage> children = new ArrayList<>(commands.length);
        for (String command : commands) {
            // 将命令字符包裹成FullBulkStringRedisMessage
            children.add(new FullBulkStringRedisMessage(ByteBufUtil.writeUtf8(ctx.alloc(), command)));
        }
        RedisMessage request = new ArrayRedisMessage(children);

        ctx.write(request, promise);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        // 读取redis服务的返回的数据
        RedisMessage redisMessage = (RedisMessage) msg;
        printAggregatedRedisResponse(redisMessage);
        ReferenceCountUtil.release(redisMessage);

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        System.err.print("exceptionCaught: ");
        cause.printStackTrace();
        ctx.close();
    }


    private static void printAggregatedRedisResponse(RedisMessage msg) {
        if (msg instanceof SimpleStringRedisMessage) {
            System.out.println(((SimpleStringRedisMessage) msg).content());
        } else if (msg instanceof ErrorRedisMessage) {
            System.out.println(((ErrorRedisMessage) msg).content());
        } else if (msg instanceof IntegerRedisMessage) {
            System.out.println(((IntegerRedisMessage) msg).value());
        } else if (msg instanceof FullBulkStringRedisMessage) {
            System.out.println(getString((FullBulkStringRedisMessage) msg));
        } else if (msg instanceof ArrayRedisMessage) {
            ((ArrayRedisMessage) msg).children().forEach(RedisCLientHandler::printAggregatedRedisResponse);
        } else {
            throw new CodecException("unknown message type: " + msg);
        }
    }


    private static String getString(FullBulkStringRedisMessage msg) {
        return msg.isNull() ? "(null)" : msg.content().toString(CharsetUtil.UTF_8);
    }
}
